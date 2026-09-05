package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.FakeHealthConnectClient
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import tech.mmarca.openvitals.domain.model.HeartRateInsightBucketDuration
import tech.mmarca.openvitals.domain.model.MaxInsightAggregateBuckets
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
 * The aggregate reads: day totals, the daily-steps series and the intraday progress line.
 * Kotlin issues typed queries, so what is pinned is the answer: which records land
 * in which day, and what an unmeasured metric reads.
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

    // Day totals.

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

    // Dart returns null here. Kotlin returns 0, so an unmeasured day looks like a zero day.
    // Pinned as it stands; changing it is a product decision.
    @Test
    fun `elevation and wheelchair read zero, not null, when the device records neither`() =
        runTest {
            val reader = activity(seeded())

            assertThat(reader.readElevationGained(date)).isWithin(1e-9).of(0.0)
            assertThat(reader.readWheelchairPushes(date)).isEqualTo(0L)
            assertThat(reader.readFloorsClimbed(date)).isEqualTo(0)
        }

    // The daily-steps series.

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
            // Not requested, so null. The metric screens branch on this.
            assertThat(daily.single().elevationGainedMeters).isNull()
            assertThat(daily.single().wheelchairPushes).isNull()
        }

    // Which day a drifted bucket belongs to. After DST the 24h buckets drift off local midnight.
    // Dating by start doubled one date and skipped another.

    @Test
    fun `a drifted bucket is dated by its midpoint, not its start (the fall-back day)`() {
        val zone = ZoneOffset.UTC

        // A bucket slipped to a 23:00 start. Dating by start would put both on Jan 2.
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
        // A drifted bucket 23:00 Jan 1 -> 00:00 Jan 3. Dating by start left Jan 2 empty.
        val bucketDate = dayBucketDate(
            start = Instant.parse("2026-01-01T23:00:00Z"),
            end = Instant.parse("2026-01-03T00:00:00Z"),
            zone = ZoneOffset.UTC,
        )

        // Midpoint ~11:30 on Jan 2.
        assertThat(bucketDate).isEqualTo(LocalDate.of(2026, 1, 2))
    }

    // The intraday progress line.

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

    // Nullness follows what was asked for, not what came back:
    // a requested metric the device never wrote reads 0.
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

    // A running total carries forward through buckets that had none.
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

    // A record the device has not written yet cannot appear on today's line.
    @Test
    fun `today stops at now rather than running on to midnight`() = onARealClock {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val startOfToday = today.atStartOfDay(zone).toInstant()
        val client = seeded(
            steps(10L, startOfToday, startOfToday.plusSeconds(1)),
            // A record stamped for later today, as a watch syncing ahead writes.
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

    // Chunked long-range reads.

    /**
     * A year of day buckets was ~800KB against the 1MB Binder buffer.
     * No request may be wider than [DailyAggregateMaxQueryDays], and the chunks must stitch back into one series.
     */
    @Test
    fun `readDailyNutrition chunks a long range and stitches the series back together`() = runTest {
        val zone = ZoneId.systemDefault()
        val firstDay = date.minusDays(300)
        fun burn(day: LocalDate, kcal: Double) = TotalCaloriesBurnedRecord(
            startTime = day.atStartOfDay(zone).toInstant().plusSeconds(12 * 3_600),
            startZoneOffset = null,
            endTime = day.atStartOfDay(zone).toInstant().plusSeconds(13 * 3_600),
            endZoneOffset = null,
            energy = Energy.kilocalories(kcal),
            metadata = Metadata.autoRecorded(watch),
        )
        val client = seeded(burn(firstDay, 1_800.0), burn(date, 2_200.0))

        val series = NutritionHealthReader(support(client), APP_PACKAGE)
            .readDailyNutrition(startDate = firstDay, endDate = date, includeHydration = false)

        // The read went out in more than one request, none wider than the slice.
        assertThat(client.groupByDurationRequestRanges.size).isGreaterThan(1)
        client.groupByDurationRequestRanges.forEach { (start, end) ->
            val days = java.time.Duration.between(start, end).toDays()
            assertThat(days).isAtMost(DailyAggregateMaxQueryDays)
        }
        // And the stitched series still carries both ends of the range.
        assertThat(series.single { it.date == firstDay }.caloriesBurnedKcal).isWithin(1e-6).of(1_800.0)
        assertThat(series.single { it.date == date }.caloriesBurnedKcal).isWithin(1e-6).of(2_200.0)
    }

    /** Same budget for heart rate: a year of daily BPM buckets must go out tiled and stitch back. */
    @Test
    fun `readDailyHeartRateSummaries chunks a long range and stitches the series back together`() = runTest {
        val zone = ZoneId.systemDefault()
        val firstDay = date.minusDays(300)
        fun hr(day: LocalDate, bpm: Long) = HeartRateRecord(
            startTime = day.atStartOfDay(zone).toInstant().plusSeconds(12 * 3_600),
            startZoneOffset = null,
            endTime = day.atStartOfDay(zone).toInstant().plusSeconds(12 * 3_600 + 60),
            endZoneOffset = null,
            samples = listOf(
                HeartRateRecord.Sample(
                    time = day.atStartOfDay(zone).toInstant().plusSeconds(12 * 3_600),
                    beatsPerMinute = bpm,
                ),
            ),
            metadata = Metadata.autoRecorded(watch),
        )
        val client = seeded(hr(firstDay, 58L), hr(date, 72L))

        val series = HeartHealthReader(support(client), "tech.mmarca.openvitals")
            .readDailyHeartRateSummaries(startDate = firstDay, endDate = date)

        assertThat(client.groupByDurationRequestRanges.size).isGreaterThan(1)
        client.groupByDurationRequestRanges.forEach { (start, end) ->
            val days = java.time.Duration.between(start, end).toDays()
            assertThat(days).isAtMost(DailyAggregateMaxQueryDays)
        }
        assertThat(series.single { it.date == firstDay }.avgBpm).isEqualTo(58L)
        assertThat(series.single { it.date == date }.avgBpm).isEqualTo(72L)
    }

    /**
     * BPM_AVG weights every sample equally, so a 1 Hz workout outvoted the per-minute series.
     * Summaries now slice by hour and fold the hours duration-weighted.
     */
    @Test
    fun `readDailyHeartRateSummaries keeps a 1 Hz workout from becoming the day average`() = runTest {
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant()
        // One background sample per hour at 70 bpm, except the workout hour.
        val background = (0L until 24L).filter { it != 19L }.map { hour ->
            HeartRateRecord(
                startTime = dayStart.plusSeconds(hour * 3_600),
                startZoneOffset = null,
                endTime = dayStart.plusSeconds(hour * 3_600 + 60),
                endZoneOffset = null,
                samples = listOf(
                    HeartRateRecord.Sample(
                        time = dayStart.plusSeconds(hour * 3_600),
                        beatsPerMinute = 70L,
                    ),
                ),
                metadata = Metadata.autoRecorded(watch),
            )
        }
        // Five minutes at 1 Hz and 140 bpm inside hour 19: 300 samples against 23.
        val workout = HeartRateRecord(
            startTime = dayStart.plusSeconds(19L * 3_600),
            startZoneOffset = null,
            endTime = dayStart.plusSeconds(19L * 3_600 + 300),
            endZoneOffset = null,
            samples = (0L until 300L).map { second ->
                HeartRateRecord.Sample(
                    time = dayStart.plusSeconds(19L * 3_600 + second),
                    beatsPerMinute = 140L,
                )
            },
            metadata = Metadata.autoRecorded(watch),
        )
        val client = seeded(*(background + workout).toTypedArray())

        val day = HeartHealthReader(support(client), APP_PACKAGE)
            .readDailyHeartRateSummaries(startDate = date, endDate = date)
            .single { it.date == date }

        // (23 × 70 + 140) / 24 ≈ 73. The per-sample day aggregate said 135.
        assertThat(day.avgBpm).isEqualTo(73L)
        assertThat(day.minBpm).isEqualTo(70L)
        assertThat(day.maxBpm).isEqualTo(140L)
    }

    @Test
    fun `readDailyHRV buckets a burst of readings so it does not outvote the spot checks`() = runTest {
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant()
        fun hrv(at: Instant, ms: Double) = HeartRateVariabilityRmssdRecord(
            time = at,
            zoneOffset = null,
            heartRateVariabilityMillis = ms,
            metadata = Metadata.autoRecorded(watch),
        )
        // Three occupied minutes: (40 + 60 + 20) / 3 = 40. The per-sample mean said 21.
        val spots = listOf(
            hrv(dayStart.plusSeconds(9L * 3_600), 40.0),
            hrv(dayStart.plusSeconds(15L * 3_600), 60.0),
        )
        val burst = (0L until 60L).map { second ->
            hrv(dayStart.plusSeconds(23L * 3_600 + second), 20.0)
        }
        val client = seeded(*(spots + burst).toTypedArray())
        val reader = HeartHealthReader(support(client), APP_PACKAGE)

        val day = reader.readDailyHRV(startDate = date, endDate = date).single()
        assertThat(day.date).isEqualTo(date)
        assertThat(day.rmssdMs).isWithin(1e-9).of(40.0)
        assertThat(reader.readHrvRmssd(date)).isWithin(1e-9).of(40.0)
    }

    @Test
    fun `readDailyRestingHR chunks a long range and stitches the series back together`() = runTest {
        val zone = ZoneId.systemDefault()
        val firstDay = date.minusDays(300)
        fun resting(day: LocalDate, bpm: Long) = RestingHeartRateRecord(
            time = day.atStartOfDay(zone).toInstant().plusSeconds(8 * 3_600),
            zoneOffset = null,
            beatsPerMinute = bpm,
            metadata = Metadata.autoRecorded(watch),
        )
        val client = seeded(resting(firstDay, 51L), resting(date, 55L))

        val series = HeartHealthReader(support(client), "tech.mmarca.openvitals")
            .readDailyRestingHR(startDate = firstDay, endDate = date)

        assertThat(client.groupByDurationRequestRanges.size).isGreaterThan(1)
        client.groupByDurationRequestRanges.forEach { (start, end) ->
            val days = java.time.Duration.between(start, end).toDays()
            assertThat(days).isAtMost(DailyAggregateMaxQueryDays)
        }
        assertThat(series.single { it.date == firstDay }.bpm).isEqualTo(51L)
        assertThat(series.single { it.date == date }.bpm).isEqualTo(55L)
    }

    @Test
    fun `readHeartRateSamplesForInsights splits every day into budgeted requests`() = runTest {
        val zone = ZoneId.systemDefault()
        val firstDay = date.minusDays(1)
        fun hr(day: LocalDate, hour: Long, bpm: Long) = HeartRateRecord(
            startTime = day.atStartOfDay(zone).toInstant().plusSeconds(hour * 3_600),
            startZoneOffset = null,
            endTime = day.atStartOfDay(zone).toInstant().plusSeconds(hour * 3_600 + 60),
            endZoneOffset = null,
            samples = listOf(
                HeartRateRecord.Sample(
                    time = day.atStartOfDay(zone).toInstant().plusSeconds(hour * 3_600),
                    beatsPerMinute = bpm,
                ),
            ),
            metadata = Metadata.autoRecorded(watch),
        )
        val client = seeded(hr(firstDay, 9, 61L), hr(date, 15, 88L))

        val samples = HeartHealthReader(support(client), APP_PACKAGE)
            .readHeartRateSamplesForInsights(
                firstDay.atStartOfDay(zone).toInstant(),
                date.plusDays(1).atStartOfDay(zone).toInstant(),
            )

        // A whole day of one-minute buckets in one request hit TransactionTooLargeException
        // and degraded to empty, so cardio load fell back to step estimates.
        val budget = HeartRateInsightBucketDuration.multipliedBy(MaxInsightAggregateBuckets)
        assertThat(client.groupByDurationRequestRanges).isNotEmpty()
        client.groupByDurationRequestRanges.forEach { (start, end) ->
            assertThat(java.time.Duration.between(start, end)).isAtMost(budget)
        }
        // Two days, more than one request each.
        assertThat(client.groupByDurationRequestRanges.size).isGreaterThan(2)
        assertThat(samples.map { it.beatsPerMinute }).containsExactly(61L, 88L).inOrder()
    }

    // Harness.

    /**
     * Runs on a real clock. Under [runTest] the reads hop to Dispatchers.IO, virtual time
     * skips to the 12-second timeout, and every assertion would test a timeout.
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
