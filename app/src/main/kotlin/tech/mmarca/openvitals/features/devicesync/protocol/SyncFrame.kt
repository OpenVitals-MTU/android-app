package tech.mmarca.openvitals.features.devicesync.protocol

/**
 * Framing for the phone-to-phone sync wire protocol.
 *
 * RFCOMM is a raw byte stream: a peer's `write()` of N bytes does NOT arrive as
 * one read of N bytes — it may be split or coalesced arbitrarily. So every
 * logical message is wrapped in a length-prefixed frame and [SyncFrameReader]
 * reassembles frames from whatever chunk boundaries arrive.
 *
 * Wire format per frame (big-endian):
 *
 * ```
 *   ┌──────────────┬────────┬─────────────────────┐
 *   │ payloadLen   │ type   │ payload             │
 *   │ uint32 (4B)  │ u8 (1B)│ payloadLen bytes    │
 *   └──────────────┴────────┴─────────────────────┘
 * ```
 *
 * The type byte is [SyncFrameType.ordinal]; the enum is append-only so the byte
 * stays stable across versions.
 */

/** The kind of a framed message. APPEND-ONLY — the ordinal is the wire type
 * byte, so never reorder or remove a value. */
enum class SyncFrameType {
    /** Capability + nonce exchange that opens a session. */
    HELLO,

    /** Authentication proof derived from the 6-digit code + both nonces. */
    AUTH,

    /** A gzipped batch of records flowing one direction. */
    BATCH,

    /** Acknowledges a received [BATCH] (stop-and-wait backpressure). */
    BATCH_ACK,

    /** The sender has no more batches to send this session. */
    SEND_DONE,

    /** Cooperative abort (user cancel, or a fatal protocol error). */
    ABORT,
}

/**
 * The largest payload a single frame may carry, a guard against a corrupt or
 * hostile length prefix allocating unbounded memory. Batches are chunked well
 * under this (~64 KB); 16 MiB is generous headroom.
 */
const val MAX_SYNC_FRAME_PAYLOAD: Int = 16 * 1024 * 1024

/**
 * Thrown when the byte stream violates the frame format (bad type byte or an
 * oversized length prefix). Fatal to the session.
 */
class SyncFrameFormatException(message: String) : Exception(message)

/** A single framed message: its [type] and raw [payload] bytes. */
class SyncFrame(val type: SyncFrameType, val payload: ByteArray) {

    /** Encodes this frame to its on-wire bytes (header + payload). */
    fun encode(): ByteArray {
        val out = ByteArray(HEADER_BYTES + payload.size)
        val len = payload.size
        out[0] = (len ushr 24).toByte()
        out[1] = (len ushr 16).toByte()
        out[2] = (len ushr 8).toByte()
        out[3] = len.toByte()
        out[4] = type.ordinal.toByte()
        payload.copyInto(out, HEADER_BYTES)
        return out
    }

    companion object {
        const val HEADER_BYTES: Int = 5
    }
}

/**
 * Reassembles [SyncFrame]s from a stream of arbitrary byte chunks. Feed each
 * inbound chunk to [addChunk]; it returns the frames that completed, buffering
 * any partial trailing frame for the next call.
 */
class SyncFrameReader {
    private var buffer: ByteArray = EMPTY

    /** Number of bytes currently buffered (a partial frame not yet complete). */
    val bufferedBytes: Int get() = buffer.size

    /** Appends [chunk] and returns every frame that is now complete, in order. */
    fun addChunk(chunk: ByteArray): List<SyncFrame> {
        val data = if (buffer.isEmpty()) chunk else buffer + chunk
        val frames = mutableListOf<SyncFrame>()
        var offset = 0
        while (data.size - offset >= SyncFrame.HEADER_BYTES) {
            val payloadLen =
                ((data[offset].toInt() and 0xFF) shl 24) or
                    ((data[offset + 1].toInt() and 0xFF) shl 16) or
                    ((data[offset + 2].toInt() and 0xFF) shl 8) or
                    (data[offset + 3].toInt() and 0xFF)
            if (payloadLen < 0 || payloadLen > MAX_SYNC_FRAME_PAYLOAD) {
                throw SyncFrameFormatException(
                    "frame payload $payloadLen exceeds max $MAX_SYNC_FRAME_PAYLOAD",
                )
            }
            val total = SyncFrame.HEADER_BYTES + payloadLen
            if (data.size - offset < total) break // wait for more bytes
            val typeByte = data[offset + 4].toInt() and 0xFF
            val types = SyncFrameType.entries
            if (typeByte >= types.size) {
                throw SyncFrameFormatException("unknown frame type byte $typeByte")
            }
            val payload = data.copyOfRange(offset + SyncFrame.HEADER_BYTES, offset + total)
            frames += SyncFrame(types[typeByte], payload)
            offset += total
        }
        // Retain only the unconsumed tail.
        buffer = if (offset < data.size) data.copyOfRange(offset, data.size) else EMPTY
        return frames
    }

    private companion object {
        val EMPTY = ByteArray(0)
    }
}
