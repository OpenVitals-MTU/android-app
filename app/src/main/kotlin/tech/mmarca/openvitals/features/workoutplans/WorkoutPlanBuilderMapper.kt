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
import tech.mmarca.openvitals.domain.model.PlannedExercisePerformanceTarget
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData
import tech.mmarca.openvitals.domain.model.PlannedExerciseWriteRequest
import tech.mmarca.openvitals.domain.model.isRestStep
import tech.mmarca.openvitals.domain.model.plannedWeightKg
import tech.mmarca.openvitals.domain.model.restPlanStep
import tech.mmarca.openvitals.features.manualentry.activity.MaxActivityRepetitionSets
import tech.mmarca.openvitals.features.manualentry.activity.MaxActivityRestSeconds
import tech.mmarca.openvitals.features.manualentry.activity.toInputText
import tech.mmarca.openvitals.features.manualentry.activity.toWeightKgOrNull

internal val WorkoutPlanTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")
internal const val MaxWorkoutPlanDurationMinutes = 7 * 24 * 60L

/** Whole minutes read as minutes; anything else as seconds. */
internal fun secondsToDurationInput(seconds: Long): Pair<String, WorkoutPlanDurationUnit> =
    if (seconds >= 60L && seconds % 60L == 0L) {
        (seconds / 60L).toString() to WorkoutPlanDurationUnit.MINUTES
    } else {
        seconds.toString() to WorkoutPlanDurationUnit.SECONDS
    }

private fun Long.toSeconds(unit: WorkoutPlanDurationUnit): Long =
    if (unit == WorkoutPlanDurationUnit.MINUTES) this * 60L else this

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

// From Health Connect.

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

/**
 * Rows from stored steps. A run of one active step, each copy followed by
 * the same rest (or by nothing), reads as one row with that many sets.
 * The scan is greedy from the left, so writing the rows back gives the
 * same steps.
 */
private fun PlannedExerciseBlockData.toBlockInput(): WorkoutPlanBlockInput {
    val rows = mutableListOf<WorkoutPlanStepInput>()
    var index = 0
    while (index < steps.size) {
        val row = steps[index].toStepInput()
        if (row.kind != WorkoutPlanStepKind.ACTIVE) {
            rows += row
            index += 1
            continue
        }
        val rest = steps.getOrNull(index + 1)?.takeIf { it.toStepInput().kind == WorkoutPlanStepKind.REST }
        val pattern = listOfNotNull(steps[index], rest)
        var sets = 0
        while (index + pattern.size <= steps.size && steps.subList(index, index + pattern.size) == pattern) {
            sets += 1
            index += pattern.size
        }
        val restSeconds = (rest?.completion as? PlannedExerciseCompletion.DurationSeconds)?.seconds
        val (setRestText, setRestUnit) = restSeconds?.let(::secondsToDurationInput) ?: ("" to WorkoutPlanDurationUnit.SECONDS)
        rows += row.copy(setsText = sets.toString(), setRestText = setRestText, setRestUnit = setRestUnit)
    }
    return WorkoutPlanBlockInput(
        nameText = description.orEmpty(),
        roundsText = repetitions.coerceAtLeast(1).toString(),
        steps = rows,
    )
}

