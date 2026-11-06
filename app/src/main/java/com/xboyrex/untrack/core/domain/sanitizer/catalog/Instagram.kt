package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Instagram =
    Sanitizer(
        id = SanitizerId("instagram"),
        name = "Instagram",
        rules = persistentListOf(Rule.RemoveParameters("igsh|igsi|rpxt|stkn")),
        match = persistentListOf(Match(HostMatch.Domain("instagram.com"))),
    )
