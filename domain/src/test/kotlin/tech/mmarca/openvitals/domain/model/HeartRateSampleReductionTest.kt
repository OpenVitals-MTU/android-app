package tech.mmarca.openvitals.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateSampleReductionTest {
    @Test
    fun `reducedForChart keeps small lists unchanged`() {
        val samples = listOf(
            HeartRateSample(Instant.ofEpochSecond(0), 60, "test"),
            HeartRateSample(Instant.ofEpochSecond(60), 70, "test"),
        )

        assertEquals(samples, samples.reducedForChart())
    }

    @Test
    fun `reducedForChart caps large lists`() {
        val samples = (0 until 10_000).map { index ->
            HeartRateSample(
                time = Instant.ofEpochSecond(index.toLong()),
                beatsPerMinute = (60 + index % 40).toLong(),
                source = "test",
            )
        }

        val reduced = samples.reducedForChart(maxSamples = 100)

        assertEquals(100, reduced.size)
        assertTrue(reduced.first().time <= reduced.last().time)
    }
}
