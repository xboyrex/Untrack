package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Decode
import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import com.xboyrex.untrack.core.domain.sanitizer.Source
import kotlinx.collections.immutable.persistentListOf

val AolSearch =
    Sanitizer(
        id = SanitizerId("aol_search"),
        name = "AOL Search",
        // The redirect target is embedded in the path, not in a query parameter: .../RU=<url>/...
        rules =
            persistentListOf(
                Rule.Follow(
                    Source.Path,
                    persistentListOf(Decode.Capture("RU=([^/]+)"), Decode.PercentDecode),
                ),
            ),
        match = persistentListOf(Match(HostMatch.Domain("search.aol.com"))),
    )
