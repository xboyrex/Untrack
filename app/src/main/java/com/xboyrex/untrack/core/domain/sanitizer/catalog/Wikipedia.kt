package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Wikipedia =
    Sanitizer(
        id = SanitizerId("wikipedia"),
        name = "Wikipedia",
        rules = persistentListOf(Rule.RemoveParameters("wprov")),
        match = persistentListOf(Match(HostMatch.Subdomains("wikipedia.org"))),
    )
