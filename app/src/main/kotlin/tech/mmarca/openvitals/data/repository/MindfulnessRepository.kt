package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MindfulnessSessionRecord
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.MindfulnessSession
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMindfulnessSessionApi::class)
class MindfulnessRepository(private val hc: HealthConnectManager) {

    companion object {
        private const val TAG = "MindfulnessRepository"
    }

    private val readMindfulnessPermission = HealthPermission.getReadPermission(MindfulnessSessionRecord::class)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun loadMindfulnessSessions(start: LocalDate, end: LocalDate): List<MindfulnessSession> {
        val granted = grantedPermissionsIfAvailable()
        if (readMindfulnessPermission !in granted) {
            Log.w(TAG, "Skipping loadMindfulnessSessions start=$start end=$end missing=$readMindfulnessPermission")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val startInstant = start.atStartOfDay(zone).toInstant()
        val endInstant = end.plusDays(1).atStartOfDay(zone).toInstant()
        return hc.readMindfulnessSessions(startInstant, endInstant)
    }
}
