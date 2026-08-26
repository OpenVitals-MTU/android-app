package tech.mmarca.openvitals.features.workoutplans

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.PlannedExerciseStep
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.model.PlannedExerciseBlockData
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData
import tech.mmarca.openvitals.domain.model.restPlanStep
import tech.mmarca.openvitals.features.manualentry.activity.ActivityRepetitionSetInput
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityPlanGoalKind
import tech.mmarca.openvitals.features.manualentry.activity.recording.planStepSensorTypeId
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

class WorkoutPlanStepMappingTest {

    private fun active(type: Int, completion: PlannedExerciseCompletion, description: String? = null) =
        PlannedExerciseStepData(
            exerciseType = type,
            exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
            description = description,
            completion = completion,
        )

    private val plan = PlannedExerciseData(
        id = "plan",
        title = "Strength",
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        startTime = Instant.parse("2026-08-26T17:00:00Z"),
        endTime = Instant.parse("2026-08-26T17:20:00Z"),
        hasExplicitTime = true,
        completedExerciseSessionId = null,
        notes = null,
        blockCount = 2,
        source = "tech.mmarca.openvitals",
        blocks = listOf(
            PlannedExerciseBlockData(
                repetitions = 2,
                description = "Push-ups",
                steps = listOf(
                    active(ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT, PlannedExerciseCompletion.Repetitions(10), "Push-ups"),
                    restPlanStep(60),
                ),
            ),
            PlannedExerciseBlockData(
                repetitions = 1,
                description = null,
                steps = listOf(
                    active(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK, PlannedExerciseCompletion.DurationSeconds(45), "Set 3"),
                    active(ExerciseSegment.EXERCISE_SEGMENT_TYPE_SQUAT, PlannedExerciseCompletion.Unknown),
                ),
            ),
        ),
    )

    @Test
    fun `plan rows unroll rounds, attach rest backwards and skip unknown goals`() {
        val rows = plan.toRepetitionSetInputs(ownSegmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT)

        assertEquals(
            listOf(
                ActivityRepetitionSetInput(repetitionsText = "10", restMinutesText = "60", label = "Push-ups"),
                ActivityRepetitionSetInput(repetitionsText = "10", restMinutesText = "60", label = "Push-ups"),
                ActivityRepetitionSetInput(
                    repetitionsText = "45",
                    segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                    isDuration = true,
                ),
            ),
            rows,
        )
    }

