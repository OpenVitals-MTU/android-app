package tech.mmarca.openvitals.data.cache

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.CardioLoadConfidence
import tech.mmarca.openvitals.domain.insights.IntensityMinutesConfidence
import tech.mmarca.openvitals.domain.insights.SleepScoreConfidence
import tech.mmarca.openvitals.domain.insights.SleepScoreEstimate
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoad
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoadTargetSource
import tech.mmarca.openvitals.domain.model.DashboardWeeklyIntensityMinutes
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage

class DashboardDataSummaryCodecTest {
    @Test
    fun `dashboard data round trips through versioned json`() {
        val start = Instant.parse("2026-06-23T06:30:00Z")
        val workout = ExerciseData(
            id = "workout-1",
            title = "Morning ride",
            exerciseType = 8,
            startTime = start,
            endTime = start.plusSeconds(3600),
            durationMs = 3_600_000,
            source = "test",
            totalDistanceMeters = 12_000.0,
            totalCaloriesKcal = 450.0,
            totalCaloriesSource = CaloriesBurnedSource.RECORDED_TOTAL,
        )
        val sleep = SleepData(
            id = "sleep-1",
            startTime = Instant.parse("2026-06-22T21:00:00Z"),
            endTime = Instant.parse("2026-06-23T05:00:00Z"),
            durationMs = 28_800_000,
            source = "test",
            stages = listOf(
                SleepStage(
                    startTime = Instant.parse("2026-06-22T21:00:00Z"),
                    endTime = Instant.parse("2026-06-22T22:00:00Z"),
                    stageType = SleepStage.STAGE_LIGHT,
                )
            ),
        )
        val data = DashboardData(
            date = LocalDate.of(2026, 6, 23),
            steps = 8_000,
            distanceMeters = 6_200.0,
            caloriesKcal = 2_100.0,
            workout = workout,
            workouts = listOf(workout),
            sleep = sleep,
            sleepScore = SleepScoreEstimate(
                score = 82,
                confidence = SleepScoreConfidence.HIGH,
                sleepStageCount = 1,
                usesSleepStages = true,
            ),
            bmi = 22.3,
            ffmi = 17.4,
            weeklyCardioLoad = DashboardWeeklyCardioLoad(
                currentScore = 250,
                targetScore = 400,
                todayScore = 40,
                confidence = CardioLoadConfidence.HIGH,
                targetSource = DashboardWeeklyCardioLoadTargetSource.RECENT_HISTORY,
            ),
            weeklyIntensityMinutes = DashboardWeeklyIntensityMinutes(
                moderateMinutes = 80,
                vigorousMinutes = 20,
                moderateEquivalentMinutes = 120,
                todayModerateEquivalentMinutes = 15,
                daysElapsed = 3,
                confidence = IntensityMinutesConfidence.MEDIUM,
            ),
            missingPermissions = setOf("missing-a"),
            loadedMetrics = setOf(
                DashboardMetric.STEPS,
                DashboardMetric.SLEEP,
                DashboardMetric.WORKOUT,
                DashboardMetric.BMI,
                DashboardMetric.FFMI,
            ),
            caloriesKcalSource = CaloriesBurnedSource.RECORDED_TOTAL,
        )

        val decoded = DashboardDataSummaryCodec.decode(DashboardDataSummaryCodec.encode(data))

        assertEquals(data.date, decoded.date)
        assertEquals(data.steps, decoded.steps)
        assertEquals(data.workout?.title, decoded.workout?.title)
        assertEquals(data.workouts.size, decoded.workouts.size)
        assertEquals(data.sleep?.stages?.size, decoded.sleep?.stages?.size)
        assertEquals(data.sleepScore.score, decoded.sleepScore.score)
        assertEquals(data.bmi, decoded.bmi)
        assertEquals(data.ffmi, decoded.ffmi)
        assertEquals(data.weeklyCardioLoad?.targetScore, decoded.weeklyCardioLoad?.targetScore)
        assertEquals(data.weeklyIntensityMinutes?.moderateEquivalentMinutes, decoded.weeklyIntensityMinutes?.moderateEquivalentMinutes)
        assertEquals(data.loadedMetrics, decoded.loadedMetrics)
        assertNotNull(decoded.sleep)
    }
}
