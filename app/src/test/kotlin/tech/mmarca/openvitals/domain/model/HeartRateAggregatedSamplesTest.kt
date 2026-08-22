package tech.mmarca.openvitals.domain.model

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateAggregatedSamplesTest {
    @Test
    fun `shouldUseAggregatedHeartRateSamples is true for day ranges`() {
        assertTrue(shouldUseAggregatedHeartRateSamples(Duration.ofHours(24)))
    }

    @Test
    fun `shouldUseAggregatedHeartRateSamples is false for workout-length ranges`() {
        assertFalse(shouldUseAggregatedHeartRateSamples(Duration.ofHours(2)))
        assertFalse(shouldUseAggregatedHeartRateSamples(HeartRateRawSampleMaxRange))
    }

    @Test
    fun `insight buckets are denser than chart buckets`() {
        assertTrue(HeartRateInsightBucketDuration < HeartRateChartBucketDuration)
        assertTrue(HeartRateInsightBucketDuration.toMinutes() <= 5)
    }

    @Test
    fun `a local day of insight buckets does not fit in one request`() {
        val bucketsInADay = Duration.ofDays(1).toMinutes() / HeartRateInsightBucketDuration.toMinutes()

        // The point of the budget: a day has to be SPLIT. Raising the bucket
        // budget past a day's worth would put the whole day back into one
        // grouped-duration response, which is the parcel Health Connect
        // refuses to hand back on a densely recorded phone.
        assertTrue(bucketsInADay > MaxInsightAggregateBuckets)
    }

    @Test
    fun `heartRateSampleFromAggregateBucket maps bucket start and average bpm`() {
        val start = Instant.parse("2026-06-01T08:00:00Z")

        val sample = heartRateSampleFromAggregateBucket(startTime = start, avgBpm = 72)

        assertTrue(sample.time == start)
        assertTrue(sample.beatsPerMinute == 72L)
    }
}
