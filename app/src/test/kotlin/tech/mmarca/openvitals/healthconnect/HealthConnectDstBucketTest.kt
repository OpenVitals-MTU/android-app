package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.FakeHealthConnectClient
import androidx.health.connect.client.units.Volume
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * What a daily read answers over a range that crosses a DST fall-back.
 *
 * `Duration.ofDays(1)` slicing is instant-aligned, so a range holding a 25-hour
 * local day is an hour longer than a whole number of slices, and Health Connect
 * returns that leftover hour as a clipped final bucket. Its midpoint lands on
 * the same local date as the full bucket before it — so a reader that dates
 * buckets one at a time reports the final day twice, or keeps only the sliver
 * and blanks a day the user actually recorded.
 *
 * Dart counterparts: `reads > readDailySteps sums a clipped tail bucket onto its
 * date instead of overwriting it` and `reads > readDailyHydration sums same-date
 * buckets instead of keeping the last` of
 * test/data/source/health/health_connect_native_data_source_test.dart.
 *
 * The zone is pinned rather than taken from the machine: on a UTC CI box there
 * is no transition to cross and every assertion here would pass vacuously.
 */
class HealthConnectDstBucketTest {

    private lateinit var systemZone: TimeZone

    @Before
    fun setUp() {
        systemZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(ZONE))
        HealthConnectRateLimitBackoff.resetForTest()
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(systemZone)
        unmockkStatic(Log::class)
    }

    @Test
    fun `the range really does hand back a clipped tail bucket`() = onARealClock {
        // The premise every other case here rests on. If Health Connect ever
        // stopped clipping, these tests would pass while proving nothing.
        val client = seeded(
            steps(500, at(LAST, 12), at(LAST, 13)),
            steps(120, at(LAST, 23), at(LAST, 23, minutes = 45)),
        )
        val buckets = client.aggregateGroupByDuration(
            androidx.health.connect.client.request.AggregateGroupByDurationRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = androidx.health.connect.client.time.TimeRangeFilter.between(
                    FIRST.atStartOfDay(zone).toInstant(),
                    LAST.plusDays(1).atStartOfDay(zone).toInstant(),
                ),
                timeRangeSlicer = java.time.Duration.ofDays(1),
            ),
        )
        val tail = buckets.last()
        assertThat(java.time.Duration.between(tail.startTime, tail.endTime))
            .isLessThan(java.time.Duration.ofDays(1))
        assertThat(dayBucketDate(tail.startTime, tail.endTime, zone)).isEqualTo(LAST)
        assertThat(buckets.count { dayBucketDate(it.startTime, it.endTime, zone) == LAST })
            .isEqualTo(2)
    }

    @Test
    fun `readDailySteps sums a clipped tail bucket onto its date instead of duplicating it`() =
        onARealClock {
            val client = seeded(
                steps(1_000, at(FIRST, 10), at(FIRST, 11)),
                steps(500, at(LAST, 12), at(LAST, 13)),
                // Inside the leftover hour that becomes its own bucket.
                steps(120, at(LAST, 23), at(LAST, 23, minutes = 45)),
            )

            val days = activity(client).readDailySteps(FIRST, LAST)

            assertThat(days.map { it.date }).containsNoDuplicates()
            assertThat(days.single { it.date == LAST }.steps).isEqualTo(620)
            assertThat(days.single { it.date == FIRST }.steps).isEqualTo(1_000)
        }

    @Test
    fun `readDailyHydration sums same-date buckets instead of keeping the last`() = onARealClock {
        val client = seeded(
            hydration(1.5, at(LAST, 12), at(LAST, 13)),
            hydration(0.2, at(LAST, 23), at(LAST, 23, minutes = 45)),
        )

        val days = hydration(client).readDailyHydration(FIRST, LAST)

        assertThat(days.map { it.date }).containsNoDuplicates()
        assertThat(days.single { it.date == LAST }.liters).isWithin(1e-9).of(1.7)
    }

    @Test
    fun `a daily heart summary spans its buckets rather than reporting the last sliver`() =
        onARealClock {
            val client = seeded(
                heartRate(at(LAST, 12), 60),
                heartRate(at(LAST, 13), 80),
                // A single high sample in the tail hour: it must raise the day's
                // maximum without becoming the day's average on its own.
                heartRate(at(LAST, 23), 150),
            )

            val days = heart(client).readDailyHeartRateSummaries(FIRST, LAST)

            val day = days.single { it.date == LAST }
            assertThat(day.maxBpm).isEqualTo(150)
            assertThat(day.minBpm).isEqualTo(60)
            // Hour-sliced and duration-weighted: three recorded hours (60, 80,
            // 150 — the last inside the DST leftover hour) count once each, so
            // the tail sample is a third of the day's average, neither the whole
            // of it nor drowned by an all-day bucket.
            assertThat(day.avgBpm).isIn(96L..97L)
        }

    private fun onARealClock(body: suspend CoroutineScope.() -> Unit) = runBlocking(block = body)

    private val zone: ZoneId get() = ZoneId.of(ZONE)
    private val watch = Device(type = Device.TYPE_WATCH)

    private fun at(date: LocalDate, hour: Long, minutes: Long = 0): Instant =
        date.atStartOfDay(zone).toInstant().plusSeconds(hour * 3_600 + minutes * 60)

    private fun steps(count: Long, start: Instant, end: Instant) = StepsRecord(
        startTime = start,
        startZoneOffset = null,
        endTime = end,
        endZoneOffset = null,
        count = count,
        metadata = Metadata.autoRecorded(watch),
    )

    private fun hydration(liters: Double, start: Instant, end: Instant) = HydrationRecord(
        startTime = start,
        startZoneOffset = null,
        endTime = end,
        endZoneOffset = null,
        volume = Volume.liters(liters),
        metadata = Metadata.autoRecorded(watch),
    )

    private fun heartRate(time: Instant, bpm: Long) = HeartRateRecord(
        startTime = time,
        startZoneOffset = null,
        endTime = time.plusSeconds(60),
        endZoneOffset = null,
        samples = listOf(HeartRateRecord.Sample(time = time, beatsPerMinute = bpm)),
        metadata = Metadata.autoRecorded(watch),
    )

    private suspend fun seeded(vararg records: Record): AggregatingFakeHealthConnectClient {
        val client = FakeHealthConnectClient()
        client.setPackageName(APP_PACKAGE)
        if (records.isNotEmpty()) client.insertRecords(records.toList())
        return AggregatingFakeHealthConnectClient(client)
    }

    private fun support(client: HealthConnectClient): HealthConnectReaderSupport {
        val diagnostics = mockk<HealthConnectDiagnostics>()
        every { diagnostics.summary() } returns "test"
        return HealthConnectReaderSupport(
            clientProvider = { client },
            diagnostics = diagnostics,
            rateLimitMessage = { "rate limited" },
        )
    }

    private fun activity(client: HealthConnectClient) =
        ActivityHealthReader(support(client), APP_PACKAGE)

    private fun hydration(client: HealthConnectClient) =
        HydrationHealthReader(support(client), APP_PACKAGE)

    private fun heart(client: HealthConnectClient) = HeartHealthReader(support(client), "tech.mmarca.openvitals")

    private companion object {
        const val APP_PACKAGE = "tech.mmarca.openvitals"

        /** Madrid fell back on 2025-10-26, making that local day 25 hours long. */
        const val ZONE = "Europe/Madrid"
        val FIRST: LocalDate = LocalDate.of(2025, 10, 25)
        val LAST: LocalDate = LocalDate.of(2025, 10, 27)
    }
}
