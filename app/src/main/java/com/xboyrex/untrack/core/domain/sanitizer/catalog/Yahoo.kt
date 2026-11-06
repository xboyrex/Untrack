package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Decode
import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import com.xboyrex.untrack.core.domain.sanitizer.Source
import kotlinx.collections.immutable.persistentListOf

val YahooReferrer =
    Sanitizer(
        id = SanitizerId("yahooReferrer"),
        name = "Yahoo Referrer",
        rules =
            persistentListOf(Rule.RemoveParameters("guccounter|guce_referrer|guce_referrer_sig")),
    )

val YahooSearch =
    Sanitizer(
        id = SanitizerId("yahoo_search"),
        name = "Yahoo Search",
        // A search results page (/search?p=...) only keeps its query; every other URL under this
        // host is a redirect whose target is embedded in the path as .../RU=<url>/... — the two
        // rules are mutually exclusive in practice, each finding nothing to do on the other's URLs.
        rules =
            persistentListOf(
                Rule.RemoveParameters("p", negate = true),
                Rule.Follow(
                    Source.Path,
                    persistentListOf(Decode.Capture("RU=([^/]+)"), Decode.PercentDecode),
                ),
            ),
        match = persistentListOf(Match(HostMatch.Subdomains("search.yahoo.com"))),
    )
