package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Meituan =
    Sanitizer(
        id = SanitizerId("meituan"),
        name = "Meituan",
        rules = persistentListOf(Rule.RemoveParameters("from|source|channel|refer|wm|c|wx.*")),
        match =
            persistentListOf(
                Match(HostMatch.Domain("meituan.com")),
                Match(HostMatch.Domain("meituan.cn")),
                Match(HostMatch.Domain("meituan.net")),
            ),
    )
