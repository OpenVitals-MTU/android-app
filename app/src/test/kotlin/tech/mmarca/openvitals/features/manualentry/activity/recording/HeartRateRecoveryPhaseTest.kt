package tech.mmarca.openvitals.features.manualentry.activity.recording

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateRecoveryPhaseTest {

    private val start: Instant = Instant.parse("2026-07-14T18:00:00Z")

    private fun state(
        phase: ActivityRecordingHrrPhase,
        effortEndedAt: Instant? = null,
        warmupSeconds: Int = 180,
        recoverySeconds: Int = 300,
    ) = ActivityRecordingState(
        status = ActivityRecordingStatus.RECORDING,
        recordingKind = ActivityRecordingKind.TIMED,
        startTime = start,
        hrrPhase = phase,
        hrrEffortEndedAt = effortEndedAt,
        hrrConfig = HeartRateRecoveryTestConfig(
            warmupSeconds = warmupSeconds,
            recoverySeconds = recoverySeconds,
        ),
    )

    @Test
    fun `the warmup counts down from the start of the recording`() {
        val s = state(phase = ActivityRecordingHrrPhase.WARMUP)

        assertEquals(
            Duration.ofSeconds(120),
            s.hrrPhaseRemaining(start.plusSeconds(60)),
        )
        assertTrue(s.isHeartRateRecoveryTest)
    }

    @Test
    fun `the effort has no deadline - it ends when the rider does`() {
        val s = state(phase = ActivityRecordingHrrPhase.EFFORT)

        // Nothing counts the effort down; the rider or their heart rate ends it.
        assertNull(s.hrrPhaseRemaining(start))
    }

    @Test
    fun `the recovery counts down from the instant effort stopped`() {
        val effortEnded = start.plusSeconds(600)
        val s = state(
            phase = ActivityRecordingHrrPhase.RECOVERY,
            effortEndedAt = effortEnded,
        )

        assertEquals(
            Duration.ofSeconds(210),
            s.hrrPhaseRemaining(effortEnded.plusSeconds(90)),
        )
    }

    @Test
    fun `a countdown never runs negative`() {
        val effortEnded = start.plusSeconds(600)
        val s = state(
            phase = ActivityRecordingHrrPhase.RECOVERY,
            effortEndedAt = effortEnded,
        )

        assertEquals(
            Duration.ZERO,
            s.hrrPhaseRemaining(effortEnded.plusSeconds(9 * 60L)),
        )
    }

    @Test
    fun `an ordinary recording is not a test`() {
        assertFalse(state(phase = ActivityRecordingHrrPhase.NONE).isHeartRateRecoveryTest)
    }

    @Test
    fun `a zero warmup has nothing to count down`() {
        val s = state(phase = ActivityRecordingHrrPhase.WARMUP, warmupSeconds = 0)

        assertNull(s.hrrPhaseRemaining(start))
    }

    @Test
    fun `countdown text formats as minutes and zero-padded seconds`() {
        assertEquals("4:30", hrrCountdownText(Duration.ofSeconds(270)))
        assertEquals("0:05", hrrCountdownText(Duration.ofSeconds(5)))
        assertEquals("10:00", hrrCountdownText(Duration.ofMinutes(10)))
    }
}
