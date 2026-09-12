package tech.mmarca.openvitals.features.workoutplans

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.PlannedExerciseStep
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.PlannedExerciseBlockData
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExercisePerformanceTarget
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData
import tech.mmarca.openvitals.domain.model.restPlanStep

class WorkoutPlanBuilderMapperTest {

    private val zone: ZoneId = ZoneId.of("Europe/Madrid")

    private val squat = WorkoutPlanStepChoice(ExerciseSegment.EXERCISE_SEGMENT_TYPE_SQUAT)

    private fun squatStep(weightKg: Double? = null) = PlannedExerciseStepData(
        exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_SQUAT,
        exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
        description = null,
        completion = PlannedExerciseCompletion.Repetitions(12),
        performanceTargets = listOfNotNull(weightKg?.let { PlannedExercisePerformanceTarget.Weight(it) }),
    )

    private fun plankStep(seconds: Long) = PlannedExerciseStepData(
        exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
        exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
        description = null,
        completion = PlannedExerciseCompletion.DurationSeconds(seconds),
    )

    private fun form(steps: List<WorkoutPlanStepInput>) = WorkoutPlanFormInput(
        titleText = "Legs",
        startDateText = "2026-08-27",
        startTimeText = "7:30",
        durationMinutesText = "20",
        blocks = listOf(WorkoutPlanBlockInput(steps = steps)),
    )

    private fun stepsOf(form: WorkoutPlanFormInput): List<PlannedExerciseStepData> =
        requireNotNull(form.toWriteRequest(zone, existingId = null)).blocks.single().steps

    private fun rowsOf(steps: List<PlannedExerciseStepData>): List<WorkoutPlanStepInput> =
        plan(blocks = listOf(PlannedExerciseBlockData(1, null, steps))).toWorkoutPlanForm(zone).blocks.single().steps

    @Test
    fun `a set group writes a copy and a rest per set and reads back as one row`() {
        val row = WorkoutPlanStepInput.active(squat).copy(
            goalValueText = "12",
            setsText = "3",
            setRestText = "2",
            setRestUnit = WorkoutPlanDurationUnit.MINUTES,
            weightKgText = "50",
        )

        val written = stepsOf(form(listOf(row)))

        assertEquals(List(3) { listOf(squatStep(50.0), restPlanStep(120)) }.flatten(), written)
        val rows = rowsOf(written)
        assertEquals(1, rows.size)
        assertEquals("3", rows[0].setsText)
        assertEquals("2", rows[0].setRestText)
        assertEquals(WorkoutPlanDurationUnit.MINUTES, rows[0].setRestUnit)
        assertEquals("50", rows[0].weightKgText)
        assertTrue(rows[0].performanceTargets.isEmpty())
        assertEquals(written, stepsOf(form(rows)))
    }

    @Test
    fun `identical steps without rests collapse with a blank set rest`() {
        val steps = List(3) { squatStep() }

        val rows = rowsOf(steps)

        assertEquals(1, rows.size)
        assertEquals("3", rows[0].setsText)
        assertEquals("", rows[0].setRestText)
        assertEquals(steps, stepsOf(form(rows)))
    }

    @Test
    fun `a run without a trailing rest keeps the odd step as its own row`() {
        val steps = listOf(squatStep(), restPlanStep(120), squatStep(), restPlanStep(120), squatStep())

        val rows = rowsOf(steps)

        assertEquals(listOf("2", "1"), rows.map { it.setsText })
        assertEquals(listOf("2", ""), rows.map { it.setRestText })
        assertEquals(WorkoutPlanDurationUnit.MINUTES, rows[0].setRestUnit)
        assertEquals(steps, stepsOf(form(rows)))
    }

    @Test
    fun `a rest after a rest or an unsupported step stays a standalone row`() {
        val unsupported = PlannedExerciseStepData(
            exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
            exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_WARMUP,
            description = "Easy jog",
            completion = PlannedExerciseCompletion.Unknown,
        )
        val steps = listOf(squatStep(), restPlanStep(60), restPlanStep(180), unsupported, restPlanStep(60))

        val rows = rowsOf(steps)

        assertEquals(
            listOf(WorkoutPlanStepKind.ACTIVE, WorkoutPlanStepKind.REST, WorkoutPlanStepKind.UNSUPPORTED, WorkoutPlanStepKind.REST),
            rows.map { it.kind },
        )
        assertEquals("1", rows[0].setRestText)
        assertEquals(WorkoutPlanDurationUnit.MINUTES, rows[0].setRestUnit)
        assertEquals("3", rows[1].goalValueText)
        assertEquals(WorkoutPlanDurationUnit.MINUTES, rows[1].durationUnit)
        assertEquals(steps, stepsOf(form(rows)))
    }

    @Test
    fun `a weight target fills the weight field and is written back beside the other targets`() {
        val heartRate = PlannedExercisePerformanceTarget.HeartRate(140.0, 160.0)
        val step = squatStep().copy(performanceTargets = listOf(heartRate, PlannedExercisePerformanceTarget.Weight(50.0)))

        val rows = rowsOf(listOf(step))

        assertEquals("50", rows.single().weightKgText)
        assertEquals(listOf(heartRate), rows.single().performanceTargets)
        assertEquals(
            listOf(heartRate, PlannedExercisePerformanceTarget.Weight(50.0)),
            stepsOf(form(rows)).single().performanceTargets,
        )
    }

