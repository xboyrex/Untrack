package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Decode
import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import com.xboyrex.untrack.core.domain.sanitizer.Source
import kotlinx.collections.immutable.persistentListOf

val Youtube =
    Sanitizer(
        id = SanitizerId("youtube"),
        name = "YouTube",
        rules =
            persistentListOf(
                Rule.RemoveParameters("(v|search_query|list|t|channel_id)", negate = true),
            ),
        match =
            persistentListOf(
                Match(HostMatch.Pattern("(?:m(?:usic)?\\.)?youtube\\.com")),
                Match(HostMatch.Domain("youtu.be")),
            ),
    )

val YoutubeRedirect =
    Sanitizer(
        id = SanitizerId("youtube_redirect"),
        name = "YouTube Redirect",
        rules =
            persistentListOf(
                Rule.Follow(Source.Parameter("q"), persistentListOf(Decode.PercentDecode)),
            ),
        match = persistentListOf(Match(HostMatch.Domain("youtube.com"), pathPrefix = "/redirect")),
    )

val YoutubeMusic =
    Sanitizer(
        id = SanitizerId("youtube_music"),
        name = "YouTube Music",
        rules = persistentListOf(Rule.RewriteHost("music\\.youtube\\.com", "youtube.com")),
        match = persistentListOf(Match(HostMatch.Domain("music.youtube.com"))),
    )

val YoutubeShortUrl =
    Sanitizer(
        id = SanitizerId("youtube_short_url"),
        name = "Youtu.be",
        // The whole target is built out of the video id in the path.
        rules =
            persistentListOf(
                Rule.Follow(
                    from = Source.Path,
                    steps =
                        persistentListOf(
                            Decode.Capture("/(.+)", "https://www.youtube.com/watch?v=$1"),
                        ),
                ),
            ),
        match = persistentListOf(Match(HostMatch.Domain("youtu.be"))),
    )
