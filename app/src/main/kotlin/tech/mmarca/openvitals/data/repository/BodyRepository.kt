package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.WeightRecord
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.BodyMeasurementWriteRequest
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodWindows
import tech.mmarca.openvitals.domain.model.BodyFatEntry
import tech.mmarca.openvitals.domain.model.BodyMeasurementEntry
import tech.mmarca.openvitals.domain.model.BodyWaterMassEntry
import tech.mmarca.openvitals.domain.model.BmrEntry
import tech.mmarca.openvitals.domain.model.BoneMassEntry
import tech.mmarca.openvitals.domain.model.HeightEntry
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.LeanBodyMassEntry
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.HealthConnectQueryCache
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class BodyRepository @Inject constructor(
    private val hc: HealthConnectManager,
    private val queryCache: HealthConnectQueryCache = HealthConnectQueryCache(),
) {

    companion object {
        private const val TAG = "BodyRepository"
    }

    private val readWeightPermission = HealthPermission.getReadPermission(WeightRecord::class)
    private val readHeightPermission = HealthPermission.getReadPermission(HeightRecord::class)
    private val readBodyFatPermission = HealthPermission.getReadPermission(BodyFatRecord::class)
    private val readLeanMassPermission = HealthPermission.getReadPermission(LeanBodyMassRecord::class)
    private val readBMRPermission = HealthPermission.getReadPermission(BasalMetabolicRateRecord::class)
    private val readBoneMassPermission = HealthPermission.getReadPermission(BoneMassRecord::class)
    private val readBodyWaterMassPermission = HealthPermission.getReadPermission(BodyWaterMassRecord::class)
    private val writeWeightPermission = HealthPermission.getWritePermission(WeightRecord::class)
    private val writeHeightPermission = HealthPermission.getWritePermission(HeightRecord::class)
    private val writeBodyFatPermission = HealthPermission.getWritePermission(BodyFatRecord::class)

    fun bodyWritePermissions(type: BodyMeasurementType): Set<String> = setOf(
        when (type) {
            BodyMeasurementType.WEIGHT -> writeWeightPermission
            BodyMeasurementType.HEIGHT -> writeHeightPermission
            BodyMeasurementType.BODY_FAT -> writeBodyFatPermission
        }
    )

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun loadBodyPeriod(query: PeriodLoadQuery, metric: BodyPeriodMetric): BodyPeriodData = coroutineScope {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        when (metric) {
            BodyPeriodMetric.ALL -> loadAllBodyPeriod(windows, granted)
            BodyPeriodMetric.WEIGHT -> {
                val entries = loadPeriodTriplet(windows) { start, end -> loadWeightEntries(start, end, granted) }
                BodyPeriodData(
                    weightEntries = entries.current,
                    previousWeightEntries = entries.previous,
                    baselineWeightEntries = entries.baseline,
                )
            }
            BodyPeriodMetric.HEIGHT -> {
                val entries = loadPeriodTriplet(windows) { start, end -> loadHeightEntries(start, end, granted) }
                BodyPeriodData(
                    heightEntries = entries.current,
                    previousHeightEntries = entries.previous,
                    baselineHeightEntries = entries.baseline,
                )
            }
            BodyPeriodMetric.BMI -> {
                val entries = async { loadPeriodTriplet(windows) { start, end -> loadWeightEntries(start, end, granted) } }
                val height = async { loadLatestHeight(granted) }
                val weightEntries = entries.await()
                BodyPeriodData(
                    weightEntries = weightEntries.current,
                    previousWeightEntries = weightEntries.previous,
                    baselineWeightEntries = weightEntries.baseline,
                    heightCm = height.await(),
                )
            }
            BodyPeriodMetric.BODY_FAT -> {
                val entries = loadPeriodTriplet(windows) { start, end -> loadBodyFatEntries(start, end, granted) }
                BodyPeriodData(
                    bodyFatEntries = entries.current,
                    previousBodyFatEntries = entries.previous,
                    baselineBodyFatEntries = entries.baseline,
                )
            }
            BodyPeriodMetric.LEAN_MASS -> {
                val entries = loadPeriodTriplet(windows) { start, end -> loadLeanBodyMassEntries(start, end, granted) }
                BodyPeriodData(
                    leanMassEntries = entries.current,
                    previousLeanMassEntries = entries.previous,
                    baselineLeanMassEntries = entries.baseline,
                )
            }
            BodyPeriodMetric.BMR -> {
                val entries = loadPeriodTriplet(windows) { start, end -> loadBmrEntries(start, end, granted) }
                BodyPeriodData(
                    bmrEntries = entries.current,
                    previousBmrEntries = entries.previous,
                    baselineBmrEntries = entries.baseline,
                )
            }
            BodyPeriodMetric.BONE_MASS -> {
                val entries = loadPeriodTriplet(windows) { start, end -> loadBoneMassEntries(start, end, granted) }
                BodyPeriodData(
                    boneMassEntries = entries.current,
                    previousBoneMassEntries = entries.previous,
                    baselineBoneMassEntries = entries.baseline,
                )
            }
            BodyPeriodMetric.BODY_WATER_MASS -> {
                val entries = loadPeriodTriplet(windows) { start, end -> loadBodyWaterMassEntries(start, end, granted) }
                BodyPeriodData(
                    bodyWaterMassEntries = entries.current,
                    previousBodyWaterMassEntries = entries.previous,
                    baselineBodyWaterMassEntries = entries.baseline,
                )
            }
        }
    }

    private suspend fun loadAllBodyPeriod(
        windows: PeriodWindows,
        granted: Set<String>,
    ): BodyPeriodData = coroutineScope {
        val weight = async { loadPeriodTriplet(windows) { start, end -> loadWeightEntries(start, end, granted) } }
        val height = async { loadPeriodTriplet(windows) { start, end -> loadHeightEntries(start, end, granted) } }
        val latestHeight = async { loadLatestHeight(granted) }
        val bodyFat = async { loadPeriodTriplet(windows) { start, end -> loadBodyFatEntries(start, end, granted) } }
        val leanMass = async { loadPeriodTriplet(windows) { start, end -> loadLeanBodyMassEntries(start, end, granted) } }
        val bmr = async { loadPeriodTriplet(windows) { start, end -> loadBmrEntries(start, end, granted) } }
        val boneMass = async { loadPeriodTriplet(windows) { start, end -> loadBoneMassEntries(start, end, granted) } }
        val bodyWaterMass = async {
            loadPeriodTriplet(windows) { start, end -> loadBodyWaterMassEntries(start, end, granted) }
        }
        val weightEntries = weight.await()
        val heightEntries = height.await()
        val bodyFatEntries = bodyFat.await()
        val leanMassEntries = leanMass.await()
        val bmrEntries = bmr.await()
        val boneMassEntries = boneMass.await()
        val bodyWaterMassEntries = bodyWaterMass.await()
        BodyPeriodData(
            weightEntries = weightEntries.current,
            previousWeightEntries = weightEntries.previous,
            baselineWeightEntries = weightEntries.baseline,
            heightCm = latestHeight.await(),
            heightEntries = heightEntries.current,
            previousHeightEntries = heightEntries.previous,
            baselineHeightEntries = heightEntries.baseline,
            bodyFatEntries = bodyFatEntries.current,
            previousBodyFatEntries = bodyFatEntries.previous,
            baselineBodyFatEntries = bodyFatEntries.baseline,
            leanMassEntries = leanMassEntries.current,
            previousLeanMassEntries = leanMassEntries.previous,
            baselineLeanMassEntries = leanMassEntries.baseline,
            bmrEntries = bmrEntries.current,
            previousBmrEntries = bmrEntries.previous,
            baselineBmrEntries = bmrEntries.baseline,
            boneMassEntries = boneMassEntries.current,
            previousBoneMassEntries = boneMassEntries.previous,
            baselineBoneMassEntries = boneMassEntries.baseline,
            bodyWaterMassEntries = bodyWaterMassEntries.current,
            previousBodyWaterMassEntries = bodyWaterMassEntries.previous,
            baselineBodyWaterMassEntries = bodyWaterMassEntries.baseline,
        )
    }

    private suspend fun <T> loadPeriodTriplet(
        windows: PeriodWindows,
        loader: suspend (LocalDate, LocalDate) -> List<T>,
    ): BodyPeriodTriplet<T> = coroutineScope {
        val current = async { loader(windows.current.start, windows.current.end) }
        val previous = async { loader(windows.previous.start, windows.previous.end) }
        val baseline = async { loader(windows.baseline.start, windows.baseline.end) }
        BodyPeriodTriplet(
            current = current.await(),
            previous = previous.await(),
            baseline = baseline.await(),
        )
    }

    suspend fun loadWeightEntries(start: LocalDate, end: LocalDate): List<WeightEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadWeightEntries(start, end, granted)
    }

    private suspend fun loadWeightEntries(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<WeightEntry> {
        if (readWeightPermission !in granted) {
            Log.w(TAG, "Skipping loadWeightEntries missingCount=1")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val startInstant = start.atStartOfDay(zone).toInstant()
        val endInstant = end.plusDays(1).atStartOfDay(zone).toInstant()
        return hc.readWeightEntries(startInstant, endInstant)
    }

    suspend fun loadLatestHeight(): Double? {
        val granted = grantedPermissionsIfAvailable()
        return loadLatestHeight(granted)
    }

    private suspend fun loadLatestHeight(granted: Set<String>): Double? {
        if (readHeightPermission !in granted) return null
        return hc.readLatestHeight()
    }

    suspend fun loadHeightEntries(start: LocalDate, end: LocalDate): List<HeightEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadHeightEntries(start, end, granted)
    }

    private suspend fun loadHeightEntries(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<HeightEntry> {
        if (readHeightPermission !in granted) {
            Log.w(TAG, "Skipping loadHeightEntries missingCount=1")
            return emptyList()
        }
        return hc.readHeightEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadBodyFatEntries(start: LocalDate, end: LocalDate): List<BodyFatEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadBodyFatEntries(start, end, granted)
    }

    private suspend fun loadBodyFatEntries(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<BodyFatEntry> {
        if (readBodyFatPermission !in granted) {
            Log.w(TAG, "Skipping loadBodyFatEntries missingCount=1")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val startInstant = start.atStartOfDay(zone).toInstant()
        val endInstant = end.plusDays(1).atStartOfDay(zone).toInstant()
        return hc.readBodyFatEntries(startInstant, endInstant)
    }

    suspend fun loadLatestLeanBodyMass(): Double? {
        val granted = grantedPermissionsIfAvailable()
        return loadLatestLeanBodyMass(granted)
    }

    private suspend fun loadLatestLeanBodyMass(granted: Set<String>): Double? {
        if (readLeanMassPermission !in granted) return null
        return hc.readLatestLeanBodyMass()
    }

    suspend fun loadLeanBodyMassEntries(start: LocalDate, end: LocalDate): List<LeanBodyMassEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadLeanBodyMassEntries(start, end, granted)
    }

    private suspend fun loadLeanBodyMassEntries(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<LeanBodyMassEntry> {
        if (readLeanMassPermission !in granted) {
            Log.w(TAG, "Skipping loadLeanBodyMassEntries missingCount=1")
            return emptyList()
        }
        return hc.readLeanBodyMassEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadLatestBMR(): Double? {
        val granted = grantedPermissionsIfAvailable()
        return loadLatestBMR(granted)
    }

    private suspend fun loadLatestBMR(granted: Set<String>): Double? {
        if (readBMRPermission !in granted) return null
        return hc.readLatestBMR()
    }

    suspend fun loadBmrEntries(start: LocalDate, end: LocalDate): List<BmrEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadBmrEntries(start, end, granted)
    }

    private suspend fun loadBmrEntries(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<BmrEntry> {
        if (readBMRPermission !in granted) {
            Log.w(TAG, "Skipping loadBmrEntries missingCount=1")
            return emptyList()
        }
        return hc.readBmrEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadLatestBoneMass(): Double? {
        val granted = grantedPermissionsIfAvailable()
        return loadLatestBoneMass(granted)
    }

    private suspend fun loadLatestBoneMass(granted: Set<String>): Double? {
        if (readBoneMassPermission !in granted) return null
        return hc.readLatestBoneMass()
    }

    suspend fun loadBoneMassEntries(start: LocalDate, end: LocalDate): List<BoneMassEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadBoneMassEntries(start, end, granted)
    }

    private suspend fun loadBoneMassEntries(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<BoneMassEntry> {
        if (readBoneMassPermission !in granted) {
            Log.w(TAG, "Skipping loadBoneMassEntries missingCount=1")
            return emptyList()
        }
        return hc.readBoneMassEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadLatestBodyWaterMass(): Double? {
        val granted = grantedPermissionsIfAvailable()
        return loadLatestBodyWaterMass(granted)
    }

    private suspend fun loadLatestBodyWaterMass(granted: Set<String>): Double? {
        if (readBodyWaterMassPermission !in granted) return null
        return hc.readLatestBodyWaterMass()
    }

    suspend fun loadBodyWaterMassEntries(start: LocalDate, end: LocalDate): List<BodyWaterMassEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadBodyWaterMassEntries(start, end, granted)
    }

    private suspend fun loadBodyWaterMassEntries(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<BodyWaterMassEntry> {
        if (readBodyWaterMassPermission !in granted) {
            Log.w(TAG, "Skipping loadBodyWaterMassEntries missingCount=1")
            return emptyList()
        }
        return hc.readBodyWaterMassEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun hasBodyWritePermission(type: BodyMeasurementType): Boolean =
        bodyWritePermissions(type).all { permission -> permission in grantedPermissionsIfAvailable() }

    suspend fun writeBodyMeasurementEntry(request: BodyMeasurementWriteRequest): String {
        val missingPermissions = bodyWritePermissions(request.type) - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping writeBodyMeasurementEntry type=${request.type} missingCount=${missingPermissions.size}")
            throw SecurityException("Missing Health Connect body write permission.")
        }
        return hc.writeBodyMeasurementEntry(request).also {
            queryCache.invalidateOperations("dashboard")
        }
    }

    suspend fun loadBodyMeasurementEntry(type: BodyMeasurementType, id: String): BodyMeasurementEntry? {
        val readPermission = when (type) {
            BodyMeasurementType.WEIGHT -> readWeightPermission
            BodyMeasurementType.HEIGHT -> readHeightPermission
            BodyMeasurementType.BODY_FAT -> readBodyFatPermission
        }
        val granted = grantedPermissionsIfAvailable()
        if (readPermission !in granted) {
            Log.w(TAG, "Skipping loadBodyMeasurementEntry type=$type missingCount=1")
            return null
        }
        return hc.readBodyMeasurementEntry(type, id)
    }

    suspend fun updateBodyMeasurementEntry(id: String, request: BodyMeasurementWriteRequest) {
        val missingPermissions = bodyWritePermissions(request.type) - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping updateBodyMeasurementEntry type=${request.type} missingCount=${missingPermissions.size}")
            throw SecurityException("Missing Health Connect body write permission.")
        }
        hc.updateBodyMeasurementEntry(id, request)
        queryCache.invalidateOperations("dashboard")
    }

    suspend fun deleteBodyMeasurementEntry(type: BodyMeasurementType, id: String) {
        val missingPermissions = bodyWritePermissions(type) - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping deleteBodyMeasurementEntry type=$type missingCount=${missingPermissions.size}")
            throw SecurityException("Missing Health Connect body write permission.")
        }
        hc.deleteBodyMeasurementEntry(type, id)
        queryCache.invalidateOperations("dashboard")
    }

    private fun LocalDate.toInstant() = atStartOfDay(ZoneId.systemDefault()).toInstant()
}

private data class BodyPeriodTriplet<T>(
    val current: List<T>,
    val previous: List<T>,
    val baseline: List<T>,
)

enum class BodyPeriodMetric {
    ALL,
    WEIGHT,
    HEIGHT,
    BMI,
    BODY_FAT,
    LEAN_MASS,
    BMR,
    BONE_MASS,
    BODY_WATER_MASS,
}

data class BodyPeriodData(
    val weightEntries: List<WeightEntry> = emptyList(),
    val previousWeightEntries: List<WeightEntry> = emptyList(),
    val baselineWeightEntries: List<WeightEntry> = emptyList(),
    val heightCm: Double? = null,
    val heightEntries: List<HeightEntry> = emptyList(),
    val previousHeightEntries: List<HeightEntry> = emptyList(),
    val baselineHeightEntries: List<HeightEntry> = emptyList(),
    val bodyFatEntries: List<BodyFatEntry> = emptyList(),
    val previousBodyFatEntries: List<BodyFatEntry> = emptyList(),
    val baselineBodyFatEntries: List<BodyFatEntry> = emptyList(),
    val leanMassKg: Double? = null,
    val leanMassEntries: List<LeanBodyMassEntry> = emptyList(),
    val previousLeanMassEntries: List<LeanBodyMassEntry> = emptyList(),
    val baselineLeanMassEntries: List<LeanBodyMassEntry> = emptyList(),
    val bmrKcal: Double? = null,
    val bmrEntries: List<BmrEntry> = emptyList(),
    val previousBmrEntries: List<BmrEntry> = emptyList(),
    val baselineBmrEntries: List<BmrEntry> = emptyList(),
    val boneMassKg: Double? = null,
    val boneMassEntries: List<BoneMassEntry> = emptyList(),
    val previousBoneMassEntries: List<BoneMassEntry> = emptyList(),
    val baselineBoneMassEntries: List<BoneMassEntry> = emptyList(),
    val bodyWaterMassKg: Double? = null,
    val bodyWaterMassEntries: List<BodyWaterMassEntry> = emptyList(),
    val previousBodyWaterMassEntries: List<BodyWaterMassEntry> = emptyList(),
    val baselineBodyWaterMassEntries: List<BodyWaterMassEntry> = emptyList(),
)
