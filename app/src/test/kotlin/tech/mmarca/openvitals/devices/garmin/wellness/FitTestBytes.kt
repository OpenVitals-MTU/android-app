package tech.mmarca.openvitals.devices.garmin.wellness

import java.io.ByteArrayOutputStream
import java.time.Instant

/** Garmin device epoch: seconds between the Unix and Garmin epochs. */
internal const val GARMIN_EPOCH_OFFSET_SECONDS = 631_065_600L

internal fun fitTimestamp(t: Instant): Long = t.epochSecond - GARMIN_EPOCH_OFFSET_SECONDS

/**
 * Minimal FIT writer (little-endian), enough for hand-built wellness files.
 * The bytes are hand-built so no real health data is committed; the layout
 * mirrors what a vívoactive writes. Port of the `_W` helper the Flutter tests
 * used.
 */
internal class FitW {
    private val b = ByteArrayOutputStream()

    fun u8(v: Int) = apply { b.write(v and 0xFF) }

    fun u8(v: Long) = u8(v.toInt())

    fun u16(v: Int) = apply {
        u8(v)
        u8(v ushr 8)
    }

    fun u32(v: Long) = apply {
        u8((v and 0xFF).toInt())
        u8(((v ushr 8) and 0xFF).toInt())
        u8(((v ushr 16) and 0xFF).toInt())
        u8(((v ushr 24) and 0xFF).toInt())
    }

    fun bytes(v: ByteArray) = apply { b.write(v) }

    fun bytes(v: List<Int>) = apply { v.forEach { u8(it) } }

    /** A definition record: local type, global message number, (num,size,base)×. */
    fun def(local: Int, global: Int, fields: List<List<Int>>) = apply {
        u8(0x40 or local)
        u8(0)
        u8(0) // little-endian
        u16(global)
        u8(fields.size)
        for (f in fields) {
            u8(f[0])
            u8(f[1])
            u8(f[2])
        }
    }

    fun toBytes(): ByteArray = b.toByteArray()
}

/**
 * Wraps a data section in the 14-byte FIT header + trailing CRC (unchecked by
 * the decoder, which reads the declared data size).
 */
internal fun fitWrap(data: ByteArray): ByteArray = FitW()
    .u8(14)
    .u8(16)
    .u16(0)
    .u32(data.size.toLong())
    .bytes(".FIT".toByteArray(Charsets.US_ASCII))
    .u16(0)
    .bytes(data)
    .u16(0)
    .toBytes()

/** `file_id` carrying just the type, so the file classifies. */
internal fun FitW.fileId(fileType: Int): FitW = apply {
    def(3, 0, listOf(listOf(0, 1, 0)))
    u8(3)
    u8(fileType)
}
