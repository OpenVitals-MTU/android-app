package tech.mmarca.openvitals.devices.garmin.wellness

import java.io.ByteArrayOutputStream
import java.time.Instant

/** Garmin device epoch: seconds between the Unix and Garmin epochs. */
internal const val GARMIN_EPOCH_OFFSET_SECONDS = 631_065_600L

internal fun fitTimestamp(t: Instant): Long = t.epochSecond - GARMIN_EPOCH_OFFSET_SECONDS

/** Minimal little-endian FIT writer for hand-built wellness files, so no real health data is committed. */
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

/** Wraps a data section in the 14-byte FIT header plus trailing CRC. */
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

/** One slot of an unstaged sleep stream: a raw feature row, or a 0/1 stage verdict. */
internal sealed class FitSleepStreamRow {
    /** Ten float16 features; index 8 is movement, index 9 heart rate. */
    class Raw(val features: FloatArray) : FitSleepStreamRow()

    class Stage(val level: Int) : FitSleepStreamRow()
}

/** A raw row with the values the estimator reads and zeros elsewhere. */
internal fun rawRow(heartRate: Float, movement: Float = 0f, activity: Float = 0f): FitSleepStreamRow.Raw =
    FitSleepStreamRow.Raw(FloatArray(10).also { it[0] = activity; it[8] = movement; it[9] = heartRate })

/**
 * A Venu SQ style sleep file: `sleep_data_info` (273), then one record per
 * minute, `sleep_data_raw` (274) or `sleep_stage` (275). Stage rows carry a
 * timestamp like the watch's, unless [stageTimestamps] is false.
 */
internal fun fitSleepRawBytes(
    start: Instant,
    offsetSeconds: Int,
    rows: List<FitSleepStreamRow>,
    sampleSeconds: Int = 60,
    stageTimestamps: Boolean = true,
): ByteArray {
    val data = FitW().fileId(49)

    // sleep_data_info: timestamp, local_timestamp, sample_length, version.
    data.def(
        1,
        273,
        listOf(listOf(253, 4, 0x86), listOf(2, 4, 0x86), listOf(1, 2, 0x84), listOf(0, 1, 0x02)),
    )
    data.u8(1)
        .u32(fitTimestamp(start))
        .u32(fitTimestamp(start) + offsetSeconds)
        .u16(sampleSeconds)
        .u8(2)

    if (stageTimestamps) {
        data.def(2, 275, listOf(listOf(253, 4, 0x86), listOf(0, 1, 0x00)))
    } else {
        data.def(2, 275, listOf(listOf(0, 1, 0x00)))
    }
    data.def(3, 274, listOf(listOf(0, 20, 0x0D)))

    for ((index, row) in rows.withIndex()) {
        when (row) {
            is FitSleepStreamRow.Raw -> {
                data.u8(3)
                for (value in row.features) data.u16(fitHalfBits(value))
            }
            is FitSleepStreamRow.Stage -> {
                data.u8(2)
                if (stageTimestamps) data.u32(fitTimestamp(start) + sampleSeconds.toLong() * index)
                data.u8(row.level)
            }
        }
    }
    return fitWrap(data.toBytes())
}

/** float32 to float16 bits, round-to-nearest. Enough for test values. */
internal fun fitHalfBits(value: Float): Int {
    val bits = value.toRawBits()
    val sign = (bits ushr 16) and 0x8000
    val exponent = (bits ushr 23) and 0xFF
    val mantissa = bits and 0x7FFFFF
    if (exponent == 0xFF) return sign or 0x7C00 or (if (mantissa != 0) 0x200 else 0)
    val rebased = exponent - 127 + 15
    if (rebased >= 0x1F) return sign or 0x7C00
    if (rebased <= 0) {
        if (rebased < -10) return sign
        val subnormal = (mantissa or 0x800000) ushr (1 - rebased)
        return sign or ((subnormal + 0x1000) ushr 13)
    }
    return sign or (rebased shl 10) or ((mantissa + 0x1000) ushr 13)
}
