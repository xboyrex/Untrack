package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Taobao =
    Sanitizer(
        id = SanitizerId("taobao"),
        name = "Taobao",
        rules =
            persistentListOf(
                Rule.RemoveParameters("smid|ut_ma|track_id|spm|share_crt_v|tbkt|isg|tk"),
            ),
        match =
            persistentListOf(
                Match(HostMatch.Subdomains("taobao.com")),
                Match(HostMatch.Subdomains("tmall.com")),
                Match(HostMatch.Subdomains("tb.cn")),
            ),
    )
