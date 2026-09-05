package tech.mmarca.openvitals.devices.garmin

import java.io.ByteArrayOutputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * One protobuf exchange with the watch, over GFDI messages 5043/5044. The
 * envelope is `[u16 requestId][u32 dataOffset][u32 totalLength]
 * [u32 chunkLength][bytes]`; replies are matched by request id.
 * Chunking is implemented for receiving only. Sending over the chunk size throws.
 */
class GarminProtobufTransport(
    /** Hands a built GFDI frame to the layer below. */
    private val send: suspend (ByteArray) -> Unit,
    /** Called with a message that answers no outstanding request. */
    var onUnsolicited: ((ByteArray) -> Unit)? = null,
    /** A watch-initiated request, with the id [respond] must use. */
    var onServiceRequest: ((requestId: Int, payload: ByteArray) -> Unit)? = null,
) {

    companion object {
        /** The largest payload the watch accepts in one message, from Gadgetbridge. */
        const val MAX_CHUNK_SIZE = 375

        /** How long to wait for a reply. An unanswered request must not leave a spinner forever. */
        val REPLY_TIMEOUT: Duration = 10.seconds

        private fun hex(bytes: ByteArray): String =
            bytes.joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }

    private var lastRequestId = 0
    private val pending = mutableMapOf<Int, CompletableDeferred<ByteArray>>()

    /**
     * Chunks in flight: request id to offset to bytes. Keyed by offset
     * because the watch retransmits chunks it thinks were lost.
     */
    private val incoming = mutableMapOf<Int, MutableMap<Long, ByteArray>>()

    /** Sends [payload] as a `Smart` message. Returns the reply, or null on timeout. */
    suspend fun request(
        payload: ByteArray,
        label: String? = null,
        timeout: Duration? = null,
    ): ByteArray? {
        require(payload.size <= MAX_CHUNK_SIZE) {
            "Protobuf request is ${payload.size}B, over the $MAX_CHUNK_SIZE B " +
                "the watch accepts in one message, and outbound chunking is " +
                "not implemented."
        }
        val requestId = nextRequestId()
        val deferred = CompletableDeferred<ByteArray>()
        pending[requestId] = deferred

        GarminLog.log(
            "[GARMIN-PB] → ${label ?: "request"} #$requestId (${payload.size}B)",
        )
        send(frame(GarminMessageId.PROTOBUF_REQUEST, requestId, payload))

        try {
            val reply = withTimeoutOrNull(timeout ?: REPLY_TIMEOUT) { deferred.await() }
            if (reply == null) {
                GarminLog.log(
                    "[GARMIN-PB] ✗ no reply to #$requestId within " +
                        "${(timeout ?: REPLY_TIMEOUT).inWholeSeconds}s",
                )
            }
            return reply
        } finally {
            pending.remove(requestId)
            incoming.remove(requestId)
        }
    }

    /**
     * Feeds an inbound protobuf message in. Returns true when consumed. The
     * watch sends requests of its own too.
     */
    suspend fun handleInbound(frame: GarminGfdiFrame): Boolean {
        if (frame.messageType != GarminMessageId.PROTOBUF_REQUEST &&
            frame.messageType != GarminMessageId.PROTOBUF_RESPONSE
        ) {
            return false
        }
        val payload = frame.payload
        if (payload.size < 14) {
            // Malformed, but still ours: ack it or the watch repeats it.
            send(buildGenericAck(frame.messageType))
            return true
        }
        val reader = GarminByteReader(payload)
        val requestId = reader.readShort()
        val dataOffset = reader.readInt()
        val totalLength = reader.readInt()
        val chunkLength = reader.readInt()
        if (payload.size < 14 + chunkLength) {
            send(buildGenericAck(frame.messageType))
            return true
        }
        val bytes = payload.copyOfRange(14, 14 + chunkLength.toInt())

        // Accumulate by id alone, not by whether we wait for it: the watch
        // answers settings requests under its own id.
        if (totalLength != chunkLength || dataOffset != 0L) {
            val chunks = incoming.getOrPut(requestId) { mutableMapOf() }
            chunks[dataOffset] = bytes
            // A chunk needs an ack that names its received offset, not the next
            // one, or the watch resends chunk zero forever.
            send(
                buildProtobufAck(
                    originalMessageType = frame.messageType,
                    requestId = requestId,
                    dataOffset = dataOffset,
                ),
            )
            val held = chunks.values.sumOf { it.size.toLong() }
            if (held < totalLength) {
                GarminLog.log("[GARMIN-PB] ← #$requestId chunk $held/$totalLength B")
                return true
            }
            // Assembled in offset order, so a late retransmission lands where it belongs.
            val assembled = ByteArrayOutputStream()
            for (offset in chunks.keys.sorted()) {
                val chunk = chunks.getValue(offset)
                assembled.write(chunk, 0, chunk.size)
            }
            incoming.remove(requestId)
            deliver(requestId, assembled.toByteArray())
            return true
        }

        // Acked by request id, not just generically, or the watch retransmits
        // every message every five seconds.
        send(
            buildProtobufAck(
                originalMessageType = frame.messageType,
                requestId = requestId,
                dataOffset = dataOffset,
            ),
        )
        deliver(requestId, bytes)
        return true
    }

    /** Hands a complete message to its waiter, or to the unsolicited hook. */
    private fun deliver(requestId: Int, bytes: ByteArray) {
        val deferred = pending[requestId]
        if (deferred != null) {
            GarminLog.log("[GARMIN-PB] ← #$requestId (${bytes.size}B) ${hex(bytes)}")
            deferred.complete(bytes)
            return
        }
        // Not an answer to anything outstanding.
        GarminLog.log(
            "[GARMIN-PB] ← unsolicited #$requestId (${bytes.size}B) ${hex(bytes)}",
        )
        onServiceRequest?.invoke(requestId, bytes)
        onUnsolicited?.invoke(bytes)
    }

    /** Answers a watch-initiated request under the id the watch chose. */
    suspend fun respond(requestId: Int, payload: ByteArray) {
        GarminLog.log("[GARMIN-PB] → response #$requestId (${payload.size}B)")
        send(frame(GarminMessageId.PROTOBUF_RESPONSE, requestId, payload))
    }

    /** Fails every outstanding request, so a dropped link does not wait out the timeout. */
    fun abort() {
        for (deferred in pending.values) {
            deferred.completeExceptionally(IllegalStateException("link closed"))
        }
        pending.clear()
        incoming.clear()
    }

    private fun nextRequestId(): Int {
        lastRequestId = (lastRequestId + 1) % 65536
        return lastRequestId
    }

    private fun frame(messageType: Int, requestId: Int, payload: ByteArray): ByteArray {
        val writer = GarminByteWriter()
            .writeShort(requestId)
            .writeInt(0) // dataOffset — single chunk
            .writeInt(payload.size) // total
            .writeInt(payload.size) // this chunk
            .writeBytes(payload)
        return GarminGfdiFrame.build(messageType, writer.toBytes())
    }
}

