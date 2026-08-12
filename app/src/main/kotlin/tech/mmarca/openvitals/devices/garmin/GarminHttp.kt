package tech.mmarca.openvitals.devices.garmin

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

/**
 * One request the watch made of the phone through Garmin's HTTP-proxy service.
 *
 * Modern watches (vívoactive 5 era) treat the paired phone as their internet:
 * weather, ephemeris and Connect IQ traffic all arrive as
 * `GdiHttpService.RawRequest` messages expecting a plausible HTTP answer back.
 * Nothing here reaches a network — the app declares no INTERNET permission at
 * all — so every request either terminates in an interceptor or is refused.
 */
data class GarminHttpRequest(
    val url: String,
    val domain: String,
    val path: String,
    val query: Map<String, String>,
    /** Garmin's method enum; 3 is POST. Absent on plain GETs. */
    val method: Int?,
    /** Header names lowercased, since the watch is inconsistent about case. */
    val headers: Map<String, String>,
    val body: ByteArray?,
    /** The watch asked for the body over the data-transfer service instead. */
    val useDataTransfer: Boolean,
)

/**
 * What to answer with. [onSent] fires once the watch actually has the bytes —
 * for a chunked body that is the last chunk, not the first reply — which is
 * the only honest moment to record "the watch has this now".
 */
data class GarminHttpResponse(
    val status: Int = 200,
    val body: ByteArray = ByteArray(0),
    val headers: Map<String, String> = emptyMap(),
    val onSent: (() -> Unit)? = null,
)

/**
 * Answers one family of watch requests. Mirrors upstream Gadgetbridge's
 * `HttpInterceptor`: [supports] claims the request by URL alone, [handle] then
 * either answers it or returns null to refuse.
 */
interface GarminHttpInterceptor {
    fun supports(request: GarminHttpRequest): Boolean
    fun handle(request: GarminHttpRequest): GarminHttpResponse?
}

/**
 * The HTTP-proxy plumbing: unwraps the watch's protobuf, runs the interceptor
 * chain, and frames whatever comes back — inline, gzipped, or handed to the
 * data-transfer service for bodies the watch would rather pull in chunks.
 *
 * Mirrors Gadgetbridge's `HttpHandler` + `DataTransferHandler`.
 */
class GarminHttpProxy(private val interceptors: List<GarminHttpInterceptor>) {

    // ── protobuf field numbers (gdi_http_service / gdi_data_transfer) ───────
    private companion object {
        const val RAW_REQUEST = 5
        const val RAW_RESPONSE = 6
        const val REQ_URL = 1
        const val REQ_METHOD = 3
        const val REQ_HEADER = 5
        const val REQ_USE_DATA_XFER = 6
        const val REQ_RAW_BODY = 7
        const val RESP_STATUS = 1
        const val RESP_HTTP_STATUS = 2
        const val RESP_BODY = 3
        const val RESP_XFER_DATA = 4
        const val RESP_HEADER = 5
        const val HEADER_KEY = 1
        const val HEADER_VALUE = 2
        const val XFER_ID = 1
        const val XFER_SIZE = 2

        const val DOWNLOAD_REQUEST = 1
        const val DOWNLOAD_RESPONSE = 2
        const val DL_REQ_ID = 1
        const val DL_REQ_OFFSET = 2
        const val DL_REQ_MAX_CHUNK = 3
        const val DL_RESP_STATUS = 1
        const val DL_RESP_ID = 2
        const val DL_RESP_OFFSET = 3
        const val DL_RESP_PAYLOAD = 4

        const val STATUS_OK = 100
        const val STATUS_UNKNOWN = 0
        const val DL_SUCCESS = 1
        const val DL_INVALID_ID = 2

        /** Conservative chunk floor when the watch names no maximum. */
        const val DEFAULT_CHUNK = 500
    }

