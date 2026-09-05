package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportResult
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * An Apple Health export is a one-shot migration. The card must say how much access it has,
 * start, and admit when part of the archive was unreadable.
 */
class AppleHealthImportCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theCardNamesTheImporterItsAccessAndTheWayIn() {
        setCard()

        composeRule.onNodeWithText(string(R.string.settings_apple_health_import_title))
            .assertIsDisplayed()
        // Grants are per record type, so the count is the only place to see which worked.
        composeRule
            .onNodeWithText(
                string(R.string.settings_apple_health_import_permissions, 1, PERMISSIONS.size),
            )
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_apple_health_import_analyze_action))
            .performScrollTo()
            .assertIsEnabled()
    }

    @Test
    fun theReportActionsHandOverTheExactReportTheImportProduced() {
        var copied: String? = null
        var saveRequests = 0
        setCard(
            result = RESULT,
            onCopyReport = { copied = it },
            onSaveReport = { saveRequests++ },
        )

        composeRule.onNodeWithText(string(R.string.settings_apple_health_import_copy_report))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText(string(R.string.settings_apple_health_import_save_report))
            .performScrollTo()
            .performClick()

        // Both affordances must pass the import's own text through; a truncated report is worse than none.
        assertEquals(RESULT.shareableReportText, copied)
        assertEquals(1, saveRequests)
    }

    @Test
    fun anArchiveThatLostItsWorkoutRoutesSaysSoRatherThanReportingSuccess() {
        setCard(result = RESULT.copy(workoutRoutesIncomplete = true))

        // Routes cannot be re-imported from a partial ZIP, so the card must admit the archive ended early.
        composeRule
            .onNodeWithText(string(R.string.settings_apple_health_import_routes_incomplete))
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun setCard(
        result: AppleHealthImportResult? = null,
        onCopyReport: (String) -> Unit = {},
        onSaveReport: () -> Unit = {},
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    AppleHealthImportCard(
                        availability = HealthConnectAvailability.AVAILABLE,
                        importPermissions = PERMISSIONS,
                        grantedPermissions = setOf(PERMISSIONS.first()),
                        isAnalyzing = false,
                        isImporting = false,
                        analysisProgress = null,
                        analysis = null,
                        selectedCategories = emptySet(),
                        progress = null,
                        result = result,
                        error = null,
                        permissionDenied = false,
                        onGrantPermissions = {},
                        onImport = {},
                        onToggleCategory = { _, _ -> },
                        onImportSelected = {},
                        onCopyReport = onCopyReport,
                        onCopyError = {},
                        onSaveReport = onSaveReport,
                    )
                }
            }
        }
    }

    private companion object {
        val PERMISSIONS = setOf(
            "android.permission.health.WRITE_STEPS",
            "android.permission.health.WRITE_HEART_RATE",
        )

        val RESULT = AppleHealthImportResult(
            parsedRecords = 12,
            parsedWorkouts = 2,
            parsedCorrelations = 0,
            parsedActivitySummaries = 0,
            convertedRecords = 12,
            importedRecords = 10,
            duplicateSkippedRecords = 1,
            notSelectedRecords = 0,
            unsupportedElements = 1,
            skippedRecords = 0,
            failedRecords = 0,
            typeSummaries = emptyList(),
            diagnostics = emptyList(),
            shareableReportText = "IMPORT_REPORT",
        )
    }
}
