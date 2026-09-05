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
 * Typed payloads inside [SyncFrame]s. Control messages are compact JSON;
 * record batches are JSON then gzipped.
 */

/** The protocol version in [SyncHello]. Bump on any wire change. */
const val SYNC_PROTOCOL_VERSION: Int = 1

/** The largest a decompressed batch may be. Turns a gzip bomb into a bounded exception. */
const val MAX_DECOMPRESSED_BATCH_BYTES: Int = 64 * 1024 * 1024

/** Thrown when a message payload cannot be decoded. Fatal to the session. */
class SyncMessageFormatException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * One record to sync: an opaque [payload], the content [key] for dedup and
 * its [recordType]. [originPackage] rides outside both, so it never touches
 * the fingerprint.
 */
class SyncItem(
    val key: String,
    val recordType: String,
    val payload: ByteArray,
    /**
     * The app that originally recorded this data, preserved through chains.
     * Optional on the wire, so mixed-version pairs still sync.
     */
    val originPackage: String? = null,
) {
    internal fun toJson(): JsonObject = buildJsonObject {
        put("k", key)
        put("t", recordType)
        put("p", Base64.getEncoder().encodeToString(payload))
        if (originPackage != null) put("o", originPackage)
    }

    internal companion object {
        fun fromJson(json: JsonObject): SyncItem = SyncItem(
            key = json.string("k"),
            recordType = json.string("t"),
            payload = Base64.getDecoder().decode(json.string("p")),
            originPackage = json["o"]
                ?.takeIf { it !is JsonNull }
                ?.jsonPrimitive?.content,
        )
    }
}

/** The opening capability + nonce exchange. Each peer sends one. */
class SyncHello(
    val protocolVersion: Int,
    val deviceName: String,
    /** The peer's Health Connect provider version, or null. Informational. */
    val hcProviderVersion: Long?,
    /** Record types this device supports. The syncable set is the intersection. */
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

/** A gzipped batch flowing one way, with a [seq] the receiver echoes in a [SyncBatchAck]. */
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
            // Inflate through a size-capped loop, so a gzip bomb never materializes.
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

/** Acknowledges batch [seq]: the stop-and-wait signal. */
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

// Helpers.

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

/** Wraps a decode so any failure surfaces as one [SyncMessageFormatException]. */
private inline fun <T> decoding(what: String, block: () -> T): T =
    try {
        block()
    } catch (e: SyncMessageFormatException) {
        throw e
    } catch (e: Exception) {
        throw SyncMessageFormatException("malformed $what payload: ${e.message}", e)
    }
