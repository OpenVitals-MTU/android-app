package tech.mmarca.openvitals.features.manualentry.mindfulness

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of the rendering case of Flutter's
 * `test/features/manualentry/mindfulness_entry_screen_test.dart`.
 *
 * The mindfulness entry screen offers two ways to record a session — sit with
 * the timer now, or type in minutes you already sat. They are not
 * interchangeable, and the sound pickers are the only reason to choose the
 * timer at all, so all three have to be on the screen together.
 */
class MindfulnessEntryContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTheTimerItsSoundPickersAndTheManualCard() {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    MindfulnessTimerCard(
                        state = STATE,
                        onDurationChanged = {},
                        onIntervalEnabledChanged = {},
                        onIntervalChanged = {},
                        onBellSoundChanged = {},
                        onBackgroundSoundChanged = {},
                        onStartTimer = {},
                        onStopTimer = {},
                        onResumeTimer = {},
                        onSaveTimerSession = {},
                        onDiscardTimer = {},
                        onRequestWritePermission = {},
                    )
                    MindfulnessManualEntryCard(
                        state = STATE,
                        onMinutesChanged = {},
                        onEntryStartTimeChanged = {},
                        onAddEntry = {},
                        onRequestWritePermission = {},
                    )
                }
            }
        }

        listOf(
            R.string.mindfulness_entry_timer_title,
            R.string.mindfulness_entry_interval_bell,
            R.string.mindfulness_entry_bell_sound,
            R.string.mindfulness_entry_background_sound,
            R.string.mindfulness_entry_start_timer,
            R.string.mindfulness_entry_manual_title,
        ).forEach { titleRes ->
            composeRule.onNodeWithText(string(titleRes)).performScrollTo().assertIsDisplayed()
        }
    }

    private companion object {
        val STATE = MindfulnessEntryUiState(
            durationMinutesText = "10",
            manualMinutesText = "15",
            isCheckingPermission = false,
            canWrite = true,
        )
    }
}
