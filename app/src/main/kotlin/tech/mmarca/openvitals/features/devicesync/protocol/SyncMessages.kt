package tech.mmarca.openvitals.features.devicesync.protocol

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Typed payloads carried inside [SyncFrame]s, with their (de)serialization.
 *
 * Small control messages (Hello, Auth, BatchAck, Abort) are compact JSON.
 * Record [SyncBatch]es — the only large payloads — are JSON then gzipped, since
 * a batch of health records compresses well and the link is slow.
 */

/**
 * The protocol version both peers announce in [SyncHello]. Bump on any wire
 * change; the session refuses a peer on a different version.
 */
const val SYNC_PROTOCOL_VERSION: Int = 1

/**
 * The largest a decompressed batch may be. A batch is JSON of at most a few
 * hundred records; 64 MiB is ample headroom, but the hard cap turns a gzip bomb
 * into a bounded [SyncMessageFormatException] instead of an out-of-memory crash.
 */
const val MAX_DECOMPRESSED_BATCH_BYTES: Int = 64 * 1024 * 1024

/** Thrown when a message payload cannot be decoded. Fatal to the session. */
class SyncMessageFormatException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * One record to sync: an opaque serialized [payload] plus the deterministic
 * content [key] used for dedup, tagged with its Health Connect [recordType]
 * (e.g. `StepsRecord`) so the receiver can group and route it.
 *
 * The protocol treats [payload] as opaque bytes — the health-record codec
 * fills it in. Dedup is entirely by [key].
 */
class SyncItem(
    val key: String,
    val recordType: String,
    val payload: ByteArray,
) {
    internal fun toJson(): JsonObject = buildJsonObject {
        put("k", key)
        put("t", recordType)
        put("p", Base64.getEncoder().encodeToString(payload))
    }

    internal companion object {
        fun fromJson(json: JsonObject): SyncItem = SyncItem(
            key = json.string("k"),
            recordType = json.string("t"),
            payload = Base64.getDecoder().decode(json.string("p")),
        )
    }
}

/** The opening capability + nonce exchange. Each peer sends one. */
class SyncHello(
    val protocolVersion: Int,
    val deviceName: String,
    /**
     * The peer's installed Health Connect provider version code, or null if
     * unknown. Informational — surfaced in the report, not used for gating.
     */
    val hcProviderVersion: Long?,
    /**
     * Record types this device supports (already filtered through the local
     * permission/provider gate). The syncable set is the intersection of both
     * peers' lists.
     */
    val supportedTypes: List<String>,
    /** This peer's 256-bit session nonce. */
    val nonce: ByteArray,
) {
    fun encode(): ByteArray = jsonToBytes(
        buildJsonObject {
            put("v", protocolVersion)
            put("name", deviceName)
            if (hcProviderVersion != null) put("hc", hcProviderVersion) else put("hc", JsonNull)
            put("types", buildJsonArray { supportedTypes.forEach { add(it) } })
            put("nonce", Base64.getEncoder().encodeToString(nonce))
        },
    )

    companion object {
        fun decode(bytes: ByteArray): SyncHello = decoding("hello") {
            val json = bytesToJson(bytes)
            SyncHello(
                protocolVersion = json.getValue("v").jsonPrimitive.int,
                deviceName = json.string("name"),
                hcProviderVersion = json["hc"]
                    ?.takeIf { it !is JsonNull }
                    ?.jsonPrimitive?.content?.toLong(),
                supportedTypes = json.getValue("types").jsonArray
                    .map { it.jsonPrimitive.content },
                nonce = Base64.getDecoder().decode(json.string("nonce")),
            )
        }
    }
}

/** The authentication proof (HMAC over the peer's nonce). One per peer. */
class SyncAuthProof(val proof: ByteArray) {
    fun encode(): ByteArray = jsonToBytes(
        buildJsonObject { put("proof", Base64.getEncoder().encodeToString(proof)) },
    )

    companion object {
        fun decode(bytes: ByteArray): SyncAuthProof = decoding("auth") {
            SyncAuthProof(Base64.getDecoder().decode(bytesToJson(bytes).string("proof")))
        }
    }
}

/**
 * A gzipped batch of records flowing one direction, tagged with a monotonic
 * [seq] the receiver echoes in a [SyncBatchAck].
 */
class SyncBatch(val seq: Int, val items: List<SyncItem>) {

    fun encode(): ByteArray {
        val json = buildJsonObject {
            put("seq", seq)
            put("items", buildJsonArray { items.forEach { add(it.toJson()) } })
        }
        val bytes = jsonToBytes(json)
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(bytes) }
        return out.toByteArray()
    }

    companion object {
        fun decode(bytes: ByteArray): SyncBatch = decoding("batch") {
            // Inflate through a size-capped loop: gzip's ratio reaches ~1000:1,
            // so a 16 MiB frame (the frame cap) could inflate toward GiB.
            // Decoding is chunked, so this throws as soon as output crosses the
            // cap — the bomb never fully materializes. The cap is far above any
            // real batch.
            val inflated = ByteArrayOutputStream()
            GZIPInputStream(ByteArrayInputStream(bytes)).use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    inflated.write(buffer, 0, n)
                    if (inflated.size() > MAX_DECOMPRESSED_BATCH_BYTES) {
                        throw SyncMessageFormatException("decompressed batch exceeds cap")
                    }
                }
            }
            val json = bytesToJson(inflated.toByteArray())
            SyncBatch(
                seq = json.getValue("seq").jsonPrimitive.int,
                items = json.getValue("items").jsonArray.map { SyncItem.fromJson(it.jsonObject) },
            )
        }
    }
}

/**
 * Acknowledges the batch with the given [seq] — the stop-and-wait signal the
 * sender waits for before sending the next batch.
 */
class SyncBatchAck(val seq: Int) {
    fun encode(): ByteArray = jsonToBytes(buildJsonObject { put("seq", seq) })

    companion object {
        fun decode(bytes: ByteArray): SyncBatchAck = decoding("batchAck") {
            SyncBatchAck(bytesToJson(bytes).getValue("seq").jsonPrimitive.int)
        }
    }
}

/** A cooperative abort with a human-readable [reason] for the report. */
class SyncAbort(val reason: String) {
    fun encode(): ByteArray = jsonToBytes(buildJsonObject { put("reason", reason) })

    companion object {
        fun decode(bytes: ByteArray): SyncAbort = SyncAbort(
            runCatching { bytesToJson(bytes).string("reason") }.getOrDefault("unknown"),
        )
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private val json = Json { ignoreUnknownKeys = true }

private fun jsonToBytes(element: JsonObject): ByteArray =
    element.toString().toByteArray(Charsets.UTF_8)

private fun bytesToJson(bytes: ByteArray): JsonObject =
    json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject

private fun JsonObject.string(key: String): String {
    val value = getValue(key)
    if (value is JsonArray || value is JsonObject || value is JsonNull) {
        throw SyncMessageFormatException("field $key is not a string")
    }
    return value.jsonPrimitive.content
}

/**
 * Wraps a decode so ANY failure — malformed JSON, a wrong-typed or missing
 * field from a buggy/hostile peer — surfaces as one typed
 * [SyncMessageFormatException] the session turns into a clean abort.
 */
private inline fun <T> decoding(what: String, block: () -> T): T =
    try {
        block()
    } catch (e: SyncMessageFormatException) {
        throw e
    } catch (e: Exception) {
        throw SyncMessageFormatException("malformed $what payload: ${e.message}", e)
    }
