package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Jd =
    Sanitizer(
        id = SanitizerId("jd"),
        name = "JD.com",
        rules = persistentListOf(Rule.RemoveParameters("share|jkl")),
        match =
            persistentListOf(
                Match(HostMatch.Subdomains("jd.com")),
                Match(HostMatch.Subdomains("3.cn")),
            ),
    )