    @Test
    fun `durations read as minutes when whole and seconds otherwise`() {
        val rows = rowsOf(listOf(plankStep(120), restPlanStep(90)))

        val plank = rows.single()
        assertEquals("2", plank.goalValueText)
        assertEquals(WorkoutPlanDurationUnit.MINUTES, plank.durationUnit)
        assertEquals("90", plank.setRestText)
        assertEquals(WorkoutPlanDurationUnit.SECONDS, plank.setRestUnit)
        assertEquals(listOf(plankStep(120), restPlanStep(90)), stepsOf(form(rows)))

        val rest = WorkoutPlanStepInput.rest()
        assertEquals("1", rest.goalValueText)
        assertEquals(WorkoutPlanDurationUnit.MINUTES, rest.durationUnit)
        assertEquals(60L, rest.goalValueOrNull())
    }

    @Test
    fun `push-ups and planks map to blocks with rep and duration goals`() {
        val pushUps = WorkoutPlanStepCatalog.first { it.description == "Push-ups" }
        val plank = WorkoutPlanStepChoice(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK)
        val form = WorkoutPlanFormInput(
            titleText = "  Strength  ",
            notesText = "",
            sessionExerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
            startDateText = "2026-08-27",
            startTimeText = "7:30",
            durationMinutesText = "20",
            blocks = listOf(
                WorkoutPlanBlockInput(
                    nameText = "Push-ups",
                    roundsText = "3",
                    steps = listOf(
                        WorkoutPlanStepInput.active(pushUps).copy(goalValueText = "10"),
                        WorkoutPlanStepInput.rest(60),
                    ),
                ),
                WorkoutPlanBlockInput(
                    roundsText = "2",
                    steps = listOf(
                        WorkoutPlanStepInput.active(plank).copy(goalValueText = "45"),
                        WorkoutPlanStepInput.rest(60),
                    ),
                ),
            ),
        )

        val request = requireNotNull(form.toWriteRequest(zone, existingId = "existing"))

        assertEquals("existing", request.id)
        assertEquals("Strength", request.title)
        assertNull(request.notes)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS, request.exerciseType)
        assertEquals(LocalDateTime.of(2026, 8, 27, 7, 30).atZone(zone).toInstant(), request.startTime)
        assertEquals(request.startTime.plusSeconds(20 * 60), request.endTime)

