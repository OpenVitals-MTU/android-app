package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import tech.mmarca.openvitals.data.model.DailyMacros
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.NutritionEntry
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId

class NutritionRepository(private val hc: HealthConnectManager) {

    companion object {
        private const val TAG = "NutritionRepository"
    }

    private val readNutritionPermission = HealthPermission.getReadPermission(NutritionRecord::class)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun loadDailyMacros(start: LocalDate, end: LocalDate): List<DailyMacros> {
        val granted = grantedPermissionsIfAvailable()
        if (readNutritionPermission !in granted) {
            Log.w(TAG, "Skipping loadDailyMacros start=$start end=$end missing=$readNutritionPermission")
            return emptyList()
        }
        return hc.readDailyMacros(start, end)
    }

    suspend fun loadNutritionEntries(start: LocalDate, end: LocalDate): List<NutritionEntry> {
        val granted = grantedPermissionsIfAvailable()
        if (readNutritionPermission !in granted) {
            Log.w(TAG, "Skipping loadNutritionEntries start=$start end=$end missing=$readNutritionPermission")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val startInstant = start.atStartOfDay(zone).toInstant()
        val endInstant = end.plusDays(1).atStartOfDay(zone).toInstant()
        return hc.readNutritionEntries(startInstant, endInstant)
    }
}
