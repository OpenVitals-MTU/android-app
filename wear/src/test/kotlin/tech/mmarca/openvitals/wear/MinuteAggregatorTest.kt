package tech.mmarca.openvitals.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.wearlink.WearLinkProtocol

/**
 * The movement count, the flags and the kind per minute, fed with synthetic
 * readings in g. The shared vector test holds the exact numbers; this one
 * holds the rules.
 */
class MinuteAggregatorTest {

    private val minute0 = 1_760_000_000_000L - 1_760_000_000_000L % 60_000L
    private var heartRateRecording = true

    private fun aggregator() = MinuteAggregator(offsetSecondsAt = { 7200 }, isHeartRateRecording = { heartRateRecording })

    /** Five readings a second of gravity alone, with sensor noise well under the jerk threshold. */
    private fun still(aggregator: MinuteAggregator, from: Long, seconds: Int) {
        for (index in 0 until seconds * 5) {
            val noise = if (index % 2 == 0) 0.002 else -0.002
            aggregator.onAcceleration(from + index * 200L, 0.0, 0.0, 1.0 + noise)
        }
    }

    /** A burst whose magnitude jumps on every reading, [count] of them. */
    private fun burst(aggregator: MinuteAggregator, from: Long, count: Int) {
        for (index in 0 until count) {
            val swing = if (index % 2 == 0) 0.3 else -0.3
            aggregator.onAcceleration(from + index * 200L, 0.0, 0.0, 1.0 + swing)
        }
    }

    private fun pulse(aggregator: MinuteAggregator, from: Long, bpm: Int = 55) {
        for (index in 0 until 6) aggregator.onHeartRate(from + index * 10_000L + 3_000L, bpm)
    }

    @Test
    fun `a still worn minute is raw with no movement and closes once the lag has passed`() {
        val aggregator = aggregator()
        still(aggregator, minute0, 60)
        pulse(aggregator, minute0)

        assertTrue(aggregator.close(minute0 + 60_000L).isEmpty())
        val closed = aggregator.close(minute0 + 90_000L).single()

        assertEquals(minute0, closed.epochMillis)
        assertEquals(WearLinkProtocol.MinuteKind.RAW, closed.kind)
        assertEquals(0, closed.movement10)
        assertEquals(300, closed.sampleCount)
        assertEquals(55, closed.bpm)
        assertEquals(6, closed.heartRateSamples)
        assertEquals(WearLinkProtocol.FLAG_HR_RECORDING, closed.flags)
        assertEquals(90, closed.zAngleMax)
        assertEquals(1000, closed.meanMilliG[2])
    }

    @Test
    fun `a turn in bed counts a short burst on the fitted scale`() {
        val aggregator = aggregator()
        still(aggregator, minute0, 30)
        burst(aggregator, minute0 + 30_000L, 20)
        still(aggregator, minute0 + 34_000L, 26)
        pulse(aggregator, minute0)

        // Nineteen jumps inside the burst, plus the one into it and the one out of it: 21 counts, 0.2 each.
        assertEquals(42, aggregator.close(minute0 + 120_000L).single().movement10)
    }

    @Test
    fun `movement is normalised to the nominal rate and saturates`() {
        val aggregator = aggregator()
        // Twenty counted jumps in a sparse minute of 100 samples count like sixty at the nominal 300.
        burst(aggregator, minute0, 100)
        pulse(aggregator, minute0)
        val sparse = aggregator.close(minute0 + 120_000L).single()

        assertEquals(300, sparse.movement10)
        assertTrue(sparse.hasFlag(WearLinkProtocol.FLAG_SPARSE))
    }

    @Test
    fun `fewer counted readings than the floor is a still minute`() {
        val aggregator = aggregator()
        still(aggregator, minute0, 10)
        // One swing: the jump into it and the jump out of it, two counts under the floor of three.
        burst(aggregator, minute0 + 10_000L, 1)
        still(aggregator, minute0 + 11_000L, 49)

        assertEquals(0, aggregator.close(minute0 + 120_000L).single().movement10)
    }

