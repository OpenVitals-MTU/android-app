package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import java.time.Instant
import kotlin.math.roundToLong
import tech.mmarca.openvitals.core.fit.FitDecoder
import tech.mmarca.openvitals.core.fit.FitMessage
import tech.mmarca.openvitals.core.fit.fitInstant
import tech.mmarca.openvitals.domain.model.BleCyclingCadenceSample
import tech.mmarca.openvitals.domain.model.BleHeartRateSample
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.model.BleSpeedSample
import tech.mmarca.openvitals.domain.model.BleStepsCadenceSample
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

/** A nightly HRV figure from a Garmin wellness FIT file: the watch's own average. */
internal data class FitHrvReading(
    val time: Instant,
    val rmssdMillis: Double,
)

/**
 * Decodes the activity data a FIT file carries: route, session summary and
 * the per-record series. Wellness lives in `GarminFitWellness.kt`.
 */
internal object FitRouteParser {
    fun parse(fitBytes: ByteArray, fileName: String? = null): RouteFileImport {
        val result = FitActivityDecoder(fitBytes).decode()
        val samples = result.samples.resolve(
            isCycling = fitSportIsCycling(result.summary.sport),
        )
        val routePoints = result.points
            .sortedBy { it.time }
            .distinctBy { it.time }
        return when (result.summary.fileType) {
            // A course is a planned route: it has no recorded series to carry.
            FitFileTypeCourse -> parseCourse(fileName, routePoints, result.summary)
            FitFileTypeWorkout -> parseWorkout(fileName, result.summary)
            else -> parseActivity(fileName, routePoints, result.summary)
                .copy(bleSamples = samples)
        }
    }

    /** The nightly HRV readings a wellness FIT carries. Empty for activity files. */
    fun parseWellnessHrv(fitBytes: ByteArray): List<FitHrvReading> =
        FitActivityDecoder(fitBytes).decode()
            .hrvReadings
            .distinctBy { it.time }
            .sortedBy { it.time }

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
            type = fitSportName(summary.sport, summary.subSport),
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
            type = fitSportName(summary.sport, summary.subSport),
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
            type = fitSportName(summary.sport, summary.subSport),
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
            type = fitSportName(summary.sport, summary.subSport),
            hasRecordedTimestamps = false,
            hasImportedTimeRange = false,
            originalPointCount = 0,
        )
    }
}

private data class FitDecodeResult(
    val points: List<ExerciseRoutePoint>,
    val summary: FitActivitySummary,
    val hrvReadings: List<FitHrvReading> = emptyList(),
    val samples: FitSamples = FitSamples(),
)

/**
 * The per-record series before the sport is known. FIT field 4 is just
 * "cadence"; only the sport decides pedal strokes or footfalls.
 */
private data class FitSamples(
    val heartRate: List<BleHeartRateSample> = emptyList(),
    val speed: List<BleSpeedSample> = emptyList(),
    val cadence: List<Pair<Instant, Int>> = emptyList(),
) {
    fun merge(other: FitSamples): FitSamples =
        FitSamples(
            heartRate = heartRate + other.heartRate,
            speed = speed + other.speed,
            cadence = cadence + other.cadence,
        )

    fun resolve(isCycling: Boolean): BleRecordingSampleBuffer =
        BleRecordingSampleBuffer(
            heartRateSamples = heartRate,
            speedSamples = speed.map { it.copy(isRunning = !isCycling) },
            cyclingCadenceSamples = if (isCycling) {
                cadence.map { (time, rpm) -> BleCyclingCadenceSample(time = time, rpm = rpm.toLong()) }
            } else {
                emptyList()
            },
            stepsCadenceSamples = if (isCycling) {
                emptyList()
            } else {
                cadence.map { (time, rate) ->
                    // FIT running cadence is strides per minute; Health Connect wants steps.
                    BleStepsCadenceSample(time = time, stepsPerMinute = rate.toLong() * 2)
                }
            },
        )
}

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
    val subSport: Int? = null,
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
            subSport = subSport ?: other.subSport,
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
            subSport = subSport ?: other.subSport,
        )
}

