package com.xboyrex.untrack

import java.util.Locale

enum class FrontendPlatform(
    val id: String,
    val title: String,
    val monogram: String,
    val sourceDomains: List<String>,
) {
    X("x", "X / Twitter", "X", listOf("x.com", "twitter.com")),
    INSTAGRAM("instagram", "Instagram", "IG", listOf("instagram.com")),
    TIKTOK("tiktok", "TikTok", "TT", listOf("tiktok.com")),
    REDDIT("reddit", "Reddit", "RD", listOf("reddit.com", "redd.it")),
    BLUESKY("bluesky", "Bluesky", "BS", listOf("bsky.app")),
    YOUTUBE("youtube", "YouTube", "YT", listOf("youtube.com", "youtu.be")),
    THREADS("threads", "Threads", "TH", listOf("threads.net", "threads.com")),
    PINTEREST("pinterest", "Pinterest", "PT", listOf("pinterest.com")),
    FACEBOOK("facebook", "Facebook", "FB", listOf("facebook.com", "fb.com"));
}

enum class FrontendRole(val badge: String) {
    EMBED("Rich Embed"),
    READER("Privacy Reader"),
    AUTOMATIC("Smart Router"),
    CUSTOM("Custom Proxy")
}

data class FrontendTarget(
    val id: String,
    val platform: FrontendPlatform,
    val domain: String,
    val pathPrefix: String? = null,
    val role: FrontendRole,
    val description: String,
    val isCustom: Boolean = false
)

object FrontendCatalog {

