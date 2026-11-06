package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Douyin =
    Sanitizer(
        id = SanitizerId("douyin"),
        name = "Douyin",
        rules =
            persistentListOf(
                Rule.RewritePath("(?i)(.*?)(?:/Ipd:|/lpD:|/9@4\\.com|/\\d+/\\d+).*", "$1"),
                Rule.RemoveParameters(".*"),
            ),
        match =
            persistentListOf(
                Match(HostMatch.Domain("douyin.com")),
                Match(HostMatch.Domain("v.douyin.com")),
                Match(HostMatch.Domain("iesdouyin.com")),
            ),
    )
