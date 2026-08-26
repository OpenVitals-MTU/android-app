package tech.mmarca.openvitals.features.workoutplans

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import android.content.Context
import tech.mmarca.openvitals.features.activity.exerciseSegmentLabelRes

/**
 * One pickable exercise. [description] is only set for presets Health Connect
 * has no constant for (push-ups ride on `OTHER_WORKOUT`), and it is what gets
 * stored on the step so the plan reads back as "Push-ups" rather than "Other
 * workout" in every app that shows it.
 */
data class WorkoutPlanStepChoice(
    val segmentType: Int,
    val description: String? = null,
    val defaultGoal: WorkoutPlanGoalType = defaultGoalFor(segmentType),
) {
    val labelRes: Int get() = exerciseSegmentLabelRes(segmentType)

    fun label(context: Context): String = description ?: context.getString(labelRes)
}

private val DurationDefaultSegmentTypes: Set<Int> = setOf(
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_STRETCHING,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_YOGA,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_PILATES,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_WALKING,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_ELLIPTICAL,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_ROWING_MACHINE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_POOL,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_WHEELCHAIR,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_HULA_HOOP,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_JUMP_ROPE,
)

fun defaultGoalFor(segmentType: Int): WorkoutPlanGoalType =
    if (segmentType in DurationDefaultSegmentTypes) WorkoutPlanGoalType.DURATION else WorkoutPlanGoalType.REPETITIONS

/** Every segment type the builder can plan, minus the ones that are not exercises (rest, pause, unknown). */
private val PlannableSegmentTypes: List<Int> = listOf(
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_ARM_CURL,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BACK_EXTENSION,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BALL_SLAM,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BENCH_PRESS,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BURPEE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_CRUNCH,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DEADLIFT,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_ELLIPTICAL,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_FORWARD_TWIST,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_FRONT_RAISE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_HIP_THRUST,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_HULA_HOOP,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_JUMPING_JACK,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_JUMP_ROPE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_LATERAL_RAISE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_CURL,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_EXTENSION,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_PRESS,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_RAISE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_LUNGE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_PILATES,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_PUNCH,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_ROWING_MACHINE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SIT_UP,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SQUAT,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_STRETCHING,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_POOL,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_UPPER_TWIST,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_WALKING,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_WHEELCHAIR,
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_YOGA,
)

/** Presets first (they are the reason the picker exists), then the catalog; the picker sorts by the localised name. */
val WorkoutPlanStepCatalog: List<WorkoutPlanStepChoice> = buildList {
    add(WorkoutPlanStepChoice(ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT, description = "Push-ups"))
    addAll(PlannableSegmentTypes.map { WorkoutPlanStepChoice(it) })
}

val WorkoutPlanSessionTypes: List<Int> = listOf(
    ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
    ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
    ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
    ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
    ExerciseSessionRecord.EXERCISE_TYPE_PILATES,
    ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING,
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
)
