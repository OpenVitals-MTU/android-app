package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.data.model.DailyMacros
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.NutritionEntry
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionRepository @Inject constructor(
    private val hc: HealthConnectManager,
) {

    companion object {
        private const val TAG = "NutritionRepository"
    }

    private val readNutritionPermission = HealthPermission.getReadPermission(NutritionRecord::class)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun loadNutritionPeriod(query: PeriodLoadQuery): NutritionPeriodData {
        val windows = query.windows
        return NutritionPeriodData(
            dailyMacros = loadDailyMacros(windows.current.start, windows.current.end),
            previousDailyMacros = loadDailyMacros(windows.previous.start, windows.previous.end),
            baselineDailyMacros = loadDailyMacros(windows.baseline.start, windows.baseline.end),
            entries = loadNutritionEntries(windows.current.start, windows.current.end),
        )
    }

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

data class NutritionPeriodData(
    val dailyMacros: List<DailyMacros> = emptyList(),
    val previousDailyMacros: List<DailyMacros> = emptyList(),
    val baselineDailyMacros: List<DailyMacros> = emptyList(),
    val entries: List<NutritionEntry> = emptyList(),
)
