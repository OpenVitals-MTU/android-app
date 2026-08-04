package tech.mmarca.openvitals.core.fit

import java.time.Instant

/** FIT epoch (1989-12-31T00:00:00Z) in Unix seconds. */
private const val FitEpochUnixSeconds = 631_065_600L

/** A FIT timestamp — seconds since the FIT epoch — as a UTC [Instant]. */
fun fitInstant(value: Long): Instant = Instant.ofEpochSecond(FitEpochUnixSeconds + value)

/**
 * One decoded FIT data message: its global number, the field maps the generic
 * [FitDecoder] walk extracted (keyed by field number), and the resolved record
 * timestamp in FIT-epoch seconds (null when the message carried none).
 *
 * This is the seam between the generic FIT container walk and any domain
 * interpretation: the reader emits these knowing no message types, and each
 * interpreter switches on [globalMessageNumber] and reads the fields it knows.
 *
 * Port of the Flutter build's `core/fit/fit_message.dart`.
 */
class FitMessage(
    val globalMessageNumber: Int,
    val values: Map<Int, Long>,
    val strings: Map<Int, String>,
    val arrays: Map<Int, List<Long>>,
    val timestamp: Long?,
)

/** One FIT file's decoded messages, and where the next chained file begins. */
class FitFileMessages(
    val messages: List<FitMessage>,
    val nextOffset: Int,
)

/**
 * Generic FIT container reader: walks a `.FIT` byte stream and emits its data
 * messages as [FitMessage]s, knowing NOTHING about what any message means. The
 * domain interpretation (activity route import, wellness HRV) lives in
 * separate consumers that switch on [FitMessage.globalMessageNumber].
 *
 * Decodes EVERY field of EVERY message — there is no message allowlist —
 * because a reusable reader has no basis to guess which a consumer wants, and
 * FIT files are small enough that the extra field decoding is free. Consumers
 * simply ignore the messages and fields they do not know.
 *
 * Extracted from `FitRouteParser`'s private decoder (one FIT decoder
 * app-wide), matching the Flutter build's `core/fit/fit_reader.dart`. Malformed
 * input throws [IllegalArgumentException] with the same messages the private
 * decoder used, so callers' error handling is unchanged.
 */
object FitDecoder {

    /**
     * True when [bytes] begins a FIT file at [offset] (a valid header carrying
     * the `.FIT` magic). Used to spot the start of each file in a concatenated
     * stream.
     */
    fun isFitFileAt(bytes: ByteArray, offset: Int): Boolean {
        if (offset < 0 || offset + FitMinimumHeaderSize > bytes.size) return false
        val headerSize = bytes[offset].toUnsignedInt()
        return headerSize >= FitMinimumHeaderSize &&
            offset + headerSize <= bytes.size &&
            bytes[offset + FitHeaderDataTypeOffset] == '.'.code.toByte() &&
            bytes[offset + FitHeaderDataTypeOffset + 1] == 'F'.code.toByte() &&
            bytes[offset + FitHeaderDataTypeOffset + 2] == 'I'.code.toByte() &&
            bytes[offset + FitHeaderDataTypeOffset + 3] == 'T'.code.toByte()
    }

    fun isFitFile(bytes: ByteArray): Boolean = isFitFileAt(bytes, 0)

    /**
     * Decodes ONE FIT file starting at [startOffset], returning its data
     * messages (in file order) and the offset the next chained file would
     * begin at. The caller loops this across a concatenated stream, resetting
     * nothing — each file is self-contained (its own local-message definitions
     * and timestamp anchor).
     */
    fun readFile(bytes: ByteArray, startOffset: Int): FitFileMessages =
        FitFileReader(bytes, startOffset).read()
}

private class FitMessageDefinition(
    val globalMessageNumber: Int,
    val littleEndian: Boolean,
    val fields: List<FitFieldDefinition>,
    val developerFieldSizes: List<Int>,
)

private class FitFieldDefinition(
    val number: Int,
    val size: Int,
    val baseType: Int,
)

