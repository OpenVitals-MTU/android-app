package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

/**
 * Ported from the native OpenVitals app (`healthconnect/SleepHealthReader.kt`
 * + the `toSleepData` mapper). Returns RAW (unmerged) sessions with stages;
 * merging, range selection, and stage-based duration all stay on the Dart side.
 */
internal class SleepHealthReader(
  private val support: HealthConnectReaderSupport,
) {
  suspend fun readSleepSessionsRaw(start: Instant, end: Instant): List<SleepDataMsg> =
    support.withLogging("readSleepSessionsRaw[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = SleepSessionRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = false,
        pageSize = 50,
      ).map { it.toMsg() }
    }

  suspend fun readSleepSessionById(id: String): SleepDataMsg? =
    support.withNullableLogging("readSleepSessionById[$id]") {
      support.client().readRecord(SleepSessionRecord::class, id).record.toMsg()
    }

  private fun SleepSessionRecord.toMsg() = SleepDataMsg(
    id = metadata.id,
    startEpochMs = startTime.toEpochMilli(),
    endEpochMs = endTime.toEpochMilli(),
    source = metadata.dataOrigin.packageName,
    title = title,
    notes = notes,
    clientRecordId = metadata.clientRecordId,
    device = metadata.device?.let {
      SleepDeviceDataMsg(type = it.type.toLong(), manufacturer = it.manufacturer, model = it.model)
    },
    stages = stages.map {
      SleepStageMsg(
        startEpochMs = it.startTime.toEpochMilli(),
        endEpochMs = it.endTime.toEpochMilli(),
        stageType = it.stage.toLong(),
      )
    },
    // The record's own provenance. The zone offsets are the ones the WRITER
    // recorded the night in -- a night slept in another timezone is the reason
    // Health Connect keeps them at all, so they are passed through rather than
    // derived from this phone's zone.
    startZoneOffsetSeconds = startZoneOffset?.totalSeconds?.toLong(),
    endZoneOffsetSeconds = endZoneOffset?.totalSeconds?.toLong(),
    lastModifiedEpochMs = metadata.lastModifiedTime.toEpochMilli(),
    clientRecordVersion = metadata.clientRecordVersion,
    recordingMethod = metadata.recordingMethod.toLong(),
  )
}
