package tech.mmarca.openvitals.domain.model

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.PlannedExerciseStep

/**
 * A rest step is one Health Connect flags by phase, or one whose segment type
 * is rest/pause regardless of phase — other apps are not consistent about
 * which of the two they set.
 */
fun PlannedExerciseStepData.isRestStep(): Boolean =
    exercisePhase == PlannedExerciseStep.EXERCISE_PHASE_REST ||
        exerciseType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST ||
        exerciseType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE

fun restPlanStep(seconds: Long): PlannedExerciseStepData =
    PlannedExerciseStepData(
        exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST,
        exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_REST,
        description = null,
        completion = PlannedExerciseCompletion.DurationSeconds(seconds),
    )

/** Every non-rest step across all blocks, in plan order, rounds not unrolled. */
fun PlannedExerciseData.activeSteps(): List<PlannedExerciseStepData> =
    blocks.flatMap { block -> block.steps.filterNot { it.isRestStep() } }
