package tech.mmarca.openvitals.features.workoutplans

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.PlannedExerciseStep
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.model.PlannedExerciseBlockData
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExercisePerformanceTarget
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData
import tech.mmarca.openvitals.domain.model.restPlanStep

class WorkoutPlanExportTest {

    private val plan = PlannedExerciseData(
        id = "p1",
        title = "Tempo run",
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        startTime = Instant.parse("2026-08-27T06:30:00Z"),
        endTime = Instant.parse("2026-08-27T07:30:00Z"),
        startZoneOffset = ZoneOffset.ofHours(2),
        hasExplicitTime = true,
        completedExerciseSessionId = "s1",
        notes = "Easy",
        blockCount = 1,
        source = "com.garmin.android.apps.connectmobile",
        blocks = listOf(
            PlannedExerciseBlockData(
                repetitions = 3,
                description = "Intervals",
                steps = listOf(
                    PlannedExerciseStepData(
                        exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
                        exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                        description = null,
                        completion = PlannedExerciseCompletion.DistanceAndDuration(1000.0, 300),
                        performanceTargets = listOf(
                            PlannedExercisePerformanceTarget.HeartRate(150.0, 165.0),
                            PlannedExercisePerformanceTarget.Speed(3.5, 4.0),
                            PlannedExercisePerformanceTarget.RateOfPerceivedExertion(7),
                            PlannedExercisePerformanceTarget.Amrap,
                        ),
                    ),
                    restPlanStep(90),
                    PlannedExerciseStepData(
                        exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_WALKING,
                        exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_COOLDOWN,
                        description = "Cool down",
                        completion = PlannedExerciseCompletion.Steps(500),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun `plans round-trip through the export file with goals and targets intact`() {
        val text = listOf(plan).toExportJson(Instant.parse("2026-08-27T08:00:00Z"))

        val restored = requireNotNull(parseWorkoutPlanExport(text)).single()

        assertNull(restored.id)
        assertEquals(plan.title, restored.title)
        assertEquals(plan.notes, restored.notes)
        assertEquals(plan.exerciseType, restored.exerciseType)
        assertEquals(plan.startTime, restored.startTime)
        assertEquals(plan.endTime, restored.endTime)
        assertEquals(plan.blocks, restored.blocks)
    }

    @Test
    fun `text that is not an export is refused`() {
        assertNull(parseWorkoutPlanExport("{\"hello\": 1}"))
        assertNull(parseWorkoutPlanExport("not json"))
    }
}
