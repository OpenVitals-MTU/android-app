package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.records.HeartRateRecord
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

/**
 * Health Connect filters a series record by the record's bounds, not its samples.
 * A 17.48-hour HeartRateRecord with 891 samples swallowed a 36-minute workout,
 * and the windowed read came back empty. The numbers here are the real export's.
 * Only the real reader against a real client can reproduce this.
 */
class SwallowingRecordTest {

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

    private val recordStart: Instant = Instant.parse("2026-06-22T00:34:00Z")
    private val recordEnd: Instant = Instant.parse("2026-06-22T18:03:00Z")
    private val workoutStart: Instant = Instant.parse("2026-06-22T06:05:00Z")
    private val workoutEnd: Instant = Instant.parse("2026-06-22T06:41:00Z")

    /** One record, 17.48 hours long, a sample every minute — as the real one was. */
    private fun swallowingRecord(): HeartRateRecord {
        val samples = generateSequence(recordStart) { it.plusSeconds(60) }
            .takeWhile { it.isBefore(recordEnd) }
            .map { HeartRateRecord.Sample(time = it, beatsPerMinute = 62L) }
            .toList()

        return HeartRateRecord(
            startTime = recordStart,
            startZoneOffset = ZoneOffset.ofHours(3),
            endTime = recordEnd,
            endZoneOffset = ZoneOffset.ofHours(3),
            samples = samples,
            metadata = Metadata.activelyRecorded(
                device = androidx.health.connect.client.records.metadata.Device(
                    type = androidx.health.connect.client.records.metadata.Device.TYPE_WATCH,
                ),
            ),
        )
    }

    private fun reader(client: FakeHealthConnectClient): HeartHealthReader {
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

    @Test
    fun `a workout buried inside a 17 hour record still has a heart rate`() = runTest {
        val client = FakeHealthConnectClient()
        client.insertRecords(listOf(swallowingRecord()))

        val samples = reader(client).readRawHeartRateSamples(workoutStart, workoutEnd)

        assertThat(samples).isNotEmpty()
    }

    @Test
    fun `and every sample it returns is actually inside the workout`() = runTest {
        // The mirror half: once the record is returned, a naive flatMap takes all 891 samples.
        val client = FakeHealthConnectClient()
        client.insertRecords(listOf(swallowingRecord()))

        val samples = reader(client).readRawHeartRateSamples(workoutStart, workoutEnd)

        assertThat(samples.map { it.time.toEpochMilli() }).isInOrder()
        samples.forEach {
            assertThat(it.time.toEpochMilli()).isAtLeast(workoutStart.toEpochMilli())
            assertThat(it.time.toEpochMilli()).isLessThan(workoutEnd.toEpochMilli())
        }
        // 36 minutes at one sample a minute.
        assertThat(samples).hasSize(36)
    }

    // A record that straddles only the start. The read comes back full and looks healthy,
    // but the front is missing. From a hike: workout at 11:50, record 11:48-12:44, chart began at 12:44.

    private val hikeStart: Instant = Instant.parse("2026-07-12T11:50:00Z")
    private val hikeEnd: Instant = Instant.parse("2026-07-12T18:00:00Z")

    /** A record that began BEFORE the workout and runs into it. */
    private fun straddlingRecord(): HeartRateRecord = heartRecord(
        Instant.parse("2026-07-12T11:48:00Z"),
        Instant.parse("2026-07-12T12:44:00Z"),
    )

    /** And the ones that sit inside it, which the windowed read finds without help. */
    private fun recordsInsideTheHike(): List<HeartRateRecord> = listOf(
        heartRecord(Instant.parse("2026-07-12T12:44:00Z"), Instant.parse("2026-07-12T13:19:00Z")),
        heartRecord(Instant.parse("2026-07-12T13:19:00Z"), Instant.parse("2026-07-12T17:58:00Z")),
    )

    private fun heartRecord(from: Instant, to: Instant): HeartRateRecord {
        val samples = generateSequence(from) { it.plusSeconds(60) }
            .takeWhile { it.isBefore(to) }
            .map { HeartRateRecord.Sample(time = it, beatsPerMinute = 88L) }
            .toList()
        return HeartRateRecord(
            startTime = from,
            startZoneOffset = ZoneOffset.UTC,
            endTime = to,
            endZoneOffset = ZoneOffset.UTC,
            samples = samples,
            metadata = Metadata.activelyRecorded(
                device = androidx.health.connect.client.records.metadata.Device(
                    type = androidx.health.connect.client.records.metadata.Device.TYPE_WATCH,
                ),
            ),
        )
    }

    @Test
    fun `a record that overlaps only the START of the workout is not lost`() = runTest {
        val client = FakeHealthConnectClient()
        client.insertRecords(listOf(straddlingRecord()) + recordsInsideTheHike())

        val samples = reader(client).readRawHeartRateSamples(hikeStart, hikeEnd)

        // The straddling record's samples from 11:50 onwards belong to the workout.
        assertThat(samples).isNotEmpty()
        assertThat(samples.first().time.toEpochMilli()).isEqualTo(hikeStart.toEpochMilli())
    }

    @Test
    fun `the trace has no hole in it`() = runTest {
        // The records cover every minute, so the samples must too: no gap wider than one minute.
        val client = FakeHealthConnectClient()
        client.insertRecords(listOf(straddlingRecord()) + recordsInsideTheHike())

        val samples = reader(client).readRawHeartRateSamples(hikeStart, hikeEnd)

        assertThat(samples.map { it.time.toEpochMilli() }).isInOrder()
        samples.forEach {
            assertThat(it.time.toEpochMilli()).isAtLeast(hikeStart.toEpochMilli())
            assertThat(it.time.toEpochMilli()).isLessThan(hikeEnd.toEpochMilli())
        }
        val widestGapMs = samples.map { it.time.toEpochMilli() }
            .zipWithNext { a, b -> b - a }
            .maxOrNull()
        assertThat(widestGapMs).isEqualTo(Duration.ofMinutes(1).toMillis())
    }

    @Test
    fun `the windowed read really does hide the straddling record`() = runTest {
        // Characterisation: the setup reproduces the bug. The read is non-empty,
        // and the record that runs into the hike is dropped without a trace.
        val client = FakeHealthConnectClient()
        client.insertRecords(listOf(straddlingRecord()) + recordsInsideTheHike())

        val direct = client.readRecords(
            androidx.health.connect.client.request.ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter =
                    androidx.health.connect.client.time.TimeRangeFilter.between(hikeStart, hikeEnd),
            ),
        )

        assertThat(direct.records).isNotEmpty()
        assertThat(direct.records.none { it.startTime.isBefore(hikeStart) }).isTrue()
    }

    @Test
    fun `the fixture really does have the swallowing shape`() = runTest {
        // Characterisation: Health Connect really hides this record from a windowed read.
        // If this starts returning it, the tests above are vacuous.
        val client = FakeHealthConnectClient()
        client.insertRecords(listOf(swallowingRecord()))

        val direct = client.readRecords(
            androidx.health.connect.client.request.ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter =
                    androidx.health.connect.client.time.TimeRangeFilter.between(workoutStart, workoutEnd),
            ),
        )

        assertThat(direct.records).isEmpty()
    }
}
