package tech.mmarca.openvitals.domain.model

import androidx.health.connect.client.records.ExerciseSessionRecord

/**
 * What a kind of exercise can be measured in. Health Connect types are
 * flat integers, so this knowledge lives in the domain, where both the
 * presentation and the splits engine ask the same question.
 */

/** Activities measured in strides: steps and step cadence mean something. */
internal val stepBasedExercises: Set<Int> = setOf(
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING,
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING,
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE,
)

/** Activities with a crank: pedalling cadence means something. */
internal val cyclingExercises: Set<Int> = setOf(
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
)

/** Activities that cover ground, or simulate it, so distance and speed exist. */
internal val distanceBasedExercises: Set<Int> = stepBasedExercises + cyclingExercises + setOf(
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE,
    ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL,
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
    ExerciseSessionRecord.EXERCISE_TYPE_SKATING,
    ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING,
    ExerciseSessionRecord.EXERCISE_TYPE_SKIING,
    ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING,
    ExerciseSessionRecord.EXERCISE_TYPE_PADDLING,
    ExerciseSessionRecord.EXERCISE_TYPE_SURFING,
    ExerciseSessionRecord.EXERCISE_TYPE_SAILING,
    ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR,
)

/** Pace reads better than speed for these; speed reads better for the rest. */
internal val prefersPaceExercises: Set<Int> = setOf(
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING,
)

/** Indoor and machine-bound activities: a missing elevation gain is not news. */
internal val indoorExercises: Set<Int> = setOf(
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE,
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE,
    ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL,
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
)

/**
 * Whether this kind of exercise travels. A strength session does not: GPS
 * drift on a bench once cut a lifting session into "1.0 km" splits.
 */
internal fun isDistanceBasedExercise(exerciseType: Int): Boolean =
    exerciseType in distanceBasedExercises
