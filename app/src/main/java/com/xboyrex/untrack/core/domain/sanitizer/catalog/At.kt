package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val AtAnalytics =
    Sanitizer(
        id = SanitizerId("at_analytics"),
        name = "AT Analytics",
        rules = persistentListOf(Rule.RemoveParameters("at_.*")),
    )