private class FitFileReader(
    private val fileBytes: ByteArray,
    private val startOffset: Int,
) {
    private val definitions = mutableMapOf<Int, FitMessageDefinition>()
    private val messages = mutableListOf<FitMessage>()
    private var lastTimestampRaw: Long? = null

    fun read(): FitFileMessages {
        val headerSize = fileBytes[startOffset].toUnsignedInt()
        require(headerSize >= FitMinimumHeaderSize && startOffset + headerSize <= fileBytes.size) {
            "FIT file header is invalid."
        }
        val dataSize = fileBytes.readUnsignedIntAt(startOffset + FitHeaderDataSizeOffset, littleEndian = true)
        require(dataSize <= Int.MAX_VALUE) {
            "FIT file data section is too large."
        }
        val dataStart = startOffset + headerSize
        val dataEnd = dataStart.toLong() + dataSize
        require(dataEnd <= fileBytes.size) {
            "FIT file data section is incomplete."
        }
        val reader = FitDataReader(fileBytes, dataStart, dataEnd.toInt())
        while (reader.hasRemaining()) {
            readRecord(reader)
        }
        val next = (dataEnd + FitCrcSize).coerceAtMost(fileBytes.size.toLong()).toInt()
        return FitFileMessages(messages = messages, nextOffset = next)
    }

    private fun readRecord(reader: FitDataReader) {
        val header = reader.readUnsignedByte()
        if (header and FitCompressedHeaderFlag != 0) {
            val localMessageType = (header ushr FitCompressedLocalMessageTypeShift) and
                FitCompressedLocalMessageTypeMask
            val timestamp = compressedTimestamp(header and FitCompressedTimestampMask)
            readDataMessage(localMessageType, timestamp, reader)
            return
        }
        val localMessageType = header and FitNormalLocalMessageTypeMask
        if (header and FitDefinitionMessageFlag != 0) {
            definitions[localMessageType] = readDefinitionMessage(header, reader)
        } else {
            readDataMessage(localMessageType, compressedTimestamp = null, reader)
        }
    }

    private fun readDefinitionMessage(header: Int, reader: FitDataReader): FitMessageDefinition {
        reader.skip(1)
        val littleEndian = when (reader.readUnsignedByte()) {
            FitArchitectureLittleEndian -> true
            FitArchitectureBigEndian -> false
            else -> throw IllegalArgumentException("FIT message architecture is invalid.")
        }
        val globalMessageNumber = reader.readUnsignedShort(littleEndian)
        val fieldCount = reader.readUnsignedByte()
        val fields = List(fieldCount) {
            FitFieldDefinition(
                number = reader.readUnsignedByte(),
                size = reader.readUnsignedByte(),
                baseType = reader.readUnsignedByte(),
            )
        }
        val developerFieldSizes = if (header and FitDeveloperDataFlag != 0) {
            val developerFieldCount = reader.readUnsignedByte()
            List(developerFieldCount) {
                reader.skip(1)
                val size = reader.readUnsignedByte()
                reader.skip(1)
                size
            }
        } else {
            emptyList()
        }
        return FitMessageDefinition(
            globalMessageNumber = globalMessageNumber,
            littleEndian = littleEndian,
            fields = fields,
            developerFieldSizes = developerFieldSizes,
        )
    }

    private fun readDataMessage(
        localMessageType: Int,
        compressedTimestamp: Long?,
        reader: FitDataReader,
    ) {
        val definition = definitions[localMessageType]
            ?: throw IllegalArgumentException("FIT data message has no definition.")
        val values = mutableMapOf<Int, Long>()
        val strings = mutableMapOf<Int, String>()
        val arrays = mutableMapOf<Int, List<Long>>()
        definition.fields.forEach { field ->
            val fieldBytes = reader.readBytes(field.size)
            fieldBytes.fitLong(field, definition.littleEndian)?.let { values[field.number] = it }
            fieldBytes.fitString(field)?.let { strings[field.number] = it }
            val array = fieldBytes.fitLongArray(field, definition.littleEndian)
            if (array.isNotEmpty()) arrays[field.number] = array
        }
        definition.developerFieldSizes.forEach { size ->
            reader.skip(size)
        }

        val explicitTimestamp = values[FitTimestampFieldNumber]
        val messageTimestamp = explicitTimestamp ?: compressedTimestamp
        if (messageTimestamp != null) lastTimestampRaw = messageTimestamp

        messages.add(
            FitMessage(
                globalMessageNumber = definition.globalMessageNumber,
                values = values,
                strings = strings,
                arrays = arrays,
                timestamp = messageTimestamp,
            ),
        )
    }

    private fun compressedTimestamp(offset: Int): Long? {
        val previous = lastTimestampRaw ?: return null
        val previousOffset = previous and FitCompressedTimestampMask.toLong()
        val delta = if (offset.toLong() < previousOffset) {
            offset.toLong() + FitCompressedTimestampRollover - previousOffset
        } else {
            offset.toLong() - previousOffset
        }
        return previous + delta
    }
}

