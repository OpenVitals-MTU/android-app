package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.FakeHealthConnectClient
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The bug that started all of this, run through the REAL reader.
 *
 * Health Connect filters a series record by the boundary of the RECORD, never by
 * the times of the samples nested inside it. A writer may group a whole day of
 * beats into one HeartRateRecord — and one on the reporter's phone genuinely did,
 * for 17.48 hours, holding 891 samples. Ask for a 36-minute workout that sits
 * inside it and Health Connect looks at the record, decides it is not in the
 * window, and hands back nothing. Successfully. The activity had a heart rate the
 * entire time and the screen said "Not available".
 *
 * The numbers here are the real ones, from the reporter's Health Connect export:
 *
 *     record  2026-06-22 00:34 → 18:03 UTC   (17.48 h, 891 samples)
 *     workout 2026-06-22 06:05 → 06:41 UTC   (09:05–09:41 in Estonia, UTC+3)
 *
 *     records OVERLAPPING the workout window: 1
 *     records STARTING inside it:             0     ← which is why the read was empty
 *
 * This test exists because no Dart-side test can see this. A fake at the Pigeon
 * boundary returns *samples*, not records — it sits ABOVE the Kotlin that has the
 * problem, so it cannot reproduce it and cannot prove the fix. Only the real
 * reader, against a real Health Connect client, can.
 */
class SwallowingRecordTest {

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

  private fun reader(client: FakeHealthConnectClient) = HeartHealthReader(
    HealthConnectReaderSupport(
      clientProvider = { client },
      diagnostics = { "test" },
    ),
  )

  @Test
  fun `a workout buried inside a 17 hour record still has a heart rate`() = runTest {
    val client = FakeHealthConnectClient()
    client.insertRecords(listOf(swallowingRecord()))

    val samples = reader(client).readRawHeartRateSamples(workoutStart, workoutEnd)

    assertThat(samples).isNotEmpty()
  }

  @Test
  fun `and every sample it returns is actually inside the workout`() = runTest {
    // The mirror-image half of the same bug. Once a record like this IS returned,
    // a naive `flatMap { it.samples }` takes all 891 of them — seventeen hours of
    // heart rate on a thirty-six minute chart.
    val client = FakeHealthConnectClient()
    client.insertRecords(listOf(swallowingRecord()))

    val samples = reader(client).readRawHeartRateSamples(workoutStart, workoutEnd)

    assertThat(samples.map { it.timeEpochMs }).isInOrder()
    samples.forEach {
      assertThat(it.timeEpochMs).isAtLeast(workoutStart.toEpochMilli())
      assertThat(it.timeEpochMs).isLessThan(workoutEnd.toEpochMilli())
    }
    // 36 minutes at one sample a minute.
    assertThat(samples).hasSize(36)
  }

  @Test
  fun `the fixture really does have the swallowing shape`() = runTest {
    // Characterisation, not regression. It proves the SETUP reproduces the bug —
    // that Health Connect really does hide this record from a windowed read — so
    // that the two tests above are testing the fix rather than testing nothing.
    // If this ever starts returning the record, the tests above become vacuous and
    // we need to know.
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
