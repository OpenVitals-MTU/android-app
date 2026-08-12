package tech.mmarca.openvitals.devices.garmin

/**
 * Little-endian read cursor over a byte array.
 *
 * Port of the read half of Gadgetbridge's `GarminByteBufferReader` /
 * `MessageReader` (via the Flutter build's `garmin_byte_reader.dart`).
 * Garmin's wire format is little-endian throughout, so the endianness is baked
 * in rather than configurable.
 *
 * Reads are UNSIGNED: [readByte] and [readShort] return the value as a
 * non-negative [Int], [readInt] returns a non-negative [Long] — matching the
 * Dart original, whose 64-bit ints made every u8/u16/u32 read naturally
 * unsigned. [readLong] returns the raw 64 bits.
 */
class GarminByteReader(private val data: ByteArray) {

    var position: Int = 0
        private set

    val remaining: Int get() = data.size - position
    val hasRemaining: Boolean get() = position < data.size

    fun readByte(): Int = data[position++].toInt() and 0xFF

    fun readShort(): Int {
        val v = (data[position].toInt() and 0xFF) or
            ((data[position + 1].toInt() and 0xFF) shl 8)
        position += 2
        return v
    }

    fun readInt(): Long {
        var v = 0L
        for (i in 0 until 4) {
            v = v or ((data[position + i].toLong() and 0xFF) shl (8 * i))
        }
        position += 4
        return v
    }

    fun readLong(): Long {
        var v = 0L
        for (i in 0 until 8) {
            v = v or ((data[position + i].toLong() and 0xFF) shl (8 * i))
        }
        position += 8
        return v
    }

    fun readBytes(length: Int): ByteArray {
        val slice = data.copyOfRange(position, position + length)
        position += length
        return slice
    }

    /**
     * A length-prefixed UTF-8 string: one length byte then that many bytes.
     *
     * Garmin includes the terminating NUL in the length, so it is stripped
     * here — otherwise every device name arrives with a trailing NUL that
     * survives into the UI. Malformed UTF-8 decodes to replacement characters
     * rather than throwing, matching Dart's `allowMalformed: true`.
     */
    fun readString(): String {
        val length = readByte()
        if (length == 0) return ""
        val bytes = readBytes(length)
        val end = if (bytes.isNotEmpty() && bytes.last() == 0.toByte()) {
            bytes.size - 1
        } else {
            bytes.size
        }
        return String(bytes, 0, end, Charsets.UTF_8)
    }

    /**
     * A NUL-terminated UTF-8 string: bytes up to the first zero, which is
     * consumed.
     *
     * The OTHER string shape on this wire. Most Garmin fields are
     * length-prefixed ([readString]), but the notification control channel
     * sends an app identifier with no length at all — so reading it with
     * [readString] would take the first byte as a length and desynchronise
     * everything after it. A missing terminator returns the rest of the buffer
     * rather than throwing, because a truncated frame is the watch's problem,
     * not a crash.
     */
    fun readNullTerminatedString(): String {
        val start = position
        while (position < data.size && data[position] != 0.toByte()) {
            position++
        }
        val text = String(data, start, position - start, Charsets.UTF_8)
        if (position < data.size) position++ // Consume the terminator.
        return text
    }

    fun skip(count: Int) {
        position += count
    }
}
