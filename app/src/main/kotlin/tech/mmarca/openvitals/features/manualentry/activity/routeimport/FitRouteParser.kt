package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import java.time.Instant
import kotlin.math.roundToLong
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

internal object FitRouteParser {
    fun parse(fitBytes: ByteArray, fileName: String? = null): RouteFileImport {
        val result = FitDecoder(fitBytes).decode()
        val routePoints = result.points
            .sortedBy { it.time }
            .distinctBy { it.time }
        return when (result.summary.fileType) {
            FitFileTypeCourse -> parseCourse(fileName, routePoints, result.summary)
            FitFileTypeWorkout -> parseWorkout(fileName, result.summary)
            else -> parseActivity(fileName, routePoints, result.summary)
        }
    }

    private fun parseActivity(
        fileName: String?,
        routePoints: List<ExerciseRoutePoint>,
        summary: FitActivitySummary,
    ): RouteFileImport {
        val startTime = summary.startTime
            ?: routePoints.firstOrNull()?.time
            ?: throw IllegalArgumentException("FIT file does not contain an activity session or timestamped activity records.")
        val endTime = (summary.endTime ?: routePoints.lastOrNull()?.time)
            ?.takeIf { startTime.isBefore(it) }
            ?: startTime.plusSeconds(1)
        val metadata = RouteFileMetadata(
            name = summary.name,
            description = null,
            type = summary.sport?.fitSportName(),
        )

        if (routePoints.size >= MinRoutePoints) {
            return buildRouteImport(
                fileName = fileName,
                points = routePoints,
                metadata = metadata,
            ).copy(
                distanceMeters = summary.distanceMeters ?: routeDistanceMeters(routePoints),
                elevationGainedMeters = summary.elevationGainedMeters ?: routeElevationGainMeters(routePoints),
                activeCaloriesKcal = summary.activeCaloriesKcal,
                totalCaloriesKcal = summary.totalCaloriesKcal,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = summary.durationSeconds,
                originalPointCount = routePoints.size,
            )
        }

        return RouteFileImport(
            fileName = fileName,
            points = emptyList(),
            distanceMeters = summary.distanceMeters ?: 0.0,
            elevationGainedMeters = summary.elevationGainedMeters ?: 0.0,
            activeCaloriesKcal = summary.activeCaloriesKcal,
            totalCaloriesKcal = summary.totalCaloriesKcal,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = summary.durationSeconds,
            name = summary.name,
            description = null,
            type = summary.sport?.fitSportName(),
            hasRecordedTimestamps = true,
            hasImportedTimeRange = true,
            originalPointCount = routePoints.size,
        )
    }

    private fun parseCourse(
        fileName: String?,
        routePoints: List<ExerciseRoutePoint>,
        summary: FitActivitySummary,
    ): RouteFileImport {
        val metadata = RouteFileMetadata(
            name = summary.name,
            description = null,
            type = summary.sport?.fitSportName(),
        )
        if (routePoints.size >= MinRoutePoints) {
            return buildRouteImport(
                fileName = fileName,
                points = routePoints,
                metadata = metadata,
                hasRecordedTimestamps = false,
                hasImportedTimeRange = false,
            ).copy(
                distanceMeters = summary.distanceMeters ?: routeDistanceMeters(routePoints),
                elevationGainedMeters = summary.elevationGainedMeters ?: routeElevationGainMeters(routePoints),
                durationSeconds = summary.durationSeconds,
            )
        }

        val startTime = summary.startTime
            ?: routePoints.firstOrNull()?.time
            ?: SyntheticFitStartTime
        val endTime = summary.endTime
            ?.takeIf { startTime.isBefore(it) }
            ?: routePoints.lastOrNull()?.time?.takeIf { startTime.isBefore(it) }
            ?: startTime.plusSeconds(summary.durationSeconds?.coerceAtLeast(1) ?: 1L)

        return RouteFileImport(
            fileName = fileName,
            points = emptyList(),
            distanceMeters = summary.distanceMeters ?: 0.0,
            elevationGainedMeters = summary.elevationGainedMeters ?: 0.0,
            activeCaloriesKcal = summary.activeCaloriesKcal,
            totalCaloriesKcal = summary.totalCaloriesKcal,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = summary.durationSeconds,
            name = metadata.name,
            description = metadata.description,
            type = metadata.type,
            hasRecordedTimestamps = false,
            hasImportedTimeRange = false,
            originalPointCount = routePoints.size,
        )
    }

