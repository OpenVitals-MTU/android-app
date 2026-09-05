package tech.mmarca.openvitals.devices.garmin

import org.json.JSONObject

/**
 * Answers the watch's OAuth exchanges with fabricated tokens. A watch whose
 * exchange fails considers itself signed out and never fetches weather.
 * The tokens only ever reach this app. Mirrors Gadgetbridge.
 */
class GarminOauthInterceptor : GarminHttpInterceptor {

    override fun supports(request: GarminHttpRequest): Boolean =
        request.path.startsWith("/api/oauth/") ||
            request.path.startsWith("/oauth/") ||
            request.path.startsWith("/oauthTokenExchangeService/")

    override fun handle(request: GarminHttpRequest): GarminHttpResponse? {
        val json = oauthJson(request.path, request.method, request.body) ?: return null
        return GarminHttpResponse(
            body = json.toByteArray(Charsets.UTF_8),
            headers = mapOf("Content-Type" to "application/json"),
        )
    }

    /** The scopes upstream fabricates from real-device dumps. An empty scope reads as broken. */
    private val oauthScopes = listOf(
        "GCS_EPHEMERIS_SONY_READ",
        "GCS_CIQ_APPSTORE_MOBILE_READ",
        "GCS_EMERGENCY_ASSISTANCE_CREATE",
        "GCS_GEOLOCATION_ELEVATION_READ",
        "GCS_IMAGE_READ",
        "GCS_LIVETRACK_FIT_CREATE",
        "GCS_LIVETRACK_FIT_READ",
        "GCS_LIVETRACK_FIT_UPDATE",
        "OMT_GOLF_SUBSCRIPTION_READ",
        "OMT_SUBSCRIPTION_READ",
    )

    /** The token endpoints themselves, upstream's shapes exactly. */
    private fun oauthJson(path: String, method: Int?, rawBody: ByteArray?): String? {
        if (method != null && method != METHOD_POST) {
            GarminLog.log("[GARMIN-HTTP] OAuth $path with unexpected method $method")
            return null
        }
        return when (path) {
            "/oauthTokenExchangeService/connectToIT" -> {
                GarminLog.log("[GARMIN-HTTP] issuing fake OAuth tokens ($path)")
                JSONObject().apply {
                    put("accessToken", java.util.UUID.randomUUID().toString())
                    put("tokenType", "Bearer")
                    put("refreshToken", java.util.UUID.randomUUID().toString())
                    put("expiresIn", 7_776_000)
                    put("scope", oauthScopes.joinToString(" "))
                    put("refreshTokenExpiresIn", "31536000")
                    put("customerId", java.util.UUID.randomUUID().toString())
                }.toString()
            }
            "/api/oauth/token", "/oauth/refresh_token/token" -> {
                GarminLog.log("[GARMIN-HTTP] refreshing fake OAuth tokens ($path)")
                // Keep the watch's refresh token when it sent one.
                val refreshToken = rawBody
                    ?.toString(Charsets.UTF_8)
                    ?.split("&")
                    ?.mapNotNull { pair ->
                        val parts = pair.split("=", limit = 2)
                        if (parts.size == 2) parts[0] to parts[1] else null
                    }
                    ?.toMap()
                    ?.get("refresh_token")
                    ?: java.util.UUID.randomUUID().toString()
                JSONObject().apply {
                    put("access_token", java.util.UUID.randomUUID().toString())
                    put("token_type", "Bearer")
                    put("expires_in", 7_776_000)
                    put("scope", oauthScopes.joinToString(" "))
                    put("refresh_token", refreshToken)
                    put("refresh_token_expires_in", "31536000")
                    put("customerId", java.util.UUID.randomUUID().toString())
                }.toString()
            }
            else -> {
                GarminLog.log("[GARMIN-HTTP] unknown OAuth path $path")
                null
            }
        }
    }

    private companion object {
        /** Garmin's method enum; the token endpoints are POST-only. */
        const val METHOD_POST = 3
    }
}