/**
 * Walks a chained FIT stream through [FitDecoder], one interpreter per file.
 * Later files fall back to, not concatenate with, earlier scalar fields.
 */
private class FitActivityDecoder(
    private val fileBytes: ByteArray,
) {
    fun decode(): FitDecodeResult {
        val points = mutableListOf<ExerciseRoutePoint>()
        val hrvReadings = mutableListOf<FitHrvReading>()
        var summary = FitActivitySummary()
        var samples = FitSamples()
        var offset = 0
        var decodedAnyFile = false

        while (offset < fileBytes.size) {
            if (!FitDecoder.isFitFileAt(fileBytes, offset)) {
                if (!decodedAnyFile) {
                    throw IllegalArgumentException("FIT file header is invalid.")
                }
                break
            }

            val file = FitDecoder.readFile(fileBytes, offset)
            val result = FitActivityInterpreter().interpret(file.messages)
            points += result.points
            hrvReadings += result.hrvReadings
            summary = summary.merge(result.summary)
            samples = samples.merge(result.samples)
            decodedAnyFile = true
            offset = file.nextOffset
        }

        return FitDecodeResult(
            points = points,
            summary = summary,
            hrvReadings = hrvReadings,
            samples = samples,
        )
    }
}

/** Interprets one file's [FitMessage]s. A wellness file yields nothing but the HRV summary. */
private class FitActivityInterpreter {
    private val points = mutableListOf<ExerciseRoutePoint>()
    private val heartRateSamples = mutableListOf<BleHeartRateSample>()
    private val speedSamples = mutableListOf<BleSpeedSample>()
    private val cadenceSamples = mutableListOf<Pair<Instant, Int>>()
    private var fileType: Int? = null
    private var metadataName: String? = null
    /** The `activity` message's name. Wins over the workout name, as in Gadgetbridge. */
    private var activityName: String? = null
    private var sport: Int? = null
    private var subSport: Int? = null
    private var firstRecordTime: Instant? = null
    private var lastRecordTime: Instant? = null
    private var sessionSummary = FitActivitySummary()
    private var lapSummary = FitActivitySummary()
    private var workoutDurationSeconds: Long? = null
    private var courseRecordIndex = 0L
    private val hrvReadings = mutableListOf<FitHrvReading>()

    fun interpret(messages: List<FitMessage>): FitDecodeResult {
        // File order matters: file_id must precede record.
        messages.forEach(::dispatch)
        return FitDecodeResult(
            points = points,
            summary = fitSummary(),
            hrvReadings = hrvReadings,
            samples = FitSamples(
                heartRate = heartRateSamples,
                speed = speedSamples,
                cadence = cadenceSamples,
            ),
        )
    }

    private fun dispatch(message: FitMessage) {
        val values = message.values
        val strings = message.strings
        val messageTimestamp = message.timestamp
        when (message.globalMessageNumber) {
            FitFileIdMessageNumber -> addFileId(values)
            FitCourseMessageNumber -> addCourseMetadata(values, strings)
            FitWorkoutMessageNumber -> addWorkoutMetadata(values, strings)
            FitWorkoutStepMessageNumber -> addWorkoutStep(values)
            FitActivityMessageNumber -> addActivityName(strings)
            FitRecordMessageNumber -> {
                if (fileType == FitFileTypeCourse) {
                    addCourseRecordPoint(values, messageTimestamp)
                } else {
                    rememberRecordTime(messageTimestamp)
                    addRecordPoint(values, messageTimestamp)
                }
            }
            FitLapMessageNumber -> addLapSummary(values, messageTimestamp)
            FitHrvStatusSummaryMessageNumber -> addHrvSummary(values, messageTimestamp)
            FitSessionMessageNumber -> {
                addSessionSummary(values, messageTimestamp)
                val sessionSport = values[FitSessionSportFieldNumber]
                    ?.toInt()
                    ?.takeUnless { it == FitSportGeneric }
                if (sport == null && sessionSport != null) {
                    sport = sessionSport
                }
                // Read here, not in toFitActivitySummary, which serves laps too:
                // a lap's field 6 is a longitude.
                val sessionSubSport = values[FitSessionSubSportFieldNumber]
                    ?.toInt()
                    ?.takeUnless { it == FitSportGeneric }
                if (subSport == null && sessionSubSport != null) {
                    subSport = sessionSubSport
                }
            }
        }
    }

