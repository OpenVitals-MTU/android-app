package tech.mmarca.openvitals.devices.garmin

/**
 * Little-endian read cursor over a byte array. Reads are unsigned:
 * [readByte] and [readShort] return non-negative [Int], [readInt] a
 * non-negative [Long]. [readLong] returns the raw 64 bits.
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
     * A length-prefixed UTF-8 string. Garmin counts the terminating NUL in
     * the length, so it is stripped. Malformed UTF-8 decodes to replacements.
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
     * A NUL-terminated UTF-8 string. The notification control channel sends
     * an app identifier with no length. A missing terminator returns the rest.
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
