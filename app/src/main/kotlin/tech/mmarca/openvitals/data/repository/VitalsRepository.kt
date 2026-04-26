package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import tech.mmarca.openvitals.data.model.BloodPressureEntry
import tech.mmarca.openvitals.data.model.BodyTempEntry
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.RespiratoryRateEntry
import tech.mmarca.openvitals.data.model.SpO2Entry
import tech.mmarca.openvitals.data.model.Vo2MaxEntry
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId

class VitalsRepository(private val hc: HealthConnectManager) {

    companion object {
        private const val TAG = "VitalsRepository"
    }

    val phase3Permissions: Set<String> get() = hc.phase3Permissions

    private val readBloodPressurePermission = HealthPermission.getReadPermission(BloodPressureRecord::class)
    private val readSpO2Permission = HealthPermission.getReadPermission(OxygenSaturationRecord::class)
    private val readRespiratoryRatePermission = HealthPermission.getReadPermission(RespiratoryRateRecord::class)
    private val readBodyTemperaturePermission = HealthPermission.getReadPermission(BodyTemperatureRecord::class)
    private val readVo2MaxPermission = HealthPermission.getReadPermission(Vo2MaxRecord::class)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun missingPermissions(): Set<String> {
        val granted = grantedPermissionsIfAvailable()
        return phase3Permissions.filterNot { it in granted }.toSet()
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

    private fun LocalDate.toInstant() = atStartOfDay(ZoneId.systemDefault()).toInstant()
}
