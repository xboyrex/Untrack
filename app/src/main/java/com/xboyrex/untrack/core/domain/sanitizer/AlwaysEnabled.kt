package com.xboyrex.untrack.core.domain.sanitizer

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * A repository in which every sanitizer is enabled and nothing can be turned off.
 *
 * The default for a caller which has no place to store the user's preferences, such as a command
 * line front end. The app replaces it with a repository backed by its settings.
 */
object AlwaysEnabled : SanitizerRepository {

    override val state:
        Flow<kotlinx.collections.immutable.ImmutableList<SanitizerRepository.SanitizerState>> =
        flowOf(persistentListOf())

    override suspend fun isEnabled(id: SanitizerId) = true

    override suspend fun setEnabled(id: SanitizerId, enabled: Boolean) = Unit
}
