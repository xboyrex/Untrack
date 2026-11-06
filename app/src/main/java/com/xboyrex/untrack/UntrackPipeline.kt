package com.xboyrex.untrack

import com.xboyrex.untrack.core.domain.Cleaner
import com.xboyrex.untrack.core.domain.preference.SitePreference
import com.xboyrex.untrack.core.domain.preference.SitePreferences
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerRepository
import com.xboyrex.untrack.core.domain.sanitizer.catalog.AllSanitizers
import com.xboyrex.untrack.core.domain.url.Url
import com.xboyrex.untrack.fxengine.cleaners.CleanerCatalog
import com.xboyrex.untrack.fxengine.cleaners.CleanerRegistry
import com.xboyrex.untrack.fxengine.cleaners.CleanerService
import com.xboyrex.untrack.fxengine.cleaners.cache.CleanerCache
import com.xboyrex.untrack.fxengine.processing.LeakCategory
import com.xboyrex.untrack.fxengine.processing.LinkLeakAnalyzer
import com.xboyrex.untrack.fxengine.processing.PlatformDomainConverter
import com.xboyrex.untrack.fxengine.processing.ProxySelections
import com.xboyrex.untrack.fxengine.utils.ProxyPlatform
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.URI
import java.util.Locale

data class AppliedChangeRow(
    val id: String,
    val label: String,
    val detail: String,
    val paramName: String? = null,
    val isRemoved: Boolean = true,
    val sourceSanitizer: String
)

data class CleanedUrlReport(
    val originalUrl: String,
    val cleanedUrl: String,
    val parsedOriginal: ParsedUrl,
    val parsedCleaned: ParsedUrl,
    val detectedPlatform: FrontendPlatform?,
    val convertedFrontendDomain: String?,
    val shortLinkResolved: Boolean,
    val shortLinkHops: List<String>,
    val parameterRows: List<AppliedChangeRow>,
    val appliedRuleNames: List<String>,
    val leakWarnings: List<LeakWarning>,
    val wasModified: Boolean,
)

data class UntrackBatchResult(
    val originalText: String,
    val outputText: String,
    val urlReports: List<CleanedUrlReport>,
) {
    val primaryReport: CleanedUrlReport?
        get() = urlReports.firstOrNull()

    val totalRemovedParams: Int
        get() = urlReports.sumOf { report -> report.parameterRows.count { it.isRemoved } }

    val anyModified: Boolean
        get() = urlReports.any { it.wasModified }
}

object UntrackPipeline {

    private val fxRegistry: CleanerRegistry by lazy {
        CleanerRegistry().apply {
            registerAll(CleanerCatalog.createBuiltInCleaners())
        }
    }
    private val fxCleanerService: CleanerService by lazy {
        CleanerService(fxRegistry, CleanerCache())
    }

    private val domainSanitizerRepo = object : SanitizerRepository {
        override val state: Flow<ImmutableList<SanitizerRepository.SanitizerState>>
            get() = flowOf(
                AllSanitizers.map { s: Sanitizer ->
                    SanitizerRepository.SanitizerState(s.id, UntrackStore.isSanitizerEnabled(s.id.value))
                }.toImmutableList()
            )

        override suspend fun isEnabled(id: SanitizerId): Boolean =
            UntrackStore.isSanitizerEnabled(id.value)

        override suspend fun setEnabled(id: SanitizerId, enabled: Boolean) {
            UntrackStore.setSanitizerEnabled(id.value, enabled)
        }
    }

    private val domainCleaner: Cleaner by lazy {
        Cleaner(sanitizers = AllSanitizers, repository = domainSanitizerRepo)
    }

