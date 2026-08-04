package tech.mmarca.openvitals.ui.components

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BucketedSeriesTest {

    private data class Sample(val time: Instant, val value: Double)

    private val dayStart: Instant = Instant.parse("2026-01-01T00:00:00Z")

    private fun sample(minute: Int, value: Double) =
        Sample(dayStart.plus(Duration.ofMinutes(minute.toLong())), value)

    private fun run(samples: List<Sample>, bucketMinutes: Int): List<BucketPoint> =
        bucketedSeries(
            samples = samples,
            bucketMinutes = bucketMinutes,
            dayStart = dayStart,
            time = Sample::time,
            value = Sample::value,
        )

    @Test fun `empty input yields no buckets`() {
        assertTrue(run(emptyList(), 5).isEmpty())
    }

    @Test fun `non-positive bucket width yields no buckets`() {
        assertTrue(run(listOf(sample(0, 10.0)), 0).isEmpty())
        assertTrue(run(listOf(sample(0, 10.0)), -5).isEmpty())
    }

    @Test fun `computes average, min and max per bucket`() {
        // 0-5 min bucket: 60, 80, 100 -> avg 80, min 60, max 100.
        val result = run(listOf(sample(0, 60.0), sample(2, 80.0), sample(4, 100.0)), 5)
        assertEquals(1, result.size)
        assertEquals(80.0, result.single().average, 1e-9)
        assertEquals(60.0, result.single().min, 0.0)
        assertEquals(100.0, result.single().max, 0.0)
        assertEquals(3, result.single().count)
    }

    @Test fun `splits samples into separate buckets and orders them by time`() {
        // One in [0,5), one in [10,15) — the [5,10) bucket is empty and omitted.
        val result = run(listOf(sample(1, 50.0), sample(12, 70.0)), 5)
        assertEquals(2, result.size)
        assertTrue(result[0].time.isBefore(result[1].time))
        assertEquals(50.0, result[0].average, 0.0)
        assertEquals(70.0, result[1].average, 0.0)
    }

    @Test fun `bucket centre sits in the middle of the window`() {
        val result = run(listOf(sample(1, 50.0)), 10)
        assertEquals(dayStart.plus(Duration.ofMinutes(5)), result.single().time)
    }

    @Test fun `skips samples before day start and non-finite values`() {
        val before = Sample(dayStart.minus(Duration.ofMinutes(1)), 99.0)
        val nan = sample(1, Double.NaN)
        val good = sample(2, 40.0)
        assertEquals(40.0, run(listOf(before, nan, good), 5).single().average, 0.0)
    }
}
