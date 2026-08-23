package tech.mmarca.openvitals.features.manualentry.activity.recording

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.testing.testUnitFormatter
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Ports the whole-screen half of the `recording screen` group of Flutter's
 * `test/features/manualentry/activity/recording/activity_recording_screen_test.dart`
 * — the cases about what the screen offers as a whole, rather than what one
 * control bar renders (that is `ActivityRecordingControlsTest`).
 *
 * All three are about a screen someone is looking at mid-effort, one-handed,
 * often at arm's length. Rearranging the dashboard while a recording is running
 * would mean dragging tiles around with a thumb that is meant to be on the
 * handlebars, so the editor is offered only when the session is stopped.
 * Focus mode is the opposite promise: everything else goes away, and it has to
 * come back on the first tap or the rider is trapped in it.
 *
 * The screen owns none of that state — the host (`ActivityEntryScreen`) does,
 * so it can drop its app bar for focus mode and put the edit toggle in it.
 * These tests therefore drive the screen the way that host does: they hold the
 * flags and hand the screen the callbacks, which is the shape the app has.
 */
class ActivityRecordingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theDashboardEditToggleOnlyOffersItselfWhileIdleOrPaused() {
        // Reported to the host rather than rendered here: the toggle lives in
        // the app bar. Offering it mid-recording would invite a layout change
        // to the very numbers being recorded.
        val reportedCanEdit = mutableListOf<Boolean>()
        val status = mutableStateOf(ActivityRecordingStatus.RECORDING)
        composeRule.setContent {
            OpenVitalsTheme {
                ActivityRecordingScreen(
                    state = gpsState(status.value),
                    unitFormatter = testUnitFormatter(),
                    onStartRecording = {},
                    onPauseRecording = {},
                    onResumeRecording = {},
                    onAddLap = {},
                    onAddMarker = {},
                    onUpdateMarker = {},
                    onDeleteMarker = {},
                    onUpdateDashboardLayout = {},
                    onChooseSource = {},
                    onAdjustRepetitionCount = {},
                    onEndRepetitionSet = {},
                    onStartNextRepetitionSet = {},
                    onFinishRecording = {},
                    onDashboardEditStateChanged = { canEdit, _, _ -> reportedCanEdit += canEdit },
                )
            }
        }
        composeRule.waitForIdle()

        assertEquals(listOf(false), reportedCanEdit.distinct())

        composeRule.runOnIdle { status.value = ActivityRecordingStatus.PAUSED }
        composeRule.waitForIdle()

