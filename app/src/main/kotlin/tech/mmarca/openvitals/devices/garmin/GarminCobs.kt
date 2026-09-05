package tech.mmarca.openvitals.devices.garmin

import java.io.ByteArrayOutputStream

/**
 * Garmin's COBS variant, from Gadgetbridge's `CobsCoDec` (AGPLv3). Not
 * textbook: a leading and a trailing `0x00`, and an extra `0x01` group when
 * the payload ends in a zero. Ported literally. [encode] is pure;
 * [GarminCobsDecoder] streams, since BLE delivers arbitrary chunks.
 */
object GarminCobs {

    /** Encodes one GFDI packet into a wire frame: `00 <cobs groups> 00`. */
    fun encode(data: ByteArray): ByteArray {
        // Worst case is about 2x plus the delimiters.
        val out = ByteArrayOutputStream(data.size + data.size / 254 + 4)
        out.write(0) // Garmin leading pad.
        var lastByteWasZero = false
        var pos = 0
        while (pos < data.size) {
            val start = pos
            var zeroIndex = pos
            while (zeroIndex < data.size && data[zeroIndex] != 0.toByte()) {
                zeroIndex++
            }
            // Only a zero advances `pos` past `zeroIndex`, which sets the trailing-zero flag.
            lastByteWasZero = zeroIndex < data.size

            var payloadSize = zeroIndex - start
            var blockStart = start
            while (payloadSize >= 0xFE) {
                out.write(0xFF) // Max-length group: 254 literal bytes, no implied 0.
                out.write(data, blockStart, 0xFE)
                payloadSize -= 0xFE
                blockStart += 0xFE
            }
            out.write(payloadSize + 1)
            out.write(data, blockStart, payloadSize)

            pos = zeroIndex + if (lastByteWasZero) 1 else 0
        }

        if (lastByteWasZero) out.write(0x01)
        out.write(0) // Trailing delimiter.
        return out.toByteArray()
    }
}

/** Reassembles COBS frames from a chunked byte stream. Stateful, single-consumer. */
class GarminCobsDecoder {

    private val buffer = ByteArrayOutputStream()

    /** Appends received [bytes] to the internal buffer. */
    fun addBytes(bytes: ByteArray) {
        buffer.write(bytes, 0, bytes.size)
    }

    /**
     * The next complete packet, or null. Call until null: one chunk can
     * complete several frames.
     */
    fun pull(): ByteArray? {
        val data = buffer.toByteArray()
        if (data.size < 4) return null // Min frame: pad + group + byte + delim.
        if (data[0] != 0.toByte()) {
            // No leading 0: desynchronised. Drop rather than loop forever.
            buffer.reset()
            return null
        }

        val out = ByteArrayOutputStream()
        // Walk groups after the pad. A `0x00` code is the frame delimiter.
        var pos = 1
        var frameEnd = -1
        while (pos < data.size) {
            val code = data[pos++].toInt() and 0xFF
            if (code == 0) {
                frameEnd = pos
                break
            }
            val payloadSize = code - 1
            if (pos + payloadSize > data.size) {
                // Group runs past the buffer: still arriving, or corrupt. Waiting is safe.
                return null
            }
            out.write(data, pos, payloadSize)
            pos += payloadSize
            // A non-max group implies a zero, unless it is the frame's last group.
            if (code != 0xFF && pos < data.size && data[pos] != 0.toByte()) {
                out.write(0)
            }
        }
        if (frameEnd < 0) return null // No delimiter yet → frame incomplete.

        // Consume through the delimiter; re-buffer what came after.
        buffer.reset()
        if (frameEnd < data.size) {
            buffer.write(data, frameEnd, data.size - frameEnd)
        }
        return out.toByteArray()
    }
}
