package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import com.xboyrex.untrack.core.domain.sanitizer.Source
import kotlinx.collections.immutable.persistentListOf

val Amazon =
    Sanitizer(
        id = SanitizerId("amazon2"),
        name = "Amazon",
        rules = persistentListOf(Rule.RemoveParameters("ref_?")),
        match = persistentListOf(Match(HostMatch.Pattern("amazon\\.[^/?#:]+"), pathPrefix = "/")),
    )

val AmazonProduct =
    Sanitizer(
        id = SanitizerId("amazon"),
        name = "Amazon Products",
        rules =
            persistentListOf(
                Rule.RewritePath(".*/(?:dp?|gp/product)/([^/?&]*).*", "/dp/$1/"),
                Rule.RemoveParameters(".*"),
            ),
        match = persistentListOf(Match(HostMatch.Pattern("amazon\\.[^/?#:]+"))),
    )

val AmazonSponsoredProduct =
    Sanitizer(
        id = SanitizerId("amazonSponsoredProduct"),
        name = "Amazon Sponsored Products",
        // The product page is not the URL itself but hidden, percent-encoded, in its "url"
        // parameter: /sspa/click?...&url=%2F...%2Fdp%2F<asin>%2F...
        rules =
            persistentListOf(
                Rule.RewritePath(
                    pattern = "(?i).*%2F(?:dp?|gp%2Fproduct)%2F([^%]*).*",
                    replacement = "/dp/$1/",
                    from = Source.Parameter("url"),
                ),
                Rule.RemoveParameters(".*"),
            ),
        match =
            persistentListOf(
                Match(HostMatch.Pattern("amazon\\.[^/?#:]+"), pathPrefix = "/sspa/click"),
            ),
    )

val AmazonShortLink =
    Sanitizer(
        id = SanitizerId("amazon_short_link"),
        name = "Amazon Short Link",
        // A code is never a single character, which keeps a bare /d/ from passing as the code "d".
        rules = persistentListOf(Rule.Resolve("(?:/d)?/[A-Za-z0-9_-]{2,}/?")),
        match =
            persistentListOf(
                Match(HostMatch.Domain("a.co"), pathPrefix = "/d/"),
                Match(HostMatch.Domain("amzn.to")),
                Match(HostMatch.Domain("amzn.eu")),
            ),
    )
