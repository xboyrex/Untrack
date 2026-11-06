package com.xboyrex.untrack.fxengine.processing

import com.xboyrex.untrack.fxengine.rules.RuleTraceStep
import com.xboyrex.untrack.fxengine.utils.AlternativeFrontendCatalog
import com.xboyrex.untrack.fxengine.utils.ProxyPlatform

enum class ProcessingProfile {
    MAIN,
    SHARE,
    BROWSER
}

data class ProxySelections(val byPlatform: Map<ProxyPlatform, String?>) {
    fun domainFor(platform: ProxyPlatform): String? = byPlatform[platform]

    companion object {
        val EMPTY = ProxySelections(emptyMap())

        /** Catalog defaults — used by legacy [com.xboyrex.untrack.fxengine.UrlProcessor] test helpers. */
        val DEFAULT: ProxySelections = ProxySelections(
            ProxyPlatform.entries.associateWith { platform ->
                AlternativeFrontendCatalog.defaultTarget(platform)?.domain
            }
        )
    }
}

data class ProcessingOptions(
    val profile: ProcessingProfile,
    val cleanTracking: Boolean,
    val convertDomains: Boolean,
    val proxySelections: ProxySelections,
    val customRulesEnabled: Boolean,
    val persistHistory: Boolean = true,
    val useCache: Boolean = true,
    val traceEnabled: Boolean = false,
    val browserFrontends: BrowserFrontendSnapshot? = null,
)

data class PipelineProcessingResult(
    val originalUrl: String,
    val url: String,
    val wasAlreadyClean: Boolean,
    val builtinChanged: Boolean,
    val domainConverted: Boolean,
    val customRuleChanged: Boolean,
    val rulesRevision: Long,
    val trace: List<RuleTraceStep>,
    val operations: List<ChangeOperation>,
    /**
     * Exact URLs used as cleaner-cache keys during this run (only when caching
     * was enabled). Needed to evict entries when a leak shows up in the output:
     * custom PRE_CLEAN rules or redirect re-entries can make these differ from
     * [originalUrl].
     */
    val cleanerCacheKeys: List<String> = emptyList(),
    /** ASCII host after cleaning/custom rules, before optional frontend conversion. */
    val routingHost: String? = null,
    val status: PipelineStatus = PipelineStatus.COMPLETE,
)
