package com.xboyrex.untrack.fxengine.cleaners.impl

import com.xboyrex.untrack.fxengine.cleaners.PlatformParameterRule
import com.xboyrex.untrack.fxengine.cleaners.UrlCleaner
import com.xboyrex.untrack.fxengine.processing.UrlNormalizer
import java.util.Locale

class CatalogParameterCleaner(
    val rule: PlatformParameterRule
) : UrlCleaner {
    override val id: String = rule.id
    override val displayName: String = rule.displayName
    override val category = rule.category
    private val removeKeys = rule.removeKeys.mapTo(mutableSetOf(), ::comparisonKey)
    private val preserveKeys = rule.preserveKeys.mapTo(mutableSetOf(), ::comparisonKey)
    private val removePrefixes = rule.removePrefixes.map(::comparisonKey)

    override fun matches(url: String): Boolean =
        if (rule.includeSubdomains) {
            UrlNormalizer.urlMatchesAnyDomain(url, rule.domains)
        } else {
            val host = UrlNormalizer.extractAsciiHost(url)
            rule.domains.any { it.equals(host, ignoreCase = true) }
        }

    private fun comparisonKey(key: String): String =
        if (rule.ignoreKeyCase) key.lowercase(Locale.ROOT) else key

    override fun clean(url: String): String {
        if (!matches(url)) return url

        return try {
            val queryIndex = url.indexOf('?')
            val fragmentIndex = url.indexOf('#')
            if (queryIndex < 0 || (fragmentIndex >= 0 && fragmentIndex < queryIndex)) {
                return url
            }

            val base = url.substring(0, queryIndex)
            val queryEnd = if (fragmentIndex >= 0) fragmentIndex else url.length
            val query = url.substring(queryIndex + 1, queryEnd)
            val fragment = if (fragmentIndex >= 0) url.substring(fragmentIndex) else ""

            // Keep an explicit empty query byte-for-byte.
            if (query.isEmpty()) return url

            val kept = query.split('&').mapNotNull { token ->
                val key = comparisonKey(token.substringBefore('='))
                when {
                    preserveKeys.contains(key) -> token
                    removeKeys.contains(key) -> null
                    removePrefixes.any { key.startsWith(it) } -> null
                    else -> token
                }
            }.filter { it.isNotEmpty() }

            if (kept.isEmpty()) base + fragment else "$base?${kept.joinToString("&")}$fragment"
        } catch (_: Exception) {
            url
        }
    }
}
