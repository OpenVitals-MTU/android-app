package tech.mmarca.openvitals.features.imports.csv

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The steps of Flutter's `test/features/imports/csv/csv_import_screen_test.dart`
 * that Kotlin exposes without a view model.
 *
 * The mapping, confirm and done steps take `CsvImportViewModel` directly, and
 * it needs a `Context`, the import service, the health repository and the
 * Health Connect manager — so those cases stay unported until those steps take
 * state and callbacks instead. That is a refactor with its own merits rather
 * than something to smuggle in behind a test, and the matrix says so.
 */
class CsvImportStepsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theImporterOpensOnThePickStepWithItsExplainer() {
        // The explainer carries the one promise that matters before a file
        // picker opens: nothing is written until you confirm.
        setPickStep(CsvImportState())

        composeRule
            .onNodeWithText(string(R.string.settings_csv_import_pick_title))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.settings_csv_import_pick_body))
            .assertIsDisplayed()
    }

    @Test
    fun tappingChooseCsvFileInvokesThePicker() {
        var picked = 0
        setPickStep(CsvImportState(), onPick = { picked++ })

        composeRule.onNodeWithText(string(R.string.settings_csv_import_pick_action)).performClick()

        assertEquals(1, picked)
    }

    @Test
    fun anUnreadableFileSaysWhyRatherThanReturningSilently() {
        setPickStep(CsvImportState(error = "Permission denied"))

        composeRule
            .onNodeWithText(
                string(R.string.settings_csv_import_unreadable_file, "Permission denied"),
            )
            .assertIsDisplayed()
    }

    @Test
    fun aFileWithOnlyAHeaderRowShowsTheEmptyFileMessage() {
        // A header with no rows under it is a readable file with nothing to
        // import — distinct from an unreadable one, and it offers the way back.
        var back = 0
        composeRule.setContent {
            OpenVitalsTheme { CsvEmptyFileBody(onBack = { back++ }) }
        }

        composeRule
            .onNodeWithText(string(R.string.settings_csv_import_empty_file))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_csv_import_back)).performClick()

        assertEquals(1, back)
    }

    private fun setPickStep(state: CsvImportState, onPick: () -> Unit = {}) {
        composeRule.setContent {
            OpenVitalsTheme { CsvPickStep(state = state, onPick = onPick) }
        }
    }
}
