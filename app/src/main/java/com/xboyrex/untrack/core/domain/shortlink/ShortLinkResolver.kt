package com.xboyrex.untrack.core.domain.shortlink

import com.xboyrex.untrack.core.domain.url.Url

/**
 * Asks the server behind a short link where it leads.
 *
 * `core-domain` only declares which URLs are short links — see `Rule.Resolve` and
 * `Cleaner.shortLinks` — and never performs a request itself. An implementation is what turns the
 * declaration into network traffic, and it is the app's to provide, behind the user's consent.
 */
fun interface ShortLinkResolver {

    /**
     * Resolves [url], which must be a short link according to `Cleaner.shortLinks`.
     *
     * A failure is reported as [Resolution.Failed] rather than thrown, so that the caller can
     * explain it to the user and fall back to cleaning the short link as it is.
     */
    suspend fun resolve(url: Url): Resolution
}

/** The outcome of [resolving][ShortLinkResolver.resolve] a short link. */
sealed interface Resolution {

    /** The short link leads to [to], which `Cleaner.clean` accepts as one of its `resolved`. */
    data class Resolved(val to: Url) : Resolution

    /** The short link could not be resolved, for the given [reason]. */
    data class Failed(val reason: Reason) : Resolution

    /** Why a short link could not be resolved, detailed enough to tell the user. */
    sealed interface Reason {

        /** The server could not be reached at all, most likely because the device is offline. */
        data object Offline : Reason

        /** The server was reached but did not answer in time. */
        data object Timeout : Reason

        /** The server answered with the HTTP error [status], such as 404 or 403. */
        data class HttpError(val status: Int) : Reason

        /** The server answered, but without redirecting anywhere. */
        data object NoRedirect : Reason

        /**
         * The server redirected to its start page rather than to a target, which is how an expired
         * or unknown short link is answered: `tiktok.com/t/<code>` → `tiktok.com/?_r=1`.
         */
        data object Expired : Reason

        /** Anything else went wrong. */
        data object Unknown : Reason
    }
}
