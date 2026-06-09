package tech.mmarca.openvitals.features.manualentry

import java.time.Instant
import tech.mmarca.openvitals.data.model.ExerciseRoutePoint

internal object FitRouteParser {
    fun parse(fitBytes: ByteArray, fileName: String? = null): RouteFileImport {
        val result = FitDecoder(fitBytes).decode()
        require(result.points.size >= MinRoutePoints) {
            "FIT activity must contain at least $MinRoutePoints timestamped GPS points."
        }
        return buildRouteImport(
            fileName = fileName,
            points = result.points,
            metadata = RouteFileMetadata(
                name = null,
                description = null,
                type = result.sport?.fitSportName(),
            ),
        )
    }
}

private data class FitDecodeResult(
    val points: List<ExerciseRoutePoint>,
    val sport: Int?,
)

private data class FitFileDecodeResult(
    val points: List<ExerciseRoutePoint>,
    val sport: Int?,
    val nextOffset: Int,
)

private data class FitMessageDefinition(
    val globalMessageNumber: Int,
    val littleEndian: Boolean,
    val fields: List<FitFieldDefinition>,
    val developerFields: List<FitDeveloperFieldDefinition>,
)

private data class FitFieldDefinition(
    val number: Int,
    val size: Int,
    val baseType: Int,
)

private data class FitDeveloperFieldDefinition(
    val size: Int,
)

private class FitDecoder(
    private val fileBytes: ByteArray,
) {
    fun decode(): FitDecodeResult {
        val points = mutableListOf<ExerciseRoutePoint>()
        var sport: Int? = null
        var offset = 0
        var decodedAnyFile = false

        while (offset < fileBytes.size) {
            if (!fileBytes.isFitFileAt(offset)) {
                if (!decodedAnyFile) {
                    throw IllegalArgumentException("FIT file header is invalid.")
                }
                break
            }

            val result = FitSingleFileDecoder(fileBytes, offset).decode()
            points += result.points
            if (sport == null) {
                sport = result.sport
            }
            decodedAnyFile = true
            offset = result.nextOffset
        }

        return FitDecodeResult(
            points = points,
            sport = sport,
        )
    }
}

