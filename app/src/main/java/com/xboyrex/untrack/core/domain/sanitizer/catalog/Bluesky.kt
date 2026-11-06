package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Decode
import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import com.xboyrex.untrack.core.domain.sanitizer.Source
import kotlinx.collections.immutable.persistentListOf

val BlueskyRedirect =
    Sanitizer(
        id = SanitizerId("bluesky_redirect"),
        name = "Bluesky Redirect",
        rules =
            persistentListOf(
                Rule.Follow(Source.Parameter("u"), persistentListOf(Decode.PercentDecode)),
            ),
        match = persistentListOf(Match(HostMatch.Domain("go.bsky.app"), pathPrefix = "/redirect")),
    )
