package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Both cards are long stacks of controls: a control that stops rendering is a setting the
 * user cannot reach, and one that stays live under an off switch appears to do nothing.
 */
class ActivityRecordingAndCaffeineCardsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun activityRecordingCard_rendersTheIntroAndEverySubControl() {
        setCard {
            ActivityRecordingPreferencesCard(
                preferences = ActivityRecordingPreferences(),
                onChange = {},
            )
        }

        listOf(
            R.string.settings_activity_recording_title,
            // Switches.
            R.string.settings_activity_recording_keep_screen_on_title,
            R.string.settings_activity_recording_auto_idle_title,
            R.string.settings_activity_recording_barometer_title,
            R.string.settings_activity_recording_rest_bell_title,
            R.string.settings_activity_recording_voice_title,
            R.string.settings_activity_recording_voice_idle_title,
            R.string.settings_activity_recording_voice_lap_title,
            // Segmented choices.
            R.string.settings_activity_recording_idle_timeout_title,
            R.string.settings_activity_recording_accuracy_title,
            R.string.settings_activity_recording_route_gap_title,
            R.string.settings_activity_recording_time_interval_title,
            R.string.settings_activity_recording_distance_interval_title,
            R.string.settings_activity_recording_voice_time_title,
            R.string.settings_activity_recording_voice_distance_title,
        ).forEach { titleRes ->
            composeRule.onNodeWithText(string(titleRes))
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun activityRecordingCard_idleTimeoutIsDeadWhileAutoIdleIsOff() {
        // With auto-idle off the timeout must read as unavailable, and tapping it must not rewrite the stored value.
        var changed: ActivityRecordingPreferences? = null
        setCard {
            ActivityRecordingPreferencesCard(
                preferences = ActivityRecordingPreferences(autoIdleEnabled = false),
                onChange = { changed = it },
            )
        }

        val sixtySeconds = string(R.string.settings_activity_recording_seconds, 60)
        composeRule.onNodeWithText(sixtySeconds)
            .performScrollTo()
            .assertIsNotEnabled()
        composeRule.onNodeWithText(sixtySeconds).performClick()

        composeRule.runOnIdle { assertNull(changed) }
    }

    @Test
    fun caffeineCard_seedsItsFieldsFromTheStoredPreferences() {
        // The card edits a draft of what is stored, or a tuned half-life is discarded on Save.
        var saved: CaffeinePreferences? = null
        val stored = CaffeinePreferences(halfLifeMinutes = 420)
        setCard {
            CaffeinePreferencesCard(
                preferences = stored,
                bodyProfile = BodyProfile(),
                onSave = { saved = it },
            )
        }

        listOf(
            R.string.settings_caffeine_title,
            R.string.caffeine_pref_half_life,
            R.string.caffeine_pref_absorption,
            R.string.caffeine_sleep_threshold,
            R.string.caffeine_pref_bedtime,
        ).forEach { labelRes ->
            composeRule.onNodeWithText(string(labelRes)).performScrollTo().assertIsDisplayed()
        }
        // The stored, non-default half-life — not the model default.
        composeRule.onNodeWithText("420").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(420, saved?.halfLifeMinutes)
            // Saving from here is what marks the caffeine profile complete.
            assertEquals(true, saved?.profileCompleted)
        }
    }

    private fun setCard(content: @Composable () -> Unit) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { content() }
            }
        }
    }
}
