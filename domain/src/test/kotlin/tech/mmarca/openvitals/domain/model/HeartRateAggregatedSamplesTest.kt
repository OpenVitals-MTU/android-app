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
    fun `insight buckets are five minutes or finer`() {
        assertTrue(HeartRateInsightBucketDuration.toMinutes() <= 5)
    }

    @Test
    fun `a fortnight of insight buckets is at most a dozen requests, and one request stays inside the parcel`() {
        val bucketsInAFortnight = Duration.ofDays(14).toMinutes() / HeartRateInsightBucketDuration.toMinutes()
        val requests = (bucketsInAFortnight + MaxInsightAggregateBuckets - 1) / MaxInsightAggregateBuckets

        // The dashboard's weekly cardio walk is the dearest read on a cold install; it must not cost thirty calls.
        assertTrue(requests <= 12)
        // 360 buckets of four metrics were measured safe for the Binder parcel; an insight bucket carries one.
        assertTrue(MaxInsightAggregateBuckets <= 360L)
    }

    @Test
    fun `heartRateSampleFromAggregateBucket maps bucket start and average bpm`() {
        val start = Instant.parse("2026-06-01T08:00:00Z")

        val sample = heartRateSampleFromAggregateBucket(startTime = start, avgBpm = 72)

        assertTrue(sample.time == start)
        assertTrue(sample.beatsPerMinute == 72L)
    }
}
