package tech.mmarca.openvitals.features.manualentry.hydration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.CaffeineSourceCategory
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of the catalog cases of Flutter's
 * `test/features/manualentry/hydration_entry_screen_test.dart`.
 *
 * Logging a drink is meant to take one tap from the notification or the tile,
 * so what this card does before the user types anything is the whole feature:
 * it has to say where the day stands, keep a long drinks list navigable, and
 * open straight onto the drink a deep link named.
 */
class HydrationEntryCatalogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTodaysHydrationAgainstTheDailyGoal() {
        // Half a litre in is a different decision from two litres in. Without
        // the goal beside it the number is unanchored, and the card is the only
        // place it appears before the drink is logged.
        setCard(state(todayLiters = 0.5, goalLiters = 2.0))

        val expected = "${FORMATTER.hydration(0.5).text} / ${FORMATTER.hydration(2.0).text}"
        composeRule.onNodeWithText(expected).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun categorySectionsStartCollapsedAndExpandOnTap() {
        // A saved-drinks list long enough to be worth categorising is too long
        // to scroll past, so every named category arrives shut.
        setCard(state(savedDrinks = listOf(espresso(), greenTea())))

        val coffees = string(R.string.hydration_catalog_section_coffees)
        composeRule.onNodeWithText(coffees).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(ESPRESSO).assertDoesNotExist()

        composeRule.onNodeWithText(coffees).performClick()

        composeRule.onNodeWithText(ESPRESSO).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun searchingForceExpandsTheSectionsAndFiltersTheRows() {
        // Typing a name is the escape hatch from the categories; leaving the
        // matching section collapsed would show a hit count and hide the hit.
        setCard(state(savedDrinks = listOf(espresso(), greenTea())))

        composeRule.onNode(hasSetTextAction()).performScrollTo().performTextInput("espr")

        composeRule.onNodeWithText(ESPRESSO).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(GREEN_TEA).assertDoesNotExist()
    }

    @Test
    fun theEditToggleSwapsLoggingForEditMoveAndDeleteActions() {
        // The same row is both "log this" and "change this". Without the
        // toggle, a tap meant to fix a drink's volume would log it instead.
        setCard(state(savedDrinks = listOf(unassignedWater())))

        composeRule.onNodeWithContentDescription(string(R.string.cd_edit_drink))
            .assertDoesNotExist()

        composeRule.onNodeWithContentDescription(string(R.string.cd_edit_saved_drinks))
            .performScrollTo()
            .performClick()

        listOf(
            R.string.cd_edit_drink,
            R.string.cd_move_drink_category,
            R.string.cd_delete_drink,
        ).forEach { descriptionRes ->
            composeRule.onNodeWithContentDescription(string(descriptionRes))
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun aLogDrinkIdDeepLinkOpensThatDrinksEntryDialog() {
        // The hydration reminder's action carries a drink id. Landing on the
        // catalog instead of that drink's dialog turns a one-tap log into a
        // hunt through the list.
        setCard(state(savedDrinks = listOf(espresso())), initialLogDrinkId = ESPRESSO_ID)

        composeRule
            .onNodeWithText(string(R.string.hydration_log_saved_drink_title, ESPRESSO))
            .assertIsDisplayed()
    }

    @Test
    fun anUnknownLogDrinkIdOpensThePlainCatalog() {
        // Drinks get deleted while a reminder is still on screen. The link has
        // to degrade to the catalog rather than to an empty dialog.
        setCard(state(savedDrinks = listOf(espresso())), initialLogDrinkId = "deleted-drink")

        composeRule
            .onNodeWithText(string(R.string.hydration_log_saved_drink_title, ESPRESSO))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.hydration_new_drink_action))
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun state(
        todayLiters: Double = 0.0,
        goalLiters: Double = 2.0,
        savedDrinks: List<CustomHydrationDrink> = emptyList(),
    ) = HydrationEntryUiState(
        isCheckingPermission = false,
        canWriteHydration = true,
        canWriteNutrition = true,
        todayHydrationLiters = todayLiters,
        dailyGoalLiters = goalLiters,
        customDrinkOptions = savedDrinks,
    )

    private fun setCard(state: HydrationEntryUiState, initialLogDrinkId: String? = null) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    HydrationTrackerCard(
                        state = state,
                        unitFormatter = FORMATTER,
                        onAddSelectedEntry = {},
                        onSaveCustomDrink = { _, _ -> },
                        onAddSavedCustomDrinkEntry = { _, _, _ -> },
                        onDeleteCustomDrink = {},
                        onMoveCustomDrinkToTarget = { _, _ -> },
                        onMoveCustomDrinkToCategory = { _, _ -> },
                        onEntryTimeChanged = {},
                        onRequestWritePermission = {},
                        initialLogDrinkId = initialLogDrinkId,
                    )
                }
            }
        }
    }

    private fun espresso() = CustomHydrationDrink(
        id = ESPRESSO_ID,
        name = ESPRESSO,
        volumeMilliliters = 30.0,
        category = CaffeineSourceCategory.COFFEE,
    )

    private fun greenTea() = CustomHydrationDrink(
        id = "green-tea",
        name = GREEN_TEA,
        volumeMilliliters = 250.0,
        category = CaffeineSourceCategory.TEA,
    )

    /** A drink with no category sits outside every section, always visible. */
    private fun unassignedWater() = CustomHydrationDrink(
        id = "tap-water",
        name = "Tap water",
        volumeMilliliters = 500.0,
    )

    private companion object {
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
        const val ESPRESSO_ID = "espresso"
        const val ESPRESSO = "Espresso"
        const val GREEN_TEA = "Green tea"
    }
}
