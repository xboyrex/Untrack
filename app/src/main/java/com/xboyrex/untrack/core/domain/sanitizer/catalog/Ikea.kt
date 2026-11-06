package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Ikea =
    Sanitizer(
        id = SanitizerId("ikea"),
        name = "IKEA",
        rules = persistentListOf(Rule.RemoveParameters(".*")),
        match = persistentListOf(Match(HostMatch.Domain("ikea.com"))),
    )
