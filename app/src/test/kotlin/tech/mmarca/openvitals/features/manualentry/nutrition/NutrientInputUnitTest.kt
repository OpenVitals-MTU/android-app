package tech.mmarca.openvitals.features.manualentry.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionNutrientUnit

/** Nutrients are typed in the unit a nutrition label prints, and stored in grams. */
class NutrientInputUnitTest {

    @Test fun `each nutrient is typed in its label unit`() {
        assertEquals(NutrientInputUnit.KCAL, NutritionNutrient.ENERGY.inputUnit)
        assertEquals(NutrientInputUnit.GRAM, NutritionNutrient.PROTEIN.inputUnit)
        assertEquals(NutrientInputUnit.GRAM, NutritionNutrient.SATURATED_FAT.inputUnit)
        assertEquals(NutrientInputUnit.MILLIGRAM, NutritionNutrient.VITAMIN_C.inputUnit)
        assertEquals(NutrientInputUnit.MILLIGRAM, NutritionNutrient.SODIUM.inputUnit)
        assertEquals(NutrientInputUnit.MILLIGRAM, NutritionNutrient.CAFFEINE.inputUnit)
        assertEquals(NutrientInputUnit.MICROGRAM, NutritionNutrient.VITAMIN_K.inputUnit)
        assertEquals(NutrientInputUnit.MICROGRAM, NutritionNutrient.VITAMIN_D.inputUnit)
        assertEquals(NutrientInputUnit.MICROGRAM, NutritionNutrient.SELENIUM.inputUnit)
    }

    @Test fun `no vitamin or mineral is typed in grams`() {
        val inGrams = NutritionNutrient.entries.filter { nutrient ->
            nutrient.unit == NutritionNutrientUnit.MASS_ADAPTIVE && nutrient.inputUnit == NutrientInputUnit.GRAM
        }

        assertEquals(emptyList<NutritionNutrient>(), inGrams)
    }

    @Test fun `typed amounts store as grams, moving the decimal point exactly`() {
        assertEquals(0.000015, row(NutritionNutrient.VITAMIN_K, "15").storedValueOrNull()!!, 0.0)
        assertEquals(2.3, row(NutritionNutrient.SODIUM, "2300").storedValueOrNull()!!, 0.0)
        assertEquals(0.095, row(NutritionNutrient.CAFFEINE, "95").storedValueOrNull()!!, 0.0)
        assertEquals(120.5, row(NutritionNutrient.PROTEIN, "120,5").storedValueOrNull()!!, 0.0)
        assertEquals(2150.0, row(NutritionNutrient.ENERGY, " 2150 ").storedValueOrNull()!!, 0.0)
    }

    @Test fun `an unparsable, zero or out-of-range amount stores nothing`() {
        assertNull(row(NutritionNutrient.VITAMIN_K, "abc").storedValueOrNull())
        assertNull(row(NutritionNutrient.VITAMIN_K, "0").storedValueOrNull())
        assertNull(row(NutritionNutrient.PROTEIN, "-1").storedValueOrNull())
        // 10 000 g is the ceiling, in whatever unit it was typed.
        assertNull(row(NutritionNutrient.SODIUM, "10000001").storedValueOrNull())
    }

    @Test fun `a stored amount fills its field in the field's unit, without trailing zeros`() {
        assertEquals("15", NutritionNutrient.VITAMIN_K.inputText(0.000015))
        assertEquals("2300", NutritionNutrient.SODIUM.inputText(2.3))
        assertEquals("95", NutritionNutrient.CAFFEINE.inputText(0.095))
        assertEquals("120", NutritionNutrient.PROTEIN.inputText(120.0))
        assertEquals("120.5", NutritionNutrient.PROTEIN.inputText(120.5))
    }

    @Test fun `a food's or drink's stored nutrients fill their rows in label units`() {
        val rows = mapOf(
            NutritionNutrient.CAFFEINE to 0.095,
            NutritionNutrient.VITAMIN_D to 0.00001,
        ).toNutrientInputRows(compareBy { it.name })

        assertEquals(listOf("95", "10"), rows.map { it.amountText })
        assertEquals(
            mapOf(NutritionNutrient.CAFFEINE to 0.095, NutritionNutrient.VITAMIN_D to 0.00001),
            rows.parsedNutrientValues(),
        )
    }

    private fun row(nutrient: NutritionNutrient, text: String) = NutrientInputRow(nutrient, text)
}
