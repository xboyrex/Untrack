package com.xboyrex.untrack.fxengine.cleaners

import com.xboyrex.untrack.fxengine.cleaners.impl.AmazonCleaner
import com.xboyrex.untrack.fxengine.cleaners.impl.CatalogParameterCleaner
import com.xboyrex.untrack.fxengine.cleaners.impl.FacebookCleaner
import com.xboyrex.untrack.fxengine.cleaners.impl.GeneralTrackingCleaner
import com.xboyrex.untrack.fxengine.cleaners.impl.GoogleMapsCleaner
import com.xboyrex.untrack.fxengine.cleaners.impl.GoogleSearchCleaner
import com.xboyrex.untrack.fxengine.cleaners.impl.InstagramCleaner
import com.xboyrex.untrack.fxengine.cleaners.impl.LinkedInCleaner
import com.xboyrex.untrack.fxengine.cleaners.impl.OfflineRedirectCleaner
import com.xboyrex.untrack.fxengine.cleaners.impl.RedditCleaner
import com.xboyrex.untrack.fxengine.cleaners.impl.SubstackCleaner
import com.xboyrex.untrack.fxengine.cleaners.impl.TikTokCleaner
import com.xboyrex.untrack.fxengine.cleaners.impl.TwitterCleaner
import com.xboyrex.untrack.fxengine.cleaners.impl.YouTubeCleaner

object CleanerCatalog {
    fun createBuiltInCleaners(): List<UrlCleaner> =
        listOf(
            AmazonCleaner,
            YouTubeCleaner,
            GoogleSearchCleaner,
            GoogleMapsCleaner,
            TwitterCleaner,
            InstagramCleaner,
            FacebookCleaner,
            RedditCleaner,
            TikTokCleaner,
            LinkedInCleaner,
            SubstackCleaner,
            OfflineRedirectCleaner
        ) +
            ParameterRuleCatalog.rules.map(::CatalogParameterCleaner) +
            GeneralTrackingCleaner()
}
