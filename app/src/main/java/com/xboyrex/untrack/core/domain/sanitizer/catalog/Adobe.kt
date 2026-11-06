package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val AdobeMarketoEngage =
    Sanitizer(
        id = SanitizerId("adobe_marketo_engage"),
        name = "Adobe Marketo Engage",
        rules = persistentListOf(Rule.RemoveParameters("mkt_.*")),
    )