    /** Bodies the watch asked to pull over the data-transfer service. */
    private class PendingTransfer(val data: ByteArray, val onSent: (() -> Unit)?)

    private val transfers = mutableMapOf<Int, PendingTransfer>()
    private var nextTransferId = 1

    /**
     * Handles one watch-initiated `Smart` message. Returns the reply to send
     * under the same request id, or null when the message is not one this
     * responder speaks (someone else's conversation).
     */
    fun handle(payload: ByteArray): ByteArray? {
        val fields = readProtobuf(payload)
        protobufField(fields, GarminSmartService.HTTP)?.bytes?.let { return handleHttp(it) }
        protobufField(fields, GarminSmartService.DATA_TRANSFER)?.bytes
            ?.let { return handleDataTransfer(it) }
        return null
    }

    // ── the HTTP service ────────────────────────────────────────────────────

    private fun handleHttp(service: ByteArray): ByteArray? {
        val raw = protobufField(readProtobuf(service), RAW_REQUEST)?.bytes
        // WebRequest (the pre-2020 shape) is not spoken here; unanswered is
        // wrong for a service we claim, so anything else gets an explicit
        // UNKNOWN back — upstream's no-interceptor answer.
        if (raw == null) {
            GarminLog.log("[GARMIN-HTTP] unsupported http request shape")
            return rawResponseError()
        }
        val request = parseRequest(raw) ?: return rawResponseError()

        val interceptor = interceptors.firstOrNull { it.supports(request) }
        if (interceptor == null) {
            GarminLog.log("[GARMIN-HTTP] not serving ${request.domain}${request.path}")
            return rawResponseError()
        }
        val response = interceptor.handle(request) ?: return rawResponseError()

        return smartHttp(
            ProtobufWriter().nested(RAW_RESPONSE, rawResponse(request, response)).toBytes(),
        )
    }

    private fun parseRequest(raw: ByteArray): GarminHttpRequest? {
        val fields = readProtobuf(raw)
        val url = protobufField(fields, REQ_URL)?.bytes?.toString(Charsets.UTF_8) ?: return null
        val headers = fields.filter { it.field == REQ_HEADER }.mapNotNull { header ->
            val headerFields = readProtobuf(header.bytes ?: return@mapNotNull null)
            val key = protobufField(headerFields, HEADER_KEY)?.bytes?.toString(Charsets.UTF_8)
            val value = protobufField(headerFields, HEADER_VALUE)?.bytes?.toString(Charsets.UTF_8)
            if (key == null || value == null) null else key.lowercase() to value
        }.toMap()
        val (domain, path, query) = parseUrl(url)
        return GarminHttpRequest(
            url = url,
            domain = domain,
            path = path,
            query = query,
            method = protobufField(fields, REQ_METHOD)?.varint?.toInt(),
            headers = headers,
            body = protobufField(fields, REQ_RAW_BODY)?.bytes,
            useDataTransfer = protobufField(fields, REQ_USE_DATA_XFER)?.varint == 1L,
        )
    }

    private fun rawResponse(
        request: GarminHttpRequest,
        response: GarminHttpResponse,
    ): ByteArray {
        val writer = ProtobufWriter()
            .varint(RESP_STATUS, STATUS_OK)
            .varint(RESP_HTTP_STATUS, response.status)
        val headers = response.headers.toMutableMap()

        if (request.useDataTransfer) {
            val id = nextTransferId++
            transfers[id] = PendingTransfer(response.body, response.onSent)
            GarminLog.log(
                "[GARMIN-HTTP] serving ${request.path} " +
                    "(${response.body.size}B as transfer $id)",
            )
            writer.nested(
                RESP_XFER_DATA,
                ProtobufWriter()
                    .varint(XFER_ID, id)
                    .varint(XFER_SIZE, response.body.size)
                    .toBytes(),
            )
        } else {
            var body = response.body
            if (request.headers["accept-encoding"] == "gzip") {
                body = gzip(body)
                headers["Content-Encoding"] = "gzip"
            }
            GarminLog.log("[GARMIN-HTTP] serving ${request.path} (${body.size}B)")
            writer.nested(RESP_BODY, body)
        }

        for ((key, value) in headers) {
            writer.nested(
                RESP_HEADER,
                ProtobufWriter().string(HEADER_KEY, key).string(HEADER_VALUE, value).toBytes(),
            )
        }
        // An inline body is on its way out with this very reply; a chunked one
        // is not delivered until its last chunk, so it reports itself later.
        if (!request.useDataTransfer) response.onSent?.invoke()
        return writer.toBytes()
    }