/** What the watch said about a find request. */
enum class GarminFindOutcome {
    /** It answered OK. */
    OK,

    /** It answered ERROR, the only reading that means it declined. */
    ERROR,

    /** Unrecognised or no answer. Treated as "probably ringing". */
    UNKNOWN,
    ;

    val declined: Boolean get() = this == ERROR
}

/**
 * The battery half of Garmin's `DeviceStatusService`, the only place the
 * watch reports a percentage. The GFDI message is just good/ok/low.
 */
object GarminDeviceStatus {

    private const val BATTERY_REQUEST = 2
    private const val BATTERY_RESPONSE = 3
    private const val LEVEL = 2

    /** Asks for the current battery level (the request message is empty). */
    fun batteryRequest(): ByteArray {
        val service = ProtobufWriter().emptyMessage(BATTERY_REQUEST).toBytes()
        return ProtobufWriter().nested(GarminSmartService.DEVICE_STATUS, service).toBytes()
    }

    /** The percentage in [reply], or null. */
    fun batteryLevel(reply: ByteArray?): Int? {
        if (reply == null || reply.isEmpty()) return null
        val service = protobufField(
            readProtobuf(reply),
            GarminSmartService.DEVICE_STATUS,
        )?.bytes ?: return null
        val response = protobufField(readProtobuf(service), BATTERY_RESPONSE)?.bytes
            ?: return null
        val level = protobufField(readProtobuf(response), LEVEL)?.varint ?: return null
        return level.toInt().takeIf { it in 0..100 }
    }
}

object GarminFindMyWatch {

    private const val FIND_REQUEST = 1
    private const val FIND_RESPONSE = 2
    private const val CANCEL_REQUEST = 3
    private const val CANCEL_RESPONSE = 4
    private const val TIMEOUT = 1
    private const val STATUS = 1

    /** Gadgetbridge's value: long enough to find a watch, short enough to stop itself. */
    val defaultTimeout: Duration = 60.seconds

    fun start(timeout: Duration = defaultTimeout): ByteArray {
        val request = ProtobufWriter().varint(TIMEOUT, timeout.inWholeSeconds).toBytes()
        val service = ProtobufWriter().nested(FIND_REQUEST, request).toBytes()
        return ProtobufWriter().nested(GarminSmartService.FIND_MY_WATCH, service).toBytes()
    }

    fun cancel(): ByteArray {
        val service = ProtobufWriter().emptyMessage(CANCEL_REQUEST).toBytes()
        return ProtobufWriter().nested(GarminSmartService.FIND_MY_WATCH, service).toBytes()
    }

    /** Whether [payload] is the watch reporting on a find. */
    fun isFindMessage(payload: ByteArray): Boolean =
        protobufField(readProtobuf(payload), GarminSmartService.FIND_MY_WATCH) != null

    /**
     * What a reply says about the request. Only an explicit ERROR means the
     * watch declined; an unparsed reply was seen while the watch rang.
     */
    fun outcome(reply: ByteArray?): GarminFindOutcome {
        if (reply == null || reply.isEmpty()) return GarminFindOutcome.UNKNOWN
        val service = protobufField(
            readProtobuf(reply),
            GarminSmartService.FIND_MY_WATCH,
        )
        val bytes = service?.bytes ?: return GarminFindOutcome.UNKNOWN
        val fields = readProtobuf(bytes)
        // A find is answered in field 2, a cancel in field 4; either is an answer.
        for (field in intArrayOf(FIND_RESPONSE, CANCEL_RESPONSE)) {
            val response = protobufField(fields, field)?.bytes ?: continue
            val status = protobufField(readProtobuf(response), STATUS)?.varint
            // A real watch answers with no status field. OK is 100 and ERROR is
            // 200, so a missing status must never read as zero.
            if (status == null || status == 100L) return GarminFindOutcome.OK
            if (status == 200L) return GarminFindOutcome.ERROR
            return GarminFindOutcome.UNKNOWN
        }
        return GarminFindOutcome.UNKNOWN
    }
}