        val (pushUpBlock, plankBlock) = request.blocks
        assertEquals(3, pushUpBlock.repetitions)
        assertEquals("Push-ups", pushUpBlock.description)
        assertEquals(
            PlannedExerciseStepData(
                exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
                exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                description = "Push-ups",
                completion = PlannedExerciseCompletion.Repetitions(10),
            ),
            pushUpBlock.steps[0],
        )
        assertEquals(
            PlannedExerciseStepData(
                exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST,
                exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_REST,
                description = null,
                completion = PlannedExerciseCompletion.DurationSeconds(60),
            ),
            pushUpBlock.steps[1],
        )
        assertEquals(2, plankBlock.repetitions)
        assertNull(plankBlock.description)
        assertEquals(
            PlannedExerciseStepData(
                exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                description = null,
                completion = PlannedExerciseCompletion.DurationSeconds(45),
            ),
            plankBlock.steps[0],
        )
    }

    @Test
    fun `loading a plan classifies rest by phase and keeps unsupported steps verbatim`() {
        val unsupported = PlannedExerciseStepData(
            exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
            exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_WARMUP,
            description = "Easy jog",
            completion = PlannedExerciseCompletion.Unknown,
        )
        val plan = plan(
            blocks = listOf(
                PlannedExerciseBlockData(
                    repetitions = 2,
                    description = "Main",
                    steps = listOf(
                        unsupported,
                        PlannedExerciseStepData(
                            exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                            exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                            description = null,
                            completion = PlannedExerciseCompletion.DurationSeconds(45),
                        ),
                        PlannedExerciseStepData(
                            exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
                            exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_REST,
                            description = "Breathe",
                            completion = PlannedExerciseCompletion.DurationSeconds(30),
                        ),
                        PlannedExerciseStepData(
                            exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_SQUAT,
                            exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_COOLDOWN,
                            description = null,
                            completion = PlannedExerciseCompletion.Repetitions(12),
                        ),
                    ),
                ),
            ),
        )

        val form = plan.toWorkoutPlanForm(zone)

        assertEquals("Pull-up ladder", form.titleText)
        assertEquals("2026-05-26", form.startDateText)
        assertEquals("10:30", form.startTimeText)
        assertEquals("5", form.durationMinutesText)
        val block = form.blocks.single()
        assertEquals("Main", block.nameText)
        assertEquals("2", block.roundsText)
        val kinds = block.steps.map { it.kind }
        // The rest after the plank is the plank's set rest, so it is not a row of its own.
        assertEquals(
            listOf(
                WorkoutPlanStepKind.UNSUPPORTED,
                WorkoutPlanStepKind.ACTIVE,
                WorkoutPlanStepKind.ACTIVE,
            ),
            kinds,
        )
        assertEquals(WorkoutPlanGoalType.DURATION, block.steps[1].goalType)
        assertEquals("45", block.steps[1].goalValueText)
        assertEquals("1", block.steps[1].setsText)
        assertEquals("30", block.steps[1].setRestText)
        assertEquals(WorkoutPlanDurationUnit.SECONDS, block.steps[1].setRestUnit)
        assertEquals(PlannedExerciseStep.EXERCISE_PHASE_COOLDOWN, block.steps[2].exercisePhase)

        // Round-trip: the unsupported step and the foreign phases survive untouched.
        val request = requireNotNull(form.toWriteRequest(zone, existingId = plan.id))
        val steps = request.blocks.single().steps
        assertEquals(unsupported, steps[0])
        assertEquals(PlannedExerciseStep.EXERCISE_PHASE_COOLDOWN, steps[3].exercisePhase)
        assertEquals(PlannedExerciseCompletion.Repetitions(12), steps[3].completion)
        // A rest step is normalised to the REST segment type on the way out.
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST, steps[2].exerciseType)
        assertEquals(PlannedExerciseCompletion.DurationSeconds(30), steps[2].completion)
    }

    @Test
    fun `copy to today keeps clock time, length and blocks with no id`() {
        val plan = plan()

        val copy = plan.toCopyForDate(LocalDate.of(2026, 8, 26), zone)

        assertNull(copy.id)
        assertEquals(LocalDateTime.of(2026, 8, 26, 10, 30).atZone(zone).toInstant(), copy.startTime)
        assertEquals(copy.startTime.plusSeconds(5 * 60), copy.endTime)
        assertEquals(plan.blocks, copy.blocks)
        assertEquals(plan.title, copy.title)
        assertEquals(plan.exerciseType, copy.exerciseType)
    }

    @Test
    fun `new plan form defaults to a rounded time today with one empty block`() {
        val form = workoutPlanFormForNewPlan(LocalDateTime.of(2026, 8, 26, 18, 47, 12))

        assertEquals("2026-08-26", form.startDateText)
        assertEquals("18:45", form.startTimeText)
        assertEquals("30", form.durationMinutesText)
        assertEquals(1, form.blocks.size)
        assertTrue(form.blocks.single().steps.isEmpty())
    }

    @Test
    fun `toWriteRequest returns null when the form does not validate`() {
        val form = workoutPlanFormForNewPlan(LocalDateTime.of(2026, 8, 26, 18, 0))

        assertNull(form.toWriteRequest(zone, existingId = null))
    }

    private fun plan(
        blocks: List<PlannedExerciseBlockData> = listOf(
            PlannedExerciseBlockData(
                repetitions = 1,
                description = "Main set",
                steps = listOf(
                    PlannedExerciseStepData(
                        exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP,
                        exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                        description = null,
                        completion = PlannedExerciseCompletion.Repetitions(8),
                    ),
                ),
            ),
        ),
    ): PlannedExerciseData = PlannedExerciseData(
        id = "planned-id",
        title = "Pull-up ladder",
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        startTime = Instant.parse("2026-05-26T08:30:00Z"),
        endTime = Instant.parse("2026-05-26T08:35:00Z"),
        hasExplicitTime = true,
        completedExerciseSessionId = null,
        notes = "Strict reps",
        blockCount = blocks.size,
        source = "tech.mmarca.openvitals",
        blocks = blocks,
    )

    @Test
    fun `performance targets and foreign goals survive a trip through the builder`() {
        val targets = listOf(
            tech.mmarca.openvitals.domain.model.PlannedExercisePerformanceTarget.HeartRate(140.0, 160.0),
            tech.mmarca.openvitals.domain.model.PlannedExercisePerformanceTarget.Power(200.0, 250.0),
        )
        val distanceStep = PlannedExerciseStepData(
            exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
            exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
            description = null,
            completion = PlannedExerciseCompletion.DistanceMeters(2000.0),
            performanceTargets = targets,
        )
        val repStep = PlannedExerciseStepData(
            exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_SQUAT,
            exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
            description = null,
            completion = PlannedExerciseCompletion.Repetitions(12),
            performanceTargets = targets,
        )
        val plan = plan(blocks = listOf(PlannedExerciseBlockData(1, null, listOf(distanceStep, repStep))))

        val form = plan.toWorkoutPlanForm(zone)
        val steps = requireNotNull(form.toWriteRequest(zone, existingId = plan.id)).blocks.single().steps

        assertEquals(WorkoutPlanStepKind.UNSUPPORTED, form.blocks.single().steps[0].kind)
        assertEquals(targets, form.blocks.single().steps[1].performanceTargets)
        assertEquals(distanceStep, steps[0])
        assertEquals(repStep, steps[1])
    }
}
