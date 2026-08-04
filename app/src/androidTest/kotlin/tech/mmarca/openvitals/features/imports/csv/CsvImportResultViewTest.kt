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
 * The finished-run cases of Flutter's
 * `test/features/imports/csv/csv_import_screen_test.dart`.
 *
 * An import is a tally, not a success or a failure: rows written, rows already
 * there, rows refused. The counts themselves are pinned by
 * `CsvImportServiceTest`; what these cover is the screen the user is left
 * looking at, where a run that wrote nothing has to say so out loud rather than
 * leaving three zeroes to be read as "done".
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
        // "Written 0" on its own reads like a file that was already imported.
        // The sentence and the reason are what separate that from a file whose
        // dates were never understood.
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
        // The report is what a user sends a maintainer when an import went
        // wrong, so both ways out of the app have to be on this screen.
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
