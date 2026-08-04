package tech.mmarca.openvitals.devices.garmin

import java.io.ByteArrayOutputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * One protobuf exchange with the watch, over GFDI messages 5043/5044.
 *
 * The envelope is `[u16 requestId][u32 dataOffset][u32 totalLength]
 * [u32 chunkLength][bytes]`, and a reply is matched to its request by the id
 * — the watch answers out of band, whenever it feels like it, so there is
 * nothing else to correlate on.
 *
 * Chunking is implemented for RECEIVING only. Every request this app sends is
 * a few dozen bytes, far under the 375-byte chunk the watch accepts, so the
 * outbound half would be untestable code written against a case that cannot
 * currently arise. Sending something larger throws rather than silently
 * truncating.
 */
class GarminProtobufTransport(
    /** Hands a built GFDI frame to the layer below. */
    private val send: suspend (ByteArray) -> Unit,
    /**
     * Called with a message the watch sent on its own account — one that
     * answers no outstanding request. The watch narrates state changes this
     * way, so a caller waiting on something can learn it has already
     * happened.
     */
    var onUnsolicited: ((ByteArray) -> Unit)? = null,
) {

    companion object {
        /**
         * The largest payload the watch accepts in one message, from
         * Gadgetbridge's `ProtocolBufferHandler` (measured on a Vívomove
         * Style).
         */
        const val MAX_CHUNK_SIZE = 375

        /**
         * How long to wait for a reply before giving up on it.
         *
         * A request the watch never answers must not leak its waiter: the
         * caller is usually a button, and a button that never resolves leaves
         * a spinner on screen forever.
         */
        val REPLY_TIMEOUT: Duration = 10.seconds

        private fun hex(bytes: ByteArray): String =
            bytes.joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }

    private var lastRequestId = 0
    private val pending = mutableMapOf<Int, CompletableDeferred<ByteArray>>()

    /**
     * Chunks in flight: request id → offset → bytes.
     *
     * Keyed by OFFSET rather than appended, because the watch retransmits
     * chunks it thinks were not acknowledged. Appending them grew a 1017-byte
     * message to 1461 and only parsed by luck, protobuf reading the declared
     * length and ignoring the tail.
     */
    private val incoming = mutableMapOf<Int, MutableMap<Long, ByteArray>>()

    /**
     * Sends [payload] as a `Smart` message and waits for the watch's reply.
     *
     * Returns the reply's protobuf bytes, or null when it does not arrive in
     * time — a caller that only wants fire-and-forget can ignore the result.
     */
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
     * Feeds an inbound protobuf message in. Returns true when it was consumed.
     *
     * Both 5043 and 5044 land here: the watch sends REQUESTS of its own as
     * well as responses, and an unmatched request id simply means it started
     * the conversation rather than answering ours.
     */
    suspend fun handleInbound(frame: GarminGfdiFrame): Boolean {
        if (frame.messageType != GarminMessageId.PROTOBUF_REQUEST &&
            frame.messageType != GarminMessageId.PROTOBUF_RESPONSE
        ) {
            return false
        }
        val payload = frame.payload
        if (payload.size < 14) {
            // Malformed, but still ours — acknowledge it or the watch repeats it.
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

        // Chunked, whoever it belongs to. Accumulation is keyed on the id
        // alone, NOT on whether we are waiting for that id: the watch answers
        // a settings request under an id OF ITS OWN rather than echoing ours,
        // so treating an unmatched id as unchunked lost every screen after the
        // first 487 bytes.
        if (totalLength != chunkLength || dataOffset != 0L) {
            val chunks = incoming.getOrPut(requestId) { mutableMapOf() }
            chunks[dataOffset] = bytes
            // A chunk needs an acknowledgement that names it, or the watch
            // never sends the next one.
            // The offset AS RECEIVED, not the next one. Gadgetbridge echoes
            // what the chunk declared; acknowledging `dataOffset +
            // chunkLength` instead left the watch resending chunk zero
            // forever, because it never saw an acknowledgement for the chunk
            // it had actually sent.
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
            // Assembled in offset order, so a retransmission that arrived
            // late lands where it belongs rather than at the end.
            val assembled = ByteArrayOutputStream()
            for (offset in chunks.keys.sorted()) {
                val chunk = chunks.getValue(offset)
                assembled.write(chunk, 0, chunk.size)
            }
            incoming.remove(requestId)
            deliver(requestId, assembled.toByteArray())
            return true
        }

        // Complete in one message — and still acknowledged BY REQUEST ID, not
        // just generically. A generic ack says the frame arrived; the watch
        // also wants to hear that the protobuf message itself was kept, and
        // without that it retransmitted every message it had ever sent us,
        // every five seconds, for as long as the link stayed open. That storm
        // is what let a stale reply arrive while a different request was
        // pending.
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

    /**
     * Hands a COMPLETE message to whoever is waiting for it, or to the
     * unsolicited hook when nobody is.
     */
    private fun deliver(requestId: Int, bytes: ByteArray) {
        val deferred = pending[requestId]
        if (deferred != null) {
            GarminLog.log("[GARMIN-PB] ← #$requestId (${bytes.size}B) ${hex(bytes)}")
            deferred.complete(bytes)
            return
        }
        // Not an answer to anything outstanding — either the watch started
        // this conversation, or it answered one of ours under its own id.
        GarminLog.log(
            "[GARMIN-PB] ← unsolicited #$requestId (${bytes.size}B) ${hex(bytes)}",
        )
        onUnsolicited?.invoke(bytes)
    }

    /**
     * Fails every outstanding request, so a dropped link does not leave a
     * caller waiting out the full timeout for a reply that can never come.
     */
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

    /** It answered ERROR — the only reading that means the watch declined. */
    ERROR,

    /**
     * It answered something this app does not recognise, or did not answer.
     * Treated as "probably ringing", because it demonstrably can be.
     */
    UNKNOWN,
    ;

    val declined: Boolean get() = this == ERROR
}

/**
 * Builds the `Smart` message that starts a find, and the one that stops it.
 *
 * Find is a TOGGLE, not a one-shot: the request carries a timeout in seconds
 * and there is a matching cancel, so the watch alerts for that long unless
 * stopped. Field numbers from `gdi_find_my_watch.proto`.
 */
object GarminFindMyWatch {

    private const val FIND_REQUEST = 1
    private const val FIND_RESPONSE = 2
    private const val CANCEL_REQUEST = 3
    private const val CANCEL_RESPONSE = 4
    private const val TIMEOUT = 1
    private const val STATUS = 1

    /**
     * Gadgetbridge's value, and a sensible one: long enough to find a watch
     * down the back of a sofa, short enough that a forgotten alert stops
     * itself.
     */
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

    /**
     * Whether [payload] is the watch reporting on a find, rather than one of
     * the many other things it narrates unprompted.
     */
    fun isFindMessage(payload: ByteArray): Boolean =
        protobufField(readProtobuf(payload), GarminSmartService.FIND_MY_WATCH) != null

    /**
     * What a reply says about the request — including "it did not say".
     *
     * Three outcomes, not two. The watch was observed to ring while this code
     * read its reply as a refusal, and treating "I could not parse that" as
     * failure is what left it ringing with the phone convinced nothing had
     * happened. Only an explicit ERROR means the watch declined.
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
            // A real vívoactive 5 answers `62 02 12 00` — the response message
            // with NO status field at all, which the schema allows since
            // status is optional. So the presence of the response IS the
            // acknowledgement, and only an explicit ERROR is a refusal. OK is
            // 100 and ERROR is 200, not 0 and 1, so a missing status must
            // never be read as zero.
            if (status == null || status == 100L) return GarminFindOutcome.OK
            if (status == 200L) return GarminFindOutcome.ERROR
            return GarminFindOutcome.UNKNOWN
        }
        return GarminFindOutcome.UNKNOWN
    }
}
