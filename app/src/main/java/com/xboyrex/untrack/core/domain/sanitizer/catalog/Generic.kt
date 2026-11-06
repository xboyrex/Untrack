package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val EmptyParameters =
    Sanitizer(
        id = SanitizerId("empty_parameters"),
        name = "Empty Parameters",
        rules = persistentListOf(Rule.RemoveEmptyParameters),
    )

val SessionIds =
    Sanitizer(
        id = SanitizerId("session_ids"),
        name = "Session IDs",
        rules = persistentListOf(Rule.RemoveParameters("j?sessionid.*")),
    )