    @Test
    fun `no heart rate while recording makes the minute unmeasurable, unless recording is off`() {
        val aggregator = aggregator()
        still(aggregator, minute0, 60)
        assertEquals(WearLinkProtocol.MinuteKind.UNMEASURABLE, aggregator.close(minute0 + 120_000L).single().kind)

        heartRateRecording = false
        val quiet = aggregator()
        still(quiet, minute0, 60)
        val closed = quiet.close(minute0 + 120_000L).single()
        assertEquals(WearLinkProtocol.MinuteKind.RAW, closed.kind)
        assertNull(closed.bpm)
        assertEquals(0, closed.flags and WearLinkProtocol.FLAG_HR_RECORDING)
    }

    @Test
    fun `off the wrist and charging are sticky and make the minute unmeasurable`() {
        val aggregator = aggregator()
        aggregator.onWorn(minute0 + 10_000L, worn = false)
        still(aggregator, minute0, 60)
        pulse(aggregator, minute0)
        aggregator.onCharging(minute0 + 60_000L, charging = true)
        still(aggregator, minute0 + 60_000L, 60)
        pulse(aggregator, minute0 + 60_000L)
        aggregator.onWorn(minute0 + 120_000L, worn = true)
        aggregator.onCharging(minute0 + 120_000L, charging = false)
        still(aggregator, minute0 + 120_000L, 60)
        pulse(aggregator, minute0 + 120_000L)

        val closed = aggregator.close(minute0 + 300_000L)

        assertEquals(listOf(true, true, false), closed.map { it.hasFlag(WearLinkProtocol.FLAG_OFF_BODY) })
        assertEquals(listOf(false, true, false), closed.map { it.hasFlag(WearLinkProtocol.FLAG_CHARGING) })
        assertEquals(
            listOf(WearLinkProtocol.MinuteKind.UNMEASURABLE, WearLinkProtocol.MinuteKind.UNMEASURABLE, WearLinkProtocol.MinuteKind.RAW),
            closed.map { it.kind },
        )
    }

    @Test
    fun `a screen wake-up makes its minute awake and a lost contact only flags it`() {
        val aggregator = aggregator()
        still(aggregator, minute0, 60)
        pulse(aggregator, minute0)
        still(aggregator, minute0 + 60_000L, 60)
        pulse(aggregator, minute0 + 60_000L)
        aggregator.onScreenOn(minute0 + 70_000L)
        aggregator.onHeartRateContact(minute0 + 80_000L, contact = false)

        val closed = aggregator.close(minute0 + 300_000L)

        assertEquals(listOf(WearLinkProtocol.MinuteKind.RAW, WearLinkProtocol.MinuteKind.AWAKE), closed.map { it.kind })
        assertEquals(listOf(false, true), closed.map { it.hasFlag(WearLinkProtocol.FLAG_HR_NO_CONTACT) })
    }

    @Test
    fun `the z-angle change carries across minutes and the first minute has none`() {
        val aggregator = aggregator()
        still(aggregator, minute0, 60)
        // The watch now lies on its side: the z axis is horizontal.
        for (index in 0 until 300) aggregator.onAcceleration(minute0 + 60_000L + index * 200L, 1.0, 0.0, 0.0)

        val closed = aggregator.close(minute0 + 300_000L)

        assertEquals(0, closed[0].zAngleDelta10)
        // Eleven in-minute differences of zero and one carried jump of ninety degrees: 90 / 12 = 7.5.
        assertEquals(75, closed[1].zAngleDelta10)
        assertEquals(0, closed[1].zAngleMax)
    }

    @Test
    fun `minutes close oldest first and only once`() {
        val aggregator = aggregator()
        still(aggregator, minute0, 60)
        still(aggregator, minute0 + 60_000L, 60)
        still(aggregator, minute0 + 120_000L, 60)

        val first = aggregator.close(minute0 + 140_000L)
        val second = aggregator.close(minute0 + 300_000L)

        assertEquals(listOf(minute0), first.map { it.epochMillis })
        assertEquals(listOf(minute0 + 60_000L, minute0 + 120_000L), second.map { it.epochMillis })
        assertTrue(aggregator.close(minute0 + 600_000L).isEmpty())
    }
}
