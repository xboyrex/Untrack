package com.xboyrex.untrack.fxengine.cleaners.impl

import com.xboyrex.untrack.fxengine.cleaners.CleanerCategory
import com.xboyrex.untrack.fxengine.cleaners.UrlCleaner
import com.xboyrex.untrack.fxengine.processing.UrlNormalizer
import com.xboyrex.untrack.fxengine.utils.Constants

/**
 * Cleaner for LinkedIn URLs - comprehensive tracking removal
 */
object LinkedInCleaner : UrlCleaner {
    override val id = "linkedin"
    override val displayName = "LinkedIn"
    override val category = CleanerCategory.SOCIAL_MEDIA
    
    // Comprehensive LinkedIn tracking parameters
    // Most extensive tracking parameter list available
    private val linkedinTracking = setOf(
        // Basic tracking
        "trk", "trkInfo", "trkEmail", "li_theme", "li_ft",
        "li_mc", "li_tc", "li_uuid", "li_at", "li_rm",
        
        // Share & referral tracking
        "share", "src", "veh", "trackingId", "refId",
        "lipi", "licu", "lici", "lict", "lics",
        
        // Analytics & attribution
        "utm_source", "utm_medium", "utm_campaign",
        "utm_term", "utm_content", "mcid", "msgConversationId",
        
        // Session & request tracking
        "sessionId", "requestId", "contextId", "csrfToken",
        "pageKey", "sessionRedirect", "fromSignIn",
        
        // Navigation tracking
        "position", "pageNum", "secureUrl", "startTask",
        "submissionId", "upsellOrderOrigin", "fromEmail",
        
        // Feed & content tracking
        "midToken", "midSig", "fromSuggestionID", "suggestedToFollow",
        "updateEntityUrn", "originTrackingId", "veh",
        
        // Notification tracking
        "msgOverlay", "messageThreadUrn", "threadUrn",
        "conversationUrn", "creatorUrn", "entityUrn",
        
        // Job & application tracking
        "alternateChannel", "alertAction", "savedSearchId",
        "searchId", "searchRequestId", "currentJobId",
        "fromJobAlertEmail", "fromJobAlertMail", "jobAlertAction",
        
        // Mobile app tracking
        "appId", "appStore", "mobileApp", "isFromMobileJob",
        "isFromMobile", "deviceType", "os", "trk_ref",
        
        // Connection & network tracking
        "invitation", "sharedKey", "sig", "invitationId",
        "invitationType", "connectionOf", "senderIdentityHint",
        
        // Groups & company tracking
        "groupId", "companyId", "organizationId", "followerId",
        "memberIdentity", "moduleKey", "detailOrigin",
        
        // Article & post tracking
        "articleId", "commentUrn", "dashCommentUrn", "replyUrn",
        "parentCommentUrn", "articleSortOrder", "sectionName",
        
        // Ads & sponsored content
        "sp", "imp", "campaign", "account", "creative",
        "sendTime", "serveTime", "displayTime", "sponsored",
        
        // Learning/Lynda tracking
        "success", "u", "e", "courseClaim", "learningHistory",
        
        // Legacy & misc parameters
        "pk", "pv", "biz", "goback", "report", "urlhash", "rcm",
        "eBP", "rs", "key", "pivot", "redirSrc", "spSrc"
    )
    
    // Parameters essential for LinkedIn functionality
    private val preserveParams = setOf(
        "keywords",     // Search keywords
        "origin",       // Search origin
        "q",            // Query parameter
        "start",        // Pagination
        "count",        // Number of results
        "filters",      // Search filters
        "sortBy",       // Sort order
        "facet",        // Faceted search
        "f_*",          // Filter parameters (pattern)
        "geoUrn",       // Geographic search
        "network",      // Network filter
        "schoolFilter", // School filter
        "companySize",  // Company size filter
        "datePosted",   // Job posting date
        "jobType",      // Job type filter
        "location",     // Location parameter
        "redirectUrl",  // OAuth redirect
        "response_type", // OAuth response type
        "client_id",    // OAuth client ID
        "state",        // OAuth state
        "scope"         // OAuth scope
    )
    
    override fun matches(url: String): Boolean {
        return UrlNormalizer.urlMatchesAnyDomain(
            url,
            listOf(Constants.LINKEDIN_DOMAIN, Constants.LINKEDIN_SHORT_DOMAIN)
        )
    }
    
    override fun clean(url: String): String {
        if (!matches(url)) return url

        try {
            // If no query parameters, return as is
            val idx = url.indexOf('?')
            if (idx == -1 || url.indexOf('#').let { it >= 0 && it < idx }) {
                return url
            }
            
            val base = url.substring(0, idx)
            val queryAndFragment = url.substring(idx + 1)
            
            // Handle fragment
            val fragmentIdx = queryAndFragment.indexOf('#')
            val query = if (fragmentIdx > -1) {
                queryAndFragment.substring(0, fragmentIdx)
            } else {
                queryAndFragment
            }
            val fragment = if (fragmentIdx > -1) {
                queryAndFragment.substring(fragmentIdx)
            } else {
                ""
            }
            
            // Process parameters - aggressive cleaning
            val kept = query.split('&').mapNotNull { pair ->
                val eqIdx = pair.indexOf('=')
                val key = if (eqIdx == -1) pair else pair.substring(0, eqIdx)
                
                // Keep essential and unknown params, remove only known tracking
                when {
                    preserveParams.contains(key) -> pair  // Always keep essential params
                    key.startsWith("f_") -> pair  // Keep filter parameters
                    linkedinTracking.contains(key) -> null  // Remove tracking params
                    else -> pair
                }
            }.filter { it.isNotEmpty() }
            
            return if (kept.isEmpty()) {
                base + fragment
            } else {
                base + "?" + kept.joinToString("&") + fragment
            }
        } catch (e: Exception) {
            // On error, return original URL
            return url
        }
    }
} 