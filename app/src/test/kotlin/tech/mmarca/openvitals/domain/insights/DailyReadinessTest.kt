package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoad
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoadTargetSource
import tech.mmarca.openvitals.domain.model.DashboardWeeklyIntensityMinutes
import tech.mmarca.openvitals.domain.model.SleepData

class DailyReadinessTest {

    private val date = LocalDate.of(2026, 6, 10)

    @Test
    fun readyWhenSleepAndRecoverySignalsAreStrong() {
        val insight = calculateDailyReadiness(
            DashboardData(
                date = date,
                sleep = sleep(Duration.ofHours(8)),
                sleepScore = sleepScore(score = 88, hours = 8.0),
                restingHeartRateBpm = 55,
                restingHeartRateBaselineBpm = 58,
                hrvRmssdMs = 62.0,
                hrvBaselineRmssdMs = 56.0,
                avgHeartRateBpm = 68,
                weeklyCardioLoad = weeklyLoad(current = 90, target = 100, today = 12),
                weeklyIntensityMinutes = weeklyIntensity(minutes = 160, today = 34),
                mindfulnessMinutes = 10,
                loadedMetrics = setOf(
                    DashboardMetric.SLEEP,
                    DashboardMetric.AVG_HEART_RATE,
                    DashboardMetric.RESTING_HEART_RATE,
                    DashboardMetric.HRV,
                    DashboardMetric.WEEKLY_CARDIO_LOAD,
                    DashboardMetric.INTENSITY_MINUTES,
                    DashboardMetric.MINDFULNESS,
                ),
            )
        )

        assertEquals(ReadinessState.READY, insight.state)
        assertEquals(ReadinessRecommendationType.HARD_TRAINING, insight.recommendationType)
        assertEquals(ReadinessConfidence.HIGH, insight.confidence)
        assertEquals(HrvStatus.BALANCED, insight.hrvStatus.status)
        assertEquals(IntensityMinutesStatus.GOAL_MET, insight.intensityMinutes.status)
        assertEquals(PhysiologicalStressLevel.RESTING, insight.physiologicalStress.level)
        assertTrue(insight.score >= 80)
        assertTrue(insight.factors.any { it.kind == ReadinessFactorKind.HRV_NORMAL })
        assertTrue(insight.factors.any { it.kind == ReadinessFactorKind.INTENSITY_MINUTES_ON_TARGET })
        assertTrue(insight.factors.any { it.kind == ReadinessFactorKind.RESTING_HR_NORMAL })
    }

    @Test
    fun recoveryDayWhenSleepHrvAndRestingHeartRateArePoor() {
        val insight = calculateDailyReadiness(
            DashboardData(
                date = date,
                sleep = sleep(Duration.ofHours(5)),
                sleepScore = sleepScore(score = 36, hours = 5.0),
                restingHeartRateBpm = 68,
                restingHeartRateBaselineBpm = 58,
                hrvRmssdMs = 35.0,
                hrvBaselineRmssdMs = 55.0,
                avgHeartRateBpm = 94,
                weeklyCardioLoad = weeklyLoad(current = 145, target = 100, today = 20),
                loadedMetrics = setOf(
                    DashboardMetric.SLEEP,
                    DashboardMetric.AVG_HEART_RATE,
                    DashboardMetric.RESTING_HEART_RATE,
                    DashboardMetric.HRV,
                    DashboardMetric.WEEKLY_CARDIO_LOAD,
                ),
            )
        )

        assertEquals(ReadinessState.REST, insight.state)
        assertEquals(ReadinessRecommendationType.REST, insight.recommendationType)
        assertEquals(HrvStatus.UNUSUALLY_LOW, insight.hrvStatus.status)
        assertEquals(PhysiologicalStressLevel.HIGH, insight.physiologicalStress.level)
        assertTrue(insight.recoveryModeSuggested)
        assertTrue(insight.score < 40)
        assertTrue(insight.factors.any { it.kind == ReadinessFactorKind.STRESS_HIGH })
        assertTrue(insight.recommendation.contains("Avoid intense training"))
    }

