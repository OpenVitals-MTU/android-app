package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.OvulationTestRecord
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.data.model.CycleData
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CycleRepository @Inject constructor(
    private val hc: HealthConnectManager,
) {

    companion object {
        private const val TAG = "CycleRepository"
    }

    val phase4Permissions: Set<String> get() = hc.phase4Permissions

    private val readMenstruationPermission = HealthPermission.getReadPermission(MenstruationFlowRecord::class)
    private val readOvulationTestPermission = HealthPermission.getReadPermission(OvulationTestRecord::class)
    private val readCervicalMucusPermission = HealthPermission.getReadPermission(CervicalMucusRecord::class)
    private val readBasalBodyTemperaturePermission =
        HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun missingPermissions(): Set<String> {
        val granted = grantedPermissionsIfAvailable()
        return phase4Permissions.filterNot { it in granted }.toSet()
    }

    suspend fun loadCyclePeriod(query: PeriodLoadQuery): CyclePeriodData =
        coroutineScope {
            val data = async { loadCycleData(query.windows.current.start, query.windows.current.end) }
            val missingPermissions = async { missingPermissions() }
            CyclePeriodData(
                data = data.await(),
                missingPermissions = missingPermissions.await(),
            )
        }

    suspend fun loadCycleData(start: LocalDate, end: LocalDate): CycleData {
        val granted = grantedPermissionsIfAvailable()
        val zone = ZoneId.systemDefault()
        val startInstant = start.atStartOfDay(zone).toInstant()
        val endInstant = end.plusDays(1).atStartOfDay(zone).toInstant()

        return coroutineScope {
            val flows = if (readMenstruationPermission in granted) {
                async { hc.readMenstruationFlowEntries(startInstant, endInstant) }
            } else {
                Log.w(TAG, "Skipping menstruation flow start=$start end=$end missing=$readMenstruationPermission")
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
                Log.w(TAG, "Skipping ovulation tests start=$start end=$end missing=$readOvulationTestPermission")
                null
            }
            val cervicalMucus = if (readCervicalMucusPermission in granted) {
                async { hc.readCervicalMucusEntries(startInstant, endInstant) }
            } else {
                Log.w(TAG, "Skipping cervical mucus start=$start end=$end missing=$readCervicalMucusPermission")
                null
            }
            val basalBodyTemperature = if (readBasalBodyTemperaturePermission in granted) {
                async { hc.readBasalBodyTemperatureEntries(startInstant, endInstant) }
            } else {
                Log.w(
                    TAG,
                    "Skipping basal body temperature start=$start end=$end missing=$readBasalBodyTemperaturePermission",
                )
                null
            }

            CycleData(
                menstruationFlows = flows?.await().orEmpty(),
                menstruationPeriods = periods?.await().orEmpty(),
                ovulationTests = ovulationTests?.await().orEmpty(),
                cervicalMucus = cervicalMucus?.await().orEmpty(),
                basalBodyTemperature = basalBodyTemperature?.await().orEmpty(),
            )
        }
    }
}

data class CyclePeriodData(
    val data: CycleData,
    val missingPermissions: Set<String>,
)
