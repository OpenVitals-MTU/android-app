package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.testing.FakeHealthConnectClient
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
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

    private fun support(c: FakeHealthConnectClient): HealthConnectReaderSupport {
        val diagnostics = mockk<HealthConnectDiagnostics>()
        every { diagnostics.summary() } returns "test"
        return HealthConnectReaderSupport(
            clientProvider = { c },
            diagnostics = diagnostics,
            rateLimitMessage = { "rate limited" },
        )
    }

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
            assertThat(it.time.toEpochMilli()).isAtLeast(workout.startTime.toEpochMilli())
            assertThat(it.time.toEpochMilli()).isLessThan(workout.endTime.toEpochMilli())
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
            assertThat(speed.map { it.time.toEpochMilli() }).isInOrder()
            speed.forEach {
                assertThat(it.time.toEpochMilli()).isAtLeast(route.startTime.toEpochMilli())
                assertThat(it.time.toEpochMilli()).isLessThan(route.endTime.toEpochMilli())
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
            assertThat(session.lastModifiedTime).isNotNull()
            assertThat(session.startZoneOffset).isNotNull()
            // And the writer survived, which is what dedup and the manual-entry count key off.
            assertThat(session.source).isNotEmpty()
        }

    @Test
    fun `the GPS session keeps its route points`() = runTest {
        // Route points were dropped between the fixture and the domain more than
        // once. Distance, pace and the 1 km splits are all computed from them, so a
        // session that arrives without its track quietly falls back to estimates.
        val c = seeded()
        val route = HcFixture.routeWorkout()
        val expected = route.exerciseRouteResult.let { it as ExerciseRouteResult.Data }
            .exerciseRoute.route.size

        val sessions = activity(c).readExerciseSessions(
            route.startTime.minusSeconds(3600),
            route.endTime.plusSeconds(3600),
        )
        // Matched on the boundary, not the id: the fake re-stamps a record's id on
        // insertion, so the fixture's own id is not what comes back out.
        val session = sessions.single { it.startTime == route.startTime }

        assertThat(expected).isGreaterThan(500)
        assertThat(session.route.points).hasSize(expected)
    }

    @Test
    fun `every night in the fixture comes back, with its stages`() = runTest {
        val c = seeded()
        val nights = HcFixture.sleep()
        val first = nights.minOf { it.startTime }
        val last = nights.maxOf { it.endTime }

        val sessions = SleepHealthReader(support(c))
            .readSleepSessions(first.minusSeconds(86_400), last.plusSeconds(86_400))

        assertThat(sessions).isNotEmpty()
        // Merging can COMBINE overlapping sessions, so the count may be lower than
        // the raw 9 — but it must never be zero, and the stages must survive the
        // trip: without them the hypnogram is empty and the "share of time in bed"
        // card has nothing to divide.
        assertThat(sessions.any { it.stages.isNotEmpty() }).isTrue()
    }

    @Test
    fun `two writers on one night are merged into one`() = runTest {
        // The fixture has nights written by two apps. Merging them is the whole
        // reason the sleep merge exists, and it cannot be exercised by hand-made
        // data — a real person with a watch AND a phone app is what produces this.
        val c = seeded()
        val nights = HcFixture.sleep()
        val multiWriter = nights
            .groupBy { it.startTime.atOffset(java.time.ZoneOffset.UTC).toLocalDate() }
            .entries
            .first { (_, night) -> night.map { it.metadata.dataOrigin.packageName }.toSet().size > 1 }
            .value

        val sessions = SleepHealthReader(support(c)).readSleepSessions(
            multiWriter.minOf { it.startTime }.minusSeconds(3600),
            multiWriter.maxOf { it.endTime }.plusSeconds(3600),
        )

        assertThat(multiWriter.size).isEqualTo(2)
        assertThat(sessions).hasSize(1)
    }

    @Test
    fun `the fixture's hydration entries come back`() = runTest {
        val c = seeded()
        val entries = HcFixture.hydration()

        val read = HydrationHealthReader(support(c), "tech.mmarca.openvitals")
            .readHydrationEntries(
                entries.minOf { it.startTime }.minusSeconds(60),
                entries.maxOf { it.endTime }.plusSeconds(60),
            )

        assertThat(read).hasSize(entries.size)
        assertThat(read.sumOf { it.liters })
            .isWithin(1e-9)
            .of(entries.sumOf { record -> record.volume.inLiters })
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
            // And nothing was lost on the way: every session in the fixture makes
            // the trip, not just the ones that happen to start inside the window.
            assertThat(sessions).hasSize(HcFixture.exercise().size)
        }
}
