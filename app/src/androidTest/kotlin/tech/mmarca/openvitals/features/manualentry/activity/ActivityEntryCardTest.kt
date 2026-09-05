package tech.mmarca.openvitals.features.manualentry.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The card where a workout is described before it is written. A missing field is data the
 * user cannot enter; a unit label that ignores the unit system turns 5 miles into 5 km.
 */
class ActivityEntryCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersEverySectionAWorkoutNeedsToBeDescribed() {
        setContent { TestActivityEntryCard(state = ActivityEntryUiState(canWrite = true, isCheckingPermission = false)) }

        listOf(
            R.string.activity_entry_type_label,
            R.string.activity_entry_title_label,
            R.string.activity_entry_start_date_label,
            R.string.activity_entry_start_time_label,
            R.string.activity_entry_duration_label,
            R.string.metric_active_calories,
            R.string.metric_calories_burned,
            R.string.activity_entry_feeling_label,
            R.string.activity_entry_notes_label,
            R.string.activity_entry_add,
            R.string.activity_entry_choose_another_source,
        ).forEach { labelRes ->
            scrollTo(string(labelRes)).assertIsDisplayed()
        }
    }

    @Test
    fun theFeelingChipsAreTheFourEmojiAndTapAgainClearsTheChoice() {
        // The feeling is optional. A chip that cannot be un-picked forces a mood onto every workout.
        val reported = mutableListOf<ActivityEntryFeeling?>()
        composeRule.setContent {
            OpenVitalsTheme {
                var selected by remember { mutableStateOf<ActivityEntryFeeling?>(null) }
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TestActivityEntryCard(
                        state = ActivityEntryUiState(
                            canWrite = true,
                            isCheckingPermission = false,
                            selectedFeeling = selected,
                        ),
                        onFeelingChanged = {
                            reported += it
                            selected = it
                        },
                    )
                }
            }
        }

        // The chips draw glyphs, not text, so the accessible name is the only handle.
        ActivityEntryFeeling.entries.forEach { feeling ->
            composeRule
                .onNodeWithContentDescription(string(feeling.labelRes))
                .performScrollTo()
                .assertIsDisplayed()
        }

        val greatLabel = string(R.string.activity_entry_feeling_great)
        composeRule.onNodeWithContentDescription(greatLabel).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription(greatLabel).performClick()

        assertEquals(listOf(ActivityEntryFeeling.GREAT, null), reported)
    }

    @Test
    fun distanceAndElevationFollowTheUnitSystem() {
        // "5" means five miles on imperial units. A metric label would store 5 km silently.
        setContent {
            TestActivityEntryCard(
                state = ActivityEntryUiState(canWrite = true, isCheckingPermission = false),
                unitFormatter = UnitFormatter(unitSystemProvider = { UnitSystem.IMPERIAL }),
            )
        }

        scrollTo(string(R.string.activity_entry_distance_label, "mi")).assertIsDisplayed()
        scrollTo(string(R.string.activity_entry_elevation_label, "ft")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_entry_distance_label, "km")).assertDoesNotExist()
    }

    @Test
    fun aValidationErrorLandsOnItsOwnFieldAsWellAsTheCard() {
        // Without the per-field message there is nothing highlighted to fix.
        setContent {
            TestActivityEntryCard(
                state = ActivityEntryUiState(
                    canWrite = true,
                    isCheckingPermission = false,
                    durationMinutesText = "0",
                    entryError = ActivityEntryError.INVALID_VALUE,
                    validationErrors = setOf(ActivityEntryValidationError.DURATION_INVALID),
                ),
            )
        }

        scrollTo(string(R.string.activity_entry_error_duration)).assertIsDisplayed()
        scrollTo(string(R.string.activity_entry_invalid_value)).assertIsDisplayed()
    }

    @Test
    fun saveAsPlanOnlyShowsForRepetitionCountedTypes() {
        // A plan is a list of steps. "Save as plan" on a run would save an empty plan.
        setContent {
            TestActivityEntryCard(
                state = ActivityEntryUiState(
                    canWrite = true,
                    isCheckingPermission = false,
                    selectedActivityType = runningEntryType,
                ),
            )
        }

        composeRule.onNodeWithText(string(R.string.activity_entry_save_as_plan)).assertDoesNotExist()
    }

    @Test
    fun aRepetitionCountedTypeGetsSaveAsPlan() {
        setContent {
            TestActivityEntryCard(
                state = ActivityEntryUiState(
                    canWrite = true,
                    isCheckingPermission = false,
                    selectedActivityType = pushUpsEntryType,
                ),
            )
        }

        scrollTo(string(R.string.activity_entry_save_as_plan)).assertIsDisplayed()
    }

    @Test
    fun aLinkedPlanShowsTheChipWithChangeEditAndRemoveInsteadOfSaveAsPlan() {
        var edited: String? = null
        setContent {
            TestActivityEntryCard(
                state = ActivityEntryUiState(
                    canWrite = true,
                    isCheckingPermission = false,
                    selectedActivityType = pushUpsEntryType,
                    linkedPlan = ActivityLinkedPlan(id = "plan-1", title = "Push-up pyramid"),
                ),
                onEditPlan = { edited = it },
            )
        }

        scrollTo(string(R.string.activity_entry_linked_plan, "Push-up pyramid")).assertIsDisplayed()
        scrollTo(string(R.string.activity_entry_linked_plan_change)).assertIsDisplayed()
        scrollTo(string(R.string.action_remove)).assertIsDisplayed()
        scrollTo(string(R.string.action_edit)).performClick()
        composeRule.onNodeWithText(string(R.string.activity_entry_save_as_plan)).assertDoesNotExist()
        org.junit.Assert.assertEquals("plan-1", edited)
    }

    private fun scrollTo(text: String) = composeRule.onNodeWithText(text).performScrollTo()

    private fun setContent(content: @Composable () -> Unit) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { content() }
            }
        }
    }
}
