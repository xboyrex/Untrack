package com.xboyrex.untrack

enum class SanitizerCategory(val label: String) {
    GLOBAL("Global Trackers"),
    SOCIAL("Social & Messaging"),
    SHOPPING("Shopping & Deals"),
    VIDEO_MUSIC("Video & Music"),
    SEARCH_REDIRECT("Search & Redirects"),
    NEWS_TECH("News & Media"),
    REGIONAL("Regional & Super-Apps")
}

sealed interface HostMatcher {
    data object AnyHost : HostMatcher
    data class ExactDomain(val domain: String) : HostMatcher
    data class Subdomains(val domain: String) : HostMatcher
    data class RegexHost(val pattern: String) : HostMatcher

    fun matches(host: String): Boolean {
        val bare = host.removePrefix("www.").removePrefix("WWW.").lowercase()
        return when (this) {
            AnyHost -> true
            is ExactDomain -> bare.equals(domain, ignoreCase = true)
            is Subdomains -> bare.equals(domain, ignoreCase = true) || bare.endsWith(".$domain", ignoreCase = true)
            is RegexHost -> Regex(pattern, RegexOption.IGNORE_CASE).matches(bare)
        }
    }
}

data class SanitizerMatch(
    val host: HostMatcher = HostMatcher.AnyHost,
    val pathPrefix: String? = null,
) {
    fun matches(url: ParsedUrl): Boolean {
        if (!host.matches(url.host)) return false
        if (pathPrefix != null && !url.path.startsWith(pathPrefix)) return false
        return true
    }
}

sealed interface RuleSource {
    data object Host : RuleSource
    data object Path : RuleSource
    data object Fragment : RuleSource
    data class Parameter(val nameRegex: String) : RuleSource
}

sealed interface SanitizerRule {
    data class RemoveParameters(val nameRegex: String, val negate: Boolean = false) : SanitizerRule
    data class RemoveExactParameters(val names: Set<String>, val prefixes: List<String> = emptyList()) : SanitizerRule
    data object RemoveEmptyParameters : SanitizerRule
    data class RemoveFragment(val pattern: String? = null) : SanitizerRule
    data class RewriteHost(
        val pattern: String,
        val replacement: String,
        val from: RuleSource = RuleSource.Host
    ) : SanitizerRule
    data class RewritePath(
        val pattern: String,
        val replacement: String,
        val from: RuleSource = RuleSource.Path
    ) : SanitizerRule
    data class FollowRedirect(
        val from: RuleSource,
        val steps: List<DecodeStep>,
        val dropParameters: Boolean = false
    ) : SanitizerRule
    data class ShortLinkResolve(
        val pathPattern: String = ".*",
        val requiredParamRegex: String? = null
    ) : SanitizerRule
}

data class SanitizerSpec(
    val id: String,
    val name: String,
    val category: SanitizerCategory,
    val description: String,
    val matches: List<SanitizerMatch> = listOf(SanitizerMatch()),
    val rules: List<SanitizerRule>,
    val isShortLinkResolver: Boolean = false
)

object SanitizerCatalog {

