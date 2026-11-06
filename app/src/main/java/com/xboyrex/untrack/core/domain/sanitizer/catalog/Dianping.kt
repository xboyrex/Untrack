package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val Dianping =
    Sanitizer(
        id = SanitizerId("dianping"),
        name = "Dianping",
        rules = persistentListOf(Rule.RemoveParameters("from|source|channel|refer|wm|c|wx.*")),
        match = persistentListOf(Match(HostMatch.Domain("dianping.com"))),
    )
