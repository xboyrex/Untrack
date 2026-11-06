package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val CarGurus =
    Sanitizer(
        id = SanitizerId("cargurus"),
        name = "CarGurus",
        rules =
            persistentListOf(
                Rule.RemoveParameters(
                    "(listingId|entitySelectingHelper\\.selectedEntity)",
                    negate = true,
                ),
            ),
        match = persistentListOf(Match(HostMatch.Domain("cargurus.co.uk"), pathPrefix = "/")),
    )
