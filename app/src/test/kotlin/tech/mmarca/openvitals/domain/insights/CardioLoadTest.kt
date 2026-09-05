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
        // Five 15-minute buckets, as Health Connect chart aggregation returns over a ~51 minute workout.
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

    @Test
    fun `without a trustworthy observed max, age sets the ceiling`() {
        // A quiet fortnight: nothing above 141. The old estimate took 151 as the maximum, so a walk
        // at 95 bpm scored as training. Tanaka for age 30 says ~187, threshold 97 bpm.
        val samples = (0 until 30).map { minute ->
            HeartRateSample(workoutStart.plusSeconds(minute * 60L), 95, source = "")
        }

        val withAge = calculateCardioLoad(
            steps = steps,
            samples = samples,
            restingHeartRate = 59L,
            baselineRestingHeartRate = 59L,
            observedMaxHeartRate = 141L,
            activityWindows = emptyList(),
            ageYears = 30,
        )
        val withoutAge = calculateCardioLoad(
            steps = steps,
            samples = samples,
            restingHeartRate = 59L,
            baselineRestingHeartRate = 59L,
            observedMaxHeartRate = 141L,
            activityWindows = emptyList(),
        )

        assertEquals(187L, withAge.maxHeartRateBpm)
        assertEquals(false, withAge.maxHeartRateObserved)
        // Against a 187 ceiling, 95 bpm is below the elevated threshold and
        // the day falls back to movement - it was never training.
        assertEquals(CardioLoadMethod.MOVEMENT_FALLBACK, withAge.method)
        // The unaided estimate still inflates; the point of passing age.
        assertEquals(151L, withoutAge.maxHeartRateBpm)
        assertEquals(CardioLoadMethod.TRIMP_ELEVATED_HEART_RATE, withoutAge.method)
    }

    @Test
    fun `a genuinely observed max beats the age estimate`() {
        // 172 seen on the wrist clears the trustworthiness bar, so it wins over any formula.
        val samples = (0 until 30).map { minute ->
            HeartRateSample(workoutStart.plusSeconds(minute * 60L), 150, source = "")
        }

        val estimate = calculateCardioLoad(
            steps = steps,
            samples = samples,
            restingHeartRate = 59L,
            baselineRestingHeartRate = 59L,
            observedMaxHeartRate = 172L,
            activityWindows = emptyList(),
            ageYears = 30,
        )

        assertEquals(172L, estimate.maxHeartRateBpm)
        assertEquals(true, estimate.maxHeartRateObserved)
    }

    @Test
    fun `a profile-stated max outranks the estimates`() {
        val samples = (0 until 30).map { minute ->
            HeartRateSample(workoutStart.plusSeconds(minute * 60L), 95, source = "")
        }

        val estimate = calculateCardioLoad(
            steps = steps,
            samples = samples,
            restingHeartRate = 59L,
            baselineRestingHeartRate = 59L,
            observedMaxHeartRate = 141L,
            activityWindows = emptyList(),
            ageYears = 30,
            explicitMaxHeartRate = 195L,
        )

        // The user stated it; nothing is guessed, so it counts as known.
        assertEquals(195L, estimate.maxHeartRateBpm)
        assertEquals(true, estimate.maxHeartRateObserved)
    }
}