private class FitDataReader(
    private val bytes: ByteArray,
    private var offset: Int,
    private val endOffset: Int,
) {
    fun hasRemaining(): Boolean = offset < endOffset

    fun readUnsignedByte(): Int {
        require(offset < endOffset) {
            "FIT file ended before data records were complete."
        }
        return bytes[offset++].toUnsignedInt()
    }

    fun readUnsignedShort(littleEndian: Boolean): Int {
        require(offset + 2 <= endOffset) {
            "FIT file ended before data records were complete."
        }
        val value = bytes.readUnsignedShortAt(offset, littleEndian)
        offset += 2
        return value
    }

    fun readBytes(size: Int): ByteArray {
        require(size >= 0 && offset + size <= endOffset) {
            "FIT file ended before data records were complete."
        }
        return bytes.copyOfRange(offset, offset + size).also {
            offset += size
        }
    }

    fun skip(size: Int) {
        require(size >= 0 && offset + size <= endOffset) {
            "FIT file ended before data records were complete."
        }
        offset += size
    }
}

private fun Byte.toUnsignedInt(): Int = toInt() and 0xFF

private fun ByteArray.readUnsignedShortAt(index: Int, littleEndian: Boolean): Int {
    val first = this[index].toUnsignedInt()
    val second = this[index + 1].toUnsignedInt()
    return if (littleEndian) {
        first or (second shl 8)
    } else {
        (first shl 8) or second
    }
}

private fun ByteArray.readSignedShortAt(index: Int, littleEndian: Boolean): Int {
    val value = readUnsignedShortAt(index, littleEndian)
    return if (value and 0x8000 != 0) value - 0x10000 else value
}

private fun ByteArray.readIntAt(index: Int, littleEndian: Boolean): Int {
    val first = this[index].toUnsignedInt()
    val second = this[index + 1].toUnsignedInt()
    val third = this[index + 2].toUnsignedInt()
    val fourth = this[index + 3].toUnsignedInt()
    return if (littleEndian) {
        first or (second shl 8) or (third shl 16) or (fourth shl 24)
    } else {
        (first shl 24) or (second shl 16) or (third shl 8) or fourth
    }
}

private fun ByteArray.readUnsignedIntAt(index: Int, littleEndian: Boolean): Long =
    readIntAt(index, littleEndian).toLong() and 0xFFFFFFFFL

/**
 * Every element of an array field, invalid sentinels dropped.
 *
 * FIT expresses an array as a field whose declared size is a multiple of its
 * base type's — the Health Snapshot messages pack a whole two-minute recording
 * into one record this way. [fitLong] reads only the first element, which is
 * right for every scalar field and silently loses the rest of an array.
 */
private fun ByteArray.fitLongArray(field: FitFieldDefinition, littleEndian: Boolean): List<Long> {
    val baseType = field.baseType and FitBaseTypeMask
    val elementSize = fitBaseTypeSize(baseType)
    if (elementSize <= 0) return emptyList()
    val out = mutableListOf<Long>()
    var offset = 0
    while (offset + elementSize <= size) {
        copyOfRange(offset, offset + elementSize)
            .fitLong(field, littleEndian)
            ?.let { out.add(it) }
        offset += elementSize
    }
    return out
}