    @Test
    fun `rows become active and rest steps with the row's own exercise`() {
        val rows = listOf(
            ActivityRepetitionSetInput(repetitionsText = "8", restMinutesText = "60"),
            ActivityRepetitionSetInput(
                repetitionsText = "45",
                segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                label = "Hold",
                isDuration = true,
            ),
        )

        val steps = requireNotNull(rows.toPlannedSteps(ownSegmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP))

        assertEquals(3, steps.size)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP, steps[0].exerciseType)
        assertEquals("Set 1", steps[0].description)
        assertEquals(PlannedExerciseCompletion.Repetitions(8), steps[0].completion)
        assertEquals(restPlanStep(60), steps[1])
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK, steps[2].exerciseType)
        assertEquals("Hold", steps[2].description)
        assertEquals(PlannedExerciseCompletion.DurationSeconds(45), steps[2].completion)
    }

    @Test
    fun `a row without a positive goal makes the whole list invalid`() {
        assertNull(listOf(ActivityRepetitionSetInput(repetitionsText = "0")).toPlannedSteps(1))
        assertNull(emptyList<ActivityRepetitionSetInput>().toPlannedSteps(1))
    }

    @Test
    fun `single block request wraps the steps under the title`() {
        val steps = listOf(active(ExerciseSegment.EXERCISE_SEGMENT_TYPE_SQUAT, PlannedExerciseCompletion.Repetitions(12)))
        val start = Instant.parse("2026-08-26T17:00:00Z")

        val request = singleBlockPlanRequest(
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            title = "Legs",
            notes = null,
            startTime = start,
            endTime = start.plusSeconds(600),
            steps = steps,
        )

        assertNull(request.id)
        assertEquals("Legs", request.title)
        assertEquals(1, request.blocks.size)
        assertEquals(1, request.blocks.single().repetitions)
        assertEquals("Legs", request.blocks.single().description)
        assertEquals(steps, request.blocks.single().steps)
    }

    @Test
    fun `a picker choice becomes a row with the builder's default goal`() {
        val plank = WorkoutPlanStepChoice(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK).toRepetitionSetInput()
        val pushUps = WorkoutPlanStepCatalog.first().toRepetitionSetInput()

        assertEquals(WorkoutPlanStepInput.DefaultActiveSeconds.toString(), plank.repetitionsText)
        assertEquals(true, plank.isDuration)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK, plank.segmentType)
        assertEquals(WorkoutPlanStepInput.DefaultRepetitions.toString(), pushUps.repetitionsText)
        assertEquals("Push-ups", pushUps.label)
        assertEquals(false, pushUps.isDuration)
    }

    @Test
    fun `plan run steps unroll rounds, fold rest and name the sensor that can count them`() {
        val steps = plan.toPlanRunSteps()

        assertEquals(3, steps.size)
        assertEquals(listOf(1, 2, 1), steps.map { it.round })
        assertEquals(listOf(2, 2, 1), steps.map { it.rounds })
        assertEquals(listOf(0, 0, 1), steps.map { it.blockIndex })
        assertEquals("Push-ups", steps[0].label)
        assertEquals(ActivityPlanGoalKind.REPS, steps[0].goalKind)
        assertEquals(10L, steps[0].goalValue)
        assertEquals(60L, steps[0].restSeconds)
        assertEquals("push_ups", steps[0].sensorTypeId)
        assertNull(steps[2].label)
        assertEquals(ActivityPlanGoalKind.SECONDS, steps[2].goalKind)
        assertEquals(45L, steps[2].goalValue)
        assertEquals(0L, steps[2].restSeconds)
        assertNull(steps[2].sensorTypeId)
        assertTrue(plan.isGuidedRunnable())
    }

    @Test
    fun `sensor lookup matches the segment and the preset label`() {
        assertEquals("pull_ups", planStepSensorTypeId(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP, "Pull-ups"))
        assertEquals("rope_skipping", planStepSensorTypeId(ExerciseSegment.EXERCISE_SEGMENT_TYPE_JUMP_ROPE, "Jump rope"))
        assertEquals("push_ups", planStepSensorTypeId(ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT, "push-ups"))
        assertEquals("trampoline_jumping", planStepSensorTypeId(ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT, "Trampoline jumping"))
        assertNull(planStepSensorTypeId(ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT, "Burpees"))
        assertNull(planStepSensorTypeId(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK, null))
        assertNull(planStepSensorTypeId(ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT, null))
    }

    @Test
    fun `a plan whose type is not set-based is not guided-runnable`() {
        val run = plan.copy(
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            blocks = listOf(
                PlannedExerciseBlockData(
                    repetitions = 1,
                    description = null,
                    steps = listOf(active(ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING, PlannedExerciseCompletion.DurationSeconds(600))),
                ),
            ),
        )
        assertFalse(run.isGuidedRunnable())
    }

    @Test
    fun `consecutive identical rows collapse into a block with rounds`() {
        val rows = listOf(
            ActivityRepetitionSetInput(repetitionsText = "10", restMinutesText = "60", label = "Push-ups"),
            ActivityRepetitionSetInput(repetitionsText = "10", restMinutesText = "60", label = "Push-ups"),
            ActivityRepetitionSetInput(repetitionsText = "10", restMinutesText = "60", label = "Push-ups"),
            ActivityRepetitionSetInput(repetitionsText = "45", segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK, isDuration = true),
            ActivityRepetitionSetInput(repetitionsText = "45", segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK, isDuration = true),
            ActivityRepetitionSetInput(repetitionsText = "8", restMinutesText = "60", label = "Push-ups"),
        )

        val blocks = requireNotNull(rows.toPlannedBlocks(ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT))

        assertEquals(listOf(3, 2, 1), blocks.map { it.repetitions })
        assertEquals(listOf("Push-ups", null, "Push-ups"), blocks.map { it.description })
        assertEquals(2, blocks[0].steps.size)
        assertEquals(PlannedExerciseCompletion.Repetitions(10), blocks[0].steps[0].completion)
        assertEquals(restPlanStep(60), blocks[0].steps[1])
        assertEquals(1, blocks[1].steps.size)
        assertEquals(PlannedExerciseCompletion.DurationSeconds(45), blocks[1].steps[0].completion)
        assertNull(emptyList<ActivityRepetitionSetInput>().toPlannedBlocks(1))
    }
}
