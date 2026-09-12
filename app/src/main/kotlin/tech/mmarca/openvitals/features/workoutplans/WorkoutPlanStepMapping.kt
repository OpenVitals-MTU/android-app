package tech.mmarca.openvitals.features.workoutplans

import androidx.health.connect.client.records.PlannedExerciseStep
import java.time.Instant
import tech.mmarca.openvitals.domain.model.PlannedExerciseBlockData
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData
import tech.mmarca.openvitals.domain.model.PlannedExerciseWriteRequest
import tech.mmarca.openvitals.domain.model.PlannedExercisePerformanceTarget
import tech.mmarca.openvitals.domain.model.isRestStep
import tech.mmarca.openvitals.domain.model.plannedWeightKg
import tech.mmarca.openvitals.domain.model.restPlanStep
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryType
import tech.mmarca.openvitals.features.manualentry.activity.ActivityRepetitionSetInput
import tech.mmarca.openvitals.features.manualentry.activity.isRepetitionLike
import tech.mmarca.openvitals.features.manualentry.activity.toInputText
import tech.mmarca.openvitals.features.manualentry.activity.toWeightKgOrNull
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityPlanGoalKind
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityPlanRunStep
import tech.mmarca.openvitals.features.manualentry.activity.recording.planStepSensorTypeId
import tech.mmarca.openvitals.features.manualentry.activity.toActivityEntryType

/**
 * The seam between a Health Connect plan and the form's step rows: blocks
 * unroll by round count and a rest step attaches to the row before it.
 */
private inline fun PlannedExerciseData.forEachUnrolledStep(
    block: (blockIndex: Int, round: Int, rounds: Int, step: PlannedExerciseStepData) -> Unit,
) {
    blocks.forEachIndexed { blockIndex, planBlock ->
        val rounds = planBlock.repetitions.coerceAtLeast(1)
        repeat(rounds) { roundIndex ->
            planBlock.steps.forEach { step -> block(blockIndex, roundIndex + 1, rounds, step) }
        }
    }
}

fun PlannedExerciseData.toRepetitionSetInputs(ownSegmentType: Int?): List<ActivityRepetitionSetInput> {
    val rows = mutableListOf<ActivityRepetitionSetInput>()
    forEachUnrolledStep { _, _, _, step ->
        val isRest = step.isRestStep()
        val segmentType = step.exerciseType.takeIf { it != ownSegmentType }
        val label = step.description?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("Set ") }
        val weightKgText = step.plannedWeightKg()?.toInputText(1).orEmpty()
        when (val completion = step.completion) {
            is PlannedExerciseCompletion.Repetitions -> if (!isRest) {
                rows += ActivityRepetitionSetInput(
                    repetitionsText = completion.repetitions.toString(),
                    segmentType = segmentType,
                    label = label,
                    weightKgText = weightKgText,
                )
            }
            is PlannedExerciseCompletion.DurationSeconds -> if (isRest) {
                val last = rows.lastOrNull() ?: return@forEachUnrolledStep
                rows[rows.lastIndex] = last.copy(restMinutesText = completion.seconds.toString())
            } else {
                rows += ActivityRepetitionSetInput(
                    repetitionsText = completion.seconds.toString(),
                    segmentType = segmentType,
                    label = label,
                    isDuration = true,
                    weightKgText = weightKgText,
                )
            }
            else -> Unit
        }
    }
    return rows
}

/**
 * The plan as the live recording runs it: the same unroll, with absolute
 * segment types, labels and recognizers. [localizedTitle] matches a
 * localized step name to its recognizer.
 */
fun PlannedExerciseData.toPlanRunSteps(
    localizedTitle: (type: ActivityEntryType) -> String? = { null },
): List<ActivityPlanRunStep> {
    val steps = mutableListOf<ActivityPlanRunStep>()
    forEachUnrolledStep { blockIndex, round, rounds, step ->
        val isRest = step.isRestStep()
        // "Set N" is the form's placeholder; a null label reads as the segment type's name.
        val label = step.description?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("Set ") }
        when (val completion = step.completion) {
            is PlannedExerciseCompletion.Repetitions -> if (!isRest) {
                steps += ActivityPlanRunStep(
                    segmentType = step.exerciseType,
                    label = label,
                    goalKind = ActivityPlanGoalKind.REPS,
                    goalValue = completion.repetitions.toLong().coerceAtLeast(1L),
                    restSeconds = 0L,
                    blockIndex = blockIndex,
                    round = round,
                    rounds = rounds,
                    sensorTypeId = planStepSensorTypeId(step.exerciseType, label, localizedTitle),
                    weightKg = step.plannedWeightKg(),
                )
            }
            is PlannedExerciseCompletion.DurationSeconds -> if (isRest) {
                val last = steps.lastOrNull() ?: return@forEachUnrolledStep
                steps[steps.lastIndex] = last.copy(restSeconds = completion.seconds.coerceAtLeast(0L))
            } else {
                steps += ActivityPlanRunStep(
                    segmentType = step.exerciseType,
                    label = label,
                    goalKind = ActivityPlanGoalKind.SECONDS,
                    goalValue = completion.seconds.coerceAtLeast(1L),
                    restSeconds = 0L,
                    blockIndex = blockIndex,
                    round = round,
                    rounds = rounds,
                    weightKg = step.plannedWeightKg(),
                )
            }
            else -> Unit
        }
    }
    return steps.numberSets()
}

