package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.WeightRecord
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

class BodyRepository(private val hc: HealthConnectManager) {

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
