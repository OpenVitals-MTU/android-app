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
import tech.mmarca.openvitals.domain.cycle.CycleCalculations
import tech.mmarca.openvitals.domain.cycle.CycleStatistics
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.CycleEntry
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleEntryWriteRequest
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
            val statistics = async { loadCycleStatistics() }
            CyclePeriodData(
                data = data.await(),
                missingPermissions = missing.await(),
                statistics = statistics.await(),
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

    override fun cycleWritePermissions(kind: CycleEntryKind): Set<String> = setOf(
        when (kind) {
            CycleEntryKind.MENSTRUATION_FLOW ->
                HealthPermission.getWritePermission(MenstruationFlowRecord::class)
            CycleEntryKind.SPOTTING ->
                HealthPermission.getWritePermission(IntermenstrualBleedingRecord::class)
            CycleEntryKind.SEXUAL_ACTIVITY ->
                HealthPermission.getWritePermission(SexualActivityRecord::class)
            CycleEntryKind.OVULATION_TEST ->
                HealthPermission.getWritePermission(OvulationTestRecord::class)
            CycleEntryKind.CERVICAL_MUCUS ->
                HealthPermission.getWritePermission(CervicalMucusRecord::class)
            CycleEntryKind.BASAL_BODY_TEMPERATURE ->
                HealthPermission.getWritePermission(BasalBodyTemperatureRecord::class)
        }
    )

    override suspend fun hasCycleWritePermission(kind: CycleEntryKind): Boolean =
        cycleWritePermissions(kind).all { it in grantedPermissionsIfAvailable() }

    override suspend fun writeCycleEntry(request: CycleEntryWriteRequest): String {
        requireWritePermission(request.kind, "writeCycleEntry")
        val id = hc.writeCycleEntry(request)
        reconcileAfterFlowMutation(request.kind, setOf(request.time.toLocalDay()))
        return id
    }

    override suspend fun loadCycleEntry(kind: CycleEntryKind, id: String): CycleEntry? =
        hc.readCycleEntry(kind, id)

    override suspend fun updateCycleEntry(id: String, request: CycleEntryWriteRequest) {
        requireWritePermission(request.kind, "updateCycleEntry")
        val previousDay = previousDayOf(request.kind, id)
        hc.updateCycleEntry(id, request)
        reconcileAfterFlowMutation(request.kind, setOfNotNull(previousDay, request.time.toLocalDay()))
    }

    override suspend fun deleteCycleEntry(kind: CycleEntryKind, id: String) {
        requireWritePermission(kind, "deleteCycleEntry")
        val previousDay = previousDayOf(kind, id)
        hc.deleteCycleEntry(kind, id)
        reconcileAfterFlowMutation(kind, setOfNotNull(previousDay))
    }

    override suspend fun loadCycleStatistics(today: LocalDate): CycleStatistics? {
        val granted = grantedPermissionsIfAvailable()
        if (readMenstruationPermission !in granted) {
            Log.w(TAG, "Skipping loadCycleStatistics missingCount=1")
            return null
        }
        val zone = ZoneId.systemDefault()
        val startInstant = today.minusMonths(STATISTICS_LOOKBACK_MONTHS).atStartOfDay(zone).toInstant()
        val endInstant = today.plusDays(1).atStartOfDay(zone).toInstant()

        return coroutineScope {
            val flows = async { hc.readMenstruationFlowEntries(startInstant, endInstant) }
            val periods = async { hc.readMenstruationPeriods(startInstant, endInstant) }

            val bleedingDays = buildSet {
                flows.await().forEach { add(it.time.toLocalDay()) }
                periods.await().forEach { period ->
                    var day = period.startTime.toLocalDay()
                    val last = period.endTime.minusMillis(1).toLocalDay()
                    while (!day.isAfter(last)) {
                        add(day)
                        day = day.plusDays(1)
                    }
                }
            }
            CycleCalculations.compute(bleedingDays, today)
        }
    }

    private suspend fun requireWritePermission(kind: CycleEntryKind, operation: String) {
        val missingPermissions = cycleWritePermissions(kind) - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping $operation kind=$kind missingCount=${missingPermissions.size}")
            throw SecurityException("Missing Health Connect cycle write permission.")
        }
    }

    private suspend fun previousDayOf(kind: CycleEntryKind, id: String): LocalDate? {
        if (kind != CycleEntryKind.MENSTRUATION_FLOW) return null
        return hc.readCycleEntry(kind, id)?.time?.toLocalDay()
    }

    // Derived MenstruationPeriodRecords track the flow days; a failure here is
    // deferrable (the write already landed, the next flow mutation reconciles).
    private suspend fun reconcileAfterFlowMutation(kind: CycleEntryKind, days: Set<LocalDate>) {
        if (kind != CycleEntryKind.MENSTRUATION_FLOW || days.isEmpty()) return
        runCatching { hc.reconcileMenstruationPeriods(days) }
            .onFailure { Log.w(TAG, "Deferred period reconcile for $days", it) }
    }

    private fun java.time.Instant.toLocalDay(): LocalDate = atZone(ZoneId.systemDefault()).toLocalDate()
}

private const val STATISTICS_LOOKBACK_MONTHS = 12L
