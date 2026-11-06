package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Decode
import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import com.xboyrex.untrack.core.domain.sanitizer.Source
import kotlinx.collections.immutable.persistentListOf

val GoogleAnalytics =
    Sanitizer(
        id = SanitizerId("google_analytics"),
        name = "Google Analytics",
        rules = persistentListOf(Rule.RemoveParameters("(?:ga_|utm_|gclid|gad_).*")),
    )

val GoogleStore =
    Sanitizer(
        id = SanitizerId("google_play_store"),
        name = "Google Play Store",
        rules = persistentListOf(Rule.RemoveParameters("hl|selections")),
        match = persistentListOf(Match(HostMatch.Domain("store.google.com"))),
    )

val GoogleAds =
    Sanitizer(
        id = SanitizerId("google_ad_services"),
        name = "Google Ads",
        rules =
            persistentListOf(
                Rule.Follow(Source.Parameter("adurl"), persistentListOf(Decode.PercentDecode)),
            ),
        match = persistentListOf(Match(HostMatch.Domain("googleadservices.com"))),
    )

val GoogleSearch =
    Sanitizer(
        id = SanitizerId("google_search"),
        name = "Google Search",
        rules =
            persistentListOf(
                Rule.Follow(Source.Parameter("url|q"), persistentListOf(Decode.PercentDecode)),
            ),
        match =
            persistentListOf(Match(HostMatch.Pattern("google\\.[^/?#:]+"), pathPrefix = "/url")),
    )

val GoogleMaps =
    Sanitizer(
        id = SanitizerId("google_maps"),
        name = "Google Maps",
        rules =
            persistentListOf(
                Rule.RewriteHost(".*", "www.google.com"),
                Rule.RewritePath(
                    ".*(@-?\\d+\\.\\d+,-?\\d+\\.\\d+,\\d+(?:\\.\\d+)?z).*",
                    "/maps/$1",
                ),
                Rule.RemoveParameters(".*"),
                Rule.RemoveFragment(),
            ),
        match =
            persistentListOf(
                Match(HostMatch.Pattern("google\\.[^/?#:]+"), pathPrefix = "/maps"),
                Match(HostMatch.Domain("maps.google.com")),
            ),
    )

val GoogleMapsShortLink =
    Sanitizer(
        id = SanitizerId("google_maps_short_link"),
        name = "Google Maps Short Link",
        rules = persistentListOf(Rule.Resolve("/[A-Za-z0-9_-]+/?")),
        match = persistentListOf(Match(HostMatch.Domain("maps.app.goo.gl"))),
    )

/**
 * `shem`, which Google appends to the target of a link shared from Search or Discover, such as
 * `?shem=dsdf,sharefoc,agadiscoversdl,,sh/x/discover/m1/4`. It ends up on every site, so this
 * applies to every URL.
 */
val GoogleShare =
    Sanitizer(
        id = SanitizerId("google_share"),
        name = "Google Share",
        rules = persistentListOf(Rule.RemoveParameters("shem")),
    )

/**
 * `share.google/<code>`, which redirects to `google.com/share.google?q=<code>` first and only from
 * there to the target. That second short link carries its code in the query.
 */
val GoogleShortLink =
    Sanitizer(
        id = SanitizerId("google_short_link"),
        name = "Google Short Link",
        rules =
            persistentListOf(
                Rule.Resolve("/[A-Za-z0-9_-]{2,}/?"),
                Rule.Resolve("/share\\.google", parameters = "q"),
            ),
        match =
            persistentListOf(
                Match(HostMatch.Domain("share.google")),
                Match(HostMatch.Domain("google.com"), pathPrefix = "/share.google"),
            ),
    )
