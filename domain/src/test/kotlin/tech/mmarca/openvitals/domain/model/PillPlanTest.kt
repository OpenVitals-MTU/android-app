package tech.mmarca.openvitals.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The pill scheme's calendar arithmetic. */
class PillPlanTest {
    private val start = LocalDate.of(2026, 7, 1)
    private val plan = PillPlan(enabled = true, activeDays = 21, pauseDays = 7, packStart = start)

    @Test fun `days count through the taking run, the pause, and the next pack`() {
        val nextPack = LocalDate.of(2026, 7, 29)
        assertEquals(PillDay(1, 21, true, nextPack), plan.dayAt(start))
        assertEquals(PillDay(21, 21, true, nextPack), plan.dayAt(LocalDate.of(2026, 7, 21)))
        assertEquals(PillDay(1, 7, false, nextPack), plan.dayAt(LocalDate.of(2026, 7, 22)))
        assertEquals(PillDay(7, 7, false, nextPack), plan.dayAt(LocalDate.of(2026, 7, 28)))
        assertEquals(PillDay(1, 21, true, LocalDate.of(2026, 8, 26)), plan.dayAt(nextPack))
    }

    @Test fun `before the first pack and without a start there is no day`() {
        assertNull(plan.dayAt(start.minusDays(1)))
        assertNull(plan.copy(packStart = null).dayAt(start))
    }

    @Test fun `a continuous scheme never pauses`() {
        val continuous = plan.copy(activeDays = 28, pauseDays = 0)
        assertTrue((0L until 60L).all { continuous.dayAt(start.plusDays(it))!!.isActive })
        assertEquals(LocalDate.of(2026, 7, 29), continuous.dayAt(start.plusDays(27))!!.nextPackStart)
    }

    @Test fun `the next taking day is today, the next pack, or the first pack`() {
        assertEquals(LocalDate.of(2026, 7, 10), plan.nextActiveDay(LocalDate.of(2026, 7, 10)))
        assertEquals(LocalDate.of(2026, 7, 29), plan.nextActiveDay(LocalDate.of(2026, 7, 25)))
        assertEquals(start, plan.nextActiveDay(start.minusDays(10)))
        assertNull(plan.copy(packStart = null).nextActiveDay(start))
    }

    @Test fun `normalizing clamps the day counts`() {
        val normalized = plan.copy(activeDays = 0, pauseDays = 40).normalized()
        assertEquals(PillPlan.ActiveDaysRange.first, normalized.activeDays)
        assertEquals(PillPlan.PauseDaysRange.last, normalized.pauseDays)
    }
}
