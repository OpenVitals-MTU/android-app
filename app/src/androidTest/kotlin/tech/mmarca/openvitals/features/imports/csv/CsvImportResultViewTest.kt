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
 * The screen the user is left looking at after an import. A run that wrote nothing has to
 * say so rather than leave three zeroes to be read as "done".
 */
class CsvImportResultViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aFinishedImportReportsWhatWasWritten() {
        setResult(
            CsvImportResult(
                outcome = CsvImportOutcome.COMPLETED,
                progress = CsvImportProgress(rowsRead = 2, written = 2),
            ),
        )

        composeRule
            .onNodeWithText(string(R.string.settings_csv_import_result, 2, 0, 0))
            .assertIsDisplayed()
        // Nothing was refused, so the run has nothing to explain.
        composeRule
            .onNodeWithText(string(R.string.settings_csv_import_result_empty))
            .assertDoesNotExist()
        composeRule
            .onNodeWithText(string(R.string.settings_csv_import_diagnostics_title))
            .assertDoesNotExist()
    }

    @Test
    fun anImportThatRejectsEveryRowSaysNothingWasImportedAndWhy() {
        // "Written 0" alone reads like a file already imported. The reason separates that from unparsed dates.
        setResult(
            CsvImportResult(
                outcome = CsvImportOutcome.COMPLETED,
                progress = CsvImportProgress(rowsRead = 2, rejected = 2),
                diagnosticCounts = mapOf(CsvImportDiagnosticReason.UNPARSABLE_TIMESTAMP to 2),
            ),
        )

        composeRule
            .onNodeWithText(string(R.string.settings_csv_import_result_empty))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.settings_csv_import_diagnostics_title))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(
                string(
                    R.string.settings_csv_import_diagnostic_line,
                    string(R.string.settings_csv_import_reason_unparsable_timestamp),
                    2,
                ),
            )
            .assertIsDisplayed()
    }

    @Test
    fun theFinishedImportOffersToTakeTheReportAway() {
        // The report is what a user sends a maintainer, so both ways out must be here.
        var copied = 0
        var saved = 0
        setResult(
            CsvImportResult(
                outcome = CsvImportOutcome.COMPLETED,
                progress = CsvImportProgress(rowsRead = 2, written = 2),
            ),
            onCopyReport = { copied++ },
            onSaveReport = { saved++ },
        )

        composeRule.onNodeWithText(string(R.string.settings_csv_import_copy_report)).performClick()
        composeRule.onNodeWithText(string(R.string.settings_csv_import_save_report)).performClick()

        assertEquals(1, copied)
        assertEquals(1, saved)
    }

    private fun setResult(
        result: CsvImportResult,
        onCopyReport: () -> Unit = {},
        onSaveReport: () -> Unit = {},
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                CsvImportResultView(
                    result = result,
                    onCopyReport = onCopyReport,
                    onSaveReport = onSaveReport,
                    onImportAnother = {},
                    onDone = {},
                )
            }
        }
    }
}
