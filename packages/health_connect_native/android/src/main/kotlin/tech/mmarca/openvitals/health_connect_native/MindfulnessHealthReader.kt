package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ported from the native OpenVitals app (`healthconnect/MindfulnessHealthReader.kt`
 * + the `toMindfulnessSession` mapper). Returns Pigeon `*Msg` types.
 *
 * MindfulnessSessionRecord is gated behind an experimental connect-client opt-in
 * (declared in the module's Gradle `freeCompilerArgs`).
 */
internal class MindfulnessHealthReader(
  private val support: HealthConnectReaderSupport,
  private val appPackageName: String,
) {
  suspend fun readMindfulnessSessions(start: Instant, end: Instant): List<MindfulnessSessionMsg> =
    support.withLogging("readMindfulnessSessions[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = MindfulnessSessionRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = false,
        pageSize = 200,
      ).map { it.toMsg() }
    }

  suspend fun readMindfulnessSession(id: String): MindfulnessSessionMsg? =
    support.withNullableLogging("readMindfulnessSession[$id]") {
      support.client().readRecord(MindfulnessSessionRecord::class, id).record.toMsg()
    }

  suspend fun readMindfulnessMinutes(start: Instant, end: Instant): Long =
    support.withLogging("readMindfulnessMinutes[$start..$end]", 0L) {
      support.client().aggregate(
        AggregateRequest(
          metrics = setOf(MindfulnessSessionRecord.MINDFULNESS_DURATION_TOTAL),
          timeRangeFilter = TimeRangeFilter.between(start, end),
        ),
      )[MindfulnessSessionRecord.MINDFULNESS_DURATION_TOTAL]?.toMinutes() ?: 0L
    }

  suspend fun writeMindfulnessSessionEntry(request: MindfulnessSessionWriteRequestMsg): String =
    withContext(Dispatchers.IO) {
      support.requireSyncEnabled()
      val startTime = Instant.ofEpochMilli(request.startEpochMs)
      val endTime = Instant.ofEpochMilli(request.endEpochMs)
      validate(request.title, startTime, endTime)
      val zone = ZoneId.systemDefault()
      val clientRecordId = "openvitals_mindfulness_${startTime.toEpochMilli()}_${UUID.randomUUID()}"
      val record = MindfulnessSessionRecord(
        startTime = startTime,
        startZoneOffset = zone.rules.getOffset(startTime),
        endTime = endTime,
        endZoneOffset = zone.rules.getOffset(endTime),
        metadata = Metadata.manualEntry(clientRecordId = clientRecordId),
        mindfulnessSessionType = MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION,
        title = request.title,
      )
      support.client().insertRecords(listOf(record))
      clientRecordId
    }

  suspend fun updateMindfulnessSessionEntry(id: String, request: MindfulnessSessionWriteRequestMsg) =
    withContext(Dispatchers.IO) {
      support.requireSyncEnabled()
      val startTime = Instant.ofEpochMilli(request.startEpochMs)
      val endTime = Instant.ofEpochMilli(request.endEpochMs)
      validate(request.title, startTime, endTime)
      val existing = support.client().readRecord(MindfulnessSessionRecord::class, id).record
      existing.requireOpenVitalsOrigin(appPackageName)
      val zone = ZoneId.systemDefault()
      val record = MindfulnessSessionRecord(
        startTime = startTime,
        startZoneOffset = zone.rules.getOffset(startTime),
        endTime = endTime,
        endZoneOffset = zone.rules.getOffset(endTime),
        metadata = Metadata.manualEntryWithId(id = id, device = existing.metadata.device),
        mindfulnessSessionType = existing.mindfulnessSessionType,
        title = request.title,
        notes = existing.notes,
      )
      support.client().updateRecords(listOf(record))
    }

  suspend fun deleteMindfulnessSessionEntry(id: String) = withContext(Dispatchers.IO) {
    support.requireSyncEnabled()
    val existing = support.client().readRecord(MindfulnessSessionRecord::class, id).record
    existing.requireOpenVitalsOrigin(appPackageName)
    support.client().deleteRecords(
      recordType = MindfulnessSessionRecord::class,
      recordIdsList = listOf(existing.metadata.id),
      clientRecordIdsList = emptyList(),
    )
  }

  private fun validate(title: String, startTime: Instant, endTime: Instant) {
    require(title.isNotBlank()) { "Mindfulness session title cannot be blank." }
    require(startTime.isBefore(endTime)) { "Mindfulness session start must be before end." }
    val durationMinutes = Duration.between(startTime, endTime).toMinutes()
    require(durationMinutes in MinSessionMinutes..MaxSessionMinutes) {
      "Mindfulness session duration must be between $MinSessionMinutes and $MaxSessionMinutes minutes."
    }
  }

  private fun MindfulnessSessionRecord.toMsg() = MindfulnessSessionMsg(
    id = metadata.id,
    title = title,
    startEpochMs = startTime.toEpochMilli(),
    endEpochMs = endTime.toEpochMilli(),
    durationMs = endTime.toEpochMilli() - startTime.toEpochMilli(),
    source = metadata.dataOrigin.packageName,
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
  )

  private companion object {
    private const val MinSessionMinutes = 1L
    private const val MaxSessionMinutes = 24L * 60L
  }
}
