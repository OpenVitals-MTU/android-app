package tech.mmarca.openvitals.devices.garmin

import java.security.MessageDigest

/** What the phone can offer for ephemeris. Callbacks, so this layer stays protocol-only. */
class GarminAgpsSource(
    /** The bytes held for [GarminAgpsKind], or null if the user supplied none. */
    val load: (GarminAgpsKind) -> ByteArray?,
    /** The watch asked for this URL — recorded so the settings screen can say so. */
    val onRequested: (url: String, kind: GarminAgpsKind?) -> Unit = { _, _ -> },
    /** The watch now has the data. */
    val onServed: (GarminAgpsKind) -> Unit = {},
    /** Held data exists but cannot be used; [reason] is shown to the user. */
    val onRejected: (kind: GarminAgpsKind, reason: String) -> Unit = { _, _ -> },
)

/**
 * Serves user-supplied GPS ephemeris to the watch. This app has no
 * INTERNET permission, so the user fetches the file, as with Gadgetbridge.
 * Mirrors upstream's `AgpsInterceptor`, etag/304 handling included.
 */
class GarminAgpsInterceptor(private val source: GarminAgpsSource) : GarminHttpInterceptor {

    override fun supports(request: GarminHttpRequest): Boolean =
        request.domain == EPHEMERIS_DOMAIN && request.path.startsWith(EPHEMERIS_PATH)

    override fun handle(request: GarminHttpRequest): GarminHttpResponse? {
        val kind = kindOf(request)
        source.onRequested(request.url, kind)
        if (kind == null) {
            GarminLog.log("[GARMIN-AGPS] refusing unknown ephemeris url ${request.path}")
            return null
        }

        val bytes = source.load(kind)
        if (bytes == null) {
            GarminLog.log("[GARMIN-AGPS] watch asked for $kind; nothing imported")
            return null
        }

        val constellations = request.query[QUERY_CONSTELLATIONS]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            .orEmpty()
        if (!GarminAgpsFile.isValid(bytes, kind, constellations)) {
            // Wrong or stale data is worse for the watch than none.
            source.onRejected(kind, REASON_UNUSABLE)
            return null
        }

        val etag = "\"" + md5Hex(bytes) + "\""
        if (request.headers[HEADER_IF_NONE_MATCH] == etag) {
            GarminLog.log("[GARMIN-AGPS] watch already has this ephemeris")
            return GarminHttpResponse(
                status = 304,
                headers = mapOf("etag" to etag),
                // The watch has it: that is the point of the 304.
                onSent = { source.onServed(kind) },
            )
        }

        GarminLog.log("[GARMIN-AGPS] serving ${bytes.size}B of $kind ephemeris")
        return GarminHttpResponse(
            body = bytes,
            headers = mapOf(
                "etag" to etag,
                "cache-control" to "max-age=14400",
                "Content-Type" to (request.headers["accept"] ?: "application/octet-stream"),
            ),
            onSent = { source.onServed(kind) },
        )
    }

    /** Which shape the watch asks for. The URL says it. */
    private fun kindOf(request: GarminHttpRequest): GarminAgpsKind? = when {
        request.query.containsKey(QUERY_CONSTELLATIONS) -> GarminAgpsKind.CONSTELLATION_TAR
        request.path.contains("/rxnetworks/") -> GarminAgpsKind.RX_NETWORKS
        request.path.startsWith("/ephemeris/cpe/sony") -> GarminAgpsKind.SONY_CPE
        else -> null
    }

    private fun md5Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("MD5").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val EPHEMERIS_DOMAIN = "api.gcs.garmin.com"
        const val EPHEMERIS_PATH = "/ephemeris/"
        const val QUERY_CONSTELLATIONS = "constellations"
        const val HEADER_IF_NONE_MATCH = "if-none-match"
        const val REASON_UNUSABLE = "unusable"
    }
}
