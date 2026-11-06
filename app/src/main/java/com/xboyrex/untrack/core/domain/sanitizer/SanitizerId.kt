package com.xboyrex.untrack.core.domain.sanitizer

@JvmInline
value class SanitizerId(val value: String) {

    companion object {

        /**
         * Proposes the removals the user remembered for a site, see
         * [SitePreference][com.xboyrex.untrack.core.domain.preference.SitePreference]. It is
         * not part of the catalog, so it has no switch in the settings and must never be used by a
         * catalog entry.
         */
        val Remembered = SanitizerId("remembered")
    }
}
