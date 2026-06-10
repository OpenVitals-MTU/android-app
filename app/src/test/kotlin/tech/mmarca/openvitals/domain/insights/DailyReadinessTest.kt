package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoad
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoadTargetSource
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
                weeklyCardioLoad = weeklyLoad(current = 90, target = 100, today = 12),
                mindfulnessMinutes = 10,
                loadedMetrics = setOf(
                    DashboardMetric.SLEEP,
                    DashboardMetric.RESTING_HEART_RATE,
                    DashboardMetric.HRV,
                    DashboardMetric.WEEKLY_CARDIO_LOAD,
                    DashboardMetric.MINDFULNESS,
                ),
            )
        )

        assertEquals(ReadinessState.READY, insight.state)
        assertEquals(ReadinessRecommendationType.HARD_TRAINING, insight.recommendationType)
        assertEquals(ReadinessConfidence.HIGH, insight.confidence)
        assertTrue(insight.score >= 80)
        assertTrue(insight.factors.any { it.kind == ReadinessFactorKind.HRV_NORMAL })
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
                weeklyCardioLoad = weeklyLoad(current = 145, target = 100, today = 20),
                loadedMetrics = setOf(
                    DashboardMetric.SLEEP,
                    DashboardMetric.RESTING_HEART_RATE,
                    DashboardMetric.HRV,
                    DashboardMetric.WEEKLY_CARDIO_LOAD,
                ),
            )
        )

        assertEquals(ReadinessState.REST, insight.state)
        assertEquals(ReadinessRecommendationType.REST, insight.recommendationType)
        assertTrue(insight.recoveryModeSuggested)
        assertTrue(insight.score < 40)
        assertTrue(insight.factors.any { it.kind == ReadinessFactorKind.STRESS_HIGH })
        assertTrue(insight.recommendation.contains("Avoid intense training"))
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
    fun unknownWhenNoSignalsAreAvailable() {
        val insight = calculateDailyReadiness(DashboardData(date = date))

        assertEquals(ReadinessState.UNKNOWN, insight.state)
        assertEquals(ReadinessConfidence.LOW, insight.confidence)
        assertEquals(0, insight.score)
        assertTrue(insight.explanation.contains("not enough local data"))
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
}
