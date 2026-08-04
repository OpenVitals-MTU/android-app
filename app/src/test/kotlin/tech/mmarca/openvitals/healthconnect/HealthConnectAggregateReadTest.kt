package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.FakeHealthConnectClient
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The aggregate reads: day totals, the daily-steps series and the intraday
 * progress line.
 *
 * Dart counterparts: the `reads`, `readRawActivityProgress` and
 * `elevation + wheelchair aggregates` groups of
 * test/data/source/health/health_connect_native_data_source_test.dart.
 *
 * Flutter's fake answers `aggregate`/`aggregateGroupByDuration` with canned
 * numbers, so its assertions are about the QUERY the data source issued (bucket
 * minutes, the metric wire names, the instant range). Kotlin issues those queries
 * itself with typed `AggregateMetric`s, so there is no wire name to get wrong;
 * what is worth pinning here is the ANSWER — which records land in which day, and
 * what a metric reads when the device recorded none.
 */
class HealthConnectAggregateReadTest {

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

    // ── day totals ──────────────────────────────────────────────────────────

    @Test
    fun `readSteps, readDistanceMeters and readFloorsClimbed use the aggregate API`() =
        runTest {
            val client = seeded(
                steps(8_421L, at(9), at(10)),
                DistanceRecord(
                    startTime = at(9),
                    startZoneOffset = null,
                    endTime = at(10),
                    endZoneOffset = null,
                    distance = Length.meters(6_123.4),
                    metadata = Metadata.autoRecorded(watch),
                ),
                FloorsClimbedRecord(
                    startTime = at(9),
                    startZoneOffset = null,
                    endTime = at(10),
                    endZoneOffset = null,
                    floors = 12.0,
                    metadata = Metadata.autoRecorded(watch),
                ),
            )
            val reader = activity(client)

            assertThat(reader.readSteps(date)).isEqualTo(8_421L)
            assertThat(reader.readDistanceMeters(date)).isWithin(1e-6).of(6_123.4)
            assertThat(reader.readFloorsClimbed(date)).isEqualTo(12)
        }

    @Test
    fun `elevation and wheelchair aggregates return the aggregated value`() = runTest {
        val client = seeded(
            ElevationGainedRecord(
                startTime = at(9),
                startZoneOffset = null,
                endTime = at(10),
                endZoneOffset = null,
                elevation = Length.meters(123.5),
                metadata = Metadata.autoRecorded(watch),
            ),
            WheelchairPushesRecord(
                startTime = at(9),
                startZoneOffset = null,
                endTime = at(10),
                endZoneOffset = null,
                count = 1_240L,
                metadata = Metadata.autoRecorded(watch),
            ),
        )
        val reader = activity(client)

        assertThat(reader.readElevationGained(date)).isWithin(1e-6).of(123.5)
        assertThat(reader.readWheelchairPushes(date)).isEqualTo(1_240L)
    }

    // Dart asserts NULL here — "the metric screens show no data rather than a zero
    // day". Kotlin DIVERGES: these day readers return a non-null 0.0/0L, so a day
    // the device never measured is indistinguishable from a day it measured as
    // zero. Pinned as it stands rather than silently left untested; changing it is
    // a product decision, not a test fix.
    @Test
    fun `elevation and wheelchair read zero, not null, when the device records neither`() =
        runTest {
            val reader = activity(seeded())

            assertThat(reader.readElevationGained(date)).isWithin(1e-9).of(0.0)
            assertThat(reader.readWheelchairPushes(date)).isEqualTo(0L)
            assertThat(reader.readFloorsClimbed(date)).isEqualTo(0)
        }

    // ── the daily-steps series ──────────────────────────────────────────────

    @Test
    fun `readDailySteps slices a day bucket over the local instant range`() = runTest {
        val client = seeded(
            steps(5_000L, at(9), at(10)),
            DistanceRecord(
                startTime = at(9),
                startZoneOffset = null,
                endTime = at(10),
                endZoneOffset = null,
                distance = Length.meters(4_000.0),
                metadata = Metadata.autoRecorded(watch),
            ),
            ActiveCaloriesBurnedRecord(
                startTime = at(9),
                startZoneOffset = null,
                endTime = at(10),
                endZoneOffset = null,
                energy = Energy.kilocalories(220.0),
                metadata = Metadata.autoRecorded(watch),
            ),
        )

        val daily = activity(client).readDailySteps(
            startDate = date,
            endDate = date,
            includeActiveCalories = true,
        )

        assertThat(daily).hasSize(1)
        assertThat(daily.single().date).isEqualTo(date)
        assertThat(daily.single().steps).isEqualTo(5_000L)
        assertThat(daily.single().distanceMeters).isWithin(1e-6).of(4_000.0)
        assertThat(daily.single().activeCaloriesKcal!!).isWithin(1e-6).of(220.0)
    }

