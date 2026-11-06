package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Dingtalk =
    Sanitizer(
        id = SanitizerId("dingtalk"),
        name = "DingTalk",
        rules = persistentListOf(Rule.RemoveParameters("from|scene|channel|source|refer")),
        match =
            persistentListOf(
                Match(HostMatch.Subdomains("dingtalk.com")),
                Match(HostMatch.Subdomains("dingtalk.cn")),
            ),
    )