    suspend fun processText(
        rawText: String,
        convertToAlternativeOverride: Boolean? = null,
        manuallyKeptParams: Set<String> = emptySet(),
        manuallyRemovedParams: Set<String> = emptySet(),
    ): UntrackBatchResult = withContext(Dispatchers.Default) {
        val trimmed = rawText.trim().removePrefix("@")
        if (trimmed.isEmpty()) {
            return@withContext UntrackBatchResult(rawText, "", emptyList())
        }

        val normalizedInput = if (
            !trimmed.contains("://") &&
            !trimmed.contains(' ') &&
            !trimmed.contains('\n') &&
            trimmed.contains('.')
        ) {
            "https://$trimmed"
        } else {
            trimmed
        }

        val matches = ParsedUrl.FIND_URLS_REGEX.findAll(normalizedInput).toList()
        if (matches.isEmpty()) {
            val single = ParsedUrl.parse(normalizedInput)
            if (single != null) {
                val rep = processSingleUrl(
                    single.toString(),
                    convertToAlternativeOverride,
                    manuallyKeptParams,
                    manuallyRemovedParams
                )
                return@withContext UntrackBatchResult(
                    originalText = rawText,
                    outputText = rep.cleanedUrl,
                    urlReports = listOf(rep)
                )
            }
            return@withContext UntrackBatchResult(rawText, rawText, emptyList())
        }

        val reports = mutableListOf<CleanedUrlReport>()
        var updatedText = normalizedInput

        for (match in matches) {
            val rawUrl = match.value.trimEnd('.', ',', ';', ')', ']', '>', '"', '\'')
            val report = processSingleUrl(
                rawUrl,
                convertToAlternativeOverride,
                manuallyKeptParams,
                manuallyRemovedParams
            )
            reports += report
            updatedText = updatedText.replaceFirst(rawUrl, report.cleanedUrl)
        }

        val finalOutput = if (UntrackStore.extractUrlOnly && reports.isNotEmpty()) {
            reports.joinToString("\n") { it.cleanedUrl }
        } else {
            updatedText
        }

        UntrackBatchResult(
            originalText = rawText,
            outputText = finalOutput,
            urlReports = reports
        )
    }

