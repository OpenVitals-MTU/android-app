package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.ExerciseData

class StressTrackingTest {

    private val date = LocalDate.of(2026, 6, 10)

    @Test
    fun lowHrvAndElevatedRestingHeartRateProduceHighStress() {
        val estimate = calculatePhysiologicalStress(
            DashboardData(
                date = date,
                avgHeartRateBpm = 91,
                restingHeartRateBpm = 72,
                restingHeartRateBaselineBpm = 58,
                hrvRmssdMs = 32.0,
                hrvBaselineRmssdMs = 55.0,
                loadedMetrics = setOf(
                    DashboardMetric.AVG_HEART_RATE,
                    DashboardMetric.RESTING_HEART_RATE,
                    DashboardMetric.HRV,
                ),
            )
        )

        assertEquals(PhysiologicalStressLevel.HIGH, estimate.level)
        assertTrue(estimate.score ?: 0 >= 76)
        assertEquals(PhysiologicalStressConfidence.HIGH, estimate.confidence)
    }

    @Test
    fun balancedHrvAndNormalRestingHeartRateProduceRestingStress() {
        val estimate = calculatePhysiologicalStress(
            DashboardData(
                date = date,
                avgHeartRateBpm = 62,
                restingHeartRateBpm = 55,
                restingHeartRateBaselineBpm = 56,
                hrvRmssdMs = 58.0,
                hrvBaselineRmssdMs = 56.0,
                loadedMetrics = setOf(
                    DashboardMetric.AVG_HEART_RATE,
                    DashboardMetric.RESTING_HEART_RATE,
                    DashboardMetric.HRV,
                ),
            )
        )

        assertEquals(PhysiologicalStressLevel.RESTING, estimate.level)
        assertEquals(PhysiologicalStressConfidence.HIGH, estimate.confidence)
    }

    @Test
    fun workoutsLowerConfidenceAndAddActivityCaveat() {
        val estimate = calculatePhysiologicalStress(
            DashboardData(
                date = date,
                avgHeartRateBpm = 82,
                restingHeartRateBpm = 58,
                restingHeartRateBaselineBpm = 58,
                hrvRmssdMs = 54.0,
                hrvBaselineRmssdMs = 56.0,
                workouts = listOf(workout()),
                loadedMetrics = setOf(
                    DashboardMetric.WORKOUT,
                    DashboardMetric.AVG_HEART_RATE,
                    DashboardMetric.RESTING_HEART_RATE,
                    DashboardMetric.HRV,
                ),
            )
        )

        assertTrue(estimate.hasWorkoutInfluence)
        assertEquals(PhysiologicalStressConfidence.MEDIUM, estimate.confidence)
        assertTrue(estimate.caveats.any { it.contains("workouts", ignoreCase = true) })
    }

    @Test
    fun noStressSignalsNeedMoreData() {
        val estimate = calculatePhysiologicalStress(DashboardData(date = date))

        assertEquals(PhysiologicalStressLevel.NEEDS_MORE_DATA, estimate.level)
        assertEquals(PhysiologicalStressConfidence.NO_DATA, estimate.confidence)
        assertEquals(null, estimate.score)
    }

    @Test
    fun oneHrvPointIsUsedButReportedAsThinCoverage() {
        val estimate = calculatePhysiologicalStress(
            DashboardData(
                date = date,
                hrvRmssdMs = 46.0,
                hrvBaselineRmssdMs = 50.0,
                hrvSampleCount = 1,
                hrvSampleStartTime = Instant.parse("2026-06-10T09:00:00Z"),
                hrvSampleEndTime = Instant.parse("2026-06-10T09:00:00Z"),
                loadedMetrics = setOf(DashboardMetric.HRV),
            )
        )

        assertEquals(PhysiologicalStressLevel.LOW, estimate.level)
        assertTrue(estimate.dataCoverage.any { it.contains("1 RMSSD point") })
        assertTrue(estimate.caveats.any { it.contains("Only one HRV point") })
    }

    @Test
    fun dayContextCanRaiseStressEstimateAroundHeartSignals() {
        val estimate = calculatePhysiologicalStress(
            DashboardData(
                date = date,
                avgHeartRateBpm = 76,
                heartRateSampleCount = 4,
                heartRateSampleStartTime = Instant.parse("2026-06-10T06:00:00Z"),
                heartRateSampleEndTime = Instant.parse("2026-06-10T10:00:00Z"),
                restingHeartRateBpm = 62,
                restingHeartRateBaselineBpm = 58,
                hrvRmssdMs = 45.0,
                hrvBaselineRmssdMs = 50.0,
                hrvSampleCount = 2,
                hrvSampleStartTime = Instant.parse("2026-06-10T06:10:00Z"),
                hrvSampleEndTime = Instant.parse("2026-06-10T10:10:00Z"),
                sleepScore = SleepScoreEstimate(
                    score = 40,
                    confidence = SleepScoreConfidence.MEDIUM,
                    sleepDurationMinutes = 300.0,
                ),
                hydrationLiters = 0.2,
                latestSkinTemperatureDeltaCelsius = 0.7,
                loadedMetrics = setOf(
                    DashboardMetric.AVG_HEART_RATE,
                    DashboardMetric.RESTING_HEART_RATE,
                    DashboardMetric.HRV,
                    DashboardMetric.SLEEP,
                    DashboardMetric.HYDRATION,
                    DashboardMetric.SKIN_TEMPERATURE,
                ),
            )
        )

        assertTrue(estimate.score ?: 0 >= 70)
        assertTrue(estimate.contributingFactors.any { it.contains("Sleep score is 40") })
        assertTrue(estimate.contributingFactors.any { it.contains("Hydration") })
        assertTrue(estimate.contributingFactors.any { it.contains("Temperature context") })
        assertTrue(estimate.dataCoverage.any { it.contains("Heart rate used 4 samples") })
        assertTrue(estimate.dataCoverage.any { it.contains("HRV used 2 RMSSD points") })
    }

    private fun workout(): ExerciseData {
        val start = Instant.parse("2026-06-10T07:00:00Z")
        val end = start.plus(Duration.ofMinutes(35))
        return ExerciseData(
            id = "run",
            title = null,
            exerciseType = 0,
            startTime = start,
            endTime = end,
            durationMs = Duration.between(start, end).toMillis(),
            source = "test",
        )
    }
}
