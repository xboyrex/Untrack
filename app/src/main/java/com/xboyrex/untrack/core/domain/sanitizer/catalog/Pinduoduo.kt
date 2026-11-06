package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Pdd =
    Sanitizer(
        id = SanitizerId("pinduoduo"),
        name = "Pinduoduo",
        rules = persistentListOf(Rule.RemoveParameters("pid|share_uin|track_id|goods_sign")),
        match =
            persistentListOf(
                Match(HostMatch.Subdomains("pinduoduo.com")),
                Match(HostMatch.Subdomains("pdd.com")),
                Match(HostMatch.Subdomains("yangkeduo.com")),
            ),
    )