    val GLOBAL_TRACKING_EXACT_KEYS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "utm_id", "utm_name", "utm_reader", "utm_brand", "utm_pubreferrer",
        "utm_swu", "utm_viz_id", "utm_referrer", "utm_social", "utm_social-type",
        "utm_source_platform", "utm_creative_format", "utm_marketing_tactic",
        "fbclid", "gclid", "gclsrc", "gad_source", "dclid", "twclid", "msclkid",
        "gad_campaignid", "srsltid", "ttclid", "li_fat_id", "sccid", "rdt_cid",
        "ef_id", "s_kwcid", "yclid", "gbraid", "wbraid", "ko_click_id", "epik",
        "_ga", "_gl", "_hsenc", "_hsmi", "__hssc", "__hstc", "__hsfp",
        "hsctatracking", "mc_cid", "mc_eid", "_openstat", "_ke", "_kx", "__s",
        "vgo_ee", "rb_clickid", "_bta_tid", "_bta_c", "ml_subscriber",
        "ml_subscriber_hash", "oly_anon_id", "oly_enc_id", "vero_conv", "vero_id",
        "wickedid", "irclickid", "irgwc", "sscid", "zanpid", "sharedid", "ranmid",
        "raneaid", "ransiteid", "shareasale_site_id", "shareasale_user_id",
        "spjobid", "spmailingid", "spreportid", "spuserid", "__twitter_impression",
        "mkt_tok", "sfmc_activityid", "guccounter", "guce_referrer", "guce_referrer_sig",
        "shem", "igshid", "igsh", "igsi"
    )

    val GLOBAL_TRACKING_PREFIXES = listOf(
        "utm_", "ga_", "gad_", "fb_", "mkt_", "at_", "cx_", "cxrecs_s", "mibextid",
        "sfmc_", "wt_", "wt.", "pk_", "mtm_", "matomo_", "piwik_", "itm_", "elq", "xtor"
    )

    val allSanitizers: List<SanitizerSpec> = listOf(
        // ---------------- GLOBAL TRACKERS ----------------
        SanitizerSpec(
            id = "india_ecommerce_fashion",
            name = "Myntra, Ajio, Meesho, Nykaa & TataCliq",
            category = SanitizerCategory.SHOPPING,
            description = "Removes affiliate, share, and campaign tracking from Indian shopping platforms.",
            matches = listOf(
                SanitizerMatch(HostMatcher.Subdomains("myntra.com")),
                SanitizerMatch(HostMatcher.Subdomains("ajio.com")),
                SanitizerMatch(HostMatcher.Subdomains("meesho.com")),
                SanitizerMatch(HostMatcher.Subdomains("nykaa.com")),
                SanitizerMatch(HostMatcher.Subdomains("tatacliq.com"))
            ),
            rules = listOf(
                SanitizerRule.RemoveExactParameters(
                    names = setOf("utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "af_dp", "af_web_dp", "pid", "c", "is_retargeting", "af_click_lookback", "af_adset_id", "af_ad_id", "af_channel", "source_caller", "shortlink", "Referrer", "referrer", "share_id", "trackerId", "cid", "aff_id"),
                    prefixes = listOf("utm_", "af_", "ef_")
                )
            )
        ),
        SanitizerSpec(
            id = "india_short_links",
            name = "Indian Shopping Short Links (fkrt.it, myntr.it, ajio.page.link)",
            category = SanitizerCategory.SHOPPING,
            description = "Resolves Flipkart, Myntra, and Ajio short links to their clean product URLs.",
            isShortLinkResolver = true,
            matches = listOf(
                SanitizerMatch(HostMatcher.ExactDomain("fkrt.it")),
                SanitizerMatch(HostMatcher.ExactDomain("myntr.it")),
                SanitizerMatch(HostMatcher.ExactDomain("dl.flipkart.com")),
                SanitizerMatch(HostMatcher.Subdomains("page.link"))
            ),
            rules = listOf(SanitizerRule.ShortLinkResolve("/.+"))
        ),
        SanitizerSpec(
            id = "india_streaming_food",
            name = "JioCinema, Hotstar, Zomato, Swiggy & BookMyShow",
            category = SanitizerCategory.VIDEO_MUSIC,
            description = "Strips app-share, referral, and campaign trackers from Indian entertainment and food links.",
            matches = listOf(
                SanitizerMatch(HostMatcher.Subdomains("jiocinema.com")),
                SanitizerMatch(HostMatcher.Subdomains("hotstar.com")),
                SanitizerMatch(HostMatcher.Subdomains("zomato.com")),
                SanitizerMatch(HostMatcher.Subdomains("swiggy.com")),
                SanitizerMatch(HostMatcher.Subdomains("bookmyshow.com"))
            ),
            rules = listOf(
                SanitizerRule.RemoveExactParameters(
                    names = setOf("utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "share_id", "ref", "referrer", "source", "af_dp", "pid", "c", "is_retargeting"),
                    prefixes = listOf("utm_", "af_")
                )
            )
        ),
        SanitizerSpec(
            id = "discord_telegram_share",
            name = "Discord, Telegram, Signal & Slack Outbound Links",
            category = SanitizerCategory.SOCIAL,
            description = "Strips instant-messaging referral and click-tracking parameters.",
            matches = listOf(SanitizerMatch(HostMatcher.AnyHost)),
            rules = listOf(
                SanitizerRule.RemoveExactParameters(
                    names = setOf("tg_rhash", "fb_Https", "s_cid", "vero_id", "wickedid", "_branch_match_id", "_branch_referrer", "branch_data", "ss_source", "ss_campaign_id", "ss_email_id", "hsa_acc", "hsa_cam", "hsa_grp", "hsa_ad", "hsa_src", "hsa_tgt", "hsa_kw", "hsa_mt", "hsa_net", "hsa_ver"),
                    prefixes = listOf("hsa_", "branch_", "tt_", "spm_")
                )
            )
        ),
        SanitizerSpec(
            id = "universal_redirect_unwrapper",
            name = "Universal Redirect & Ad-Gate Unwrapper",
            category = SanitizerCategory.SEARCH_REDIRECT,
            description = "Extracts embedded destination URLs from l.instagram.com, out.reddit.com, t.umblr.com, vk.com/away.php, and common redirect gateways.",
            matches = listOf(
                SanitizerMatch(HostMatcher.Subdomains("l.instagram.com")),
                SanitizerMatch(HostMatcher.Subdomains("t.umblr.com")),
                SanitizerMatch(HostMatcher.Subdomains("vk.com"), pathPrefix = "/away.php"),
                SanitizerMatch(HostMatcher.Subdomains("steamcommunity.com"), pathPrefix = "/linkfilter"),
                SanitizerMatch(HostMatcher.Subdomains("awstrack.me"))
            ),
            rules = listOf(
                SanitizerRule.FollowRedirect(
                    from = RuleSource.Parameter("u|z|to|url|target|dest|redirect|redirect_url"),
                    steps = listOf(DecodeStep.PercentDecode)
                )
            )
        ),
        SanitizerSpec(
            id = "global_universal_trackers",
            name = "Universal Ad & Analytics Trackers",
            category = SanitizerCategory.GLOBAL,
            description = "Strips 65+ universal tracking parameters (utm_*, fbclid, gclid, msclkid, ttclid, mc_eid, HubSpot, Matomo, etc.)",
            rules = listOf(
                SanitizerRule.RemoveExactParameters(GLOBAL_TRACKING_EXACT_KEYS, GLOBAL_TRACKING_PREFIXES)
            )
        ),
        SanitizerSpec(
            id = "google_analytics",
            name = "Google Analytics & Ads (ga_*, utm_*, gclid, gad_*)",
            category = SanitizerCategory.GLOBAL,
            description = "Removes Google Analytics, AdWords, and Discover share tracking tokens",
            rules = listOf(
                SanitizerRule.RemoveParameters("(?:ga_|utm_|gclid|gad_|shem).*")
            )
        ),
        SanitizerSpec(
            id = "facebook_analytics",
            name = "Meta & Facebook Analytics (fb_*, fbclid, sfnsn, mibextid)",
            category = SanitizerCategory.GLOBAL,
            description = "Removes Facebook click IDs, Meta ad campaign parameters, and share tokens",
            rules = listOf(
                SanitizerRule.RemoveParameters("(?:fb_|fbclid|sfnsn|cHash|mibextid|ad_id|adset_id|campaign_id|gc_id|h_ad_id|placement).*")
            )
        ),
        SanitizerSpec(
            id = "adobe_marketo",
            name = "Adobe Marketo Engage (mkt_*)",
            category = SanitizerCategory.GLOBAL,
            description = "Strips Adobe Marketo email and campaign tracking parameters",
            rules = listOf(SanitizerRule.RemoveParameters("mkt_.*"))
        ),
        SanitizerSpec(
            id = "salesforce_mc",
            name = "Salesforce Marketing Cloud (sfmc_*)",
            category = SanitizerCategory.GLOBAL,
            description = "Strips Salesforce Marketing Cloud subscriber tracking tokens",
            rules = listOf(SanitizerRule.RemoveParameters("sfmc_.*"))
        ),
        SanitizerSpec(
            id = "at_cx_webtrekk",
            name = "AT Internet, Cxense & Webtrekk (at_*, cx_*, wt_*)",
            category = SanitizerCategory.GLOBAL,
            description = "Strips European publisher analytics parameters globally",
            rules = listOf(SanitizerRule.RemoveParameters("(?:at_|cx_|cxrecs_s|wt_).*"))
        ),
        SanitizerSpec(
            id = "yahoo_referrer",
            name = "Yahoo Consent & Referrer (guccounter, guce_*)",
            category = SanitizerCategory.GLOBAL,
            description = "Removes Yahoo Oath/GUCE referrer signature parameters globally",
            rules = listOf(SanitizerRule.RemoveParameters("guccounter|guce_referrer|guce_referrer_sig"))
        ),
        SanitizerSpec(
            id = "session_ids",
            name = "Session IDs (sessionid, jsessionid)",
            category = SanitizerCategory.GLOBAL,
            description = "Removes leaked server session identifiers from shared links",
            rules = listOf(SanitizerRule.RemoveParameters("j?sessionid.*"))
        ),
        SanitizerSpec(
            id = "echobox_fragment",
            name = "Echobox Social Fragments (#Echobox=*)",
            category = SanitizerCategory.GLOBAL,
            description = "Removes #Echobox=... social media automation fragments",
            rules = listOf(SanitizerRule.RemoveFragment("Echobox=.*"))
        ),
        SanitizerSpec(
            id = "empty_parameters",
            name = "Empty Query Parameters (?key=)",
            category = SanitizerCategory.GLOBAL,
            description = "Cleans up dangling empty query parameters while preserving valueless flags",
            rules = listOf(SanitizerRule.RemoveEmptyParameters)
        ),

        // ---------------- SOCIAL & MESSAGING ----------------
        SanitizerSpec(
            id = "x_twitter",
            name = "X (Twitter)",
            category = SanitizerCategory.SOCIAL,
            description = "Removes s, t, ref_src, ref_url, twclid and all share tracking on x.com & twitter.com",
            matches = listOf(SanitizerMatch(HostMatcher.RegexHost("(?:(?:mobile|m)\\.)?(?:twitter|x|fixupx|fxtwitter|vxtwitter|xcancel)\\.com"))),
            rules = listOf(SanitizerRule.RemoveParameters(".*"))
        ),
        SanitizerSpec(
            id = "instagram",
            name = "Instagram",
            category = SanitizerCategory.SOCIAL,
            description = "Removes igsh, igshid, igsi, rpxt, stkn while preserving carousel img_index",
            matches = listOf(SanitizerMatch(HostMatcher.Subdomains("instagram.com"))),
            rules = listOf(SanitizerRule.RemoveParameters("igsh|igshid|igsi|rpxt|stkn"))
        ),
        SanitizerSpec(
            id = "tiktok",
            name = "TikTok",
            category = SanitizerCategory.SOCIAL,
            description = "Strips _t, _r, checksum, sec_user_id, share_app_id and all TikTok tracking parameters",
            matches = listOf(SanitizerMatch(HostMatcher.Subdomains("tiktok.com"))),
            rules = listOf(SanitizerRule.RemoveParameters(".*"))
        ),
        SanitizerSpec(
            id = "tiktok_short_link",
            name = "TikTok Short Links (vm.tiktok.com, vt.tiktok.com, /t/)",
            category = SanitizerCategory.SOCIAL,
            description = "Unshortens TikTok mobile share links over network hops without loading the destination page",
            matches = listOf(
                SanitizerMatch(HostMatcher.ExactDomain("vm.tiktok.com")),
                SanitizerMatch(HostMatcher.ExactDomain("vt.tiktok.com")),
                SanitizerMatch(HostMatcher.ExactDomain("tiktok.com"), pathPrefix = "/t/"),
                SanitizerMatch(HostMatcher.ExactDomain("tt.site"), pathPrefix = "/t/")
            ),
            rules = listOf(SanitizerRule.ShortLinkResolve("(?:/t)?/[A-Za-z0-9_-]{2,}/?")),
            isShortLinkResolver = true
        ),
        SanitizerSpec(
            id = "reddit",
            name = "Reddit",
            category = SanitizerCategory.SOCIAL,
            description = "Removes share_id, utm_name, context and tracking parameters from Reddit links",
            matches = listOf(SanitizerMatch(HostMatcher.Subdomains("reddit.com"))),
            rules = listOf(SanitizerRule.RemoveParameters(".*"))
        ),
        SanitizerSpec(
            id = "reddit_short_link",
            name = "Reddit App Short Links (/r/.../s/ & redd.it)",
            category = SanitizerCategory.SOCIAL,
            description = "Resolves opaque /r/subreddit/s/code mobile share links to canonical post URLs",
            matches = listOf(
                SanitizerMatch(HostMatcher.Subdomains("reddit.com")),
                SanitizerMatch(HostMatcher.ExactDomain("redd.it"))
            ),
            rules = listOf(SanitizerRule.ShortLinkResolve("(?:/r/[^/]+/s)?/[A-Za-z0-9_-]{2,}/?")),
            isShortLinkResolver = true
        ),
        SanitizerSpec(
            id = "reddit_out_mail",
            name = "Reddit Outbound & Newsletter Redirects",
            category = SanitizerCategory.SOCIAL,
            description = "Unwraps out.reddit.com and click.redditmail.com redirect wrappers offline",
            matches = listOf(
                SanitizerMatch(HostMatcher.ExactDomain("out.reddit.com")),
                SanitizerMatch(HostMatcher.ExactDomain("click.redditmail.com"))
            ),
            rules = listOf(
                SanitizerRule.FollowRedirect(RuleSource.Parameter("url"), listOf(DecodeStep.PercentDecode)),
                SanitizerRule.FollowRedirect(
                    RuleSource.Path,
                    listOf(DecodeStep.Capture("/[^/]+/(.+)"), DecodeStep.PercentDecode),
                    dropParameters = true
                )
            )
        ),
        SanitizerSpec(
            id = "facebook_com",
            name = "Facebook Posts & Redirects",
            category = SanitizerCategory.SOCIAL,
            description = "Keeps only id & story_fbid on facebook.com and unwraps l.facebook.com link shims",
            matches = listOf(SanitizerMatch(HostMatcher.Subdomains("facebook.com"))),
            rules = listOf(
                SanitizerRule.FollowRedirect(RuleSource.Parameter("u"), listOf(DecodeStep.PercentDecode)),
                SanitizerRule.RemoveParameters("(id|story_fbid)", negate = true)
            )
        ),
        SanitizerSpec(
            id = "threads",
            name = "Threads (threads.net & threads.com)",
            category = SanitizerCategory.SOCIAL,
            description = "Removes xmt, igshid and all tracking parameters from Threads links",
            matches = listOf(
                SanitizerMatch(HostMatcher.Subdomains("threads.net")),
                SanitizerMatch(HostMatcher.Subdomains("threads.com"))
            ),
            rules = listOf(SanitizerRule.RemoveParameters(".*"))
        ),
        SanitizerSpec(
            id = "bluesky_redirect",
            name = "Bluesky Redirects (go.bsky.app)",
            category = SanitizerCategory.SOCIAL,
            description = "Unwraps go.bsky.app/redirect?u=... links offline",
            matches = listOf(SanitizerMatch(HostMatcher.ExactDomain("go.bsky.app"), pathPrefix = "/redirect")),
            rules = listOf(SanitizerRule.FollowRedirect(RuleSource.Parameter("u"), listOf(DecodeStep.PercentDecode)))
        ),
        SanitizerSpec(
            id = "linkedin",
            name = "LinkedIn",
            category = SanitizerCategory.SOCIAL,
            description = "Removes rcm, trk, trackingId, lipi and share tokens from LinkedIn URLs",
            matches = listOf(SanitizerMatch(HostMatcher.Subdomains("linkedin.com"))),
            rules = listOf(SanitizerRule.RemoveParameters("rcm|trk|trkInfo|trackingId|lipi|licu|originalSubdomain"))
        ),
        SanitizerSpec(
            id = "pinterest",
            name = "Pinterest",
            category = SanitizerCategory.SOCIAL,
            description = "Removes e_t, e_t_s, ouuid, pin_unauth and share tracking on Pinterest",
            matches = listOf(SanitizerMatch(HostMatcher.Subdomains("pinterest.com"))),
            rules = listOf(SanitizerRule.RemoveParameters("e_t|e_t_s|e_t_cs|ouuid|pin_unauth|sfo|sender"))
        ),
        SanitizerSpec(
            id = "snapchat_whatsapp",
            name = "Snapchat & WhatsApp",
            category = SanitizerCategory.SOCIAL,
            description = "Strips share_id, locale, link_source and link_medium from Snapchat & WhatsApp links",
            matches = listOf(
                SanitizerMatch(HostMatcher.Subdomains("snapchat.com")),
                SanitizerMatch(HostMatcher.Subdomains("whatsapp.com"))
            ),
            rules = listOf(SanitizerRule.RemoveParameters(".*"))
        ),
        SanitizerSpec(
            id = "jodel",
            name = "Jodel Share Unwrapper",
            category = SanitizerCategory.SOCIAL,
            description = "Decodes Base64 JSON payload in shared.jodel.com links",
            matches = listOf(SanitizerMatch(HostMatcher.ExactDomain("shared.jodel.com"))),
            rules = listOf(
                SanitizerRule.FollowRedirect(
                    RuleSource.Parameter("data"),
                    listOf(
                        DecodeStep.Capture("([^?]*)"),
                        DecodeStep.PercentDecode,
                        DecodeStep.Base64Decode,
                        DecodeStep.JsonField("\$android_url")
                    )
                )
            )
        ),

        // ---------------- VIDEO & MUSIC ----------------
        SanitizerSpec(
            id = "youtube",
            name = "YouTube & YouTube Shorts",
            category = SanitizerCategory.VIDEO_MUSIC,
            description = "Strips si, pp, feature and share tokens while preserving v, t, list, search_query, and channel_id",
            matches = listOf(
                SanitizerMatch(HostMatcher.RegexHost("(?:m(?:usic)?\\.)?youtube\\.com")),
                SanitizerMatch(HostMatcher.ExactDomain("youtu.be"))
            ),
            rules = listOf(
                SanitizerRule.FollowRedirect(RuleSource.Parameter("q"), listOf(DecodeStep.PercentDecode)),
                SanitizerRule.RemoveParameters("(v|search_query|list|t|channel_id|index)", negate = true)
            )
        ),
        SanitizerSpec(
            id = "youtu_be_expand",
            name = "Youtu.be Short URL Expander",
            category = SanitizerCategory.VIDEO_MUSIC,
            description = "Expands youtu.be/<id> links into canonical youtube.com/watch?v=<id> URLs",
            matches = listOf(SanitizerMatch(HostMatcher.ExactDomain("youtu.be"))),
            rules = listOf(
                SanitizerRule.FollowRedirect(
                    RuleSource.Path,
                    listOf(DecodeStep.Capture("/([^/?#]+)", "https://www.youtube.com/watch?v=\$1"))
                )
            )
        ),
        SanitizerSpec(
            id = "spotify",
            name = "Spotify",
            category = SanitizerCategory.VIDEO_MUSIC,
            description = "Strips si, nd, dl_branch, context and share tracking from open.spotify.com links",
            matches = listOf(SanitizerMatch(HostMatcher.Subdomains("spotify.com"))),
            rules = listOf(SanitizerRule.RemoveParameters(".*"))
        ),
        SanitizerSpec(
            id = "twitch_netflix_bilibili",
            name = "Twitch, Netflix & Bilibili",
            category = SanitizerCategory.VIDEO_MUSIC,
            description = "Removes tt_medium, trkid, tctx, vd_source, seid, share_source and copy_link",
            matches = listOf(
                SanitizerMatch(HostMatcher.Subdomains("twitch.tv")),
                SanitizerMatch(HostMatcher.Subdomains("netflix.com")),
                SanitizerMatch(HostMatcher.Subdomains("bilibili.com"))
            ),
            rules = listOf(SanitizerRule.RemoveParameters("tt_medium|tt_content|trkid|tctx|vd_source|seid|from|share_source|copy_link"))
        ),

        // ---------------- SHOPPING & DEALS ----------------
        SanitizerSpec(
            id = "amazon_products",
            name = "Amazon Products & Sponsored Links",
            category = SanitizerCategory.SHOPPING,
            description = "Canonicalizes Amazon product URLs to /dp/<ASIN>/ and strips SEO slugs, ref tags, and query parameters",
            matches = listOf(SanitizerMatch(HostMatcher.RegexHost("(?:[^.]+\\.)*amazon\\.[^/?#:]+"))),
            rules = listOf(
                SanitizerRule.RewritePath(
                    pattern = "(?i).*%2F(?:dp?|gp%2Fproduct)%2F([^%/?&]*).*",
                    replacement = "/dp/\$1/",
                    from = RuleSource.Parameter("url")
                ),
                SanitizerRule.RewritePath(".*/(?:dp?|gp/product|gp/aw/d)/([^/?&]+).*", "/dp/\$1/"),
                SanitizerRule.RemoveParameters(".*")
            )
        ),
        SanitizerSpec(
            id = "amazon_short_links",
            name = "Amazon Short Links (amzn.to, a.co, amzn.eu)",
            category = SanitizerCategory.SHOPPING,
            description = "Resolves shortened Amazon links over network hops and cleans the target product URL",
            matches = listOf(
                SanitizerMatch(HostMatcher.ExactDomain("amzn.to")),
                SanitizerMatch(HostMatcher.ExactDomain("amzn.eu")),
                SanitizerMatch(HostMatcher.ExactDomain("a.co"))
            ),
            rules = listOf(SanitizerRule.ShortLinkResolve("(?:/d)?/[A-Za-z0-9_-]{2,}/?")),
            isShortLinkResolver = true
        ),
        SanitizerSpec(
            id = "flipkart",
            name = "Flipkart",
            category = SanitizerCategory.SHOPPING,
            description = "Strips otracker, ssid, lid, marketplace, qH and app share tracking from Flipkart URLs",
            matches = listOf(SanitizerMatch(HostMatcher.Subdomains("flipkart.com"))),
            rules = listOf(SanitizerRule.RemoveParameters(".*"))
        ),
        SanitizerSpec(
            id = "ebay",
            name = "eBay",
            category = SanitizerCategory.SHOPPING,
            description = "Removes mkcid, mkevt, mkrid, ssspo, sssrc, ssuid and affiliate parameters on eBay item pages",
            matches = listOf(SanitizerMatch(HostMatcher.RegexHost("(?:[^.]+\\.)*ebay\\.[^/?#:]+"), pathPrefix = "/itm/")),
            rules = listOf(SanitizerRule.RemoveParameters(".*"))
        ),
        SanitizerSpec(
            id = "aliexpress",
            name = "AliExpress",
            category = SanitizerCategory.SHOPPING,
            description = "Strips spm, aff_fcid, aff_trace_key, pdp_npi, scm and tracking parameters on AliExpress",
            matches = listOf(SanitizerMatch(HostMatcher.RegexHost("(?:[^.]+\\.)*aliexpress\\.[^/?#:]+"))),
            rules = listOf(SanitizerRule.RemoveParameters("(sku_id|currency|language|gatewayAdapt)", negate = true))
        ),
        SanitizerSpec(
            id = "newegg_ikea_humble",
            name = "NewEgg, IKEA, Humble Bundle & Kogan",
            category = SanitizerCategory.SHOPPING,
            description = "Canonicalizes NewEgg /p/<ID> product paths and removes tracking on IKEA, Humble Bundle & Kogan",
            matches = listOf(
                SanitizerMatch(HostMatcher.RegexHost("(?:[^.]+\\.)*newegg\\.[^/?#:]+")),
                SanitizerMatch(HostMatcher.Subdomains("ikea.com")),
                SanitizerMatch(HostMatcher.Subdomains("humblebundle.com")),
                SanitizerMatch(HostMatcher.Subdomains("kogan.com"))
            ),
            rules = listOf(
                SanitizerRule.RewritePath("/.+(/p/[0-9A-Za-z]+).*", "\$1"),
                SanitizerRule.RemoveParameters(".*")
            )
        ),
        SanitizerSpec(
            id = "mydealz_pepper",
            name = "Pepper / MyDealz / DesiDime / HotUKDeals Network",
            category = SanitizerCategory.SHOPPING,
            description = "Rewrites /share-deal-from-app/ redirects and strips tracking across DesiDime, MyDealz, HotUKDeals & Pepper sites",
            matches = listOf(
                SanitizerMatch(HostMatcher.Subdomains("desidime.com")),
                SanitizerMatch(HostMatcher.Subdomains("mydealz.de")),
                SanitizerMatch(HostMatcher.Subdomains("hotukdeals.com")),
                SanitizerMatch(HostMatcher.Subdomains("dealabs.com")),
                SanitizerMatch(HostMatcher.Subdomains("chollometro.com")),
                SanitizerMatch(HostMatcher.Subdomains("pepper.com"))
            ),
            rules = listOf(
                SanitizerRule.RewritePath("/share-deal-from-app/(.+)", "/deals/a-\$1"),
                SanitizerRule.RemoveParameters(".*")
            )
        ),
        SanitizerSpec(
            id = "lazada_shopee_taobao",
            name = "Lazada, Shopee, Taobao, Tmall, JD & Pinduoduo",
            category = SanitizerCategory.SHOPPING,
            description = "Strips e-commerce share tokens across Lazada, Shopee, Taobao, Tmall, JD.com, and Pinduoduo",
            matches = listOf(
                SanitizerMatch(HostMatcher.Subdomains("lazada.com.my")),
                SanitizerMatch(HostMatcher.Subdomains("shopee.com.my")),
                SanitizerMatch(HostMatcher.Subdomains("taobao.com")),
                SanitizerMatch(HostMatcher.Subdomains("tmall.com")),
                SanitizerMatch(HostMatcher.Subdomains("jd.com")),
                SanitizerMatch(HostMatcher.Subdomains("pinduoduo.com"))
            ),
            rules = listOf(SanitizerRule.RemoveParameters(".*"))
        ),

        // ---------------- SEARCH, MAPS & REDIRECTS ----------------
        SanitizerSpec(
            id = "google_search_ads",
            name = "Google Search & Google Ads Redirects",
            category = SanitizerCategory.SEARCH_REDIRECT,
            description = "Unwraps google.com/url?q=... and googleadservices.com?adurl=... redirects offline",
            matches = listOf(
                SanitizerMatch(HostMatcher.RegexHost("(?:[^.]+\\.)*google\\.[^/?#:]+"), pathPrefix = "/url"),
                SanitizerMatch(HostMatcher.Subdomains("googleadservices.com"))
            ),
            rules = listOf(
                SanitizerRule.FollowRedirect(RuleSource.Parameter("url|q|adurl"), listOf(DecodeStep.PercentDecode))
            )
        ),
        SanitizerSpec(
            id = "google_maps",
            name = "Google Maps & Store",
            category = SanitizerCategory.SEARCH_REDIRECT,
            description = "Cleans protobuf data blobs from Google Maps URLs and hl/selections from Google Store",
            matches = listOf(
                SanitizerMatch(HostMatcher.RegexHost("(?:[^.]+\\.)*google\\.[^/?#:]+"), pathPrefix = "/maps"),
                SanitizerMatch(HostMatcher.ExactDomain("maps.google.com")),
                SanitizerMatch(HostMatcher.ExactDomain("store.google.com"))
            ),
            rules = listOf(
                SanitizerRule.RewritePath(".*(@-?\\d+\\.\\d+,-?\\d+\\.\\d+,\\d+(?:\\.\\d+)?z).*", "/maps/\$1"),
                SanitizerRule.RemoveParameters("hl|selections|entry|g_ep")
            )
        ),
        SanitizerSpec(
            id = "google_short_links",
            name = "Google Short Links (share.google & maps.app.goo.gl)",
            category = SanitizerCategory.SEARCH_REDIRECT,
            description = "Resolves share.google and maps.app.goo.gl short links over network hops",
            matches = listOf(
                SanitizerMatch(HostMatcher.ExactDomain("share.google")),
                SanitizerMatch(HostMatcher.ExactDomain("maps.app.goo.gl"))
            ),
            rules = listOf(SanitizerRule.ShortLinkResolve("/[A-Za-z0-9_-]{2,}/?")),
            isShortLinkResolver = true
        ),
        SanitizerSpec(
            id = "universal_shorteners",
            name = "Universal URL Shorteners (bit.ly, tinyurl, t.co, rb.gy, is.gd, cutt.ly)",
            category = SanitizerCategory.SEARCH_REDIRECT,
            description = "Unshortens bit.ly, tinyurl.com, t.co, rb.gy, is.gd, cutt.ly, ow.ly, shorturl.at & lnkd.in links",
            matches = listOf(
                SanitizerMatch(HostMatcher.ExactDomain("bit.ly")),
                SanitizerMatch(HostMatcher.ExactDomain("tinyurl.com")),
                SanitizerMatch(HostMatcher.ExactDomain("t.co")),
                SanitizerMatch(HostMatcher.ExactDomain("rb.gy")),
                SanitizerMatch(HostMatcher.ExactDomain("is.gd")),
                SanitizerMatch(HostMatcher.ExactDomain("cutt.ly")),
                SanitizerMatch(HostMatcher.ExactDomain("ow.ly")),
                SanitizerMatch(HostMatcher.ExactDomain("shorturl.at")),
                SanitizerMatch(HostMatcher.ExactDomain("lnkd.in"))
            ),
            rules = listOf(SanitizerRule.ShortLinkResolve("/[A-Za-z0-9_-]{2,}/?")),
            isShortLinkResolver = true
        ),
        SanitizerSpec(
            id = "affiliate_redirects",
            name = "Affiliate Redirect Unwrappers (AvantLink, GeoRiot, Jdoqocy, LinkSynergy)",
            category = SanitizerCategory.SEARCH_REDIRECT,
            description = "Unwraps affiliate click-tracking wrappers offline to extract the direct destination URL",
            matches = listOf(
                SanitizerMatch(HostMatcher.Subdomains("avantlink.com")),
                SanitizerMatch(HostMatcher.RegexHost("target\\.georiot\\.[^/?#:]+")),
                SanitizerMatch(HostMatcher.Subdomains("jdoqocy.com")),
                SanitizerMatch(HostMatcher.RegexHost("(?:[^.]+\\.)*linksynergy\\.[^/?#:]+"))
            ),
            rules = listOf(
                SanitizerRule.FollowRedirect(RuleSource.Parameter("url|GR_URL|murl"), listOf(DecodeStep.PercentDecode))
            )
        ),
        SanitizerSpec(
            id = "yahoo_aol_bing_ddg_yandex",
            name = "Yahoo, AOL, Bing, DuckDuckGo & Yandex Search",
            category = SanitizerCategory.SEARCH_REDIRECT,
            description = "Unwraps /RU=.../ search redirects and cleans search engine tracking parameters",
            matches = listOf(
                SanitizerMatch(HostMatcher.Subdomains("search.yahoo.com")),
                SanitizerMatch(HostMatcher.Subdomains("search.aol.com")),
                SanitizerMatch(HostMatcher.Subdomains("bing.com")),
                SanitizerMatch(HostMatcher.Subdomains("duckduckgo.com")),
                SanitizerMatch(HostMatcher.Subdomains("yandex.com"))
            ),
            rules = listOf(
                SanitizerRule.FollowRedirect(
                    RuleSource.Path,
                    listOf(DecodeStep.Capture("RU=([^/]+)"), DecodeStep.PercentDecode)
                ),
                SanitizerRule.RemoveParameters("cvid|t|atb")
            )
        ),

        // ---------------- NEWS, SUBSTACK & PUBLISHING ----------------
        SanitizerSpec(
            id = "substack",
            name = "Substack",
            category = SanitizerCategory.NEWS_TECH,
            description = "Rewrites open.substack.com/pub/<name> to <name>.substack.com and removes r, token & triedRedirect",
            matches = listOf(SanitizerMatch(HostMatcher.Subdomains("substack.com"))),
            rules = listOf(
                SanitizerRule.RewriteHost("/pub/([^/]+)(?:/.*)?", "\$1.substack.com", from = RuleSource.Path),
                SanitizerRule.RewritePath("/pub/[^/]+(/.*)?", "\$1"),
                SanitizerRule.RemoveParameters("(publication_id|post_id|note_id|j)", negate = true)
            )
        ),
        SanitizerSpec(
            id = "news_publishers",
            name = "Wikipedia, The Guardian, NYTimes, Medium, FastCompany, Spiegel & Heise",
            category = SanitizerCategory.NEWS_TECH,
            description = "Removes wprov, cmp, smid, source, sk and article share tracking across major publishers",
            matches = listOf(
                SanitizerMatch(HostMatcher.Subdomains("wikipedia.org")),
                SanitizerMatch(HostMatcher.Subdomains("theguardian.com")),
                SanitizerMatch(HostMatcher.Subdomains("nytimes.com")),
                SanitizerMatch(HostMatcher.Subdomains("medium.com")),
                SanitizerMatch(HostMatcher.Subdomains("fastcompany.com")),
                SanitizerMatch(HostMatcher.Subdomains("spiegel.de")),
                SanitizerMatch(HostMatcher.Subdomains("heise.de")),
                SanitizerMatch(HostMatcher.Subdomains("change.org"))
            ),
            rules = listOf(SanitizerRule.RemoveParameters("wprov|cmp|smid|source|sk|utm_.*"))
        ),

        // ---------------- REGIONAL & SUPER-APPS ----------------
        SanitizerSpec(
            id = "regional_super_apps",
            name = "Douyin, Xiaohongshu, WeChat, Weibo, Zhihu, DingTalk & Feishu",
            category = SanitizerCategory.REGIONAL,
            description = "Cleans tracking parameters and share suffixes from Asian super-apps",
            matches = listOf(
                SanitizerMatch(HostMatcher.Subdomains("douyin.com")),
                SanitizerMatch(HostMatcher.Subdomains("xiaohongshu.com")),
                SanitizerMatch(HostMatcher.Subdomains("xhslink.com")),
                SanitizerMatch(HostMatcher.Subdomains("weixin.qq.com")),
                SanitizerMatch(HostMatcher.Subdomains("weibo.com")),
                SanitizerMatch(HostMatcher.Subdomains("zhihu.com")),
                SanitizerMatch(HostMatcher.Subdomains("dingtalk.com")),
                SanitizerMatch(HostMatcher.Subdomains("feishu.cn"))
            ),
            rules = listOf(SanitizerRule.RemoveParameters(".*"))
        )
    )
}
