package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.PlannedExerciseBlock
import androidx.health.connect.client.records.PlannedExerciseStep
import androidx.health.connect.client.records.ExerciseCompletionGoal
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.kilocalories
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ActivityExerciseSegmentWrite
import tech.mmarca.openvitals.domain.model.ActivityPauseInterval
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData

class ActivityHealthReaderTest {
    @Test fun `exercise segments include active intervals around pauses`() {
        val start = Instant.parse("2026-05-26T08:00:00Z")
        val firstPauseStart = start.plusSeconds(600)
        val firstPauseEnd = start.plusSeconds(900)
        val secondPauseStart = start.plusSeconds(1_800)
        val secondPauseEnd = start.plusSeconds(1_920)
        val end = start.plusSeconds(3_600)
        val request = ActivityWriteRequest(
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            startTime = start,
            endTime = end,
            pauseIntervals = listOf(
                ActivityPauseInterval(firstPauseStart, firstPauseEnd),
                ActivityPauseInterval(secondPauseStart, secondPauseEnd),
            ),
        )

        val segments = request.toExerciseSegments()

        assertEquals(5, segments.size)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING, segments[0].segmentType)
        assertEquals(start, segments[0].startTime)
        assertEquals(firstPauseStart, segments[0].endTime)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE, segments[1].segmentType)
        assertEquals(firstPauseStart, segments[1].startTime)
        assertEquals(firstPauseEnd, segments[1].endTime)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING, segments[2].segmentType)
        assertEquals(firstPauseEnd, segments[2].startTime)
        assertEquals(secondPauseStart, segments[2].endTime)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE, segments[3].segmentType)
        assertEquals(secondPauseStart, segments[3].startTime)
        assertEquals(secondPauseEnd, segments[3].endTime)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING, segments[4].segmentType)
        assertEquals(secondPauseEnd, segments[4].startTime)
        assertEquals(end, segments[4].endTime)
    }

    @Test fun `exercise segments include one active segment without pauses`() {
        val start = Instant.parse("2026-05-26T08:00:00Z")
        val end = start.plusSeconds(1_800)
        val request = ActivityWriteRequest(
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
            startTime = start,
            endTime = end,
        )

        val segments = request.toExerciseSegments()

        assertEquals(1, segments.size)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_WALKING, segments.first().segmentType)
        assertEquals(start, segments.first().startTime)
        assertEquals(end, segments.first().endTime)
    }

    @Test fun `exercise segments preserve explicit repetitions and set index`() {
        val start = Instant.parse("2026-05-26T08:00:00Z")
        val firstSetEnd = start.plusSeconds(60)
        val restEnd = firstSetEnd.plusSeconds(30)
        val end = restEnd.plusSeconds(60)
        val request = ActivityWriteRequest(
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
            startTime = start,
            endTime = end,
            exerciseSegments = listOf(
                ActivityExerciseSegmentWrite(
                    startTime = start,
                    endTime = firstSetEnd,
                    segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP,
                    repetitions = 8,
                    setIndex = 0,
                ),
                ActivityExerciseSegmentWrite(
                    startTime = firstSetEnd,
                    endTime = restEnd,
                    segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST,
                ),
                ActivityExerciseSegmentWrite(
                    startTime = restEnd,
                    endTime = end,
                    segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP,
                    repetitions = 6,
                    setIndex = 1,
                ),
            ),
        )

        val segments = request.toExerciseSegments()

        assertEquals(3, segments.size)
        assertEquals(8, segments[0].repetitions)
        assertEquals(0, segments[0].setIndex)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST, segments[1].segmentType)
        assertEquals(6, segments[2].repetitions)
        assertEquals(1, segments[2].setIndex)
    }

    @Test fun `planned exercise blocks preserve set repetitions and rest duration`() {
        val block = PlannedExerciseBlock(
            repetitions = 1,
            description = "Main set",
            steps = listOf(
                PlannedExerciseStep(
                    exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP,
                    exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                    completionGoal = ExerciseCompletionGoal.RepetitionsGoal(8),
                    performanceTargets = emptyList(),
                    description = "Set 1",
                ),
                PlannedExerciseStep(
                    exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST,
                    exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_REST,
                    completionGoal = ExerciseCompletionGoal.DurationGoal(java.time.Duration.ofSeconds(60)),
                    performanceTargets = emptyList(),
                    description = "Rest",
                ),
            ),
        )

        val data = block.toPlannedExerciseBlockData()

        assertEquals(1, data.repetitions)
        assertEquals("Main set", data.description)
        assertEquals(PlannedExerciseCompletion.Repetitions(8), data.steps[0].completion)
        assertEquals(PlannedExerciseCompletion.DurationSeconds(60), data.steps[1].completion)
    }

    @Test fun `planned exercise step data writes repetitions goal`() {
        val step = PlannedExerciseStepData(
            exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP,
            exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
            description = "Set 1",
            completion = PlannedExerciseCompletion.Repetitions(6),
        ).toPlannedExerciseStep()

        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP, step.exerciseType)
        assertEquals(PlannedExerciseStep.EXERCISE_PHASE_ACTIVE, step.exercisePhase)
        assertEquals(ExerciseCompletionGoal.RepetitionsGoal(6), step.completionGoal)
    }

    @Test
    fun `dailyStepDateChunks splits long ranges into inclusive chunks`() {
        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 1, 10)

        val chunks = dailyStepDateChunks(start, end, maxDays = 4)

        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 1) to LocalDate.of(2026, 1, 4),
                LocalDate.of(2026, 1, 5) to LocalDate.of(2026, 1, 8),
                LocalDate.of(2026, 1, 9) to LocalDate.of(2026, 1, 10),
            ),
            chunks,
        )
    }

    @Test
    fun `dailyStepDateChunks returns empty list for invalid ranges`() {
        assertTrue(
            dailyStepDateChunks(
                startDate = LocalDate.of(2026, 1, 10),
                endDate = LocalDate.of(2026, 1, 1),
            ).isEmpty()
        )
    }

    @Test
    fun `totalCaloriesBurnedRecordDates includes every overlapping local date`() {
        val zone = ZoneId.of("UTC")
        val records = listOf(
            TotalCaloriesBurnedRecord(
                startTime = Instant.parse("2026-06-01T23:30:00Z"),
                startZoneOffset = null,
                endTime = Instant.parse("2026-06-02T00:30:00Z"),
                endZoneOffset = null,
                energy = 100.0.kilocalories,
                metadata = Metadata.manualEntry(),
            )
        )

        val dates = records.totalCaloriesBurnedRecordDates(
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 3),
            zone = zone,
        )

        assertEquals(
            setOf(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 2),
            ),
            dates,
        )
    }

    @Test
    fun `totalCaloriesRecordedOrDailyEstimated adds active calories to full day BMR when total is missing`() {
        val value = totalCaloriesRecordedOrDailyEstimated(
            recordedTotalCaloriesKcal = null,
            activeCaloriesKcal = 228.0,
            bmrKcalPerDay = 1_715.0,
        )

        assertEquals(1_943.0, value!!.kcal, 0.01)
        assertEquals(CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR, value.source)
    }

    @Test
    fun `totalCaloriesRecordedOrIntervalEstimated prorates BMR for interval estimates`() {
        val value = totalCaloriesRecordedOrIntervalEstimated(
            recordedTotalCaloriesKcal = null,
            activeCaloriesKcal = 361.0,
            bmrKcalPerDay = 1_800.0,
            start = Instant.parse("2026-06-01T00:00:00Z"),
            end = Instant.parse("2026-06-01T12:00:00Z"),
        )

        assertEquals(1_261.0, value!!.kcal, 0.01)
        assertEquals(CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR, value.source)
    }

    @Test
    fun `totalCaloriesRecordedOrDailyEstimated keeps recorded total when available`() {
        val value = totalCaloriesRecordedOrDailyEstimated(
            recordedTotalCaloriesKcal = 1_800.0,
            activeCaloriesKcal = 361.0,
            bmrKcalPerDay = 1_800.0,
        )

        assertEquals(1_800.0, value!!.kcal, 0.01)
        assertEquals(CaloriesBurnedSource.RECORDED_TOTAL, value.source)
    }

    @Test
    fun `totalCaloriesRecordedOrDailyEstimated does not use active calories without BMR`() {
        val value = totalCaloriesRecordedOrDailyEstimated(
            recordedTotalCaloriesKcal = null,
            activeCaloriesKcal = 361.0,
            bmrKcalPerDay = null,
        )

        assertNull(value)
    }
}
