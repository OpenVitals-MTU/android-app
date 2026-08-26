package tech.mmarca.openvitals.features.workoutplans

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.PlannedExerciseStep
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import tech.mmarca.openvitals.domain.model.PlannedExerciseBlockData
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData
import tech.mmarca.openvitals.domain.model.PlannedExerciseWriteRequest
import tech.mmarca.openvitals.domain.model.isRestStep

internal val WorkoutPlanTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")
internal const val MaxWorkoutPlanDurationMinutes = 7 * 24 * 60L

internal fun String.toWorkoutPlanDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(trim()) }.getOrNull()

internal fun String.toWorkoutPlanTimeOrNull(): LocalTime? =
    runCatching { LocalTime.parse(trim(), WorkoutPlanTimeFormatter) }.getOrNull()

internal fun workoutPlanFormForNewPlan(now: LocalDateTime): WorkoutPlanFormInput {
    val roundedMinute = now.minute / 5 * 5
    val start = now.withMinute(roundedMinute).withSecond(0).withNano(0)
    return WorkoutPlanFormInput(
        startDateText = DateTimeFormatter.ISO_LOCAL_DATE.format(start.toLocalDate()),
        startTimeText = WorkoutPlanTimeFormatter.format(start.toLocalTime()),
        blocks = listOf(WorkoutPlanBlockInput()),
    )
}

// --- from Health Connect ---------------------------------------------------

internal fun PlannedExerciseData.toWorkoutPlanForm(zone: ZoneId): WorkoutPlanFormInput {
    val start = startTime.atZone(startZoneOffset ?: zone)
    val minutes = Duration.ofMillis(durationMs).toMinutes().coerceIn(1L, MaxWorkoutPlanDurationMinutes)
    return WorkoutPlanFormInput(
        titleText = title.orEmpty(),
        notesText = notes.orEmpty(),
        sessionExerciseType = exerciseType,
        startDateText = DateTimeFormatter.ISO_LOCAL_DATE.format(start.toLocalDate()),
        startTimeText = WorkoutPlanTimeFormatter.format(start.toLocalTime()),
        durationMinutesText = minutes.toString(),
        blocks = blocks.map { it.toBlockInput() },
    )
}

private fun PlannedExerciseBlockData.toBlockInput(): WorkoutPlanBlockInput =
    WorkoutPlanBlockInput(
        nameText = description.orEmpty(),
        roundsText = repetitions.coerceAtLeast(1).toString(),
        steps = steps.map { it.toStepInput() },
    )

internal fun PlannedExerciseStepData.toStepInput(): WorkoutPlanStepInput {
    val isRest = isRestStep()
    val completion = completion
    return when {
        isRest && completion is PlannedExerciseCompletion.DurationSeconds ->
            WorkoutPlanStepInput(
                kind = WorkoutPlanStepKind.REST,
                segmentType = exerciseType,
                exercisePhase = exercisePhase,
                descriptionText = description.orEmpty(),
                goalType = WorkoutPlanGoalType.DURATION,
                goalValueText = completion.seconds.toString(),
                performanceTargets = performanceTargets,
            )
        !isRest && completion is PlannedExerciseCompletion.Repetitions ->
            WorkoutPlanStepInput(
                kind = WorkoutPlanStepKind.ACTIVE,
                segmentType = exerciseType,
                exercisePhase = exercisePhase,
                descriptionText = description.orEmpty(),
                goalType = WorkoutPlanGoalType.REPETITIONS,
                goalValueText = completion.repetitions.toString(),
                performanceTargets = performanceTargets,
            )
        !isRest && completion is PlannedExerciseCompletion.DurationSeconds ->
            WorkoutPlanStepInput(
                kind = WorkoutPlanStepKind.ACTIVE,
                segmentType = exerciseType,
                exercisePhase = exercisePhase,
                descriptionText = description.orEmpty(),
                goalType = WorkoutPlanGoalType.DURATION,
                goalValueText = completion.seconds.toString(),
                performanceTargets = performanceTargets,
            )
        else ->
            WorkoutPlanStepInput(
                kind = WorkoutPlanStepKind.UNSUPPORTED,
                segmentType = exerciseType,
                exercisePhase = exercisePhase,
                descriptionText = description.orEmpty(),
                raw = this,
            )
    }
}

// --- to Health Connect -----------------------------------------------------

