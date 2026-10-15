package tech.mmarca.openvitals.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BleRecordingSampleBufferTest {
    @Test
    fun trimmed_keepsLatestSamplesPerSeries() {
        val start = Instant.parse("2024-01-01T12:00:00Z")
        var buffer = BleRecordingSampleBuffer()
        repeat(5) { index ->
            buffer = buffer.withHeartRateSample(start.plusSeconds(index.toLong()), (100 + index).toLong())
        }
        val trimmed = buffer.trimmed(maxSamplesPerSeries = 3)
        assertEquals(3, trimmed.heartRateSamples.size)
        assertEquals(104L, trimmed.heartRateSamples.last().beatsPerMinute)
    }

    @Test
    fun isEmpty_whenNoSamples() {
        assertTrue(BleRecordingSampleBuffer().isEmpty())
    }
}
