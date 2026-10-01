package tech.mmarca.openvitals.features.manualentry.activity.recording

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BleRecordingMetrics

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

    @Test fun `the wheel sensor distance wins over GPS once it reports`() {
        val state = ActivityRecordingState(status = ActivityRecordingStatus.RECORDING, distanceMeters = 40.0)
        assertEquals(40.0, state.liveDistanceMeters, 0.0)

        val next = state.withBleMetrics(BleRecordingMetrics(cyclingDistanceMeters = 0.0), previousWheelMeters = null)
        assertEquals(0.0, next.liveDistanceMeters, 0.0)
    }

    @Test fun `wheel distance adds up only while recording`() {
        val recording = ActivityRecordingState(status = ActivityRecordingStatus.RECORDING, sensorDistanceMeters = 100.0)
        val wheel = BleRecordingMetrics(cyclingDistanceMeters = 530.0)

        assertEquals(130.0, recording.withBleMetrics(wheel, previousWheelMeters = 500.0).sensorDistanceMeters!!, 1e-9)
        // The first reading of a recording sets the baseline.
        assertEquals(100.0, recording.withBleMetrics(wheel, previousWheelMeters = null).sensorDistanceMeters!!, 1e-9)
        val paused = recording.copy(status = ActivityRecordingStatus.PAUSED)
        assertEquals(100.0, paused.withBleMetrics(wheel, previousWheelMeters = 500.0).sensorDistanceMeters!!, 1e-9)
        // No sensor, no sensor distance: GPS keeps it.
        val idle = ActivityRecordingState(status = ActivityRecordingStatus.RECORDING)
        assertNull(idle.withBleMetrics(BleRecordingMetrics(), previousWheelMeters = null).sensorDistanceMeters)
    }

    @Test fun `wheel distance gain survives a sensor reconnect`() {
        assertEquals(0.0, wheelDistanceGain(previous = null, current = 12.0), 0.0)
        assertEquals(4.2, wheelDistanceGain(previous = 10.0, current = 14.2), 1e-9)
        // The sensor total started again at 0.
        assertEquals(6.3, wheelDistanceGain(previous = 1_200.0, current = 6.3), 1e-9)
    }

    @Test fun `the speed sensor sets the top speed while recording`() {
        val state = ActivityRecordingState(status = ActivityRecordingStatus.RECORDING, maxSpeedMetersPerSecond = 8.0)

        assertEquals(11.0, state.withBleMetrics(BleRecordingMetrics(cyclingSpeedMetersPerSecond = 11.0), null).maxSpeedMetersPerSecond, 0.0)
        assertEquals(8.0, state.withBleMetrics(BleRecordingMetrics(cyclingSpeedMetersPerSecond = 6.0), null).maxSpeedMetersPerSecond, 0.0)
        // A glitch reading is not a top speed.
        assertEquals(8.0, state.withBleMetrics(BleRecordingMetrics(cyclingSpeedMetersPerSecond = 90.0), null).maxSpeedMetersPerSecond, 0.0)
        val paused = state.copy(status = ActivityRecordingStatus.PAUSED)
        assertEquals(8.0, paused.withBleMetrics(BleRecordingMetrics(cyclingSpeedMetersPerSecond = 11.0), null).maxSpeedMetersPerSecond, 0.0)
    }
}
