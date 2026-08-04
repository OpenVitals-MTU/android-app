package tech.mmarca.openvitals.ui.components

import tech.mmarca.openvitals.core.period.DatePeriod
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodHeatmapTest {

    @Test fun `month heatmap pads leading empty weekdays`() {
        val period = DatePeriod(
            start = LocalDate.of(2026, 5, 1),
            end = LocalDate.of(2026, 5, 31),
        )

        val cells = periodMonthHeatmapCells(emptyList(), period)

        assertEquals(35, cells.size)
        assertNull(cells[0].date)
        assertNull(cells[3].date)
        assertEquals(LocalDate.of(2026, 5, 1), cells[4].date)
    }

    @Test fun `month heatmap marks future days outside loaded period`() {
        val period = DatePeriod(
            start = LocalDate.of(2026, 5, 1),
            end = LocalDate.of(2026, 5, 24),
        )

        val cells = periodMonthHeatmapCells(emptyList(), period)
        val may24 = cells.single { it.date == LocalDate.of(2026, 5, 24) }
        val may25 = cells.single { it.date == LocalDate.of(2026, 5, 25) }

        assertTrue(may24.isWithinLoadedPeriod)
        assertFalse(may25.isWithinLoadedPeriod)
    }

    @Test fun `year heatmap includes each day in the year and aggregates values by date`() {
        val period = DatePeriod(
            start = LocalDate.of(2026, 1, 1),
            end = LocalDate.of(2026, 3, 1),
        )

        val cells = periodYearHeatmapCells(
            values = listOf(
                PeriodChartValue(LocalDate.of(2026, 1, 1), 1.0),
                PeriodChartValue(LocalDate.of(2026, 1, 1), 2.0),
            ),
            period = period,
        )

        assertEquals(365, cells.size)
        assertEquals(3.0, cells.first().value, 0.0)
        assertFalse(cells.single { it.date == LocalDate.of(2026, 3, 2) }.isWithinLoadedPeriod)
    }

    @Test fun `year heatmap includes leap day`() {
        val period = DatePeriod(
            start = LocalDate.of(2024, 1, 1),
            end = LocalDate.of(2024, 12, 31),
        )

        val cells = periodYearHeatmapCells(emptyList(), period)

        assertEquals(366, cells.size)
        assertTrue(cells.any { it.date == LocalDate.of(2024, 2, 29) })
    }
}