private class FitSingleFileDecoder(
    private val fileBytes: ByteArray,
    private val startOffset: Int,
) {
    private val definitions = mutableMapOf<Int, FitMessageDefinition>()
    private val points = mutableListOf<ExerciseRoutePoint>()
    private var sport: Int? = null
    private var lastTimestampRaw: Long? = null

    fun decode(): FitFileDecodeResult {
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

        return FitFileDecodeResult(
            points = points,
            sport = sport,
            nextOffset = (dataEnd + FitCrcSize).coerceAtMost(fileBytes.size.toLong()).toInt(),
        )
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
        val developerFields = if (header and FitDeveloperDataFlag != 0) {
            val developerFieldCount = reader.readUnsignedByte()
            List(developerFieldCount) {
                reader.skip(1)
                val size = reader.readUnsignedByte()
                reader.skip(1)
                FitDeveloperFieldDefinition(size = size)
            }
        } else {
            emptyList()
        }
        return FitMessageDefinition(
            globalMessageNumber = globalMessageNumber,
            littleEndian = littleEndian,
            fields = fields,
            developerFields = developerFields,
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

        definition.fields.forEach { field ->
            val fieldBytes = reader.readBytes(field.size)
            if (field.number == FitTimestampFieldNumber || definition.globalMessageNumber in FitParsedMessageNumbers) {
                fieldBytes.fitLong(field, definition.littleEndian)?.let { value ->
                    values[field.number] = value
                }
            }
        }
        definition.developerFields.forEach { field ->
            reader.skip(field.size)
        }

        val explicitTimestamp = values[FitTimestampFieldNumber]
        val messageTimestamp = explicitTimestamp ?: compressedTimestamp
        if (messageTimestamp != null) {
            lastTimestampRaw = messageTimestamp
        }

        when (definition.globalMessageNumber) {
            FitRecordMessageNumber -> addRecordPoint(values, messageTimestamp)
            FitSessionMessageNumber -> {
                val sessionSport = values[FitSessionSportFieldNumber]
                    ?.toInt()
                    ?.takeUnless { it == FitSportGeneric }
                if (sport == null && sessionSport != null) {
                    sport = sessionSport
                }
            }
        }
    }

    private fun addRecordPoint(values: Map<Int, Long>, timestampRaw: Long?) {
        val timestamp = timestampRaw ?: return
        val latitude = values[FitRecordPositionLatFieldNumber]
            ?.fitSemicirclesToDegrees()
            ?.takeIf { it in MinLatitude..MaxLatitude }
            ?: return
        val longitude = values[FitRecordPositionLongFieldNumber]
            ?.fitSemicirclesToDegrees()
            ?.takeIf { it in MinLongitude..MaxLongitude }
            ?: return
        val altitudeMeters = (values[FitRecordEnhancedAltitudeFieldNumber]
            ?: values[FitRecordAltitudeFieldNumber])
            ?.fitAltitudeMeters()

        points += ExerciseRoutePoint(
            time = timestamp.fitDateTimeInstant(),
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = altitudeMeters,
            horizontalAccuracyMeters = null,
            verticalAccuracyMeters = null,
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

internal fun ByteArray.isFitFile(): Boolean = isFitFileAt(0)

private fun ByteArray.isFitFileAt(offset: Int): Boolean {
    if (offset < 0 || offset + FitMinimumHeaderSize > size) return false
    val headerSize = this[offset].toUnsignedInt()
    return headerSize >= FitMinimumHeaderSize &&
        offset + headerSize <= size &&
        this[offset + FitHeaderDataTypeOffset] == '.'.code.toByte() &&
        this[offset + FitHeaderDataTypeOffset + 1] == 'F'.code.toByte() &&
        this[offset + FitHeaderDataTypeOffset + 2] == 'I'.code.toByte() &&
        this[offset + FitHeaderDataTypeOffset + 3] == 'T'.code.toByte()
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

private fun Long.fitSemicirclesToDegrees(): Double =
    toDouble() * 180.0 / FitSemicircleDegreesDivisor

private fun Long.fitAltitudeMeters(): Double =
    toDouble() / FitAltitudeScale - FitAltitudeOffsetMeters

private fun Long.fitDateTimeInstant(): Instant =
    Instant.ofEpochSecond(FitEpochUnixSeconds + this)

private fun Int.fitSportName(): String? =
    when (this) {
        1 -> "running"
        2,
        21 -> "cycling"
        5 -> "swimming"
        11 -> "walking"
        12,
        13 -> "skiing"
        14 -> "snowboarding"
        15 -> "rowing"
        17 -> "hiking"
        19,
        37,
        41,
        42 -> "paddling"
        25 -> "golf"
        30,
        33 -> "skating"
        32 -> "sailing"
        35 -> "snowshoeing"
        38 -> "surfing"
        else -> null
    }

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
private const val FitRecordMessageNumber = 20
private const val FitSessionMessageNumber = 18
private const val FitTimestampFieldNumber = 253
private const val FitSessionSportFieldNumber = 5
private const val FitRecordPositionLatFieldNumber = 0
private const val FitRecordPositionLongFieldNumber = 1
private const val FitRecordAltitudeFieldNumber = 2
private const val FitRecordEnhancedAltitudeFieldNumber = 78
private const val FitSportGeneric = 0
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
private const val FitEpochUnixSeconds = 631_065_600L
private const val FitSemicircleDegreesDivisor = 2_147_483_648.0
private const val FitAltitudeScale = 5.0
private const val FitAltitudeOffsetMeters = 500.0
private val FitParsedMessageNumbers = setOf(FitRecordMessageNumber, FitSessionMessageNumber)
