package tech.mmarca.openvitals.features.activity

import androidx.health.connect.client.records.ExerciseSessionRecord
import java.io.OutputStream
import kotlin.math.floor
import kotlin.math.roundToLong
import tech.mmarca.openvitals.core.fit.FitBaseType
import tech.mmarca.openvitals.core.fit.FitEncoder
import tech.mmarca.openvitals.core.fit.FitEncoderField
import tech.mmarca.openvitals.core.fit.fitSemicircles
import tech.mmarca.openvitals.core.fit.fitTimestamp
import tech.mmarca.openvitals.domain.model.ActivityCadenceKind
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

/**
 * A FIT Activity file in the order Garmin's cookbook writes one: file_id, a
 * timer start, one record per second that has a sample, the timer stop, one
 * lap, one session, the activity. Records carry heart rate, speed and
 * cadence. Without [routePoints] no position field is defined anywhere,
 * which is the routeless export's promise; with them the records also carry
 * position, altitude and the distance covered along the route.
 *
 * Ascent is written as session 22 (the FIT profile) and lap 21 (what
 * `FitRouteParser` reads on both). Session power fields (20/21) and lap
 * position fields (3-6) are never written: the parser reads session 21 as
 * ascent and lap 5 as the sport. Everything is a scaled integer.
 */
internal fun writeActivityWorkoutFit(
    workout: ExerciseData,
    series: ActivityWorkoutSeries,
    output: OutputStream,
    routePoints: List<ExerciseRoutePoint> = emptyList(),
) {
    val timeline = workout.workoutExportTimeline(series, routePoints)
    val totals = workout.fitTotals(series, timeline)
    val encoder = FitEncoder()
    encoder.writeFitFileId(totals)
    encoder.defineMessage(
        FitLocalEvent, FitEventMessage,
        listOf(
            FitEncoderField(FitTimestampField, FitBaseType.UINT32),
            FitEncoderField(FitEventField, FitBaseType.ENUM),
            FitEncoderField(FitEventTypeField, FitBaseType.ENUM),
        ),
    )
    encoder.writeFitTimerEvent(totals.startTimestamp, FitEventTypeStart)
    encoder.writeFitRecords(timeline, workout.exportCadenceKind())
    encoder.writeFitTimerEvent(totals.endTimestamp, FitEventTypeStopAll)
    encoder.writeFitLap(totals)
    encoder.writeFitSession(totals, workout.exerciseType.toFitSport())
    encoder.writeFitActivity(workout, totals)
    encoder.writeTo(output)
}

/** What the lap and the session both say, already in FIT units. */
private class FitWorkoutTotals(
    val startTimestamp: Long,
    val endTimestamp: Long,
    /** total_elapsed/timer_time carry ms directly (scale 1000). */
    val durationScaled: Long,
    val distanceScaled: Long?,
    val calories: Long?,
    val ascentMeters: Long?,
    val averageBpm: Long?,
    val maximumBpm: Long?,
    val averageSpeed: Long?,
    val maximumSpeed: Long?,
    val averageCadence: Long?,
    val maximumCadence: Long?,
)

private fun ExerciseData.fitTotals(
    series: ActivityWorkoutSeries,
    timeline: List<WorkoutExportPoint>,
): FitWorkoutTotals {
    val samples = heartRateSamplesForExport(series.heartRate)
    val cadenceKind = exportCadenceKind()
    return FitWorkoutTotals(
        startTimestamp = fitTimestamp(startTime),
        endTimestamp = fitTimestamp(endTime),
        durationScaled = durationMs,
        distanceScaled = totalDistanceMeters?.let { (it * FitDistanceScale).roundToLong() },
        calories = (totalCaloriesKcal ?: activeCaloriesKcal)?.roundToLong()?.coerceIn(0, 0xFFFE),
        ascentMeters = elevationGainedMeters?.roundToLong()?.coerceIn(0, 0xFFFE),
        averageBpm = averageHeartRateForExport(samples)?.coerceIn(1, 255),
        maximumBpm = samples.maxOfOrNull { it.beatsPerMinute }?.coerceIn(1, 255),
        averageSpeed = averageSpeedForExport(timeline)?.toFitSpeed(),
        maximumSpeed = timeline.mapNotNull { it.speedMetersPerSecond }.maxOrNull()?.toFitSpeed(),
        averageCadence = averageCadenceForExport(timeline)?.toFitCadence(cadenceKind)?.toFitCadenceByte(),
        maximumCadence = timeline.mapNotNull { it.cadence }.maxOrNull()?.toFitCadence(cadenceKind)?.toFitCadenceByte(),
    )
}

