package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Webtrekk =
    Sanitizer(
        id = SanitizerId("webtrekk"),
        name = "Webtrekk",
        rules = persistentListOf(Rule.RemoveParameters("wt_.*")),
    )
