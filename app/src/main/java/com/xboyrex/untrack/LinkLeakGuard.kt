package com.xboyrex.untrack

import java.util.Locale

enum class LeakType(val label: String, val description: String) {
    CREDENTIALS("Embedded Credentials", "URL contains username:password@ authentication data"),
    EMAIL("Email Address", "URL exposes a personal email address in query or path"),
    JWT("JWT Bearer Token", "URL contains a signed JSON Web Token (eyJ...)"),
    SENSITIVE_TOKEN("Private Auth / Session Token", "URL contains a private API key, session token, or invite secret"),
    PRECISE_GPS("Precise GPS Coordinates", "URL query contains high-precision latitude & longitude coordinates")
}

data class LeakWarning(
    val type: LeakType,
    val parameterName: String? = null
)

object LinkLeakGuard {

    private val EMAIL_REGEX = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val JWT_REGEX = Regex("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")
    private val COORD_REGEX = Regex("[-+]?\\d+\\.\\d{4,}")

    private val SENSITIVE_KEYS = setOf(
        "access_token", "refresh_token", "id_token", "token", "auth", "authorization",
        "auth_token", "otp", "one_time_code", "reset", "reset_token", "password_reset_token",
        "invite", "invite_code", "invitation_token", "api_key", "apikey", "secret",
        "client_secret", "session", "session_id", "session_token", "sessionid", "signature",
        "sig", "private_token", "share_token", "confirmation_token", "unlock_token",
        "recovery_token", "bearer"
    )

    private val LAT_KEYS = setOf("lat", "latitude")
    private val LON_KEYS = setOf("lon", "lng", "longitude")

    fun inspect(url: ParsedUrl): List<LeakWarning> {
        val warnings = mutableListOf<LeakWarning>()

        if (!url.userInfo.isNullOrBlank()) {
            warnings += LeakWarning(LeakType.CREDENTIALS)
        }

        val decodedPath = ParsedUrl.percentDecodeOnce(url.path)
        if (EMAIL_REGEX.containsMatchIn(decodedPath)) {
            warnings += LeakWarning(LeakType.EMAIL)
        }
        if (JWT_REGEX.containsMatchIn(decodedPath)) {
            warnings += LeakWarning(LeakType.JWT)
        }

        var hasLat: String? = null
        var hasLon: String? = null

        for (param in url.parameters) {
            val keyLower = param.name.lowercase(Locale.ROOT)
            val valDecoded = ParsedUrl.percentDecodeOnce(param.value.orEmpty())

            if (EMAIL_REGEX.containsMatchIn(valDecoded)) {
                warnings += LeakWarning(LeakType.EMAIL, param.name)
            }
            if (JWT_REGEX.containsMatchIn(valDecoded)) {
                warnings += LeakWarning(LeakType.JWT, param.name)
            }
            if (keyLower in SENSITIVE_KEYS && valDecoded.length >= 8) {
                warnings += LeakWarning(LeakType.SENSITIVE_TOKEN, param.name)
            }
            if (keyLower in LAT_KEYS && COORD_REGEX.matches(valDecoded)) {
                hasLat = param.name
            }
            if (keyLower in LON_KEYS && COORD_REGEX.matches(valDecoded)) {
                hasLon = param.name
            }
        }

        if (hasLat != null && hasLon != null) {
            warnings += LeakWarning(LeakType.PRECISE_GPS, "$hasLat, $hasLon")
        }

        return warnings.distinctBy { it.type to it.parameterName }
    }

    fun redactLeaks(url: ParsedUrl): ParsedUrl {
        val warnings = inspect(url)
        val sensitiveParams = warnings.mapNotNull { it.parameterName }
            .flatMap { it.split(",").map(String::trim) }
            .map { it.lowercase(Locale.ROOT) }
            .toSet()

        return url.copy(
            userInfo = null,
            parameters = url.parameters.filterNot { param ->
                val keyLower = param.name.lowercase(Locale.ROOT)
                val valDecoded = ParsedUrl.percentDecodeOnce(param.value.orEmpty())
                keyLower in sensitiveParams ||
                    keyLower in SENSITIVE_KEYS ||
                    EMAIL_REGEX.containsMatchIn(valDecoded) ||
                    JWT_REGEX.containsMatchIn(valDecoded)
            }
        )
    }
}