    @Test
    fun `readDailySteps maps floors when requested and leaves elevation null when not`() =
        runTest {
            val client = seeded(
                steps(100L, at(9), at(10)),
                FloorsClimbedRecord(
                    startTime = at(9),
                    startZoneOffset = null,
                    endTime = at(10),
                    endZoneOffset = null,
                    floors = 12.0,
                    metadata = Metadata.autoRecorded(watch),
                ),
            )

            val daily = activity(client).readDailySteps(
                startDate = date,
                endDate = date,
                includeFloors = true,
            )

            assertThat(daily.single().floorsClimbed).isEqualTo(12)
            // Not requested -> left null. This is the permission-granted-no-data vs
            // permission-missing distinction the metric screens branch on.
            assertThat(daily.single().elevationGainedMeters).isNull()
            assertThat(daily.single().wheelchairPushes).isNull()
        }

    // ── which day a drifted bucket belongs to ───────────────────────────────
    //
    // `Duration.ofDays(1)` slicing stays instant-aligned, so after a DST
    // transition the absolute 24h buckets drift up to an hour off local midnight.
    // Dating a bucket by its START then doubled the fall-back date and skipped the
    // spring-forward one — the bright/dark blip pair on every year heatmap.

    @Test
    fun `a drifted bucket is dated by its midpoint, not its start (the fall-back day)`() {
        val zone = ZoneOffset.UTC

        // The full day, then a bucket that has slipped to a 23:00 start. Dating by
        // start would put BOTH on Jan 2, doubling it and leaving Jan 3 empty.
        val first = dayBucketDate(
            start = Instant.parse("2026-01-02T00:00:00Z"),
            end = Instant.parse("2026-01-02T23:00:00Z"),
            zone = zone,
        )
        val second = dayBucketDate(
            start = Instant.parse("2026-01-02T23:00:00Z"),
            end = Instant.parse("2026-01-03T23:00:00Z"),
            zone = zone,
        )

        assertThat(first).isEqualTo(LocalDate.of(2026, 1, 2))
        // Midpoint 11:00 on Jan 3 — inside the day the bucket actually covers.
        assertThat(second).isEqualTo(LocalDate.of(2026, 1, 3))
    }

    @Test
    fun `the midpoint keeps the spring-forward day a start-dated bucket would skip`() {
        // A drifted bucket running 23:00 Jan 1 -> 00:00 Jan 3 in local wall time.
        // Dating by start left NO bucket on Jan 2 at all — a false empty day.
        val bucketDate = dayBucketDate(
            start = Instant.parse("2026-01-01T23:00:00Z"),
            end = Instant.parse("2026-01-03T00:00:00Z"),
            zone = ZoneOffset.UTC,
        )

        // Midpoint ~11:30 on Jan 2.
        assertThat(bucketDate).isEqualTo(LocalDate.of(2026, 1, 2))
    }

    // ── the intraday progress line ──────────────────────────────────────────

    @Test
    fun `readRawActivityProgress accumulates each contribution into a running total`() =
        onARealClock {
            val client = seeded(
                steps(1_200L, at(8), at(9)),
                steps(800L, at(9), at(10)),
                steps(2_000L, at(10), at(11)),
                distance(800.0, at(8), at(9)),
                distance(500.0, at(9), at(10)),
                distance(1_400.0, at(10), at(11)),
            )

            val points = progress(client, includeDistance = true)

            assertThat(points).hasSize(3)
            // Cumulative, not per-contribution.
            assertThat(points.map { it.totalSteps }).containsExactly(1_200L, 2_000L, 4_000L)
                .inOrder()
            assertThat(points.last().totalDistanceMeters!!).isWithin(1e-6).of(2_700.0)
            assertThat(points.last().time).isEqualTo(at(11))
        }

    // Dart: a metric the DEVICE never reports stays null rather than drawing a
    // zero line. Kotlin DIVERGES on which half decides: nullness follows what was
    // ASKED FOR (the granted permissions), not what came back — a requested metric
    // the device never wrote reads a cumulative 0 from the very first point.
    @Test
    fun `an unrequested metric stays null, while a requested one reads zero`() = onARealClock {
        val client = seeded(steps(1_000L, at(8), at(9)))

        val point = progress(client, includeFloors = true).single()

        assertThat(point.totalSteps).isEqualTo(1_000L)
        // Requested, never recorded -> 0, not null.
        assertThat(point.totalFloorsClimbed).isEqualTo(0)
        // Never requested -> null.
        assertThat(point.totalElevationGainedMeters).isNull()
        assertThat(point.totalWheelchairPushes).isNull()
        assertThat(point.totalDistanceMeters).isNull()
    }