    @Test
    fun hrvStatusUsesPersonalBaselineThresholds() {
        assertEquals(
            HrvStatus.BALANCED,
            calculateHrvStatus(hrvRmssdMs = 51.0, baselineRmssdMs = 50.0, hasHrvData = true).status,
        )
        assertEquals(
            HrvStatus.LOW,
            calculateHrvStatus(hrvRmssdMs = 42.0, baselineRmssdMs = 50.0, hasHrvData = true).status,
        )
        assertEquals(
            HrvStatus.UNUSUALLY_LOW,
            calculateHrvStatus(hrvRmssdMs = 34.0, baselineRmssdMs = 50.0, hasHrvData = true).status,
        )
        assertEquals(
            HrvStatus.HIGH,
            calculateHrvStatus(hrvRmssdMs = 58.0, baselineRmssdMs = 50.0, hasHrvData = true).status,
        )
        assertEquals(
            HrvStatus.UNUSUALLY_HIGH,
            calculateHrvStatus(hrvRmssdMs = 66.0, baselineRmssdMs = 50.0, hasHrvData = true).status,
        )
        assertEquals(
            HrvStatus.NEEDS_MORE_HRV,
            calculateHrvStatus(hrvRmssdMs = null, baselineRmssdMs = 50.0, hasHrvData = true).status,
        )
        assertEquals(
            HrvStatus.NEEDS_MORE_HRV,
            calculateHrvStatus(hrvRmssdMs = 50.0, baselineRmssdMs = null, hasHrvData = true).status,
        )
        assertEquals(
            HrvStatus.NEEDS_MORE_HRV,
            calculateHrvStatus(hrvRmssdMs = 50.0, baselineRmssdMs = 50.0, hasHrvData = false).status,
        )
    }

    @Test
    fun checkSymptomsWhenTemperatureSignalIsUnusual() {
        val insight = calculateDailyReadiness(
            DashboardData(
                date = date,
                sleep = sleep(Duration.ofHours(6)),
                sleepScore = sleepScore(score = 55, hours = 6.0),
                restingHeartRateBpm = 67,
                restingHeartRateBaselineBpm = 58,
                hrvRmssdMs = 38.0,
                hrvBaselineRmssdMs = 55.0,
                latestBodyTemperatureCelsius = 38.0,
                loadedMetrics = setOf(
                    DashboardMetric.SLEEP,
                    DashboardMetric.RESTING_HEART_RATE,
                    DashboardMetric.HRV,
                    DashboardMetric.BODY_TEMPERATURE,
                ),
            )
        )

        assertEquals(ReadinessState.REST, insight.state)
        assertEquals(ReadinessRecommendationType.CHECK_SYMPTOMS, insight.recommendationType)
        assertTrue(insight.factors.any { it.kind == ReadinessFactorKind.TEMPERATURE_ELEVATED })
        assertTrue(insight.recommendation.contains("If you feel unwell"))
    }

    @Test
    fun intensityMinutesReadinessUsesWeeklyPace() {
        assertEquals(
            IntensityMinutesStatus.GOAL_MET,
            calculateIntensityMinutesReadiness(
                weeklyIntensityMinutes = weeklyIntensity(minutes = 151, today = 20),
                hasIntensityData = true,
            ).status,
        )
        assertEquals(
            IntensityMinutesStatus.ON_TRACK,
            calculateIntensityMinutesReadiness(
                weeklyIntensityMinutes = weeklyIntensity(minutes = 70, today = 10, daysElapsed = 3),
                hasIntensityData = true,
            ).status,
        )
        assertEquals(
            IntensityMinutesStatus.BEHIND,
            calculateIntensityMinutesReadiness(
                weeklyIntensityMinutes = weeklyIntensity(minutes = 90, today = 0, daysElapsed = 6),
                hasIntensityData = true,
            ).status,
        )
        assertEquals(
            IntensityMinutesStatus.NEEDS_MORE_DATA,
            calculateIntensityMinutesReadiness(
                weeklyIntensityMinutes = null,
                hasIntensityData = true,
            ).status,
        )
    }

    @Test
    fun unknownWhenNoSignalsAreAvailable() {
        val insight = calculateDailyReadiness(DashboardData(date = date))

        assertEquals(ReadinessState.UNKNOWN, insight.state)
        assertEquals(ReadinessConfidence.LOW, insight.confidence)
        assertEquals(0, insight.score)
        assertTrue(insight.explanation.contains("not enough local data"))
    }

