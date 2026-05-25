package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.data.model.BloodPressureEntry
import tech.mmarca.openvitals.data.model.BodyTempEntry
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.RespiratoryRateEntry
import tech.mmarca.openvitals.data.model.SpO2Entry
import tech.mmarca.openvitals.data.model.VitalsMeasurementType
import tech.mmarca.openvitals.data.model.VitalsMeasurementWriteRequest
import tech.mmarca.openvitals.data.model.Vo2MaxEntry
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VitalsRepository @Inject constructor(
    private val hc: HealthConnectManager,
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

    suspend fun loadVitalsPeriod(query: PeriodLoadQuery, metric: VitalsPeriodMetric): VitalsPeriodData {
        val windows = query.windows
        return when (metric) {
            VitalsPeriodMetric.BLOOD_PRESSURE -> VitalsPeriodData(
                missingVitalsPermissions = missingPermissions(),
                bloodPressure = loadBloodPressure(windows.current.start, windows.current.end),
                previousBloodPressure = loadBloodPressure(windows.previous.start, windows.previous.end),
                baselineBloodPressure = loadBloodPressure(windows.baseline.start, windows.baseline.end),
            )
            VitalsPeriodMetric.SPO2 -> VitalsPeriodData(
                missingVitalsPermissions = missingPermissions(),
                spO2 = loadSpO2(windows.current.start, windows.current.end),
                previousSpO2 = loadSpO2(windows.previous.start, windows.previous.end),
                baselineSpO2 = loadSpO2(windows.baseline.start, windows.baseline.end),
            )
            VitalsPeriodMetric.VO2_MAX -> VitalsPeriodData(
                missingVitalsPermissions = missingPermissions(),
                vo2Max = loadVo2Max(windows.current.start, windows.current.end),
                previousVo2Max = loadVo2Max(windows.previous.start, windows.previous.end),
                baselineVo2Max = loadVo2Max(windows.baseline.start, windows.baseline.end),
            )
            VitalsPeriodMetric.RESPIRATORY_RATE -> VitalsPeriodData(
                missingVitalsPermissions = missingPermissions(),
                respiratoryRate = loadRespiratoryRate(windows.current.start, windows.current.end),
                previousRespiratoryRate = loadRespiratoryRate(windows.previous.start, windows.previous.end),
                baselineRespiratoryRate = loadRespiratoryRate(windows.baseline.start, windows.baseline.end),
            )
            VitalsPeriodMetric.BODY_TEMPERATURE -> VitalsPeriodData(
                missingVitalsPermissions = missingPermissions(),
                bodyTemperature = loadBodyTemperature(windows.current.start, windows.current.end),
                previousBodyTemperature = loadBodyTemperature(windows.previous.start, windows.previous.end),
                baselineBodyTemperature = loadBodyTemperature(windows.baseline.start, windows.baseline.end),
            )
        }
    }

    suspend fun loadBloodPressure(start: LocalDate, end: LocalDate): List<BloodPressureEntry> {
        val granted = grantedPermissionsIfAvailable()
        if (readBloodPressurePermission !in granted) {
            Log.w(TAG, "Skipping loadBloodPressure start=$start end=$end missing=$readBloodPressurePermission")
            return emptyList()
        }
        return hc.readBloodPressureEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadSpO2(start: LocalDate, end: LocalDate): List<SpO2Entry> {
        val granted = grantedPermissionsIfAvailable()
        if (readSpO2Permission !in granted) {
            Log.w(TAG, "Skipping loadSpO2 start=$start end=$end missing=$readSpO2Permission")
            return emptyList()
        }
        return hc.readSpO2Entries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadRespiratoryRate(start: LocalDate, end: LocalDate): List<RespiratoryRateEntry> {
        val granted = grantedPermissionsIfAvailable()
        if (readRespiratoryRatePermission !in granted) {
            Log.w(TAG, "Skipping loadRespiratoryRate start=$start end=$end missing=$readRespiratoryRatePermission")
            return emptyList()
        }
        return hc.readRespiratoryRateEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadBodyTemperature(start: LocalDate, end: LocalDate): List<BodyTempEntry> {
        val granted = grantedPermissionsIfAvailable()
        if (readBodyTemperaturePermission !in granted) {
            Log.w(TAG, "Skipping loadBodyTemperature start=$start end=$end missing=$readBodyTemperaturePermission")
            return emptyList()
        }
        return hc.readBodyTemperatureEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun loadVo2Max(start: LocalDate, end: LocalDate): List<Vo2MaxEntry> {
        val granted = grantedPermissionsIfAvailable()
        if (readVo2MaxPermission !in granted) {
            Log.w(TAG, "Skipping loadVo2Max start=$start end=$end missing=$readVo2MaxPermission")
            return emptyList()
        }
        return hc.readVo2MaxEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    suspend fun hasVitalsWritePermission(type: VitalsMeasurementType): Boolean =
        vitalsWritePermissions(type).all { permission -> permission in grantedPermissionsIfAvailable() }

    suspend fun writeVitalsMeasurementEntry(request: VitalsMeasurementWriteRequest): String {
        val missingPermissions = vitalsWritePermissions(request.type) - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping writeVitalsMeasurementEntry type=${request.type} missing=$missingPermissions")
            throw IllegalStateException("Missing Health Connect write permission for ${request.type}")
        }
        return hc.writeVitalsMeasurementEntry(request)
    }

    private fun LocalDate.toInstant() = atStartOfDay(ZoneId.systemDefault()).toInstant()
}

enum class VitalsPeriodMetric {
    BLOOD_PRESSURE,
    SPO2,
    VO2_MAX,
    RESPIRATORY_RATE,
    BODY_TEMPERATURE,
}

data class VitalsPeriodData(
    val missingVitalsPermissions: Set<String> = emptySet(),
    val bloodPressure: List<BloodPressureEntry> = emptyList(),
    val previousBloodPressure: List<BloodPressureEntry> = emptyList(),
    val baselineBloodPressure: List<BloodPressureEntry> = emptyList(),
    val spO2: List<SpO2Entry> = emptyList(),
    val previousSpO2: List<SpO2Entry> = emptyList(),
    val baselineSpO2: List<SpO2Entry> = emptyList(),
    val respiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val previousRespiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val baselineRespiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val bodyTemperature: List<BodyTempEntry> = emptyList(),
    val previousBodyTemperature: List<BodyTempEntry> = emptyList(),
    val baselineBodyTemperature: List<BodyTempEntry> = emptyList(),
    val vo2Max: List<Vo2MaxEntry> = emptyList(),
    val previousVo2Max: List<Vo2MaxEntry> = emptyList(),
    val baselineVo2Max: List<Vo2MaxEntry> = emptyList(),
)
