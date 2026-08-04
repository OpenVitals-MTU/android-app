package tech.mmarca.openvitals.features.manualentry.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Ports the `repetitions` group of Flutter's
 * `test/features/manualentry/activity/activity_entry_screen_test.dart`.
 *
 * Repetition inputs are shown or hidden purely on the activity type, and each
 * shape means something different to Health Connect: a total, a step count, or
 * a list of sets with rests between them. Showing the wrong shape either loses
 * the sets a user typed or asks for reps on a bike ride. The per-set fields
 * matter most: they are the only place in the app where several identical boxes
 * sit next to each other, so a wiring mistake writes one set's reps over another.
 */
class ActivityRepetitionInputsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aPlainGpsActivityGetsNoRepetitionInputsAtAll() {
        setInputs(ActivityEntryUiState(selectedActivityType = runningEntryType))

        composeRule.onNodeWithText(string(R.string.activity_entry_repetitions_title)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.activity_entry_steps_title)).assertDoesNotExist()
    }

    @Test
    fun aStepCountedTypeGetsOneTotalAndNoModeSwitch() {
        // Steps are counted by the phone, not written down set by set. A
        // Total/Sets switch here would offer a shape steps cannot be stored in.
        setInputs(ActivityEntryUiState(selectedActivityType = walkingEntryType))

        composeRule.onAllNodesWithText(string(R.string.activity_entry_steps_title)).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_entry_repetition_mode_total)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.activity_entry_repetition_mode_sets)).assertDoesNotExist()
    }

    @Test
    fun aRepetitionCountedTypeOffersTotalAndSets() {
        setInputs(ActivityEntryUiState(selectedActivityType = pushUpsEntryType))

        composeRule.onAllNodesWithText(string(R.string.activity_entry_repetitions_title)).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_entry_repetitions_label)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_entry_repetition_mode_sets)).assertIsDisplayed()
    }

    @Test
    fun theLastRemainingSetCannotBeDeleted() {
        // An empty set list is not a workout the write path can build anything
        // from, and there would be no field left to type the reps back into.
        setInputs(
            ActivityEntryUiState(
                selectedActivityType = pushUpsEntryType,
                repetitionMode = ActivityRepetitionEntryMode.SETS,
                repetitionSets = listOf(ActivityRepetitionSetInput()),
            ),
        )

        composeRule.onNodeWithText(string(R.string.activity_entry_set_repetitions_label, 1)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_entry_set_rest_label)).assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription(string(R.string.cd_delete_entry)).assertCountEquals(1)
        composeRule.onAllNodesWithContentDescription(string(R.string.cd_delete_entry)).onFirst().assertIsNotEnabled()
    }

    @Test
    fun withASecondSetAddingAndDeletingBothReachTheForm() {
        var added = 0
        val removed = mutableListOf<Int>()
        setInputs(
            state = ActivityEntryUiState(
                selectedActivityType = pushUpsEntryType,
                repetitionMode = ActivityRepetitionEntryMode.SETS,
                repetitionSets = listOf(ActivityRepetitionSetInput(), ActivityRepetitionSetInput()),
            ),
            onAddSet = { added++ },
            onRemoveSet = { removed += it },
        )

        composeRule.onNodeWithText(string(R.string.activity_entry_set_repetitions_label, 2)).assertIsDisplayed()

        val deleteButtons = composeRule.onAllNodesWithContentDescription(string(R.string.cd_delete_entry))
        deleteButtons.assertCountEquals(2)
        deleteButtons[1].assertIsEnabled().performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.activity_entry_add_set)).performScrollTo().performClick()

        assertEquals("the second set is the one deleted", listOf(1), removed)
        assertEquals(1, added)
    }

    @Test
    fun typingInOneSetDoesNotBleedIntoAnother() {
        // Each box is bound to its own index. Cross-wire them and a user who
        // corrects set two silently rewrites set one — the kind of mistake that
        // is only ever noticed after the workout has been saved.
        val reported = mutableListOf<Pair<Int, String>>()
        setInputs(
            state = ActivityEntryUiState(
                selectedActivityType = pushUpsEntryType,
                repetitionMode = ActivityRepetitionEntryMode.SETS,
                repetitionSets = listOf(
                    ActivityRepetitionSetInput(repetitionsText = "12"),
                    ActivityRepetitionSetInput(repetitionsText = "8"),
                ),
            ),
            onSetRepetitionsChanged = { index, value -> reported += index to value },
        )

        // Field order is set 1 reps, set 1 rest, set 2 reps, set 2 rest.
        val fields = composeRule.onAllNodes(hasSetTextAction())
        fields[0].assertTextContains("12")
        fields[2].assertTextContains("8")

        fields[2].performScrollTo().performTextReplacement("9")

        assertEquals("only set two is touched", listOf(1 to "9"), reported)
    }

    private fun setInputs(
        state: ActivityEntryUiState,
        onAddSet: () -> Unit = {},
        onRemoveSet: (Int) -> Unit = {},
        onSetRepetitionsChanged: (Int, String) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ActivityRepetitionInputs(
                        state = state,
                        enabled = true,
                        onModeChanged = {},
                        onTotalChanged = {},
                        onSetRepetitionsChanged = onSetRepetitionsChanged,
                        onSetRestChanged = { _, _ -> },
                        onAddSet = onAddSet,
                        onRemoveSet = onRemoveSet,
                    )
                }
            }
        }
    }
}
