package tech.mmarca.openvitals.features.nutrition

import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient

/** `cumulativeNutritionPoints`. */
class NutritionIntradayChartTest {

    private val day: Instant = Instant.parse("2026-03-04T00:00:00Z")

    private fun entry(time: Instant, energyKcal: Double) = NutritionEntry(
        time = time,
        mealType = 0,
        name = "Meal",
        energyKcal = energyKcal,
        proteinGrams = null,
        carbsGrams = null,
        fatGrams = null,
        fiberGrams = null,
        sugarGrams = null,
        source = "Test",
    )

    @Test fun `cumulativeNutritionPoints sorts by time, accumulates, and drops non-positive readings`() {
        val points = listOf(
            entry(day.plus(Duration.ofHours(13)), energyKcal = 700.0),
            entry(day.plus(Duration.ofHours(8)), energyKcal = 300.0),
            // Zero and null readings never enter the curve.
            entry(day.plus(Duration.ofHours(10)), energyKcal = 0.0),
        ).cumulativeNutritionPoints(NutritionNutrient.ENERGY)

        assertEquals(listOf(300.0, 1_000.0), points.map { it.second })
        assertEquals(8, points.first().first.atZone(ZoneOffset.UTC).hour)
        assertEquals(13, points.last().first.atZone(ZoneOffset.UTC).hour)
    }

    @Test fun `cumulativeNutritionPoints an entry with no value for the nutrient is skipped`() {
        val points = listOf(entry(day.plus(Duration.ofHours(9)), energyKcal = 500.0))
            .cumulativeNutritionPoints(NutritionNutrient.PROTEIN)

        assertTrue(points.isEmpty())
    }
}
