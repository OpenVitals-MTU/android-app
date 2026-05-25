package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.WeightRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.data.model.BodyFatEntry
import tech.mmarca.openvitals.data.model.BmrEntry
import tech.mmarca.openvitals.data.model.BoneMassEntry
import tech.mmarca.openvitals.data.model.HeightEntry
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.LeanBodyMassEntry
import tech.mmarca.openvitals.data.model.WeightEntry
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyRepository @Inject constructor(
    private val hc: HealthConnectManager,
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

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun loadBodyPeriod(query: PeriodLoadQuery, metric: BodyPeriodMetric): BodyPeriodData {
        val windows = query.windows
        return when (metric) {
            BodyPeriodMetric.WEIGHT -> BodyPeriodData(
                weightEntries = loadWeightEntries(windows.current.start, windows.current.end),
                previousWeightEntries = loadWeightEntries(windows.previous.start, windows.previous.end),
                baselineWeightEntries = loadWeightEntries(windows.baseline.start, windows.baseline.end),
            )
            BodyPeriodMetric.HEIGHT -> BodyPeriodData(
                heightEntries = loadHeightEntries(windows.current.start, windows.current.end),
                previousHeightEntries = loadHeightEntries(windows.previous.start, windows.previous.end),
                baselineHeightEntries = loadHeightEntries(windows.baseline.start, windows.baseline.end),
            )
            BodyPeriodMetric.BMI -> BodyPeriodData(
                weightEntries = loadWeightEntries(windows.current.start, windows.current.end),
                previousWeightEntries = loadWeightEntries(windows.previous.start, windows.previous.end),
                baselineWeightEntries = loadWeightEntries(windows.baseline.start, windows.baseline.end),
                heightCm = loadLatestHeight(),
            )
            BodyPeriodMetric.BODY_FAT -> BodyPeriodData(
                bodyFatEntries = loadBodyFatEntries(windows.current.start, windows.current.end),
                previousBodyFatEntries = loadBodyFatEntries(windows.previous.start, windows.previous.end),
                baselineBodyFatEntries = loadBodyFatEntries(windows.baseline.start, windows.baseline.end),
            )
            BodyPeriodMetric.LEAN_MASS -> BodyPeriodData(
                leanMassEntries = loadLeanBodyMassEntries(windows.current.start, windows.current.end),
                previousLeanMassEntries = loadLeanBodyMassEntries(windows.previous.start, windows.previous.end),
                baselineLeanMassEntries = loadLeanBodyMassEntries(windows.baseline.start, windows.baseline.end),
            )
            BodyPeriodMetric.BMR -> BodyPeriodData(
                bmrEntries = loadBmrEntries(windows.current.start, windows.current.end),
                previousBmrEntries = loadBmrEntries(windows.previous.start, windows.previous.end),
                baselineBmrEntries = loadBmrEntries(windows.baseline.start, windows.baseline.end),
            )
            BodyPeriodMetric.BONE_MASS -> BodyPeriodData(
                boneMassEntries = loadBoneMassEntries(windows.current.start, windows.current.end),
                previousBoneMassEntries = loadBoneMassEntries(windows.previous.start, windows.previous.end),
                baselineBoneMassEntries = loadBoneMassEntries(windows.baseline.start, windows.baseline.end),
            )
        }
    }

    suspend fun loadWeightEntries(start: LocalDate, end: LocalDate): List<WeightEntry> {
        val granted = grantedPermissionsIfAvailable()
        if (readWeightPermission !in granted) {
            Log.w(TAG, "Skipping loadWeightEntries missing=$readWeightPermission")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val startInstant = start.atStartOfDay(zone).toInstant()
        val endInstant = end.plusDays(1).atStartOfDay(zone).toInstant()
        return hc.readWeightEntries(startInstant, endInstant)
    }

    suspend fun loadLatestHeight(): Double? {
        val granted = grantedPermissionsIfAvailable()
        if (readHeightPermission !in granted) return null
        return hc.readLatestHeight()
    }

    suspend fun loadHeightEntries(start: LocalDate, end: LocalDate): List<HeightEntry> {
        val granted = grantedPermissionsIfAvailable()
        if (readHeightPermission !in granted) {
            Log.w(TAG, "Skipping loadHeightEntries missing=$readHeightPermission")
            return emptyList()
        }
        return hc.readHeightEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadBodyFatEntries(start: LocalDate, end: LocalDate): List<BodyFatEntry> {
        val granted = grantedPermissionsIfAvailable()
        if (readBodyFatPermission !in granted) {
            Log.w(TAG, "Skipping loadBodyFatEntries missing=$readBodyFatPermission")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val startInstant = start.atStartOfDay(zone).toInstant()
        val endInstant = end.plusDays(1).atStartOfDay(zone).toInstant()
        return hc.readBodyFatEntries(startInstant, endInstant)
    }

    suspend fun loadLatestLeanBodyMass(): Double? {
        val granted = grantedPermissionsIfAvailable()
        if (readLeanMassPermission !in granted) return null
        return hc.readLatestLeanBodyMass()
    }

    suspend fun loadLeanBodyMassEntries(start: LocalDate, end: LocalDate): List<LeanBodyMassEntry> {
        val granted = grantedPermissionsIfAvailable()
        if (readLeanMassPermission !in granted) {
            Log.w(TAG, "Skipping loadLeanBodyMassEntries missing=$readLeanMassPermission")
            return emptyList()
        }
        return hc.readLeanBodyMassEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadLatestBMR(): Double? {
        val granted = grantedPermissionsIfAvailable()
        if (readBMRPermission !in granted) return null
        return hc.readLatestBMR()
    }

    suspend fun loadBmrEntries(start: LocalDate, end: LocalDate): List<BmrEntry> {
        val granted = grantedPermissionsIfAvailable()
        if (readBMRPermission !in granted) {
            Log.w(TAG, "Skipping loadBmrEntries missing=$readBMRPermission")
            return emptyList()
        }
        return hc.readBmrEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadLatestBoneMass(): Double? {
        val granted = grantedPermissionsIfAvailable()
        if (readBoneMassPermission !in granted) return null
        return hc.readLatestBoneMass()
    }

    suspend fun loadBoneMassEntries(start: LocalDate, end: LocalDate): List<BoneMassEntry> {
        val granted = grantedPermissionsIfAvailable()
        if (readBoneMassPermission !in granted) {
            Log.w(TAG, "Skipping loadBoneMassEntries missing=$readBoneMassPermission")
            return emptyList()
        }
        return hc.readBoneMassEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    private fun LocalDate.toInstant() = atStartOfDay(ZoneId.systemDefault()).toInstant()
}

enum class BodyPeriodMetric {
    WEIGHT,
    HEIGHT,
    BMI,
    BODY_FAT,
    LEAN_MASS,
    BMR,
    BONE_MASS,
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
)
