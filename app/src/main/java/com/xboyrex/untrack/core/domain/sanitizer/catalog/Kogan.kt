package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Kogan =
    Sanitizer(
        id = SanitizerId("kogan"),
        name = "Kogan",
        rules = persistentListOf(Rule.RemoveParameters(".*")),
        match = persistentListOf(Match(HostMatch.Domain("kogan.com"))),
    )