        assertTrue("a paused session may be rearranged", reportedCanEdit.last())
    }

    @Test
    fun editingTheDashboardSwapsTheTabsForTheAddFieldChips() {
        // The editor takes over the tab area rather than sitting under it: the
        // map and the split tables are not editable, and leaving them reachable
        // would let the user wander off mid-edit into a tab that ignores them.
        var toggleEdit: (() -> Unit)? = null
        composeRule.setContent {
            OpenVitalsTheme {
                ActivityRecordingScreen(
                    state = gpsState(ActivityRecordingStatus.PAUSED),
                    unitFormatter = testUnitFormatter(),
                    onStartRecording = {},
                    onPauseRecording = {},
                    onResumeRecording = {},
                    onAddLap = {},
                    onAddMarker = {},
                    onUpdateMarker = {},
                    onDeleteMarker = {},
                    onUpdateDashboardLayout = {},
                    onChooseSource = {},
                    onAdjustRepetitionCount = {},
                    onEndRepetitionSet = {},
                    onStartNextRepetitionSet = {},
                    onFinishRecording = {},
                    onDashboardEditStateChanged = { _, _, toggle -> toggleEdit = toggle },
                )
            }
        }

        val addField = string(R.string.activity_entry_recording_dashboard_add_field)
        val mapTab = string(R.string.activity_entry_recording_tab_map)
        composeRule.onNodeWithText(addField).assertDoesNotExist()
        composeRule.onNodeWithText(mapTab).assertExists()

        composeRule.runOnIdle { requireNotNull(toggleEdit).invoke() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(addField).assertExists()
        composeRule.onNodeWithText(mapTab).assertDoesNotExist()
    }

    @Test
    fun focusEntersAndExitsFullScreenMode() {
        composeRule.setContent {
            OpenVitalsTheme {
                var focusMode by remember { mutableStateOf(false) }
                ActivityRecordingScreen(
                    state = gpsState(ActivityRecordingStatus.RECORDING),
                    unitFormatter = testUnitFormatter(),
                    onStartRecording = {},
                    onPauseRecording = {},
                    onResumeRecording = {},
                    onAddLap = {},
                    onAddMarker = {},
                    onUpdateMarker = {},
                    onDeleteMarker = {},
                    onUpdateDashboardLayout = {},
                    onChooseSource = {},
                    onAdjustRepetitionCount = {},
                    onEndRepetitionSet = {},
                    onStartNextRepetitionSet = {},
                    onFinishRecording = {},
                    isFocusMode = focusMode,
                    onFocusModeChanged = { focusMode = it },
                )
            }
        }

        val statsTab = string(R.string.activity_entry_recording_tab_stats)
        val exitFocus = string(R.string.cd_exit_recording_focus_mode)
        composeRule.onNodeWithText(statsTab).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.activity_entry_recording_focus)).performClick()
        composeRule.waitForIdle()

        // Focus mode replaces the tabs with the clock, the dashboard grid and
        // one full-width Pause.
        composeRule.onNodeWithText(statsTab).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(exitFocus).assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(string(R.string.cd_toggle_recording_outdoor_mode))
            .assertIsDisplayed()

        // The way out has to be one tap, from inside a mode that hides the
        // system bars and the app bar with them.
        composeRule.onNodeWithContentDescription(exitFocus).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(statsTab).assertIsDisplayed()
    }

    @Test
    fun theOutdoorToggleInFocusModeHandsTheChoiceBackToTheHost() {
        // Outdoor mode survives leaving focus mode, so the screen must not keep
        // it: it reports the change and the host holds it. A toggle that only
        // flipped a local flag would snap back to normal contrast the moment
        // the rider left focus mode, in the same sunlight.
        val reported = mutableListOf<Boolean>()
        composeRule.setContent {
            OpenVitalsTheme {
                var outdoor by remember { mutableStateOf(false) }
                ActivityRecordingScreen(
                    state = gpsState(ActivityRecordingStatus.RECORDING),
                    unitFormatter = testUnitFormatter(),
                    onStartRecording = {},
                    onPauseRecording = {},
                    onResumeRecording = {},
                    onAddLap = {},
                    onAddMarker = {},
                    onUpdateMarker = {},
                    onDeleteMarker = {},
                    onUpdateDashboardLayout = {},
                    onChooseSource = {},
                    onAdjustRepetitionCount = {},
                    onEndRepetitionSet = {},
                    onStartNextRepetitionSet = {},
                    onFinishRecording = {},
                    isFocusMode = true,
                    isOutdoorMode = outdoor,
                    onOutdoorModeChanged = {
                        outdoor = it
                        reported += it
                    },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(string(R.string.cd_toggle_recording_outdoor_mode))
            .performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(true), reported)
    }

    @Test
    fun aRepetitionRecordingIsNeverLeftInFocusMode() {
        // `canUseFocusMode` excludes repetitions: focus mode is a clock and a
        // distance read at arm's length, and a set of pull-ups has neither. A
        // host that asked for it anyway must be corrected rather than obeyed,
        // or the counter and its +/- would be off screen mid-set.
        val reported = mutableListOf<Boolean>()
        composeRule.setContent {
            OpenVitalsTheme {
                var focusMode by remember { mutableStateOf(true) }
                ActivityRecordingScreen(
                    state = ActivityRecordingState(
                        recordingKind = ActivityRecordingKind.REPETITION,
                        activityTypeId = "pull_ups",
                        status = ActivityRecordingStatus.RECORDING,
                        startTime = START,
                        currentSetRepetitionCount = 7L,
                    ),
                    unitFormatter = testUnitFormatter(),
                    onStartRecording = {},
                    onPauseRecording = {},
                    onResumeRecording = {},
                    onAddLap = {},
                    onAddMarker = {},
                    onUpdateMarker = {},
                    onDeleteMarker = {},
                    onUpdateDashboardLayout = {},
                    onChooseSource = {},
                    onAdjustRepetitionCount = {},
                    onEndRepetitionSet = {},
                    onStartNextRepetitionSet = {},
                    onFinishRecording = {},
                    isFocusMode = focusMode,
                    onFocusModeChanged = {
                        focusMode = it
                        reported += it
                    },
                )
            }
        }
        composeRule.waitForIdle()

        assertEquals(listOf(false), reported)
        composeRule
            .onNodeWithContentDescription(string(R.string.cd_exit_recording_focus_mode))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.activity_entry_recording_end_set)).assertExists()
    }

    /**
     * A GPS session that is under way, so the pre-start fix poller stays off.
     *
     * Only the IDLE branch of the screen asks the device where it is; every
     * case here is about a session that has already started, which is what
     * keeps these tests off the real GPS.
     */
    @Test
    fun `dismissing the CoMaps card before starting keeps it dismissed once riding`() {
        // Setting a ride up and riding it is one continuous act. The dismissal used to be
        // keyed on the recording's start time, so pressing Start reset it and the card the
        // user had just swatted away came straight back, needing a second dismissal with
        // the ride already running.
        val status = mutableStateOf(ActivityRecordingStatus.IDLE)
        composeRule.setContent {
            OpenVitalsTheme {
                ActivityRecordingScreen(
                    state = gpsState(status.value),
                    unitFormatter = testUnitFormatter(),
                    onStartRecording = {},
                    onPauseRecording = {},
                    onResumeRecording = {},
                    onAddLap = {},
                    onAddMarker = {},
                    onUpdateMarker = {},
                    onDeleteMarker = {},
                    onUpdateDashboardLayout = {},
                    onChooseSource = {},
                    onAdjustRepetitionCount = {},
                    onEndRepetitionSet = {},
                    onStartNextRepetitionSet = {},
                    onFinishRecording = {},
                    coMapsNavigation = CoMapsNavigationState.NotNavigating,
                )
            }
        }

        val guidanceTitle = string(R.string.recording_comaps_title)
        composeRule.onNodeWithText(guidanceTitle).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(string(R.string.action_close)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(guidanceTitle).assertDoesNotExist()

        composeRule.runOnIdle { status.value = ActivityRecordingStatus.RECORDING }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(guidanceTitle).assertDoesNotExist()
    }

    @Test
    fun `the tab row is the first thing on the screen, above any guidance card`() {
        // The guidance card is about one tab's content and can run tall; above the tabs it
        // pushed the way between them down the screen.
        composeRule.setContent {
            OpenVitalsTheme {
                ActivityRecordingScreen(
                    state = gpsState(ActivityRecordingStatus.IDLE),
                    unitFormatter = testUnitFormatter(),
                    onStartRecording = {},
                    onPauseRecording = {},
                    onResumeRecording = {},
                    onAddLap = {},
                    onAddMarker = {},
                    onUpdateMarker = {},
                    onDeleteMarker = {},
                    onUpdateDashboardLayout = {},
                    onChooseSource = {},
                    onAdjustRepetitionCount = {},
                    onEndRepetitionSet = {},
                    onStartNextRepetitionSet = {},
                    onFinishRecording = {},
                    coMapsNavigation = CoMapsNavigationState.NotNavigating,
                )
            }
        }

        // Guidance lives on the map tab, so that is where the two can be compared.
        composeRule.onNodeWithText(string(R.string.activity_entry_recording_tab_map)).performClick()
        composeRule.waitForIdle()

        val tabTop = composeRule
            .onNodeWithText(string(R.string.activity_entry_recording_tab_map))
            .fetchSemanticsNode()
            .positionInRoot
            .y
        val guidanceTop = composeRule
            .onNodeWithText(string(R.string.recording_comaps_title))
            .fetchSemanticsNode()
            .positionInRoot
            .y

        assertTrue("tabs at $tabTop must sit above the guidance card at $guidanceTop", tabTop < guidanceTop)
    }

    @Test
    fun `starting with no route set asks before it starts, and starts when answered`() {
        // Start is a question here, not a refusal. It used to raise a toast telling the user to
        // dismiss the guidance card -- an instruction they could not follow from any tab but the
        // map, since that is the only place the card lives.
        var started = 0
        composeRule.setContent {
            OpenVitalsTheme {
                ActivityRecordingScreen(
                    state = gpsState(ActivityRecordingStatus.IDLE),
                    unitFormatter = testUnitFormatter(),
                    onStartRecording = { started += 1 },
                    onPauseRecording = {},
                    onResumeRecording = {},
                    onAddLap = {},
                    onAddMarker = {},
                    onUpdateMarker = {},
                    onDeleteMarker = {},
                    onUpdateDashboardLayout = {},
                    onChooseSource = {},
                    onAdjustRepetitionCount = {},
                    onEndRepetitionSet = {},
                    onStartNextRepetitionSet = {},
                    onFinishRecording = {},
                    coMapsNavigation = CoMapsNavigationState.NotNavigating,
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.action_start)).performClick()
        composeRule.waitForIdle()

        // The question, and nothing recording behind it.
        composeRule.onNodeWithText(string(R.string.recording_comaps_no_route_title)).assertIsDisplayed()
        assertEquals(0, started)

        composeRule.onNodeWithText(string(R.string.recording_comaps_no_route_confirm)).performClick()
        composeRule.waitForIdle()

        assertEquals(1, started)
        composeRule.onNodeWithText(string(R.string.recording_comaps_no_route_title)).assertDoesNotExist()
    }

    @Test
    fun `a route already being followed starts on the first press`() {
        // The gate is about the absence of a route. With CoMaps guiding there is nothing to ask.
        var started = 0
        composeRule.setContent {
            OpenVitalsTheme {
                ActivityRecordingScreen(
                    state = gpsState(ActivityRecordingStatus.IDLE),
                    unitFormatter = testUnitFormatter(),
                    onStartRecording = { started += 1 },
                    onPauseRecording = {},
                    onResumeRecording = {},
                    onAddLap = {},
                    onAddMarker = {},
                    onUpdateMarker = {},
                    onDeleteMarker = {},
                    onUpdateDashboardLayout = {},
                    onChooseSource = {},
                    onAdjustRepetitionCount = {},
                    onEndRepetitionSet = {},
                    onStartNextRepetitionSet = {},
                    onFinishRecording = {},
                    coMapsNavigation = CoMapsNavigationState.Disabled,
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.action_start)).performClick()
        composeRule.waitForIdle()

        assertEquals(1, started)
        composeRule.onNodeWithText(string(R.string.recording_comaps_no_route_title)).assertDoesNotExist()
    }

    private fun gpsState(status: ActivityRecordingStatus) = ActivityRecordingState(
        recordingKind = ActivityRecordingKind.GPS_ROUTE,
        activityTypeId = "running",
        status = status,
        startTime = START,
        pausedStartedAt = if (status == ActivityRecordingStatus.PAUSED) PAUSED_AT else null,
    )

    private companion object {
        /** Fixed, so nothing here depends on when the suite runs. */
        val START: Instant = Instant.parse("2026-06-23T08:00:00Z")
        val PAUSED_AT: Instant = Instant.parse("2026-06-23T08:05:00Z")
    }
}
