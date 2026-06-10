package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.mergeSleepSessions
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class SleepRepository @Inject constructor(
    private val hc: HealthConnectManager,
) {

    companion object {
        private const val TAG = "SleepRepository"
    }

    private val readSleepPermission = HealthPermission.getReadPermission(SleepSessionRecord::class)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun loadSleepPeriod(query: PeriodLoadQuery, sleepRangeMode: SleepRangeMode): SleepPeriodData = coroutineScope {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        val sessions = async { loadSleepSessions(sleepQueryStart(windows.current.start, sleepRangeMode), windows.current.end, granted) }
        val previousSessions = async { loadSleepSessions(sleepQueryStart(windows.previous.start, sleepRangeMode), windows.previous.end, granted) }
        val baselineSessions = async { loadSleepSessions(sleepQueryStart(windows.baseline.start, sleepRangeMode), windows.baseline.end, granted) }
        SleepPeriodData(
            sessions = sessions.await(),
            previousSessions = previousSessions.await(),
            baselineSessions = baselineSessions.await(),
        )
    }

    suspend fun loadSleepSessions(start: LocalDate, end: LocalDate): List<SleepData> {
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

    suspend fun loadSleepSession(id: String): SleepData? {
        val granted = grantedPermissionsIfAvailable()
        if (readSleepPermission !in granted) {
            Log.w(TAG, "Skipping loadSleepSession missingCount=1")
            return null
        }
        return hc.readSleepSession(id)
    }

    private fun sleepQueryStart(start: LocalDate, sleepRangeMode: SleepRangeMode): LocalDate =
        when (sleepRangeMode) {
            SleepRangeMode.ROLLING_24H -> start
            SleepRangeMode.NOON,
            SleepRangeMode.EVENING_18H -> start.minusDays(1)
        }
}

data class SleepPeriodData(
    val sessions: List<SleepData> = emptyList(),
    val previousSessions: List<SleepData> = emptyList(),
    val baselineSessions: List<SleepData> = emptyList(),
)
