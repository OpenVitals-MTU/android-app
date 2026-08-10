package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.HeartRateSample

class CardioLoadTest {
    private val day = java.time.LocalDate.of(2026, 8, 10)
    private val workoutStart = Instant.parse("2026-08-10T06:50:00Z")
    private val workoutEnd = Instant.parse("2026-08-10T07:41:00Z")
    private val windows = listOf(CardioLoadTimeWindow(workoutStart, workoutEnd))
    private val steps = DailySteps(
        date = day,
        steps = 8_000L,
        distanceMeters = 7_000.0,
        activeCaloriesKcal = 400.0,
    )

    @Test
    fun `fifteen minute chart buckets force movement fallback`() {
        // Mimics Health Connect chart aggregation over a ~51 minute workout:
        // five 15-minute buckets with no consecutive pair inside the 5-minute gap budget.
        val samples = listOf(
            HeartRateSample(workoutStart, 150, source = ""),
            HeartRateSample(workoutStart.plusSeconds(15 * 60), 155, source = ""),
            HeartRateSample(workoutStart.plusSeconds(30 * 60), 158, source = ""),
            HeartRateSample(workoutStart.plusSeconds(45 * 60), 156, source = ""),
            HeartRateSample(workoutStart.plusSeconds(60 * 60), 154, source = ""),
        )

        val estimate = calculateCardioLoad(
            steps = steps,
            samples = samples,
            restingHeartRate = 60L,
            baselineRestingHeartRate = 60L,
            observedMaxHeartRate = 170L,
            activityWindows = windows,
        )

        assertEquals(CardioLoadMethod.MOVEMENT_FALLBACK, estimate.method)
        assertEquals(0.0, estimate.coveredMinutes, 0.001)
        assertTrue(estimate.score > 0)
    }

    @Test
    fun `one minute insight buckets keep TRIMP coverage`() {
        val samples = (0 until 50).map { minute ->
            HeartRateSample(
                time = workoutStart.plusSeconds(minute * 60L),
                beatsPerMinute = 150L + (minute % 5),
                source = "",
            )
        }

        val estimate = calculateCardioLoad(
            steps = steps,
            samples = samples,
            restingHeartRate = 60L,
            baselineRestingHeartRate = 60L,
            observedMaxHeartRate = 170L,
            activityWindows = windows,
        )

        assertEquals(CardioLoadMethod.TRIMP_ACTIVITY_WINDOWS, estimate.method)
        assertTrue(estimate.coveredMinutes >= 5.0)
        assertTrue(estimate.score > 0)
    }
}
