package tech.mmarca.openvitals.wear.features.measure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeasureMathTest {

    @Test
    fun settledHeartRate_needsEnoughValidReadings() {
        assertNull(settledHeartRate(List(HeartRateReadingsNeeded - 1) { 70.0 }))
        // Zero means "no signal" and does not count.
        assertNull(settledHeartRate(List(HeartRateReadingsNeeded - 1) { 70.0 } + 0.0))
    }

    @Test
    fun settledHeartRate_isMedianOfLastFive_soOneStrayReadingDoesNotMoveIt() {
        val readings = listOf(90.0, 88.0, 85.0, 72.0, 71.0, 140.0, 70.0, 72.0)
        assertEquals(72.0, settledHeartRate(readings)!!, 0.0)
    }

    @Test
    fun rrIntervals_areMillisecondsBetweenBeats() {
        val beats = listOf(0L, 800_000_000L, 1_650_000_000L)
        assertEquals(listOf(800.0, 850.0), rrIntervalsMillis(beats))
    }

    @Test
    fun rmssd_ofAlternatingIntervals_isTheirDifference() {
        val rr = List(HrvIntervalsNeeded + 1) { if (it % 2 == 0) 800.0 else 840.0 }
        assertEquals(40.0, rmssd(rr)!!, 1e-9)
    }

    @Test
    fun rmssd_dropsImpossibleAndEctopicIntervals() {
        val steady = List(HrvIntervalsNeeded + 1) { if (it % 2 == 0) 800.0 else 840.0 }
        val noisy = steady.take(10) + 250.0 + 1_400.0 + steady.drop(10)
        assertEquals(40.0, rmssd(noisy)!!, 1e-9)
    }

    @Test
    fun rmssd_withTooFewBeats_isNull() {
        assertNull(rmssd(List(HrvIntervalsNeeded - 1) { 800.0 }))
    }
}