    val builtInTargets: List<FrontendTarget> = listOf(
        // X / Twitter
        FrontendTarget("x_fixupx", FrontendPlatform.X, "fixupx.com", role = FrontendRole.EMBED, description = "Rich media embeds for Discord, Telegram & WhatsApp"),
        FrontendTarget("x_fxtwitter", FrontendPlatform.X, "fxtwitter.com", role = FrontendRole.EMBED, description = "Classic FxTwitter media embed service"),
        FrontendTarget("x_vxtwitter", FrontendPlatform.X, "vxtwitter.com", role = FrontendRole.EMBED, description = "VxTwitter lightweight video & image embeds"),
        FrontendTarget("x_xcancel", FrontendPlatform.X, "xcancel.com", role = FrontendRole.READER, description = "Privacy web reader without X login wall"),
        FrontendTarget("x_nitter_net", FrontendPlatform.X, "nitter.net", role = FrontendRole.READER, description = "Official Nitter privacy frontend instance"),
        FrontendTarget("x_twitterviewer", FrontendPlatform.X, "twitterviewer.net", role = FrontendRole.READER, description = "Clean web reader for posts and threads"),
        FrontendTarget("x_nitter_catsarch", FrontendPlatform.X, "nitter.catsarch.com", role = FrontendRole.READER, description = "CatsArch Nitter community instance"),
        FrontendTarget("x_nitter_tiekoetter", FrontendPlatform.X, "nitter.tiekoetter.com", role = FrontendRole.READER, description = "Tiekoetter Nitter community instance"),
        FrontendTarget("x_nitter_kareem", FrontendPlatform.X, "nitter.kareem.one", role = FrontendRole.READER, description = "Kareem Nitter community instance"),
        FrontendTarget("x_nitter_privacyredirect", FrontendPlatform.X, "nitter.privacyredirect.com", role = FrontendRole.READER, description = "PrivacyRedirect Nitter mirror"),
        FrontendTarget("x_nuku_trabun", FrontendPlatform.X, "nuku.trabun.org", role = FrontendRole.READER, description = "Trabun Nuku privacy reader"),
        FrontendTarget("x_twiiit", FrontendPlatform.X, "twiiit.com", role = FrontendRole.AUTOMATIC, description = "Auto-routes to the fastest healthy Nitter instance"),
        FrontendTarget("x_farside", FrontendPlatform.X, "farside.link", pathPrefix = "/nitter", role = FrontendRole.AUTOMATIC, description = "Farside load-balanced Nitter router"),

        // Instagram
        FrontendTarget("ig_toinstagram", FrontendPlatform.INSTAGRAM, "toinstagram.com", role = FrontendRole.EMBED, description = "Embeds Reels, posts & descriptions cleanly"),
        FrontendTarget("ig_adamlikes", FrontendPlatform.INSTAGRAM, "adamlikes.men", role = FrontendRole.EMBED, description = "Fast Instagram media & caption embed proxy"),
        FrontendTarget("ig_instagram7", FrontendPlatform.INSTAGRAM, "instagram7.com", role = FrontendRole.EMBED, description = "Direct media embed proxy for Instagram"),
        FrontendTarget("ig_kittygram", FrontendPlatform.INSTAGRAM, "kittygr.am", role = FrontendRole.READER, description = "Privacy-friendly Kittygram web viewer"),
        FrontendTarget("ig_kg_meowing", FrontendPlatform.INSTAGRAM, "kg.meowing.de", role = FrontendRole.READER, description = "Meowing Kittygram privacy reader"),
        FrontendTarget("ig_kittygram_kareem", FrontendPlatform.INSTAGRAM, "kittygram.kareem.one", role = FrontendRole.READER, description = "Kareem Kittygram privacy reader"),

        // TikTok
        FrontendTarget("tk_tnktok", FrontendPlatform.TIKTOK, "tnktok.com", role = FrontendRole.EMBED, description = "Embeds TikTok videos & multi-photo slideshows"),
        FrontendTarget("tk_tfxktok", FrontendPlatform.TIKTOK, "tfxktok.com", role = FrontendRole.EMBED, description = "Fast TikTok video & stats embed proxy"),
        FrontendTarget("tk_tiktokez", FrontendPlatform.TIKTOK, "tiktokez.com", role = FrontendRole.EMBED, description = "Clean TikTok inline video embedder"),
        FrontendTarget("tk_kktiktok", FrontendPlatform.TIKTOK, "kktiktok.com", role = FrontendRole.EMBED, description = "Lightweight video-only TikTok embed proxy"),

        // Reddit
        FrontendTarget("rd_redlib_catsarch", FrontendPlatform.REDDIT, "redlib.catsarch.com", role = FrontendRole.READER, description = "Fast Redlib privacy frontend for Reddit"),
        FrontendTarget("rd_redlib_privacyredirect", FrontendPlatform.REDDIT, "redlib.privacyredirect.com", role = FrontendRole.READER, description = "PrivacyRedirect Redlib instance"),
        FrontendTarget("rd_redlib_nadeko", FrontendPlatform.REDDIT, "redlib.nadeko.net", role = FrontendRole.READER, description = "Nadeko Redlib private reader"),
        FrontendTarget("rd_redlib_privadency", FrontendPlatform.REDDIT, "redlib.privadency.com", role = FrontendRole.READER, description = "Privadency Redlib instance"),
        FrontendTarget("rd_safereddit", FrontendPlatform.REDDIT, "safereddit.com", role = FrontendRole.READER, description = "SafeReddit distraction-free web viewer"),
        FrontendTarget("rd_red_artemislena", FrontendPlatform.REDDIT, "red.artemislena.eu", role = FrontendRole.READER, description = "Artemislena Redlib community instance"),
        FrontendTarget("rd_redlib_r4fo", FrontendPlatform.REDDIT, "redlib.r4fo.com", role = FrontendRole.READER, description = "R4FO Redlib community instance"),
        FrontendTarget("rd_redlib_cow", FrontendPlatform.REDDIT, "redlib.cow.rip", role = FrontendRole.READER, description = "Cow.rip Redlib community instance"),
        FrontendTarget("rd_old_reddit", FrontendPlatform.REDDIT, "old.reddit.com", role = FrontendRole.READER, description = "Classic lightweight Old Reddit interface"),

        // Bluesky
        FrontendTarget("bs_fxbsky", FrontendPlatform.BLUESKY, "fxbsky.app", role = FrontendRole.EMBED, description = "Rich Bluesky post & video embedder"),
        FrontendTarget("bs_skylib_coffee", FrontendPlatform.BLUESKY, "skylib.coffee", role = FrontendRole.READER, description = "Skylib private Bluesky web reader"),
        FrontendTarget("bs_skylib_catsarch", FrontendPlatform.BLUESKY, "skylib.catsarch.com", role = FrontendRole.READER, description = "CatsArch Skylib instance"),

        // YouTube
        FrontendTarget("yt_inv_nadeko", FrontendPlatform.YOUTUBE, "inv.nadeko.net", role = FrontendRole.READER, description = "Invidious ad-free privacy frontend"),
        FrontendTarget("yt_invidious_nerdvpn", FrontendPlatform.YOUTUBE, "invidious.nerdvpn.de", role = FrontendRole.READER, description = "NerdVPN Invidious instance"),

        // Threads
        FrontendTarget("th_shoelace", FrontendPlatform.THREADS, "shoelace.mint.lgbt", role = FrontendRole.READER, description = "Shoelace privacy reader for Threads"),

        // Pinterest
        FrontendTarget("pt_bunk", FrontendPlatform.PINTEREST, "pinterest.bunk.im", role = FrontendRole.READER, description = "Bunk privacy frontend for Pinterest pins"),
    )

