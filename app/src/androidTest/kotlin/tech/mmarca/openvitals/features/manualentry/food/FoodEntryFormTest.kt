package tech.mmarca.openvitals.features.manualentry.food

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.CustomFood
import tech.mmarca.openvitals.domain.model.FoodCategory
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.nutrition.titleRes
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * A food is made once and logged many times. The card must show where the day stands, file
 * each food under its category, and turn a tap on a food into a portion with its amount ready.
 */
class FoodEntryFormTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTodaysEnergyAndSaysWhenTheCatalogIsEmpty() {
        setCard(state(todayEnergyKcal = 450.0))

        composeRule
            .onNodeWithText(string(R.string.food_today_energy, FORMATTER.energy(450.0).text))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.food_catalog_empty)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.food_new_food_action)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun foodsAreFiledUnderTheirCategoryWithTheirAmountAndEnergy() {
        // Uncategorised foods come first, then each category.
        setCard(state(foods = listOf(banana(), rice())))

        composeRule.onNodeWithText(string(R.string.food_catalog_saved_outside)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.food_catalog_section_fruit)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(BANANA).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(RICE).performScrollTo().assertIsDisplayed()
        composeRule
            .onNodeWithText("${foodAmountLabel(120.0, FORMATTER)} · ${FORMATTER.energy(105.0).text}")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun newFoodSavesNameAmountCategoryAndNutrients() {
        var saved: CustomFoodInput? = null
        var savedId: String? = "unset"
        setCard(state(), onSaveCustomFood = { input, id ->
            saved = input
            savedId = id
        })

        composeRule.onNodeWithText(string(R.string.food_new_food_action)).performScrollTo().performClick()
        // The dialog title reads the same as the button, so the name field stands in for it.
        composeRule.onNodeWithTag("food_name").assertIsDisplayed()
        // Nothing to save yet: no name, no amount, no nutrient.
        composeRule.onNodeWithText(string(R.string.action_save)).assertIsNotEnabled()

        composeRule.onNodeWithTag("food_name").performTextInput(BANANA)
        composeRule.onNodeWithTag("food_amount").performTextInput("120")
        composeRule.onNodeWithTag("food_category").performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.food_catalog_section_fruit)).performClick()
        composeRule.onNodeWithText(string(R.string.hydration_custom_drink_add_nutrient)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(NutritionNutrient.ENERGY.titleRes())).performScrollTo().performClick()
        composeRule.onNodeWithTag("nutrient_amount_ENERGY").performScrollTo().performTextInput("105")
        composeRule.onNodeWithText(string(R.string.action_save)).performClick()

        composeRule.runOnIdle {
            val input = saved
            assertNotNull(input)
            assertEquals(BANANA, input!!.name)
            assertEquals(120.0, input.amountGrams, 1e-9)
            assertEquals(FoodCategory.FRUIT, input.category)
            assertEquals(mapOf(NutritionNutrient.ENERGY to 105.0), input.nutrientValues)
            // A new food carries no id; the view model mints one.
            assertNull(savedId)
        }
    }

    @Test
    fun tappingAFoodOpensThePortionDialogWithItsAmountPreselected() {
        var logged: Triple<CustomFood, Double, Instant>? = null
        setCard(state(foods = listOf(banana())), onLogFood = { food, grams, time -> logged = Triple(food, grams, time) })

        composeRule.onNodeWithText(BANANA).performScrollTo().performClick()

        composeRule.onNodeWithText(string(R.string.food_log_food_title, BANANA)).assertIsDisplayed()
        composeRule.onNodeWithTag("food_log_amount").assertTextContains("120")
        composeRule.onNodeWithText(string(R.string.action_save)).performClick()
        composeRule.runOnIdle {
            assertEquals(banana(), logged?.first)
            assertEquals(120.0, logged?.second ?: 0.0, 1e-9)
        }
    }

    @Test
    fun theRowMenuEditsOrDeletesTheFood() {
        var deleted: CustomFood? = null
        setCard(state(foods = listOf(banana())), onDeleteCustomFood = { deleted = it })

        composeRule.onNodeWithContentDescription(string(R.string.cd_food_actions)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.action_edit)).performClick()
        composeRule.onNodeWithText(string(R.string.food_edit_food_title)).assertIsDisplayed()
        // The edit form arrives filled.
        composeRule.onNodeWithTag("food_name").assertTextContains(BANANA)
        composeRule.onNodeWithTag("nutrient_amount_ENERGY").performScrollTo().assertTextContains("105.0")
        composeRule.onNodeWithText(string(R.string.action_cancel)).performClick()

        composeRule.onNodeWithContentDescription(string(R.string.cd_food_actions)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.action_delete)).performClick()
        composeRule.runOnIdle { assertEquals(banana(), deleted) }
    }

    @Test
    fun withoutWritePermissionTheCalloutShowsAndAFoodDoesNotOpenThePortionDialog() {
        setCard(state(foods = listOf(banana()), canWrite = false))

        composeRule.onNodeWithText(string(R.string.food_tracker_permission_needed)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(BANANA).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.food_log_food_title, BANANA)).assertDoesNotExist()
    }

    @Test
    fun theAllowedAmountRangeIsStatedInTheFieldsOwnUnit() {
        var metricMessage = ""
        var imperialMessage = ""
        composeRule.setContent {
            OpenVitalsTheme {
                metricMessage = foodInvalidAmountText(UnitFormatter(unitSystemProvider = { UnitSystem.METRIC }))
                imperialMessage = foodInvalidAmountText(UnitFormatter(unitSystemProvider = { UnitSystem.IMPERIAL }))
            }
        }

        composeRule.runOnIdle {
            assertTrue(metricMessage.contains(foodInputUnitLabel(UnitSystem.METRIC)))
            assertTrue(imperialMessage.contains(foodInputUnitLabel(UnitSystem.IMPERIAL)))
            assertFalse(imperialMessage.contains(" ${foodInputUnitLabel(UnitSystem.METRIC)}"))
        }
    }

    private fun state(
        todayEnergyKcal: Double = 0.0,
        foods: List<CustomFood> = emptyList(),
        canWrite: Boolean = true,
    ) = FoodEntryUiState(
        isCheckingPermission = false,
        canWrite = canWrite,
        todayEnergyKcal = todayEnergyKcal,
        foodOptions = foods,
    )

    private fun setCard(
        state: FoodEntryUiState,
        onSaveCustomFood: (CustomFoodInput, String?) -> Unit = { _, _ -> },
        onLogFood: (CustomFood, Double, Instant) -> Unit = { _, _, _ -> },
        onDeleteCustomFood: (CustomFood) -> Unit = {},
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    FoodTrackerCard(
                        state = state,
                        unitFormatter = FORMATTER,
                        onSaveCustomFood = onSaveCustomFood,
                        onLogFood = onLogFood,
                        onDeleteCustomFood = onDeleteCustomFood,
                        onRequestWritePermission = {},
                    )
                }
            }
        }
    }

    private fun banana() = CustomFood(
        id = "banana",
        name = BANANA,
        amountGrams = 120.0,
        nutrientValues = mapOf(NutritionNutrient.ENERGY to 105.0),
        category = FoodCategory.FRUIT,
    )

    private fun rice() = CustomFood(
        id = "rice",
        name = RICE,
        amountGrams = 150.0,
        nutrientValues = mapOf(NutritionNutrient.TOTAL_CARBOHYDRATE to 42.0),
    )

    private companion object {
        const val BANANA = "Banana"
        const val RICE = "Rice"
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
    }
}
