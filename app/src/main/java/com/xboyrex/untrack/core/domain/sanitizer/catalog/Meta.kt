package com.xboyrex.untrack.core.domain.sanitizer.catalog

import com.xboyrex.untrack.core.domain.sanitizer.Rule
import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.SanitizerId
import kotlinx.collections.immutable.persistentListOf

val MetaAd =
    Sanitizer(
        id = SanitizerId("meta_ad"),
        name = "Meta Ads",
        rules =
            persistentListOf(
                Rule.RemoveParameters("ad_id|adset_id|campaign_id|gc_id|h_ad_id|placement"),
            ),
    )
