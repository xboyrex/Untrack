package com.xboyrex.untrack.fxengine.cleaners.model

import com.xboyrex.untrack.fxengine.processing.ChangeOperation
import com.xboyrex.untrack.fxengine.processing.PipelineStatus

/**
 * Result of URL processing with non-sensitive details about what was changed.
 */
data class ProcessingResult(
    val originalUrl: String,
    val cleanedUrl: String,
    val operations: List<ChangeOperation>,
    val totalPasses: Int,
    val wasModified: Boolean = originalUrl != cleanedUrl,
    val status: PipelineStatus = PipelineStatus.COMPLETE,
)
