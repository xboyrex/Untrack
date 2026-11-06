package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Lazada =
    Sanitizer(
        id = SanitizerId("lazada"),
        name = "Lazada",
        rules = persistentListOf(Rule.RemoveParameters(".*")),
        match = persistentListOf(Match(HostMatch.Domain("lazada.com.my"))),
    )

val LazadaShortLink =
    Sanitizer(
        id = SanitizerId("lazada_short_link"),
        name = "Lazada Short Link",
        // Every country Lazada operates in has a short link host of its own, s.lazada.com.my for
        // Malaysia for example. They are listed rather than matched by `[a-z.]+`, which would also
        // accept s.lazada.com.evil.com.
        rules = persistentListOf(Rule.Resolve("/[^/]+/?")),
        match =
            persistentListOf(
                Match(
                    HostMatch.Pattern("s\\.lazada\\.(?:com\\.my|sg|co\\.th|com\\.ph|co\\.id|vn)"),
                ),
            ),
    )
