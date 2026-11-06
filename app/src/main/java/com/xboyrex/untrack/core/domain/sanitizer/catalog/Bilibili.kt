package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Bilibili =
    Sanitizer(
        id = SanitizerId("bilibili"),
        name = "Bilibili",
        rules =
            persistentListOf(Rule.RemoveParameters("vd_source|seid|from|share_source|copy_link")),
        match = persistentListOf(Match(HostMatch.Domain("bilibili.com"))),
    )
