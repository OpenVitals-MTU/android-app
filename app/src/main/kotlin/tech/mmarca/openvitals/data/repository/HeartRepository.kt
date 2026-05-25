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

    suspend fun loadHeartPeriod(query: PeriodLoadQuery, metric: HeartPeriodMetric): HeartPeriodData {
        val windows = query.windows
        return when (metric) {
            HeartPeriodMetric.AVERAGE_HEART_RATE -> if (query.range == TimeRange.DAY) {
                HeartPeriodData(
                    daySamples = loadHeartRateSamples(query.selectedDate),
                    previousDaySamples = loadHeartRateSamples(windows.previous.start),
                    baselineDailySummaries = loadDailyHeartRateSummaries(windows.baseline.start, windows.baseline.end),
                )
            } else {
                HeartPeriodData(
                    dailySummaries = loadDailyHeartRateSummaries(windows.current.start, windows.current.end),
                    previousDailySummaries = loadDailyHeartRateSummaries(windows.previous.start, windows.previous.end),
                    baselineDailySummaries = loadDailyHeartRateSummaries(windows.baseline.start, windows.baseline.end),
                )
            }
            HeartPeriodMetric.RESTING_HEART_RATE -> if (query.range == TimeRange.DAY) {
                HeartPeriodData(
                    dayRestingBpm = loadRestingHeartRate(query.selectedDate),
                    previousDayRestingBpm = loadRestingHeartRate(windows.previous.start),
                    baselineDailyRestingHR = loadDailyRestingHR(windows.baseline.start, windows.baseline.end),
                )
            } else {
                HeartPeriodData(
                    dailyRestingHR = loadDailyRestingHR(windows.current.start, windows.current.end),
                    previousDailyRestingHR = loadDailyRestingHR(windows.previous.start, windows.previous.end),
                    baselineDailyRestingHR = loadDailyRestingHR(windows.baseline.start, windows.baseline.end),
                )
            }
            HeartPeriodMetric.HRV -> if (query.range == TimeRange.DAY) {
                HeartPeriodData(
                    dayHrvMs = loadHrvRmssd(query.selectedDate),
                    previousDayHrvMs = loadHrvRmssd(windows.previous.start),
                    baselineDailyHrv = loadDailyHRV(windows.baseline.start, windows.baseline.end),
                )
            } else {
                HeartPeriodData(
                    dailyHrv = loadDailyHRV(windows.current.start, windows.current.end),
                    previousDailyHrv = loadDailyHRV(windows.previous.start, windows.previous.end),
                    baselineDailyHrv = loadDailyHRV(windows.baseline.start, windows.baseline.end),
                )
            }
        }
    }

    suspend fun loadHeartRateSamples(date: LocalDate): List<HeartRateSample> {
        val granted = grantedPermissionsIfAvailable()
        if (readHeartRatePermission !in granted) {
            Log.w(TAG, "Skipping loadHeartRateSamples date=$date missing=$readHeartRatePermission")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return hc.readHeartRateSamples(start, end)
    }

    suspend fun loadDailyHeartRateSummaries(start: LocalDate, end: LocalDate): List<HeartRateSummary> {
        val granted = grantedPermissionsIfAvailable()
        if (readHeartRatePermission !in granted) {
            Log.w(TAG, "Skipping loadDailyHeartRateSummaries start=$start end=$end missing=$readHeartRatePermission")
            return emptyList()
        }
        return hc.readDailyHeartRateSummaries(start, end)
    }

    suspend fun loadRestingHeartRate(date: LocalDate): Long? {
        val granted = grantedPermissionsIfAvailable()
        if (readRestingHRPermission !in granted) return null
        return hc.readRestingHeartRate(date)
    }

    suspend fun loadDailyRestingHR(start: LocalDate, end: LocalDate): List<DailyRestingHR> {
        val granted = grantedPermissionsIfAvailable()
        if (readRestingHRPermission !in granted) {
            Log.w(TAG, "Skipping loadDailyRestingHR start=$start end=$end missing=$readRestingHRPermission")
            return emptyList()
        }
        return hc.readDailyRestingHR(start, end)
    }

    suspend fun loadHrvRmssd(date: LocalDate): Double? {
        val granted = grantedPermissionsIfAvailable()
        if (readHrvPermission !in granted) return null
        return hc.readHrvRmssd(date)
    }

    suspend fun loadDailyHRV(start: LocalDate, end: LocalDate): List<DailyHrv> {
        val granted = grantedPermissionsIfAvailable()
        if (readHrvPermission !in granted) {
            Log.w(TAG, "Skipping loadDailyHRV start=$start end=$end missing=$readHrvPermission")
            return emptyList()
        }
        return hc.readDailyHRV(start, end)
    }
}

enum class HeartPeriodMetric {
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
