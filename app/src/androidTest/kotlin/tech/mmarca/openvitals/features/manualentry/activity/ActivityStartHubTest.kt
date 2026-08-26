package tech.mmarca.openvitals.features.manualentry.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The start hub replaced the source chooser and both plan pickers. It has to
 * keep the three ways in (plans, record, log by hand) without letting a file
 * import sneak back in as a fourth, and a plan row has to be one tap from the
 * prefilled form.
 */
class ActivityStartHubTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun offersPlansRecordAndManualLoggingAndKeepsFileImportOut() {
        var recorded = 0
        var manual = 0
        setHub(
            ActivityEntryUiState(canWrite = true, isCheckingPermission = false, hubPlans = listOf(plan())),
            onRecord = { recorded++ },
            onLogManually = { manual++ },
        )

        composeRule.onNodeWithText(string(R.string.activity_entry_record_gps)).performClick()
        composeRule.onNodeWithText(string(R.string.activity_entry_hub_log_manually)).performClick()
        composeRule.onNodeWithText(string(R.string.activity_entry_hub_manage_plans)).assertIsDisplayed()
        composeRule.onNodeWithText("Push-up pyramid").assertIsDisplayed()

        assertEquals(1, recorded)
        assertEquals(1, manual)
    }

    @Test
    fun aPlanRowLogsFromThatPlan() {
        var logged: String? = null
        setHub(
            ActivityEntryUiState(canWrite = true, isCheckingPermission = false, hubPlans = listOf(plan())),
            onLogFromPlan = { logged = it },
        )

        composeRule.onNodeWithText(string(R.string.activity_entry_hub_log_from_plan)).performClick()

        assertEquals("plan-1", logged)
    }

    @Test
    fun noPlansSaysSoAndAMissingPermissionOffersTheGrant() {
        var grants = 0
        setHub(
            ActivityEntryUiState(canWrite = false, isCheckingPermission = false, hubPlansError = ScreenError.PermissionDenied),
            onRequestWritePermission = { grants++ },
        )

        composeRule.onNodeWithText(string(R.string.action_grant)).performClick()
        assertEquals(1, grants)
    }

    @Test
    fun anEmptyPlanListExplainsWhereToBuildOne() {
        setHub(ActivityEntryUiState(canWrite = true, isCheckingPermission = false))

        composeRule.onNodeWithText(string(R.string.activity_entry_hub_plans_empty)).assertIsDisplayed()
    }

    private fun plan(): PlannedExerciseData = PlannedExerciseData(
        id = "plan-1",
        title = "Push-up pyramid",
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        startTime = Instant.now(),
        endTime = Instant.now().plusSeconds(20 * 60),
        hasExplicitTime = true,
        completedExerciseSessionId = null,
        notes = null,
        blockCount = 1,
        source = "tech.mmarca.openvitals",
    )

    private fun setHub(
        state: ActivityEntryUiState,
        onLogFromPlan: (String) -> Unit = {},
        onRecord: () -> Unit = {},
        onLogManually: () -> Unit = {},
        onRequestWritePermission: () -> Unit = {},
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ActivityStartHub(
                        state = state,
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        onLogFromPlan = onLogFromPlan,
                        onStartPlan = {},
                        onRepeatPlan = {},
                        onRecord = onRecord,
                        onLogManually = onLogManually,
                        onManagePlans = {},
                        onRequestWritePermission = onRequestWritePermission,
                    )
                }
            }
        }
    }
}
