package com.xboyrex.untrack

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

object NetClient {
    val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    val downloadHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}

enum class PostCleanAction(val label: String, val subtitle: String) {
    SHOW_IN_APP("Open in Untrack Studio", "Inspect changes, toggle parameters, or switch frontends"),
    AUTO_COPY("Auto-Copy Clean URL", "Immediately copy cleaned link to clipboard & show toast"),
    AUTO_SHARE("Auto-Open Share Sheet", "Immediately open system Share menu with cleaned link"),
    AUTO_OPEN("Auto-Open in Browser", "Immediately launch cleaned link in your preferred browser")
}

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val originalUrl: String,
    val cleanedUrl: String,
    val domain: String,
    val removedParamsCount: Int,
    val frontendConverted: String?,
    val shortLinkResolved: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("originalUrl", originalUrl)
        put("cleanedUrl", cleanedUrl)
        put("domain", domain)
        put("removedParamsCount", removedParamsCount)
        put("frontendConverted", frontendConverted ?: "")
        put("shortLinkResolved", shortLinkResolved)
        put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(obj: JSONObject): HistoryItem? = runCatching {
            HistoryItem(
                id = obj.optString("id", UUID.randomUUID().toString()),
                originalUrl = obj.optString("originalUrl").ifBlank { return null },
                cleanedUrl = obj.optString("cleanedUrl").ifBlank { return null },
                domain = obj.optString("domain", "link"),
                removedParamsCount = obj.optInt("removedParamsCount", 0),
                frontendConverted = obj.optString("frontendConverted").takeIf { it.isNotBlank() },
                shortLinkResolved = obj.optBoolean("shortLinkResolved", false),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        }.getOrNull()
    }
}

object UntrackStore {
    private var prefs: SharedPreferences? = null

    // Core cleaning toggles
    var cleanTrackersEnabled by mutableStateOf(true)
        private set
    var convertFrontendsEnabled by mutableStateOf(true)
        private set
    var resolveShortLinksEnabled by mutableStateOf(true)
        private set
    var extractUrlOnly by mutableStateOf(false)
        private set
    var decodeUrlEnabled by mutableStateOf(true)
        private set
    var customRulesEnabled by mutableStateOf(true)
        private set
    var linkGuardEnabled by mutableStateOf(true)
        private set
    var rememberSiteChoicesEnabled by mutableStateOf(true)
        private set
    var historyEnabled by mutableStateOf(true)
        private set
    var useCustomTabs by mutableStateOf(true)
        private set
    var protectScreenEnabled by mutableStateOf(false)
        private set
    var registeredAsBrowser by mutableStateOf(false)
        private set

    var sharePostAction by mutableStateOf(PostCleanAction.SHOW_IN_APP)
        private set
    var browserPostAction by mutableStateOf(PostCleanAction.AUTO_OPEN)
        private set

    // Lifetime counters
    var totalLinksCleaned by mutableIntStateOf(0)
        private set
    var totalTrackersRemoved by mutableIntStateOf(0)
        private set
    var totalFrontendsFixed by mutableIntStateOf(0)
        private set

    // Disabled built-in sanitizers
    val disabledSanitizers = mutableStateMapOf<String, Boolean>()

    // Frontend platform enabled & selected target domain
    val enabledFrontendPlatforms = mutableStateMapOf<FrontendPlatform, Boolean>()
    val selectedFrontendDomains = mutableStateMapOf<FrontendPlatform, String>()
    val customFrontendDomains = mutableStateMapOf<FrontendPlatform, List<String>>()

    // Per-site remembered parameter decisions: key = "site|param", value = true (REMOVE) or false (KEEP)
    val rememberedSiteParams = mutableStateMapOf<String, Boolean>()

    // Custom URL rules
    val customRules = mutableStateListOf<CustomUrlRule>()

    // History items
    val history = mutableStateListOf<HistoryItem>()

