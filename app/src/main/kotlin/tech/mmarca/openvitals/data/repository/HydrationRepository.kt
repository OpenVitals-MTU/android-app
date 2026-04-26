package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import tech.mmarca.openvitals.data.model.DailyHydration
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate

class HydrationRepository(private val hc: HealthConnectManager) {

    companion object {
        private const val TAG = "HydrationRepository"
    }

    private val readHydrationPermission = HealthPermission.getReadPermission(HydrationRecord::class)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun loadDailyHydration(start: LocalDate, end: LocalDate): List<DailyHydration> {
        val granted = grantedPermissionsIfAvailable()
        if (readHydrationPermission !in granted) {
            Log.w(TAG, "Skipping loadDailyHydration start=$start end=$end missing=$readHydrationPermission")
            return emptyList()
        }
        return hc.readDailyHydration(start, end)
    }
}