    private fun parseWorkout(fileName: String?, summary: FitActivitySummary): RouteFileImport {
        val durationSeconds = summary.durationSeconds?.coerceAtLeast(1)
        return RouteFileImport(
            fileName = fileName,
            points = emptyList(),
            distanceMeters = summary.distanceMeters ?: 0.0,
            elevationGainedMeters = summary.elevationGainedMeters ?: 0.0,
            activeCaloriesKcal = summary.activeCaloriesKcal,
            totalCaloriesKcal = summary.totalCaloriesKcal,
            startTime = SyntheticFitStartTime,
            endTime = SyntheticFitStartTime.plusSeconds(durationSeconds ?: DefaultFitWorkoutDurationSeconds),
            durationSeconds = durationSeconds,
            name = summary.name,
            description = null,
            type = summary.sport?.fitSportName(),
            hasRecordedTimestamps = false,
            hasImportedTimeRange = false,
            originalPointCount = 0,
        )
    }
}

private data class FitDecodeResult(
    val points: List<ExerciseRoutePoint>,
    val summary: FitActivitySummary,
)

private data class FitFileDecodeResult(
    val points: List<ExerciseRoutePoint>,
    val summary: FitActivitySummary,
    val nextOffset: Int,
)

private data class FitActivitySummary(
    val fileType: Int? = null,
    val name: String? = null,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val durationSeconds: Long? = null,
    val distanceMeters: Double? = null,
    val elevationGainedMeters: Double? = null,
    val activeCaloriesKcal: Double? = null,
    val totalCaloriesKcal: Double? = null,
    val sport: Int? = null,
) {
    fun merge(other: FitActivitySummary): FitActivitySummary =
        FitActivitySummary(
            fileType = fileType ?: other.fileType,
            name = name ?: other.name,
            startTime = startTime.earliest(other.startTime),
            endTime = endTime.latest(other.endTime),
            durationSeconds = durationSeconds.sumWith(other.durationSeconds),
            distanceMeters = distanceMeters.sumWith(other.distanceMeters),
            elevationGainedMeters = elevationGainedMeters.sumWith(other.elevationGainedMeters),
            activeCaloriesKcal = activeCaloriesKcal.sumWith(other.activeCaloriesKcal),
            totalCaloriesKcal = totalCaloriesKcal.sumWith(other.totalCaloriesKcal),
            sport = sport ?: other.sport,
        )

    fun withFallback(other: FitActivitySummary): FitActivitySummary =
        FitActivitySummary(
            fileType = fileType ?: other.fileType,
            name = name ?: other.name,
            startTime = startTime ?: other.startTime,
            endTime = endTime ?: other.endTime,
            durationSeconds = durationSeconds ?: other.durationSeconds,
            distanceMeters = distanceMeters ?: other.distanceMeters,
            elevationGainedMeters = elevationGainedMeters ?: other.elevationGainedMeters,
            activeCaloriesKcal = activeCaloriesKcal ?: other.activeCaloriesKcal,
            totalCaloriesKcal = totalCaloriesKcal ?: other.totalCaloriesKcal,
            sport = sport ?: other.sport,
        )
}

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
        var summary = FitActivitySummary()
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
            summary = summary.merge(result.summary)
            decodedAnyFile = true
            offset = result.nextOffset
        }

        return FitDecodeResult(
            points = points,
            summary = summary,
        )
    }
}

