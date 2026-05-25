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
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HydrationRepository @Inject constructor(
    private val hc: HealthConnectManager,
) {

    companion object {
        private const val TAG = "HydrationRepository"
    }

    private val readHydrationPermission = HealthPermission.getReadPermission(HydrationRecord::class)
    private val writeHydrationPermission = HealthPermission.getWritePermission(HydrationRecord::class)
    val hydrationWritePermissions: Set<String> get() = setOf(writeHydrationPermission)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun loadHydrationPeriod(query: PeriodLoadQuery): HydrationPeriodData {
        val windows = query.windows
        return HydrationPeriodData(
            dailyHydration = loadDailyHydration(windows.current.start, windows.current.end),
            previousDailyHydration = loadDailyHydration(windows.previous.start, windows.previous.end),
            baselineDailyHydration = loadDailyHydration(windows.baseline.start, windows.baseline.end),
            hydrationEntries = loadHydrationEntries(windows.current.start, windows.current.end),
        )
    }

    suspend fun loadDailyHydration(start: LocalDate, end: LocalDate): List<DailyHydration> {
        val granted = grantedPermissionsIfAvailable()
        if (readHydrationPermission !in granted) {
            Log.w(TAG, "Skipping loadDailyHydration start=$start end=$end missing=$readHydrationPermission")
            return emptyList()
        }
        return hc.readDailyHydration(start, end)
    }

    suspend fun loadHydrationEntries(start: LocalDate, end: LocalDate): List<HydrationEntry> {
        val granted = grantedPermissionsIfAvailable()
        if (readHydrationPermission !in granted) {
            Log.w(TAG, "Skipping loadHydrationEntries start=$start end=$end missing=$readHydrationPermission")
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
            Log.w(TAG, "Skipping writeHydrationEntry missing=$writeHydrationPermission")
            throw SecurityException("Missing Health Connect hydration write permission.")
        }
        return hc.writeHydrationEntry(request)
    }
}

data class HydrationPeriodData(
    val dailyHydration: List<DailyHydration> = emptyList(),
    val previousDailyHydration: List<DailyHydration> = emptyList(),
    val baselineDailyHydration: List<DailyHydration> = emptyList(),
    val hydrationEntries: List<HydrationEntry> = emptyList(),
)