    private fun addFileId(values: Map<Int, Long>) {
        fileType = values[FitFileIdTypeFieldNumber]?.toInt() ?: fileType
    }

    /** Garmin's nightly HRV: uint16 scaled by 128 into ms of RMSSD. */
    private fun addHrvSummary(values: Map<Int, Long>, timestampRaw: Long?) {
        val raw = values[FitHrvLastNightAverageFieldNumber] ?: return
        if (timestampRaw == null) return
        hrvReadings += FitHrvReading(
            time = fitInstant(timestampRaw),
            rmssdMillis = raw / FitHrvRmssdScale,
        )
    }

    private fun addCourseMetadata(values: Map<Int, Long>, strings: Map<Int, String>) {
        metadataName = metadataName ?: strings[FitCourseNameFieldNumber]
        sport = sport ?: values[FitCourseSportFieldNumber]
            ?.toInt()
            ?.takeUnless { it == FitSportGeneric }
    }

    private fun addActivityName(strings: Map<Int, String>) {
        if (activityName != null) return
        activityName = strings[FitActivityNameFieldNumber]?.trim()?.takeIf { it.isNotEmpty() }
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
        val time = timestampRaw?.let(::fitInstant) ?: return
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
                    name = activityName ?: metadataName,
                    durationSeconds = workoutDurationSeconds,
                    sport = sport,
                    subSport = subSport,
                )
            )
    }

    private fun addCourseRecordPoint(values: Map<Int, Long>, timestampRaw: Long?) {
        val timestamp = timestampRaw?.let(::fitInstant)
            ?: SyntheticFitStartTime.plusSeconds(courseRecordIndex)
        courseRecordIndex += 1
        addRecordPoint(values, timestamp)
    }

    private fun addRecordPoint(values: Map<Int, Long>, timestampRaw: Long?) {
        val timestamp = timestampRaw ?: return
        addRecordPoint(values, fitInstant(timestamp))
    }

    /**
     * Heart rate, cadence and speed off the `record` message. Speed is mm/s;
     * `enhanced_speed` wins when present. Zero cadence is real, zero heart
     * rate is not.
     */
    private fun addSamples(values: Map<Int, Long>, timestamp: Instant) {
        val bpm = values[FitRecordHeartRateFieldNumber]
        if (bpm != null && bpm > 0L && bpm < 300L) {
            heartRateSamples += BleHeartRateSample(time = timestamp, beatsPerMinute = bpm)
        }

        val cadence = values[FitRecordCadenceFieldNumber]
        if (cadence != null && cadence >= 0L && cadence < 250L) {
            cadenceSamples += timestamp to cadence.toInt()
        }

        val speedRaw = values[FitRecordEnhancedSpeedFieldNumber]
            ?: values[FitRecordSpeedFieldNumber]
        if (speedRaw != null && speedRaw > 0L) {
            speedSamples += BleSpeedSample(
                time = timestamp,
                metersPerSecond = speedRaw / FitSpeedScale,
                // Set from the session's sport once known; see [FitSamples.resolve].
                isRunning = false,
            )
        }
    }

    private fun addRecordPoint(values: Map<Int, Long>, timestamp: Instant) {
        // Before the GPS guard: an indoor session has samples but no position.
        addSamples(values, timestamp)

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
}

