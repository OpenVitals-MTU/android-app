package tech.mmarca.openvitals.features.activity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.model.BmrEntry
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailySteps
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CaloriesDerivationsTest {

    private val day = LocalDate.of(2026, 6, 10)
    private val period = DatePeriod(day.minusDays(6), day)

    private fun nutrition(date: LocalDate, kcal: Double) =
        DailyNutrition(date, hydrationLiters = 0.0, caloriesBurnedKcal = kcal)

    private fun steps(date: LocalDate, activeKcal: Double?) =
        DailySteps(date, steps = 0L, distanceMeters = 0.0, activeCaloriesKcal = activeKcal)

    private fun bmr(date: LocalDate, kcal: Double) = BmrEntry(
        time = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(),
        kcalPerDay = kcal,
        source = "test",
    )

    // Statistics.

    @Test
    fun `statistics are null for empty input rather than zero`() {
        val statistics = caloriesStatistics(CaloriesUiState())

        assertNull(statistics.totalSum)
        assertNull(statistics.totalAverage)
        assertNull(statistics.activeSum)
        assertNull(statistics.activeAverage)
        assertEquals(0, statistics.bmrReadingCount)
    }

    @Test
    fun `statistics ignore days whose calories carry no data`() {
        // A zero-kcal day is NO_DATA, not a day on which nothing was burned.
        val statistics = caloriesStatistics(
            CaloriesUiState(
                nutrition = listOf(nutrition(day, 0.0), nutrition(day.minusDays(1), 2_000.0)),
                dailySteps = listOf(steps(day, null), steps(day.minusDays(1), 400.0)),
            )
        )

        assertEquals(2_000.0, statistics.totalSum!!, 0.01)
        assertEquals(2_000.0, statistics.totalAverage!!, 0.01)
        assertEquals(400.0, statistics.activeSum!!, 0.01)
        assertEquals(400.0, statistics.activeAverage!!, 0.01)
    }

    @Test
    fun `statistics average over the days that reported, not the days in the window`() {
        val statistics = caloriesStatistics(
            CaloriesUiState(
                nutrition = listOf(nutrition(day, 2_000.0), nutrition(day.minusDays(1), 3_000.0)),
            )
        )

        assertEquals(5_000.0, statistics.totalSum!!, 0.01)
        assertEquals(2_500.0, statistics.totalAverage!!, 0.01)
    }

    @Test
    fun `a latest BMR with no entries still counts as one reading`() {
        val statistics = caloriesStatistics(CaloriesUiState(latestBmrKcal = 1_700.0))

        assertEquals(1, statistics.bmrReadingCount)
    }

    @Test
    fun `BMR entries outrank the standalone latest reading for the count`() {
        val statistics = caloriesStatistics(
            CaloriesUiState(
                bmrEntries = listOf(bmr(day, 1_700.0), bmr(day.minusDays(1), 1_690.0)),
                latestBmrKcal = 1_700.0,
            )
        )

        assertEquals(2, statistics.bmrReadingCount)
    }

    // Breakdown rows.

    @Test
    fun `breakdown rows are empty when nothing was recorded`() {
        assertTrue(caloriesBreakdownRows(CaloriesUiState(), period).isEmpty())
    }

    @Test
    fun `breakdown merges total active and BMR onto one row per day`() {
        val state = CaloriesUiState(
            nutrition = listOf(nutrition(day, 2_400.0)),
            dailySteps = listOf(steps(day, 500.0)),
            bmrEntries = listOf(bmr(day, 1_700.0)),
        )

        val row = caloriesBreakdownRows(state, period).single()

        assertEquals(day, row.date)
        assertEquals(2_400.0, row.totalCaloriesKcal!!, 0.01)
        assertEquals(500.0, row.activeCaloriesKcal!!, 0.01)
        assertEquals(1_700.0, row.bmrKcal!!, 0.01)
    }

    @Test
    fun `breakdown keeps the newest BMR entry of a day`() {
        val earlier = BmrEntry(
            time = day.atTime(6, 0).atZone(ZoneId.systemDefault()).toInstant(),
            kcalPerDay = 1_600.0,
            source = "test",
        )
        val state = CaloriesUiState(
            nutrition = listOf(nutrition(day, 2_400.0)),
            bmrEntries = listOf(earlier, bmr(day, 1_750.0)),
        )

        assertEquals(1_750.0, caloriesBreakdownRows(state, period).single().bmrKcal!!, 0.01)
    }

    @Test
    fun `breakdown drops days outside the period`() {
        val outside = day.minusDays(30)
        val state = CaloriesUiState(
            nutrition = listOf(nutrition(day, 2_400.0), nutrition(outside, 2_000.0)),
        )

        val dates = caloriesBreakdownRows(state, period).map { it.date }

        assertEquals(listOf(day), dates)
    }

    @Test
    fun `a day with only a fallback BMR still produces a row with null totals`() {
        val state = CaloriesUiState(
            dailySteps = listOf(steps(day, null)),
            latestBmrEntry = bmr(day.minusDays(40), 1_680.0),
        )

        val row = caloriesBreakdownRows(state, period).single()

        assertEquals(day, row.date)
        assertNull(row.totalCaloriesKcal)
        assertNull(row.activeCaloriesKcal)
        assertEquals(1_680.0, row.bmrKcal!!, 0.01)
    }

    @Test
    fun `a day with no total no active and no BMR is not a row`() {
        val state = CaloriesUiState(dailySteps = listOf(steps(day, null)))

        assertTrue(caloriesBreakdownRows(state, period).isEmpty())
    }

    @Test
    fun `latestBmrEntry is derived where the entries are set, so a state copy keeps it`() {
        val state = CaloriesUiState(
            bmrEntries = listOf(bmr(day, 1_700.0)),
            latestBmrEntry = bmr(day, 1_700.0),
        )

        val copied = state.copy(isLoading = true)

        assertEquals(1_700.0, copied.displayBmrKcal!!, 0.01)
    }

    @Test
    fun `an instant-based BMR entry lands on its local date`() {
        val instant = Instant.parse("2026-06-10T09:30:00Z")
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val state = CaloriesUiState(
            bmrEntries = listOf(BmrEntry(time = instant, kcalPerDay = 1_710.0, source = "test")),
        )

        val row = caloriesBreakdownRows(state, DatePeriod(localDate, localDate)).single()

        assertEquals(localDate, row.date)
    }
}
