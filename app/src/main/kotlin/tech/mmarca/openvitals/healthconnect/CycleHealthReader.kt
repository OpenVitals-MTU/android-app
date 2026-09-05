package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.celsius
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.domain.cycle.CycleCalculations
import tech.mmarca.openvitals.domain.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.domain.model.CervicalMucusEntry
import tech.mmarca.openvitals.domain.model.CycleEntry
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleEntryWriteRequest
import tech.mmarca.openvitals.domain.model.CycleRecordValues
import tech.mmarca.openvitals.domain.model.IntermenstrualBleedingEntry
import tech.mmarca.openvitals.domain.model.MenstruationFlowEntry
import tech.mmarca.openvitals.domain.model.MenstruationPeriodEntry
import tech.mmarca.openvitals.domain.model.OvulationTestEntry
import tech.mmarca.openvitals.domain.model.SexualActivityEntry
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.reflect.KClass

internal class CycleHealthReader(
    private val support: HealthConnectReaderSupport,
    private val appPackageName: String,
) {
    suspend fun readMenstruationFlowEntries(start: Instant, end: Instant): List<MenstruationFlowEntry> =
        support.withLogging("readMenstruationFlowEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = MenstruationFlowRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = 200,
            ).map { record ->
                MenstruationFlowEntry(
                    time = record.time,
                    flow = record.flow,
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = record.isOpenVitalsEntry(),
                )
            }
        }

    suspend fun readMenstruationPeriods(start: Instant, end: Instant): List<MenstruationPeriodEntry> =
        support.withLogging("readMenstruationPeriods[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = MenstruationPeriodRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = 100,
            ).map { record ->
                MenstruationPeriodEntry(
                    startTime = record.startTime,
                    endTime = record.endTime,
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = record.isOpenVitalsEntry(),
                )
            }
        }

    suspend fun readOvulationTests(start: Instant, end: Instant): List<OvulationTestEntry> =
        support.withLogging("readOvulationTests[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = OvulationTestRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = 200,
            ).map { record ->
                OvulationTestEntry(
                    time = record.time,
                    result = record.result,
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = record.isOpenVitalsEntry(),
                )
            }
        }

    suspend fun readCervicalMucusEntries(start: Instant, end: Instant): List<CervicalMucusEntry> =
        support.withLogging("readCervicalMucusEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = CervicalMucusRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = 200,
            ).map { record ->
                CervicalMucusEntry(
                    time = record.time,
                    appearance = record.appearance,
                    sensation = record.sensation,
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = record.isOpenVitalsEntry(),
                )
            }
        }

    suspend fun readBasalBodyTemperatureEntries(start: Instant, end: Instant): List<BasalBodyTemperatureEntry> =
        support.withLogging("readBasalBodyTemperatureEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = BasalBodyTemperatureRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = 200,
            ).map { record ->
                BasalBodyTemperatureEntry(
                    time = record.time,
                    temperatureCelsius = record.temperature.inCelsius,
                    measurementLocation = record.measurementLocation,
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = record.isOpenVitalsEntry(),
                )
            }
        }

    suspend fun readIntermenstrualBleedingEntries(start: Instant, end: Instant): List<IntermenstrualBleedingEntry> =
        support.withLogging("readIntermenstrualBleedingEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = IntermenstrualBleedingRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = 200,
            ).map { record ->
                IntermenstrualBleedingEntry(
                    time = record.time,
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = record.isOpenVitalsEntry(),
                )
            }
        }

    suspend fun readSexualActivityEntries(start: Instant, end: Instant): List<SexualActivityEntry> =
        support.withLogging("readSexualActivityEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = SexualActivityRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = 200,
            ).map { record ->
                SexualActivityEntry(
                    time = record.time,
                    protectionUsed = record.protectionUsed,
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = record.isOpenVitalsEntry(),
                )
            }
        }

    suspend fun writeCycleEntry(request: CycleEntryWriteRequest): String = withContext(Dispatchers.IO) {
        validateCycleEntry(request)

        val clientRecordId =
            "openvitals_cycle_${request.kind.name.lowercase()}_${request.time.toEpochMilli()}_${UUID.randomUUID()}"
        val metadata = Metadata.manualEntry(
            device = Device(type = Device.TYPE_PHONE),
            clientRecordId = clientRecordId,
        )
        support.client().insertRecords(listOf(request.toRecord(metadata)))
        clientRecordId
    }

    suspend fun readCycleEntry(kind: CycleEntryKind, id: String): CycleEntry? =
        support.withNullableLogging("readCycleEntry[$kind][$id]") {
            when (kind) {
                CycleEntryKind.MENSTRUATION_FLOW -> {
                    val record = support.client().readRecord(MenstruationFlowRecord::class, id).record
                    record.toCycleEntry(kind) { copy(flow = record.flow) }
                }
                CycleEntryKind.SPOTTING -> {
                    val record = support.client().readRecord(IntermenstrualBleedingRecord::class, id).record
                    record.toCycleEntry(kind) { this }
                }
                CycleEntryKind.SEXUAL_ACTIVITY -> {
                    val record = support.client().readRecord(SexualActivityRecord::class, id).record
                    record.toCycleEntry(kind) { copy(protectionUsed = record.protectionUsed) }
                }
                CycleEntryKind.OVULATION_TEST -> {
                    val record = support.client().readRecord(OvulationTestRecord::class, id).record
                    record.toCycleEntry(kind) { copy(ovulationTestResult = record.result) }
                }
                CycleEntryKind.CERVICAL_MUCUS -> {
                    val record = support.client().readRecord(CervicalMucusRecord::class, id).record
                    record.toCycleEntry(kind) {
                        copy(mucusAppearance = record.appearance, mucusSensation = record.sensation)
                    }
                }
                CycleEntryKind.BASAL_BODY_TEMPERATURE -> {
                    val record = support.client().readRecord(BasalBodyTemperatureRecord::class, id).record
                    record.toCycleEntry(kind) {
                        copy(
                            temperatureCelsius = record.temperature.inCelsius,
                            measurementLocation = record.measurementLocation,
                        )
                    }
                }
            }
        }

    suspend fun updateCycleEntry(id: String, request: CycleEntryWriteRequest) = withContext(Dispatchers.IO) {
        validateCycleEntry(request)

        val existing: Record = support.client().readRecord(request.kind.recordClass(), id).record
        existing.requireOpenVitalsOrigin(appPackageName)

        val metadata = Metadata.manualEntryWithId(
            id = id,
            device = existing.metadata.device ?: Device(type = Device.TYPE_PHONE),
        )
        support.client().updateRecords(listOf(request.toRecord(metadata)))
    }

    suspend fun deleteCycleEntry(kind: CycleEntryKind, id: String) = withContext(Dispatchers.IO) {
        val existing: Record = support.client().readRecord(kind.recordClass(), id).record
        existing.requireOpenVitalsOrigin(appPackageName)
        support.client().deleteRecords(
            recordType = kind.recordClass(),
            recordIdsList = listOf(existing.metadata.id),
            clientRecordIdsList = emptyList(),
        )
    }

    /**
     * Rewrites the derived MenstruationPeriodRecords around [days]. Spans
     * covered by another app are left alone. Failures are deferrable.
     */
    suspend fun reconcileMenstruationPeriods(days: Set<LocalDate>) = withContext(Dispatchers.IO) {
        if (days.isEmpty()) return@withContext

        val zone = ZoneId.systemDefault()
        val windowStart = days.min().minusDays(RECONCILE_WINDOW_DAYS).atStartOfDay(zone).toInstant()
        val windowEnd = days.max().plusDays(RECONCILE_WINDOW_DAYS + 1).atStartOfDay(zone).toInstant()

        val flowDays = support.client().readRecordsPaged(
            recordType = MenstruationFlowRecord::class,
            timeRangeFilter = TimeRangeFilter.between(windowStart, windowEnd),
            ascendingOrder = true,
            pageSize = 200,
        ).map { it.time.atZone(zone).toLocalDate() }

        val periods = support.client().readRecordsPaged(
            recordType = MenstruationPeriodRecord::class,
            timeRangeFilter = TimeRangeFilter.between(windowStart, windowEnd),
            ascendingOrder = true,
            pageSize = 100,
        )
        val (own, foreign) = periods.partition {
            isOpenVitalsRecord(it.metadata.dataOrigin.packageName, appPackageName)
        }

        val actions = periodReconcileActions(
            desiredSpans = CycleCalculations.bleedingSegments(flowDays),
            ownExisting = own.map { ExistingPeriod(it.metadata.id, it.span(zone)) },
            foreignSpans = foreign.map { it.span(zone) },
        )
        if (actions.isEmpty) return@withContext

        if (actions.toDeleteUids.isNotEmpty()) {
            support.client().deleteRecords(
                recordType = MenstruationPeriodRecord::class,
                recordIdsList = actions.toDeleteUids,
                clientRecordIdsList = emptyList(),
            )
        }
        if (actions.toUpdate.isNotEmpty()) {
            support.client().updateRecords(
                actions.toUpdate.map { (uid, span) ->
                    val device = own.firstOrNull { it.metadata.id == uid }?.metadata?.device
                    span.toPeriodRecord(
                        zone = zone,
                        metadata = Metadata.manualEntryWithId(
                            id = uid,
                            device = device ?: Device(type = Device.TYPE_PHONE),
                        ),
                    )
                },
            )
        }
        if (actions.toInsert.isNotEmpty()) {
            support.client().insertRecords(
                actions.toInsert.map { span ->
                    span.toPeriodRecord(
                        zone = zone,
                        metadata = Metadata.manualEntry(
                            device = Device(type = Device.TYPE_PHONE),
                            clientRecordId = "openvitals_cycle_period_${span.start}",
                        ),
                    )
                },
            )
        }
    }

    private fun Record.isOpenVitalsEntry(): Boolean =
        isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName)

    private fun Record.toCycleEntry(
        kind: CycleEntryKind,
        fill: CycleEntry.() -> CycleEntry,
    ): CycleEntry = CycleEntry(
        id = metadata.id,
        kind = kind,
        time = instantOf(this),
        source = SyncedSourceOverlay.displaySource(metadata),
        isOpenVitalsEntry = isOpenVitalsEntry(),
    ).fill()

    private companion object {
        const val RECONCILE_WINDOW_DAYS = 40L
    }
}

