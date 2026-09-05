package tech.mmarca.openvitals.features.activity

import androidx.health.connect.client.records.ExerciseSessionRecord
import tech.mmarca.openvitals.domain.model.cyclingExercises
import tech.mmarca.openvitals.domain.model.distanceBasedExercises
import tech.mmarca.openvitals.domain.model.indoorExercises
import tech.mmarca.openvitals.domain.model.prefersPaceExercises
import tech.mmarca.openvitals.domain.model.stepBasedExercises

/**
 * Which metric rows the activity detail screen shows. One rule: a row
 * appears if it has a value, or if its absence is worth reporting for this
 * kind of activity. Recorded data is never hidden. An unknown exercise type
 * reports only the universal absences.
 */
internal enum class ActivityDetailMetric {
    DURATION,
    MOVING_TIME,
    STEPS,
    STEPS_CADENCE,
    DISTANCE,
    AVERAGE_PACE,
    AVERAGE_SPEED,
    RECORDED_SPEED,
    CYCLING_CADENCE,
    AVERAGE_POWER,
    AVERAGE_HEART_RATE,
    CALORIES_BURNED,
    ACTIVE_CALORIES,
    WHEELCHAIR_PUSHES,
    FLOORS_CLIMBED,
    ELEVATION_GAINED,
}

/** The rule: a row appears when it has a value, or when its absence matters here. */
internal fun showsMetricRow(
    hasValue: Boolean,
    metric: ActivityDetailMetric,
    exerciseType: Int,
): Boolean = hasValue || isMetricRelevant(metric, exerciseType)

/** Whether the absence of [metric] is worth reporting for [exerciseType]. */
internal fun isMetricRelevant(metric: ActivityDetailMetric, exerciseType: Int): Boolean =
    when (metric) {
        // Every session has a duration, a heart rate and an energy cost.
        ActivityDetailMetric.DURATION,
        ActivityDetailMetric.MOVING_TIME,
        ActivityDetailMetric.AVERAGE_HEART_RATE,
        ActivityDetailMetric.CALORIES_BURNED,
        ActivityDetailMetric.ACTIVE_CALORIES,
        -> true

        ActivityDetailMetric.STEPS -> exerciseType in stepBasedExercises
        ActivityDetailMetric.CYCLING_CADENCE -> exerciseType in cyclingExercises
        ActivityDetailMetric.DISTANCE -> exerciseType in distanceBasedExercises
        ActivityDetailMetric.AVERAGE_PACE -> exerciseType in prefersPaceExercises
        ActivityDetailMetric.AVERAGE_SPEED -> exerciseType in distanceBasedExercises
        ActivityDetailMetric.WHEELCHAIR_PUSHES ->
            exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
        ActivityDetailMetric.FLOORS_CLIMBED ->
            exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING ||
                exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE
        ActivityDetailMetric.ELEVATION_GAINED ->
            exerciseType in distanceBasedExercises && exerciseType !in indoorExercises

        // These need hardware most people do not own; absence says nothing.
        ActivityDetailMetric.AVERAGE_POWER,
        ActivityDetailMetric.STEPS_CADENCE,
        ActivityDetailMetric.RECORDED_SPEED,
        -> false
    }