private fun FitEncoder.writeFitFileId(totals: FitWorkoutTotals) {
    defineMessage(
        FitLocalFileId, FitFileIdMessage,
        listOf(
            FitEncoderField(FitFileIdTypeField, FitBaseType.ENUM),
            FitEncoderField(FitFileIdManufacturerField, FitBaseType.UINT16),
            FitEncoderField(FitFileIdProductField, FitBaseType.UINT16),
            FitEncoderField(FitFileIdTimeCreatedField, FitBaseType.UINT32),
        ),
    )
    writeMessage(
        FitLocalFileId,
        mapOf(
            FitFileIdTypeField to FitFileTypeActivity,
            FitFileIdManufacturerField to FitManufacturerDevelopment,
            FitFileIdProductField to 1L,
            FitFileIdTimeCreatedField to totals.startTimestamp,
        ),
    )
}

private fun FitEncoder.writeFitTimerEvent(timestamp: Long, eventType: Long) {
    writeMessage(
        FitLocalEvent,
        mapOf(
            FitTimestampField to timestamp,
            FitEventField to FitEventTimer,
            FitEventTypeField to eventType,
        ),
    )
}

/** Only the fields some second has are defined; a second without one writes the invalid value. */
private fun FitEncoder.writeFitRecords(
    timeline: List<WorkoutExportPoint>,
    cadenceKind: ActivityCadenceKind,
) {
    if (timeline.isEmpty()) return
    val fields = buildList {
        add(FitEncoderField(FitTimestampField, FitBaseType.UINT32))
        if (timeline.any { it.position != null }) {
            add(FitEncoderField(FitRecordPositionLatField, FitBaseType.SINT32))
            add(FitEncoderField(FitRecordPositionLongField, FitBaseType.SINT32))
            add(FitEncoderField(FitRecordDistanceField, FitBaseType.UINT32))
        }
        if (timeline.any { it.position?.altitudeMeters != null }) {
            add(FitEncoderField(FitRecordAltitudeField, FitBaseType.UINT16))
        }
        if (timeline.any { it.heartRateBpm != null }) {
            add(FitEncoderField(FitRecordHeartRateField, FitBaseType.UINT8))
        }
        if (timeline.any { it.cadence != null }) {
            add(FitEncoderField(FitRecordCadenceField, FitBaseType.UINT8))
            add(FitEncoderField(FitRecordFractionalCadenceField, FitBaseType.UINT8))
        }
        if (timeline.any { it.speedMetersPerSecond != null }) {
            add(FitEncoderField(FitRecordSpeedField, FitBaseType.UINT16))
        }
    }
    defineMessage(FitLocalRecord, FitRecordMessage, fields)
    timeline.forEach { point ->
        val cadence = point.cadence?.toFitCadence(cadenceKind)
        val wholeCadence = cadence?.let { floor(it).toLong().coerceIn(0, MaxFitCadence) }
        writeMessage(
            FitLocalRecord,
            mapOf(
                FitTimestampField to fitTimestamp(point.time),
                FitRecordPositionLatField to point.position?.let { fitSemicircles(it.latitude) },
                FitRecordPositionLongField to point.position?.let { fitSemicircles(it.longitude) },
                FitRecordDistanceField to point.distanceMeters?.let { (it * FitDistanceScale).roundToLong() },
                FitRecordAltitudeField to point.position?.altitudeMeters?.toFitAltitude(),
                FitRecordHeartRateField to point.heartRateBpm,
                FitRecordCadenceField to wholeCadence,
                FitRecordFractionalCadenceField to cadence?.let { value ->
                    ((value - checkNotNull(wholeCadence)) * FitFractionalCadenceScale)
                        .roundToLong()
                        .coerceIn(0, FitFractionalCadenceScale.toLong() - 1)
                },
                FitRecordSpeedField to point.speedMetersPerSecond?.toFitSpeed(),
            ),
        )
    }
}

