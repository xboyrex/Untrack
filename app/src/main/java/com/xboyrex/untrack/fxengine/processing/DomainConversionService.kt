package com.xboyrex.untrack.fxengine.processing

import com.xboyrex.untrack.fxengine.UrlProcessor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps built-in social-domain conversion separate from custom rules.
 */
@Singleton
class DomainConversionService @Inject constructor(
    private val urlProcessor: UrlProcessor
) {
    fun convert(
        url: String,
        enabled: Boolean,
        selections: ProxySelections,
    ): String = urlProcessor.applyDomainConversions(
        url = url,
        convertToAlternative = enabled,
        selections = selections,
    )
}