internal fun PlannedExerciseStepData.toStepInput(): WorkoutPlanStepInput {
    val isRest = isRestStep()
    val completion = completion
    val weightKg = plannedWeightKg()
    // The weight target is edited in its own field; the rest ride along untouched.
    val otherTargets = if (weightKg == null) performanceTargets else performanceTargets - PlannedExercisePerformanceTarget.Weight(weightKg)
    return when {
        isRest && completion is PlannedExerciseCompletion.DurationSeconds -> {
            val (text, unit) = secondsToDurationInput(completion.seconds)
            WorkoutPlanStepInput(
                kind = WorkoutPlanStepKind.REST,
                segmentType = exerciseType,
                exercisePhase = exercisePhase,
                descriptionText = description.orEmpty(),
                goalType = WorkoutPlanGoalType.DURATION,
                goalValueText = text,
                durationUnit = unit,
                performanceTargets = performanceTargets,
            )
        }
        !isRest && completion is PlannedExerciseCompletion.Repetitions ->
            WorkoutPlanStepInput(
                kind = WorkoutPlanStepKind.ACTIVE,
                segmentType = exerciseType,
                exercisePhase = exercisePhase,
                descriptionText = description.orEmpty(),
                goalType = WorkoutPlanGoalType.REPETITIONS,
                goalValueText = completion.repetitions.toString(),
                weightKgText = weightKg?.toInputText(1).orEmpty(),
                performanceTargets = otherTargets,
            )
        !isRest && completion is PlannedExerciseCompletion.DurationSeconds -> {
            val (text, unit) = secondsToDurationInput(completion.seconds)
            WorkoutPlanStepInput(
                kind = WorkoutPlanStepKind.ACTIVE,
                segmentType = exerciseType,
                exercisePhase = exercisePhase,
                descriptionText = description.orEmpty(),
                goalType = WorkoutPlanGoalType.DURATION,
                goalValueText = text,
                durationUnit = unit,
                weightKgText = weightKg?.toInputText(1).orEmpty(),
                performanceTargets = otherTargets,
            )
        }
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

// To Health Connect.

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
            fun stepError(kind: WorkoutPlanValidationErrorKind) {
                add(WorkoutPlanValidationError(kind, blockId = block.id, stepId = step.id))
            }
            if (step.kind != WorkoutPlanStepKind.UNSUPPORTED && step.goalValueOrNull() == null) {
                stepError(WorkoutPlanValidationErrorKind.STEP_GOAL_INVALID)
            }
            if (step.kind == WorkoutPlanStepKind.ACTIVE) {
                if (step.setsOrNull() == null) stepError(WorkoutPlanValidationErrorKind.STEP_SETS_INVALID)
                if (step.setRestSecondsOrNull() == null) stepError(WorkoutPlanValidationErrorKind.STEP_SET_REST_INVALID)
                if (step.weightKgText.isNotBlank() && step.weightKgOrNull() == null) {
                    stepError(WorkoutPlanValidationErrorKind.STEP_WEIGHT_INVALID)
                }
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

/** The goal in reps or seconds; durations honour the unit and stay within a day. */
internal fun WorkoutPlanStepInput.goalValueOrNull(): Long? {
    val value = goalValueText.trim().toLongOrNull()?.takeIf { it >= 1L } ?: return null
    if (goalType != WorkoutPlanGoalType.DURATION) return value
    if (value > MaxActivityRestSeconds) return null
    return value.toSeconds(durationUnit).takeIf { it <= MaxActivityRestSeconds }
}

internal fun WorkoutPlanStepInput.setsOrNull(): Int? =
    setsText.trim().toIntOrNull()?.takeIf { it in 1..MaxActivityRepetitionSets }

/** Rest after each set in seconds; blank means none, anything unparsable is null. */
internal fun WorkoutPlanStepInput.setRestSecondsOrNull(): Long? {
    val text = setRestText.trim()
    if (text.isEmpty()) return 0L
    val value = text.toLongOrNull()?.takeIf { it in 0..MaxActivityRestSeconds } ?: return null
    return value.toSeconds(setRestUnit).takeIf { it <= MaxActivityRestSeconds }
}

internal fun WorkoutPlanStepInput.weightKgOrNull(): Double? = weightKgText.toWeightKgOrNull()

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
        steps = steps.flatMap { it.toStepDataList() },
    )

/** A row as stored steps: an active row repeats once per set, each copy followed by its rest. */
internal fun WorkoutPlanStepInput.toStepDataList(): List<PlannedExerciseStepData> {
    raw?.let { if (kind == WorkoutPlanStepKind.UNSUPPORTED) return listOf(it) }
    val value = goalValueOrNull() ?: 1L
    return when (kind) {
        WorkoutPlanStepKind.REST -> listOf(
            PlannedExerciseStepData(
                exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST,
                exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_REST,
                description = null,
                completion = PlannedExerciseCompletion.DurationSeconds(value),
            ),
        )
        WorkoutPlanStepKind.ACTIVE,
        WorkoutPlanStepKind.UNSUPPORTED,
        -> {
            val step = PlannedExerciseStepData(
                exerciseType = segmentType,
                exercisePhase = exercisePhase,
                description = descriptionText.trim().takeIf { it.isNotEmpty() },
                completion = when (goalType) {
                    WorkoutPlanGoalType.REPETITIONS -> PlannedExerciseCompletion.Repetitions(value.toInt())
                    WorkoutPlanGoalType.DURATION -> PlannedExerciseCompletion.DurationSeconds(value)
                },
                performanceTargets = performanceTargets +
                    listOfNotNull(weightKgOrNull()?.let { PlannedExercisePerformanceTarget.Weight(it) }),
            )
            val restSeconds = setRestSecondsOrNull() ?: 0L
            val perSet = if (restSeconds > 0L) listOf(step, restPlanStep(restSeconds)) else listOf(step)
            List(setsOrNull() ?: 1) { perSet }.flatten()
        }
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