    fun targetsFor(platform: FrontendPlatform, customDomains: List<String> = emptyList()): List<FrontendTarget> {
        val builtIn = builtInTargets.filter { it.platform == platform }
        val custom = customDomains.map { domain ->
            FrontendTarget(
                id = "custom_${platform.id}_$domain",
                platform = platform,
                domain = domain,
                role = FrontendRole.CUSTOM,
                description = "Custom user proxy domain",
                isCustom = true
            )
        }
        return builtIn + custom
    }

    fun defaultTargetFor(platform: FrontendPlatform): FrontendTarget? =
        builtInTargets.firstOrNull { it.platform == platform }

    fun detectPlatform(url: ParsedUrl, customDomainsMap: Map<FrontendPlatform, List<String>>): FrontendPlatform? {
        val h = url.asciiHost
        for (platform in FrontendPlatform.entries) {
            val allDomains = platform.sourceDomains +
                builtInTargets.filter { it.platform == platform }.map { it.domain } +
                customDomainsMap[platform].orEmpty()
            if (allDomains.any { ParsedUrl.hostMatchesDomain(h, it) }) {
                return platform
            }
        }
        return null
    }

    fun convertUrl(
        url: ParsedUrl,
        convertToAlternative: Boolean,
        enabledPlatforms: Set<FrontendPlatform>,
        selectedDomains: Map<FrontendPlatform, String>,
        customDomains: Map<FrontendPlatform, List<String>>
    ): Pair<ParsedUrl, FrontendPlatform?> {
        val platform = detectPlatform(url, customDomains) ?: return url to null
        if (platform !in enabledPlatforms) return url to null

        if (!convertToAlternative) {
            val canonical = platform.sourceDomains.first()
            val cleanedPath = if (url.asciiHost == "farside.link" && url.path.startsWith("/nitter")) {
                url.path.removePrefix("/nitter").ifEmpty { "/" }
            } else {
                url.path
            }
            if (url.asciiHost == canonical && cleanedPath == url.path) return url to null
            return url.copy(host = canonical, path = cleanedPath) to platform
        }

        val targetDomain = selectedDomains[platform]
            ?: defaultTargetFor(platform)?.domain
            ?: return url to null

        if (targetDomain.isBlank()) return url to null

        val targetSpec = builtInTargets.find { it.platform == platform && it.domain.equals(targetDomain, ignoreCase = true) }

        if (platform == FrontendPlatform.YOUTUBE && ParsedUrl.hostMatchesDomain(url.asciiHost, "youtu.be")) {
            val videoId = url.path.trim('/').takeIf { it.isNotBlank() } ?: return url to null
            val newParams = listOf(ParsedParameter("v", videoId)) + url.parameters
            return url.copy(host = targetDomain, path = "/watch", parameters = newParams) to platform
        }

        var cleanPath = if (url.asciiHost == "farside.link" && url.path.startsWith("/nitter")) {
            url.path.removePrefix("/nitter").ifEmpty { "/" }
        } else {
            url.path
        }

        val prefix = targetSpec?.pathPrefix
        if (!prefix.isNullOrBlank() && !cleanPath.startsWith(prefix)) {
            cleanPath = if (cleanPath.startsWith("/")) "$prefix$cleanPath" else "$prefix/$cleanPath"
        }

        if (url.asciiHost.equals(targetDomain, ignoreCase = true) && cleanPath == url.path) {
            return url to null
        }

        return url.copy(host = targetDomain.lowercase(Locale.ROOT), path = cleanPath) to platform
    }
}
