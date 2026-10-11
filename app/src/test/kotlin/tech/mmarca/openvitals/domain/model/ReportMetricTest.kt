package tech.mmarca.openvitals.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportMetricTest {

    @Test
    fun `every nutrient Health Connect stores has exactly one report metric`() {
        NutritionNutrient.entries.forEach { nutrient ->
            assertEquals(nutrient.name, 1, ReportMetric.entries.count { it.nutrient == nutrient })
        }
    }

    @Test
    fun `the breakdown is every nutrient past the five with a dashboard tile`() {
        val withTiles = setOf(
            NutritionNutrient.ENERGY,
            NutritionNutrient.PROTEIN,
            NutritionNutrient.TOTAL_CARBOHYDRATE,
            NutritionNutrient.TOTAL_FAT,
            NutritionNutrient.CAFFEINE,
        )
        val breakdown = ReportMetric.entries.filter { it.isNutrientBreakdown }

        assertEquals(NutritionNutrient.entries.toSet() - withTiles, breakdown.mapNotNull { it.nutrient }.toSet())
        assertTrue(breakdown.all { it.section == ReportSection.NUTRITION && it.valueKind == ReportValueKind.SUM })
    }
}
