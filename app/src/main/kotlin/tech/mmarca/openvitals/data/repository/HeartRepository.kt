package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.stats.timeBucketedAverageOrNull
import tech.mmarca.openvitals.data.local.heartratecache.HeartRateDayCacheDao
import tech.mmarca.openvitals.data.local.heartratecache.HeartRateDayEntity
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.HeartRateDayAggregate
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.model.HrvSample
import tech.mmarca.openvitals.domain.model.reducedForChart
import tech.mmarca.openvitals.domain.model.RestingHeartRateSample
import tech.mmarca.openvitals.domain.model.dayAverageBpm
import tech.mmarca.openvitals.domain.query.HeartPeriodData
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class HeartRepositoryImpl @Inject constructor(
    private val hc: HealthConnectManager,
    private val dayCache: HeartRateDayCacheDao? = null,
) : HeartRepository {

    companion object {
        private const val TAG = "HeartRepository"

        /** Raise it when the cached day average is computed differently. Every row is then read again. */
        private const val DayCacheVersion = 1

        /** Samples one raw read aims to hold: about two weeks of once-a-minute data. A bigger day is read alone. */
        internal const val DayCacheReadSamples = 50_000L

        /** Samples one load may read raw. Older stale days keep the hourly average until a later load. */
        internal const val DayCacheLoadSamples = 250_000L
    }

    private val readHeartRatePermission = HealthPermission.getReadPermission(HeartRateRecord::class)
    private val readRestingHRPermission = HealthPermission.getReadPermission(RestingHeartRateRecord::class)
    private val readHrvPermission = HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)

    /** One load fills the day cache at a time, so parallel windows do not read the same days twice. */
    private val dayCacheFill = Mutex()

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    override suspend fun loadHeartPeriod(
        query: PeriodLoadQuery,
        metric: HeartPeriodMetric,
    ): HeartPeriodData {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        val data = coroutineScope {
            when (metric) {
                HeartPeriodMetric.ALL -> loadAllHeartPeriod(query, granted)
                HeartPeriodMetric.AVERAGE_HEART_RATE -> if (query.range == TimeRange.DAY) {
                    val daySamples = async { loadRawHeartRateSamplesForDayGraph(query.selectedDate, granted) }
                    val previousDayAvgBpm = async { loadAvgHeartRate(windows.previous.start, granted) }
                    val baselineDailySummaries = async {
                        loadDailyHeartRateSummaries(windows.baseline.start, windows.baseline.end, granted)
                    }
                    HeartPeriodData(
                        daySamples = daySamples.await(),
                        previousDayAvgBpm = previousDayAvgBpm.await(),
                        baselineDailySummaries = baselineDailySummaries.await(),
                    )
                } else {
                    val dailySummaries = async {
                        loadDailyHeartRateSummaries(windows.current.start, windows.current.end, granted)
                    }
                    val previousDailySummaries = async {
                        loadDailyHeartRateSummaries(windows.previous.start, windows.previous.end, granted)
                    }
                    val baselineDailySummaries = async {
                        loadDailyHeartRateSummaries(windows.baseline.start, windows.baseline.end, granted)
                    }
                    HeartPeriodData(
                        dailySummaries = dailySummaries.await(),
                        previousDailySummaries = previousDailySummaries.await(),
                        baselineDailySummaries = baselineDailySummaries.await(),
                    )
                }
                HeartPeriodMetric.RESTING_HEART_RATE -> if (query.range == TimeRange.DAY) {
                    val dayRestingSamples = async { loadRestingHeartRateSamplesForDay(query.selectedDate, granted) }
                    val previousDayRestingBpm = async { loadRestingHeartRate(windows.previous.start, granted) }
                    val baselineDailyRestingHR = async {
                        loadDailyRestingHR(windows.baseline.start, windows.baseline.end, granted)
                    }
                    val samples = dayRestingSamples.await()
                    HeartPeriodData(
                        dayRestingSamples = samples,
                        dayRestingBpm = samples.dayAverageBpm(),
                        previousDayRestingBpm = previousDayRestingBpm.await(),
                        baselineDailyRestingHR = baselineDailyRestingHR.await(),
                    )
                } else {
                    val dailyRestingHR = async { loadDailyRestingHR(windows.current.start, windows.current.end, granted) }
                    val previousDailyRestingHR = async {
                        loadDailyRestingHR(windows.previous.start, windows.previous.end, granted)
                    }
                    val baselineDailyRestingHR = async {
                        loadDailyRestingHR(windows.baseline.start, windows.baseline.end, granted)
                    }
                    HeartPeriodData(
                        dailyRestingHR = dailyRestingHR.await(),
                        previousDailyRestingHR = previousDailyRestingHR.await(),
                        baselineDailyRestingHR = baselineDailyRestingHR.await(),
                    )
                }
                HeartPeriodMetric.HRV -> if (query.range == TimeRange.DAY) {
                    val dayHrvSamples = async { loadHrvSamplesForDay(query.selectedDate, granted) }
                    val baselineDailyHrv = async { loadDailyHRV(windows.baseline.start, windows.baseline.end, granted) }
                    val samples = dayHrvSamples.await()
                    HeartPeriodData(
                        dayHrvSamples = samples,
                        dayHrvMs = samples.averageRmssdMs(),
                        baselineDailyHrv = baselineDailyHrv.await(),
                    )
                } else {
                    val dailyHrv = async { loadDailyHRV(windows.current.start, windows.current.end, granted) }
                    val previousDailyHrv = async { loadDailyHRV(windows.previous.start, windows.previous.end, granted) }
                    val baselineDailyHrv = async { loadDailyHRV(windows.baseline.start, windows.baseline.end, granted) }
                    HeartPeriodData(
                        dailyHrv = dailyHrv.await(),
                        previousDailyHrv = previousDailyHrv.await(),
                        baselineDailyHrv = baselineDailyHrv.await(),
                    )
                }
            }
        }
        return data
    }

    private suspend fun loadAllHeartPeriod(
        query: PeriodLoadQuery,
        granted: Set<String>,
    ): HeartPeriodData = coroutineScope {
        if (query.range == TimeRange.DAY) {
            val daySamples = async { loadRawHeartRateSamplesForDayGraph(query.selectedDate, granted) }
            val dayRestingSamples = async { loadRestingHeartRateSamplesForDay(query.selectedDate, granted) }
            val dayHrvSamples = async { loadHrvSamplesForDay(query.selectedDate, granted) }
            val restingSamples = dayRestingSamples.await()
            val hrvSamples = dayHrvSamples.await()
            HeartPeriodData(
                daySamples = daySamples.await(),
                dayRestingSamples = restingSamples,
                dayRestingBpm = restingSamples.dayAverageBpm(),
                dayHrvSamples = hrvSamples,
                dayHrvMs = hrvSamples.averageRmssdMs(),
            )
        } else {
            val current = query.windows.current
            val dailySummaries = async {
                loadDailyHeartRateSummaries(current.start, current.end, granted)
            }
            val dailyRestingHR = async { loadDailyRestingHR(current.start, current.end, granted) }
            val dailyHrv = async { loadDailyHRV(current.start, current.end, granted) }
            HeartPeriodData(
                dailySummaries = dailySummaries.await(),
                dailyRestingHR = dailyRestingHR.await(),
                dailyHrv = dailyHrv.await(),
            )
        }
    }

    override suspend fun loadAvgHeartRate(date: LocalDate): Long? {
        val granted = grantedPermissionsIfAvailable()
        return loadAvgHeartRate(date, granted)
    }

    private suspend fun loadAvgHeartRate(date: LocalDate, granted: Set<String>): Long? {
        if (readHeartRatePermission !in granted) {
            Log.w(TAG, "Skipping loadAvgHeartRate missingCount=1")
            return null
        }
        return hc.readAvgHeartRate(date)
    }

    override suspend fun loadRawHeartRateSamplesForDayGraph(date: LocalDate): List<HeartRateSample> {
        val granted = grantedPermissionsIfAvailable()
        return loadRawHeartRateSamplesForDayGraph(date, granted)
    }

    private suspend fun loadRawHeartRateSamplesForDayGraph(
        date: LocalDate,
        granted: Set<String>,
    ): List<HeartRateSample> {
        if (readHeartRatePermission !in granted) {
            Log.w(TAG, "Skipping loadRawHeartRateSamplesForDayGraph missingCount=1")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return hc.readRawHeartRateSamples(start, end)
    }

    private suspend fun loadRestingHeartRateSamplesForDay(
        date: LocalDate,
        granted: Set<String>,
    ): List<RestingHeartRateSample> {
        if (readRestingHRPermission !in granted) {
            Log.w(TAG, "Skipping loadRestingHeartRateSamplesForDay missingCount=1")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return hc.readRestingHeartRateSamples(start, end)
    }

    private suspend fun loadHrvSamplesForDay(
        date: LocalDate,
        granted: Set<String>,
    ): List<HrvSample> {
        if (readHrvPermission !in granted) {
            Log.w(TAG, "Skipping loadHrvSamplesForDay missingCount=1")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return hc.readHrvSamples(start, end)
    }

    override suspend fun loadHeartRateSamples(start: LocalDate, end: LocalDate): List<HeartRateSample> {
        val granted = grantedPermissionsIfAvailable()
        return loadHeartRateSamples(start, end, granted)
    }

    override suspend fun loadHeartRateSamples(start: Instant, end: Instant): List<HeartRateSample> {
        val granted = grantedPermissionsIfAvailable()
        return loadHeartRateSamples(start, end, granted)
    }

    override suspend fun loadMaxHeartRate(start: Instant, end: Instant): Long? {
        if (readHeartRatePermission !in grantedPermissionsIfAvailable()) {
            Log.w(TAG, "Skipping loadMaxHeartRate missingCount=1")
            return null
        }
        return hc.readMaxHeartRate(start, end)
    }

    private suspend fun loadHeartRateSamples(
        start: Instant,
        end: Instant,
        granted: Set<String>,
    ): List<HeartRateSample> {
        if (readHeartRatePermission !in granted) {
            Log.w(TAG, "Skipping loadHeartRateSamples missingCount=1")
            return emptyList()
        }
        if (!end.isAfter(start)) return emptyList()
        // The reader widens the read past the record bounds and clips the samples to the window.
        return hc.readRawHeartRateSamples(start, end).reducedForChart()
    }

    private suspend fun loadHeartRateSamples(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<HeartRateSample> {
        if (readHeartRatePermission !in granted) {
            Log.w(TAG, "Skipping loadHeartRateSamples missingCount=1")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val startInstant = start.atStartOfDay(zone).toInstant()
        val endInstant = end.plusDays(1).atStartOfDay(zone).toInstant()
        return hc.readHeartRateSamplesForInsights(startInstant, endInstant)
            .groupBy { it.time.atZone(zone).toLocalDate() }
            .flatMap { (_, daySamples) -> daySamples.reducedForChart() }
    }

    override suspend fun loadDailyHeartRateSummaries(start: LocalDate, end: LocalDate): List<HeartRateSummary> {
        val granted = grantedPermissionsIfAvailable()
        return loadDailyHeartRateSummaries(start, end, granted)
    }

    private suspend fun loadDailyHeartRateSummaries(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<HeartRateSummary> {
        if (readHeartRatePermission !in granted) {
            Log.w(TAG, "Skipping loadDailyHeartRateSummaries missingCount=1")
            return emptyList()
        }
        val days = hc.readDailyHeartRateAggregates(start, end)
        val averages = rawDayAverages(days)
        return days.map { day ->
            val average = averages[day.summary.date] ?: return@map day.summary
            day.summary.copy(avgBpm = average.roundToLong())
        }
    }

    /**
     * Each day's average as the day view computes it, from raw samples. Inside an
     * hour, Health Connect weights by sample, so a 1 Hz workout still pulls the
     * hourly fold up. Cached per day and read again when the day's hourly signature
     * changes. Days past the load's budget are missing from the map.
     */
    private suspend fun rawDayAverages(days: List<HeartRateDayAggregate>): Map<LocalDate, Double> {
        val cache = dayCache ?: return emptyMap()
        if (days.isEmpty()) return emptyMap()
        val averages = HashMap<LocalDate, Double>()
        val first = days.minOf { it.summary.date }.toEpochDay()
        val last = days.maxOf { it.summary.date }.toEpochDay()
        if (takeCached(cache.daysBetween(first, last), days, averages).isEmpty()) return averages
        dayCacheFill.withLock {
            // A parallel load may have read these days while this one waited.
            val stale = takeCached(cache.daysBetween(first, last), days, averages)
            val reads = planRawDayReads(
                days = days,
                stale = stale.mapTo(HashSet()) { it.summary.date },
                readSamples = DayCacheReadSamples,
                loadSamples = DayCacheLoadSamples,
            )
            for (read in reads) {
                val oldest = read.last().summary.date
                val newest = read.first().summary.date
                val rawAverages = hc.readDailyHeartRateAverages(oldest, newest)
                val rows = read.mapNotNull { day ->
                    val average = rawAverages[day.summary.date] ?: return@mapNotNull null
                    HeartRateDayEntity(day.summary.date.toEpochDay(), cacheSignature(day), average)
                }
                if (rows.isEmpty()) continue
                cache.upsert(rows)
                rows.forEach { averages[LocalDate.ofEpochDay(it.epochDay)] = it.averageBpm }
            }
        }
        return averages
    }

    /** Puts each day whose cached row still matches into [averages]. Returns the other days. */
    private fun takeCached(
        rows: List<HeartRateDayEntity>,
        days: List<HeartRateDayAggregate>,
        averages: MutableMap<LocalDate, Double>,
    ): List<HeartRateDayAggregate> {
        val byDay = rows.associateBy { it.epochDay }
        return days.filter { day ->
            val row = byDay[day.summary.date.toEpochDay()]
            if (row == null || row.signature != cacheSignature(day)) return@filter true
            averages[day.summary.date] = row.averageBpm
            false
        }
    }

    private fun cacheSignature(day: HeartRateDayAggregate): String = "v$DayCacheVersion|${day.signature}"

    override suspend fun loadRestingHeartRate(date: LocalDate): Long? {
        val granted = grantedPermissionsIfAvailable()
        return loadRestingHeartRate(date, granted)
    }

    private suspend fun loadRestingHeartRate(
        date: LocalDate,
        granted: Set<String>,
    ): Long? {
        if (readRestingHRPermission !in granted) return null
        return hc.readRestingHeartRate(date)
    }

    override suspend fun loadDailyRestingHR(start: LocalDate, end: LocalDate): List<DailyRestingHR> {
        val granted = grantedPermissionsIfAvailable()
        return loadDailyRestingHR(start, end, granted)
    }

    private suspend fun loadDailyRestingHR(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<DailyRestingHR> {
        if (readRestingHRPermission !in granted) {
            Log.w(TAG, "Skipping loadDailyRestingHR missingCount=1")
            return emptyList()
        }
        return hc.readDailyRestingHR(start, end)
    }

    override suspend fun loadHrvRmssd(date: LocalDate): Double? {
        val granted = grantedPermissionsIfAvailable()
        return loadHrvRmssd(date, granted)
    }

    override suspend fun loadHrvSamples(start: Instant, end: Instant): List<HrvSample> {
        val granted = grantedPermissionsIfAvailable()
        if (readHrvPermission !in granted) {
            Log.w(TAG, "Skipping loadHrvSamples missingCount=1")
            return emptyList()
        }
        return hc.readHrvSamples(start, end)
    }

    private suspend fun loadHrvRmssd(
        date: LocalDate,
        granted: Set<String>,
    ): Double? = loadHrvSamplesForDay(date, granted).averageRmssdMs()

    override suspend fun loadDailyHRV(start: LocalDate, end: LocalDate): List<DailyHrv> {
        val granted = grantedPermissionsIfAvailable()
        return loadDailyHRV(start, end, granted)
    }

    private suspend fun loadDailyHRV(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<DailyHrv> {
        if (readHrvPermission !in granted) {
            Log.w(TAG, "Skipping loadDailyHRV missingCount=1")
            return emptyList()
        }
        return hc.readDailyHRV(start, end)
    }
}

private fun List<HrvSample>.averageRmssdMs(): Double? =
    timeBucketedAverageOrNull(time = { it.time }, value = { it.rmssdMs })

enum class HeartPeriodMetric {
    ALL,
    AVERAGE_HEART_RATE,
    RESTING_HEART_RATE,
    HRV,
}