    suspend fun processSingleUrl(
        rawUrl: String,
        convertToAlternativeOverride: Boolean? = null,
        manuallyKeptParams: Set<String> = emptySet(),
        manuallyRemovedParams: Set<String> = emptySet(),
    ): CleanedUrlReport {
        val initialParsed = ParsedUrl.parse(rawUrl)
            ?: return CleanedUrlReport(
                originalUrl = rawUrl,
                cleanedUrl = rawUrl,
                parsedOriginal = ParsedUrl("https", null, "invalid.url", null, "", emptyList(), null),
                parsedCleaned = ParsedUrl("https", null, "invalid.url", null, "", emptyList(), null),
                detectedPlatform = null,
                convertedFrontendDomain = null,
                shortLinkResolved = false,
                shortLinkHops = emptyList(),
                parameterRows = emptyList(),
                appliedRuleNames = emptyList(),
                leakWarnings = emptyList(),
                wasModified = false
            )

        var current = initialParsed
        val appliedRuleNames = mutableListOf<String>()
        val shortLinkHops = mutableListOf<String>()
        var shortLinkResolved = false
        val resolvedShortMap = mutableMapOf<Url, Url>()

        // 1. Short-link network resolution (uses both core.domain.Cleaner.shortLinkRequest & SanitizerCatalog)
        if (UntrackStore.resolveShortLinksEnabled && isShortLinkUrl(current)) {
            val resolved = withContext(Dispatchers.IO) { resolveShortLinkHops(current) }
            if (resolved.first != current) {
                val origDomainUrl = Url.parse(current.toString())
                val targetDomainUrl = Url.parse(resolved.first.toString())
                if (origDomainUrl != null && targetDomainUrl != null) {
                    resolvedShortMap[origDomainUrl] = targetDomainUrl
                }
                current = resolved.first
                shortLinkHops.addAll(resolved.second)
                shortLinkResolved = true
                appliedRuleNames += "Unshortened (${shortLinkHops.size} hop${if (shortLinkHops.size == 1) "" else "s"})"
            }
        }

        val postResolveOriginal = current

        // 2. PRE_CLEAN Custom Rules
        if (UntrackStore.customRulesEnabled) {
            UntrackStore.customRules
                .filter { it.enabled && it.phase == CustomRulePhase.PRE_CLEAN && it.matches(current) }
                .forEach { rule ->
                    val after = rule.apply(current)
                    if (after != current) {
                        current = after
                        appliedRuleNames += "Custom: ${rule.name}"
                    }
                }
        }

        val removedBySanitizer = mutableMapOf<String, String>()

        if (UntrackStore.cleanTrackersEnabled) {
            // 3A. Run verbatim core.domain.Cleaner (all 93 AllSanitizers + SitePreferences)
            runCatching {
                val sitePrefs = buildCoreSitePreferences()
                val coreRes = domainCleaner.clean(
                    text = current.toString(),
                    decodeUrl = false,
                    resolved = resolvedShortMap,
                    remembered = sitePrefs
                )
                val firstCleaned = coreRes.urls.firstOrNull()
                if (firstCleaned != null && firstCleaned.applied.isNotEmpty()) {
                    val parsedAfterCore = ParsedUrl.parse(firstCleaned.cleaned.toString())
                    if (parsedAfterCore != null && parsedAfterCore != current) {
                        val afterNames = parsedAfterCore.parameters.map { it.name.lowercase(Locale.ROOT) }.toSet()
                        current.parameters.forEach { p ->
                            if (p.name.lowercase(Locale.ROOT) !in afterNames) {
                                removedBySanitizer.putIfAbsent(p.name, "Core Catalog Sanitizer")
                            }
                        }
                        firstCleaned.applied.mapNotNull { it.sanitizerId?.value }.distinct().forEach { sId ->
                            val friendly = AllSanitizers.firstOrNull { s: Sanitizer -> s.id.value == sId }?.name ?: sId
                            if (friendly !in appliedRuleNames) appliedRuleNames += friendly
                        }
                        current = parsedAfterCore
                    }
                }
            }

            // 3B. Run verbatim fxengine.cleaners.CleanerService (12 Dedicated Cleaners + 17 ParameterRuleCatalog + GeneralTrackingCleaner)
            runCatching {
                val fxRes = fxCleanerService.deepCleanWithDetailsWithoutCache(current.toString())
                if (fxRes.cleanedUrl != current.toString()) {
                    val parsedAfterFx = ParsedUrl.parse(fxRes.cleanedUrl)
                    if (parsedAfterFx != null) {
                        val afterNames = parsedAfterFx.parameters.map { it.name.lowercase(Locale.ROOT) }.toSet()
                        val cleanerSource = fxRes.operations.firstOrNull()?.source?.takeIf { it.isNotBlank() } ?: "Deep Cleaner"
                        current.parameters.forEach { p ->
                            if (p.name.lowercase(Locale.ROOT) !in afterNames) {
                                removedBySanitizer.putIfAbsent(p.name, cleanerSource)
                            }
                        }
                        fxRes.operations.map { it.source }.filter { it.isNotBlank() }.forEach { src ->
                            if (src !in appliedRuleNames) appliedRuleNames += src
                        }
                        current = parsedAfterFx
                    }
                }
            }

            // 3C. Run SanitizerCatalog iterative rules
            for (pass in 0 until 3) {
                var changedInPass = false
                val activeSanitizers = SanitizerCatalog.allSanitizers.filter { spec ->
                    UntrackStore.isSanitizerEnabled(spec.id) && spec.matches.any { it.matches(current) }
                }
                for (spec in activeSanitizers) {
                    for (rule in spec.rules) {
                        val beforeRule = current
                        current = applySanitizerRule(
                            url = current,
                            rule = rule,
                            sanitizerName = spec.name,
                            removedBySanitizer = removedBySanitizer,
                            manuallyKeptParams = manuallyKeptParams
                        )
                        if (current != beforeRule) {
                            changedInPass = true
                            if (spec.name !in appliedRuleNames) {
                                appliedRuleNames += spec.name
                            }
                        }
                    }
                }
                if (!changedInPass) break
            }
        }

        // 4. Apply Per-Site Remembered Choices & Interactive Manual Overrides
        val site = current.site
        val filteredParams = mutableListOf<ParsedParameter>()
        for (param in postResolveOriginal.parameters) {
            val keyLower = param.name.lowercase(Locale.ROOT)
            val wasRemovedByCatalog = current.parameters.none { it.name.equals(param.name, ignoreCase = true) }
            val rememberedDecision = UntrackStore.getSiteParamDecision(site, keyLower)

            val shouldRemove = when {
                keyLower in manuallyRemovedParams.map { it.lowercase(Locale.ROOT) } -> true
                keyLower in manuallyKeptParams.map { it.lowercase(Locale.ROOT) } -> false
                rememberedDecision != null -> rememberedDecision
                else -> wasRemovedByCatalog
            }

            if (shouldRemove) {
                if (param.name !in removedBySanitizer) {
                    removedBySanitizer[param.name] = if (rememberedDecision == true) "Remembered for $site" else "Manual Removal"
                }
            } else {
                val survivingParam = current.parameters.firstOrNull { it.name.equals(param.name, ignoreCase = true) } ?: param
                filteredParams += survivingParam
            }
        }
        // Keep any newly added essential parameters (e.g. from redirect unwrapping)
        for (param in current.parameters) {
            if (filteredParams.none { it.name.equals(param.name, ignoreCase = true) } &&
                postResolveOriginal.parameters.none { it.name.equals(param.name, ignoreCase = true) }
            ) {
                filteredParams += param
            }
        }
        current = current.copy(parameters = filteredParams)

        // 5. POST_CLEAN Custom Rules
        if (UntrackStore.customRulesEnabled) {
            UntrackStore.customRules
                .filter { it.enabled && it.phase == CustomRulePhase.POST_CLEAN && it.matches(current) }
                .forEach { rule ->
                    val after = rule.apply(current)
                    if (after != current) {
                        current = after
                        appliedRuleNames += "Custom: ${rule.name}"
                    }
                }
        }

        // 6. Alternative Privacy Frontend / Rich Embed Domain Conversion (uses both FrontendCatalog & fxengine PlatformDomainConverter)
        val enabledPlatforms = FrontendPlatform.entries.filter { UntrackStore.isPlatformEnabled(it) }.toSet()
        val shouldConvert = convertToAlternativeOverride ?: UntrackStore.convertFrontendsEnabled
        val detectedPlatform = FrontendCatalog.detectPlatform(current, UntrackStore.customFrontendDomains)
        var convertedDomain: String? = null

        if (detectedPlatform != null) {
            val activePlatforms = if (convertToAlternativeOverride != null) FrontendPlatform.entries.toSet() else enabledPlatforms
            if (detectedPlatform in activePlatforms) {
                val (convertedUrl, platformUsed) = FrontendCatalog.convertUrl(
                    url = current,
                    convertToAlternative = shouldConvert,
                    enabledPlatforms = activePlatforms,
                    selectedDomains = UntrackStore.selectedFrontendDomains,
                    customDomains = UntrackStore.customFrontendDomains
                )
                if (convertedUrl != current && platformUsed != null) {
                    current = convertedUrl
                    convertedDomain = convertedUrl.host
                    appliedRuleNames += "${platformUsed.title} → ${convertedUrl.host}"
                } else {
                    // Fallback to fxengine PlatformDomainConverter for specialized path/host conversions
                    runCatching {
                        val proxyMap = mutableMapOf<ProxyPlatform, String?>()
                        ProxyPlatform.entries.forEach { pp ->
                            val fp = FrontendPlatform.entries.firstOrNull { it.name == pp.name }
                            if (fp != null && fp in activePlatforms) {
                                proxyMap[pp] = UntrackStore.getSelectedDomain(fp)
                            } else {
                                proxyMap[pp] = null
                            }
                        }
                        val fxConvertedStr = PlatformDomainConverter.apply(
                            current.toString(),
                            shouldConvert,
                            ProxySelections(proxyMap)
                        )
                        val fxParsed = ParsedUrl.parse(fxConvertedStr)
                        if (fxParsed != null && fxParsed != current) {
                            current = fxParsed
                            convertedDomain = fxParsed.host
                            appliedRuleNames += "${detectedPlatform.title} → ${fxParsed.host}"
                        }
                    }
                }
            }
        }

        // 7. POST_CONVERSION Custom Rules
        if (UntrackStore.customRulesEnabled) {
            UntrackStore.customRules
                .filter { it.enabled && it.phase == CustomRulePhase.POST_CONVERSION && it.matches(current) }
                .forEach { rule ->
                    val after = rule.apply(current)
                    if (after != current) {
                        current = after
                        appliedRuleNames += "Custom: ${rule.name}"
                    }
                }
        }

        // 8. Component-aware URL decoding
        if (UntrackStore.decodeUrlEnabled) {
            current = current.decoded()
        }

        val survivingNames = current.parameters.map { it.name.lowercase(Locale.ROOT) }.toSet()
        val parameterRows = postResolveOriginal.parameters.mapIndexed { idx, param ->
            val isRemoved = param.name.lowercase(Locale.ROOT) !in survivingNames
            val sourceLabel = removedBySanitizer[param.name]
                ?: if (isRemoved) "Tracker Sanitizer" else "Preserved Parameter"
            AppliedChangeRow(
                id = "param_${idx}_${param.name}",
                label = "?${param.name}=${param.value.orEmpty()}",
                detail = if (isRemoved) "Removed by $sourceLabel" else "Kept in cleaned URL ($sourceLabel)",
                paramName = param.name,
                isRemoved = isRemoved,
                sourceSanitizer = sourceLabel
            )
        }

        // 9. Combine Private Link Guard findings from both LinkLeakGuard & fxengine.processing.LinkLeakAnalyzer
        val leakWarnings = if (UntrackStore.linkGuardEnabled) {
            val baseWarnings = LinkLeakGuard.inspect(current).toMutableList()
            val fxFindings = LinkLeakAnalyzer.analyze(current.toString())
            fxFindings.forEach { finding ->
                val mappedType = when (finding.category) {
                    LeakCategory.CREDENTIALS -> LeakType.CREDENTIALS
                    LeakCategory.EMAIL -> LeakType.EMAIL
                    LeakCategory.JWT -> LeakType.JWT
                    LeakCategory.TOKEN_PARAM -> LeakType.SENSITIVE_TOKEN
                    LeakCategory.COORDINATES -> LeakType.PRECISE_GPS
                }
                val candidate = LeakWarning(mappedType, finding.parameterName)
                if (baseWarnings.none { it.type == candidate.type && it.parameterName == candidate.parameterName }) {
                    baseWarnings += candidate
                }
            }
            baseWarnings
        } else {
            emptyList()
        }

        val finalUrlStr = current.toString()
        val wasModified = finalUrlStr != initialParsed.toString() ||
            shortLinkResolved ||
            convertedDomain != null ||
            parameterRows.any { it.isRemoved }

        return CleanedUrlReport(
            originalUrl = rawUrl,
            cleanedUrl = finalUrlStr,
            parsedOriginal = initialParsed,
            parsedCleaned = current,
            detectedPlatform = detectedPlatform,
            convertedFrontendDomain = convertedDomain,
            shortLinkResolved = shortLinkResolved,
            shortLinkHops = shortLinkHops,
            parameterRows = parameterRows,
            appliedRuleNames = appliedRuleNames.distinct(),
            leakWarnings = leakWarnings,
            wasModified = wasModified
        )
    }

