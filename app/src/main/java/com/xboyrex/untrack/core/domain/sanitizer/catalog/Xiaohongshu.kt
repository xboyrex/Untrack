package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Xiaohongshu =
    Sanitizer(
        id = SanitizerId("xiaohongshu"),
        name = "Xiaohongshu",
        rules = persistentListOf(Rule.RemoveParameters(".*")),
        match =
            persistentListOf(
                Match(HostMatch.Domain("xiaohongshu.com")),
                Match(HostMatch.Domain("xhslink.com")),
            ),
    )

val XiaohongshuShortLink =
    Sanitizer(
        id = SanitizerId("xiaohongshu_short_link"),
        name = "Xiaohongshu Short Link",
        // Codes come either bare or below a single letter, such as /o/<code> or /a/<code>. A code
        // is never a single character, which keeps a bare /o/ from passing as the code "o".
        rules = persistentListOf(Rule.Resolve("(?:/[A-Za-z])?/[A-Za-z0-9_-]{2,}/?")),
        match = persistentListOf(Match(HostMatch.Pattern("xhslink\\.(?:com|cn)"))),
    )
