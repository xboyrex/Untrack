package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Decode
import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import com.xboyrex.untrack.core.domain.sanitizer.Source
import kotlinx.collections.immutable.persistentListOf

val LinkSynergy =
    Sanitizer(
        id = SanitizerId("linksynergy"),
        name = "LinkSynergy",
        rules =
            persistentListOf(
                Rule.Follow(Source.Parameter("murl"), persistentListOf(Decode.PercentDecode)),
            ),
        match =
            persistentListOf(
                Match(
                    HostMatch.Pattern("(?:[^.]+\\.)*linksynergy\\.[^/?#:]+"),
                    pathPrefix = "/link",
                ),
            ),
    )
