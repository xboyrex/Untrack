package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val SalesforceParameters =
    Sanitizer(
        id = SanitizerId("salesforce"),
        name = "Salesforce",
        // "utm_*" is deliberately not included here: it is generic Google Analytics tracking,
        // already covered by GoogleAnalytics, not something specific to Salesforce.
        rules = persistentListOf(Rule.RemoveParameters("sfmc_.*")),
    )
