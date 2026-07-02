package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.model.HrvSample
import tech.mmarca.openvitals.domain.model.reducedForChart
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.RestingHeartRateSample
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

@Singleton
class HeartRepositoryImpl @Inject constructor(
    private val hc: HealthConnectManager,
) : HeartRepository {

    companion object {
        private const val TAG = "HeartRepository"
    }

    private val readHeartRatePermission = HealthPermission.getReadPermission(HeartRateRecord::class)
    private val readRestingHRPermission = HealthPermission.getReadPermission(RestingHeartRateRecord::class)
    private val readHrvPermission = HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    @Suppress("UNUSED_PARAMETER")
    override suspend fun loadHeartPeriod(
        query: PeriodLoadQuery,
        metric: HeartPeriodMetric,
        refreshMode: RefreshMode,
    ): HeartPeriodData {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        val data = coroutineScope {
            when (metric) {
                HeartPeriodMetric.ALL -> loadAllHeartPeriod(query, granted)
                HeartPeriodMetric.AVERAGE_HEART_RATE -> if (query.range == TimeRange.DAY) {
                    val daySamples = async { loadRawHeartRateSamplesForDayGraph(query.selectedDate, granted) }
                    val previousDaySamples = async { loadHeartRateSamples(windows.previous.start, granted) }
                    val baselineDailySummaries = async {
                        loadDailyHeartRateSummaries(windows.baseline.start, windows.baseline.end, granted)
                    }
                    HeartPeriodData(
                        daySamples = daySamples.await(),
                        previousDaySamples = previousDaySamples.await(),
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
                        dayRestingBpm = samples.averageRestingBpm(),
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
        return if (query.range == TimeRange.DAY) {
            enrichDayHeartRateSamples(data, query, metric, granted)
        } else {
            data
        }
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
                dayRestingBpm = restingSamples.averageRestingBpm(),
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

    override suspend fun loadHeartRateSamples(date: LocalDate): List<HeartRateSample> {
        val granted = grantedPermissionsIfAvailable()
        return loadHeartRateSamples(date, granted)
    }

    private suspend fun loadHeartRateSamples(
        date: LocalDate,
        granted: Set<String>,
    ): List<HeartRateSample> {
        if (readHeartRatePermission !in granted) {
            Log.w(TAG, "Skipping loadHeartRateSamples missingCount=1")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return hc.readHeartRateSamples(start, end).reducedForChart()
    }

    private suspend fun enrichDayHeartRateSamples(
        data: HeartPeriodData,
        query: PeriodLoadQuery,
        metric: HeartPeriodMetric,
        granted: Set<String>,
    ): HeartPeriodData = coroutineScope {
        when (metric) {
            HeartPeriodMetric.ALL -> if (data.daySamples.isEmpty()) {
                data.copy(daySamples = loadRawHeartRateSamplesForDayGraph(query.selectedDate, granted))
            } else {
                data
            }
            HeartPeriodMetric.AVERAGE_HEART_RATE -> {
                val daySamples = if (data.daySamples.isEmpty()) {
                    async { loadRawHeartRateSamplesForDayGraph(query.selectedDate, granted) }
                } else {
                    null
                }
                val previousDaySamples = if (data.previousDaySamples.isEmpty()) {
                    async { loadHeartRateSamples(query.windows.previous.start, granted) }
                } else {
                    null
                }
                data.copy(
                    daySamples = daySamples?.await() ?: data.daySamples,
                    previousDaySamples = previousDaySamples?.await() ?: data.previousDaySamples,
                )
            }
            else -> data
        }
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

    private suspend fun loadHeartRateSamples(
        start: Instant,
        end: Instant,
        granted: Set<String>,
    ): List<HeartRateSample> {
        if (readHeartRatePermission !in granted) {
            Log.w(TAG, "Skipping loadHeartRateSamples missingCount=1")
            return emptyList()
        }
        return hc.readHeartRateSamples(start, end).reducedForChart()
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
        return hc.readHeartRateSamples(startInstant, endInstant)
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
        return hc.readDailyHeartRateSummaries(start, end)
    }

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
    ): Double? {
        return loadDailyHRV(date, date, granted).firstOrNull { it.date == date }?.rmssdMs
    }

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

private fun List<RestingHeartRateSample>.averageRestingBpm(): Long? =
    takeIf { it.isNotEmpty() }?.map { it.beatsPerMinute }?.average()?.roundToLong()

private fun List<HrvSample>.averageRmssdMs(): Double? =
    takeIf { it.isNotEmpty() }?.map { it.rmssdMs }?.average()

enum class HeartPeriodMetric {
    ALL,
    AVERAGE_HEART_RATE,
    RESTING_HEART_RATE,
    HRV,
}