private fun ByteArray.fitLong(field: FitFieldDefinition, littleEndian: Boolean): Long? {
    val baseType = field.baseType and FitBaseTypeMask
    val baseTypeSize = fitBaseTypeSize(baseType)
    if (baseTypeSize <= 0 || size < baseTypeSize) return null

    return when (baseType) {
        FitBaseTypeEnum,
        FitBaseTypeUInt8 -> this[0].toUnsignedInt()
            .takeUnless { it == FitInvalidUInt8 }
            ?.toLong()
        FitBaseTypeSInt8 -> this[0].toInt()
            .takeUnless { it == FitInvalidSInt8 }
            ?.toLong()
        FitBaseTypeSInt16 -> readSignedShortAt(0, littleEndian)
            .takeUnless { it == FitInvalidSInt16 }
            ?.toLong()
        FitBaseTypeUInt16 -> readUnsignedShortAt(0, littleEndian)
            .takeUnless { it == FitInvalidUInt16 }
            ?.toLong()
        FitBaseTypeSInt32 -> readIntAt(0, littleEndian)
            .takeUnless { it == FitInvalidSInt32 }
            ?.toLong()
        FitBaseTypeUInt32 -> readUnsignedIntAt(0, littleEndian)
            .takeUnless { it == FitInvalidUInt32 }
        FitBaseTypeUInt8z -> this[0].toUnsignedInt()
            .takeUnless { it == 0 }
            ?.toLong()
        FitBaseTypeUInt16z -> readUnsignedShortAt(0, littleEndian)
            .takeUnless { it == 0 }
            ?.toLong()
        FitBaseTypeUInt32z -> readUnsignedIntAt(0, littleEndian)
            .takeUnless { it == 0L }
        else -> null
    }
}

private fun ByteArray.fitString(field: FitFieldDefinition): String? {
    val baseType = field.baseType and FitBaseTypeMask
    if (baseType != FitBaseTypeString) return null
    // Trim, and treat an all-blank name as absent — inlined rather than
    // importing the route-import helper so the reader stays generic.
    return toString(Charsets.UTF_8)
        .trimEnd('\u0000')
        .trim()
        .takeIf { it.isNotBlank() }
}

private fun fitBaseTypeSize(baseType: Int): Int =
    when (baseType) {
        FitBaseTypeEnum,
        FitBaseTypeSInt8,
        FitBaseTypeUInt8,
        FitBaseTypeString,
        FitBaseTypeUInt8z,
        FitBaseTypeByte -> 1
        FitBaseTypeSInt16,
        FitBaseTypeUInt16,
        FitBaseTypeUInt16z -> 2
        FitBaseTypeSInt32,
        FitBaseTypeUInt32,
        FitBaseTypeFloat32,
        FitBaseTypeUInt32z -> 4
        FitBaseTypeFloat64,
        FitBaseTypeSInt64,
        FitBaseTypeUInt64,
        FitBaseTypeUInt64z -> 8
        else -> 0
    }

// FIT container framing.
private const val FitMinimumHeaderSize = 12
private const val FitHeaderDataSizeOffset = 4
private const val FitHeaderDataTypeOffset = 8
private const val FitCrcSize = 2
private const val FitCompressedHeaderFlag = 0x80
private const val FitCompressedLocalMessageTypeShift = 5
private const val FitCompressedLocalMessageTypeMask = 0x03
private const val FitCompressedTimestampMask = 0x1F
private const val FitCompressedTimestampRollover = 0x20L
private const val FitDefinitionMessageFlag = 0x40
private const val FitDeveloperDataFlag = 0x20
private const val FitNormalLocalMessageTypeMask = 0x0F
private const val FitArchitectureLittleEndian = 0
private const val FitArchitectureBigEndian = 1

/** `timestamp` is field 253 on every message that carries one. */
private const val FitTimestampFieldNumber = 253

// FIT base types and their invalid sentinels.
private const val FitBaseTypeMask = 0x1F
private const val FitBaseTypeEnum = 0
private const val FitBaseTypeSInt8 = 1
private const val FitBaseTypeUInt8 = 2
private const val FitBaseTypeSInt16 = 3
private const val FitBaseTypeUInt16 = 4
private const val FitBaseTypeSInt32 = 5
private const val FitBaseTypeUInt32 = 6
private const val FitBaseTypeString = 7
private const val FitBaseTypeFloat32 = 8
private const val FitBaseTypeFloat64 = 9
private const val FitBaseTypeUInt8z = 10
private const val FitBaseTypeUInt16z = 11
private const val FitBaseTypeUInt32z = 12
private const val FitBaseTypeByte = 13
private const val FitBaseTypeSInt64 = 14
private const val FitBaseTypeUInt64 = 15
private const val FitBaseTypeUInt64z = 16
private const val FitInvalidUInt8 = 0xFF
private const val FitInvalidSInt8 = 0x7F
private const val FitInvalidUInt16 = 0xFFFF
private const val FitInvalidSInt16 = 0x7FFF
private const val FitInvalidUInt32 = 0xFFFFFFFFL
private const val FitInvalidSInt32 = 0x7FFFFFFF
