package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val IlMessaggero =
    Sanitizer(
        id = SanitizerId("ilmessaggero"),
        name = "Il Messaggero",
        rules = persistentListOf(Rule.RemoveParameters("topic", negate = true)),
        match = persistentListOf(Match(HostMatch.Domain("archivio.ilmessaggero.it"))),
    )
