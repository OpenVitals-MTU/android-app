package tech.mmarca.openvitals.features.activity

import androidx.health.connect.client.records.ExerciseSessionRecord
import tech.mmarca.openvitals.domain.model.cyclingExercises
import tech.mmarca.openvitals.domain.model.distanceBasedExercises
import tech.mmarca.openvitals.domain.model.indoorExercises
import tech.mmarca.openvitals.domain.model.prefersPaceExercises
import tech.mmarca.openvitals.domain.model.stepBasedExercises

/**
 * Which metric rows are worth showing on the activity detail screen.
 *
 * The card used to render a fixed row list for every activity and fall back to
 * "Not available" whenever a field was null, so a bike ride advertised Steps,
 * Floors climbed and Wheelchair pushes — all of them absent, none of them ever
 * going to be present. This picks the rows instead, on ONE rule:
 *
 *   Show a row if it has a value, OR if the absence is worth reporting for this
 *   kind of activity.
 *
 * The two halves matter equally. "Has a value" comes first, so recorded data is
 * NEVER hidden — if a device somehow reports steps for a bike ride, the row
 * appears, and no relevance table can suppress it. Relevance only decides which
 * ABSENCES are informative: "Distance: Not available" tells a cyclist something
 * real (the GPS did not record), while "Wheelchair pushes: Not available" tells
 * them nothing at all.
 *
 * An exercise type this table has never seen — a new Health Connect constant, an
 * import from another app — needs no special case. It matches none of the sets,
 * so it reports only the absences that are universal (duration, heart rate,
 * energy) and still shows every metric it actually recorded. The card can never
 * come out empty, because a session always has a duration.
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

/**
 * The screen's whole rule, in one place: a row appears when it HAS a value, or
 * when its absence is worth reporting for this kind of activity.
 *
 * The order matters. Recorded data is never hidden — if a device somehow reports
 * steps for a bike ride, the row appears and no relevance table can suppress it.
 */
internal fun showsMetricRow(
    hasValue: Boolean,
    metric: ActivityDetailMetric,
    exerciseType: Int,
): Boolean = hasValue || isMetricRelevant(metric, exerciseType)

/**
 * Whether the ABSENCE of [metric] is worth reporting for [exerciseType].
 *
 * Callers must still show the row whenever it has a value — see the rule in
 * the doc above. This answers only the second half of it.
 */
internal fun isMetricRelevant(metric: ActivityDetailMetric, exerciseType: Int): Boolean =
    when (metric) {
        // Every session has a duration, a heart rate worth looking for, and
        // burns energy — there is no activity for which these are meaningless.
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

        // Everything below needs hardware most people do not own — a power
        // meter, a footpod, a bike computer reporting its own average. Their
        // absence is the normal case and says nothing about the activity, so
        // they earn a row only by actually being recorded. Announcing
        // "Average power: Not available" on every ride would just be the old
        // noise wearing a better label.
        ActivityDetailMetric.AVERAGE_POWER,
        ActivityDetailMetric.STEPS_CADENCE,
        ActivityDetailMetric.RECORDED_SPEED,
        -> false
    }
