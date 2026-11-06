package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Wechat =
    Sanitizer(
        id = SanitizerId("wechat"),
        name = "WeChat",
        rules = persistentListOf(Rule.RemoveParameters("__biz|mid|idx|sn|scene|wx_header")),
        match =
            persistentListOf(
                Match(HostMatch.Domain("weixin.qq.com")),
                Match(HostMatch.Domain("url.cn")),
            ),
    )
