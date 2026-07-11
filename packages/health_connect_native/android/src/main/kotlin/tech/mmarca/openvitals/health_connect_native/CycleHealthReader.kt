package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

/**
 * Ported from the native OpenVitals app (`healthconnect/CycleHealthReader.kt`).
 * All cycle records are read-only (no ownership needed). Returns Pigeon `*Msg`.
 */
internal class CycleHealthReader(
  private val support: HealthConnectReaderSupport,
) {
  suspend fun readMenstruationFlowEntries(start: Instant, end: Instant): List<MenstruationFlowEntryMsg> =
    support.withLogging("readMenstruationFlowEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = MenstruationFlowRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
        pageSize = 200,
      ).map { record ->
        MenstruationFlowEntryMsg(
          timeEpochMs = record.time.toEpochMilli(),
          flow = record.flow.toLong(),
          source = record.metadata.dataOrigin.packageName,
        )
      }
    }

  suspend fun readMenstruationPeriods(start: Instant, end: Instant): List<MenstruationPeriodEntryMsg> =
    support.withLogging("readMenstruationPeriods[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = MenstruationPeriodRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
        pageSize = 100,
      ).map { record ->
        MenstruationPeriodEntryMsg(
          startEpochMs = record.startTime.toEpochMilli(),
          endEpochMs = record.endTime.toEpochMilli(),
          source = record.metadata.dataOrigin.packageName,
        )
      }
    }

  suspend fun readOvulationTests(start: Instant, end: Instant): List<OvulationTestEntryMsg> =
    support.withLogging("readOvulationTests[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = OvulationTestRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
        pageSize = 200,
      ).map { record ->
        OvulationTestEntryMsg(
          timeEpochMs = record.time.toEpochMilli(),
          result = record.result.toLong(),
          source = record.metadata.dataOrigin.packageName,
        )
      }
    }

  suspend fun readCervicalMucusEntries(start: Instant, end: Instant): List<CervicalMucusEntryMsg> =
    support.withLogging("readCervicalMucusEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = CervicalMucusRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
        pageSize = 200,
      ).map { record ->
        CervicalMucusEntryMsg(
          timeEpochMs = record.time.toEpochMilli(),
          appearance = record.appearance.toLong(),
          sensation = record.sensation.toLong(),
          source = record.metadata.dataOrigin.packageName,
        )
      }
    }

  suspend fun readBasalBodyTemperatureEntries(start: Instant, end: Instant): List<BasalBodyTemperatureEntryMsg> =
    support.withLogging("readBasalBodyTemperatureEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = BasalBodyTemperatureRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
        pageSize = 200,
      ).map { record ->
        BasalBodyTemperatureEntryMsg(
          timeEpochMs = record.time.toEpochMilli(),
          temperatureCelsius = record.temperature.inCelsius,
          measurementLocation = record.measurementLocation.toLong(),
          source = record.metadata.dataOrigin.packageName,
        )
      }
    }

  suspend fun readIntermenstrualBleedingEntries(start: Instant, end: Instant): List<IntermenstrualBleedingEntryMsg> =
    support.withLogging("readIntermenstrualBleedingEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = IntermenstrualBleedingRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
        pageSize = 200,
      ).map { record ->
        IntermenstrualBleedingEntryMsg(
          timeEpochMs = record.time.toEpochMilli(),
          source = record.metadata.dataOrigin.packageName,
        )
      }
    }

  suspend fun readSexualActivityEntries(start: Instant, end: Instant): List<SexualActivityEntryMsg> =
    support.withLogging("readSexualActivityEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = SexualActivityRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
        pageSize = 200,
      ).map { record ->
        SexualActivityEntryMsg(
          timeEpochMs = record.time.toEpochMilli(),
          protectionUsed = record.protectionUsed.toLong(),
          source = record.metadata.dataOrigin.packageName,
        )
      }
    }
}
