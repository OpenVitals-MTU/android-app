package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthDataTest {

    // ExerciseData.durationMinutes.

    @Test fun `durationMinutes truncates sub-minute remainder`() {
        assertEquals(
            mapOf(59_999L to 0L, 90_000L to 1L, 3_600_000L to 60L),
            listOf(59_999L, 90_000L, 3_600_000L).associateWith { exercise(durationMs = it).durationMinutes },
        )
    }

    // SleepData.durationHours.

    @Test fun `durationHours returns fractional hours`() {
        assertEquals(7.5, sleep(durationMs = 27_000_000L).durationHours, 0.001)
    }

    @Test fun `durationHours is zero for zero duration`() {
        assertEquals(0.0, sleep(durationMs = 0L).durationHours, 0.0)
    }

    // SleepStage.durationMs.

    @Test fun `SleepStage durationMs equals end minus start epoch millis`() {
        val stage = SleepStage(
            startTime = Instant.ofEpochMilli(1_000_000L),
            endTime = Instant.ofEpochMilli(2_500_000L),
            stageType = SleepStage.STAGE_REM,
        )
        assertEquals(1_500_000L, stage.durationMs)
    }

    // Optional fields: null means "not read", a zero is a reading.

    @Test fun `DailySteps optional fields default to null and a provided zero stays zero`() {
        val unset = DailySteps(date = LocalDate.of(2026, 1, 1), steps = 1_000L, distanceMeters = 800.0)
        val zero = DailySteps(
            date = LocalDate.of(2026, 1, 1),
            steps = 0L,
            distanceMeters = 0.0,
            floorsClimbed = 0,
            activeCaloriesKcal = 0.0,
            elevationGainedMeters = 0.0,
        )

        assertEquals(
            listOf(null, null, null),
            listOf(unset.floorsClimbed, unset.activeCaloriesKcal, unset.elevationGainedMeters),
        )
        assertEquals(
            listOf<Number?>(0, 0.0, 0.0),
            listOf(zero.floorsClimbed, zero.activeCaloriesKcal, zero.elevationGainedMeters),
        )
    }

    @Test fun `ActivityProgressPoint defaults detailed optional fields to null`() {
        val point = ActivityProgressPoint(
            time = Instant.EPOCH,
            totalSteps = 1_000L,
            totalDistanceMeters = null,
            totalCaloriesBurnedKcal = null,
        )

        assertEquals(
            listOf(null, null, null),
            listOf(point.totalActiveCaloriesKcal, point.totalFloorsClimbed, point.totalElevationGainedMeters),
        )
    }

    @Test fun `DashboardData optional readings default to null`() {
        val data = DashboardData(date = LocalDate.of(2026, 1, 1))

        assertEquals(
            listOf(null, null, null, null, null, null),
            listOf(
                data.weightKg,
                data.weightTime,
                data.heightCm,
                data.heightTime,
                data.floorsClimbed,
                data.elevationGainedMeters,
            ),
        )
    }

    // Helpers.

    private fun exercise(durationMs: Long) = ExerciseData(
        id = "1",
        title = null,
        exerciseType = 0,
        startTime = Instant.EPOCH,
        endTime = Instant.EPOCH,
        durationMs = durationMs,
        source = "test",
    )

    private fun sleep(durationMs: Long) = SleepData(
        id = "1",
        startTime = Instant.EPOCH,
        endTime = Instant.EPOCH,
        durationMs = durationMs,
        source = "test",
    )
}
