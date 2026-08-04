package tech.mmarca.openvitals.domain.model

import androidx.health.connect.client.records.ExerciseSessionRecord

/**
 * What a given kind of exercise can meaningfully be measured in.
 *
 * Health Connect exercise types are a flat list of integers, and nothing in the
 * data says a bench press does not cover ground. That has to be knowledge the
 * app carries — and it has to live in the DOMAIN, because both the presentation
 * layer (which metric rows are worth showing) and the domain itself (whether an
 * activity can be cut into distance splits) need to ask the same question and
 * must not answer it differently.
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

/**
 * Activities that cover ground, or simulate covering it, so a distance and a
 * speed exist even on a machine.
 */
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

/**
 * Indoor and machine-bound activities: the ground never rises, so a missing
 * elevation gain is not news.
 */
internal val indoorExercises: Set<Int> = setOf(
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE,
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE,
    ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL,
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
)

/**
 * Whether this kind of exercise travels — the question that decides whether a
 * distance means anything for it.
 *
 * A strength session does NOT, and that matters beyond tidiness: a phone left on
 * a bench picks up a couple of hundred metres of GPS drift, Health Connect
 * faithfully records it, and the activity screen then cut a lifting session into
 * "1.0 km" and "181 m" splits at a 30:29 min/km pace. The distance was real data;
 * the splits were nonsense. Splits are only meaningful for an activity that
 * actually goes somewhere.
 */
internal fun isDistanceBasedExercise(exerciseType: Int): Boolean =
    exerciseType in distanceBasedExercises