    @Test
    fun explanationJoinsFactorDetailsWithoutDoublePunctuation() {
        val insight = calculateDailyReadiness(
            DashboardData(
                date = date,
                sleep = sleep(Duration.ofHours(5)),
                sleepScore = sleepScore(score = 36, hours = 5.0),
                restingHeartRateBpm = 68,
                restingHeartRateBaselineBpm = 58,
                hrvRmssdMs = 35.0,
                hrvBaselineRmssdMs = 55.0,
                avgHeartRateBpm = 94,
                weeklyCardioLoad = weeklyLoad(current = 145, target = 100, today = 20),
                loadedMetrics = setOf(
                    DashboardMetric.SLEEP,
                    DashboardMetric.AVG_HEART_RATE,
                    DashboardMetric.RESTING_HEART_RATE,
                    DashboardMetric.HRV,
                    DashboardMetric.WEEKLY_CARDIO_LOAD,
                ),
            ),
        )

        assertFalse(insight.explanation.contains("., and"))
        assertTrue(insight.explanation.endsWith("."))
    }

    @Test
    fun nutritionFactorNotShownWhenOnlyHydrationIsLogged() {
        val insight = calculateDailyReadiness(
            DashboardData(
                date = date,
                hydrationLiters = 1.5,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 0.0,
                loadedMetrics = setOf(
                    DashboardMetric.HYDRATION,
                    DashboardMetric.PROTEIN,
                    DashboardMetric.CARBS,
                    DashboardMetric.FAT,
                ),
            ),
            goals = DailyReadinessGoalInputs(hydrationLitersGoal = 2.0),
        )

        assertFalse(insight.factors.any { it.kind == ReadinessFactorKind.NUTRITION_LOGGED })
    }

    @Test
    fun nutritionFactorShownWhenMealDataIsPresent() {
        val insight = calculateDailyReadiness(
            DashboardData(
                date = date,
                caloriesInKcal = 1_800.0,
                loadedMetrics = setOf(DashboardMetric.CALORIES_IN),
            ),
        )

        assertTrue(insight.factors.any { it.kind == ReadinessFactorKind.NUTRITION_LOGGED })
    }

    @Test
    fun lowConfidenceWhenBaselinesAreMissing() {
        val insight = calculateDailyReadiness(
            DashboardData(
                date = date,
                sleep = sleep(Duration.ofHours(7)),
                sleepScore = sleepScore(score = 76, hours = 7.0),
                restingHeartRateBpm = 56,
                hrvRmssdMs = 50.0,
                loadedMetrics = setOf(
                    DashboardMetric.SLEEP,
                    DashboardMetric.RESTING_HEART_RATE,
                    DashboardMetric.HRV,
                ),
            )
        )

        assertEquals(ReadinessConfidence.LOW, insight.confidence)
        assertEquals("new_user_not_enough_baseline", insight.confidenceReason)
        assertEquals(HrvStatus.NEEDS_MORE_HRV, insight.hrvStatus.status)
        assertTrue(insight.factors.any { it.kind == ReadinessFactorKind.NEW_USER_NOT_ENOUGH_BASELINE })
    }

    private fun sleep(duration: Duration): SleepData =
        SleepData(
            id = "sleep",
            startTime = Instant.parse("2026-06-10T00:00:00Z"),
            endTime = Instant.parse("2026-06-10T00:00:00Z").plus(duration),
            durationMs = duration.toMillis(),
            source = "test",
        )

    private fun sleepScore(
        score: Int,
        hours: Double,
    ): SleepScoreEstimate =
        SleepScoreEstimate(
            score = score,
            confidence = SleepScoreConfidence.HIGH,
            sleepDurationMinutes = hours * 60.0,
            timeInBedMinutes = hours * 60.0,
        )

    private fun weeklyLoad(
        current: Int,
        target: Int,
        today: Int,
    ): DashboardWeeklyCardioLoad =
        DashboardWeeklyCardioLoad(
            currentScore = current,
            targetScore = target,
            todayScore = today,
            confidence = CardioLoadConfidence.HIGH,
            targetSource = DashboardWeeklyCardioLoadTargetSource.RECENT_HISTORY,
        )

    private fun weeklyIntensity(
        minutes: Int,
        today: Int,
        daysElapsed: Int = 7,
        confidence: IntensityMinutesConfidence = IntensityMinutesConfidence.HIGH,
    ): DashboardWeeklyIntensityMinutes =
        DashboardWeeklyIntensityMinutes(
            moderateMinutes = minutes,
            vigorousMinutes = 0,
            moderateEquivalentMinutes = minutes,
            todayModerateEquivalentMinutes = today,
            daysElapsed = daysElapsed,
            confidence = confidence,
        )
}