    private fun rawResponseError(): ByteArray = smartHttp(
        ProtobufWriter()
            .nested(
                RAW_RESPONSE,
                ProtobufWriter().varint(RESP_STATUS, STATUS_UNKNOWN).toBytes(),
            )
            .toBytes(),
    )

    private fun smartHttp(service: ByteArray): ByteArray =
        ProtobufWriter().nested(GarminSmartService.HTTP, service).toBytes()

    // ── the data-transfer service (chunked response bodies) ─────────────────

    private fun handleDataTransfer(service: ByteArray): ByteArray? {
        val request = protobufField(readProtobuf(service), DOWNLOAD_REQUEST)?.bytes ?: return null
        val fields = readProtobuf(request)
        val id = protobufField(fields, DL_REQ_ID)?.varint?.toInt() ?: return null
        val offset = protobufField(fields, DL_REQ_OFFSET)?.varint?.toInt() ?: return null
        val maxChunk = protobufField(fields, DL_REQ_MAX_CHUNK)?.varint?.toInt() ?: DEFAULT_CHUNK

        val transfer = transfers[id]
        val response = ProtobufWriter()
        if (transfer == null || offset > transfer.data.size) {
            GarminLog.log("[GARMIN-HTTP] transfer $id@$offset: no such transfer")
            response.nested(
                DOWNLOAD_RESPONSE,
                ProtobufWriter()
                    .varint(DL_RESP_STATUS, DL_INVALID_ID)
                    .varint(DL_RESP_ID, id)
                    .varint(DL_RESP_OFFSET, offset)
                    .toBytes(),
            )
        } else {
            val data = transfer.data
            val end = minOf(offset + maxChunk, data.size)
            val chunk = data.copyOfRange(offset, end)
            GarminLog.log("[GARMIN-HTTP] transfer $id: $offset..$end of ${data.size}")
            if (end == data.size) {
                // Served in full; the id is never valid again, and only now is
                // it true that the watch has the whole body.
                transfers.remove(id)
                transfer.onSent?.invoke()
            }
            response.nested(
                DOWNLOAD_RESPONSE,
                ProtobufWriter()
                    .varint(DL_RESP_STATUS, DL_SUCCESS)
                    .varint(DL_RESP_ID, id)
                    .varint(DL_RESP_OFFSET, offset)
                    .nested(DL_RESP_PAYLOAD, chunk)
                    .toBytes(),
            )
        }
        return ProtobufWriter()
            .nested(GarminSmartService.DATA_TRANSFER, response.toBytes())
            .toBytes()
    }

    private fun gzip(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }

    private fun parseUrl(url: String): Triple<String, String, Map<String, String>> {
        val withoutScheme = url.substringAfter("://", url)
        val domain = withoutScheme.substringBefore("/")
        val pathAndQuery = withoutScheme.removePrefix(domain)
        val path = pathAndQuery.substringBefore("?")
        val query = pathAndQuery.substringAfter("?", "")
            .split("&")
            .filter { it.isNotBlank() }
            .associate { pair ->
                pair.substringBefore("=") to
                    java.net.URLDecoder.decode(pair.substringAfter("=", ""), "UTF-8")
            }
        return Triple(domain, path, query)
    }
}
