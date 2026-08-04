package tech.mmarca.openvitals.features.manualentry.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Ports the `source card` and `plan pickers` groups of Flutter's
 * `test/features/manualentry/activity/activity_entry_screen_test.dart`.
 *
 * These two cards are the only doors into activity entry. If a button goes
 * missing there is no other route to that feature — the screen simply cannot be
 * used, with nothing on it to say why. The permission explainer matters for the
 * same reason: a user who cannot write to Health Connect has to be told that,
 * and handed the grant, or the form below looks broken rather than blocked.
 */
class ActivityEntrySourceCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun offersTheThreeSourcesAndKeepsFileImportOutOfThem() {
        setContent {
            ActivityEntrySourceCard(
                state = ActivityEntryUiState(canWrite = true, isCheckingPermission = false),
                onStartManualEntry = {},
                onCreateFromExistingPlan = {},
                onRecordGpsActivity = {},
                onRequestWritePermission = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.activity_entry_create_manual)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_entry_create_from_existing_plan)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_entry_record_gps)).assertIsDisplayed()
        // Importing a route file lives in Settings -> Data import. Offering it
        // here too would give the same job two homes that behave differently.
        composeRule.onNodeWithText(string(R.string.settings_route_import_action)).assertDoesNotExist()
    }

    @Test
    fun aMissingWritePermissionSaysSoAndOffersTheGrant() {
        // Without this the three buttons still sit there, and every one of them
        // dead-ends in a refusal the user had no way to have predicted.
        setContent {
            ActivityEntrySourceCard(
                state = ActivityEntryUiState(canWrite = false, isCheckingPermission = false),
                onStartManualEntry = {},
                onCreateFromExistingPlan = {},
                onRecordGpsActivity = {},
                onRequestWritePermission = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.activity_entry_permission_needed)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_grant)).assertIsDisplayed()
    }

    @Test
    fun theGrantActionOnlyAsksForPermissionAndOpensNothing() {
        // Granting is not choosing a source. If Grant also started an entry the
        // user would land in a form they never asked for, mid permission dialog.
        var granted = 0
        val chosen = mutableListOf<String>()
        setContent {
            ActivityEntrySourceCard(
                state = ActivityEntryUiState(canWrite = false, isCheckingPermission = false),
                onStartManualEntry = { chosen += "manual" },
                onCreateFromExistingPlan = { chosen += "plan" },
                onRecordGpsActivity = { chosen += "record" },
                onRequestWritePermission = { granted++ },
            )
        }

        composeRule.onNodeWithText(string(R.string.action_grant)).performScrollTo().performClick()

        assertEquals(1, granted)
        assertEquals(emptyList<String>(), chosen)
    }

    @Test
    fun anEmptyPlanListStillOffersAWayBack() {
        // A user who picks "from existing plan" and has none would otherwise be
        // stranded on an empty card with no visible exit.
        var back = 0
        setContent {
            ActivityPlanActivityPickerCard(
                state = ActivityEntryUiState(canWrite = true, isCheckingPermission = false),
                onSelectActivity = {},
                onChooseSource = { back++ },
            )
        }

        composeRule.onNodeWithText(string(R.string.activity_entry_plan_activity_picker_empty)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_entry_choose_another_source))
            .performScrollTo()
            .performClick()

        assertEquals(1, back)
    }

    private fun setContent(content: @Composable () -> Unit) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { content() }
            }
        }
    }
}
