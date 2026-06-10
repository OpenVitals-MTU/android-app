package tech.mmarca.openvitals.features.activity

import androidx.health.connect.client.records.ExerciseSegment
import java.time.Instant
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseSegmentData

class ActivityMetricsTest {

    @Test fun `moving duration excludes pause segments`() {
        val workout = workout(
            durationMs = 3_600_000L,
            segments = listOf(
                segment(0, 1_200, ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING),
                segment(1_200, 1_800, ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE),
                segment(1_800, 3_600, ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING),
            ),
        )

        assertEquals(3_000_000L, workout.movingDurationMs())
    }

    @Test fun `average pace and speed use moving duration when pauses are present`() {
        val workout = workout(
            durationMs = 3_600_000L,
            totalDistanceMeters = 10_000.0,
            segments = listOf(
                segment(0, 1_200, ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING),
                segment(1_200, 1_800, ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE),
                segment(1_800, 3_600, ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING),
            ),
        )
        val formatter = formatter(UnitSystem.METRIC)

        assertEquals("5:00 min/km", workout.averagePace(formatter)?.text)
        assertEquals("12.0 km/h", workout.averageSpeed(formatter)?.text)
    }

    @Test fun `average pace and speed need distance`() {
        val workout = workout(totalDistanceMeters = null)
        val formatter = formatter(UnitSystem.METRIC)

        assertNull(workout.averagePace(formatter))
        assertNull(workout.averageSpeed(formatter))
    }

    private fun workout(
        durationMs: Long = 3_600_000L,
        totalDistanceMeters: Double? = null,
        segments: List<ExerciseSegmentData> = emptyList(),
    ) = ExerciseData(
        id = "activity-1",
        title = "Morning run",
        exerciseType = 56,
        startTime = Instant.EPOCH,
        endTime = Instant.EPOCH.plusMillis(durationMs),
        durationMs = durationMs,
        source = "test",
        totalDistanceMeters = totalDistanceMeters,
        segments = segments,
    )

    private fun segment(startSeconds: Long, endSeconds: Long, type: Int) =
        ExerciseSegmentData(
            startTime = Instant.EPOCH.plusSeconds(startSeconds),
            endTime = Instant.EPOCH.plusSeconds(endSeconds),
            segmentType = type,
            repetitions = 0,
        )

    private fun formatter(unitSystem: UnitSystem): UnitFormatter =
        UnitFormatter(
            unitSystemProvider = { unitSystem },
            localeProvider = { Locale.US },
        )
}
