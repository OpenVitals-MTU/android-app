package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.testing.FakeHealthConnectClient
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The REAL Kotlin readers, against the REAL corpus, on the JVM.
 *
 * 3,593 records derived from an actual Health Connect export — 43 heart-rate
 * records, 3 exercise sessions across two writers, 9 nights of sleep, 2,052
 * distance records, a 954-point GPS route — loaded into Google's own
 * FakeHealthConnectClient and read back through the readers the app actually ships.
 *
 * Everything below the Pigeon boundary is real here. That is the point: the bugs
 * this project keeps producing live in exactly this layer, and a Dart-side fake
 * cannot see them, because it sits above the code with the problem.
 */
class FixtureReaderTest {

  /**
   * Seeds the fixture, ONE WRITER AT A TIME.
   *
   * Health Connect stamps a record's `dataOrigin` from the package that inserted
   * it — an app cannot claim to be another app — and Google's fake faithfully does
   * the same, overwriting whatever `dataOrigin` the record was built with. So
   * inserting everything in one go collapses 21 writers into one, and every
   * multi-writer test (dedup, sleep merge, the manual-entry count) silently becomes
   * a single-writer test that passes for the wrong reason.
   *
   * `setPackageName` is the fake's answer to that, and it is the ONLY reason
   * multi-writer behaviour is testable at all: on a real device you would need one
   * signed APK per writer.
   */
  private suspend fun seeded(): FakeHealthConnectClient {
    val client = FakeHealthConnectClient()
    HcFixture.allRecords()
      .groupBy { it.metadata.dataOrigin.packageName }
      .forEach { (writer, records) ->
        client.setPackageName(writer)
        client.insertRecords(records)
      }
    return client
  }

  private fun heart(c: FakeHealthConnectClient) = HeartHealthReader(support(c))
  private fun activity(c: FakeHealthConnectClient) =
    ActivityHealthReader(support(c), "tech.mmarca.openvitals")

  private fun support(c: FakeHealthConnectClient) = HealthConnectReaderSupport(
    clientProvider = { c },
    diagnostics = { "test" },
  )

  @Test
  fun `the corpus loads and the swallowing record is still in it`() = runTest {
    val swallowing = HcFixture.swallowingHeartRateRecord()
    val hours =
      (swallowing.endTime.toEpochMilli() - swallowing.startTime.toEpochMilli()) / 3600000.0

    assertThat(hours).isGreaterThan(12.0)
    assertThat(swallowing.samples.size).isGreaterThan(500)
    // And it really does contain a workout, or it swallows nothing.
    val workout = HcFixture.swallowedWorkout()
    assertThat(workout.startTime).isAtLeast(swallowing.startTime)
    assertThat(workout.endTime).isAtMost(swallowing.endTime)
  }

  @Test
  fun `Health Connect hides that workout's heart rate from a windowed read`() = runTest {
    // Characterisation. Proves the corpus reproduces the bug, so the next test is
    // testing the FIX rather than testing nothing.
    val c = seeded()
    val workout = HcFixture.swallowedWorkout()

    val direct = c.readRecords(
      ReadRecordsRequest(
        recordType = HeartRateRecord::class,
        timeRangeFilter = TimeRangeFilter.between(workout.startTime, workout.endTime),
      ),
    )

    assertThat(direct.records).isEmpty()
  }

  @Test
  fun `but the real reader finds it anyway`() = runTest {
    val c = seeded()
    val workout = HcFixture.swallowedWorkout()

    val samples = heart(c).readRawHeartRateSamples(workout.startTime, workout.endTime)

    assertThat(samples).isNotEmpty()
    samples.forEach {
      assertThat(it.timeEpochMs).isAtLeast(workout.startTime.toEpochMilli())
      assertThat(it.timeEpochMs).isLessThan(workout.endTime.toEpochMilli())
    }
  }

  @Test
  fun `speed samples survive the same trap, which is what the splits ride on`() =
    runTest {
      // The 1 km splits silently fell back to "estimated" on exactly the activities
      // whose heart rate had vanished — same bug, different record type. SpeedRecord
      // is a series record too, and Health Connect filters it by the record's own
      // boundary just the same.
      val c = seeded()
      val route = HcFixture.routeWorkout()

      val speed = activity(c).readSpeedSamples(route.startTime, route.endTime)

      assertThat(speed).isNotEmpty()
      assertThat(speed.map { it.timeEpochMs }).isInOrder()
      speed.forEach {
        assertThat(it.timeEpochMs).isAtLeast(route.startTime.toEpochMilli())
        assertThat(it.timeEpochMs).isLessThan(route.endTime.toEpochMilli())
      }
    }

  @Test
  fun `every record keeps the provenance the Pigeon messages kept dropping`() =
    runTest {
      // recordingMethod, lastModifiedTime and the zone offsets were declared on the
      // domain models, rendered by the UI, and carried by NOTHING — the messages they
      // cross on never had the fields. This asserts the READER puts them in the Msg,
      // which is the half no Dart test can see.
      val c = seeded()
      val week = HcFixture.swallowingHeartRateRecord()

      val sessions = activity(c).readExerciseSessions(
        week.startTime.minusSeconds(86400 * 7),
        week.endTime.plusSeconds(86400 * 7),
      )

      assertThat(sessions).isNotEmpty()
      val session = sessions.first()
      assertThat(session.recordingMethod).isNotNull()
      assertThat(session.lastModifiedEpochMs).isNotNull()
      assertThat(session.startZoneOffsetSeconds).isNotNull()
      // And the writer survived, which is what dedup and the manual-entry count key off.
      assertThat(session.source).isNotEmpty()
    }

  @Test
  fun `sessions come from more than one writer, so dedup has something to do`() =
    runTest {
      val c = seeded()
      val hr = HcFixture.swallowingHeartRateRecord()

      val sessions = activity(c).readExerciseSessions(
        hr.startTime.minusSeconds(86400 * 7),
        hr.endTime.plusSeconds(86400 * 7),
      )

      assertThat(sessions.map { it.source }.toSet().size).isAtLeast(2)
    }
}
