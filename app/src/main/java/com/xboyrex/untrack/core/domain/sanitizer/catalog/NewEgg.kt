package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val NewEgg =
    Sanitizer(
        id = SanitizerId("newegg"),
        name = "NewEgg",
        rules =
            persistentListOf(
                Rule.RewritePath("/.+(/p/[0-9A-Z]+).*", "$1"),
                Rule.RemoveParameters(".*"),
            ),
        match = persistentListOf(Match(HostMatch.Pattern("newegg\\.[^./?#:]+"))),
    )