private fun FitEncoder.writeFitLap(totals: FitWorkoutTotals) {
    defineMessage(
        FitLocalLap, FitLapMessage,
        listOf(
            FitEncoderField(FitTimestampField, FitBaseType.UINT32),
            FitEncoderField(FitMessageIndexField, FitBaseType.UINT16),
            FitEncoderField(FitEventField, FitBaseType.ENUM),
            FitEncoderField(FitEventTypeField, FitBaseType.ENUM),
            FitEncoderField(FitStartTimeField, FitBaseType.UINT32),
            FitEncoderField(FitTotalElapsedTimeField, FitBaseType.UINT32),
            FitEncoderField(FitTotalTimerTimeField, FitBaseType.UINT32),
            FitEncoderField(FitTotalDistanceField, FitBaseType.UINT32),
            FitEncoderField(FitTotalCaloriesField, FitBaseType.UINT16),
            FitEncoderField(FitLapAvgSpeedField, FitBaseType.UINT16),
            FitEncoderField(FitLapMaxSpeedField, FitBaseType.UINT16),
            FitEncoderField(FitLapAvgHeartRateField, FitBaseType.UINT8),
            FitEncoderField(FitLapMaxHeartRateField, FitBaseType.UINT8),
            FitEncoderField(FitLapAvgCadenceField, FitBaseType.UINT8),
            FitEncoderField(FitLapMaxCadenceField, FitBaseType.UINT8),
            FitEncoderField(FitLapTotalAscentField, FitBaseType.UINT16),
        ),
    )
    writeMessage(
        FitLocalLap,
        mapOf(
            FitTimestampField to totals.endTimestamp,
            FitMessageIndexField to 0L,
            FitEventField to FitEventLap,
            FitEventTypeField to FitEventTypeStop,
            FitStartTimeField to totals.startTimestamp,
            FitTotalElapsedTimeField to totals.durationScaled,
            FitTotalTimerTimeField to totals.durationScaled,
            FitTotalDistanceField to totals.distanceScaled,
            FitTotalCaloriesField to totals.calories,
            FitLapAvgSpeedField to totals.averageSpeed,
            FitLapMaxSpeedField to totals.maximumSpeed,
            FitLapAvgHeartRateField to totals.averageBpm,
            FitLapMaxHeartRateField to totals.maximumBpm,
            FitLapAvgCadenceField to totals.averageCadence,
            FitLapMaxCadenceField to totals.maximumCadence,
            FitLapTotalAscentField to totals.ascentMeters,
        ),
    )
}

private fun FitEncoder.writeFitSession(totals: FitWorkoutTotals, sport: Pair<Long, Long>) {
    defineMessage(
        FitLocalSession, FitSessionMessage,
        listOf(
            FitEncoderField(FitTimestampField, FitBaseType.UINT32),
            FitEncoderField(FitMessageIndexField, FitBaseType.UINT16),
            FitEncoderField(FitEventField, FitBaseType.ENUM),
            FitEncoderField(FitEventTypeField, FitBaseType.ENUM),
            FitEncoderField(FitStartTimeField, FitBaseType.UINT32),
            FitEncoderField(FitSessionSportField, FitBaseType.ENUM),
            FitEncoderField(FitSessionSubSportField, FitBaseType.ENUM),
            FitEncoderField(FitTotalElapsedTimeField, FitBaseType.UINT32),
            FitEncoderField(FitTotalTimerTimeField, FitBaseType.UINT32),
            FitEncoderField(FitTotalDistanceField, FitBaseType.UINT32),
            FitEncoderField(FitTotalCaloriesField, FitBaseType.UINT16),
            FitEncoderField(FitSessionAvgSpeedField, FitBaseType.UINT16),
            FitEncoderField(FitSessionMaxSpeedField, FitBaseType.UINT16),
            FitEncoderField(FitSessionAvgHeartRateField, FitBaseType.UINT8),
            FitEncoderField(FitSessionMaxHeartRateField, FitBaseType.UINT8),
            FitEncoderField(FitSessionAvgCadenceField, FitBaseType.UINT8),
            FitEncoderField(FitSessionMaxCadenceField, FitBaseType.UINT8),
            FitEncoderField(FitSessionTotalAscentField, FitBaseType.UINT16),
            FitEncoderField(FitSessionFirstLapIndexField, FitBaseType.UINT16),
            FitEncoderField(FitSessionNumLapsField, FitBaseType.UINT16),
        ),
    )
    writeMessage(
        FitLocalSession,
        mapOf(
            FitTimestampField to totals.endTimestamp,
            FitMessageIndexField to 0L,
            FitEventField to FitEventSession,
            FitEventTypeField to FitEventTypeStop,
            FitStartTimeField to totals.startTimestamp,
            FitSessionSportField to sport.first,
            FitSessionSubSportField to sport.second,
            FitTotalElapsedTimeField to totals.durationScaled,
            FitTotalTimerTimeField to totals.durationScaled,
            FitTotalDistanceField to totals.distanceScaled,
            FitTotalCaloriesField to totals.calories,
            FitSessionAvgSpeedField to totals.averageSpeed,
            FitSessionMaxSpeedField to totals.maximumSpeed,
            FitSessionAvgHeartRateField to totals.averageBpm,
            FitSessionMaxHeartRateField to totals.maximumBpm,
            FitSessionAvgCadenceField to totals.averageCadence,
            FitSessionMaxCadenceField to totals.maximumCadence,
            FitSessionTotalAscentField to totals.ascentMeters,
            FitSessionFirstLapIndexField to 0L,
            FitSessionNumLapsField to 1L,
        ),
    )
}

