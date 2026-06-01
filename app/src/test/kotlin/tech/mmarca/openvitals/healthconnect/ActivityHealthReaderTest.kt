package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.data.model.ActivityPauseInterval
import tech.mmarca.openvitals.data.model.ActivityWriteRequest

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
}