private class FitSingleFileDecoder(
    private val fileBytes: ByteArray,
    private val startOffset: Int,
) {
    private val definitions = mutableMapOf<Int, FitMessageDefinition>()
    private val points = mutableListOf<ExerciseRoutePoint>()
    private var fileType: Int? = null
    private var metadataName: String? = null
    private var sport: Int? = null
    private var lastTimestampRaw: Long? = null
    private var firstRecordTime: Instant? = null
    private var lastRecordTime: Instant? = null
    private var sessionSummary = FitActivitySummary()
    private var lapSummary = FitActivitySummary()
    private var workoutDurationSeconds: Long? = null
    private var courseRecordIndex = 0L

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
            summary = fitSummary(),
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
        val strings = mutableMapOf<Int, String>()

        definition.fields.forEach { field ->
            val fieldBytes = reader.readBytes(field.size)
            if (field.number == FitTimestampFieldNumber || definition.globalMessageNumber in FitParsedMessageNumbers) {
                fieldBytes.fitLong(field, definition.littleEndian)?.let { value ->
                    values[field.number] = value
                }
                fieldBytes.fitString(field)?.let { value ->
                    strings[field.number] = value
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
            FitFileIdMessageNumber -> addFileId(values)
            FitCourseMessageNumber -> addCourseMetadata(values, strings)
            FitWorkoutMessageNumber -> addWorkoutMetadata(values, strings)
            FitWorkoutStepMessageNumber -> addWorkoutStep(values)
            FitRecordMessageNumber -> {
                if (fileType == FitFileTypeCourse) {
                    addCourseRecordPoint(values, messageTimestamp)
                } else {
                    rememberRecordTime(messageTimestamp)
                    addRecordPoint(values, messageTimestamp)
                }
            }
            FitLapMessageNumber -> addLapSummary(values, messageTimestamp)
            FitSessionMessageNumber -> {
                addSessionSummary(values, messageTimestamp)
                val sessionSport = values[FitSessionSportFieldNumber]
                    ?.toInt()
                    ?.takeUnless { it == FitSportGeneric }
                if (sport == null && sessionSport != null) {
                    sport = sessionSport
                }
            }
        }
    }

    private fun addFileId(values: Map<Int, Long>) {
        fileType = values[FitFileIdTypeFieldNumber]?.toInt() ?: fileType
    }

    private fun addCourseMetadata(values: Map<Int, Long>, strings: Map<Int, String>) {
        metadataName = metadataName ?: strings[FitCourseNameFieldNumber]
        sport = sport ?: values[FitCourseSportFieldNumber]
            ?.toInt()
            ?.takeUnless { it == FitSportGeneric }
    }

    private fun addWorkoutMetadata(values: Map<Int, Long>, strings: Map<Int, String>) {
        metadataName = metadataName ?: strings[FitWorkoutNameFieldNumber]
        sport = sport ?: values[FitWorkoutSportFieldNumber]
            ?.toInt()
            ?.takeUnless { it == FitSportGeneric }
    }

    private fun addWorkoutStep(values: Map<Int, Long>) {
        val durationType = values[FitWorkoutStepDurationTypeFieldNumber]?.toInt() ?: return
        val durationValue = values[FitWorkoutStepDurationValueFieldNumber] ?: return
        val seconds = when (durationType) {
            FitWorkoutDurationTypeTime,
            FitWorkoutDurationTypeRepeatUntilTime,
            FitWorkoutDurationTypeRepetitionTime -> durationValue.fitScaledDouble(FitTimeScale).roundToLong()
            else -> null
        }?.takeIf { it > 0L } ?: return
        workoutDurationSeconds = workoutDurationSeconds.sumWith(seconds)
    }

    private fun addSessionSummary(values: Map<Int, Long>, timestampRaw: Long?) {
        sessionSummary = sessionSummary.merge(values.toFitActivitySummary(timestampRaw))
    }

    private fun addLapSummary(values: Map<Int, Long>, timestampRaw: Long?) {
        lapSummary = lapSummary.merge(values.toFitActivitySummary(timestampRaw))
    }

    private fun rememberRecordTime(timestampRaw: Long?) {
        val time = timestampRaw?.fitDateTimeInstant() ?: return
        firstRecordTime = firstRecordTime.earliest(time)
        lastRecordTime = lastRecordTime.latest(time)
    }

    private fun fitSummary(): FitActivitySummary {
        val recordSummary = FitActivitySummary(
            startTime = firstRecordTime,
            endTime = lastRecordTime,
            durationSeconds = firstRecordTime?.let { start ->
                lastRecordTime?.let { end ->
                    java.time.Duration.between(start, end).seconds.takeIf { it > 0L }
                }
            },
        )
        return sessionSummary
            .withFallback(lapSummary)
            .withFallback(recordSummary)
            .withFallback(
                FitActivitySummary(
                    fileType = fileType,
                    name = metadataName,
                    durationSeconds = workoutDurationSeconds,
                    sport = sport,
                )
            )
    }

    private fun addCourseRecordPoint(values: Map<Int, Long>, timestampRaw: Long?) {
        val timestamp = timestampRaw?.fitDateTimeInstant()
            ?: SyntheticFitStartTime.plusSeconds(courseRecordIndex)
        courseRecordIndex += 1
        addRecordPoint(values, timestamp)
    }

    private fun addRecordPoint(values: Map<Int, Long>, timestampRaw: Long?) {
        val timestamp = timestampRaw ?: return
        addRecordPoint(values, timestamp.fitDateTimeInstant())
    }

    private fun addRecordPoint(values: Map<Int, Long>, timestamp: Instant) {
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
            time = timestamp,
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

private fun Map<Int, Long>.toFitActivitySummary(timestampRaw: Long?): FitActivitySummary {
    val startTime = this[FitStartTimeFieldNumber]?.fitDateTimeInstant()
    val durationSeconds = (this[FitTotalElapsedTimeFieldNumber] ?: this[FitTotalTimerTimeFieldNumber])
        ?.fitScaledDouble(FitTimeScale)
    val endTime = when {
        startTime != null && durationSeconds != null && durationSeconds > 0.0 -> {
            startTime.plusMillis((durationSeconds * 1000.0).roundToLong())
        }
        timestampRaw != null -> timestampRaw.fitDateTimeInstant()
        else -> null
    }
    val sport = this[FitSessionSportFieldNumber]
        ?.toInt()
        ?.takeUnless { it == FitSportGeneric }

    return FitActivitySummary(
        startTime = startTime,
        endTime = endTime,
        durationSeconds = durationSeconds?.roundToLong(),
        distanceMeters = this[FitTotalDistanceFieldNumber]?.fitScaledDouble(FitDistanceScale),
        elevationGainedMeters = this[FitTotalAscentFieldNumber]?.toDouble(),
        activeCaloriesKcal = this[FitTotalCaloriesFieldNumber]?.toDouble(),
        sport = sport,
    )
}

private fun Instant?.earliest(other: Instant?): Instant? =
    when {
        this == null -> other
        other == null -> this
        isBefore(other) -> this
        else -> other
    }

private fun Instant?.latest(other: Instant?): Instant? =
    when {
        this == null -> other
        other == null -> this
        isAfter(other) -> this
        else -> other
    }

private fun Double?.sumWith(other: Double?): Double? =
    when {
        this == null -> other
        other == null -> this
        else -> this + other
    }

private fun Long?.sumWith(other: Long?): Long? =
    when {
        this == null -> other
        other == null -> this
        else -> this + other
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

private fun ByteArray.fitString(field: FitFieldDefinition): String? {
    val baseType = field.baseType and FitBaseTypeMask
    if (baseType != FitBaseTypeString) return null
    return toString(Charsets.UTF_8)
        .trimEnd('\u0000')
        .cleanText()
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

private fun Long.fitScaledDouble(scale: Double): Double =
    toDouble() / scale

private fun Long.fitDateTimeInstant(): Instant =
    Instant.ofEpochSecond(FitEpochUnixSeconds + this)

private fun Int.fitSportName(): String? =
    when (this) {
        1 -> "running"
        2,
        21 -> "cycling"
        4 -> "fitness equipment"
        5 -> "swimming"
        10 -> "training"
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
        47 -> "boxing"
        62 -> "interval training"
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
private const val FitFileIdMessageNumber = 0
private const val FitFileIdTypeFieldNumber = 0
private const val FitFileTypeWorkout = 5
private const val FitFileTypeCourse = 6
private const val FitRecordMessageNumber = 20
private const val FitLapMessageNumber = 19
private const val FitSessionMessageNumber = 18
private const val FitCourseMessageNumber = 31
private const val FitCourseSportFieldNumber = 4
private const val FitCourseNameFieldNumber = 5
private const val FitWorkoutMessageNumber = 26
private const val FitWorkoutSportFieldNumber = 4
private const val FitWorkoutNameFieldNumber = 8
private const val FitWorkoutStepMessageNumber = 27
private const val FitWorkoutStepDurationTypeFieldNumber = 1
private const val FitWorkoutStepDurationValueFieldNumber = 2
private const val FitTimestampFieldNumber = 253
private const val FitStartTimeFieldNumber = 2
private const val FitSessionSportFieldNumber = 5
private const val FitTotalElapsedTimeFieldNumber = 7
private const val FitTotalTimerTimeFieldNumber = 8
private const val FitTotalDistanceFieldNumber = 9
private const val FitTotalCaloriesFieldNumber = 11
private const val FitTotalAscentFieldNumber = 21
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
private const val FitTimeScale = 1000.0
private const val FitDistanceScale = 100.0
private const val FitWorkoutDurationTypeTime = 0
private const val FitWorkoutDurationTypeRepeatUntilTime = 7
private const val FitWorkoutDurationTypeRepetitionTime = 28
private const val DefaultFitWorkoutDurationSeconds = 30 * 60L
private val SyntheticFitStartTime: Instant = Instant.EPOCH
private val FitParsedMessageNumbers = setOf(
    FitFileIdMessageNumber,
    FitRecordMessageNumber,
    FitLapMessageNumber,
    FitSessionMessageNumber,
    FitCourseMessageNumber,
    FitWorkoutMessageNumber,
    FitWorkoutStepMessageNumber,
)
