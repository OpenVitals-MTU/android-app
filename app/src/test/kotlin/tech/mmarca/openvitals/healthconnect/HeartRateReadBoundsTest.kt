package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.FakeHealthConnectClient
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.domain.model.HeartRateInsightBucketDuration

/**
 * A long window must never come back sample by sample. 28 days of a 1 Hz
 * source is 2.4 million samples, and reading them for one maximum ran the
 * app out of memory.
 */
class HeartRateReadBoundsTest {
    private val start: Instant = Instant.parse("2026-06-01T00:00:00Z")
    private val spikeAt: Instant = start.plus(Duration.ofHours(30))

    @Before
    fun setUp() {
        HealthConnectRateLimitBackoff.resetForTest()
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        HealthConnectRateLimitBackoff.resetForTest()
    }

    @Test
    fun `the maximum comes from an aggregate, over any window`() = runTest {
        val client = seeded(days = 3)

        val max = reader(client).readMaxHeartRate(start, start.plus(Duration.ofDays(3)))

        assertThat(max).isEqualTo(187L)
    }

    @Test
    fun `a day is still read sample by sample`() = runTest {
        val client = seeded(days = 3)

        val samples = reader(client).readRawHeartRateSamples(start, start.plus(Duration.ofHours(24)))

        assertThat(samples).hasSize(24 * 60)
        assertThat(client.groupByDurationRequestRanges).isEmpty()
    }

    @Test
    fun `a window past the raw limit comes back as buckets`() = runTest {
        val client = seeded(days = 3)
        val end = start.plus(Duration.ofDays(3))

        val samples = reader(client).readRawHeartRateSamples(start, end)

        val bucketsInWindow = Duration.between(start, end).dividedBy(HeartRateInsightBucketDuration)
        // The walk splits on local days. An edge window of four hours or less stays raw: 240 minute samples each.
        val rawEdgeSamples = 2 * 240L
        assertThat(samples).isNotEmpty()
        assertThat(samples.size.toLong()).isAtMost(bucketsInWindow + rawEdgeSamples)
        assertThat(samples.size).isLessThan(3 * 24 * 60 / 3)
        assertThat(client.groupByDurationRequestRanges).isNotEmpty()
    }

    /** One record per six hours, a sample a minute at 62 bpm, and one 187 bpm spike. */
    private suspend fun seeded(days: Int): AggregatingFakeHealthConnectClient {
        val inner = FakeHealthConnectClient()
        val records = (0 until days * 4).map { index ->
            val recordStart = start.plus(Duration.ofHours(6L * index))
            val recordEnd = recordStart.plus(Duration.ofHours(6))
            HeartRateRecord(
                startTime = recordStart,
                startZoneOffset = ZoneOffset.UTC,
                endTime = recordEnd,
                endZoneOffset = ZoneOffset.UTC,
                samples = generateSequence(recordStart) { it.plusSeconds(60) }
                    .takeWhile { it.isBefore(recordEnd) }
                    .map { HeartRateRecord.Sample(time = it, beatsPerMinute = if (it == spikeAt) 187L else 62L) }
                    .toList(),
                metadata = Metadata.activelyRecorded(device = Device(type = Device.TYPE_WATCH)),
            )
        }
        inner.insertRecords(records)
        return AggregatingFakeHealthConnectClient(inner)
    }

    private fun reader(client: AggregatingFakeHealthConnectClient): HeartHealthReader {
        val diagnostics = mockk<HealthConnectDiagnostics>()
        every { diagnostics.summary() } returns "test"
        return HeartHealthReader(
            HealthConnectReaderSupport(
                clientProvider = { client },
                diagnostics = diagnostics,
                rateLimitMessage = { "rate limited" },
            ),
            "tech.mmarca.openvitals",
        )
    }
}