    private fun buildCoreSitePreferences(): SitePreferences {
        if (!UntrackStore.rememberSiteChoicesEnabled) return kotlinx.collections.immutable.persistentMapOf()
        val removeBySite = mutableMapOf<String, MutableSet<String>>()
        val keepBySite = mutableMapOf<String, MutableSet<String>>()
        for ((key, shouldRemove) in UntrackStore.rememberedSiteParams) {
            val site = key.substringBefore('|', "")
            val param = key.substringAfter('|', "")
            if (site.isNotBlank() && param.isNotBlank()) {
                if (shouldRemove) {
                    removeBySite.getOrPut(site) { mutableSetOf() }.add(param)
                } else {
                    keepBySite.getOrPut(site) { mutableSetOf() }.add(param)
                }
            }
        }
        val allSites = removeBySite.keys + keepBySite.keys
        return allSites.associateWith { s ->
            SitePreference(
                remove = removeBySite[s]?.toImmutableSet() ?: persistentSetOf(),
                keep = keepBySite[s]?.toImmutableSet() ?: persistentSetOf()
            )
        }.toImmutableMap()
    }

    private suspend fun isShortLinkUrl(url: ParsedUrl): Boolean {
        val inCatalog = SanitizerCatalog.allSanitizers.any { spec ->
            spec.isShortLinkResolver &&
                UntrackStore.isSanitizerEnabled(spec.id) &&
                spec.matches.any { it.matches(url) }
        }
        if (inCatalog) return true
        val domainUrl = Url.parse(url.toString()) ?: return false
        return domainCleaner.isShortLink(domainUrl)
    }

