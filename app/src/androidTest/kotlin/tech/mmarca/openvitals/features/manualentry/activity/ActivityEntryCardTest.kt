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
 * Ports the `entry card` group (and the training-plan visibility case) of
 * Flutter's `test/features/manualentry/activity/activity_entry_screen_test.dart`.
 *
 * This card is where a workout is described before it is written to Health
 * Connect for good. A field that goes missing is data the user cannot enter and
 * cannot come back for; a unit label that stops following the unit system turns
 * a 5 mile run into a 5 km one silently; and a validation message that lands on
 * the card instead of the field leaves the user hunting for which box is wrong.
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
        // The feeling is the one part of an entry that is not a number, and it
        // is optional. A chip that cannot be un-picked would force a mood onto
        // every workout a user ever logs.
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

        // The chips draw outlined sentiment glyphs rather than emoji, so there is
        // no text to find them by — the accessible name is the only handle, which
        // is the right one to assert anyway: it is what a screen reader reads.
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
        // A run entered as "5" means five miles to someone on imperial units. A
        // label that stayed metric would have the app store 5 km and never say so.
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
        // The card-level message says "fix the highlighted fields"; without the
        // per-field message there is nothing highlighted to fix.
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
    fun theTrainingPlanSectionOnlyShowsForRepetitionCountedTypes() {
        // A plan is a list of sets. Offering "Save plan" on a run would save a
        // plan with nothing in it for the user to ever start from.
        setContent {
            TestActivityEntryCard(
                state = ActivityEntryUiState(
                    canWrite = true,
                    isCheckingPermission = false,
                    selectedActivityType = runningEntryType,
                ),
            )
        }

        composeRule.onNodeWithText(string(R.string.activity_entry_save_training_plan)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.activity_entry_training_plan_label)).assertDoesNotExist()
    }

    @Test
    fun aRepetitionCountedTypeGetsTheTrainingPlanSection() {
        setContent {
            TestActivityEntryCard(
                state = ActivityEntryUiState(
                    canWrite = true,
                    isCheckingPermission = false,
                    selectedActivityType = pushUpsEntryType,
                ),
            )
        }

        scrollTo(string(R.string.activity_entry_training_plan_label)).assertIsDisplayed()
        scrollTo(string(R.string.activity_entry_save_training_plan)).assertIsDisplayed()
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
