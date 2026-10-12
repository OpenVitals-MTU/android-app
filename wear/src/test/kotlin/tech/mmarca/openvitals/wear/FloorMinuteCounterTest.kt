package tech.mmarca.openvitals.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.wearlink.WearLinkProtocol

class FloorMinuteCounterTest {

    private val start = 1_791_568_860_000L

    /** The pressure at [metres] above sea level in the standard atmosphere. */
    private fun pressureAt(metres: Double): Float =
        (1013.25 * Math.pow(1.0 - metres / 44_330.0, 5.255)).toFloat()

    /** One reading a second at the given altitudes, ten seconds each; the rows the counter returned. */
    private fun climb(counter: FloorMinuteCounter, from: Long, vararg metres: Double): List<WearLinkProtocol.FloorMinute> {
        val rows = ArrayList<WearLinkProtocol.FloorMinute>()
        metres.forEachIndexed { bucket, altitude ->
            repeat(10) { second ->
                counter.onPressure(from + bucket * 10_000L + second * 1000L, pressureAt(altitude))?.let { rows += it }
            }
        }
        return rows
    }

    @Test
    fun `the altitude formula matches the standard atmosphere`() {
        assertEquals(0.0, FloorMinuteCounter.altitudeMetres(1013.25), 0.01)
        assertEquals(100.0, FloorMinuteCounter.altitudeMetres(pressureAt(100.0).toDouble()), 0.1)
    }

    @Test
    fun `each 3 m of ascent is one floor, added up within the minute`() {
        val rows = climb(FloorMinuteCounter(null), start, 100.0, 103.2, 106.5, 107.0, 107.0)
        assertEquals(listOf(1, 2), rows.map { it.floors })
        assertEquals(start, rows.last().epochMillis)
    }

    @Test
    fun `less than 3 m, or going down and up again, climbs nothing`() {
        assertEquals(emptyList<WearLinkProtocol.FloorMinute>(), climb(FloorMinuteCounter(null), start, 100.0, 102.5, 100.0, 102.5, 100.5))
    }

    @Test
    fun `going down moves the low point, so climbing back up counts from there`() {
        val rows = climb(FloorMinuteCounter(null), start, 100.0, 94.0, 97.2, 97.2)
        assertEquals(listOf(1), rows.map { it.floors })
    }

    @Test
    fun `a restart inside a minute adds to its stored row`() {
        val stored = WearLinkProtocol.FloorMinute(start, 2)
        val rows = climb(FloorMinuteCounter(stored), start, 100.0, 103.2, 103.2)
        assertEquals(3, rows.last().floors)
    }

    @Test
    fun `after a gap the weather may have moved, so the next reading starts a new low point`() {
        val counter = FloorMinuteCounter(null)
        climb(counter, start, 100.0, 100.0)
        val rows = climb(counter, start + 10 * 60_000L, 104.0, 104.0, 104.0)
        assertEquals(emptyList<WearLinkProtocol.FloorMinute>(), rows)
    }

    @Test
    fun `a broken reading is ignored`() {
        assertNull(FloorMinuteCounter(null).onPressure(start, Float.NaN))
        assertNull(FloorMinuteCounter(null).onPressure(start, 0f))
    }
}
