package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Decode
import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import com.xboyrex.untrack.core.domain.sanitizer.Source
import kotlinx.collections.immutable.persistentListOf

val Jodel =
    Sanitizer(
        id = SanitizerId("jodel"),
        name = "Jodel",
        // The share link hides the real URL in a base64 encoded JSON payload. The capture
        // is needed because a Jodel link carries a second "?" inside its own query.
        rules =
            persistentListOf(
                Rule.Follow(
                    from = Source.Parameter("data"),
                    steps =
                        persistentListOf(
                            Decode.Capture("([^?]*)"),
                            Decode.PercentDecode,
                            Decode.Base64Decode,
                            Decode.JsonField("\$android_url"),
                        ),
                ),
            ),
        match = persistentListOf(Match(HostMatch.Domain("shared.jodel.com"))),
    )
