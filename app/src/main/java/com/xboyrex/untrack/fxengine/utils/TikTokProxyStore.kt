package com.xboyrex.untrack.fxengine.utils

/**
 * Thin facade over [ProxyRoster] for TikTok embed proxy domains — mirrors [InstagramProxyStore].
 *
 * The fixed roster lives in [AlternativeFrontendCatalog]; user-defined custom proxies
 * are persisted by `PreferencesManager` (`custom_tiktok_proxies` pref) which mirrors
 * them into [ProxyRoster] on construction and on every mutation.
 *
 * Thread safety: delegated to [ProxyRoster]'s immutable snapshot model.
 */
object TikTokProxyStore {

    private val platform = ProxyPlatform.TIKTOK

    /** Replace the custom proxy snapshot (called by PreferencesManager). */
    fun setCustomProxies(proxies: List<String>) {
        ProxyRoster.setCustomProxies(platform, proxies)
    }

    fun getCustomProxies(): List<String> = ProxyRoster.getCustomProxies(platform)

    /** Active selectable targets for TikTok embed proxies. */
    fun activeProxies(): List<String> =
        ProxyRoster.activeTargets(platform).map { it.domain }

    /** Every domain recognised as a TikTok proxy (all built-ins + custom + legacy). */
    fun allKnownProxies(): List<String> = ProxyRoster.allKnownDomains(platform)

    /** Test helper — clears TikTok platform state so tests don't leak into each other. */
    fun reset() {
        ProxyRoster.resetPlatform(platform)
    }

    fun normalizeCustomProxyInput(raw: String): String =
        ProxyRoster.normalizeCustomProxyInput(raw)

    fun isValidProxyDomainFormat(domain: String): Boolean =
        ProxyRoster.isValidProxyDomainFormat(domain)

    fun isReservedDomain(domain: String): Boolean =
        ProxyRoster.isReservedDomain(domain)

    fun isDuplicate(domain: String): Boolean =
        ProxyRoster.isDuplicate(platform, domain)
}
