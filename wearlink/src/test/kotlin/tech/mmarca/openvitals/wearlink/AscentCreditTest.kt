package tech.mmarca.openvitals.wearlink

import org.junit.Assert.assertEquals
import org.junit.Test

class AscentCreditTest {

    @Test
    fun `ten steps carry one floor's height, and no more than was climbed`() {
        assertEquals(6.0, AscentCredit.creditedMetres(climbedMetres = 15.0, steps = 20), 1e-9)
        assertEquals(4.0, AscentCredit.creditedMetres(climbedMetres = 4.0, steps = 60), 1e-9)
        assertEquals(0.0, AscentCredit.creditedMetres(climbedMetres = 9.0, steps = 0), 1e-9)
    }

    @Test
    fun `a climb counts only as far as the steps of its minute and the one before carry it`() {
        val start = 1_793_700_000_000L - 1_793_700_000_000L % MINUTE
        val climbs = listOf(
            // 6 m with 10 + 10 steps around it: 20 steps carry 6 m, all of it.
            start + MINUTE to 6.0,
            // 3 m in an elevator, no steps: nothing.
            start + 10 * MINUTE to 3.0,
            // 9 m with 15 steps: 15 steps carry 4.5 m.
            start + 20 * MINUTE to 9.0,
        )
        val steps = listOf(start to 10, start + MINUTE to 10, start + 20 * MINUTE to 15)

        assertEquals(
            listOf(start + MINUTE to 6.0, start + 20 * MINUTE to 4.5),
            AscentCredit.creditedPerMinute(climbs, steps),
        )
    }

    @Test
    fun `a minute listed twice counts once, with its larger climb and step count`() {
        val start = 1_793_700_000_000L - 1_793_700_000_000L % MINUTE

        assertEquals(
            listOf(start to 3.0),
            AscentCredit.creditedPerMinute(listOf(start to 2.0, start to 3.0), listOf(start to 5, start to 20)),
        )
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
