package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.FakeHealthConnectClient
import com.google.common.truth.Truth.assertThat
import java.time.Duration
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

  // ── The other half of the same problem: a record that straddles only the START ──
  //
  // The tests above are about a record that swallows the WHOLE window, which leaves
  // the windowed read empty — and "empty" was the signal to go looking again. A
  // record that overlaps only the start of the window does not leave it empty: the
  // rest of the workout is covered by records that begin inside it, so the read comes
  // back full of samples and looks perfectly healthy. It is simply missing the front.
  //
  // Reported from a long hike: the workout begins at 11:50, Gadgetbridge's heart-rate
  // record runs 11:48-12:44, and the chart began at 12:44. Fifty-three minutes of a
  // six-hour hike, gone, with a gapless-looking trace either side of the hole.

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

    // The workout starts at 11:50 and had a heart rate from the first minute. The
    // straddling record's samples from 11:50 onwards belong to it.
    assertThat(samples).isNotEmpty()
    assertThat(samples.first().timeEpochMs).isEqualTo(hikeStart.toEpochMilli())
  }

  @Test
  fun `the trace has no hole in it`() = runTest {
    // The hole was 53 minutes wide and the trace either side of it looked perfectly
    // healthy, so "the chart has samples" proves nothing at all. The records cover
    // every minute between them, so the samples must too: no gap wider than the one
    // minute between beats.
    val client = FakeHealthConnectClient()
    client.insertRecords(listOf(straddlingRecord()) + recordsInsideTheHike())

    val samples = reader(client).readRawHeartRateSamples(hikeStart, hikeEnd)

    assertThat(samples.map { it.timeEpochMs }).isInOrder()
    samples.forEach {
      assertThat(it.timeEpochMs).isAtLeast(hikeStart.toEpochMilli())
      assertThat(it.timeEpochMs).isLessThan(hikeEnd.toEpochMilli())
    }
    val widestGapMs = samples.map { it.timeEpochMs }
      .zipWithNext { a, b -> b - a }
      .maxOrNull()
    assertThat(widestGapMs).isEqualTo(Duration.ofMinutes(1).toMillis())
  }

  @Test
  fun `the windowed read really does hide the straddling record`() = runTest {
    // Characterisation, not regression: it proves the SETUP reproduces the bug, so
    // that the tests above are testing the fix rather than testing nothing.
    //
    // The read comes back NON-EMPTY — which is the whole difficulty. The records that
    // sit inside the hike are returned and look like a healthy trace; the one that
    // merely runs INTO the hike is dropped, and nothing about the result says so.
    // (This client drops it for starting too early. A real one has been observed to
    // key on the record's start instead. Either way it is dropped, and either way the
    // widened read below catches it.)
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
