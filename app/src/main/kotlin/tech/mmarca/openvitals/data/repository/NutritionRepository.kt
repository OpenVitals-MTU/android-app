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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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

    suspend fun loadNutritionPeriod(query: PeriodLoadQuery): NutritionPeriodData = coroutineScope {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        val dailyMacros = async { loadDailyMacros(windows.current.start, windows.current.end, granted) }
        val previousDailyMacros = async { loadDailyMacros(windows.previous.start, windows.previous.end, granted) }
        val baselineDailyMacros = async { loadDailyMacros(windows.baseline.start, windows.baseline.end, granted) }
        val entries = async { loadNutritionEntries(windows.current.start, windows.current.end, granted) }
        NutritionPeriodData(
            dailyMacros = dailyMacros.await(),
            previousDailyMacros = previousDailyMacros.await(),
            baselineDailyMacros = baselineDailyMacros.await(),
            entries = entries.await(),
        )
    }

    suspend fun loadDailyMacros(start: LocalDate, end: LocalDate): List<DailyMacros> {
        val granted = grantedPermissionsIfAvailable()
        return loadDailyMacros(start, end, granted)
    }

    private suspend fun loadDailyMacros(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<DailyMacros> {
        if (readNutritionPermission !in granted) {
            Log.w(TAG, "Skipping loadDailyMacros missingCount=1")
            return emptyList()
        }
        return hc.readDailyMacros(start, end)
    }

    suspend fun loadNutritionEntries(start: LocalDate, end: LocalDate): List<NutritionEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadNutritionEntries(start, end, granted)
    }

    private suspend fun loadNutritionEntries(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<NutritionEntry> {
        if (readNutritionPermission !in granted) {
            Log.w(TAG, "Skipping loadNutritionEntries missingCount=1")
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
