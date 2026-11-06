package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Decode
import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import com.xboyrex.untrack.core.domain.sanitizer.Source
import kotlinx.collections.immutable.persistentListOf

val FacebookAnalytics =
    Sanitizer(
        id = SanitizerId("facebook"),
        name = "Facebook Analytics",
        rules = persistentListOf(Rule.RemoveParameters("(?:fb_|fbclid|sfnsn|cHash).*")),
    )

val Facebook =
    Sanitizer(
        id = SanitizerId("facebook_com"),
        name = "Facebook.com",
        rules = persistentListOf(Rule.RemoveParameters("(id|story_fbid)", negate = true)),
        match = persistentListOf(Match(HostMatch.Pattern("(?:m\\.)?facebook\\.com"))),
    )

val FacebookRedirect =
    Sanitizer(
        id = SanitizerId("facebook_redirect"),
        name = "Facebook Redirect",
        // Facebook interstitials such as /l.php (outgoing link warning) and /flx/warn/ (external
        // link warning) hide the actual target, percent-encoded, in the "u" parameter. Once
        // extracted, FacebookAnalytics strips the fbclid it carries.
        rules =
            persistentListOf(
                Rule.Follow(Source.Parameter("u"), persistentListOf(Decode.PercentDecode)),
            ),
        match =
            persistentListOf(
                Match(HostMatch.Subdomains("facebook.com"), pathPrefix = "/l.php"),
                Match(HostMatch.Subdomains("facebook.com"), pathPrefix = "/flx/warn"),
            ),
    )
