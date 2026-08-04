package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Ports of the one- and two-case settings-card files: `reminder_test_card`,
 * `csv_import_card`, `metabolism_card` and `debug_diagnostics_card`.
 *
 * Each is small, and each is the only thing standing between a user and a
 * feature they cannot otherwise reach — a card that renders its title but loses
 * its button is a dead end with no error to report.
 */
class SettingsSmallCardsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reminderTestCard_rendersTheTitleBodyAndShowReminderAction() {
        var shown = 0
        setCard { ReminderTestCard(onShowTestReminder = { shown++ }) }

        composeRule.onNodeWithText(string(R.string.settings_reminder_test_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_reminder_test_body)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_reminder_test_show_hydration)).performClick()

        assertEquals(1, shown)
    }

    @Test
    fun csvImportCard_namesTheImporterAndWhatItIsFor() {
        var opened = 0
        setCard { CsvImportCard(onOpenCsvImport = { opened++ }) }

        composeRule.onNodeWithText(string(R.string.settings_csv_import_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_csv_import_body)).assertIsDisplayed()
        // The card is not itself clickable — the action is its own button, so
        // a user reads what the importer does before committing to a file picker.
        composeRule.onNodeWithText(string(R.string.settings_csv_import_action))
            .performScrollTo()
            .performClick()

        assertEquals(1, opened)
    }

    @Test
    fun debugDiagnosticsCard_rendersTheTitleBodyAndBothActions() {
        var shared = 0
        var saved = 0
        setCard { DebugDiagnosticsCard(onSaveLogs = { saved++ }, onShareLogs = { shared++ }) }

        composeRule.onNodeWithText(string(R.string.settings_debug_logs_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_debug_logs_body)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.settings_debug_logs_share)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.settings_debug_logs_save)).performScrollTo().performClick()

        assertEquals(1, shared)
        assertEquals(1, saved)
    }

    @Test
    fun metabolismCard_surfacesHormonalStatus_whichUsedToBeBuriedUnderCaffeine() {
        // Pregnancy roughly triples caffeine's half-life, so it belongs where
        // someone would look for it rather than inside a caffeine-model editor
        // most people never open.
        var saved: CaffeinePreferences? = null
        setCard {
            MetabolismCard(
                preferences = CaffeinePreferences(),
                onSave = { saved = it },
            )
        }

        composeRule.onNodeWithText(string(R.string.settings_metabolism_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_metabolism_hormonal_status))
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()

        // Saving from this card is what marks the profile complete; without it
        // the app keeps asking for details the user has already given.
        assertTrue("saving marks the profile complete", saved?.profileCompleted == true)
    }

    private fun setCard(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { content() }
            }
        }
    }
}
