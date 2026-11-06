package com.xboyrex.untrack.core.domain.sanitizer

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Removes tracking from the URLs of one site or one tracking product.
 *
 * A sanitizer is data, not code: [match] says which URLs it applies to and [rules] say what it
 * would do to them. Interpreting either of those is `Cleaner`'s job, which is what lets it propose
 * the changes to the user before any of them is applied.
 *
 * @param name Displayed in the settings. For a brand this is the brand's name and is not
 *   translated; a descriptive name is translated by the app, which looks up [id].
 * @param match The URLs this sanitizer applies to. A URL matching *any* entry is sanitized; the
 *   default matches every URL, for sanitizers which remove a tracking product's parameters from
 *   wherever they appear.
 */
data class Sanitizer(
    val id: SanitizerId,
    val name: String,
    val rules: ImmutableList<Rule>,
    val match: ImmutableList<Match> = persistentListOf(Match()),
)

/**
 * Whether this sanitizer declares short links which only their server can resolve, which makes it
 * the only kind of sanitizer that can lead to a network request.
 */
val Sanitizer.resolvesShortLinks: Boolean
    get() = resolveRules.isNotEmpty()

/** The [Rule.Resolve] rules of this sanitizer, which declare its short links. */
internal val Sanitizer.resolveRules: List<Rule.Resolve>
    get() = rules.filterIsInstance<Rule.Resolve>()
