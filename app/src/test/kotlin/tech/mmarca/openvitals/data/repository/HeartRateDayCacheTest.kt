package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.local.heartratecache.HeartRateDayCacheDao
import tech.mmarca.openvitals.data.local.heartratecache.HeartRateDayEntity
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.HeartRateDayAggregate
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/**
 * Week, month and year bars must show the day view's average. Health Connect's
 * hourly average is sample-weighted, so a 1 Hz workout hour reads high. The
 * repository replaces it with the raw average, cached per day.
 */
class HeartRateDayCacheTest {

    @Before
    fun mockLog() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun unmockLog() {
        unmockkStatic(Log::class)
    }

    private val today = LocalDate.of(2026, 9, 24)
    private val cache = InMemoryDayCache()
    private val hc = mockk<HealthConnectManager>().also { hc ->
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { hc.grantedPermissions() } returns setOf(HealthPermission.getReadPermission(HeartRateRecord::class))
    }

    private fun day(date: LocalDate, hourlyAvg: Long, samples: Long = 2_000L, signature: String = "a") =
        HeartRateDayAggregate(
            summary = HeartRateSummary(date = date, avgBpm = hourlyAvg, minBpm = 51L, maxBpm = 137L),
            sampleCount = samples,
            signature = signature,
        )

    @Test
    fun `a new day is read raw, cached, and replaces the hourly average`() = runTest {
        coEvery { hc.readDailyHeartRateAggregates(today, today) } returns listOf(day(today, hourlyAvg = 71L))
        coEvery { hc.readDailyHeartRateAverages(today, today) } returns mapOf(today to 65.2)

        val summary = HeartRepositoryImpl(hc, cache).loadDailyHeartRateSummaries(today, today).single()

        assertEquals(HeartRateSummary(today, avgBpm = 65L, minBpm = 51L, maxBpm = 137L), summary)
        assertEquals(65.2, cache.rows.getValue(today.toEpochDay()).averageBpm, 1e-9)
    }

    @Test
    fun `an unchanged day comes from the cache without a raw read`() = runTest {
        coEvery { hc.readDailyHeartRateAggregates(today, today) } returns listOf(day(today, hourlyAvg = 71L))
        coEvery { hc.readDailyHeartRateAverages(today, today) } returns mapOf(today to 65.2)
        val repository = HeartRepositoryImpl(hc, cache)

        repository.loadDailyHeartRateSummaries(today, today)
        val second = repository.loadDailyHeartRateSummaries(today, today).single()

        assertEquals(65L, second.avgBpm)
        coVerify(exactly = 1) { hc.readDailyHeartRateAverages(any(), any()) }
    }

    @Test
    fun `a day whose hours changed is read again`() = runTest {
        val repository = HeartRepositoryImpl(hc, cache)
        coEvery { hc.readDailyHeartRateAggregates(today, today) } returns listOf(day(today, 71L, signature = "a"))
        coEvery { hc.readDailyHeartRateAverages(today, today) } returns mapOf(today to 65.2)
        repository.loadDailyHeartRateSummaries(today, today)

        // A late sync added a run: the hourly signature moved.
        coEvery { hc.readDailyHeartRateAggregates(today, today) } returns listOf(day(today, 74L, signature = "b"))
        coEvery { hc.readDailyHeartRateAverages(today, today) } returns mapOf(today to 67.6)
        val summary = repository.loadDailyHeartRateSummaries(today, today).single()

        assertEquals(68L, summary.avgBpm)
        coVerify(exactly = 2) { hc.readDailyHeartRateAverages(today, today) }
    }

    @Test
    fun `a failed raw read keeps the hourly average and caches nothing`() = runTest {
        coEvery { hc.readDailyHeartRateAggregates(today, today) } returns listOf(day(today, hourlyAvg = 71L))
        coEvery { hc.readDailyHeartRateAverages(today, today) } returns emptyMap()

        val summary = HeartRepositoryImpl(hc, cache).loadDailyHeartRateSummaries(today, today).single()

        assertEquals(71L, summary.avgBpm)
        assertTrue(cache.rows.isEmpty())
    }

    @Test
    fun `past the load budget the newest days are read and older ones stay hourly`() = runTest {
        // Four days, each half the load budget: the newest two fit.
        val samples = HeartRepositoryImpl.DayCacheLoadSamples / 2
        val days = (0L until 4L).map { back -> day(today.minusDays(back), hourlyAvg = 80L, samples = samples) }
        coEvery { hc.readDailyHeartRateAggregates(any(), any()) } returns days
        coEvery { hc.readDailyHeartRateAverages(any(), any()) } answers {
            val from = firstArg<LocalDate>()
            val to = secondArg<LocalDate>()
            generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.associateWith { 70.0 }
        }

        val summaries = HeartRepositoryImpl(hc, cache)
            .loadDailyHeartRateSummaries(today.minusDays(3), today)
            .associate { it.date to it.avgBpm }

        assertEquals(70L, summaries.getValue(today))
        assertEquals(70L, summaries.getValue(today.minusDays(1)))
        assertEquals(80L, summaries.getValue(today.minusDays(2)))
        assertEquals(80L, summaries.getValue(today.minusDays(3)))
        coVerify(exactly = 0) { hc.readDailyHeartRateAverages(today.minusDays(2), any()) }
    }

    @Test
    fun `without a cache the hourly average is served as before`() = runTest {
        coEvery { hc.readDailyHeartRateAggregates(today, today) } returns listOf(day(today, hourlyAvg = 71L))

        val summary = HeartRepositoryImpl(hc).loadDailyHeartRateSummaries(today, today).single()

        assertEquals(71L, summary.avgBpm)
        coVerify(exactly = 0) { hc.readDailyHeartRateAverages(any(), any()) }
    }

    @Test
    fun `a read spans neighbouring stale days and stops at a cached one or at its size`() {
        val days = (0L until 6L).map { back -> day(today.minusDays(back), 70L, samples = 20_000L) }
        val stale = days.map { it.summary.date }.toSet() - today.minusDays(3)

        val reads = planRawDayReads(days, stale, readSamples = 50_000L, loadSamples = 1_000_000L)
            .map { read -> read.map { it.summary.date } }

        assertEquals(
            listOf(
                listOf(today, today.minusDays(1)),
                listOf(today.minusDays(2)),
                listOf(today.minusDays(4), today.minusDays(5)),
            ),
            reads,
        )
    }

    @Test
    fun `a day without a sample count is planned as a full 1 Hz day`() {
        val days = listOf(day(today, 70L, samples = 0L), day(today.minusDays(1), 70L, samples = 0L))

        val reads = planRawDayReads(days, days.map { it.summary.date }.toSet(), 50_000L, loadSamples = 100_000L)

        assertEquals(listOf(listOf(today)), reads.map { read -> read.map { it.summary.date } })
    }

    private class InMemoryDayCache : HeartRateDayCacheDao {
        val rows = linkedMapOf<Long, HeartRateDayEntity>()

        override suspend fun daysBetween(fromEpochDay: Long, toEpochDay: Long): List<HeartRateDayEntity> =
            rows.values.filter { it.epochDay in fromEpochDay..toEpochDay }

        override suspend fun upsert(rows: List<HeartRateDayEntity>) {
            rows.forEach { this.rows[it.epochDay] = it }
        }
    }
}
