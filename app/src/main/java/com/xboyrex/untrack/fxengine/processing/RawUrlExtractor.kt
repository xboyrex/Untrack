package com.xboyrex.untrack.fxengine.processing

import com.xboyrex.untrack.fxengine.UrlProcessor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts the first accepted URL without decoding any URL component.
 */
@Singleton
class RawUrlExtractor @Inject constructor() {
    fun extract(input: String): String? = UrlProcessor.findFirstValidUrl(input)
}
