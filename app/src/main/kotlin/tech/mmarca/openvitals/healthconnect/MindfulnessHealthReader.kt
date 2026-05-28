package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.time.TimeRangeFilter
import tech.mmarca.openvitals.data.model.MindfulnessSession
import tech.mmarca.openvitals.data.model.MindfulnessSessionWriteRequest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMindfulnessSessionApi::class)
internal class MindfulnessHealthReader(
    private val support: HealthConnectReaderSupport,
    private val appPackageName: String,
) {
    suspend fun readMindfulnessSessions(start: Instant, end: Instant): List<MindfulnessSession> =
        support.withLogging("readMindfulnessSessions[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = MindfulnessSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 200,
            ).map { it.toMindfulnessSession(appPackageName) }
        }

    suspend fun readMindfulnessSession(id: String): MindfulnessSession? =
        support.withNullableLogging("readMindfulnessSession[$id]") {
            support.client().readRecord(MindfulnessSessionRecord::class, id).record.toMindfulnessSession(appPackageName)
        }

    suspend fun readMindfulnessMinutes(date: LocalDate): Int {
        val (start, end) = support.dayRange(date)
        return readMindfulnessSessions(start, end).sumOf { it.durationMinutes }.toInt()
    }

    suspend fun writeMindfulnessSessionEntry(request: MindfulnessSessionWriteRequest): String = withContext(Dispatchers.IO) {
        validateMindfulnessSession(request)

        val zone = ZoneId.systemDefault()
        val clientRecordId = "openvitals_mindfulness_${request.startTime.toEpochMilli()}_${UUID.randomUUID()}"
        val record = MindfulnessSessionRecord(
            startTime = request.startTime,
            startZoneOffset = zone.rules.getOffset(request.startTime),
            endTime = request.endTime,
            endZoneOffset = zone.rules.getOffset(request.endTime),
            metadata = Metadata.manualEntry(clientRecordId = clientRecordId),
            mindfulnessSessionType = MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION,
            title = request.title,
        )

        support.client().insertRecords(listOf(record))
        clientRecordId
    }

    suspend fun updateMindfulnessSessionEntry(id: String, request: MindfulnessSessionWriteRequest) =
        withContext(Dispatchers.IO) {
            validateMindfulnessSession(request)

            val existing = support.client().readRecord(MindfulnessSessionRecord::class, id).record
            existing.requireOpenVitalsOrigin(appPackageName)

            val zone = ZoneId.systemDefault()
            val record = MindfulnessSessionRecord(
                startTime = request.startTime,
                startZoneOffset = zone.rules.getOffset(request.startTime),
                endTime = request.endTime,
                endZoneOffset = zone.rules.getOffset(request.endTime),
                metadata = Metadata.manualEntryWithId(id = id, device = existing.metadata.device),
                mindfulnessSessionType = existing.mindfulnessSessionType,
                title = request.title,
                notes = existing.notes,
            )

            support.client().updateRecords(listOf(record))
        }

    private fun validateMindfulnessSession(request: MindfulnessSessionWriteRequest) {
        require(request.title.isNotBlank()) { "Mindfulness session title cannot be blank." }
        require(request.startTime.isBefore(request.endTime)) { "Mindfulness session start must be before end." }
        val durationMinutes = java.time.Duration.between(request.startTime, request.endTime).toMinutes()
        require(durationMinutes in MinSessionMinutes..MaxSessionMinutes) {
            "Mindfulness session duration must be between $MinSessionMinutes and $MaxSessionMinutes minutes."
        }
    }
}

private const val MinSessionMinutes = 1L
private const val MaxSessionMinutes = 24L * 60L
