package tech.mmarca.openvitals.features.activity

import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Instant
import java.util.TreeMap
import kotlin.math.roundToLong
import tech.mmarca.openvitals.core.geo.haversineMeters
import tech.mmarca.openvitals.domain.model.ActivityCadenceKind
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.SpeedSample

/** The per-sample series the detail screen read for a workout, as the exports take them. */
internal data class ActivityWorkoutSeries(
    val heartRate: List<HeartRateSample> = emptyList(),
    val speed: List<SpeedSample> = emptyList(),
    val cadence: List<ActivityCadenceSample> = emptyList(),
)

/**
 * One second of an export: what each series recorded in it. Cadence is in
 * the workout's own unit, steps per minute or pedal rpm, see
 * [exportCadenceKind]. Position and distance only on an export with the route.
 */
internal data class WorkoutExportPoint(
    val time: Instant,
    val heartRateBpm: Long? = null,
    val speedMetersPerSecond: Double? = null,
    val cadence: Double? = null,
    val position: ExerciseRoutePoint? = null,
    val distanceMeters: Double? = null,
)

/**
 * Every series on one clock of whole seconds, the resolution of a FIT
 * record. Two samples of one series in a second: the later wins.
 * [routePoints] add position and the distance covered along them; without
 * them the timeline has no location in it.
 */
internal fun ExerciseData.workoutExportTimeline(
    series: ActivityWorkoutSeries,
    routePoints: List<ExerciseRoutePoint> = emptyList(),
): List<WorkoutExportPoint> {
    val points = TreeMap<Long, WorkoutExportPoint>()
    fun update(time: Instant, change: (WorkoutExportPoint) -> WorkoutExportPoint) {
        val second = time.epochSecond
        points[second] = change(points[second] ?: WorkoutExportPoint(Instant.ofEpochSecond(second)))
    }
    heartRateSamplesForExport(series.heartRate).forEach { sample ->
        update(sample.time) { it.copy(heartRateBpm = sample.beatsPerMinute) }
    }
    series.speed
        .filter { isInSession(it.time) && it.metersPerSecond.isFinite() && it.metersPerSecond >= 0.0 }
        .sortedBy { it.time }
        .forEach { sample -> update(sample.time) { it.copy(speedMetersPerSecond = sample.metersPerSecond) } }
    val cadenceKind = exportCadenceKind()
    series.cadence
        .filter { it.kind == cadenceKind && isInSession(it.time) && it.rate.isFinite() && it.rate >= 0.0 }
        .sortedBy { it.time }
        .forEach { sample -> update(sample.time) { it.copy(cadence = sample.rate) } }
    var distanceMeters = 0.0
    routePoints.forEachIndexed { index, point ->
        routePoints.getOrNull(index - 1)?.let { previous ->
            distanceMeters += haversineMeters(previous.latitude, previous.longitude, point.latitude, point.longitude)
        }
        val covered = distanceMeters
        update(point.time) { it.copy(position = point, distanceMeters = covered) }
    }
    return points.values.toList()
}

/**
 * The one cadence FIT and TCX can name for the sport: pedal rpm on a ride,
 * steps otherwise. Both importers decide it by sport the same way.
 */
internal fun ExerciseData.exportCadenceKind(): ActivityCadenceKind = when (exerciseType) {
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
    -> ActivityCadenceKind.CYCLING
    else -> ActivityCadenceKind.STEPS
}

/** The samples worth exporting: inside the session, ordered, inside `unsignedByte`. 0 bpm is a dropout. */
internal fun ExerciseData.heartRateSamplesForExport(
    heartRateSamples: List<HeartRateSample>,
): List<HeartRateSample> =
    heartRateSamples
        .filter { it.beatsPerMinute in 1..255 }
        .filter { isInSession(it.time) }
        .sortedBy { it.time }

/** The recorded average wins; a computed one only stands in when there is none. */
internal fun ExerciseData.averageHeartRateForExport(
    heartRateSamples: List<HeartRateSample>,
): Long? =
    averageHeartRateBpm
        ?: heartRateSamples
            .takeIf { it.isNotEmpty() }
            ?.map { it.beatsPerMinute }
            ?.average()
            ?.roundToLong()

/** The recorded average wins over the sampled one, as for heart rate. */
internal fun ExerciseData.averageSpeedForExport(timeline: List<WorkoutExportPoint>): Double? =
    averageSpeedMetersPerSecond
        ?: timeline.mapNotNull { it.speedMetersPerSecond }.takeIf { it.isNotEmpty() }?.average()

/** In the unit of [exportCadenceKind]. The recorded average wins over the sampled one. */
internal fun ExerciseData.averageCadenceForExport(timeline: List<WorkoutExportPoint>): Double? {
    val recorded = when (exportCadenceKind()) {
        ActivityCadenceKind.CYCLING -> averageCyclingCadenceRpm
        ActivityCadenceKind.STEPS -> averageStepsCadenceRate
    }
    return recorded ?: timeline.mapNotNull { it.cadence }.takeIf { it.isNotEmpty() }?.average()
}

private fun ExerciseData.isInSession(time: Instant): Boolean =
    !time.isBefore(startTime) && !time.isAfter(endTime)
