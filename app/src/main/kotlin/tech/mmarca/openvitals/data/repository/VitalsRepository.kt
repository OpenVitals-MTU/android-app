package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodWindows
import tech.mmarca.openvitals.core.performance.AppCoroutineScope
import tech.mmarca.openvitals.data.cache.CachedPeriodRepositoryLoader
import tech.mmarca.openvitals.data.cache.MetricSummaryCacheStore
import tech.mmarca.openvitals.data.cache.VitalsPeriodDataCodec
import tech.mmarca.openvitals.data.cache.periodSummaryKey
import tech.mmarca.openvitals.domain.model.BloodGlucoseEntry
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BodyTempEntry
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementEntry
import tech.mmarca.openvitals.domain.model.VitalsMeasurementWriteRequest
import tech.mmarca.openvitals.domain.model.Vo2MaxEntry
import tech.mmarca.openvitals.domain.query.VitalsPeriodData
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.HealthConnectQueryCache
import tech.mmarca.openvitals.healthconnect.permissionFingerprint
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class VitalsRepository @Inject constructor(
    private val hc: HealthConnectManager,
    private val queryCache: HealthConnectQueryCache = HealthConnectQueryCache(),
    private val metricSummaryCacheStore: MetricSummaryCacheStore? = null,
    @param:AppCoroutineScope private val appScope: CoroutineScope? = null,
) {

    companion object {
        private const val TAG = "VitalsRepository"
    }

    val phase3Permissions: Set<String> get() = hc.phase3Permissions

    private val readBloodPressurePermission = HealthPermission.getReadPermission(BloodPressureRecord::class)
    private val readSpO2Permission = HealthPermission.getReadPermission(OxygenSaturationRecord::class)
    private val readRespiratoryRatePermission = HealthPermission.getReadPermission(RespiratoryRateRecord::class)
    private val readBodyTemperaturePermission = HealthPermission.getReadPermission(BodyTemperatureRecord::class)
    private val readVo2MaxPermission = HealthPermission.getReadPermission(Vo2MaxRecord::class)
    private val readBloodGlucosePermission = HealthPermission.getReadPermission(BloodGlucoseRecord::class)
    private val readSkinTemperaturePermission = HealthPermission.getReadPermission(SkinTemperatureRecord::class)
    private val writeBloodPressurePermission = HealthPermission.getWritePermission(BloodPressureRecord::class)
    private val writeSpO2Permission = HealthPermission.getWritePermission(OxygenSaturationRecord::class)
    private val writeRespiratoryRatePermission = HealthPermission.getWritePermission(RespiratoryRateRecord::class)
    private val writeBodyTemperaturePermission = HealthPermission.getWritePermission(BodyTemperatureRecord::class)

    fun vitalsWritePermissions(type: VitalsMeasurementType): Set<String> = setOf(
        when (type) {
            VitalsMeasurementType.BLOOD_PRESSURE -> writeBloodPressurePermission
            VitalsMeasurementType.SPO2 -> writeSpO2Permission
            VitalsMeasurementType.RESPIRATORY_RATE -> writeRespiratoryRatePermission
            VitalsMeasurementType.BODY_TEMPERATURE -> writeBodyTemperaturePermission
        }
    )

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun missingPermissions(): Set<String> {
        val granted = grantedPermissionsIfAvailable()
        return phase3Permissions.filterNot { it in granted }.toSet()
    }

    suspend fun loadVitalsPeriod(
        query: PeriodLoadQuery,
        metric: VitalsPeriodMetric,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): VitalsPeriodData {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        val missingPermissions = phase3Permissions.filterNot { it in granted }.toSet()
        val key = periodSummaryKey(
            surface = VitalsPeriodDataCodec.Surface,
            query = query,
            metricSet = metric.name,
            permissionFingerprint = granted.permissionFingerprint(),
            schemaVersion = VitalsPeriodDataCodec.SchemaVersion,
        )
        return periodCacheLoader().load(
            key = key,
            refreshMode = refreshMode,
            decode = VitalsPeriodDataCodec::decode,
            encode = VitalsPeriodDataCodec::encode,
        ) {
            coroutineScope {
        when (metric) {
            VitalsPeriodMetric.ALL -> {
                val current = windows.current
                val bloodPressure = async { loadBloodPressure(current.start, current.end, granted) }
                val spO2 = async { loadSpO2(current.start, current.end, granted) }
                val vo2Max = async { loadVo2Max(current.start, current.end, granted) }
                val respiratoryRate = async { loadRespiratoryRate(current.start, current.end, granted) }
                val bodyTemperature = async { loadBodyTemperature(current.start, current.end, granted) }
                val bloodGlucose = async { loadBloodGlucose(current.start, current.end, granted) }
                val skinTemperature = async { loadSkinTemperature(current.start, current.end, granted) }
                VitalsPeriodData(
                    missingVitalsPermissions = missingPermissions,
                    bloodPressure = bloodPressure.await(),
                    spO2 = spO2.await(),
                    respiratoryRate = respiratoryRate.await(),
                    bodyTemperature = bodyTemperature.await(),
                    vo2Max = vo2Max.await(),
                    bloodGlucose = bloodGlucose.await(),
                    skinTemperature = skinTemperature.await(),
                )
            }
            VitalsPeriodMetric.BLOOD_PRESSURE -> {
                val entries = loadPeriodTriplet(windows) { start, end -> loadBloodPressure(start, end, granted) }
                VitalsPeriodData(
                    missingVitalsPermissions = missingPermissions,
                    bloodPressure = entries.current,
                    previousBloodPressure = entries.previous,
                    baselineBloodPressure = entries.baseline,
                )
            }
            VitalsPeriodMetric.SPO2 -> {
                val entries = loadPeriodTriplet(windows) { start, end -> loadSpO2(start, end, granted) }
                VitalsPeriodData(
                    missingVitalsPermissions = missingPermissions,
                    spO2 = entries.current,
                    previousSpO2 = entries.previous,
                    baselineSpO2 = entries.baseline,
                )
            }
            VitalsPeriodMetric.VO2_MAX -> {
                val entries = loadPeriodTriplet(windows) { start, end -> loadVo2Max(start, end, granted) }
                VitalsPeriodData(
                    missingVitalsPermissions = missingPermissions,
                    vo2Max = entries.current,
                    previousVo2Max = entries.previous,
                    baselineVo2Max = entries.baseline,
                )
            }
            VitalsPeriodMetric.RESPIRATORY_RATE -> {
                val entries = loadPeriodTriplet(windows) { start, end -> loadRespiratoryRate(start, end, granted) }
                VitalsPeriodData(
                    missingVitalsPermissions = missingPermissions,
                    respiratoryRate = entries.current,
                    previousRespiratoryRate = entries.previous,
                    baselineRespiratoryRate = entries.baseline,
                )
            }
            VitalsPeriodMetric.BODY_TEMPERATURE -> {
                val entries = loadPeriodTriplet(windows) { start, end -> loadBodyTemperature(start, end, granted) }
                VitalsPeriodData(
                    missingVitalsPermissions = missingPermissions,
                    bodyTemperature = entries.current,
                    previousBodyTemperature = entries.previous,
                    baselineBodyTemperature = entries.baseline,
                )
            }
            VitalsPeriodMetric.BLOOD_GLUCOSE -> {
                val entries = loadPeriodTriplet(windows) { start, end -> loadBloodGlucose(start, end, granted) }
                VitalsPeriodData(
                    missingVitalsPermissions = missingPermissions,
                    bloodGlucose = entries.current,
                    previousBloodGlucose = entries.previous,
                    baselineBloodGlucose = entries.baseline,
                )
            }
            VitalsPeriodMetric.SKIN_TEMPERATURE -> {
                val entries = loadPeriodTriplet(windows) { start, end -> loadSkinTemperature(start, end, granted) }
                VitalsPeriodData(
                    missingVitalsPermissions = missingPermissions,
                    skinTemperature = entries.current,
                    previousSkinTemperature = entries.previous,
                    baselineSkinTemperature = entries.baseline,
                )
            }
        }
            }
        }
    }

    private fun periodCacheLoader(): CachedPeriodRepositoryLoader =
        CachedPeriodRepositoryLoader(
            cacheStore = metricSummaryCacheStore,
            appScope = appScope,
            tag = TAG,
        )

    private suspend fun <T> loadPeriodTriplet(
        windows: PeriodWindows,
        loader: suspend (LocalDate, LocalDate) -> List<T>,
    ): VitalsPeriodTriplet<T> = coroutineScope {
        val current = async { loader(windows.current.start, windows.current.end) }
        val previous = async { loader(windows.previous.start, windows.previous.end) }
        val baseline = async { loader(windows.baseline.start, windows.baseline.end) }
        VitalsPeriodTriplet(
            current = current.await(),
            previous = previous.await(),
            baseline = baseline.await(),
        )
    }

    suspend fun loadBloodPressure(start: LocalDate, end: LocalDate): List<BloodPressureEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadBloodPressure(start, end, granted)
    }

    private suspend fun loadBloodPressure(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<BloodPressureEntry> {
        if (readBloodPressurePermission !in granted) {
            Log.w(TAG, "Skipping loadBloodPressure missingCount=1")
            return emptyList()
        }
        return hc.readBloodPressureEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadSpO2(start: LocalDate, end: LocalDate): List<SpO2Entry> {
        val granted = grantedPermissionsIfAvailable()
        return loadSpO2(start, end, granted)
    }

    private suspend fun loadSpO2(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<SpO2Entry> {
        if (readSpO2Permission !in granted) {
            Log.w(TAG, "Skipping loadSpO2 missingCount=1")
            return emptyList()
        }
        return hc.readSpO2Entries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadRespiratoryRate(start: LocalDate, end: LocalDate): List<RespiratoryRateEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadRespiratoryRate(start, end, granted)
    }

    private suspend fun loadRespiratoryRate(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<RespiratoryRateEntry> {
        if (readRespiratoryRatePermission !in granted) {
            Log.w(TAG, "Skipping loadRespiratoryRate missingCount=1")
            return emptyList()
        }
        return hc.readRespiratoryRateEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadBodyTemperature(start: LocalDate, end: LocalDate): List<BodyTempEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadBodyTemperature(start, end, granted)
    }

    private suspend fun loadBodyTemperature(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<BodyTempEntry> {
        if (readBodyTemperaturePermission !in granted) {
            Log.w(TAG, "Skipping loadBodyTemperature missingCount=1")
            return emptyList()
        }
        return hc.readBodyTemperatureEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadVo2Max(start: LocalDate, end: LocalDate): List<Vo2MaxEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadVo2Max(start, end, granted)
    }

    private suspend fun loadVo2Max(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<Vo2MaxEntry> {
        if (readVo2MaxPermission !in granted) {
            Log.w(TAG, "Skipping loadVo2Max missingCount=1")
            return emptyList()
        }
        return hc.readVo2MaxEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadBloodGlucose(start: LocalDate, end: LocalDate): List<BloodGlucoseEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadBloodGlucose(start, end, granted)
    }

    private suspend fun loadBloodGlucose(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<BloodGlucoseEntry> {
        if (readBloodGlucosePermission !in granted) {
            Log.w(TAG, "Skipping loadBloodGlucose missingCount=1")
            return emptyList()
        }
        return hc.readBloodGlucoseEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadSkinTemperature(start: LocalDate, end: LocalDate): List<SkinTemperatureEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadSkinTemperature(start, end, granted)
    }

    private suspend fun loadSkinTemperature(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<SkinTemperatureEntry> {
        if (!hc.isSkinTemperatureAvailable() || readSkinTemperaturePermission !in granted) {
            Log.w(TAG, "Skipping loadSkinTemperature missingCount=1")
            return emptyList()
        }
        return hc.readSkinTemperatureEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun hasVitalsWritePermission(type: VitalsMeasurementType): Boolean =
        vitalsWritePermissions(type).all { permission -> permission in grantedPermissionsIfAvailable() }

    suspend fun writeVitalsMeasurementEntry(request: VitalsMeasurementWriteRequest): String {
        val missingPermissions = vitalsWritePermissions(request.type) - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping writeVitalsMeasurementEntry type=${request.type} missingCount=${missingPermissions.size}")
            throw IllegalStateException("Missing Health Connect write permission for ${request.type}")
        }
        return hc.writeVitalsMeasurementEntry(request).also {
            queryCache.invalidateOperations("dashboard")
        }
    }

    suspend fun loadVitalsMeasurementEntry(type: VitalsMeasurementType, id: String): VitalsMeasurementEntry? {
        val readPermission = when (type) {
            VitalsMeasurementType.BLOOD_PRESSURE -> readBloodPressurePermission
            VitalsMeasurementType.SPO2 -> readSpO2Permission
            VitalsMeasurementType.RESPIRATORY_RATE -> readRespiratoryRatePermission
            VitalsMeasurementType.BODY_TEMPERATURE -> readBodyTemperaturePermission
        }
        val granted = grantedPermissionsIfAvailable()
        if (readPermission !in granted) {
            Log.w(TAG, "Skipping loadVitalsMeasurementEntry type=$type missingCount=1")
            return null
        }
        return hc.readVitalsMeasurementEntry(type, id)
    }

    suspend fun updateVitalsMeasurementEntry(id: String, request: VitalsMeasurementWriteRequest) {
        val missingPermissions = vitalsWritePermissions(request.type) - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping updateVitalsMeasurementEntry type=${request.type} missingCount=${missingPermissions.size}")
            throw IllegalStateException("Missing Health Connect write permission for ${request.type}")
        }
        hc.updateVitalsMeasurementEntry(id, request)
        queryCache.invalidateOperations("dashboard")
    }

    suspend fun deleteVitalsMeasurementEntry(type: VitalsMeasurementType, id: String) {
        val missingPermissions = vitalsWritePermissions(type) - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping deleteVitalsMeasurementEntry type=$type missingCount=${missingPermissions.size}")
            throw IllegalStateException("Missing Health Connect write permission for $type")
        }
        hc.deleteVitalsMeasurementEntry(type, id)
        queryCache.invalidateOperations("dashboard")
    }

    private fun LocalDate.toInstant() = atStartOfDay(ZoneId.systemDefault()).toInstant()
}

private data class VitalsPeriodTriplet<T>(
    val current: List<T>,
    val previous: List<T>,
    val baseline: List<T>,
)

enum class VitalsPeriodMetric {
    ALL,
    BLOOD_PRESSURE,
    SPO2,
    VO2_MAX,
    RESPIRATORY_RATE,
    BODY_TEMPERATURE,
    BLOOD_GLUCOSE,
    SKIN_TEMPERATURE,
}
