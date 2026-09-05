package tech.mmarca.openvitals.core.fit

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.time.Instant

/** A UTC [Instant] as a FIT timestamp, the inverse of [fitInstant]. Pre-1990 clamps to 0. */
fun fitTimestamp(time: Instant): Long =
    (time.epochSecond - FitEpochUnixSeconds).coerceAtLeast(0L)

/**
 * FIT base types as they appear on the wire, high bit set on multi-byte
 * types. No floats: the convention is scaled integers.
 */
object FitBaseType {
    const val ENUM = 0x00
    const val SINT8 = 0x01
    const val UINT8 = 0x02
    const val STRING = 0x07
    const val SINT16 = 0x83
    const val UINT16 = 0x84
    const val SINT32 = 0x85
    const val UINT32 = 0x86
}

/** One field of a definition. Only a STRING needs its size spelled out. */
class FitEncoderField(
    val number: Int,
    val baseType: Int,
    val size: Int = fitEncoderBaseTypeSize(baseType),
) {
    init {
        require(size in 1..255) { "FIT field size must fit in a byte: $size" }
    }
}

/**
 * Generic FIT container writer, the mirror of [FitDecoder]: definition and
 * data records, invalid sentinels for absent values, and the file framing.
 * A data message writes every defined field in definition order from the
 * map, so a record cannot misalign. Buffered in memory for the CRCs.
 */
class FitEncoder {

    private val records = ByteArrayOutputStream()
    private val definitions = mutableMapOf<Int, List<FitEncoderField>>()

    /** Writes a definition record and remembers it for [writeMessage]. */
    fun defineMessage(
        localMessageType: Int,
        globalMessageNumber: Int,
        fields: List<FitEncoderField>,
    ) {
        require(localMessageType in 0..15) { "Local message type must be 0..15: $localMessageType" }
        require(fields.size <= 255) { "A FIT definition holds at most 255 fields." }
        records.write(FitEncoderDefinitionFlag or localMessageType)
        records.write(0) // reserved
        records.write(FitEncoderLittleEndian)
        records.writeUInt16(globalMessageNumber.toLong())
        records.write(fields.size)
        fields.forEach { field ->
            records.write(field.number)
            records.write(field.size)
            records.write(field.baseType)
        }
        definitions[localMessageType] = fields
    }

    /** Writes one data record for a previously defined local message type. */
    fun writeMessage(
        localMessageType: Int,
        values: Map<Int, Long?> = emptyMap(),
        strings: Map<Int, String> = emptyMap(),
    ) {
        val fields = checkNotNull(definitions[localMessageType]) {
            "Local message type $localMessageType has no definition."
        }
        records.write(localMessageType)
        fields.forEach { field ->
            if (field.baseType == FitBaseType.STRING) {
                records.writeFitString(strings[field.number], field.size)
            } else {
                records.writeScalar(values[field.number], field.baseType)
            }
        }
    }

    /** Frames the records into a complete FIT file: header, data, CRCs. */
    fun writeTo(output: OutputStream) {
        val body = records.toByteArray()
        val file = ByteArray(FitEncoderHeaderSize + body.size + 2)
        file[0] = FitEncoderHeaderSize.toByte()
        file[1] = FitEncoderProtocolVersion.toByte()
        file.setUInt16(2, FitEncoderProfileVersion)
        file.setUInt32(4, body.size.toLong())
        FitEncoderMagic.forEachIndexed { index, char -> file[8 + index] = char.code.toByte() }
        file.setUInt16(12, FitCrc.compute(file, offset = 0, length = 12))
        body.copyInto(file, destinationOffset = FitEncoderHeaderSize)
        // The trailing CRC covers header and data.
        file.setUInt16(file.size - 2, FitCrc.compute(file, offset = 0, length = file.size - 2))
        output.write(file)
    }
}

private fun ByteArrayOutputStream.writeScalar(value: Long?, baseType: Int) {
    when (baseType) {
        FitBaseType.ENUM,
        FitBaseType.UINT8 -> write(((value ?: 0xFFL) and 0xFF).toInt())
        FitBaseType.SINT8 -> write(((value ?: 0x7FL) and 0xFF).toInt())
        FitBaseType.UINT16 -> writeUInt16(value ?: 0xFFFFL)
        FitBaseType.SINT16 -> writeUInt16(value ?: 0x7FFFL)
        FitBaseType.UINT32 -> writeUInt32(value ?: 0xFFFFFFFFL)
        FitBaseType.SINT32 -> writeUInt32(value ?: 0x7FFFFFFFL)
        else -> error("Unsupported FIT base type for encoding: $baseType")
    }
}

/** UTF-8 bytes, truncated to leave room for at least one NUL, then NUL-padded. */
private fun ByteArrayOutputStream.writeFitString(value: String?, size: Int) {
    val bytes = value.orEmpty().toByteArray(Charsets.UTF_8)
    val written = minOf(bytes.size, size - 1)
    write(bytes, 0, written)
    repeat(size - written) { write(0) }
}

private fun ByteArrayOutputStream.writeUInt16(value: Long) {
    write((value and 0xFF).toInt())
    write(((value shr 8) and 0xFF).toInt())
}

private fun ByteArrayOutputStream.writeUInt32(value: Long) {
    write((value and 0xFF).toInt())
    write(((value shr 8) and 0xFF).toInt())
    write(((value shr 16) and 0xFF).toInt())
    write(((value shr 24) and 0xFF).toInt())
}

private fun ByteArray.setUInt16(offset: Int, value: Int) {
    this[offset] = (value and 0xFF).toByte()
    this[offset + 1] = ((value shr 8) and 0xFF).toByte()
}

private fun ByteArray.setUInt32(offset: Int, value: Long) {
    this[offset] = (value and 0xFF).toByte()
    this[offset + 1] = ((value shr 8) and 0xFF).toByte()
    this[offset + 2] = ((value shr 16) and 0xFF).toByte()
    this[offset + 3] = ((value shr 24) and 0xFF).toByte()
}

private fun fitEncoderBaseTypeSize(baseType: Int): Int = when (baseType) {
    FitBaseType.ENUM, FitBaseType.SINT8, FitBaseType.UINT8, FitBaseType.STRING -> 1
    FitBaseType.SINT16, FitBaseType.UINT16 -> 2
    FitBaseType.SINT32, FitBaseType.UINT32 -> 4
    else -> error("Unsupported FIT base type for encoding: $baseType")
}

private const val FitEncoderHeaderSize = 14
private const val FitEncoderProtocolVersion = 0x10
private const val FitEncoderProfileVersion = 2132
private const val FitEncoderMagic = ".FIT"
private const val FitEncoderDefinitionFlag = 0x40
private const val FitEncoderLittleEndian = 0