private fun Map<Int, Long>.toFitActivitySummary(timestampRaw: Long?): FitActivitySummary {
    val startTime = this[FitStartTimeFieldNumber]?.let(::fitInstant)
    val durationSeconds = (this[FitTotalElapsedTimeFieldNumber] ?: this[FitTotalTimerTimeFieldNumber])
        ?.fitScaledDouble(FitTimeScale)
    val endTime = when {
        startTime != null && durationSeconds != null && durationSeconds > 0.0 -> {
            startTime.plusMillis((durationSeconds * 1000.0).roundToLong())
        }
        timestampRaw != null -> fitInstant(timestampRaw)
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
        // Field 11 is total_calories. It was once written into active calories,
        // which made real rides fail validation. FIT has no active-calorie
        // field, so active stays null.
        totalCaloriesKcal = this[FitTotalCaloriesFieldNumber]?.toDouble(),
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

internal fun ByteArray.isFitFile(): Boolean = FitDecoder.isFitFile(this)

private fun Long.fitSemicirclesToDegrees(): Double =
    toDouble() * 180.0 / FitSemicircleDegreesDivisor

private fun Long.fitAltitudeMeters(): Double =
    toDouble() / FitAltitudeScale - FitAltitudeOffsetMeters

private fun Long.fitScaledDouble(scale: Double): Double =
    toDouble() / scale

/** FIT sport 2 and 21 are cycling. Decides which cadence record type is used. */
private fun fitSportIsCycling(sport: Int?): Boolean = sport == 2 || sport == 21

/**
 * What the file says this was. The sub-sport wins when it names the activity
 * (treadmill, trainer, strength); qualifiers like "trail" leave the sport to speak.
 */
private fun fitSportName(sport: Int?, subSport: Int? = null): String? =
    fitSubSportName(subSport) ?: sport?.fitPlainSportName()

/** The sub-sports that ARE the activity. FIT `sub_sport` enum. */
private fun fitSubSportName(value: Int?): String? =
    when (value) {
        1 -> "treadmill"
        // 5 spin, 6 indoor_cycling.
        5,
        6 -> "indoor cycling"
        14 -> "indoor rowing"
        20 -> "strength training"
        else -> null
    }

private fun Int.fitPlainSportName(): String? =
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

/** FIT `activity` message. Field 8 `name` is in `fit_profile.json`, not the public profile. */
private const val FitActivityMessageNumber = 34
private const val FitActivityNameFieldNumber = 8
private const val FitWorkoutStepDurationTypeFieldNumber = 1
private const val FitWorkoutStepDurationValueFieldNumber = 2
private const val FitStartTimeFieldNumber = 2
private const val FitSessionSportFieldNumber = 5

/** Session field 6, `sub_sport`: what tells a treadmill run from a street run. */
private const val FitSessionSubSportFieldNumber = 6
private const val FitTotalElapsedTimeFieldNumber = 7
private const val FitTotalTimerTimeFieldNumber = 8
private const val FitTotalDistanceFieldNumber = 9
private const val FitTotalCaloriesFieldNumber = 11
private const val FitTotalAscentFieldNumber = 21
private const val FitRecordPositionLatFieldNumber = 0
private const val FitRecordPositionLongFieldNumber = 1
private const val FitRecordAltitudeFieldNumber = 2

// The per-record series beside each position.
private const val FitRecordHeartRateFieldNumber = 3
private const val FitRecordCadenceFieldNumber = 4
private const val FitRecordSpeedFieldNumber = 6
private const val FitRecordEnhancedSpeedFieldNumber = 73
private const val FitRecordEnhancedAltitudeFieldNumber = 78

/** FIT stores speed as an integer of millimetres per second. */
private const val FitSpeedScale = 1000.0
private const val FitSportGeneric = 0
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

private const val FitHrvStatusSummaryMessageNumber = 370
private const val FitHrvLastNightAverageFieldNumber = 1
private const val FitHrvRmssdScale = 128.0