    // Dart: a metric stays non-null from the bucket it FIRST APPEARS in, and its
    // running total carries forward through buckets that had none. Kotlin keeps
    // the carry-forward half; the first half falls out of the divergence above.
    @Test
    fun `a metric's running total carries forward through contributions that had none`() =
        onARealClock {
            val client = seeded(
                steps(1_000L, at(8), at(9)),
                steps(500L, at(9), at(10)),
                steps(500L, at(10), at(11)),
                FloorsClimbedRecord(
                    startTime = at(9),
                    startZoneOffset = null,
                    endTime = at(10),
                    endZoneOffset = null,
                    floors = 3.0,
                    metadata = Metadata.autoRecorded(watch),
                ),
            )

            val points = progress(client, includeFloors = true)

            assertThat(points).hasSize(3)
            assertThat(points[0].totalFloorsClimbed).isEqualTo(0)
            assertThat(points[1].totalFloorsClimbed).isEqualTo(3)
            // Carried forward even though this contribution had none.
            assertThat(points[2].totalFloorsClimbed).isEqualTo(3)
        }

    @Test
    fun `a past day is read across the whole of it, and nothing outside it`() = onARealClock {
        val client = seeded(
            steps(10L, at(0), at(0).plusSeconds(60)),
            steps(20L, at(23), at(23).plusSeconds(1_800)),
            // The next day's first hour must not leak into this day's line.
            steps(999L, at(24), at(24).plusSeconds(1_800)),
            // Nor must the previous day's last hour.
            steps(888L, at(-1), at(0)),
        )

        val points = progress(client)

        assertThat(points.map { it.totalSteps }).containsExactly(10L, 30L).inOrder()
    }

    // Dart pins this on the query's end instant; Kotlin bounds the raw read the
    // same way, so the observable half is that a record the device has not written
    // yet cannot appear on today's line.
    @Test
    fun `today stops at now rather than running on to midnight`() = onARealClock {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val startOfToday = today.atStartOfDay(zone).toInstant()
        val client = seeded(
            steps(10L, startOfToday, startOfToday.plusSeconds(1)),
            // A record stamped for later today: real on a device whose watch syncs
            // ahead, and never part of the progress line as it stands now.
            steps(999L, Instant.now().plusSeconds(7_200), Instant.now().plusSeconds(10_800)),
        )

        val points = activity(client).readRawActivityProgress(
            date = today,
            includeDistance = false,
            includeCalories = false,
            includeActiveCalories = false,
            includeWheelchairPushes = false,
            includeFloors = false,
            includeElevation = false,
        )

        assertThat(points.map { it.totalSteps }).containsExactly(10L)
    }

    @Test
    fun `no contributions means no points`() = onARealClock {
        assertThat(progress(seeded())).isEmpty()
    }

    // ── harness ─────────────────────────────────────────────────────────────

    /**
     * Runs a case against a REAL clock rather than [runTest]'s virtual one.
     *
     * `readRawActivityProgress` guards itself with a 12-second
     * `withTimeoutOrNull` budget, and every read underneath it hops to
     * `Dispatchers.IO`. The moment the body suspends on that real dispatcher,
     * `runTest` judges its own scheduler idle and fast-forwards virtual time to
     * the next scheduled event — the timeout — so the budget "expires" before the
     * read has run at all and the reader returns its empty-list fallback. Every
     * assertion below would then be made against a timeout, not against the
     * records seeded for it.
     */
    private fun onARealClock(body: suspend CoroutineScope.() -> Unit) = runBlocking(block = body)

    /** A fixed past date, well away from any DST transition in any zone. */
    private val date: LocalDate = LocalDate.of(2026, 1, 2)
    private val watch = Device(type = Device.TYPE_WATCH)

    /** [hour] o'clock local on [date]; negative and >=24 hours roll into the neighbouring day. */
    private fun at(hour: Long): Instant =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().plusSeconds(hour * 3_600)

    private fun steps(count: Long, start: Instant, end: Instant) = StepsRecord(
        startTime = start,
        startZoneOffset = null,
        endTime = end,
        endZoneOffset = null,
        count = count,
        metadata = Metadata.autoRecorded(watch),
    )

    private fun distance(meters: Double, start: Instant, end: Instant) = DistanceRecord(
        startTime = start,
        startZoneOffset = null,
        endTime = end,
        endZoneOffset = null,
        distance = Length.meters(meters),
        metadata = Metadata.autoRecorded(watch),
    )

    private suspend fun progress(
        client: HealthConnectClient,
        includeDistance: Boolean = false,
        includeFloors: Boolean = false,
    ) = activity(client).readRawActivityProgress(
        date = date,
        includeDistance = includeDistance,
        includeCalories = false,
        includeActiveCalories = false,
        includeWheelchairPushes = false,
        includeFloors = includeFloors,
        includeElevation = false,
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

    private companion object {
        const val APP_PACKAGE = "tech.mmarca.openvitals"
    }
}
