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
 * The ask-versus-enable decision lives in the switch's `onCheckedChange`, so the card is
 * the only place to observe it. The OS round trip itself is out of reach in a test.
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

        // Once the permission is there the same flip enables the reminder.
        composeRule.runOnIdle { hasPermission = true }
        composeRule.onNode(isToggleable()).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("it must not ask twice", 1, requested)
        assertEquals(true, toggled)
    }

    @Test
    fun aDeniedPermissionLeavesTheReminderOffRatherThanSilentlyDead() {
        // An enabled reminder with no permission never arrives. Refusing the flip keeps the switch honest.
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