private fun instantOf(record: Record): Instant = when (record) {
    is MenstruationFlowRecord -> record.time
    is IntermenstrualBleedingRecord -> record.time
    is SexualActivityRecord -> record.time
    is OvulationTestRecord -> record.time
    is CervicalMucusRecord -> record.time
    is BasalBodyTemperatureRecord -> record.time
    else -> error("Unexpected cycle record ${record::class.simpleName}")
}

private fun CycleEntryKind.recordClass(): KClass<out Record> = when (this) {
    CycleEntryKind.MENSTRUATION_FLOW -> MenstruationFlowRecord::class
    CycleEntryKind.SPOTTING -> IntermenstrualBleedingRecord::class
    CycleEntryKind.SEXUAL_ACTIVITY -> SexualActivityRecord::class
    CycleEntryKind.OVULATION_TEST -> OvulationTestRecord::class
    CycleEntryKind.CERVICAL_MUCUS -> CervicalMucusRecord::class
    CycleEntryKind.BASAL_BODY_TEMPERATURE -> BasalBodyTemperatureRecord::class
}

private fun CycleEntryWriteRequest.toRecord(metadata: Metadata): Record {
    val zoneOffset = ZoneId.systemDefault().rules.getOffset(time)
    return when (kind) {
        CycleEntryKind.MENSTRUATION_FLOW -> MenstruationFlowRecord(
            time = time,
            zoneOffset = zoneOffset,
            flow = requireNotNull(flow),
            metadata = metadata,
        )
        CycleEntryKind.SPOTTING -> IntermenstrualBleedingRecord(
            time = time,
            zoneOffset = zoneOffset,
            metadata = metadata,
        )
        CycleEntryKind.SEXUAL_ACTIVITY -> SexualActivityRecord(
            time = time,
            zoneOffset = zoneOffset,
            protectionUsed = requireNotNull(protectionUsed),
            metadata = metadata,
        )
        CycleEntryKind.OVULATION_TEST -> OvulationTestRecord(
            time = time,
            zoneOffset = zoneOffset,
            result = requireNotNull(ovulationTestResult),
            metadata = metadata,
        )
        CycleEntryKind.CERVICAL_MUCUS -> CervicalMucusRecord(
            time = time,
            zoneOffset = zoneOffset,
            appearance = mucusAppearance ?: CycleRecordValues.MUCUS_APPEARANCE_UNKNOWN,
            sensation = mucusSensation ?: CycleRecordValues.MUCUS_SENSATION_UNKNOWN,
            metadata = metadata,
        )
        CycleEntryKind.BASAL_BODY_TEMPERATURE -> BasalBodyTemperatureRecord(
            time = time,
            zoneOffset = zoneOffset,
            temperature = requireNotNull(temperatureCelsius).celsius,
            measurementLocation = measurementLocation ?: CycleRecordValues.MEASUREMENT_LOCATION_UNKNOWN,
            metadata = metadata,
        )
    }
}

