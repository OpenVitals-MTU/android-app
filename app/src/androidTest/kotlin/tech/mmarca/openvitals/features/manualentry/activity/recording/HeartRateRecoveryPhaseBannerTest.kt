package tech.mmarca.openvitals.features.manualentry.activity.recording

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * During a recovery test the banner is what matters. Losing End effort strands a rider whose
 * legs give out first; showing it during recovery would move the measurement instant.
 */
class HeartRateRecoveryPhaseBannerTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theEffortCanAlwaysBeEndedByHand() {
        // The target heart rate is a convenience. The rider must still be able to stop abruptly.
        var ended = 0
        setBanner(
            state = state(phase = ActivityRecordingHrrPhase.EFFORT, heartRateBpm = 176L),
            now = START.plus(Duration.ofMinutes(5)),
            onEndEffort = { ended++ },
        )

        composeRule.onNodeWithText(string(R.string.activity_recording_hrr_phase_effort)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_recording_hrr_current_bpm, 176)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_recording_hrr_end_effort)).performClick()

        assertEquals(1, ended)
    }

    @Test
    fun duringTheRecoveryThereIsNothingToPressOnlyToKeepStill() {
        val effortEnded = START.plus(Duration.ofMinutes(10))
        setBanner(
            state = state(phase = ActivityRecordingHrrPhase.RECOVERY, effortEndedAt = effortEnded),
            now = effortEnded.plus(Duration.ofSeconds(30)),
        )

        composeRule.onNodeWithText(string(R.string.activity_recording_hrr_end_effort)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.activity_recording_hrr_phase_recovery)).assertIsDisplayed()
        // Five minutes of recovery, thirty seconds in.
        composeRule.onNodeWithText("4:30").assertIsDisplayed()
    }

    private fun state(
        phase: ActivityRecordingHrrPhase,
        effortEndedAt: Instant? = null,
        heartRateBpm: Long? = null,
    ) = ActivityRecordingState(
        status = ActivityRecordingStatus.RECORDING,
        recordingKind = ActivityRecordingKind.TIMED,
        startTime = START,
        hrrPhase = phase,
        hrrEffortEndedAt = effortEndedAt,
        currentHeartRateBpm = heartRateBpm,
        hrrConfig = HeartRateRecoveryTestConfig(warmupSeconds = 180, recoverySeconds = 300),
    )

    private fun setBanner(
        state: ActivityRecordingState,
        now: Instant,
        onEndEffort: () -> Unit = {},
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier) {
                    ActivityHeartRateRecoveryPhaseBanner(
                        state = state,
                        now = now,
                        onEndEffort = onEndEffort,
                    )
                }
            }
        }
    }

    private companion object {
        val START: Instant = Instant.parse("2026-07-14T18:00:00Z")
    }
}
