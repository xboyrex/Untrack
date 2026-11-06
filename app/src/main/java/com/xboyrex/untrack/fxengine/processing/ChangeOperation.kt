package com.xboyrex.untrack.fxengine.processing

enum class ChangeOperationType {
    PARAMETERS_REMOVED,
    REDIRECT_EXTRACTED,
    HOST_CONVERTED,
    CUSTOM_RULE_APPLIED,
    /**
     * A cleaner changed a same-host URL without removing identifiable query
     * parameter names, such as canonicalising an Amazon product link.
     */
    URL_CANONICALIZED
}

/**
 * Non-sensitive description of one URL-processing change.
 *
 * Parameter values and complete URLs must never be stored here.
 */
data class ChangeOperation(
    val type: ChangeOperationType,
    /** Cleaner/rule display name, e.g. "Instagram", "Redirect Unwrapper", or a custom rule name. */
    val source: String,
    /** Parameter names only — never values. Only for [ChangeOperationType.PARAMETERS_REMOVED]. */
    val parameterNames: List<String> = emptyList(),
    /** Host names only — never full URLs. For redirect and host-conversion operations. */
    val fromHost: String? = null,
    val toHost: String? = null
)
