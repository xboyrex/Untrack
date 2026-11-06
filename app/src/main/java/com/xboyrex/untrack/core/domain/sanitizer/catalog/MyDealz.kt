package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.HostMatch
import com.xboyrex.untrack.core.domain.sanitizer.Match
import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val MyDealzParameters =
    Sanitizer(
        id = SanitizerId("mydealz_parameters"),
        name = "MyDealz Parameters",
        rules = persistentListOf(Rule.RemoveParameters(".*")),
        match =
            persistentListOf(
                Match(HostMatch.Subdomains("mydealz.de")),
                Match(HostMatch.Subdomains("chollometro.com")),
                Match(HostMatch.Subdomains("dealabs.com")),
                Match(HostMatch.Subdomains("desidime.com")),
                Match(HostMatch.Subdomains("hotukdeals.com")),
                Match(HostMatch.Subdomains("nl.pepper.com")),
                Match(HostMatch.Subdomains("pepper.it")),
                Match(HostMatch.Subdomains("pepper.pl")),
                Match(HostMatch.Subdomains("pepper.ru")),
                Match(HostMatch.Subdomains("promodescuentos.com")),
                Match(HostMatch.Subdomains("pelando.com.br")),
                Match(HostMatch.Subdomains("preisjaeger.at")),
            ),
    )

val MyDealzRedirects =
    Sanitizer(
        id = SanitizerId("mydealz_redirects"),
        name = "MyDealz Redirects",
        rules = persistentListOf(Rule.RewritePath("/share-deal-from-app/(.+)", "/deals/a-$1")),
        match =
            persistentListOf(
                Match(HostMatch.Subdomains("mydealz.de")),
                Match(HostMatch.Subdomains("chollometro.com")),
                Match(HostMatch.Subdomains("dealabs.com")),
                Match(HostMatch.Subdomains("desidime.com")),
                Match(HostMatch.Subdomains("hotukdeals.com")),
                Match(HostMatch.Subdomains("nl.pepper.com")),
                Match(HostMatch.Subdomains("pepper.it")),
                Match(HostMatch.Subdomains("pepper.pl")),
                Match(HostMatch.Subdomains("pepper.ru")),
                Match(HostMatch.Subdomains("promodescuentos.com")),
                Match(HostMatch.Subdomains("pelando.com.br")),
                Match(HostMatch.Subdomains("preisjaeger.at")),
            ),
    )
