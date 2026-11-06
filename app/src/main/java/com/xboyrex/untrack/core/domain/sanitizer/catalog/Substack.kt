package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import com.xboyrex.untrack.core.domain.sanitizer.Source
import kotlinx.collections.immutable.persistentListOf

val Substack =
    Sanitizer(
        id = SanitizerId("substack"),
        name = "Substack",
        // "open.substack.com/pub/<publication>/…" is an indirection for opening an article in
        // the app; the publication it belongs to is named in the path, and the canonical URL is
        // that publication's own subdomain. Both rewrites find nothing on any other path.
        //
        // Everything else is tracking, most notably "r" (the reader who received the mail) and
        // "token" (a JWT identifying them). The keep-list holds the parameters which *are* the
        // article reference on "app-link" URLs, plus the "j" blob of "/redirect/<uuid>" links,
        // which is undocumented and would leave a dead link if it were dropped.
        rules =
            persistentListOf(
                Rule.RewriteHost("/pub/([^/]+)(?:/.*)?", "$1.substack.com", from = Source.Path),
                Rule.RewritePath("/pub/[^/]+(/.*)?", "$1"),
                Rule.RemoveParameters("publication_id|post_id|note_id|j", negate = true),
            ),
        match = persistentListOf(Match(HostMatch.Subdomains("substack.com"))),
    )
