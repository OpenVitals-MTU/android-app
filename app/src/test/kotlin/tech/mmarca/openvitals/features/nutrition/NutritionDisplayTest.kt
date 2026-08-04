package tech.mmarca.openvitals.features.nutrition

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionNutrientGroup

/**
 * Ported from mobile-app test/features/nutrition/nutrition_display_test.dart.
 *
 * Kotlin keeps every nutrient series on [NutritionDisplayState.overviewNutrients] and
 * splits primary from additional at the render site, so the split is asserted over
 * [primaryNutritionOverviewNutrients] and [NutritionNutrient.group].
 */
class NutritionDisplayTest {

    private val monday: LocalDate = LocalDate.of(2026, 3, 2)
    private val weekQuery = PeriodLoadQuery(
        range = TimeRange.WEEK,
        anchorDate = monday,
        weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    )

    private fun build(
        dailyMacros: List<DailyMacros> = emptyList(),
        entries: List<NutritionEntry> = emptyList(),
        goal: Double = 2_000.0,
    ) = NutritionPresentationMapper.build(
        query = weekQuery,
        metric = NutritionMetric.CALORIES_IN,
        dailyGoal = goal,
        dailyMacros = dailyMacros,
        previousDailyMacros = emptyList(),
        baselineDailyMacros = emptyList(),
        entries = entries,
    )

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
        source = "Test source",
    )

    @Test fun `tracked nutrients split into primary and grouped additional ones`() {
        val display = build(
            dailyMacros = listOf(
                DailyMacros(
                    date = monday,
                    nutrientValues = mapOf(NutritionNutrient.SODIUM to 2.1),
                    energyKcal = 1_900.0,
                    proteinGrams = 85.0,
                ),
            ),
        )

        val tracked = display.overviewNutrients.filter { it.hasTrackedValues }.map { it.nutrient }
        assertTrue(
            tracked.containsAll(
                listOf(
                    NutritionNutrient.ENERGY,
                    NutritionNutrient.PROTEIN,
                    NutritionNutrient.SODIUM,
                ),
            ),
        )

        // Sodium is not a primary macro, so it lands in its own group.
        val additional = tracked.filterNot { it in primaryNutritionOverviewNutrients }
        assertEquals(listOf(NutritionNutrient.SODIUM), additional)

        val byGroup = additional.groupBy { it.group }
        assertEquals(listOf(NutritionNutrient.SODIUM), byGroup[NutritionNutrientGroup.MINERALS])
        assertEquals(null, byGroup[NutritionNutrientGroup.VITAMINS])
    }

    @Test fun `the day curve accumulates in time order, skipping absent readings`() {
        val day = Instant.parse("2026-03-02T00:00:00Z")
        val entries = listOf(
            entry(day.plus(Duration.ofHours(13)), energyKcal = 700.0),
            entry(day.plus(Duration.ofHours(8)), energyKcal = 300.0),
            entry(day.plus(Duration.ofHours(10)), energyKcal = 0.0),
        )

        val samples = entries.cumulativeNutritionPoints(NutritionNutrient.ENERGY)
        assertEquals(listOf(300.0, 1_000.0), samples.map { it.second })
        assertEquals(8, samples.first().first.atZone(ZoneOffset.UTC).hour)

        // A nutrient the entries carry nothing for gets no curve at all.
        assertTrue(entries.cumulativeNutritionPoints(NutritionNutrient.PROTEIN).isEmpty())
    }

    @Test fun `meals are listed newest first, and indexed by their day`() {
        val entries = listOf(
            entry(Instant.parse("2026-03-02T08:00:00Z"), energyKcal = 300.0),
            entry(Instant.parse("2026-03-03T13:00:00Z"), energyKcal = 700.0),
            entry(Instant.parse("2026-03-02T20:00:00Z"), energyKcal = 500.0),
        )

        // Entries alone are data.
        assertTrue(build(entries = entries).hasData)

        val newestFirst = entries.nutritionEntriesNewestFirst()
        assertEquals(Instant.parse("2026-03-03T13:00:00Z"), newestFirst.first().time)

        val mondayEntries = entries.nutritionEntriesOnDay(monday, ZoneOffset.UTC)
        assertEquals(2, mondayEntries.size)
        // Each day's list is newest first too.
        assertEquals(Instant.parse("2026-03-02T20:00:00Z"), mondayEntries.first().time)
        assertEquals(1, entries.nutritionEntriesOnDay(monday.plusDays(1), ZoneOffset.UTC).size)
    }
}
