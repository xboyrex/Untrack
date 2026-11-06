package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Decode
import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import com.xboyrex.untrack.core.domain.sanitizer.Source
import kotlinx.collections.immutable.persistentListOf

val GeoRiot =
    Sanitizer(
        id = SanitizerId("georiot"),
        name = "GeoRiot",
        rules =
            persistentListOf(
                Rule.Follow(Source.Parameter("GR_URL"), persistentListOf(Decode.PercentDecode)),
            ),
        match =
            persistentListOf(
                Match(HostMatch.Pattern("target\\.georiot\\.[^/?#:]+"), pathPrefix = "/Proxy.ashx"),
            ),
    )
