package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val ElFinanciero =
    Sanitizer(
        id = SanitizerId("elfinanciero"),
        name = "ElFinanciero",
        rules = persistentListOf(Rule.RemoveParameters("outputType")),
        match = persistentListOf(Match(HostMatch.Domain("elfinanciero.com.mx"))),
    )
