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

    @Test
    fun `every English sentence has a structured mirror carrying its numbers`() {
        // Every appended sentence must have an item beside it, or the localized screen drops a line.
        val hrStart = Instant.parse("2026-06-10T06:00:00Z")
        val hrEnd = Instant.parse("2026-06-10T10:00:00Z")
        val estimate = calculatePhysiologicalStress(
            DashboardData(
                date = date,
                avgHeartRateBpm = 76,
                heartRateSampleCount = 4,
                heartRateSampleStartTime = hrStart,
                heartRateSampleEndTime = hrEnd,
                restingHeartRateBpm = 62,
                restingHeartRateBaselineBpm = 58,
                hrvRmssdMs = 45.0,
                hrvBaselineRmssdMs = 50.0,
                hrvSampleCount = 2,
                hrvSampleStartTime = hrStart,
                hrvSampleEndTime = hrEnd,
                sleepScore = SleepScoreEstimate(
                    score = 40,
                    confidence = SleepScoreConfidence.MEDIUM,
                    sleepDurationMinutes = 300.0,
                ),
                hydrationLiters = 0.2,
                latestSkinTemperatureDeltaCelsius = 0.7,
                mindfulnessMinutes = 12,
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

        assertEquals(estimate.contributingFactors.size, estimate.factorItems.size)
        assertEquals(estimate.dataCoverage.size, estimate.coverageItems.size)
        assertEquals(estimate.caveats.size, estimate.caveatItems.size)

        // The numbers travel as args rather than being baked into a sentence.
        val hrvItem = estimate.factorItems.first { it.template == StressItemTemplate.HRV_BELOW_BASELINE }
        assertEquals(10.0, hrvItem.args.single(), 0.0)
        val restingItem = estimate.factorItems.first { it.template == StressItemTemplate.RESTING_HR_ABOVE }
        assertEquals(4.0, restingItem.args.single(), 0.0)
        val sleepItem = estimate.factorItems.first { it.template == StressItemTemplate.SLEEP_RAISES_STRAIN }
        assertEquals(40.0, sleepItem.args.single(), 0.0)
        val hydrationItem = estimate.factorItems.first { it.template == StressItemTemplate.HYDRATION_SO_FAR }
        assertEquals(0.2, hydrationItem.args.single(), 1e-9)
        val mindfulItem = estimate.factorItems.first { it.template == StressItemTemplate.MINDFULNESS_LOGGED }
        assertEquals(12.0, mindfulItem.args.single(), 0.0)

        // Coverage windows pass epoch millis; an absent optional is NaN.
        val coverage = estimate.coverageItems.first { it.template == StressItemTemplate.COVERAGE_HR_SAMPLES }
        assertEquals(4.0, coverage.args[0], 0.0)
        assertEquals(hrStart.toEpochMilli().toDouble(), coverage.args[1], 0.0)
        assertEquals(hrEnd.toEpochMilli().toDouble(), coverage.args[2], 0.0)

        // Temperature carries both readings, NaN for the one that is missing.
        val temperature = estimate.factorItems
            .first { it.template == StressItemTemplate.TEMPERATURE_SLIGHTLY_ELEVATED }
        assertTrue(temperature.args[0].isNaN())
        assertEquals(0.7, temperature.args[1], 1e-9)

        assertTrue(estimate.caveatItems.first().template == StressItemTemplate.CAVEAT_NOT_MENTAL_STRESS)
    }

    @Test
    fun `the needs-more-data estimate still carries its caveat and coverage items`() {
        val estimate = calculatePhysiologicalStress(
            DashboardData(
                date = date,
                loadedMetrics = setOf(DashboardMetric.AVG_HEART_RATE, DashboardMetric.HRV),
            )
        )

        assertEquals(PhysiologicalStressLevel.NEEDS_MORE_DATA, estimate.level)
        assertEquals(estimate.caveats.size, estimate.caveatItems.size)
        assertEquals(estimate.dataCoverage.size, estimate.coverageItems.size)
        assertTrue(estimate.coverageItems.any { it.template == StressItemTemplate.COVERAGE_HR_NONE })
        assertTrue(estimate.coverageItems.any { it.template == StressItemTemplate.COVERAGE_HRV_NONE })
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
