package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.model.DailyHrv
import tech.mmarca.openvitals.data.model.DailyRestingHR
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.HeartRateSample
import tech.mmarca.openvitals.data.model.HeartRateSummary
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class HeartRepository @Inject constructor(
    private val hc: HealthConnectManager,
) {

    companion object {
        private const val TAG = "HeartRepository"
    }

    private val readHeartRatePermission = HealthPermission.getReadPermission(HeartRateRecord::class)
    private val readRestingHRPermission = HealthPermission.getReadPermission(RestingHeartRateRecord::class)
    private val readHrvPermission = HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun loadHeartPeriod(query: PeriodLoadQuery, metric: HeartPeriodMetric): HeartPeriodData = coroutineScope {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        when (metric) {
            HeartPeriodMetric.ALL -> loadAllHeartPeriod(query, granted)
            HeartPeriodMetric.AVERAGE_HEART_RATE -> if (query.range == TimeRange.DAY) {
                val daySamples = async { loadHeartRateSamples(query.selectedDate, granted) }
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
                val dayRestingBpm = async { loadRestingHeartRate(query.selectedDate, granted) }
                val previousDayRestingBpm = async { loadRestingHeartRate(windows.previous.start, granted) }
                val baselineDailyRestingHR = async {
                    loadDailyRestingHR(windows.baseline.start, windows.baseline.end, granted)
                }
                HeartPeriodData(
                    dayRestingBpm = dayRestingBpm.await(),
                    previousDayRestingBpm = previousDayRestingBpm.await(),
                    baselineDailyRestingHR = baselineDailyRestingHR.await(),
                )
            } else {
                val dailyRestingHR = async { loadDailyRestingHR(windows.current.start, windows.current.end, granted) }
                val previousDailyRestingHR = async { loadDailyRestingHR(windows.previous.start, windows.previous.end, granted) }
                val baselineDailyRestingHR = async { loadDailyRestingHR(windows.baseline.start, windows.baseline.end, granted) }
                HeartPeriodData(
                    dailyRestingHR = dailyRestingHR.await(),
                    previousDailyRestingHR = previousDailyRestingHR.await(),
                    baselineDailyRestingHR = baselineDailyRestingHR.await(),
                )
            }
            HeartPeriodMetric.HRV -> if (query.range == TimeRange.DAY) {
                val dayHrvMs = async { loadHrvRmssd(query.selectedDate, granted) }
                val previousDayHrvMs = async { loadHrvRmssd(windows.previous.start, granted) }
                val baselineDailyHrv = async { loadDailyHRV(windows.baseline.start, windows.baseline.end, granted) }
                HeartPeriodData(
                    dayHrvMs = dayHrvMs.await(),
                    previousDayHrvMs = previousDayHrvMs.await(),
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

    private suspend fun loadAllHeartPeriod(
        query: PeriodLoadQuery,
        granted: Set<String>,
    ): HeartPeriodData = coroutineScope {
        if (query.range == TimeRange.DAY) {
            val daySamples = async { loadHeartRateSamples(query.selectedDate, granted) }
            val dayRestingBpm = async { loadRestingHeartRate(query.selectedDate, granted) }
            val dayHrvMs = async { loadHrvRmssd(query.selectedDate, granted) }
            HeartPeriodData(
                daySamples = daySamples.await(),
                dayRestingBpm = dayRestingBpm.await(),
                dayHrvMs = dayHrvMs.await(),
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

    suspend fun loadHeartRateSamples(date: LocalDate): List<HeartRateSample> {
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
        return hc.readHeartRateSamples(start, end)
    }

    suspend fun loadHeartRateSamples(start: LocalDate, end: LocalDate): List<HeartRateSample> {
        val granted = grantedPermissionsIfAvailable()
        return loadHeartRateSamples(start, end, granted)
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
    }

    suspend fun loadDailyHeartRateSummaries(start: LocalDate, end: LocalDate): List<HeartRateSummary> {
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

    suspend fun loadRestingHeartRate(date: LocalDate): Long? {
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

    suspend fun loadDailyRestingHR(start: LocalDate, end: LocalDate): List<DailyRestingHR> {
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

    suspend fun loadHrvRmssd(date: LocalDate): Double? {
        val granted = grantedPermissionsIfAvailable()
        return loadHrvRmssd(date, granted)
    }

    private suspend fun loadHrvRmssd(
        date: LocalDate,
        granted: Set<String>,
    ): Double? {
        if (readHrvPermission !in granted) return null
        return hc.readHrvRmssd(date)
    }

    suspend fun loadDailyHRV(start: LocalDate, end: LocalDate): List<DailyHrv> {
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

enum class HeartPeriodMetric {
    ALL,
    AVERAGE_HEART_RATE,
    RESTING_HEART_RATE,
    HRV,
}

data class HeartPeriodData(
    val daySamples: List<HeartRateSample> = emptyList(),
    val previousDaySamples: List<HeartRateSample> = emptyList(),
    val dailySummaries: List<HeartRateSummary> = emptyList(),
    val previousDailySummaries: List<HeartRateSummary> = emptyList(),
    val baselineDailySummaries: List<HeartRateSummary> = emptyList(),
    val dayRestingBpm: Long? = null,
    val previousDayRestingBpm: Long? = null,
    val dayHrvMs: Double? = null,
    val previousDayHrvMs: Double? = null,
    val dailyRestingHR: List<DailyRestingHR> = emptyList(),
    val previousDailyRestingHR: List<DailyRestingHR> = emptyList(),
    val baselineDailyRestingHR: List<DailyRestingHR> = emptyList(),
    val dailyHrv: List<DailyHrv> = emptyList(),
    val previousDailyHrv: List<DailyHrv> = emptyList(),
    val baselineDailyHrv: List<DailyHrv> = emptyList(),
)
