package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Yandex =
    Sanitizer(
        id = SanitizerId("yandex"),
        name = "Yandex",
        rules = persistentListOf(Rule.RemoveParameters("text", negate = true)),
        match =
            persistentListOf(
                Match(HostMatch.Domain("yandex.com")),
                Match(HostMatch.Domain("ya.ru")),
            ),
    )
