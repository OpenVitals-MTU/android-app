package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.HeartRateSample

class IntensityMinutesTest {

    private val start = Instant.parse("2026-06-10T10:00:00Z")

    @Test
    fun heartRateReserveCountsModerateMinutes() {
        val estimate = calculateIntensityMinutes(
            samples = samples(bpm = 120L, minutes = 30),
            restingHeartRate = 60L,
            baselineRestingHeartRate = null,
            observedMaxHeartRate = 180L,
            activityWindows = listOf(CardioLoadTimeWindow(start, start.plusSeconds(30 * 60L))),
            workouts = emptyList(),
            dailyActiveCaloriesKcal = null,
            cardioLoadScore = null,
        )

        assertEquals(30, estimate.moderateMinutes)
        assertEquals(0, estimate.vigorousMinutes)
        assertEquals(30, estimate.moderateEquivalentMinutes)
        assertEquals(IntensityMinutesConfidence.HIGH, estimate.confidence)
        assertEquals(IntensityMinutesMethod.HEART_RATE_RESERVE, estimate.method)
    }

    @Test
    fun heartRateReserveCountsVigorousMinutesDouble() {
        val estimate = calculateIntensityMinutes(
            samples = samples(bpm = 140L, minutes = 30),
            restingHeartRate = 60L,
            baselineRestingHeartRate = null,
            observedMaxHeartRate = 180L,
            activityWindows = listOf(CardioLoadTimeWindow(start, start.plusSeconds(30 * 60L))),
            workouts = emptyList(),
            dailyActiveCaloriesKcal = null,
            cardioLoadScore = null,
        )

        assertEquals(0, estimate.moderateMinutes)
        assertEquals(30, estimate.vigorousMinutes)
        assertEquals(60, estimate.moderateEquivalentMinutes)
    }

    @Test
    fun workoutActiveCaloriesFallbackIsLowConfidence() {
        val estimate = calculateIntensityMinutes(
            samples = emptyList(),
            restingHeartRate = null,
            baselineRestingHeartRate = null,
            observedMaxHeartRate = null,
            activityWindows = emptyList(),
            workouts = listOf(IntensityWorkoutInput(durationMinutes = 30.0, activeCaloriesKcal = 270.0)),
            dailyActiveCaloriesKcal = null,
            cardioLoadScore = null,
        )

        assertEquals(30, estimate.vigorousMinutes)
        assertEquals(60, estimate.moderateEquivalentMinutes)
        assertEquals(IntensityMinutesConfidence.LOW, estimate.confidence)
        assertEquals(IntensityMinutesMethod.WORKOUT_ACTIVE_CALORIES, estimate.method)
    }

    @Test
    fun cardioLoadFallbackProvidesLowConfidenceEstimate() {
        val estimate = calculateIntensityMinutes(
            samples = emptyList(),
            restingHeartRate = null,
            baselineRestingHeartRate = null,
            observedMaxHeartRate = null,
            activityWindows = emptyList(),
            workouts = emptyList(),
            dailyActiveCaloriesKcal = null,
            cardioLoadScore = 3,
        )

        assertEquals(12, estimate.moderateEquivalentMinutes)
        assertEquals(IntensityMinutesConfidence.LOW, estimate.confidence)
        assertEquals(IntensityMinutesMethod.CARDIO_LOAD, estimate.method)
    }

    @Test
    fun noInputsReturnNoData() {
        val estimate = calculateIntensityMinutes(
            samples = emptyList(),
            restingHeartRate = null,
            baselineRestingHeartRate = null,
            observedMaxHeartRate = null,
            activityWindows = emptyList(),
            workouts = emptyList(),
            dailyActiveCaloriesKcal = null,
            cardioLoadScore = null,
        )

        assertEquals(IntensityMinutesConfidence.NO_DATA, estimate.confidence)
        assertEquals(IntensityMinutesMethod.NO_DATA, estimate.method)
        assertEquals(0, estimate.moderateEquivalentMinutes)
    }

    private fun samples(bpm: Long, minutes: Int): List<HeartRateSample> =
        (0..minutes).map { minute ->
            HeartRateSample(
                time = start.plusSeconds(minute * 60L),
                beatsPerMinute = bpm,
                source = "watch",
            )
        }
}
