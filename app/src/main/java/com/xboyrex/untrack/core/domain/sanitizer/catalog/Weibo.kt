package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Weibo =
    Sanitizer(
        id = SanitizerId("weibo"),
        name = "Weibo",
        rules = persistentListOf(Rule.RemoveParameters("from|refer|share_token")),
        match =
            persistentListOf(
                Match(HostMatch.Domain("weibo.com")),
                Match(HostMatch.Domain("m.weibo.cn")),
            ),
    )
