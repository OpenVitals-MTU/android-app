package tech.mmarca.openvitals.data.model

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HealthDataTest {

    // ─── ExerciseData.durationMinutes ─────────────────────────────────────────

    @Test fun `durationMinutes truncates sub-minute remainder`() {
        assertEquals(1L, exercise(durationMs = 90_000L).durationMinutes)
    }

    @Test fun `durationMinutes is zero for sub-minute duration`() {
        assertEquals(0L, exercise(durationMs = 59_999L).durationMinutes)
    }

    @Test fun `durationMinutes is exact for whole-minute duration`() {
        assertEquals(60L, exercise(durationMs = 3_600_000L).durationMinutes)
    }

    // ─── SleepData.durationHours ──────────────────────────────────────────────

    @Test fun `durationHours returns fractional hours`() {
        assertEquals(7.5, sleep(durationMs = 27_000_000L).durationHours, 0.001)
    }

    @Test fun `durationHours is zero for zero duration`() {
        assertEquals(0.0, sleep(durationMs = 0L).durationHours, 0.0)
    }

    // ─── SleepStage.durationMs ────────────────────────────────────────────────

    @Test fun `SleepStage durationMs equals end minus start epoch millis`() {
        val stage = SleepStage(
            startTime = Instant.ofEpochMilli(1_000_000L),
            endTime = Instant.ofEpochMilli(2_500_000L),
            stageType = SleepStage.STAGE_REM,
        )
        assertEquals(1_500_000L, stage.durationMs)
    }

    // ─── DailySteps optional A3 fields ───────────────────────────────────────

    @Test fun `DailySteps defaults all optional fields to null`() {
        val day = DailySteps(date = LocalDate.of(2026, 1, 1), steps = 1_000L, distanceMeters = 800.0)
        assertNull(day.floorsClimbed)
        assertNull(day.activeCaloriesKcal)
        assertNull(day.elevationGainedMeters)
    }

    @Test fun `DailySteps stores all optional fields when provided`() {
        val day = DailySteps(
            date = LocalDate.of(2026, 1, 1),
            steps = 10_000L,
            distanceMeters = 7_500.0,
            floorsClimbed = 15,
            activeCaloriesKcal = 420.5,
            elevationGainedMeters = 65.0,
        )
        assertEquals(15, day.floorsClimbed)
        assertEquals(420.5, day.activeCaloriesKcal!!, 0.01)
        assertEquals(65.0, day.elevationGainedMeters!!, 0.01)
    }

    // ─── ActivityProgressPoint optional fields ───────────────────────────────

    @Test fun `ActivityProgressPoint defaults detailed optional fields to null`() {
        val point = ActivityProgressPoint(
            time = Instant.EPOCH,
            totalSteps = 1_000L,
            totalDistanceMeters = null,
            totalCaloriesBurnedKcal = null,
        )

        assertNull(point.totalActiveCaloriesKcal)
        assertNull(point.totalFloorsClimbed)
        assertNull(point.totalElevationGainedMeters)
    }

    @Test fun `ActivityProgressPoint stores detailed optional fields`() {
        val point = ActivityProgressPoint(
            time = Instant.EPOCH,
            totalSteps = 1_000L,
            totalDistanceMeters = 800.0,
            totalCaloriesBurnedKcal = 120.0,
            totalActiveCaloriesKcal = 80.0,
            totalFloorsClimbed = 4,
            totalElevationGainedMeters = 20.0,
        )

        assertEquals(80.0, point.totalActiveCaloriesKcal!!, 0.01)
        assertEquals(4, point.totalFloorsClimbed)
        assertEquals(20.0, point.totalElevationGainedMeters!!, 0.01)
    }

    // ─── DashboardData floorsClimbed + elevationGainedMeters ────────────────────

    @Test fun `DashboardData defaults weight to null`() {
        val data = DashboardData(date = LocalDate.of(2026, 1, 1))
        assertNull(data.weightKg)
        assertNull(data.weightTime)
        assertNull(data.heightTime)
    }

    @Test fun `DashboardData stores latest weight with time when provided`() {
        val time = Instant.parse("2026-01-01T08:00:00Z")
        val data = DashboardData(date = LocalDate.of(2026, 1, 1), weightKg = 74.2, weightTime = time)
        assertEquals(74.2, data.weightKg!!, 0.01)
        assertEquals(time, data.weightTime)
    }

    @Test fun `DashboardData stores latest height with time when provided`() {
        val time = Instant.parse("2026-01-02T08:00:00Z")
        val data = DashboardData(date = LocalDate.of(2026, 1, 1), heightCm = 178.0, heightTime = time)
        assertEquals(178.0, data.heightCm!!, 0.01)
        assertEquals(time, data.heightTime)
    }

    @Test fun `DashboardData defaults floorsClimbed to null`() {
        val data = DashboardData(date = LocalDate.of(2026, 1, 1))
        assertNull(data.floorsClimbed)
    }

    @Test fun `DashboardData stores floorsClimbed when provided`() {
        val data = DashboardData(date = LocalDate.of(2026, 1, 1), floorsClimbed = 8)
        assertEquals(8, data.floorsClimbed)
    }

    @Test fun `DashboardData defaults elevationGainedMeters to null`() {
        val data = DashboardData(date = LocalDate.of(2026, 1, 1))
        assertNull(data.elevationGainedMeters)
    }

    @Test fun `DashboardData stores elevationGainedMeters when provided`() {
        val data = DashboardData(date = LocalDate.of(2026, 1, 1), elevationGainedMeters = 120.0)
        assertEquals(120.0, data.elevationGainedMeters!!, 0.01)
    }

    @Test fun `DailySteps floorsClimbed zero is non-null — permission granted no data`() {
        val day = DailySteps(date = LocalDate.of(2026, 1, 1), steps = 0L, distanceMeters = 0.0, floorsClimbed = 0)
        assertEquals(0, day.floorsClimbed)
    }

    @Test fun `DailySteps elevationGainedMeters zero is non-null — permission granted no data`() {
        val day = DailySteps(date = LocalDate.of(2026, 1, 1), steps = 0L, distanceMeters = 0.0, elevationGainedMeters = 0.0)
        assertEquals(0.0, day.elevationGainedMeters!!, 0.0)
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

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
