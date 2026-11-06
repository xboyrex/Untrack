package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val CxAnalytics =
    Sanitizer(
        id = SanitizerId("cx_analytics"),
        name = "CX Analytics",
        rules = persistentListOf(Rule.RemoveParameters("(?:cx_|cxrecs_s|mibextid).*")),
    )
