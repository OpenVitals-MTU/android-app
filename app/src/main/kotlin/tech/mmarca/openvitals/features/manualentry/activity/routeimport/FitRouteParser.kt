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

/**
 * A nightly HRV figure decoded from a Garmin wellness FIT file.
 *
 * The watch's own aggregate (`hrv_status_summary.last_night_average`), not a
 * beat-to-beat computation — Garmin already did the averaging.
 */
internal data class FitHrvReading(
    val time: Instant,
    val rmssdMillis: Double,
)

/**
 * Decodes the **activity** data a FIT file carries (route points, session
 * summary, and the per-record HR/speed/cadence series). Built on the generic
 * [FitDecoder]; the Garmin-proprietary wellness interpretation lives in
 * `devices/garmin/wellness/GarminFitWellness.kt`, consuming the same reader.
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

    /**
     * The nightly HRV readings a Garmin wellness FIT carries, if any. Empty for
     * activity/course/workout files — the caller uses this as the fallback when
     * a FIT file turns out not to be an activity at all.
     */
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
 * The per-record series, before the sport is known.
 *
 * FIT field 4 is just "cadence" — it does not say whether those are pedal
 * strokes or footfalls, and Health Connect keeps the two in different record
 * types. Only the session's sport can decide, and the session is parsed after
 * the records, so the kind is resolved last.
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
                    // FIT reports running cadence as STRIDES per minute — one
                    // leg. Health Connect wants steps. A runner at 90 spm is
                    // taking 180 steps.
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
 * Walks a (possibly chained) FIT byte stream through the generic [FitDecoder]
 * and interprets each file's messages into the activity carriers. One
 * [FitActivityInterpreter] per file; the results merge across the stream so a
 * later file falls back to — rather than concatenates with — an earlier file's
 * one-per-file scalar fields.
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

/**
 * Interprets one file's decoded [FitMessage]s into the activity raw structs.
 * Its switch cases are disjoint from the Garmin wellness interpreter's, so a
 * wellness file simply yields no points, an empty summary and no samples here
 * (bar the HRV summary, kept for [FitRouteParser.parseWellnessHrv]).
 */
private class FitActivityInterpreter {
    private val points = mutableListOf<ExerciseRoutePoint>()
    private val heartRateSamples = mutableListOf<BleHeartRateSample>()
    private val speedSamples = mutableListOf<BleSpeedSample>()
    private val cadenceSamples = mutableListOf<Pair<Instant, Int>>()
    private var fileType: Int? = null
    private var metadataName: String? = null
    /**
     * The name carried by the `activity` message, when the watch wrote one.
     * It is the title the wearer sees on the wrist and wins over the workout
     * name: a scheduled "Tempo run" workout executed as "Evening Run" should
     * import under the latter, which is the order Gadgetbridge settled on too
     * (activity > workout > session > sport).
     */
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
        // Dispatched in file order, so cases that depend on an earlier message
        // (file_id before record) still see it.
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
                // Read HERE and not in toFitActivitySummary, which serves the
                // lap message too — a lap's field 6 is end_position_long, and
                // reading a longitude as a sub-sport would name the activity at
                // random.
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

    /**
     * Garmin's nightly HRV: `hrv_status_summary.last_night_average`, a uint16
     * scaled by 128 into milliseconds of RMSSD, stamped by the message
     * timestamp. The uint16 invalid sentinel never reaches here — the generic
     * reader already drops it.
     */
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
     * Heart rate, cadence and speed, straight off the `record` message.
     *
     * FIT stores speed as an integer of millimetres per second (scale 1000),
     * and `enhanced_speed` is the same thing with more headroom, so it wins
     * when present. Heart rate and cadence are plain bytes. A zero cadence is a
     * real reading — you stopped pedalling — but a zero heart rate is not, so
     * only the latter is dropped.
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
                // Set from the session's sport once it is known — see
                // [FitSamples.resolve].
                isRunning = false,
            )
        }
    }

    private fun addRecordPoint(values: Map<Int, Long>, timestamp: Instant) {
        // BEFORE the GPS guard, deliberately. A record without a position still
        // carries a heart rate and a cadence — an indoor trainer session has
        // nothing else — and an early return would throw all of it away.
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
        // FIT session field 11 is `total_calories`. It was being written into
        // ACTIVE calories — the constant says TOTAL and the field it fed said
        // ACTIVE, and nothing objected.
        //
        // The consequence was not just a mislabelled number. Nothing then filled
        // `totalCalories`, so the form estimated one, and the estimate came out
        // BELOW the total that was sitting in the active field — so importing a
        // real ride produced "Total calories cannot be lower than active
        // calories" and would not save.
        //
        // The FIT session message has no separate active-calorie field, so
        // active is left unknown rather than invented. Null is honest; a number
        // is not.
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

/**
 * FIT sport 2 and 21 are cycling; everything else is on foot or in the water.
 *
 * It decides which Health Connect record the cadence goes into: pedalling
 * cadence and step cadence are different record types, and FIT field 4 is just
 * "cadence".
 */
private fun fitSportIsCycling(sport: Int?): Boolean = sport == 2 || sport == 21

/**
 * What the file says this was, in the words the type inference reads.
 *
 * The SUB-sport wins when it names the activity outright: a treadmill run is
 * not a run that happens to be indoors, it is a different Health Connect
 * exercise type, and the same goes for a trainer ride and a strength session.
 * Sub-sports that merely qualify an outdoor sport ("street", "trail", "road")
 * name nothing and leave the sport to speak.
 */
private fun fitSportName(sport: Int?, subSport: Int? = null): String? =
    fitSubSportName(subSport) ?: sport?.fitPlainSportName()

/** The sub-sports that ARE the activity. FIT `sub_sport` enum. */
private fun fitSubSportName(value: Int?): String? =
    when (value) {
        1 -> "treadmill"
        // 5 spin, 6 indoor_cycling — a trainer and a spin bike, both stationary.
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

/**
 * FIT `activity` message. Its `name` (field 8) is absent from Garmin's public
 * profile but present in `fit_profile.json` and written by watches that let
 * the wearer title an activity.
 */
private const val FitActivityMessageNumber = 34
private const val FitActivityNameFieldNumber = 8
private const val FitWorkoutStepDurationTypeFieldNumber = 1
private const val FitWorkoutStepDurationValueFieldNumber = 2
private const val FitStartTimeFieldNumber = 2
private const val FitSessionSportFieldNumber = 5

/**
 * FIT session field 6, `sub_sport`: the field that knows the session was run on
 * a TREADMILL rather than a street, and pedalled on a trainer rather than a
 * road. The sport alone cannot say — an indoor ride and an Alpine descent are
 * both sport 2 — and without it every indoor session imported as its outdoor
 * twin.
 */
private const val FitSessionSubSportFieldNumber = 6
private const val FitTotalElapsedTimeFieldNumber = 7
private const val FitTotalTimerTimeFieldNumber = 8
private const val FitTotalDistanceFieldNumber = 9
private const val FitTotalCaloriesFieldNumber = 11
private const val FitTotalAscentFieldNumber = 21
private const val FitRecordPositionLatFieldNumber = 0
private const val FitRecordPositionLongFieldNumber = 1
private const val FitRecordAltitudeFieldNumber = 2

// The per-record series a FIT carries beside each position. Before these were
// read, an import arrived with a route and nothing else: no heart rate, no
// cadence, no speed, and therefore not a single graph on the activity. An
// indoor ride — no positions at all — arrived with nothing whatsoever.
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
