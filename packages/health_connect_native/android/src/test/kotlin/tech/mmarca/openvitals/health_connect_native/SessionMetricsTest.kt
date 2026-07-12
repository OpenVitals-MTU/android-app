package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.testing.FakeHealthConnectClient
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The sibling-record bug, and the power read, through the real reader.
 *
 * A Health Connect `ExerciseSessionRecord` carries almost nothing. A watch writes
 * the walk as a session with little more than a duration, and puts its steps,
 * distance, calories and elevation in *separate* records covering the same window.
 * Reading the session alone therefore reported "Not available" for numbers the watch
 * had in fact recorded — directly above a chart of that same activity's step
 * cadence, and splits that added up to a distance the page refused to show.
 *
 * The only way to reattach them is to aggregate over the session's own window, which
 * is what `readExerciseSessionMetrics` does. And aggregation is exactly what Google's
 * fake refuses to implement — so none of this could be tested until
 * [AggregatingFakeHealthConnectClient] existed.
 *
 * CAVEAT, and it is a real one: the interval pro-rating these numbers rest on is
 * UNCALIBRATED against a real device (see that class's doc). These tests prove the
 * wiring — that `readExerciseSessionMetrics` asks for the right aggregates, over the
 * right window, and puts them in the right Msg fields. They do not prove the app
 * would show the same number a phone would.
 */
class SessionMetricsTest {

  private suspend fun seeded(): AggregatingFakeHealthConnectClient {
    val inner = FakeHealthConnectClient()
    HcFixture.allRecords()
      .groupBy { it.metadata.dataOrigin.packageName }
      .forEach { (writer, records) ->
        inner.setPackageName(writer)
        inner.insertRecords(records)
      }
    return AggregatingFakeHealthConnectClient(inner)
  }

  private fun reader(c: AggregatingFakeHealthConnectClient) = ActivityHealthReader(
    HealthConnectReaderSupport(clientProvider = { c }, diagnostics = { "test" }),
    "tech.mmarca.openvitals",
  )

  @Test
  fun `a session gets back the steps and distance written BESIDE it`() = runTest {
    val client = seeded()
    val route = HcFixture.routeWorkout()

    val metrics = reader(client).readExerciseSessionMetrics(
      route.startTime,
      route.endTime,
      listOf("steps", "distance", "activeCalories"),
    )

    // The session record itself has none of these. They exist only as sibling
    // records over the same window, and this is the read that reattaches them.
    assertThat(metrics.steps).isNotNull()
    assertThat(metrics.steps!!).isGreaterThan(0L)
    assertThat(metrics.totalDistanceMeters).isNotNull()
    assertThat(metrics.totalDistanceMeters!!).isGreaterThan(0.0)
    assertThat(metrics.activeCaloriesKcal).isNotNull()
  }

  @Test
  fun `a metric that was not asked for stays null, and is never zero`() = runTest {
    // "Unknown" and "zero" are different answers, and the app branches on the
    // difference: a device that records no floors must not be told it climbed 0.
    val client = seeded()
    val route = HcFixture.routeWorkout()

    val metrics = reader(client).readExerciseSessionMetrics(
      route.startTime,
      route.endTime,
      listOf("steps"),
    )

    assertThat(metrics.steps).isNotNull()
    assertThat(metrics.totalDistanceMeters).isNull()
    assertThat(metrics.activeCaloriesKcal).isNull()
    assertThat(metrics.averagePowerWatts).isNull()
  }

  @Test
  fun `average power comes back, which it never did before e7dfba37`() = runTest {
    // The app asks Health Connect for READ_POWER, tells you so during onboarding,
    // writes PowerRecord from a BLE sensor, and renders an "Average power" row — and
    // never read power back, because `power` was missing from the SESSION_METRICS
    // wire table. The row only earns its place by HAVING a value, so it never
    // appeared at all. Not "Not available": absent.
    val client = seeded()
    val route = HcFixture.routeWorkout()

    val metrics = reader(client).readExerciseSessionMetrics(
      route.startTime,
      route.endTime,
      listOf("power"),
    )

    assertThat(metrics.averagePowerWatts).isNotNull()
    assertThat(metrics.averagePowerWatts!!).isGreaterThan(0.0)
  }

  @Test
  fun `an unknown wire name is skipped, not thrown`() = runTest {
    // The contract with Dart: SESSION_METRICS skips a name it does not know, so a
    // NEWER Dart asking an OLDER host for a metric degrades to null instead of
    // breaking the whole read. That tolerance is also how `power` went missing for
    // months without anything failing — so it is worth pinning deliberately rather
    // than rediscovering.
    val client = seeded()
    val route = HcFixture.routeWorkout()

    val metrics = reader(client).readExerciseSessionMetrics(
      route.startTime,
      route.endTime,
      listOf("steps", "somethingNobodyImplemented"),
    )

    assertThat(metrics.steps).isNotNull()
  }

  @Test
  fun `the heart rate of the swallowed workout aggregates too`() = runTest {
    // Aggregation slices by TIME, not by record — which is exactly why it can see
    // inside the 17.48-hour record that a windowed read cannot, and why it is the
    // last-resort fallback in readRawHeartRateSamples.
    val client = seeded()
    val workout = HcFixture.swallowedWorkout()

    val buckets = HeartHealthReader(
      HealthConnectReaderSupport(clientProvider = { client }, diagnostics = { "test" }),
    ).readHeartRateAggregatedBuckets(
      workout.startTime,
      workout.endTime,
      60_000L,
    )

    assertThat(buckets).isNotEmpty()
    buckets.forEach {
      assertThat(it.avgBpm).isGreaterThan(0L)
      assertThat(it.startEpochMs).isAtLeast(workout.startTime.toEpochMilli())
    }
  }
}
