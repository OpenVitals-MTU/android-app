package tech.mmarca.openvitals.domain.model

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
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
    fun `a local day of insight buckets does not fit in one request`() {
        val bucketsInADay = Duration.ofDays(1).toMinutes() / HeartRateInsightBucketDuration.toMinutes()

        // A day has to be split: a whole day in one grouped-duration response is the parcel Health Connect refuses.
        assertTrue(bucketsInADay > MaxInsightAggregateBuckets)
    }

    @Test
    fun `heartRateSampleFromAggregateBucket maps bucket start and average bpm`() {
        val start = Instant.parse("2026-06-01T08:00:00Z")

        val sample = heartRateSampleFromAggregateBucket(startTime = start, avgBpm = 72)

        assertTrue(sample.time == start)
        assertTrue(sample.beatsPerMinute == 72L)
    }

    @Test
    fun `insight buckets merge across folds and truncate like BPM_AVG`() {
        val dayStart = Instant.parse("2026-06-01T00:00:00Z")
        val windows = listOf(dayStart to dayStart.plus(Duration.ofDays(1)))
        val first = HeartRateInsightBuckets(windows).apply {
            add(sample(dayStart.plusSeconds(10), 60))
            add(sample(dayStart.plusSeconds(400), 90))
        }
        val second = HeartRateInsightBuckets(windows).apply {
            add(sample(dayStart.plusSeconds(20), 61))
            // Outside every window: dropped.
            add(sample(dayStart.minusSeconds(1), 200))
        }

        val merged = HeartRateInsightBuckets(windows).apply {
            addAll(first)
            addAll(second)
        }.samples()

        assertEquals(
            listOf(dayStart to 60L, dayStart.plus(HeartRateInsightBucketDuration) to 90L),
            merged.map { it.time to it.beatsPerMinute },
        )
    }

    @Test
    fun `insight buckets keep a short window raw and restart the grid at each window`() {
        val edgeStart = Instant.parse("2026-06-01T22:03:00Z")
        val dayStart = Instant.parse("2026-06-02T00:00:00Z")
        val windows = listOf(edgeStart to dayStart, dayStart to dayStart.plus(Duration.ofHours(10)))

        val samples = HeartRateInsightBuckets(windows).apply {
            add(sample(edgeStart.plusSeconds(1), 70))
            add(sample(edgeStart.plusSeconds(2), 71))
            add(sample(dayStart.plusSeconds(301), 80))
        }.samples()

        assertEquals(
            listOf(
                edgeStart.plusSeconds(1) to 70L,
                edgeStart.plusSeconds(2) to 71L,
                dayStart.plus(HeartRateInsightBucketDuration) to 80L,
            ),
            samples.map { it.time to it.beatsPerMinute },
        )
    }

    private fun sample(time: Instant, bpm: Long) = HeartRateSample(time = time, beatsPerMinute = bpm, source = "watch")
}
