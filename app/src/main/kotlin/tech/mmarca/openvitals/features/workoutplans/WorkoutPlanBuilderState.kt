package tech.mmarca.openvitals.features.workoutplans

import androidx.compose.runtime.Immutable
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.PlannedExerciseStep
import java.util.UUID
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.domain.model.PlannedExercisePerformanceTarget
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData

enum class WorkoutPlanGoalType {
    REPETITIONS,
    DURATION,
}

/**
 * How the builder treats a step. [UNSUPPORTED] keeps a step the builder cannot
 * edit (a distance goal from another app, say) so a round-trip through the
 * builder never silently drops part of someone else's plan.
 */
enum class WorkoutPlanStepKind {
    ACTIVE,
    REST,
    UNSUPPORTED,
}

@Immutable
data class WorkoutPlanStepInput(
    val id: String = newWorkoutPlanInputId(),
    val kind: WorkoutPlanStepKind,
    val segmentType: Int,
    val exercisePhase: Int = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
    val descriptionText: String = "",
    val goalType: WorkoutPlanGoalType = WorkoutPlanGoalType.REPETITIONS,
    val goalValueText: String = "",
    val raw: PlannedExerciseStepData? = null,
    /** Targets another app attached (pace, heart rate, …): shown, never edited, written back as they came. */
    val performanceTargets: List<PlannedExercisePerformanceTarget> = emptyList(),
) {
    companion object {
        fun rest(seconds: Long = DefaultRestSeconds): WorkoutPlanStepInput =
            WorkoutPlanStepInput(
                kind = WorkoutPlanStepKind.REST,
                segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST,
                exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_REST,
                goalType = WorkoutPlanGoalType.DURATION,
                goalValueText = seconds.toString(),
            )

        fun active(choice: WorkoutPlanStepChoice): WorkoutPlanStepInput =
            WorkoutPlanStepInput(
                kind = WorkoutPlanStepKind.ACTIVE,
                segmentType = choice.segmentType,
                descriptionText = choice.description.orEmpty(),
                goalType = choice.defaultGoal,
                goalValueText = when (choice.defaultGoal) {
                    WorkoutPlanGoalType.REPETITIONS -> DefaultRepetitions.toString()
                    WorkoutPlanGoalType.DURATION -> DefaultActiveSeconds.toString()
                },
            )

        const val DefaultRestSeconds = 60L
        const val DefaultRepetitions = 10
        const val DefaultActiveSeconds = 30L
    }
}

@Immutable
data class WorkoutPlanBlockInput(
    val id: String = newWorkoutPlanInputId(),
    val nameText: String = "",
    val roundsText: String = "1",
    val steps: List<WorkoutPlanStepInput> = emptyList(),
)

/** The editable part of the builder; also the baseline the dirty check compares against. */
@Immutable
data class WorkoutPlanFormInput(
    val titleText: String = "",
    val notesText: String = "",
    val sessionExerciseType: Int = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
    val startDateText: String = "",
    val startTimeText: String = "",
    val durationMinutesText: String = DefaultDurationMinutes.toString(),
    val blocks: List<WorkoutPlanBlockInput> = emptyList(),
) {
    companion object {
        const val DefaultDurationMinutes = 30L
    }
}

enum class WorkoutPlanValidationErrorKind {
    TITLE_REQUIRED,
    START_DATE_INVALID,
    START_TIME_INVALID,
    DURATION_INVALID,
    NO_BLOCKS,
    BLOCK_ROUNDS_INVALID,
    BLOCK_EMPTY,
    STEP_GOAL_INVALID,
    NO_ACTIVE_STEP,
}

@Immutable
data class WorkoutPlanValidationError(
    val kind: WorkoutPlanValidationErrorKind,
    val blockId: String? = null,
    val stepId: String? = null,
)

@Immutable
data class WorkoutPlanBuilderUiState(
    val planId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAvailable: Boolean = true,
    val isOwnedByApp: Boolean = true,
    val form: WorkoutPlanFormInput = WorkoutPlanFormInput(),
    val baseline: WorkoutPlanFormInput = WorkoutPlanFormInput(),
    val validationErrors: Set<WorkoutPlanValidationError> = emptySet(),
    val error: ScreenError? = null,
    val writePermissions: Set<String> = emptySet(),
    val savedPlanId: String? = null,
) {
    val isDirty: Boolean get() = form != baseline
    val isEditing: Boolean get() = planId != null
    val saveCompleted: Boolean get() = savedPlanId != null
    val canEdit: Boolean get() = isAvailable && isOwnedByApp && !isLoading && !isSaving

    fun errorFor(kind: WorkoutPlanValidationErrorKind): WorkoutPlanValidationError? =
        validationErrors.firstOrNull { it.kind == kind && it.blockId == null && it.stepId == null }

    fun blockError(blockId: String): WorkoutPlanValidationError? =
        validationErrors.firstOrNull { it.blockId == blockId && it.stepId == null }

    fun stepError(stepId: String): WorkoutPlanValidationError? =
        validationErrors.firstOrNull { it.stepId == stepId }
}

internal fun newWorkoutPlanInputId(): String = UUID.randomUUID().toString()
