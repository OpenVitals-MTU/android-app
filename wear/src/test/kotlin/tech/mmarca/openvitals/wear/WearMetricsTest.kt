package tech.mmarca.openvitals.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.wearlink.WearLinkProtocol

class WearMetricsTest {

    @Test
    fun `every metric reads back what it wrote, at the value's own time`() {
        val sample = WearLinkProtocol.HeartRateSample(1_791_616_222_062, 83)
        assertEquals(sample, roundTrip(WearMetrics.HEART_RATE, sample))
        assertEquals(sample.epochMillis, WearMetrics.HEART_RATE.timeOf(sample))

        val minute = WearLinkProtocol.SleepMinute(
            epochMillis = 1_791_568_860_000,
            kind = WearLinkProtocol.MinuteKind.RAW,
            offsetSeconds = 7200,
            flags = 48,
            sampleCount = 374,
            movement10 = 0,
            bpm = 64,
            heartRateSd10 = 12,
            heartRateSamples = 6,
            meanMilliG = intArrayOf(-137, -754, 637),
            sdMilliG = intArrayOf(3, 3, 3),
            zAngleMin = 40,
            zAngleMax = 41,
            zAngleDelta10 = null,
        )
        val back = roundTrip(WearMetrics.SLEEP_MINUTES, minute)
        // SleepMinute holds arrays, so compare the lines rather than the objects.
        assertEquals(WearLinkProtocol.formatSleepMinute(minute), back?.let(WearLinkProtocol::formatSleepMinute))
        assertEquals(minute.epochMillis, WearMetrics.SLEEP_MINUTES.timeOf(minute))
    }

    @Test
    fun `a row another build wrote and this one cannot read decodes to nothing`() {
        assertNull(WearMetrics.HEART_RATE.decode("HR not-a-time 83"))
        assertNull(WearMetrics.SLEEP_MINUTES.decode("XX 1 2 3"))
    }

    @Test
    fun `keys are unique and are the capabilities the link announces`() {
        val keys = WearMetrics.ALL.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertEquals(listOf(WearLinkProtocol.CAP_HEART_RATE, WearLinkProtocol.CAP_SLEEP_MINUTES), keys)
    }

    @Test
    fun `a sample is final once taken, a minute is rewritten as it fills in`() {
        assertEquals(WearMetric.OnSameTime.KEEP, WearMetrics.HEART_RATE.onSameTime)
        assertEquals(WearMetric.OnSameTime.REPLACE, WearMetrics.SLEEP_MINUTES.onSameTime)
    }

    private fun <T> roundTrip(metric: WearMetric<T>, value: T): T? = metric.decode(metric.encode(value))
}