    fun init(context: Context) {
        val p = context.getSharedPreferences("untrack_store", Context.MODE_PRIVATE).also { prefs = it }
        cleanTrackersEnabled = p.getBoolean("clean_trackers", true)
        convertFrontendsEnabled = p.getBoolean("convert_frontends", true)
        resolveShortLinksEnabled = p.getBoolean("resolve_short_links", true)
        extractUrlOnly = p.getBoolean("extract_url_only", false)
        decodeUrlEnabled = p.getBoolean("decode_url", true)
        customRulesEnabled = p.getBoolean("custom_rules_enabled", true)
        linkGuardEnabled = p.getBoolean("link_guard_enabled", true)
        rememberSiteChoicesEnabled = p.getBoolean("remember_site_choices", true)
        historyEnabled = p.getBoolean("history_enabled", true)
        useCustomTabs = p.getBoolean("use_custom_tabs", true)
        protectScreenEnabled = p.getBoolean("protect_screen", false)
        registeredAsBrowser = isBrowserAliasEnabled(context)

        sharePostAction = runCatching {
            PostCleanAction.valueOf(p.getString("share_post_action", PostCleanAction.SHOW_IN_APP.name)!!)
        }.getOrDefault(PostCleanAction.SHOW_IN_APP)

        browserPostAction = runCatching {
            PostCleanAction.valueOf(p.getString("browser_post_action", PostCleanAction.AUTO_OPEN.name)!!)
        }.getOrDefault(PostCleanAction.AUTO_OPEN)

        totalLinksCleaned = p.getInt("total_links_cleaned", 0)
        totalTrackersRemoved = p.getInt("total_trackers_removed", 0)
        totalFrontendsFixed = p.getInt("total_frontends_fixed", 0)

        // Load disabled sanitizers
        disabledSanitizers.clear()
        p.getStringSet("disabled_sanitizers", emptySet())?.forEach { id ->
            disabledSanitizers[id] = true
        }

        // Load frontend platform settings
        FrontendPlatform.entries.forEach { platform ->
            val defaultOn = platform in setOf(
                FrontendPlatform.X,
                FrontendPlatform.INSTAGRAM,
                FrontendPlatform.TIKTOK,
                FrontendPlatform.REDDIT,
                FrontendPlatform.BLUESKY
            )
            enabledFrontendPlatforms[platform] = p.getBoolean("fe_enabled_${platform.id}", defaultOn)
            val defaultDomain = FrontendCatalog.defaultTargetFor(platform)?.domain.orEmpty()
            selectedFrontendDomains[platform] = p.getString("fe_domain_${platform.id}", defaultDomain) ?: defaultDomain
            val rawCustom = p.getString("fe_custom_${platform.id}", "") ?: ""
            customFrontendDomains[platform] = rawCustom.split(",").map(String::trim).filter(String::isNotEmpty)
        }

        // Load remembered site params
        rememberedSiteParams.clear()
        val memJson = p.getString("remembered_site_params_json", null)
        if (!memJson.isNullOrBlank()) {
            runCatching {
                val obj = JSONObject(memJson)
                obj.keys().forEach { k ->
                    rememberedSiteParams[k] = obj.optBoolean(k, true)
                }
            }
        }

        // Load custom rules
        customRules.clear()
        val rulesJson = p.getString("custom_rules_json", null)
        if (!rulesJson.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(rulesJson)
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let(CustomUrlRule::fromJson)?.let(customRules::add)
                }
            }
        }
        if (customRules.isEmpty() && !p.getBoolean("seeded_default_custom_rules", false)) {
            seedStarterCustomRules()
            p.edit().putBoolean("seeded_default_custom_rules", true).apply()
        }

        // Load history
        history.clear()
        val histJson = p.getString("history_json", null)
        if (!histJson.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(histJson)
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let(HistoryItem::fromJson)?.let(history::add)
                }
            }
        }
    }

    private fun seedStarterCustomRules() {
        customRules.addAll(
            listOf(
                CustomUrlRule(
                    name = "Strip Discord & Telegram Referral Params",
                    hostPattern = "*",
                    phase = CustomRulePhase.POST_CLEAN,
                    action = CustomRuleAction.RemoveParams(listOf("ref", "referral", "source", "via", "from_source")),
                    enabled = true
                ),
                CustomUrlRule(
                    name = "GitHub Clean ?tab & Tracking",
                    hostPattern = "github.com",
                    phase = CustomRulePhase.POST_CLEAN,
                    action = CustomRuleAction.RemoveParams(listOf("utm_source", "utm_campaign", " s")),
                    enabled = true
                )
            )
        )
        saveCustomRules()
    }

    fun setCleanTrackers(v: Boolean) {
        cleanTrackersEnabled = v
        prefs?.edit()?.putBoolean("clean_trackers", v)?.apply()
    }

    fun setConvertFrontends(v: Boolean) {
        convertFrontendsEnabled = v
        prefs?.edit()?.putBoolean("convert_frontends", v)?.apply()
    }

    fun setResolveShortLinks(v: Boolean) {
        resolveShortLinksEnabled = v
        prefs?.edit()?.putBoolean("resolve_short_links", v)?.apply()
    }

    fun updateExtractUrlOnly(v: Boolean) {
        extractUrlOnly = v
        prefs?.edit()?.putBoolean("extract_url_only", v)?.apply()
    }

    fun setDecodeUrl(v: Boolean) {
        decodeUrlEnabled = v
        prefs?.edit()?.putBoolean("decode_url", v)?.apply()
    }

    fun setCustomRules(v: Boolean) {
        customRulesEnabled = v
        prefs?.edit()?.putBoolean("custom_rules_enabled", v)?.apply()
    }

    fun setLinkGuard(v: Boolean) {
        linkGuardEnabled = v
        prefs?.edit()?.putBoolean("link_guard_enabled", v)?.apply()
    }

    fun setRememberSiteChoices(v: Boolean) {
        rememberSiteChoicesEnabled = v
        prefs?.edit()?.putBoolean("remember_site_choices", v)?.apply()
        if (!v) clearRememberedSiteParams()
    }

    fun setHistory(v: Boolean) {
        historyEnabled = v
        prefs?.edit()?.putBoolean("history_enabled", v)?.apply()
        if (!v) clearHistory()
    }

    fun setCustomTabs(v: Boolean) {
        useCustomTabs = v
        prefs?.edit()?.putBoolean("use_custom_tabs", v)?.apply()
    }

    fun setProtectScreen(v: Boolean) {
        protectScreenEnabled = v
        prefs?.edit()?.putBoolean("protect_screen", v)?.apply()
    }

    fun updateSharePostAction(action: PostCleanAction) {
        sharePostAction = action
        prefs?.edit()?.putString("share_post_action", action.name)?.apply()
    }

    fun updateBrowserPostAction(action: PostCleanAction) {
        browserPostAction = action
        prefs?.edit()?.putString("browser_post_action", action.name)?.apply()
    }

    fun isSanitizerEnabled(id: String): Boolean = disabledSanitizers[id] != true

    fun setSanitizerEnabled(id: String, enabled: Boolean) {
        if (enabled) disabledSanitizers.remove(id) else disabledSanitizers[id] = true
        prefs?.edit()?.putStringSet("disabled_sanitizers", disabledSanitizers.keys.toSet())?.apply()
    }

    fun enableAllSanitizers() {
        disabledSanitizers.clear()
        prefs?.edit()?.putStringSet("disabled_sanitizers", emptySet())?.apply()
    }

    fun isPlatformEnabled(platform: FrontendPlatform): Boolean =
        enabledFrontendPlatforms[platform] == true

    fun setPlatformEnabled(platform: FrontendPlatform, enabled: Boolean) {
        enabledFrontendPlatforms[platform] = enabled
        prefs?.edit()?.putBoolean("fe_enabled_${platform.id}", enabled)?.apply()
    }

    fun getSelectedDomain(platform: FrontendPlatform): String =
        selectedFrontendDomains[platform] ?: FrontendCatalog.defaultTargetFor(platform)?.domain.orEmpty()

    fun setSelectedDomain(platform: FrontendPlatform, domain: String) {
        selectedFrontendDomains[platform] = domain
        prefs?.edit()?.putString("fe_domain_${platform.id}", domain)?.apply()
    }

    fun addCustomDomain(platform: FrontendPlatform, rawDomain: String): Boolean {
        val cleaned = rawDomain.trim()
            .lowercase(Locale.ROOT)
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
        if (cleaned.isBlank() || !cleaned.contains('.')) return false
        val current = customFrontendDomains[platform].orEmpty().toMutableList()
        if (cleaned !in current) {
            current.add(cleaned)
            customFrontendDomains[platform] = current
            prefs?.edit()?.putString("fe_custom_${platform.id}", current.joinToString(","))?.apply()
        }
        setSelectedDomain(platform, cleaned)
        setPlatformEnabled(platform, true)
        return true
    }

    fun removeCustomDomain(platform: FrontendPlatform, domain: String) {
        val current = customFrontendDomains[platform].orEmpty().filterNot { it.equals(domain, ignoreCase = true) }
        customFrontendDomains[platform] = current
        prefs?.edit()?.putString("fe_custom_${platform.id}", current.joinToString(","))?.apply()
        if (getSelectedDomain(platform).equals(domain, ignoreCase = true)) {
            setSelectedDomain(platform, FrontendCatalog.defaultTargetFor(platform)?.domain.orEmpty())
        }
    }

    fun setSiteParamDecision(site: String, paramName: String, remove: Boolean) {
        if (!rememberSiteChoicesEnabled) return
        val key = "${site.lowercase(Locale.ROOT)}|${paramName.lowercase(Locale.ROOT)}"
        rememberedSiteParams[key] = remove
        saveRememberedSiteParams()
    }

    fun getSiteParamDecision(site: String, paramName: String): Boolean? {
        if (!rememberSiteChoicesEnabled) return null
        val key = "${site.lowercase(Locale.ROOT)}|${paramName.lowercase(Locale.ROOT)}"
        return rememberedSiteParams[key]
    }

    fun clearRememberedSiteParams() {
        rememberedSiteParams.clear()
        prefs?.edit()?.remove("remembered_site_params_json")?.apply()
    }

    private fun saveRememberedSiteParams() {
        val obj = JSONObject()
        rememberedSiteParams.forEach { (k, v) -> obj.put(k, v) }
        prefs?.edit()?.putString("remembered_site_params_json", obj.toString())?.apply()
    }

    fun addOrUpdateCustomRule(rule: CustomUrlRule) {
        val idx = customRules.indexOfFirst { it.id == rule.id }
        if (idx >= 0) customRules[idx] = rule else customRules.add(0, rule)
        saveCustomRules()
    }

    fun deleteCustomRule(id: String) {
        customRules.removeAll { it.id == id }
        saveCustomRules()
    }

    fun toggleCustomRule(id: String, enabled: Boolean) {
        val idx = customRules.indexOfFirst { it.id == id }
        if (idx >= 0) {
            customRules[idx] = customRules[idx].copy(enabled = enabled)
            saveCustomRules()
        }
    }

    private fun saveCustomRules() {
        val arr = JSONArray()
        customRules.forEach { arr.put(it.toJson()) }
        prefs?.edit()?.putString("custom_rules_json", arr.toString())?.apply()
    }

    fun recordCleanResult(item: HistoryItem) {
        totalLinksCleaned += 1
        totalTrackersRemoved += item.removedParamsCount
        if (item.frontendConverted != null) {
            totalFrontendsFixed += 1
        }
        prefs?.edit()
            ?.putInt("total_links_cleaned", totalLinksCleaned)
            ?.putInt("total_trackers_removed", totalTrackersRemoved)
            ?.putInt("total_frontends_fixed", totalFrontendsFixed)
            ?.apply()

        if (!historyEnabled) return
        history.removeAll { it.originalUrl == item.originalUrl && it.cleanedUrl == item.cleanedUrl }
        history.add(0, item)
        while (history.size > 150) {
            history.removeAt(history.lastIndex)
        }
        saveHistory()
    }

    fun deleteHistoryItem(id: String) {
        history.removeAll { it.id == id }
        saveHistory()
    }

    fun clearHistory() {
        history.clear()
        prefs?.edit()?.remove("history_json")?.apply()
    }

    private fun saveHistory() {
        val arr = JSONArray()
        history.forEach { arr.put(it.toJson()) }
        prefs?.edit()?.putString("history_json", arr.toString())?.apply()
    }

    fun setBrowserInterceptor(context: Context, enabled: Boolean) {
        runCatching {
            val component = ComponentName(context, "${context.packageName}.BrowserInterceptorActivity")
            val state = if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            context.packageManager.setComponentEnabledSetting(
                component,
                state,
                PackageManager.DONT_KILL_APP
            )
            registeredAsBrowser = enabled
        }
    }

    private fun isBrowserAliasEnabled(context: Context): Boolean = runCatching {
        val component = ComponentName(context, "${context.packageName}.BrowserInterceptorActivity")
        context.packageManager.getComponentEnabledSetting(component) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }.getOrDefault(false)

    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("app", "Untrack")
        root.put("version", 1)
        root.put("cleanTrackers", cleanTrackersEnabled)
        root.put("convertFrontends", convertFrontendsEnabled)
        root.put("resolveShortLinks", resolveShortLinksEnabled)
        root.put("extractUrlOnly", extractUrlOnly)
        root.put("decodeUrl", decodeUrlEnabled)
        root.put("customRulesEnabled", customRulesEnabled)
        root.put("linkGuardEnabled", linkGuardEnabled)

        val rulesArr = JSONArray()
        customRules.forEach { rulesArr.put(it.toJson()) }
        root.put("customRules", rulesArr)

        val feObj = JSONObject()
        FrontendPlatform.entries.forEach { p ->
            feObj.put(p.id, JSONObject().apply {
                put("enabled", isPlatformEnabled(p))
                put("selectedDomain", getSelectedDomain(p))
                put("customDomains", JSONArray(customFrontendDomains[p].orEmpty()))
            })
        }
        root.put("frontends", feObj)
        return root.toString(2)
    }

    fun importBackupJson(rawJson: String): Boolean = runCatching {
        val root = JSONObject(rawJson)
        setCleanTrackers(root.optBoolean("cleanTrackers", cleanTrackersEnabled))
        setConvertFrontends(root.optBoolean("convertFrontends", convertFrontendsEnabled))
        setResolveShortLinks(root.optBoolean("resolveShortLinks", resolveShortLinksEnabled))
        updateExtractUrlOnly(root.optBoolean("extractUrlOnly", extractUrlOnly))
        setDecodeUrl(root.optBoolean("decodeUrl", decodeUrlEnabled))
        setCustomRules(root.optBoolean("customRulesEnabled", customRulesEnabled))
        setLinkGuard(root.optBoolean("linkGuardEnabled", linkGuardEnabled))

        root.optJSONArray("customRules")?.let { arr ->
            customRules.clear()
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let(CustomUrlRule::fromJson)?.let(customRules::add)
            }
            saveCustomRules()
        }

        root.optJSONObject("frontends")?.let { feObj ->
            FrontendPlatform.entries.forEach { p ->
                feObj.optJSONObject(p.id)?.let { pObj ->
                    setPlatformEnabled(p, pObj.optBoolean("enabled", isPlatformEnabled(p)))
                    pObj.optString("selectedDomain").takeIf { it.isNotBlank() }?.let {
                        setSelectedDomain(p, it)
                    }
                    pObj.optJSONArray("customDomains")?.let { cArr ->
                        for (idx in 0 until cArr.length()) {
                            cArr.optString(idx)?.takeIf { it.isNotBlank() }?.let { addCustomDomain(p, it) }
                        }
                    }
                }
            }
        }
        true
    }.getOrDefault(false)
}
