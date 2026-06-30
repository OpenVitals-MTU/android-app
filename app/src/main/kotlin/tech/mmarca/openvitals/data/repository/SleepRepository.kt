package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.mergeSleepSessions
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.domain.query.SleepPeriodData
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class SleepRepositoryImpl @Inject constructor(
    private val hc: HealthConnectManager,
) : SleepRepository {

    companion object {
        private const val TAG = "SleepRepositoryImpl"
    }

    private val readSleepPermission = HealthPermission.getReadPermission(SleepSessionRecord::class)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    @Suppress("UNUSED_PARAMETER")
    override suspend fun loadSleepPeriod(
        query: PeriodLoadQuery,
        sleepRangeMode: SleepRangeMode,
        refreshMode: RefreshMode,
    ): SleepPeriodData {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        if (readSleepPermission !in granted) {
            Log.w(TAG, "Skipping loadSleepPeriod missingCount=1")
            return SleepPeriodData()
        }
        return coroutineScope {
            val current = async {
                hc.readSleepData(windows.current.start, windows.current.end, sleepRangeMode)
            }
            val previous = async {
                hc.readSleepData(windows.previous.start, windows.previous.end, sleepRangeMode)
            }
            val baseline = async {
                hc.readSleepData(windows.baseline.start, windows.baseline.end, sleepRangeMode)
            }
            val currentData = current.await()
            val previousData = previous.await()
            val baselineData = baseline.await()
            SleepPeriodData(
                sessions = currentData.sessions,
                previousSessions = previousData.sessions,
                baselineSessions = baselineData.sessions,
                dailyDurations = currentData.dailyAggregateDurations,
                previousDailyDurations = previousData.dailyAggregateDurations,
                baselineDailyDurations = baselineData.dailyAggregateDurations,
            )
        }
    }

    override suspend fun loadSleepSessions(start: LocalDate, end: LocalDate): List<SleepData> {
        val granted = grantedPermissionsIfAvailable()
        return loadSleepSessions(start, end, granted)
    }

    private suspend fun loadSleepSessions(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<SleepData> {
        if (readSleepPermission !in granted) {
            Log.w(TAG, "Skipping loadSleepSessions missingCount=1")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val queryStart = start.minusDays(1).atStartOfDay(zone).toInstant()
        val queryEnd = end.plusDays(1).atStartOfDay(zone).toInstant()
        return mergeSleepSessions(hc.readSleepSessions(queryStart, queryEnd))
            .filter { session ->
                val sessionDate = session.endTime.atZone(zone).toLocalDate()
                !sessionDate.isBefore(start) && !sessionDate.isAfter(end)
            }
    }

    override suspend fun loadSleepSession(id: String): SleepData? {
        val granted = grantedPermissionsIfAvailable()
        if (readSleepPermission !in granted) {
            Log.w(TAG, "Skipping loadSleepSession missingCount=1")
            return null
        }
        return hc.readSleepSession(id)
    }
}
