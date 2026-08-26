package tech.mmarca.openvitals.features.workoutplans

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExercisePerformanceTarget
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData
import tech.mmarca.openvitals.features.activity.exerciseSegmentLabel
import tech.mmarca.openvitals.features.activity.exerciseTypeLabel

/** A step reads as its stored label when it has one, else as the segment type's name. */
@Composable
internal fun stepLabel(step: WorkoutPlanStepInput): String =
    step.descriptionText.trim().takeIf { it.isNotEmpty() } ?: exerciseSegmentLabel(step.segmentType)

@Composable
internal fun plannedStepLabel(step: PlannedExerciseStepData): String =
    step.description?.trim()?.takeIf { it.isNotEmpty() } ?: exerciseSegmentLabel(step.exerciseType)

@Composable
internal fun sessionTypeLabel(exerciseType: Int): String = exerciseTypeLabel(exerciseType)

@Composable
internal fun goalText(goalType: WorkoutPlanGoalType, value: Long): String = when (goalType) {
    WorkoutPlanGoalType.REPETITIONS -> stringResource(R.string.activity_entry_plan_preview_reps, value.toInt())
    WorkoutPlanGoalType.DURATION -> stringResource(R.string.workout_plan_preview_seconds, value)
}

@Composable
internal fun completionText(completion: PlannedExerciseCompletion): String? = when (completion) {
    is PlannedExerciseCompletion.Repetitions -> goalText(WorkoutPlanGoalType.REPETITIONS, completion.repetitions.toLong())
    is PlannedExerciseCompletion.DurationSeconds -> goalText(WorkoutPlanGoalType.DURATION, completion.seconds)
    is PlannedExerciseCompletion.DistanceMeters -> stringResource(R.string.workout_plan_goal_distance, completion.meters / 1000.0)
    is PlannedExerciseCompletion.DistanceAndDuration ->
        stringResource(R.string.workout_plan_goal_distance, completion.meters / 1000.0) + " · " +
            goalText(WorkoutPlanGoalType.DURATION, completion.seconds)
    is PlannedExerciseCompletion.Steps -> stringResource(R.string.workout_plan_goal_steps, completion.steps)
    is PlannedExerciseCompletion.ActiveCaloriesKcal,
    is PlannedExerciseCompletion.TotalCaloriesKcal,
    -> stringResource(
        R.string.workout_plan_goal_kcal,
        when (completion) {
            is PlannedExerciseCompletion.ActiveCaloriesKcal -> completion.kcal
            is PlannedExerciseCompletion.TotalCaloriesKcal -> completion.kcal
            else -> 0.0
        },
    )
    PlannedExerciseCompletion.Manual,
    PlannedExerciseCompletion.Unknown,
    -> null
}

/** "HR 120–150 bpm", "Power 200–250 W", …; null for a target the app cannot describe. */
@Composable
internal fun performanceTargetText(target: PlannedExercisePerformanceTarget): String? = when (target) {
    is PlannedExercisePerformanceTarget.HeartRate ->
        stringResource(R.string.workout_plan_target_heart_rate, target.minBpm.toInt(), target.maxBpm.toInt())
    is PlannedExercisePerformanceTarget.Power ->
        stringResource(R.string.workout_plan_target_power, target.minWatts.toInt(), target.maxWatts.toInt())
    is PlannedExercisePerformanceTarget.Speed ->
        stringResource(R.string.workout_plan_target_speed, target.minMetersPerSecond * 3.6, target.maxMetersPerSecond * 3.6)
    is PlannedExercisePerformanceTarget.Cadence ->
        stringResource(R.string.workout_plan_target_cadence, target.minRpm.toInt(), target.maxRpm.toInt())
    is PlannedExercisePerformanceTarget.Weight -> stringResource(R.string.workout_plan_target_weight, target.kilograms)
    is PlannedExercisePerformanceTarget.RateOfPerceivedExertion -> stringResource(R.string.workout_plan_target_rpe, target.rpe)
    PlannedExercisePerformanceTarget.Amrap -> stringResource(R.string.workout_plan_target_amrap)
    PlannedExercisePerformanceTarget.Unknown -> null
}

@Composable
internal fun WorkoutPlanValidationError.message(): String = stringResource(
    when (kind) {
        WorkoutPlanValidationErrorKind.TITLE_REQUIRED -> R.string.workout_plan_error_title_required
        WorkoutPlanValidationErrorKind.START_DATE_INVALID -> R.string.workout_plan_error_start_date
        WorkoutPlanValidationErrorKind.START_TIME_INVALID -> R.string.workout_plan_error_start_time
        WorkoutPlanValidationErrorKind.DURATION_INVALID -> R.string.workout_plan_error_duration
        WorkoutPlanValidationErrorKind.NO_BLOCKS -> R.string.workout_plan_error_no_blocks
        WorkoutPlanValidationErrorKind.BLOCK_ROUNDS_INVALID -> R.string.workout_plan_error_block_rounds
        WorkoutPlanValidationErrorKind.BLOCK_EMPTY -> R.string.workout_plan_error_block_empty
        WorkoutPlanValidationErrorKind.STEP_GOAL_INVALID -> R.string.workout_plan_error_step_goal
        WorkoutPlanValidationErrorKind.NO_ACTIVE_STEP -> R.string.workout_plan_error_no_active_step
    },
)
