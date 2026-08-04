package tech.mmarca.openvitals.features.manualentry.activity.recording

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityRecordingStateTest {

    @Test fun `movingDuration excludes open auto idle time`() {
        val start = Instant.parse("2026-01-01T10:00:00Z")
        val state = ActivityRecordingState(
            status = ActivityRecordingStatus.RECORDING,
            startTime = start,
            autoIdleEnabled = true,
            autoIdleTimeoutMillis = 10_000L,
            lastMovementAt = start,
        )

        assertEquals(
            Duration.ofSeconds(10),
            state.movingDuration(start.plusSeconds(30)),
        )
    }

    @Test fun `movingDuration excludes manual pauses and auto idle`() {
        val start = Instant.parse("2026-01-01T10:00:00Z")
        val state = ActivityRecordingState(
            status = ActivityRecordingStatus.PAUSED,
            startTime = start,
            pausedStartedAt = start.plusSeconds(50),
            totalPausedMillis = 5_000L,
            autoIdleEnabled = true,
            autoIdleTimeoutMillis = 10_000L,
            lastMovementAt = start.plusSeconds(20),
            totalIdleMillis = 20_000L,
        )

        assertEquals(
            Duration.ofSeconds(15),
            state.movingDuration(start.plusSeconds(60)),
        )
    }

    @Test fun `repetition movingDuration excludes recorded and open rest time`() {
        val start = Instant.parse("2026-01-01T10:00:00Z")
        val now = start.plusSeconds(90)
        val state = ActivityRecordingState(
            status = ActivityRecordingStatus.RESTING,
            recordingKind = ActivityRecordingKind.REPETITION,
            startTime = start,
            accumulatedRestMillis = 20_000L,
            restStartedAt = start.plusSeconds(70),
            repetitionRestSeconds = 30L,
        )

        assertEquals(Duration.ofSeconds(40), state.restDuration(now))
        assertEquals(Duration.ofSeconds(50), state.movingDuration(now))
        assertEquals(
            Duration.ofSeconds(90),
            state.movingDuration(now).plus(state.restDuration(now)),
        )
    }

    @Test fun `effective speed is zero while idle or gps is poor`() {
        val start = Instant.parse("2026-01-01T10:00:00Z")
        val idleState = ActivityRecordingState(
            status = ActivityRecordingStatus.RECORDING,
            startTime = start,
            currentSpeedMetersPerSecond = 6.0,
            autoIdleEnabled = true,
            autoIdleTimeoutMillis = 10_000L,
            lastMovementAt = start,
            gpsStatus = ActivityGpsStatus.FIX,
        )
        val poorGpsState = idleState.copy(
            lastMovementAt = start.plusSeconds(20),
            gpsStatus = ActivityGpsStatus.POOR_ACCURACY,
        )

        assertEquals(0.0, idleState.effectiveCurrentSpeedMetersPerSecond(start.plusSeconds(20)), 0.0)
        assertEquals(0.0, poorGpsState.effectiveCurrentSpeedMetersPerSecond(start.plusSeconds(21)), 0.0)
        assertEquals(6.0, idleState.effectiveCurrentSpeedMetersPerSecond(start.plusSeconds(5)), 0.0)
    }
}
