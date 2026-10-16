package tech.mmarca.openvitals.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** A weigh-in arrives in pieces, in any order, with repeats. */
class ScaleWeighInTest {

    private val weight = ScaleReading(weightKg = 69.9)
    private val weightWithImpedance = ScaleReading(weightKg = 69.9, impedanceLowOhm = 543.2)
    private val weightComplete = ScaleReading(weightKg = 69.9, heartRateBpm = 92, impedanceLowOhm = 543.2)
    private val secondImpedance = ScaleReading(impedanceHighOhm = 497.6)
    private val complete = ScaleReading(69.9, 92, 543.2, 497.6)

    private fun weighIn(reading: ScaleReading, updatedMillis: Long = 1_000, writtenMillis: Long? = null) =
        ScaleWeighIn(
            scaleTimestamp = 1744250605,
            profile = 1,
            time = Instant.ofEpochSecond(1744250605),
            reading = reading,
            updatedMillis = updatedMillis,
            writtenMillis = writtenMillis,
        )

    @Test
    fun `the pieces add up to the same weigh-in in either order`() {
        val weightFirst = weighIn(weight)
            .mergedWith(weightWithImpedance, 2_000)
            .mergedWith(weightComplete, 3_000)
            .mergedWith(secondImpedance, 4_000)
        val impedanceFirst = weighIn(secondImpedance).mergedWith(weightComplete, 2_000)

        assertEquals(complete, weightFirst.reading)
        assertEquals(complete, impedanceFirst.reading)
        assertEquals(4_000, weightFirst.updatedMillis)
    }

    @Test
    fun `a repeat, or a piece that says less, changes nothing`() {
        val full = weighIn(complete)

        assertSame(full, full.mergedWith(weightComplete, 2_000))
        assertSame(full, full.mergedWith(weight, 2_000))
        assertSame(full, full.mergedWith(ScaleReading(), 2_000))
    }

    @Test
    fun `the revision rises with every change, even when the clock does not`() {
        val grown = weighIn(weight, updatedMillis = 5_000).mergedWith(weightWithImpedance, nowMillis = 4_000)

        assertEquals(5_001, grown.updatedMillis)
    }

    @Test
    fun `a weigh-in waits for Health Connect until its current state is written`() {
        assertFalse("no weight yet, so nothing to write", weighIn(secondImpedance).isPending)
        assertTrue(weighIn(weight).isPending)
        assertFalse(weighIn(weight, updatedMillis = 1_000, writtenMillis = 1_000).isPending)
        assertTrue("it grew after the write", weighIn(complete, updatedMillis = 2_000, writtenMillis = 1_000).isPending)
    }
}
