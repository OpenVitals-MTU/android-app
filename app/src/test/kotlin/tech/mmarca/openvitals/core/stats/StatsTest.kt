package tech.mmarca.openvitals.core.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.math.roundToInt

class StatsTest {

    @Test
    fun `averageOrNull returns null for an empty list rather than NaN`() {
        assertNull(emptyList<Double>().averageOrNull())
        // The whole point: the stdlib's average() hands back NaN here.
        assertTrue(emptyList<Double>().average().isNaN())
    }

    @Test
    fun `averageOrNull averages a populated list`() {
        assertEquals(2.0, listOf(1.0, 2.0, 3.0).averageOrNull()!!, 1e-9)
    }

    @Test
    fun `averageOrNull keeps a genuine zero distinct from no data`() {
        assertEquals(0.0, listOf(0.0, 0.0).averageOrNull()!!, 1e-9)
        assertNull(emptyList<Double>().averageOrNull())
    }

    @Test
    fun `averageOrNull works for long and int samples`() {
        assertEquals(55.0, listOf(50L, 60L).averageOrNull()!!, 1e-9)
        assertNull(emptyList<Long>().averageOrNull())
        assertEquals(3.0, listOf(2, 4).averageOrNull()!!, 1e-9)
        assertNull(emptyList<Int>().averageOrNull())
    }

    @Test
    fun `averageOrZero reports zero only where zero is the answer we want`() {
        assertEquals(0.0, emptyList<Double>().averageOrZero(), 1e-9)
        assertEquals(2.5, listOf(2.0, 3.0).averageOrZero(), 1e-9)
        assertEquals(0.0, emptyList<Long>().averageOrZero(), 1e-9)
    }

    @Test
    fun `rounding an empty average throws, so it must never reach the formatter`() {
        // Kotlin's roundToInt rejects NaN outright rather than quietly yielding 0,
        // so an unguarded average of no readings takes the whole section down.
        assertThrows(IllegalArgumentException::class.java) {
            emptyList<Int>().average().roundToInt()
        }
        assertNull(emptyList<Int>().averageOrNull())
    }
}
