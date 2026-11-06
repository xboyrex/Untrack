package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Tiktok =
    Sanitizer(
        id = SanitizerId("tiktok"),
        name = "TikTok",
        rules = persistentListOf(Rule.RemoveParameters(".*")),
        match = persistentListOf(Match(HostMatch.Domain("tiktok.com"))),
    )

val TiktokShortLink =
    Sanitizer(
        id = SanitizerId("tiktok_short_link"),
        name = "TikTok Short Link",
        // tiktok.com and tt.site keep their short links under /t/, next to the site's own pages;
        // vm. and vt.tiktok.com serve nothing but short links. A code is never a single character,
        // which keeps a bare /t/ from passing as the code "t".
        rules = persistentListOf(Rule.Resolve("(?:/t)?/[A-Za-z0-9_-]{2,}/?")),
        match =
            persistentListOf(
                Match(HostMatch.Domain("tiktok.com"), pathPrefix = "/t/"),
                Match(HostMatch.Domain("tt.site"), pathPrefix = "/t/"),
                Match(HostMatch.Domain("vm.tiktok.com")),
                Match(HostMatch.Domain("vt.tiktok.com")),
            ),
    )
