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
    private fun climb(counter: AscentMinuteCounter, from: Long, vararg metres: Double): List<WearLinkProtocol.AscentMinute> {
        val rows = ArrayList<WearLinkProtocol.AscentMinute>()
        metres.forEachIndexed { bucket, altitude ->
            repeat(10) { second ->
                counter.onPressure(from + bucket * 10_000L + second * 1000L, pressureAt(altitude))?.let { rows += it }
            }
        }
        return rows
    }

    @Test
    fun `the altitude formula matches the standard atmosphere`() {
        assertEquals(0.0, AscentMinuteCounter.altitudeMetres(1013.25), 0.01)
        assertEquals(100.0, AscentMinuteCounter.altitudeMetres(pressureAt(100.0).toDouble()), 0.1)
    }

    @Test
    fun `a rise of 3 m or more counts with all its metres, added up within the minute`() {
        val rows = climb(AscentMinuteCounter(null), start, 100.0, 103.2, 106.5, 107.0, 107.0)
        // 3.2 m, then 3.3 m more; the last 0.5 m stays below the dead band.
        assertEquals(listOf(32, 65), rows.map { it.decimetres })
        assertEquals(start, rows.last().epochMillis)
    }

    @Test
    fun `less than 3 m, or going down and up again, climbs nothing`() {
        assertEquals(emptyList<WearLinkProtocol.AscentMinute>(), climb(AscentMinuteCounter(null), start, 100.0, 102.5, 100.0, 102.5, 100.5))
    }

    @Test
    fun `going down moves the low point, so climbing back up counts from there`() {
        val rows = climb(AscentMinuteCounter(null), start, 100.0, 94.0, 97.2, 97.2)
        assertEquals(listOf(32), rows.map { it.decimetres })
    }

    @Test
    fun `a restart inside a minute adds to its stored row`() {
        val stored = WearLinkProtocol.AscentMinute(start, 20)
        val rows = climb(AscentMinuteCounter(stored), start, 100.0, 103.2, 103.2)
        assertEquals(52, rows.last().decimetres)
    }

    @Test
    fun `after a gap the weather may have moved, so the next reading starts a new low point`() {
        val counter = AscentMinuteCounter(null)
        climb(counter, start, 100.0, 100.0)
        val rows = climb(counter, start + 10 * 60_000L, 104.0, 104.0, 104.0)
        assertEquals(emptyList<WearLinkProtocol.AscentMinute>(), rows)
    }

    @Test
    fun `a broken reading is ignored`() {
        assertNull(AscentMinuteCounter(null).onPressure(start, Float.NaN))
        assertNull(AscentMinuteCounter(null).onPressure(start, 0f))
    }
}
