package tech.mmarca.openvitals.features.manualentry.activity.recording

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import java.time.Duration
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.testing.testUnitFormatter
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Ports the portable half of the `recording screen` group of Flutter's
 * `test/features/manualentry/activity/recording/activity_recording_screen_test.dart`.
 *
 * The control bar is what a rider reaches for mid-effort, one-handed, without
 * reading. Offering Finish before a session has started ends a recording that
 * never happened; hiding Pause once it has started means the only way to stop
 * for a level crossing is to finish. Lap and Marker are guarded because neither
 * means anything before there is a position to hang them on.
 */
class ActivityRecordingControlsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun anIdleGpsSessionOffersStartAndCancelOnly() {
        setContent {
            GpsRecordingControls(
                state = ActivityRecordingState(
                    recordingKind = ActivityRecordingKind.GPS_ROUTE,
                    activityTypeId = "running",
                ),
                canStartRecording = true,
                onStartRecording = {},
                onPauseRecording = {},
                onResumeRecording = {},
                onEnterFocusMode = {},
                onFinishRecording = {},
                onAddLap = {},
                onAddMarker = {},
                onChooseSource = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.action_start)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_cancel)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_pause)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.action_finish)).assertDoesNotExist()
    }

    @Test
    fun aRunningGpsSessionShowsItsTabsPauseLapAndMarker() {
        // The tab row and the control bar are the two halves the recording
        // screen assembles; a lap with one point is a lap of nothing, and a
        // marker with no position has nowhere to sit on the map.
        setContent {
            ActivityRecordingTabRow(selectedTab = ActivityRecordingTab.STATS, onSelect = {})
            GpsRecordingControls(
                state = ActivityRecordingState(
                    recordingKind = ActivityRecordingKind.GPS_ROUTE,
                    activityTypeId = "running",
                    status = ActivityRecordingStatus.RECORDING,
                    startTime = START,
                ),
                canStartRecording = false,
                onStartRecording = {},
                onPauseRecording = {},
                onResumeRecording = {},
                onEnterFocusMode = {},
                onFinishRecording = {},
                onAddLap = {},
                onAddMarker = {},
                onChooseSource = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.activity_entry_recording_tab_stats)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_entry_recording_tab_map)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_entry_recording_tab_intervals)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_pause)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_entry_recording_focus)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_finish)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_entry_recording_lap))
            .performScrollTo()
            .assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.activity_entry_recording_marker))
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun aRepetitionSessionCountsRepsAndOffersEndSet() {
        // Focus mode is a clock and a distance read at arm's length; a set of
        // pull-ups has neither, so it is deliberately not offered here.
        setContent {
            RepetitionRecordingStats(
                state = repetitionState(count = 7L),
                totalTime = Duration.ofMinutes(2),
                movingTime = Duration.ofMinutes(2),
                unitFormatter = testUnitFormatter(),
                onAdjustRepetitionCount = {},
            )
            RepetitionRecordingControls(
                state = repetitionState(count = 7L),
                onEndRepetitionSet = {},
                onStartNextRepetitionSet = {},
                onFinishRecording = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.activity_entry_repetitions_title)).assertIsDisplayed()
        composeRule.onNodeWithText(testUnitFormatter().count(7L)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_entry_recording_end_set))
            .performScrollTo()
            .assertIsEnabled()
        composeRule.onNodeWithText(string(R.string.activity_entry_recording_end_session))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_entry_recording_focus)).assertDoesNotExist()
    }

    @Test
    fun aRepetitionSetCannotEndBeforeARepIsCounted() {
        // An empty set written into the workout is a set the user never did.
        setContent {
            RepetitionRecordingControls(
                state = repetitionState(count = 0L),
                onEndRepetitionSet = {},
                onStartNextRepetitionSet = {},
                onFinishRecording = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.activity_entry_recording_end_set)).assertIsNotEnabled()
    }

    private fun repetitionState(count: Long) = ActivityRecordingState(
        recordingKind = ActivityRecordingKind.REPETITION,
        activityTypeId = "pull_ups",
        status = ActivityRecordingStatus.RECORDING,
        startTime = START,
        currentSetRepetitionCount = count,
    )

    private fun setContent(content: @Composable () -> Unit) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { content() }
            }
        }
    }

    private companion object {
        val START: Instant = Instant.parse("2026-06-23T08:00:00Z")
    }
}
