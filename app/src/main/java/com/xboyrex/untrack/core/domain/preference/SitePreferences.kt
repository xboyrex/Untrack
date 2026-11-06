package com.xboyrex.untrack.core.domain.preference

import com.xboyrex.untrack.core.domain.url.Url
import com.xboyrex.untrack.core.domain.withoutWww
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

/**
 * What the user decided about the parameters of one site, remembered across shares.
 *
 * Only deviations from the catalog are held, and only by parameter name: a value changes with every
 * share, a name does not. Because no sanitizer id is involved either, a remembered choice survives
 * the catalog being reworked in a later release.
 *
 * @param remove Parameters removed although no sanitizer proposes it.
 * @param keep Parameters kept although a sanitizer proposes removing them.
 */
data class SitePreference(
    val remove: ImmutableSet<String> = persistentSetOf(),
    val keep: ImmutableSet<String> = persistentSetOf(),
)

/** Every remembered [SitePreference], keyed by [site][Url.site]. */
typealias SitePreferences = ImmutableMap<String, SitePreference>

/**
 * The site a choice is remembered for: the host, lowercased and without a leading `www.` — the same
 * normalization [HostMatch.Domain][com.xboyrex.untrack.core.domain.sanitizer.HostMatch] uses. A
 * subdomain is a site of its own.
 */
val Url.site: String
    get() = host.withoutWww().lowercase()
