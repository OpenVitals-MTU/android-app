package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.data.model.DailyHydration
import tech.mmarca.openvitals.data.model.HydrationEntry
import tech.mmarca.openvitals.data.model.HydrationWriteRequest
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.HealthConnectQueryCache
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class HydrationRepository @Inject constructor(
    private val hc: HealthConnectManager,
    private val queryCache: HealthConnectQueryCache = HealthConnectQueryCache(),
    private val preferencesRepository: PreferencesRepository? = null,
) {

    companion object {
        private const val TAG = "HydrationRepository"
    }

    private val readHydrationPermission = HealthPermission.getReadPermission(HydrationRecord::class)
    private val writeHydrationPermission = HealthPermission.getWritePermission(HydrationRecord::class)
    val hydrationWritePermissions: Set<String> get() = setOf(writeHydrationPermission)

    fun hydrationContainerVolumeMilliliters(): Map<String, Double> =
        preferencesRepository?.hydrationContainerVolumeMilliliters().orEmpty()

    fun setHydrationContainerVolumeMilliliters(containerId: String, milliliters: Double) {
        preferencesRepository?.setHydrationContainerVolumeMilliliters(containerId, milliliters)
    }

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun loadHydrationPeriod(query: PeriodLoadQuery): HydrationPeriodData = coroutineScope {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        val dailyHydration = async { loadDailyHydration(windows.current.start, windows.current.end, granted) }
        val previousDailyHydration = async { loadDailyHydration(windows.previous.start, windows.previous.end, granted) }
        val baselineDailyHydration = async { loadDailyHydration(windows.baseline.start, windows.baseline.end, granted) }
        val hydrationEntries = async { loadHydrationEntries(windows.current.start, windows.current.end, granted) }
        HydrationPeriodData(
            dailyHydration = dailyHydration.await(),
            previousDailyHydration = previousDailyHydration.await(),
            baselineDailyHydration = baselineDailyHydration.await(),
            hydrationEntries = hydrationEntries.await(),
        )
    }

    suspend fun loadDailyHydration(start: LocalDate, end: LocalDate): List<DailyHydration> {
        val granted = grantedPermissionsIfAvailable()
        return loadDailyHydration(start, end, granted)
    }

    private suspend fun loadDailyHydration(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<DailyHydration> {
        if (readHydrationPermission !in granted) {
            Log.w(TAG, "Skipping loadDailyHydration missingCount=1")
            return emptyList()
        }
        return hc.readDailyHydration(start, end)
    }

    suspend fun loadHydrationEntries(start: LocalDate, end: LocalDate): List<HydrationEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadHydrationEntries(start, end, granted)
    }

    private suspend fun loadHydrationEntries(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<HydrationEntry> {
        if (readHydrationPermission !in granted) {
            Log.w(TAG, "Skipping loadHydrationEntries missingCount=1")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        return hc.readHydrationEntries(
            start = start.atStartOfDay(zone).toInstant(),
            end = end.plusDays(1).atStartOfDay(zone).toInstant(),
        )
    }

    suspend fun hasHydrationWritePermission(): Boolean =
        writeHydrationPermission in grantedPermissionsIfAvailable()

    suspend fun writeHydrationEntry(request: HydrationWriteRequest): String {
        val granted = grantedPermissionsIfAvailable()
        if (writeHydrationPermission !in granted) {
            Log.w(TAG, "Skipping writeHydrationEntry missingCount=1")
            throw SecurityException("Missing Health Connect hydration write permission.")
        }
        return hc.writeHydrationEntry(request).also {
            queryCache.invalidateOperations("dashboard")
        }
    }

    suspend fun loadHydrationEntry(id: String): HydrationEntry? {
        val granted = grantedPermissionsIfAvailable()
        if (readHydrationPermission !in granted) {
            Log.w(TAG, "Skipping loadHydrationEntry missingCount=1")
            return null
        }
        return hc.readHydrationEntry(id)
    }

    suspend fun updateHydrationEntry(id: String, request: HydrationWriteRequest) {
        val granted = grantedPermissionsIfAvailable()
        if (writeHydrationPermission !in granted) {
            Log.w(TAG, "Skipping updateHydrationEntry missingCount=1")
            throw SecurityException("Missing Health Connect hydration write permission.")
        }
        hc.updateHydrationEntry(id, request)
        queryCache.invalidateOperations("dashboard")
    }

    suspend fun deleteHydrationEntry(id: String) {
        val granted = grantedPermissionsIfAvailable()
        if (writeHydrationPermission !in granted) {
            Log.w(TAG, "Skipping deleteHydrationEntry missingCount=1")
            throw SecurityException("Missing Health Connect hydration write permission.")
        }
        hc.deleteHydrationEntry(id)
        queryCache.invalidateOperations("dashboard")
    }
}

data class HydrationPeriodData(
    val dailyHydration: List<DailyHydration> = emptyList(),
    val previousDailyHydration: List<DailyHydration> = emptyList(),
    val baselineDailyHydration: List<DailyHydration> = emptyList(),
    val hydrationEntries: List<HydrationEntry> = emptyList(),
)
