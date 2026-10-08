package tech.mmarca.openvitals.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The movement count and the flags per minute, fed with synthetic readings:
 * a still wrist, a turn in bed, the off-body sensor and a screen wake-up.
 */
class MinuteAggregatorTest {

    private val minute0 = 1_760_000_000_000L - 1_760_000_000_000L % 60_000L
    private val gravity = 9.81f

    /** Five readings a second of gravity alone, with sensor noise well under the jerk threshold. */
    private fun still(aggregator: MinuteAggregator, from: Long, seconds: Int) {
        for (index in 0 until seconds * 5) {
            val noise = if (index % 2 == 0) 0.02f else -0.02f
            aggregator.onAcceleration(from + index * 200L, 0f, 0f, gravity + noise)
        }
    }

    /** A burst of readings whose magnitude swings far enough to count, [count] of them. */
    private fun burst(aggregator: MinuteAggregator, from: Long, count: Int) {
        for (index in 0 until count) {
            val swing = if (index % 2 == 0) 3f else -3f
            aggregator.onAcceleration(from + index * 200L, 0f, 0f, gravity + swing)
        }
    }

    @Test
    fun `a still minute counts nothing and closes once the lag has passed`() {
        val aggregator = MinuteAggregator()
        still(aggregator, minute0, 60)

        assertTrue(aggregator.close(minute0 + 60_000L).isEmpty())
        val closed = aggregator.close(minute0 + 90_000L)

        assertEquals(1, closed.size)
        assertEquals(minute0, closed[0].startEpochMillis)
        assertEquals(0f, closed[0].movement, 0f)
        assertFalse(closed[0].offBody)
        assertFalse(closed[0].screenOn)
    }

    @Test
    fun `a turn in bed counts a short burst on the fitted scale`() {
        val aggregator = MinuteAggregator()
        still(aggregator, minute0, 30)
        burst(aggregator, minute0 + 30_000L, 20)
        still(aggregator, minute0 + 34_000L, 26)

        val closed = aggregator.close(minute0 + 120_000L).single()

        // Nineteen jumps inside the burst, plus the one into it and the one out of it: 21 counts, 0.2 each.
        assertEquals(4.2f, closed.movement, 0.01f)
    }

    @Test
    fun `a minute of constant motion saturates`() {
        val aggregator = MinuteAggregator()
        burst(aggregator, minute0, 300)

        assertEquals(30f, aggregator.close(minute0 + 120_000L).single().movement, 0f)
    }

    @Test
    fun `fewer counted readings than the floor is a still minute`() {
        val aggregator = MinuteAggregator()
        still(aggregator, minute0, 10)
        // One swing: the jump into it and the jump out of it, two counts under the floor of three.
        burst(aggregator, minute0 + 10_000L, 1)
        still(aggregator, minute0 + 11_000L, 49)

        assertEquals(0f, aggregator.close(minute0 + 120_000L).single().movement, 0f)
    }

    @Test
    fun `off the wrist marks the minute and sticks until worn again`() {
        val aggregator = MinuteAggregator()
        aggregator.onWorn(minute0 + 10_000L, worn = false)
        still(aggregator, minute0, 60)
        still(aggregator, minute0 + 60_000L, 60)
        aggregator.onWorn(minute0 + 120_000L, worn = true)
        still(aggregator, minute0 + 120_000L, 60)

        val closed = aggregator.close(minute0 + 300_000L)

        assertEquals(listOf(true, true, false), closed.map { it.offBody })
    }

    @Test
    fun `a screen wake-up flags its minute only`() {
        val aggregator = MinuteAggregator()
        still(aggregator, minute0, 60)
        still(aggregator, minute0 + 60_000L, 60)
        aggregator.onScreenOn(minute0 + 70_000L)

        val closed = aggregator.close(minute0 + 300_000L)

        assertEquals(listOf(false, true), closed.map { it.screenOn })
    }

    @Test
    fun `minutes close oldest first and only once`() {
        val aggregator = MinuteAggregator()
        still(aggregator, minute0, 60)
        still(aggregator, minute0 + 60_000L, 60)
        still(aggregator, minute0 + 120_000L, 60)

        val first = aggregator.close(minute0 + 140_000L)
        val second = aggregator.close(minute0 + 300_000L)

        assertEquals(listOf(minute0), first.map { it.startEpochMillis })
        assertEquals(listOf(minute0 + 60_000L, minute0 + 120_000L), second.map { it.startEpochMillis })
        assertTrue(aggregator.close(minute0 + 600_000L).isEmpty())
    }
}
