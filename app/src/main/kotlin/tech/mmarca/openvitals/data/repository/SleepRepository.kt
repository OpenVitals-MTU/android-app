package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.model.mergeSleepSessions
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId

class SleepRepository(private val hc: HealthConnectManager) {

    companion object {
        private const val TAG = "SleepRepository"
    }

    private val readSleepPermission = HealthPermission.getReadPermission(SleepSessionRecord::class)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

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
}
