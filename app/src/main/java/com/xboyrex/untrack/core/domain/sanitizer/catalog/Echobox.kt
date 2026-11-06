package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Echobox =
    Sanitizer(
        id = SanitizerId("echobox"),
        name = "Echobox",
        rules = persistentListOf(Rule.RemoveFragment("Echobox=.*")),
    )
