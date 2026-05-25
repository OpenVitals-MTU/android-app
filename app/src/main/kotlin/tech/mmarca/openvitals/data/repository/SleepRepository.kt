package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.model.mergeSleepSessions
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

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

    suspend fun loadSleepPeriod(query: PeriodLoadQuery, sleepRangeMode: SleepRangeMode): SleepPeriodData {
        val windows = query.windows
        return SleepPeriodData(
            sessions = loadSleepSessions(sleepQueryStart(windows.current.start, sleepRangeMode), windows.current.end),
            previousSessions = loadSleepSessions(sleepQueryStart(windows.previous.start, sleepRangeMode), windows.previous.end),
            baselineSessions = loadSleepSessions(sleepQueryStart(windows.baseline.start, sleepRangeMode), windows.baseline.end),
        )
    }

    suspend fun loadSleepSessions(start: LocalDate, end: LocalDate): List<SleepData> {
        val granted = grantedPermissionsIfAvailable()
        if (readSleepPermission !in granted) {
            Log.w(TAG, "Skipping loadSleepSessions start=$start end=$end missing=$readSleepPermission")
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
            Log.w(TAG, "Skipping loadSleepSession id=$id missing=$readSleepPermission")
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