    private suspend fun resolveShortLinkHops(start: ParsedUrl): Pair<ParsedUrl, List<String>> {
        var current = start
        val hops = mutableListOf<String>()
        for (hop in 0 until 5) {
            if (hop > 0 && !isShortLinkUrl(current)) break
            val domainUrl = Url.parse(current.toString())
            val reqUrlStr = (domainUrl?.let { domainCleaner.shortLinkRequest(it) }?.toString())
                ?: current.copy(fragment = null).toString()

            val nextUrlStr = runCatching {
                val req = Request.Builder()
                    .url(reqUrlStr)
                    .header("User-Agent", "Mozilla/5.0 (Android 15; Mobile; rv:130.0) Gecko/130.0 Firefox/130.0")
                    .get()
                    .build()
                NetClient.http.newCall(req).execute().use { resp ->
                    if (resp.code in 300..399) {
                        resp.header("Location")
                    } else null
                }
            }.getOrNull() ?: break

            val resolvedNext = resolveRedirectLocation(current, nextUrlStr) ?: break
            if (resolvedNext.path.isEmpty() || resolvedNext.path == "/") break
            hops += resolvedNext.host
            current = resolvedNext
        }
        return current to hops
    }

    private fun resolveRedirectLocation(current: ParsedUrl, location: String): ParsedUrl? {
        return when {
            location.startsWith("http://", ignoreCase = true) || location.startsWith("https://", ignoreCase = true) ->
                ParsedUrl.parse(location)
            location.startsWith("//") ->
                ParsedUrl.parse("${current.scheme}:$location")
            location.startsWith("/") ->
                ParsedUrl.parse("${current.scheme}://${current.host}$location")
            else -> runCatching {
                URI(current.toString()).resolve(location).toString()
            }.getOrNull()?.let(ParsedUrl::parse)
        }
    }

