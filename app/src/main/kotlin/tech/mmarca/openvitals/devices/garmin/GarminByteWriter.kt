package tech.mmarca.openvitals.devices.garmin

/** Little-endian write cursor. Grows as needed; writers return `this` for chaining. */
class GarminByteWriter(initialCapacity: Int = 256) {

    private var buffer = ByteArray(if (initialCapacity > 0) initialCapacity else 1)

    var length: Int = 0
        private set

    private fun ensure(extra: Int) {
        if (length + extra <= buffer.size) return
        var next = buffer.size * 2
        while (next < length + extra) {
            next *= 2
        }
        buffer = buffer.copyOf(next)
    }

    fun writeByte(value: Int): GarminByteWriter {
        ensure(1)
        buffer[length++] = value.toByte()
        return this
    }

    fun writeShort(value: Int): GarminByteWriter {
        ensure(2)
        buffer[length++] = value.toByte()
        buffer[length++] = (value shr 8).toByte()
        return this
    }

    fun writeInt(value: Long): GarminByteWriter {
        ensure(4)
        for (i in 0 until 4) {
            buffer[length++] = (value shr (8 * i)).toByte()
        }
        return this
    }

    fun writeInt(value: Int): GarminByteWriter = writeInt(value.toLong())

    fun writeLong(value: Long): GarminByteWriter {
        ensure(8)
        for (i in 0 until 8) {
            buffer[length++] = (value shr (8 * i)).toByte()
        }
        return this
    }

    fun writeBytes(bytes: ByteArray): GarminByteWriter {
        ensure(bytes.size)
        bytes.copyInto(buffer, length)
        length += bytes.size
        return this
    }

    /**
     * A length-prefixed UTF-8 string, NUL-terminated inside the length.
     * Truncated to fit the length byte, so a long name cannot corrupt the frame.
     */
    fun writeString(value: String): GarminByteWriter {
        val encoded = value.toByteArray(Charsets.UTF_8)
        val bytes = if (encoded.size > 254) encoded.copyOf(254) else encoded
        writeByte(bytes.size + 1)
        writeBytes(bytes)
        writeByte(0)
        return this
    }

    /** Overwrites 2 bytes at [offset], to backfill the frame length. */
    fun patchShort(offset: Int, value: Int) {
        buffer[offset] = value.toByte()
        buffer[offset + 1] = (value shr 8).toByte()
    }

    fun toBytes(): ByteArray = buffer.copyOf(length)
}
