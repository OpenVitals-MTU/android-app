package tech.mmarca.openvitals.features.nutrition

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.preferences.NutritionAverageBasis

/** The divisor behind "calories per day" (#259): which daily figure is right depends on whether blank days count. */
class NutritionAveragesTest {

    private val week = DatePeriod(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 10))
    private val afterTheWeek = LocalDate.of(2026, 5, 20)

    @Test fun `logged days ignores the days with nothing recorded`() {
        // Three days logged out of seven: the average of what was recorded, not of the week.
        val average = nutritionDailyAverage(
            values = listOf(1_800.0, 2_100.0, 2_400.0),
            period = week,
            basis = NutritionAverageBasis.LOGGED_DAYS,
            today = afterTheWeek,
        )

        assertEquals(2_100.0, average, 0.001)
    }

    @Test fun `every day divides by the whole period, not by the values it was given`() {
        // The values list is sparse, so counting it would silently become the logged-days average.
        val average = nutritionDailyAverage(
            values = listOf(1_800.0, 2_100.0, 2_400.0),
            period = week,
            basis = NutritionAverageBasis.EVERY_DAY,
            today = afterTheWeek,
        )

        assertEquals(6_300.0 / 7, average, 0.001)
    }

    @Test fun `a zero day counts against the every-day average but not the logged one`() {
        val values = listOf(2_000.0, 0.0, 2_000.0, 0.0)
        val period = DatePeriod(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 7))

        assertEquals(
            2_000.0,
            nutritionDailyAverage(values, period, NutritionAverageBasis.LOGGED_DAYS, afterTheWeek),
            0.001,
        )
        assertEquals(
            1_000.0,
            nutritionDailyAverage(values, period, NutritionAverageBasis.EVERY_DAY, afterTheWeek),
            0.001,
        )
    }

    @Test fun `a month still running divides by the days that have happened`() {
        // Dividing May's food by 31 on the 10th would report a third of what was eaten.
        val month = DatePeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31))

        val average = nutritionDailyAverage(
            values = List(10) { 2_000.0 },
            period = month,
            basis = NutritionAverageBasis.EVERY_DAY,
            today = LocalDate.of(2026, 5, 10),
        )

        assertEquals(2_000.0, average, 0.001)
    }

    @Test fun `a period already over divides by all of itself`() {
        val month = DatePeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))

        val average = nutritionDailyAverage(
            values = List(10) { 3_000.0 },
            period = month,
            basis = NutritionAverageBasis.EVERY_DAY,
            today = LocalDate.of(2026, 5, 10),
        )

        assertEquals(30_000.0 / 30, average, 0.001)
    }

    @Test fun `nothing logged averages to nothing rather than dividing by zero`() {
        assertEquals(
            0.0,
            nutritionDailyAverage(emptyList(), week, NutritionAverageBasis.LOGGED_DAYS, afterTheWeek),
            0.001,
        )
        assertEquals(
            0.0,
            nutritionDailyAverage(listOf(0.0, 0.0), week, NutritionAverageBasis.EVERY_DAY, afterTheWeek),
            0.001,
        )
    }

    @Test fun `a period that has not started has no average to give`() {
        val nextWeek = DatePeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7))

        assertEquals(
            0.0,
            nutritionDailyAverage(listOf(2_000.0), nextWeek, NutritionAverageBasis.EVERY_DAY, afterTheWeek),
            0.001,
        )
    }
}
