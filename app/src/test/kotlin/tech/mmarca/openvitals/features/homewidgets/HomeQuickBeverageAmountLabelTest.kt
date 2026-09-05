package tech.mmarca.openvitals.features.homewidgets

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.domain.preferences.UnitSystem

class HomeQuickBeverageAmountLabelTest {
    private val metric = unitFormatter(UnitSystem.METRIC)
    private val imperial = unitFormatter(UnitSystem.IMPERIAL)

    @Test
    fun `metric sub-litre volumes read in millilitres, without a space`() {
        // The widget's compact form; the entry screen renders "250 ml".
        assertEquals("250ml", quickBeverageAmountLabel(drink(250.0), metric))
        assertEquals("330ml", quickBeverageAmountLabel(drink(330.0), metric))
    }

    @Test
    fun `metric volumes of a litre and up read in litres`() {
        assertEquals("1.00 L", quickBeverageAmountLabel(drink(1000.0), metric))
        assertEquals("1.50 L", quickBeverageAmountLabel(drink(1500.0), metric))
    }

    @Test
    fun `imperial always reads through the formatter`() {
        // No millilitre special case: even a sub-litre drink is fluid ounces.
        assertEquals("8 fl oz", quickBeverageAmountLabel(drink(250.0), imperial))
        assertEquals("34 fl oz", quickBeverageAmountLabel(drink(1000.0), imperial))
    }

    private fun drink(volumeMilliliters: Double): CustomHydrationDrink =
        CustomHydrationDrink(
            id = "a",
            name = "A",
            volumeMilliliters = volumeMilliliters,
        )
}
