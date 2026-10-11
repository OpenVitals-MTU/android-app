package tech.mmarca.openvitals.features.reports

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.preferences.UnitSystem

class ReportNutrientDisplayTest {

    private val formatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC }, localeProvider = { Locale.US })

    private fun shown(nutrient: NutritionNutrient, storedValue: Double) =
        reportNutrientDisplay(nutrient, storedValue, formatter).text

    @Test
    fun `a breakdown nutrient prints in the unit its label uses, not in grams`() {
        assertEquals("2,300 mg", shown(NutritionNutrient.SODIUM, 2.3))
        assertEquals("15 µg", shown(NutritionNutrient.VITAMIN_D, 0.000015))
        assertEquals("28 g", shown(NutritionNutrient.DIETARY_FIBER, 28.0))
        assertEquals("250 kcal", shown(NutritionNutrient.ENERGY_FROM_FAT, 250.0))
    }

    @Test
    fun `an amount under ten keeps a decimal`() {
        assertEquals("2.4 µg", shown(NutritionNutrient.VITAMIN_B12, 0.0000024))
        assertEquals("0.5 g", shown(NutritionNutrient.TRANS_FAT, 0.5))
    }
}
