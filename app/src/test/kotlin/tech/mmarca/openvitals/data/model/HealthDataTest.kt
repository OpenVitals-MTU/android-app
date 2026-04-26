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

    // ─── SleepData.durationFormatted ──────────────────────────────────────────

    @Test fun `durationFormatted formats hours and minutes`() {
        assertEquals("7h 30m", sleep(durationMs = 27_000_000L).durationFormatted)
    }

    @Test fun `durationFormatted shows zero minutes when exact hours`() {
        assertEquals("8h 0m", sleep(durationMs = 28_800_000L).durationFormatted)
    }

    @Test fun `durationFormatted shows zero hours for sub-hour duration`() {
        assertEquals("0h 45m", sleep(durationMs = 2_700_000L).durationFormatted)
    }

    @Test fun `durationFormatted handles midnight boundary`() {
        assertEquals("0h 0m", sleep(durationMs = 0L).durationFormatted)
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

    // ─── SleepStage.stageLabel ────────────────────────────────────────────────

    @Test fun `stageLabel returns correct label for every known type`() {
        assertEquals("Awake", SleepStage.stageLabel(SleepStage.STAGE_AWAKE))
        assertEquals("Sleeping", SleepStage.stageLabel(SleepStage.STAGE_SLEEPING))
        assertEquals("Out of bed", SleepStage.stageLabel(SleepStage.STAGE_OUT_OF_BED))
        assertEquals("Light", SleepStage.stageLabel(SleepStage.STAGE_LIGHT))
        assertEquals("Deep", SleepStage.stageLabel(SleepStage.STAGE_DEEP))
        assertEquals("REM", SleepStage.stageLabel(SleepStage.STAGE_REM))
        assertEquals("Awake in bed", SleepStage.stageLabel(SleepStage.STAGE_AWAKE_IN_BED))
        assertEquals("Unknown", SleepStage.stageLabel(SleepStage.STAGE_UNKNOWN))
    }

    @Test fun `stageLabel returns Unknown for unrecognized type`() {
        assertEquals("Unknown", SleepStage.stageLabel(99))
        assertEquals("Unknown", SleepStage.stageLabel(-1))
    }

    // ─── DataSource.displayName ───────────────────────────────────────────────

    @Test fun `displayName recognizes all known app package names`() {
        assertEquals("Samsung Health", DataSource("com.samsung.health", null, null).displayName)
        assertEquals("Fitbit", DataSource("com.fitbit.FitbitMobile", null, null).displayName)
        assertEquals("OpenTracks", DataSource("de.dennisguse.opentracks", null, null).displayName)
        assertEquals("Strava", DataSource("com.strava", null, null).displayName)
        assertEquals("Garmin Connect", DataSource("com.garmin.android.apps.connectmobile", null, null).displayName)
        assertEquals("Polar Flow", DataSource("com.polar.flow", null, null).displayName)
        assertEquals("Google Fit", DataSource("com.google.android.apps.fitness", null, null).displayName)
    }

    @Test fun `displayName falls back to capitalized last package segment`() {
        assertEquals("Myapp", DataSource("com.example.myapp", null, null).displayName)
        assertEquals("Tracker", DataSource("dev.tracker", null, null).displayName)
    }

    @Test fun `displayName capitalizes single-segment package`() {
        assertEquals("Health", DataSource("health", null, null).displayName)
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

    // ─── DashboardData floorsClimbed + elevationGainedMeters ────────────────────

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
