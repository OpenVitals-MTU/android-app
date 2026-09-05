package tech.mmarca.openvitals.features.manualentry.hydration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.NutritionNutrient

class HydrationCustomDrinkInputTest {

    @Test
    fun `customHydrationDrinkFromInput normalizes the name and sorts nutrients by enum-constant name`() {
        val drink = CustomHydrationDrinkInput(
            name = "  Flat white  ",
            volumeMilliliters = 250.0,
            nutrientValues = mapOf(
                NutritionNutrient.PROTEIN to 3.0,
                NutritionNutrient.ENERGY to 120.0,
                NutritionNutrient.CAFFEINE to 0.08,
            ),
        ).toCustomHydrationDrink(id = "drink-1")

        assertNotNull(drink)
        assertEquals("Flat white", drink!!.name)
        assertEquals("drink-1", drink.id)
        // Sorted by the enum-constant name: CAFFEINE, ENERGY, PROTEIN.
        assertEquals(
            listOf(
                NutritionNutrient.CAFFEINE,
                NutritionNutrient.ENERGY,
                NutritionNutrient.PROTEIN,
            ),
            drink.nutrientValues.keys.toList(),
        )
    }

    @Test
    fun `customHydrationDrinkFromInput rejects a blank name and an out-of-range volume`() {
        assertNull(
            CustomHydrationDrinkInput(name = "   ", volumeMilliliters = 250.0)
                .toCustomHydrationDrink(id = "x"),
        )
        assertNull(
            CustomHydrationDrinkInput(name = "Tea", volumeMilliliters = 0.0)
                .toCustomHydrationDrink(id = "x"),
        )
    }

    @Test
    fun `customHydrationDrinkFromInput one invalid nutrient rejects the whole drink, not just that nutrient`() {
        // A single bad value fails the drink rather than silently dropping the nutrient.
        val drink = CustomHydrationDrinkInput(
            name = "Soda",
            volumeMilliliters = 330.0,
            nutrientValues = mapOf(
                NutritionNutrient.ENERGY to 140.0,
                NutritionNutrient.SUGAR to -1.0,
            ),
        ).toCustomHydrationDrink(id = "x")

        assertNull(drink)
    }

    @Test
    fun `customHydrationDrinkFromInput rejects a nutrient value above the maximum`() {
        assertNull(
            CustomHydrationDrinkInput(
                name = "Soda",
                volumeMilliliters = 330.0,
                nutrientValues = mapOf(
                    NutritionNutrient.ENERGY to MaxCustomDrinkNutrientValue + 1.0,
                ),
            ).toCustomHydrationDrink(id = "x"),
        )
    }

    @Test
    fun `customHydrationDrinkFromInput rejects a hydration multiplier outside 0 to 1`() {
        assertNull(
            CustomHydrationDrinkInput(
                name = "Beer",
                volumeMilliliters = 330.0,
                hydrationMultiplier = 1.5,
            ).toCustomHydrationDrink(id = "x"),
        )
    }

    @Test
    fun `hydration impact maps a multiplier back onto its option`() {
        assertEquals(HydrationImpactOption.FULL, hydrationImpactOptionForMultiplier(1.0))
        assertEquals(HydrationImpactOption.NONE, hydrationImpactOptionForMultiplier(0.0))
        assertEquals(HydrationImpactOption.PARTIAL, hydrationImpactOptionForMultiplier(0.5))
    }

    @Test
    fun `hydration impact partial percent parses only strictly between 0 and 100`() {
        assertEquals(
            0.5,
            hydrationImpactMultiplier(HydrationImpactOption.PARTIAL, "50") ?: Double.NaN,
            1e-9,
        )
        assertEquals(
            0.005,
            hydrationImpactMultiplier(HydrationImpactOption.PARTIAL, "0,5") ?: Double.NaN,
            1e-9,
        )
        assertNull(hydrationImpactMultiplier(HydrationImpactOption.PARTIAL, "0"))
        assertNull(hydrationImpactMultiplier(HydrationImpactOption.PARTIAL, "100"))
        assertNull(hydrationImpactMultiplier(HydrationImpactOption.PARTIAL, "abc"))
    }

    @Test
    fun `hydration impact full and none ignore the percent text`() {
        assertEquals(
            1.0,
            hydrationImpactMultiplier(HydrationImpactOption.FULL, "abc") ?: Double.NaN,
            1e-9,
        )
        assertEquals(
            0.0,
            hydrationImpactMultiplier(HydrationImpactOption.NONE, "abc") ?: Double.NaN,
            1e-9,
        )
    }

    @Test
    fun `hydration impact percent text falls back to the default outside the partial range`() {
        assertEquals("42", hydrationImpactPercentText(0.42))
        assertEquals(DefaultPartialHydrationImpactPercent.toString(), hydrationImpactPercentText(1.0))
        assertEquals(DefaultPartialHydrationImpactPercent.toString(), hydrationImpactPercentText(0.0))
        // Clamped into [1, 99] rather than rounding to 0 or 100.
        assertEquals("1", hydrationImpactPercentText(0.001))
        assertEquals("99", hydrationImpactPercentText(0.999))
    }

    @Test
    fun `isValidCustomDrinkNutrientValue accepts 0 exclusive to max inclusive and nothing else`() {
        assertFalse(isValidCustomDrinkNutrientValue(0.0))
        assertTrue(isValidCustomDrinkNutrientValue(0.1))
        assertTrue(isValidCustomDrinkNutrientValue(MaxCustomDrinkNutrientValue))
        assertFalse(isValidCustomDrinkNutrientValue(MaxCustomDrinkNutrientValue + 0.1))
        assertFalse(isValidCustomDrinkNutrientValue(Double.NaN))
        assertFalse(isValidCustomDrinkNutrientValue(Double.POSITIVE_INFINITY))
    }
}