/** Consecutive identical steps of one round are one set group: "Set 2 of 3". Rest is not compared. */
private fun List<ActivityPlanRunStep>.numberSets(): List<ActivityPlanRunStep> {
    val numbered = ArrayList<ActivityPlanRunStep>(size)
    var start = 0
    while (start < size) {
        var end = start + 1
        while (end < size && this[end].sameSetAs(this[start])) end += 1
        for (index in start until end) numbered += this[index].copy(setIndex = index - start + 1, sets = end - start)
        start = end
    }
    return numbered
}

private fun ActivityPlanRunStep.sameSetAs(other: ActivityPlanRunStep): Boolean =
    blockIndex == other.blockIndex &&
        round == other.round &&
        segmentType == other.segmentType &&
        label == other.label &&
        goalKind == other.goalKind &&
        goalValue == other.goalValue &&
        weightKg == other.weightKg

/** A plan the live recording can walk through: a set-based type with at least one countable step. */
fun PlannedExerciseData.isGuidedRunnable(): Boolean =
    toActivityEntryType()?.isRepetitionLike == true && toPlanRunSteps().isNotEmpty()

/** Form rows to plan steps. Null when a goal is not a positive number. */
fun List<ActivityRepetitionSetInput>.toPlannedSteps(ownSegmentType: Int): List<PlannedExerciseStepData>? =
    flatMapIndexed { index, row ->
        val goal = row.repetitionsText.trim().toLongOrNull()?.takeIf { it > 0L } ?: return null
        val weightKg = if (row.weightKgText.isBlank()) null else row.weightKgText.toWeightKgOrNull() ?: return null
        buildList {
            add(
                PlannedExerciseStepData(
                    exerciseType = row.segmentType ?: ownSegmentType,
                    exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                    description = row.label ?: "Set ${index + 1}",
                    completion = if (row.isDuration) {
                        PlannedExerciseCompletion.DurationSeconds(goal)
                    } else {
                        PlannedExerciseCompletion.Repetitions(goal.toInt())
                    },
                    performanceTargets = listOfNotNull(weightKg?.let { PlannedExercisePerformanceTarget.Weight(it) }),
                ),
            )
            row.restMinutesText.trim().toLongOrNull()?.takeIf { it > 0L }?.let { add(restPlanStep(it)) }
        }
    }.takeIf { it.isNotEmpty() }

/**
 * Form rows as plan blocks: consecutive identical rows collapse into one
 * block with that many rounds. A differing rest breaks the run.
 */
fun List<ActivityRepetitionSetInput>.toPlannedBlocks(ownSegmentType: Int): List<PlannedExerciseBlockData>? {
    val rows = this
    if (rows.isEmpty()) return null
    val blocks = mutableListOf<PlannedExerciseBlockData>()
    var runStart = 0
    while (runStart < rows.size) {
        val head = rows[runStart]
        var runEnd = runStart + 1
        while (runEnd < rows.size && rows[runEnd].sameStepAs(head)) runEnd += 1
        val steps = listOf(head).toPlannedSteps(ownSegmentType) ?: return null
        blocks += PlannedExerciseBlockData(
            repetitions = runEnd - runStart,
            description = head.label,
            steps = steps,
        )
        runStart = runEnd
    }
    return blocks
}

private fun ActivityRepetitionSetInput.sameStepAs(other: ActivityRepetitionSetInput): Boolean =
    segmentType == other.segmentType &&
        label == other.label &&
        isDuration == other.isDuration &&
        repetitionsText.trim() == other.repetitionsText.trim() &&
        restMinutesText.trim() == other.restMinutesText.trim() &&
        weightKgText.trim() == other.weightKgText.trim()

/** The plan the entry form hands to the builder for refinement. */
fun planRequestFromRows(
    exerciseType: Int,
    title: String,
    notes: String?,
    startTime: Instant,
    endTime: Instant,
    blocks: List<PlannedExerciseBlockData>,
): PlannedExerciseWriteRequest =
    PlannedExerciseWriteRequest(
        id = null,
        exerciseType = exerciseType,
        startTime = startTime,
        endTime = endTime,
        title = title,
        notes = notes,
        blocks = blocks,
    )

/** The one-block plan the entry form hands to the builder for refinement. */
fun singleBlockPlanRequest(
    exerciseType: Int,
    title: String,
    notes: String?,
    startTime: Instant,
    endTime: Instant,
    steps: List<PlannedExerciseStepData>,
): PlannedExerciseWriteRequest =
    PlannedExerciseWriteRequest(
        id = null,
        exerciseType = exerciseType,
        startTime = startTime,
        endTime = endTime,
        title = title,
        notes = notes,
        blocks = listOf(PlannedExerciseBlockData(repetitions = 1, description = title, steps = steps)),
    )

/** A picker choice as a fresh form row, using the builder's defaults for the goal. */
fun WorkoutPlanStepChoice.toRepetitionSetInput(): ActivityRepetitionSetInput =
    ActivityRepetitionSetInput(
        repetitionsText = when (defaultGoal) {
            WorkoutPlanGoalType.REPETITIONS -> WorkoutPlanStepInput.DefaultRepetitions.toString()
            WorkoutPlanGoalType.DURATION -> WorkoutPlanStepInput.DefaultActiveSeconds.toString()
        },
        segmentType = segmentType,
        label = description,
        isDuration = defaultGoal == WorkoutPlanGoalType.DURATION,
    )
