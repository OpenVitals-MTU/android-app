package tech.mmarca.openvitals.devices.garmin

import java.io.ByteArrayOutputStream

/**
 * Garmin's COBS variant, the framing under every GFDI packet.
 *
 * Port of Gadgetbridge's `CobsCoDec` (AGPLv3). It is NOT textbook COBS: a
 * frame is bracketed by a LEADING and a trailing `0x00` (standard COBS has
 * only the trailing delimiter), and a payload ending in a zero gets an extra
 * `0x01` group so the decoder can tell "ends in zero" from "ends at a block
 * boundary". Both quirks are load-bearing, so this is ported literally rather
 * than swapped for a library.
 *
 * Split into a pure [encode] and a streaming [GarminCobsDecoder]: bytes arrive
 * from BLE in arbitrary chunks, so the decoder buffers until it sees the
 * trailing `0x00` that ends a frame.
 */
object GarminCobs {

    /** Encodes one GFDI packet into a wire frame: `00 <cobs groups> 00`. */
    fun encode(data: ByteArray): ByteArray {
        // Worst case is ~2x plus the two delimiters; the stream grows anyway.
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
            // The scan stopped either at a zero or at the end; only a zero
            // advances `pos` past `zeroIndex` below, which is how the
            // trailing-zero flag is set.
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

/**
 * Reassembles COBS frames from an arbitrarily-chunked byte stream.
 *
 * Feed it whatever BLE delivers; [pull] returns the next fully-decoded GFDI
 * packet or null. Stateful and single-consumer, mirroring Gadgetbridge's
 * `CobsCoDec` which holds one accumulation buffer per connection.
 */
class GarminCobsDecoder {

    private val buffer = ByteArrayOutputStream()

    /** Appends received [bytes] to the internal buffer. */
    fun addBytes(bytes: ByteArray) {
        buffer.write(bytes, 0, bytes.size)
    }

    /**
     * Decodes and returns the next complete packet, or null when the buffer
     * does not yet hold a full frame (no `0x00` delimiter past the leading
     * pad).
     *
     * Call repeatedly until it returns null — one BLE chunk can complete more
     * than one frame, and the decoded packet keeps whatever follows it
     * buffered.
     */
    fun pull(): ByteArray? {
        val data = buffer.toByteArray()
        if (data.size < 4) return null // Min frame: pad + group + byte + delim.
        if (data[0] != 0.toByte()) {
            // No leading 0 → the buffer is desynchronised. Drop it rather than
            // loop forever on a frame that can never decode.
            buffer.reset()
            return null
        }

        val out = ByteArrayOutputStream()
        // Walk groups after the leading pad. A `0x00` code IS the frame
        // delimiter (internal — the codec's leading/trailing pads bracket each
        // frame), so it ends this frame and marks where the leftover begins.
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
                // Group runs past the buffer: either still arriving, or
                // corrupt. Waiting is safe — a real frame ends in a delimiter
                // the scan has not reached.
                return null
            }
            out.write(data, pos, payloadSize)
            pos += payloadSize
            // A non-max group implies a zero after its payload — unless the
            // next byte is the delimiter, i.e. this was the frame's last
            // group. (Standard COBS: the final block carries no implied zero.)
            if (code != 0xFF && pos < data.size && data[pos] != 0.toByte()) {
                out.write(0)
            }
        }
        if (frameEnd < 0) return null // No delimiter yet → frame incomplete.

        // Consume through the delimiter; re-buffer whatever came after it.
        buffer.reset()
        if (frameEnd < data.size) {
            buffer.write(data, frameEnd, data.size - frameEnd)
        }
        return out.toByteArray()
    }
}