/** local_timestamp lets a viewer show the local start; it needs the session's zone offset. */
private fun FitEncoder.writeFitActivity(workout: ExerciseData, totals: FitWorkoutTotals) {
    val title = workout.title?.takeIf { it.isNotBlank() }
    val zoneOffset = workout.endZoneOffset ?: workout.startZoneOffset
    val activityFields = mutableListOf(
        FitEncoderField(FitTimestampField, FitBaseType.UINT32),
        FitEncoderField(FitActivityTotalTimerTimeField, FitBaseType.UINT32),
        FitEncoderField(FitActivityNumSessionsField, FitBaseType.UINT16),
        FitEncoderField(FitActivityTypeField, FitBaseType.ENUM),
        FitEncoderField(FitActivityEventField, FitBaseType.ENUM),
        FitEncoderField(FitActivityEventTypeField, FitBaseType.ENUM),
    )
    if (zoneOffset != null) {
        activityFields += FitEncoderField(FitActivityLocalTimestampField, FitBaseType.UINT32)
    }
    if (title != null) {
        // FIT strings are fixed-size; size the field to this title, capped.
        val titleSize = minOf(title.toByteArray(Charsets.UTF_8).size + 1, MaxFitActivityNameBytes)
        activityFields += FitEncoderField(FitActivityNameField, FitBaseType.STRING, titleSize)
    }
    defineMessage(FitLocalActivity, FitActivityMessage, activityFields)
    writeMessage(
        FitLocalActivity,
        values = mapOf(
            FitTimestampField to totals.endTimestamp,
            FitActivityTotalTimerTimeField to totals.durationScaled,
            FitActivityNumSessionsField to 1L,
            FitActivityTypeField to FitActivityTypeManual,
            FitActivityEventField to FitEventActivity,
            FitActivityEventTypeField to FitEventTypeStop,
            FitActivityLocalTimestampField to zoneOffset?.let { totals.endTimestamp + it.totalSeconds },
        ),
        strings = if (title != null) mapOf(FitActivityNameField to title) else emptyMap(),
    )
}

/** mm/s in a uint16. */
private fun Double.toFitSpeed(): Long = (this * FitSpeedScale).roundToLong().coerceIn(0, 0xFFFE)

/** FIT running cadence counts one foot: 170 steps a minute is 85. A ride's rpm is as is. */
private fun Double.toFitCadence(kind: ActivityCadenceKind): Double = when (kind) {
    ActivityCadenceKind.CYCLING -> this
    ActivityCadenceKind.STEPS -> this / 2.0
}

private fun Double.toFitCadenceByte(): Long = roundToLong().coerceIn(0, MaxFitCadence)

/** Metres as FIT's uint16: scale 5, offset 500. */
private fun Double.toFitAltitude(): Long =
    ((this + FitAltitudeOffsetMeters) * FitAltitudeScale).roundToLong().coerceIn(0, 0xFFFE)

/**
 * Health Connect exercise type to FIT `(sport, sub_sport)`. The sub-sport is
 * set only where it is the activity. Unmapped is (generic, generic).
 */
