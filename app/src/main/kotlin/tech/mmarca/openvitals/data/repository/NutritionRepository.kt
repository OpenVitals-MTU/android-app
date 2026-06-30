package tech.mmarca.openvitals.data.repository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.query.NutritionPeriodData
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class NutritionRepositoryImpl @Inject constructor(
    private val hc: HealthConnectManager,
) : NutritionRepository {

    companion object {
        private const val TAG = "NutritionRepository"
    }

    private val readNutritionPermission = HealthPermission.getReadPermission(NutritionRecord::class)
    private val writeNutritionPermission = HealthPermission.getWritePermission(NutritionRecord::class)
    override val nutritionWritePermissions: Set<String> get() = setOf(writeNutritionPermission)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    @Suppress("UNUSED_PARAMETER")
    override suspend fun loadNutritionPeriod(
        query: PeriodLoadQuery,
        refreshMode: RefreshMode,
    ): NutritionPeriodData {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        return coroutineScope {
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
    }

    override suspend fun loadDailyMacros(start: LocalDate, end: LocalDate): List<DailyMacros> {
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

    override suspend fun loadNutritionEntries(start: LocalDate, end: LocalDate): List<NutritionEntry> {
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

    override suspend fun hasNutritionWritePermission(): Boolean =
        writeNutritionPermission in grantedPermissionsIfAvailable()

    override suspend fun writeCarbsEntry(request: NutritionWriteRequest): String {
        val granted = grantedPermissionsIfAvailable()
        if (writeNutritionPermission !in granted) {
            Log.w(TAG, "Skipping writeCarbsEntry missingCount=1")
            throw SecurityException("Missing Health Connect nutrition write permission.")
        }
        return hc.writeCarbsEntry(request)
    }
}
