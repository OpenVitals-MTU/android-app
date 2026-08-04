package tech.mmarca.openvitals.features.hydration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.model.HydrationReminderConfig
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of Flutter's
 * `test/features/hydration/reminders/hydration_reminder_card_test.dart`.
 *
 * The card is the whole permission flow for reminders — Kotlin has no
 * reminder-settings view model, so the ask-versus-enable decision lives in the
 * switch's own lambda and cannot be reached any other way. That decision is
 * what these pin: flipping the switch on without notification permission has to
 * ask for it rather than storing an enabled reminder that will never fire.
 */
class HydrationReminderCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun offShowsOnlyTheSwitchAndTheOffSummary() {
        setCard(config = HydrationReminderConfig(enabled = false))

        composeRule
            .onNodeWithText(string(R.string.hydration_reminders_summary_off))
            .assertIsDisplayed()
        // No interval or window controls until it is actually on.
        composeRule.onNodeWithText(string(R.string.action_grant_permission)).assertDoesNotExist()
    }

    @Test
    fun onShowsTheIntervalAndTheWindow() {
        val config = HydrationReminderConfig(
            enabled = true,
            intervalMinutes = 120,
            activeStartTime = LocalTime.of(7, 0),
            activeEndTime = LocalTime.of(22, 0),
        )
        setCard(config = config)

        val formatter = DateTimeFormatterProvider().shortTime()
        composeRule
            .onNodeWithText(
                string(
                    R.string.hydration_reminders_summary_on,
                    120,
                    formatter.format(LocalTime.of(7, 0)),
                    formatter.format(LocalTime.of(22, 0)),
                ),
            )
            .assertIsDisplayed()
    }

    @Test
    fun blockedByPermissionWarnsAndOffersToGrant() {
        // An enabled reminder with no notification permission is a reminder
        // that silently never arrives. Saying so, and offering the grant, is
        // the difference between a broken feature and an unfinished setup.
        var requested = 0
        setCard(
            config = HydrationReminderConfig(enabled = true),
            hasPermission = false,
            onRequestPermission = { requested++ },
        )

        composeRule
            .onNodeWithText(string(R.string.hydration_reminders_permission_needed))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.action_grant_permission))
            .performScrollTo()
            .performClick()

        assertEquals(1, requested)
    }

    @Test
    fun flippingTheSwitchOnWithoutPermissionAsksInsteadOfEnabling() {
        var requested = 0
        var toggled: Boolean? = null
        setCard(
            config = HydrationReminderConfig(enabled = false),
            hasPermission = false,
            onRequestPermission = { requested++ },
            onToggle = { toggled = it },
        )

        composeRule.onNodeWithText(string(R.string.hydration_reminders_title)).performScrollTo()
        composeRule.onNode(
            androidx.compose.ui.test.isToggleable(),
        ).performClick()

        assertEquals("it must ask for permission", 1, requested)
        assertEquals("and must not store an enabled reminder", null, toggled)
    }

    @Test
    fun flippingTheSwitchOffNeverAsksForPermission() {
        // Turning something off cannot require a permission to do it.
        var requested = 0
        var toggled: Boolean? = null
        setCard(
            config = HydrationReminderConfig(enabled = true),
            hasPermission = false,
            onRequestPermission = { requested++ },
            onToggle = { toggled = it },
        )

        composeRule.onNode(androidx.compose.ui.test.isToggleable()).performClick()

        assertEquals(0, requested)
        assertEquals(false, toggled)
    }

    @Test
    fun tappingATimeRowOpensATimePickerAndReportsWhatItConfirms() {
        // The window is two clock times, and typing is not an option — the row's
        // trailing action IS the control. A row that renders the time but opens
        // nothing leaves the window at its default with no way to say so.
        //
        // "Select" is NOT the dialog's word: `HydrationReminderTimeRow` labels
        // every row's trailing button with it, so an enabled card already shows
        // two of them with nothing open. The dialog is identified as a dialog,
        // and by the Cancel that only it carries.
        var start: LocalTime? = null
        var end: LocalTime? = null
        setCard(
            config = HydrationReminderConfig(
                enabled = true,
                activeStartTime = LocalTime.of(7, 0),
                activeEndTime = LocalTime.of(22, 0),
            ),
            onSelectActiveStartTime = { start = it },
            onSelectActiveEndTime = { end = it },
        )

        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.action_cancel)).assertDoesNotExist()

        // One Select per time row, in layout order, so the first belongs to the
        // "Active from" row. WHICH row was opened is proved by the callback that
        // fires below, not by that ordering.
        val rowActions = composeRule.onAllNodesWithText(string(R.string.action_select))
        rowActions.assertCountEquals(2)
        rowActions[0].performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNode(isDialog()).assertExists()
        composeRule.onNodeWithText(string(R.string.action_cancel)).assertIsDisplayed()

        // Confirmed untouched, the picker hands back the time it was seeded
        // with — and hands it to the start of the window, not the end.
        composeRule
            .onNode(hasText(string(R.string.action_select)) and hasAnyAncestor(isDialog()))
            .performClick()
        composeRule.waitForIdle()

        assertEquals(LocalTime.of(7, 0), start)
        assertNull("the Active-from row must not drive the end of the window", end)
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    private fun setCard(
        config: HydrationReminderConfig,
        hasPermission: Boolean = true,
        onRequestPermission: () -> Unit = {},
        onToggle: (Boolean) -> Unit = {},
        onSelectActiveStartTime: (LocalTime) -> Unit = {},
        onSelectActiveEndTime: (LocalTime) -> Unit = {},
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    HydrationReminderCard(
                        config = config,
                        hasNotificationPermission = hasPermission,
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        onToggleReminders = onToggle,
                        onRequestNotificationPermission = onRequestPermission,
                        onDecreaseInterval = {},
                        onIncreaseInterval = {},
                        onSelectActiveStartTime = onSelectActiveStartTime,
                        onSelectActiveEndTime = onSelectActiveEndTime,
                    )
                }
            }
        }
    }
}
