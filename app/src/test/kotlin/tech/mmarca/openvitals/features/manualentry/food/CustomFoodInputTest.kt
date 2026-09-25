package tech.mmarca.openvitals.features.manualentry.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.CustomFood
import tech.mmarca.openvitals.domain.model.FoodCategory
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.preferences.UnitSystem

class CustomFoodInputTest {

    private fun food(
        id: String = "f",
        name: String = id,
        category: FoodCategory? = null,
        amountGrams: Double = 100.0,
        nutrientValues: Map<NutritionNutrient, Double> = mapOf(NutritionNutrient.ENERGY to 100.0),
    ) = CustomFood(id = id, name = name, amountGrams = amountGrams, nutrientValues = nutrientValues, category = category)

    @Test
    fun `toCustomFood trims the name and sorts nutrients by enum-constant name`() {
        val food = CustomFoodInput(
            name = "  Banana  ",
            amountGrams = 120.0,
            category = FoodCategory.FRUIT,
            nutrientValues = mapOf(
                NutritionNutrient.TOTAL_CARBOHYDRATE to 27.0,
                NutritionNutrient.ENERGY to 105.0,
            ),
        ).toCustomFood(id = "food-1")

        assertNotNull(food)
        assertEquals("Banana", food!!.name)
        assertEquals("food-1", food.id)
        assertEquals(FoodCategory.FRUIT, food.category)
        assertEquals(
            listOf(NutritionNutrient.ENERGY, NutritionNutrient.TOTAL_CARBOHYDRATE),
            food.nutrientValues.keys.toList(),
        )
    }

    @Test
    fun `toCustomFood rejects a blank name, an out-of-range amount and no nutrients`() {
        val nutrients = mapOf(NutritionNutrient.ENERGY to 10.0)

        assertNull(CustomFoodInput(name = "  ", amountGrams = 100.0, nutrientValues = nutrients).toCustomFood("x"))
        assertNull(CustomFoodInput(name = "Rice", amountGrams = 0.5, nutrientValues = nutrients).toCustomFood("x"))
        assertNull(CustomFoodInput(name = "Rice", amountGrams = MaxFoodAmountGrams + 1, nutrientValues = nutrients).toCustomFood("x"))
        // A food with no nutrient has nothing to write.
        assertNull(CustomFoodInput(name = "Rice", amountGrams = 100.0).toCustomFood("x"))
    }

    @Test
    fun `toCustomFood one invalid nutrient rejects the whole food`() {
        assertNull(
            CustomFoodInput(
                name = "Cake",
                amountGrams = 80.0,
                nutrientValues = mapOf(
                    NutritionNutrient.ENERGY to 300.0,
                    NutritionNutrient.SUGAR to -1.0,
                ),
            ).toCustomFood("x"),
        )
    }

    @Test
    fun `isValidFoodAmountGrams accepts the range inclusive and nothing else`() {
        assertFalse(isValidFoodAmountGrams(0.0))
        assertFalse(isValidFoodAmountGrams(0.99))
        assertTrue(isValidFoodAmountGrams(MinFoodAmountGrams))
        assertTrue(isValidFoodAmountGrams(MaxFoodAmountGrams))
        assertFalse(isValidFoodAmountGrams(MaxFoodAmountGrams + 0.1))
        assertFalse(isValidFoodAmountGrams(Double.NaN))
        assertFalse(isValidFoodAmountGrams(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `nutrientValuesFor scales every nutrient by the portion`() {
        val banana = food(
            amountGrams = 120.0,
            nutrientValues = mapOf(NutritionNutrient.ENERGY to 105.0, NutritionNutrient.TOTAL_CARBOHYDRATE to 27.0),
        )

        val half = banana.nutrientValuesFor(60.0)

        assertEquals(52.5, half.getValue(NutritionNutrient.ENERGY), 1e-9)
        assertEquals(13.5, half.getValue(NutritionNutrient.TOTAL_CARBOHYDRATE), 1e-9)
    }

    @Test
    fun `metric food input is grams and round-trips through the input text`() {
        assertEquals(120.0, foodInputGrams("120", UnitSystem.METRIC) ?: 0.0, 1e-9)
        assertEquals(12.5, foodInputGrams("12,5", UnitSystem.METRIC) ?: 0.0, 1e-9)
        assertEquals("120", foodInputAmountText(120.0, UnitSystem.METRIC))
        assertEquals("12.5", foodInputAmountText(12.5, UnitSystem.METRIC))
        assertEquals("", foodInputAmountText(null, UnitSystem.METRIC))
        assertNull(foodInputGrams("nope", UnitSystem.METRIC))
    }

    @Test
    fun `imperial food input converts ounces to grams and back`() {
        assertEquals(GramsPerOunce * 4, foodInputGrams("4", UnitSystem.IMPERIAL) ?: 0.0, 1e-9)
        assertEquals("4.2", foodInputAmountText(120.0, UnitSystem.IMPERIAL))
        assertEquals("oz", foodInputUnitLabel(UnitSystem.IMPERIAL))
        assertEquals("g", foodInputUnitLabel(UnitSystem.METRIC))
    }

    @Test
    fun `groupFoodCatalog lists uncategorised foods first, then categories in order, skipping empty ones`() {
        val sections = groupFoodCatalog(
            listOf(
                food("cake", category = FoodCategory.DESSERT),
                food("water-melon"),
                food("apple", category = FoodCategory.FRUIT),
            ),
        )

        assertEquals(listOf(null, FoodCategory.FRUIT, FoodCategory.DESSERT), sections.map { it.category })
        assertEquals(listOf("water-melon"), sections[0].foods.map { it.id })
        assertEquals(listOf("apple"), sections[1].foods.map { it.id })
    }

    @Test
    fun `groupFoodCatalog filters by name, ignoring case`() {
        val sections = groupFoodCatalog(
            listOf(food("a", name = "Apple pie", category = FoodCategory.DESSERT), food("b", name = "Banana")),
            query = " apple ",
        )

        assertEquals(listOf(FoodCategory.DESSERT), sections.map { it.category })
        assertEquals(listOf("a"), sections.single().foods.map { it.id })
        assertTrue(groupFoodCatalog(listOf(food("b", name = "Banana")), query = "zzz").isEmpty())
    }
}
