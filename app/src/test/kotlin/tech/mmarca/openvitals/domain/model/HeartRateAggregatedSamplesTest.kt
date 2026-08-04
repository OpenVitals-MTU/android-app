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
    fun `heartRateSampleFromAggregateBucket maps bucket start and average bpm`() {
        val start = Instant.parse("2026-06-01T08:00:00Z")

        val sample = heartRateSampleFromAggregateBucket(startTime = start, avgBpm = 72)

        assertTrue(sample.time == start)
        assertTrue(sample.beatsPerMinute == 72L)
    }
}
