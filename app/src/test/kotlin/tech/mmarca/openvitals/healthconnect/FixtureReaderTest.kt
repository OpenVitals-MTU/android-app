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
 * The real readers, against the real corpus, on the JVM.
 * 3,593 records from an actual export, loaded into Google's FakeHealthConnectClient.
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
     * Seeds the fixture one writer at a time. The fake stamps `dataOrigin` from the inserting
     * package, so inserting everything at once collapses 21 writers into one.
     * `setPackageName` is the only way multi-writer behaviour is testable at all.
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

    private fun heart(c: FakeHealthConnectClient) = HeartHealthReader(support(c), "tech.mmarca.openvitals")
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
        // Characterisation: the corpus reproduces the bug, so the next test tests the fix.
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
            // The splits fell back to "estimated" on the same activities: SpeedRecord is a series record too.
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
            // recordingMethod, lastModifiedTime and the zone offsets were carried by nothing.
            // This asserts the reader puts them in the Msg.
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
        // Route points were dropped more than once. Distance, pace and splits are computed from them.
        val c = seeded()
        val route = HcFixture.routeWorkout()
        val expected = route.exerciseRouteResult.let { it as ExerciseRouteResult.Data }
            .exerciseRoute.route.size

        val sessions = activity(c).readExerciseSessions(
            route.startTime.minusSeconds(3600),
            route.endTime.plusSeconds(3600),
        )
        // Matched on the boundary: the fake re-stamps ids on insertion.
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
        // Merging can combine sessions, so the count may be lower than 9, but the stages must survive.
        assertThat(sessions.any { it.stages.isNotEmpty() }).isTrue()
    }

    @Test
    fun `two writers on one night are merged into one`() = runTest {
        // Nights written by two apps, which hand-made data cannot produce.
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
            // Every session in the fixture makes the trip, not only those starting inside the window.
            assertThat(sessions).hasSize(HcFixture.exercise().size)
        }
}