    private fun applySanitizerRule(
        url: ParsedUrl,
        rule: SanitizerRule,
        sanitizerName: String,
        removedBySanitizer: MutableMap<String, String>,
        manuallyKeptParams: Set<String>
    ): ParsedUrl {
        val keptLower = manuallyKeptParams.map { it.lowercase(Locale.ROOT) }.toSet()
        return when (rule) {
            is SanitizerRule.RemoveExactParameters -> {
                val remaining = url.parameters.filter { param ->
                    val k = param.name.lowercase(Locale.ROOT)
                    if (k in keptLower) return@filter true
                    val matchesExact = k in rule.names
                    val matchesPrefix = rule.prefixes.any { k.startsWith(it) }
                    val remove = matchesExact || matchesPrefix
                    if (remove) removedBySanitizer.putIfAbsent(param.name, sanitizerName)
                    !remove
                }
                url.copy(parameters = remaining)
            }
            is SanitizerRule.RemoveParameters -> {
                val regex = Regex("^(?:${rule.nameRegex})$", RegexOption.IGNORE_CASE)
                val remaining = url.parameters.filter { param ->
                    val k = param.name.lowercase(Locale.ROOT)
                    if (k in keptLower) return@filter true
                    val matches = regex.matches(param.name)
                    val remove = if (rule.negate) !matches else matches
                    if (remove) removedBySanitizer.putIfAbsent(param.name, sanitizerName)
                    !remove
                }
                url.copy(parameters = remaining)
            }
            SanitizerRule.RemoveEmptyParameters -> {
                url.copy(parameters = url.parameters.filterNot { it.value?.isEmpty() == true })
            }
            is SanitizerRule.RemoveFragment -> {
                val frag = url.fragment ?: return url
                val shouldRemove = rule.pattern == null || Regex(rule.pattern, RegexOption.IGNORE_CASE).matches(frag)
                if (shouldRemove) url.copy(fragment = null) else url
            }
            is SanitizerRule.RewriteHost -> {
                val sourceVal = extractSource(url, rule.from) ?: return url
                val regex = Regex(rule.pattern, RegexOption.IGNORE_CASE)
                if (!regex.matches(sourceVal)) return url
                val newHost = regex.replaceFirst(sourceVal, rule.replacement)
                url.copy(host = newHost)
            }
            is SanitizerRule.RewritePath -> {
                val sourceVal = extractSource(url, rule.from) ?: return url
                val regex = Regex(rule.pattern, RegexOption.IGNORE_CASE)
                if (!regex.matches(sourceVal)) return url
                val newPath = regex.replaceFirst(sourceVal, rule.replacement)
                url.copy(path = newPath)
            }
            is SanitizerRule.FollowRedirect -> {
                var candidate = extractSource(url, rule.from) ?: return url
                for (step in rule.steps) {
                    candidate = ParsedUrl.decodeStep(candidate, step) ?: return url
                }
                val target = ParsedUrl.parse(candidate) ?: return url
                if (rule.dropParameters) target.copy(parameters = emptyList()) else target
            }
            is SanitizerRule.ShortLinkResolve -> url
        }
    }

    private fun extractSource(url: ParsedUrl, source: RuleSource): String? = when (source) {
        RuleSource.Host -> url.host
        RuleSource.Path -> url.path
        RuleSource.Fragment -> url.fragment
        is RuleSource.Parameter -> {
            val regex = Regex("^(?:${source.nameRegex})$", RegexOption.IGNORE_CASE)
            url.parameters.firstOrNull { regex.matches(it.name) }?.value
        }
    }
}
