package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.model.BeverageCategory
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
    fun `a drink sipped over two hours peaks later and lower than one taken at once`() {
        // The same 160 mg downed at once or nursed over two hours: the interval spreads the dose.
        val atOnce = entry.copy(
            caffeineMg = 160.0,
            endTime = entry.startTime.plusSeconds(60),
        )
        val slow = entry.copy(
            caffeineMg = 160.0,
            endTime = entry.startTime.plusSeconds(2 * 60 * 60),
        )

        val atOncePeak = CaffeineInsightCalculator.peakContribution(atOnce, preferences)
        val slowPeak = CaffeineInsightCalculator.peakContribution(slow, preferences)

        assertTrue(slowPeak.time.isAfter(atOncePeak.time))
        assertTrue(slowPeak.valueMg < atOncePeak.valueMg)
        // Half an hour in, only a quarter of the slow drink has even been swallowed.
        val halfHour = entry.startTime.plusSeconds(30 * 60)
        val atOnceHalfHour = CaffeineInsightCalculator.contributionMg(atOnce, halfHour, preferences)
        val slowHalfHour = CaffeineInsightCalculator.contributionMg(slow, halfHour, preferences)
        assertTrue(slowHalfHour < atOnceHalfHour / 2)
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
        // Midday on the first day: no 22:30 bedtime has been lived yet.
        assertEquals(0, insights.totalNights)
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
        assertEquals(BeverageCategory.COFFEE, coffeePreset.category)
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

    @Test
    fun `a bedtime before noon belongs to the night that ENDS the day`() {
        val insights = CaffeineInsightCalculator.build(
            entries = listOf(entry),
            period = DatePeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1)),
            preferences = preferences.copy(bedtime = LocalTime.of(6, 30)),
            now = Instant.parse("2026-07-03T12:00:00Z"),
            zone = ZoneOffset.UTC,
        )

        // July 1's 06:30 bedtime is the morning of July 2. Anchored to July 1 it sat before the coffee.
        val expected = CaffeineInsightCalculator.activeCaffeineMg(
            entries = listOf(entry),
            at = Instant.parse("2026-07-02T06:30:00Z"),
            preferences = preferences,
        )
        assertEquals(expected, insights.dailyStats.single().bedtimeMg, 0.001)
        assertTrue(insights.dailyStats.single().bedtimeMg > 0.0)
    }

    @Test
    fun `an evening bedtime stays on its own date`() {
        val insights = CaffeineInsightCalculator.build(
            entries = listOf(entry),
            period = DatePeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1)),
            preferences = preferences.copy(bedtime = LocalTime.of(22, 30)),
            now = Instant.parse("2026-07-03T12:00:00Z"),
            zone = ZoneOffset.UTC,
        )

        val expected = CaffeineInsightCalculator.activeCaffeineMg(
            entries = listOf(entry),
            at = Instant.parse("2026-07-01T22:30:00Z"),
            preferences = preferences,
        )
        assertEquals(expected, insights.dailyStats.single().bedtimeMg, 0.001)
    }

    @Test
    fun `nights that have not been lived yet are not counted`() {
        val insights = CaffeineInsightCalculator.build(
            entries = listOf(entry),
            period = DatePeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3)),
            preferences = preferences,
            now = Instant.parse("2026-07-02T12:00:00Z"),
            zone = ZoneOffset.UTC,
        )

        // Only July 1's 22:30 has passed; July 2's and July 3's are projections.
        assertEquals(listOf(true, false, false), insights.dailyStats.map { it.nightCompleted })
        assertEquals(1, insights.totalNights)
        assertEquals(1, insights.safeNights)
    }

    @Test
    fun `an unlived night neither breaks nor extends the safe sleep streak`() {
        val yesterday = Instant.parse("2026-07-01T08:00:00Z")
        val today = Instant.parse("2026-07-02T08:00:00Z")
        val entries = listOf(
            entry.copy(id = "small", startTime = yesterday, endTime = yesterday.plusSeconds(600), caffeineMg = 20.0),
            entry.copy(id = "huge", startTime = today, endTime = today.plusSeconds(600), caffeineMg = 900.0),
        )

        val insights = CaffeineInsightCalculator.build(
            entries = entries,
            period = DatePeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)),
            preferences = preferences,
            now = Instant.parse("2026-07-02T12:00:00Z"),
            zone = ZoneOffset.UTC,
        )

        // Tonight is projected unsafe but has not happened: the streak counts from the last night lived.
        assertEquals(false, insights.dailyStats.last().safeForSleep)
        assertEquals(false, insights.dailyStats.last().nightCompleted)
        assertEquals(1, insights.safeSleepStreak)
    }
}
