package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Feishu =
    Sanitizer(
        id = SanitizerId("feishu"),
        name = "Feishu",
        rules = persistentListOf(Rule.RemoveParameters("from|scene|channel|source|refer")),
        match =
            persistentListOf(
                Match(HostMatch.Subdomains("feishu.cn")),
                Match(HostMatch.Subdomains("feishu.net")),
            ),
    )
