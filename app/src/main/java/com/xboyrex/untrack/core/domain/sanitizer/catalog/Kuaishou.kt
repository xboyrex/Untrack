package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Kuaishou =
    Sanitizer(
        id = SanitizerId("kuaishou"),
        name = "Kuaishou",
        rules = persistentListOf(Rule.RemoveParameters("share|userId|photoId")),
        match =
            persistentListOf(
                Match(HostMatch.Domain("kuaishou.com")),
                Match(HostMatch.Domain("v.kuaishou.com")),
            ),
    )
