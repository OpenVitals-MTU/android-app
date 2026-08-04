package tech.mmarca.openvitals.data.repository
import tech.mmarca.openvitals.data.repository.contract.CycleRepository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.SexualActivityRecord
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.query.CyclePeriodData
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CycleRepositoryImpl @Inject constructor(
    private val hc: HealthConnectManager,
) : CycleRepository {

    companion object {
        private const val TAG = "CycleRepository"
    }

    override val phase4Permissions: Set<String> get() = hc.phase4Permissions

    private val readMenstruationPermission = HealthPermission.getReadPermission(MenstruationFlowRecord::class)
    private val readOvulationTestPermission = HealthPermission.getReadPermission(OvulationTestRecord::class)
    private val readCervicalMucusPermission = HealthPermission.getReadPermission(CervicalMucusRecord::class)
    private val readBasalBodyTemperaturePermission =
        HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class)
    private val readIntermenstrualBleedingPermission =
        HealthPermission.getReadPermission(IntermenstrualBleedingRecord::class)
    private val readSexualActivityPermission = HealthPermission.getReadPermission(SexualActivityRecord::class)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    override suspend fun missingPermissions(): Set<String> {
        val granted = grantedPermissionsIfAvailable()
        return phase4Permissions.filterNot { it in granted }.toSet()
    }

    @Suppress("UNUSED_PARAMETER")
    override suspend fun loadCyclePeriod(
        query: PeriodLoadQuery,
        refreshMode: RefreshMode,
    ): CyclePeriodData {
        return coroutineScope {
            val data = async { loadCycleData(query.windows.current.start, query.windows.current.end) }
            val missing = async { missingPermissions() }
            CyclePeriodData(
                data = data.await(),
                missingPermissions = missing.await(),
            )
        }
    }

    override suspend fun loadCycleData(start: LocalDate, end: LocalDate): CycleData {
        val granted = grantedPermissionsIfAvailable()
        val zone = ZoneId.systemDefault()
        val startInstant = start.atStartOfDay(zone).toInstant()
        val endInstant = end.plusDays(1).atStartOfDay(zone).toInstant()

        return coroutineScope {
            val flows = if (readMenstruationPermission in granted) {
                async { hc.readMenstruationFlowEntries(startInstant, endInstant) }
            } else {
                Log.w(TAG, "Skipping menstruation flow missingCount=1")
                null
            }
            val periods = if (readMenstruationPermission in granted) {
                async { hc.readMenstruationPeriods(startInstant, endInstant) }
            } else {
                null
            }
            val ovulationTests = if (readOvulationTestPermission in granted) {
                async { hc.readOvulationTests(startInstant, endInstant) }
            } else {
                Log.w(TAG, "Skipping ovulation tests missingCount=1")
                null
            }
            val cervicalMucus = if (readCervicalMucusPermission in granted) {
                async { hc.readCervicalMucusEntries(startInstant, endInstant) }
            } else {
                Log.w(TAG, "Skipping cervical mucus missingCount=1")
                null
            }
            val basalBodyTemperature = if (readBasalBodyTemperaturePermission in granted) {
                async { hc.readBasalBodyTemperatureEntries(startInstant, endInstant) }
            } else {
                Log.w(TAG, "Skipping basal body temperature missingCount=1")
                null
            }
            val intermenstrualBleeding = if (readIntermenstrualBleedingPermission in granted) {
                async { hc.readIntermenstrualBleedingEntries(startInstant, endInstant) }
            } else {
                Log.w(TAG, "Skipping intermenstrual bleeding missingCount=1")
                null
            }
            val sexualActivity = if (readSexualActivityPermission in granted) {
                async { hc.readSexualActivityEntries(startInstant, endInstant) }
            } else {
                Log.w(TAG, "Skipping sexual activity missingCount=1")
                null
            }

            CycleData(
                menstruationFlows = flows?.await().orEmpty(),
                menstruationPeriods = periods?.await().orEmpty(),
                ovulationTests = ovulationTests?.await().orEmpty(),
                cervicalMucus = cervicalMucus?.await().orEmpty(),
                basalBodyTemperature = basalBodyTemperature?.await().orEmpty(),
                intermenstrualBleeding = intermenstrualBleeding?.await().orEmpty(),
                sexualActivity = sexualActivity?.await().orEmpty(),
            )
        }
    }
}
