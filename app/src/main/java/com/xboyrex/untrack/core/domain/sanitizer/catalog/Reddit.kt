package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Decode
import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import com.xboyrex.untrack.core.domain.sanitizer.Source
import kotlinx.collections.immutable.persistentListOf

val Reddit =
    Sanitizer(
        id = SanitizerId("reddit"),
        name = "Reddit",
        rules = persistentListOf(Rule.RemoveParameters(".*")),
        match = persistentListOf(Match(HostMatch.Domain("reddit.com"))),
    )

val RedditOut =
    Sanitizer(
        id = SanitizerId("reddit_out"),
        name = "Reddit (out.reddit.com)",
        rules =
            persistentListOf(
                Rule.Follow(Source.Parameter("url"), persistentListOf(Decode.PercentDecode)),
            ),
        match = persistentListOf(Match(HostMatch.Domain("out.reddit.com"))),
    )

val RedditMail =
    Sanitizer(
        id = SanitizerId("reddit_mail"),
        name = "Reddit (click.redditmail.com)",
        // The target sits in the path, and the query appended to it is Reddit's
        // newsletter tracking rather than the article's own.
        rules =
            persistentListOf(
                Rule.Follow(
                    from = Source.Path,
                    steps = persistentListOf(Decode.Capture("/[^/]+/(.+)"), Decode.PercentDecode),
                    dropParameters = true,
                ),
            ),
        match = persistentListOf(Match(HostMatch.Domain("click.redditmail.com"))),
    )

val RedditShortLink =
    Sanitizer(
        id = SanitizerId("reddit_short_link"),
        name = "Reddit Short Link",
        // The share links of the Reddit app: /r/<subreddit>/s/<code>.
        rules = persistentListOf(Rule.Resolve("/r/[^/]+/s/[A-Za-z0-9_-]+/?")),
        match = persistentListOf(Match(HostMatch.Subdomains("reddit.com"))),
    )
