package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.time.TimeRangeFilter
import tech.mmarca.openvitals.data.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.data.model.CervicalMucusEntry
import tech.mmarca.openvitals.data.model.MenstruationFlowEntry
import tech.mmarca.openvitals.data.model.MenstruationPeriodEntry
import tech.mmarca.openvitals.data.model.OvulationTestEntry
import java.time.Instant

internal class CycleHealthReader(
    private val support: HealthConnectReaderSupport,
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
                    source = record.metadata.dataOrigin.packageName,
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
                    source = record.metadata.dataOrigin.packageName,
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
                    source = record.metadata.dataOrigin.packageName,
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
                    source = record.metadata.dataOrigin.packageName,
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
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }
}
