package tech.mmarca.openvitals.core.stats

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TimeBucketedAverageTest {

    private data class Timed(val time: Instant, val value: Double)

    private val dayStart: Instant = Instant.parse("2026-09-01T00:00:00Z")

    private fun at(minute: Long, second: Long = 0, value: Double) =
        Timed(dayStart.plusSeconds(minute * 60 + second), value)

    private fun List<Timed>.bucketed(bucket: Duration = Duration.ofMinutes(1)): Double? =
        timeBucketedAverageOrNull(time = { it.time }, value = { it.value }, bucket = bucket)

    @Test
    fun `no samples has no average`() {
        assertNull(emptyList<Timed>().bucketed())
    }

    @Test
    fun `a single sample is its own average`() {
        assertEquals(72.0, listOf(at(minute = 30, value = 72.0)).bucketed()!!, 1e-9)
    }

    @Test
    fun `uniform once-a-minute sampling reduces to the plain mean`() {
        val samples = listOf(
            at(minute = 0, value = 60.0),
            at(minute = 1, value = 70.0),
            at(minute = 2, value = 80.0),
        )
        assertEquals(70.0, samples.bucketed()!!, 1e-9)
    }

    @Test
    fun `samples inside one minute average together before counting once`() {
        val samples = listOf(
            at(minute = 0, second = 10, value = 80.0),
            at(minute = 0, second = 40, value = 100.0),
            at(minute = 1, value = 60.0),
        )
        // (90 + 60) / 2, not (80 + 100 + 60) / 3.
        assertEquals(75.0, samples.bucketed()!!, 1e-9)
    }

    @Test
    fun `empty minutes are skipped not interpolated`() {
        val samples = listOf(
            at(minute = 0, value = 90.0),
            at(minute = 500, value = 96.0),
        )
        // A lone spot check hours away still counts as exactly one bucket.
        assertEquals(93.0, samples.bucketed()!!, 1e-9)
    }

    /**
     * The field report that motivated this function: Gadgetbridge wrote a day
     * of per-minute background heart rate around 76 bpm plus a ~50-minute
     * workout at 1 Hz around 133 bpm. Gadgetbridge and Google Fit called the
     * day 79 bpm; the per-sample mean called it 115.
     */
    @Test
    fun `a 1 Hz workout does not outvote the per-minute background series`() {
        val background = (0L until 1390L).map { minute -> at(minute = minute, value = 76.0) }
        val workout = (0L until 50L).flatMap { minute ->
            (0L until 60L).map { second ->
                at(minute = 1390L + minute, second = second, value = 133.0)
            }
        }
        val day = background + workout

        // Per-sample: 3000 workout samples against 1390 background ones.
        assertEquals(115.0, day.map { it.value }.average(), 0.5)
        // Per-minute: 50 workout minutes against 1390 background ones.
        assertEquals(78.0, day.bucketed()!!, 0.5)
    }

    @Test
    fun `a custom bucket width groups accordingly`() {
        val samples = listOf(
            at(minute = 0, value = 60.0),
            at(minute = 2, value = 80.0),
            at(minute = 7, value = 100.0),
        )
        // Five-minute buckets: {60, 80} then {100}.
        assertEquals(85.0, samples.bucketed(Duration.ofMinutes(5))!!, 1e-9)
    }

    @Test
    fun `a non-positive bucket is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            listOf(at(minute = 0, value = 60.0)).bucketed(Duration.ZERO)
        }
    }
}