private fun Int.toFitSport(): Pair<Long, Long> = when (this) {
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> 1L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> 1L to 1L
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> 2L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> 2L to 6L
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> 11L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> 17L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> 5L to 17L
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> 5L to 18L
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING -> 15L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> 15L to 14L
    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
    ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
    ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
    -> 10L to 20L
    ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> 4L to 15L
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING,
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE,
    -> 4L to 16L
    ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> 62L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> 6L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> 7L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> 8L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN -> 9L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_SKIING -> 13L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING -> 14L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_PADDLING -> 19L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_GOLF -> 25L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING -> 31L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_SAILING -> 32L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING,
    ExerciseSessionRecord.EXERCISE_TYPE_SKATING,
    -> 33L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING -> 35L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_SURFING -> 38L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_BOXING -> 47L to 0L
    else -> 0L to 0L
}

// The FIT vocabulary this exporter writes, from Garmin's FIT profile. The
// read side keeps its own table in FitRouteParser.
private const val FitLocalFileId = 0
private const val FitLocalRecord = 1
private const val FitLocalLap = 2
private const val FitLocalSession = 3
private const val FitLocalActivity = 4
private const val FitLocalEvent = 5
private const val FitFileIdMessage = 0
private const val FitSessionMessage = 18
private const val FitLapMessage = 19
private const val FitRecordMessage = 20
private const val FitEventMessage = 21
private const val FitActivityMessage = 34
private const val FitTimestampField = 253
private const val FitMessageIndexField = 254
private const val FitFileIdTypeField = 0
private const val FitFileIdManufacturerField = 1
private const val FitFileIdProductField = 2
private const val FitFileIdTimeCreatedField = 4
private const val FitRecordPositionLatField = 0
private const val FitRecordPositionLongField = 1
private const val FitRecordAltitudeField = 2
private const val FitRecordHeartRateField = 3
private const val FitRecordCadenceField = 4
private const val FitRecordDistanceField = 5
private const val FitRecordSpeedField = 6
private const val FitRecordFractionalCadenceField = 53
private const val FitEventField = 0
private const val FitEventTypeField = 1
private const val FitStartTimeField = 2
private const val FitSessionSportField = 5
private const val FitSessionSubSportField = 6
private const val FitTotalElapsedTimeField = 7
private const val FitTotalTimerTimeField = 8
private const val FitTotalDistanceField = 9
private const val FitTotalCaloriesField = 11
private const val FitLapAvgSpeedField = 13
private const val FitLapMaxSpeedField = 14
private const val FitLapAvgHeartRateField = 15
private const val FitLapMaxHeartRateField = 16
private const val FitLapAvgCadenceField = 17
private const val FitLapMaxCadenceField = 18
/** Lap numbering; also what our importer reads on both messages. */
private const val FitLapTotalAscentField = 21
private const val FitSessionAvgSpeedField = 14
private const val FitSessionMaxSpeedField = 15
private const val FitSessionAvgHeartRateField = 16
private const val FitSessionMaxHeartRateField = 17
private const val FitSessionAvgCadenceField = 18
private const val FitSessionMaxCadenceField = 19
/** Session numbering. 21 there is max_power and must never be written. */
private const val FitSessionTotalAscentField = 22
private const val FitSessionFirstLapIndexField = 25
private const val FitSessionNumLapsField = 26
private const val FitActivityTotalTimerTimeField = 0
private const val FitActivityNumSessionsField = 1
private const val FitActivityTypeField = 2
private const val FitActivityEventField = 3
private const val FitActivityEventTypeField = 4
private const val FitActivityLocalTimestampField = 5
private const val FitActivityNameField = 8
private const val FitFileTypeActivity = 4L
private const val FitManufacturerDevelopment = 255L
private const val FitEventTimer = 0L
private const val FitEventLap = 9L
private const val FitEventSession = 8L
private const val FitEventActivity = 26L
private const val FitEventTypeStart = 0L
private const val FitEventTypeStop = 1L
private const val FitEventTypeStopAll = 4L
private const val FitActivityTypeManual = 0L
private const val FitDistanceScale = 100.0
private const val FitSpeedScale = 1000.0
private const val FitAltitudeScale = 5.0
private const val FitAltitudeOffsetMeters = 500.0
private const val FitFractionalCadenceScale = 128.0
private const val MaxFitCadence = 254L
private const val MaxFitActivityNameBytes = 64