internal fun validateWorkoutPlan(form: WorkoutPlanFormInput): Set<WorkoutPlanValidationError> = buildSet {
    if (form.titleText.isBlank()) add(WorkoutPlanValidationError(WorkoutPlanValidationErrorKind.TITLE_REQUIRED))
    if (form.startDateText.toWorkoutPlanDateOrNull() == null) {
        add(WorkoutPlanValidationError(WorkoutPlanValidationErrorKind.START_DATE_INVALID))
    }
    if (form.startTimeText.toWorkoutPlanTimeOrNull() == null) {
        add(WorkoutPlanValidationError(WorkoutPlanValidationErrorKind.START_TIME_INVALID))
    }
    val minutes = form.durationMinutesText.trim().toLongOrNull()
    if (minutes == null || minutes < 1L || minutes > MaxWorkoutPlanDurationMinutes) {
        add(WorkoutPlanValidationError(WorkoutPlanValidationErrorKind.DURATION_INVALID))
    }
    if (form.blocks.isEmpty()) add(WorkoutPlanValidationError(WorkoutPlanValidationErrorKind.NO_BLOCKS))
    form.blocks.forEach { block ->
        val rounds = block.roundsText.trim().toIntOrNull()
        if (rounds == null || rounds < 1) {
            add(WorkoutPlanValidationError(WorkoutPlanValidationErrorKind.BLOCK_ROUNDS_INVALID, blockId = block.id))
        }
        if (block.steps.isEmpty()) {
            add(WorkoutPlanValidationError(WorkoutPlanValidationErrorKind.BLOCK_EMPTY, blockId = block.id))
        }
        block.steps.forEach { step ->
            if (step.kind != WorkoutPlanStepKind.UNSUPPORTED && step.goalValueOrNull() == null) {
                add(
                    WorkoutPlanValidationError(
                        WorkoutPlanValidationErrorKind.STEP_GOAL_INVALID,
                        blockId = block.id,
                        stepId = step.id,
                    ),
                )
            }
        }
    }
    val hasActiveStep = form.blocks.any { block ->
        block.steps.any { it.kind == WorkoutPlanStepKind.ACTIVE || it.kind == WorkoutPlanStepKind.UNSUPPORTED }
    }
    if (form.blocks.isNotEmpty() && form.blocks.all { it.steps.isNotEmpty() } && !hasActiveStep) {
        add(WorkoutPlanValidationError(WorkoutPlanValidationErrorKind.NO_ACTIVE_STEP))
    }
}

internal fun WorkoutPlanStepInput.goalValueOrNull(): Long? =
    goalValueText.trim().toLongOrNull()?.takeIf { it >= 1L }

/** Null when [validateWorkoutPlan] reports anything; the caller shows those instead. */
internal fun WorkoutPlanFormInput.toWriteRequest(zone: ZoneId, existingId: String?): PlannedExerciseWriteRequest? {
    if (validateWorkoutPlan(this).isNotEmpty()) return null
    val date = startDateText.toWorkoutPlanDateOrNull() ?: return null
    val time = startTimeText.toWorkoutPlanTimeOrNull() ?: return null
    val minutes = durationMinutesText.trim().toLongOrNull() ?: return null
    val start = date.atTime(time).atZone(zone).toInstant()
    return PlannedExerciseWriteRequest(
        id = existingId,
        exerciseType = sessionExerciseType,
        startTime = start,
        endTime = start.plus(Duration.ofMinutes(minutes)),
        title = titleText.trim(),
        notes = notesText.trim().takeIf { it.isNotEmpty() },
        blocks = blocks.map { it.toBlockData() },
    )
}

private fun WorkoutPlanBlockInput.toBlockData(): PlannedExerciseBlockData =
    PlannedExerciseBlockData(
        repetitions = roundsText.trim().toIntOrNull()?.coerceAtLeast(1) ?: 1,
        description = nameText.trim().takeIf { it.isNotEmpty() },
        steps = steps.map { it.toStepData() },
    )

internal fun WorkoutPlanStepInput.toStepData(): PlannedExerciseStepData {
    raw?.let { if (kind == WorkoutPlanStepKind.UNSUPPORTED) return it }
    val value = goalValueOrNull() ?: 1L
    return when (kind) {
        WorkoutPlanStepKind.REST -> PlannedExerciseStepData(
            exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST,
            exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_REST,
            description = null,
            completion = PlannedExerciseCompletion.DurationSeconds(value),
        )
        WorkoutPlanStepKind.ACTIVE,
        WorkoutPlanStepKind.UNSUPPORTED,
        -> PlannedExerciseStepData(
            exerciseType = segmentType,
            exercisePhase = exercisePhase,
            description = descriptionText.trim().takeIf { it.isNotEmpty() },
            completion = when (goalType) {
                WorkoutPlanGoalType.REPETITIONS -> PlannedExerciseCompletion.Repetitions(value.toInt())
                WorkoutPlanGoalType.DURATION -> PlannedExerciseCompletion.DurationSeconds(value)
            },
            performanceTargets = performanceTargets,
        )
    }
}

/** The same routine on another day: same clock time, same length, same blocks, a fresh record. */
internal fun PlannedExerciseData.toCopyForDate(
    date: LocalDate,
    zone: ZoneId,
    startTimeOfDay: LocalTime? = null,
): PlannedExerciseWriteRequest {
    val time = startTimeOfDay ?: startTime.atZone(zone).toLocalTime()
    val start = date.atTime(time).atZone(zone).toInstant()
    return PlannedExerciseWriteRequest(
        id = null,
        exerciseType = exerciseType,
        startTime = start,
        endTime = start.plus(Duration.ofMillis(durationMs.coerceAtLeast(60_000L))),
        title = title,
        notes = notes,
        blocks = blocks,
    )
}

internal val PlannedExerciseData.stepCount: Int
    get() = blocks.sumOf { it.steps.size }
