package tech.mmarca.openvitals.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import java.time.LocalDate

class PeriodMonthHeatmapCellsTest {

    private fun cellsFor(period: DatePeriod, rolling: Boolean) =
        periodMonthHeatmapCells(values = emptyList(), period = period, rolling = rolling)

    @Test
    fun `a calendar month draws the whole month`() {
        val period = DatePeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30))

        val dates = cellsFor(period, rolling = false).mapNotNull { it.date }

        assertEquals(LocalDate.of(2026, 6, 1), dates.first())
        assertEquals(LocalDate.of(2026, 6, 30), dates.last())
        assertEquals(30, dates.size)
    }

    @Test
    fun `a rolling window spans the month boundary instead of one calendar month`() {
        // "Last 30 days" ending 20 Jun starts 22 May. Drawing only May left the June half invisible.
        val period = DatePeriod(LocalDate.of(2026, 5, 22), LocalDate.of(2026, 6, 20))

        val dates = cellsFor(period, rolling = true).mapNotNull { it.date }

        assertEquals(LocalDate.of(2026, 5, 22), dates.first())
        assertEquals(LocalDate.of(2026, 6, 20), dates.last())
        assertEquals(30, dates.size)
        assertTrue(dates.contains(LocalDate.of(2026, 6, 1)))
    }

    @Test
    fun `a rolling window keeps the values on both sides of the month boundary`() {
        // 21 Jun to 20 Jul crosses the month boundary; the July value used to be dropped.
        val period = DatePeriod(LocalDate.of(2026, 6, 21), LocalDate.of(2026, 7, 20))

        val cells = periodMonthHeatmapCells(
            values = listOf(
                PeriodChartValue(LocalDate.of(2026, 6, 25), 1800.0), // first month
                PeriodChartValue(LocalDate.of(2026, 7, 5), 2200.0), // second month
            ),
            period = period,
            rolling = true,
        ).filter { it.date != null }

        // The grid is exactly the 30-day window — not one calendar month of it.
        assertEquals(LocalDate.of(2026, 6, 21), cells.first().date)
        assertEquals(LocalDate.of(2026, 7, 20), cells.last().date)
        assertEquals(30, cells.size)
        assertTrue(cells.all { it.isWithinLoadedPeriod })
        assertEquals(1800.0, cells.single { it.date == LocalDate.of(2026, 6, 25) }.value, 0.0)
        assertEquals(2200.0, cells.single { it.date == LocalDate.of(2026, 7, 5) }.value, 0.0)
        // Nothing before the window sneaks in.
        assertFalse(cells.any { it.date!!.isBefore(LocalDate.of(2026, 6, 21)) })
    }

    @Test
    fun `a rolling window marks every drawn day as loaded`() {
        val period = DatePeriod(LocalDate.of(2026, 5, 22), LocalDate.of(2026, 6, 20))

        val cells = cellsFor(period, rolling = true).filter { it.date != null }

        assertTrue(cells.all { it.isWithinLoadedPeriod })
    }

    @Test
    fun `a calendar month greys the days outside the loaded window on both sides`() {
        // A part-month window: only 10-20 June are loaded.
        val period = DatePeriod(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 20))

        val cells = cellsFor(period, rolling = false).filter { it.date != null }
        val loaded = cells.filter { it.isWithinLoadedPeriod }.mapNotNull { it.date }

        assertEquals(LocalDate.of(2026, 6, 10), loaded.first())
        assertEquals(LocalDate.of(2026, 6, 20), loaded.last())
        // The 9th precedes the window; it used to count as loaded.
        assertFalse(cells.single { it.date == LocalDate.of(2026, 6, 9) }.isWithinLoadedPeriod)
    }

    @Test
    fun `the grid always fills whole Monday-to-Sunday weeks`() {
        val period = DatePeriod(LocalDate.of(2026, 5, 22), LocalDate.of(2026, 6, 20))

        val rolling = cellsFor(period, rolling = true)
        val calendar = cellsFor(DatePeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)), rolling = false)

        assertEquals(0, rolling.size % 7)
        assertEquals(0, calendar.size % 7)
    }
}
