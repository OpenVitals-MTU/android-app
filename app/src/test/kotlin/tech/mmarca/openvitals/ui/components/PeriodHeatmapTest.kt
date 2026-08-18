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

        assertEquals(365, cells.count { it.date != null })
        assertEquals(0, cells.size % 7)
        // 2026-01-01 is a Thursday: three fillers keep the columns Monday-aligned.
        assertNull(cells[0].date)
        assertEquals(LocalDate.of(2026, 1, 1), cells[3].date)
        assertEquals(3.0, cells[3].value, 0.0)
        assertFalse(cells.single { it.date == LocalDate.of(2026, 3, 2) }.isWithinLoadedPeriod)
    }

    @Test fun `year heatmap includes leap day`() {
        val period = DatePeriod(
            start = LocalDate.of(2024, 1, 1),
            end = LocalDate.of(2024, 12, 31),
        )

        val cells = periodYearHeatmapCells(emptyList(), period)

        assertEquals(366, cells.count { it.date != null })
        assertEquals(0, cells.size % 7)
        assertTrue(cells.any { it.date == LocalDate.of(2024, 2, 29) })
    }

    @Test fun `rolling year heatmap spans the loaded window across both calendar years`() {
        val period = DatePeriod(
            start = LocalDate.of(2025, 8, 19),
            end = LocalDate.of(2026, 8, 18),
        )

        val cells = periodYearHeatmapCells(
            values = listOf(PeriodChartValue(LocalDate.of(2026, 8, 10), 5.0)),
            period = period,
            rolling = true,
        )
        val dates = cells.mapNotNull { it.date }

        assertEquals(365, dates.size)
        assertEquals(LocalDate.of(2025, 8, 19), dates.first())
        assertEquals(LocalDate.of(2026, 8, 18), dates.last())
        val sessionDay = cells.single { it.date == LocalDate.of(2026, 8, 10) }
        assertEquals(5.0, sessionDay.value, 0.0)
        assertTrue(sessionDay.isWithinLoadedPeriod)
    }

    @Test fun `year heatmap month labels sit on the columns containing each month's first day`() {
        val period = DatePeriod(
            start = LocalDate.of(2026, 1, 1),
            end = LocalDate.of(2026, 12, 31),
        )

        val weeks = periodYearHeatmapCells(emptyList(), period).chunked(7)
        val labels = yearHeatmapMonthStartColumns(weeks)

        assertEquals(12, labels.size)
        assertEquals(0 to LocalDate.of(2026, 1, 1), labels.first())
        assertTrue(labels.all { (column, monthStart) ->
            monthStart.dayOfMonth == 1 && weeks[column].any { it.date == monthStart }
        })
    }
}
