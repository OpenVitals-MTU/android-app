package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.model.CaffeineEntry
import tech.mmarca.openvitals.domain.model.CaffeineSourceCategory
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences

class CaffeineInsightCalculatorTest {

    private val preferences = CaffeinePreferences()
    private val entry = CaffeineEntry(
        id = "coffee-1",
        startTime = Instant.parse("2026-07-01T08:00:00Z"),
        endTime = Instant.parse("2026-07-01T08:10:00Z"),
        caffeineMg = 100.0,
        name = "Coffee",
        source = "test.source",
        mealType = 0,
    )

    @Test
    fun `contribution is zero before intake and positive after absorption`() {
        assertEquals(
            0.0,
            CaffeineInsightCalculator.contributionMg(
                entry = entry,
                at = Instant.parse("2026-07-01T07:59:00Z"),
                preferences = preferences,
            ),
            0.001,
        )

        val contribution = CaffeineInsightCalculator.contributionMg(
            entry = entry,
            at = Instant.parse("2026-07-01T09:00:00Z"),
            preferences = preferences,
        )

        assertTrue(contribution > 0.0)
    }

    @Test
    fun `active caffeine decays over time`() {
        val early = CaffeineInsightCalculator.activeCaffeineMg(
            entries = listOf(entry),
            at = Instant.parse("2026-07-01T09:00:00Z"),
            preferences = preferences,
        )
        val late = CaffeineInsightCalculator.activeCaffeineMg(
            entries = listOf(entry),
            at = Instant.parse("2026-07-01T20:00:00Z"),
            preferences = preferences,
        )

        assertTrue(early > late)
    }

    @Test
    fun `build returns bedtime safety source and time bucket insights`() {
        val insights = CaffeineInsightCalculator.build(
            entries = listOf(entry),
            period = DatePeriod(
                start = LocalDate.of(2026, 7, 1),
                end = LocalDate.of(2026, 7, 3),
            ),
            preferences = preferences,
            now = Instant.parse("2026-07-01T12:00:00Z"),
            zone = ZoneOffset.UTC,
        )

        assertEquals(100.0, insights.periodTotalMg, 0.001)
        assertEquals(100.0 / 3.0, insights.periodAverageMg, 0.001)
        assertEquals(1, insights.loggedDays)
        assertEquals(3, insights.totalNights)
        assertEquals("test.source", insights.sourceTotals.single().label)
        assertEquals("Coffee", insights.categoryTotals.single().label)
        assertNotNull(insights.timeToThresholdMinutes)
        assertTrue(insights.curvePoints.isNotEmpty())
        assertEquals(CaffeineSourceCategory.COFFEE, insights.entryInsights.single().inferredCategory)
        assertEquals("Drip coffee", insights.entryInsights.single().catalogMatch?.item?.name)
    }

    @Test
    fun `caffeine health catalog matches health connect names without local entries`() {
        assertEquals(224, CaffeineHealthDrinkCatalog.items.size)

        val redBull = CaffeineHealthDrinkCatalog.matchName("Red Bull 250 ml")
        val cokeZero = CaffeineHealthDrinkCatalog.matchName("Coke Zero")
        val matcha = CaffeineHealthDrinkCatalog.matchName("Matcha latte")

        assertEquals("Red Bull", redBull?.item?.name)
        assertEquals(CaffeineSourceCategory.ENERGY_DRINK, redBull?.item?.category)
        assertEquals(CaffeineSourceCategory.SODA, cokeZero?.item?.category)
        assertEquals(CaffeineSourceCategory.TEA, matcha?.item?.category)

        val coffeePreset = CaffeineHealthDrinkCatalog.beveragePresets()
            .first { it.id == "caffeinehealth-drip-coffee" }
        assertEquals(240.0, coffeePreset.volumeMilliliters, 0.001)
        assertEquals(true, coffeePreset.isPreloaded)
        assertEquals(CaffeineSourceCategory.COFFEE, coffeePreset.category)
        assertEquals(2.0, coffeePreset.nutrientValues[NutritionNutrient.ENERGY] ?: 0.0, 0.001)
        assertEquals(0.095, coffeePreset.nutrientValues[NutritionNutrient.CAFFEINE] ?: 0.0, 0.001)

        val redBullPreset = CaffeineHealthDrinkCatalog.beveragePresets()
            .first { it.id == "caffeinehealth-red-bull" }
        assertEquals(110.0, redBullPreset.nutrientValues[NutritionNutrient.ENERGY] ?: 0.0, 0.001)
        assertEquals(27.0, redBullPreset.nutrientValues[NutritionNutrient.SUGAR] ?: 0.0, 0.001)
        assertEquals(0.08, redBullPreset.nutrientValues[NutritionNutrient.CAFFEINE] ?: 0.0, 0.001)
        assertEquals(
            CaffeineSourceCategory.COFFEE,
            CaffeineHealthDrinkCatalog.beveragePresetItem(coffeePreset.id)?.category,
        )
    }
}
