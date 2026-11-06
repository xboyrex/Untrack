package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Zhihu =
    Sanitizer(
        id = SanitizerId("zhihu"),
        name = "Zhihu",
        rules = persistentListOf(Rule.RemoveParameters("share_redirect|share_code")),
        match = persistentListOf(Match(HostMatch.Domain("zhihu.com"))),
    )