private fun MenstruationPeriodRecord.span(zone: ZoneId): ClosedRange<LocalDate> =
    startTime.atZone(zone).toLocalDate()..endTime.minusMillis(1).atZone(zone).toLocalDate()

private fun ClosedRange<LocalDate>.toPeriodRecord(zone: ZoneId, metadata: Metadata): MenstruationPeriodRecord {
    val startInstant = start.atStartOfDay(zone).toInstant()
    val endInstant = endInclusive.plusDays(1).atStartOfDay(zone).toInstant()
    return MenstruationPeriodRecord(
        startTime = startInstant,
        startZoneOffset = zone.rules.getOffset(startInstant),
        endTime = endInstant,
        endZoneOffset = zone.rules.getOffset(endInstant),
        metadata = metadata,
    )
}

/** Per-kind payload validation. BBT uses the 35-39 °C basal range, not the platform's 0-100. */
internal fun validateCycleEntry(request: CycleEntryWriteRequest, now: Instant = Instant.now()) {
    require(!request.time.isAfter(now.plus(FUTURE_TIME_GRACE))) {
        "Cycle entries cannot be in the future."
    }
    when (request.kind) {
        CycleEntryKind.MENSTRUATION_FLOW -> {
            val flow = requireNotNull(request.flow) { "Menstruation flow requires a flow level." }
            require(
                flow in setOf(
                    CycleRecordValues.FLOW_LIGHT,
                    CycleRecordValues.FLOW_MEDIUM,
                    CycleRecordValues.FLOW_HEAVY,
                )
            ) { "Unknown menstruation flow level: $flow" }
        }
        CycleEntryKind.SPOTTING -> Unit
        CycleEntryKind.SEXUAL_ACTIVITY -> {
            val protection = requireNotNull(request.protectionUsed) { "Sexual activity requires a protection value." }
            require(
                protection in setOf(
                    CycleRecordValues.PROTECTION_UNKNOWN,
                    CycleRecordValues.PROTECTION_PROTECTED,
                    CycleRecordValues.PROTECTION_UNPROTECTED,
                )
            ) { "Unknown protection value: $protection" }
        }
        CycleEntryKind.OVULATION_TEST -> {
            val result = requireNotNull(request.ovulationTestResult) { "Ovulation test requires a result." }
            require(
                result in setOf(
                    CycleRecordValues.OVULATION_INCONCLUSIVE,
                    CycleRecordValues.OVULATION_POSITIVE,
                    CycleRecordValues.OVULATION_HIGH,
                    CycleRecordValues.OVULATION_NEGATIVE,
                )
            ) { "Unknown ovulation test result: $result" }
        }
        CycleEntryKind.CERVICAL_MUCUS -> {
            val appearance = request.mucusAppearance
            val sensation = request.mucusSensation
            require(appearance == null || appearance in 0..CycleRecordValues.MUCUS_APPEARANCE_UNUSUAL) {
                "Unknown cervical mucus appearance: $appearance"
            }
            require(sensation == null || sensation in 0..CycleRecordValues.MUCUS_SENSATION_HEAVY) {
                "Unknown cervical mucus sensation: $sensation"
            }
            val hasAppearance = appearance != null && appearance != CycleRecordValues.MUCUS_APPEARANCE_UNKNOWN
            val hasSensation = sensation != null && sensation != CycleRecordValues.MUCUS_SENSATION_UNKNOWN
            require(hasAppearance || hasSensation) {
                "Cervical mucus requires an appearance or a sensation."
            }
        }
        CycleEntryKind.BASAL_BODY_TEMPERATURE -> {
            val celsius = requireNotNull(request.temperatureCelsius) { "Basal body temperature requires a value." }
            require(celsius in MIN_BBT_CELSIUS..MAX_BBT_CELSIUS) {
                "Basal body temperature out of range: $celsius"
            }
            val location = request.measurementLocation
            require(location == null || location in CycleRecordValues.MeasurementLocationRange) {
                "Unknown measurement location: $location"
            }
        }
    }
}

internal const val MIN_BBT_CELSIUS = 35.0
internal const val MAX_BBT_CELSIUS = 39.0
private val FUTURE_TIME_GRACE: Duration = Duration.ofMinutes(5)
