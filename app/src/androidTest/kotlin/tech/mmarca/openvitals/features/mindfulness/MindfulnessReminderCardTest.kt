package tech.mmarca.openvitals.features.mindfulness

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.model.MindfulnessReminderConfig
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Ports the permission cases of Flutter's
 * `test/features/mindfulness/reminders/mindfulness_reminder_settings_view_model_test.dart`.
 *
 * Kotlin has no reminder-settings view model to point them at: the ask-versus-
 * enable decision lives in the switch's own `onCheckedChange`, so the card is
 * the only place it can be observed — the same route
 * `HydrationReminderCardTest` takes for the hydration half of this behaviour.
 *
 * The half that stays out of reach is the OS round trip itself: the launcher and
 * the `enableRemindersAfterPermission` flag live in `MindfulnessScreen`, and a
 * test cannot grant `POST_NOTIFICATIONS` to its own process without restarting
 * it. What the card owns is the decision either side of that trip, and that is
 * what these pin.
 */
class MindfulnessReminderCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun enablingWithoutPermissionAsksFirstAndEnablesOnceItIsGranted() {
        var requested = 0
        var toggled: Boolean? = null
        var hasPermission by mutableStateOf(false)
        setCard(
            config = MindfulnessReminderConfig(enabled = false),
            hasPermission = { hasPermission },
            onRequestPermission = { requested++ },
            onToggle = { toggled = it },
        )

        composeRule.onNode(isToggleable()).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("flipping it on must ask for the permission", 1, requested)
        assertNull("and must not store a reminder that can never fire", toggled)

        // Once the permission is there the same flip enables the reminder
        // instead of asking again.
        composeRule.runOnIdle { hasPermission = true }
        composeRule.onNode(isToggleable()).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("it must not ask twice", 1, requested)
        assertEquals(true, toggled)
    }

    @Test
    fun aDeniedPermissionLeavesTheReminderOffRatherThanSilentlyDead() {
        // The failure this guards against is silent: an enabled reminder with no
        // notification permission never arrives, and the card would still read
        // as switched on. Refusing the flip keeps the switch honest.
        var toggled: Boolean? = null
        setCard(
            config = MindfulnessReminderConfig(enabled = false),
            hasPermission = { false },
            onToggle = { toggled = it },
        )

        composeRule.onNode(isToggleable()).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertNull(toggled)
        composeRule.onNode(isToggleable()).assertIsOff()
        composeRule
            .onNodeWithText(string(R.string.mindfulness_reminders_summary_off))
            .assertIsDisplayed()
    }

    private fun setCard(
        config: MindfulnessReminderConfig,
        hasPermission: () -> Boolean,
        onRequestPermission: () -> Unit = {},
        onToggle: (Boolean) -> Unit = {},
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    MindfulnessReminderCard(
                        config = config,
                        hasNotificationPermission = hasPermission(),
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        onToggleReminders = onToggle,
                        onRequestNotificationPermission = onRequestPermission,
                        onSelectReminderTime = {},
                    )
                }
            }
        }
    }
}
