package tech.mmarca.openvitals.devices.garmin

/**
 * Little-endian write cursor.
 *
 * Port of Gadgetbridge's `MessageWriter` (via the Flutter build's
 * `garmin_byte_writer.dart`). Grows as needed; [toBytes] returns exactly what
 * was written. Writers return `this` so call sites can chain, mirroring the
 * Dart cascades the protocol code was written with.
 */
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
     * A length-prefixed UTF-8 string, NUL-terminated inside the length — the
     * shape `MessageWriter.writeString` produces and the watch expects.
     *
     * Truncated to fit the single length byte (including the NUL), because an
     * over-long device name must not corrupt every field after it in the frame.
     */
    fun writeString(value: String): GarminByteWriter {
        val encoded = value.toByteArray(Charsets.UTF_8)
        val bytes = if (encoded.size > 254) encoded.copyOf(254) else encoded
        writeByte(bytes.size + 1)
        writeBytes(bytes)
        writeByte(0)
        return this
    }

    /**
     * Overwrites 2 bytes at [offset] — used to backfill the frame length once
     * the payload is written.
     */
    fun patchShort(offset: Int, value: Int) {
        buffer[offset] = value.toByte()
        buffer[offset + 1] = (value shr 8).toByte()
    }

    fun toBytes(): ByteArray = buffer.copyOf(length)
}
