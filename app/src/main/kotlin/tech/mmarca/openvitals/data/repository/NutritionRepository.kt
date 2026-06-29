package tech.mmarca.openvitals.data.repository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.performance.AppCoroutineScope
import tech.mmarca.openvitals.data.cache.CachedPeriodRepositoryLoader
import tech.mmarca.openvitals.data.cache.MetricSummaryCacheStore
import tech.mmarca.openvitals.data.cache.NutritionPeriodDataCodec
import tech.mmarca.openvitals.data.cache.periodSummaryKey
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.query.NutritionPeriodData
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.permissionFingerprint
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class NutritionRepositoryImpl @Inject constructor(
    private val hc: HealthConnectManager,
    private val metricSummaryCacheStore: MetricSummaryCacheStore? = null,
    @param:AppCoroutineScope private val appScope: CoroutineScope? = null,
) : NutritionRepository {

    companion object {
        private const val TAG = "NutritionRepository"
    }

    private val readNutritionPermission = HealthPermission.getReadPermission(NutritionRecord::class)
    private val writeNutritionPermission = HealthPermission.getWritePermission(NutritionRecord::class)
    override val nutritionWritePermissions: Set<String> get() = setOf(writeNutritionPermission)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    override suspend fun loadNutritionPeriod(
        query: PeriodLoadQuery,
        refreshMode: RefreshMode,
    ): NutritionPeriodData {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        val key = periodSummaryKey(
            surface = NutritionPeriodDataCodec.Surface,
            query = query,
            metricSet = "nutrition",
            permissionFingerprint = granted.permissionFingerprint(),
            schemaVersion = NutritionPeriodDataCodec.SchemaVersion,
        )
        return periodCacheLoader().load(
            key = key,
            refreshMode = refreshMode,
            decode = NutritionPeriodDataCodec::decode,
            encode = NutritionPeriodDataCodec::encode,
        ) {
            coroutineScope {
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
    }

    private fun periodCacheLoader(): CachedPeriodRepositoryLoader =
        CachedPeriodRepositoryLoader(
            cacheStore = metricSummaryCacheStore,
            appScope = appScope,
            tag = TAG,
        )

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
        return hc.writeCarbsEntry(request).also {
            metricSummaryCacheStore?.invalidateSurface(NutritionPeriodDataCodec.Surface)
        }
    }
}
