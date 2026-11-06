package com.xboyrex.untrack.core.domain.sanitizer

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

/** Repository for enabled and disabling [Sanitizers][Sanitizer]. */
interface SanitizerRepository {

    data class SanitizerState(val id: SanitizerId, val enabled: Boolean)

    val state: Flow<ImmutableList<SanitizerState>>

    suspend fun isEnabled(id: SanitizerId): Boolean

    suspend fun setEnabled(id: SanitizerId, enabled: Boolean)
}
