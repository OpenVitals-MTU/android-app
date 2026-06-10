package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.time.TimeRangeFilter
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.mergeSleepSessions
import tech.mmarca.openvitals.domain.model.mergedSleepSessionComponentIds
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

internal class SleepHealthReader(
    private val support: HealthConnectReaderSupport,
) {
    suspend fun readSleepSession(date: LocalDate): SleepData? {
        val (start, end) = support.dayRange(date)
        val queryStart = start.minus(Duration.ofDays(1))
        return support.withNullableLogging("readSleepSession[$date][$queryStart..$end]") {
            val sessions = support.client().readRecordsPaged(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(queryStart, end),
                ascendingOrder = false,
                pageSize = 50,
            ).map { it.toSleepData() }

            mergeSleepSessions(sessions).firstOrNull { session ->
                !session.endTime.isBefore(start) && session.endTime.isBefore(end)
            }
        }
    }

    suspend fun readLastSleepSession(): SleepData? =
        support.withNullableLogging("readLastSleepSession") {
            val sessions = support.client().readRecordsPaged(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                ascendingOrder = false,
                pageSize = 50,
                maxRecords = 50,
            ).map { it.toSleepData() }

            mergeSleepSessions(sessions).firstOrNull()
        }

    suspend fun readSleepSessions(start: Instant, end: Instant): List<SleepData> =
        support.withLogging("readSleepSessions[$start..$end]", emptyList()) {
            val sessions = support.client().readRecordsPaged(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 50,
            ).map { it.toSleepData() }

            mergeSleepSessions(sessions)
        }

    suspend fun readSleepSession(id: String): SleepData? =
        support.withNullableLogging("readSleepSession[$id]") {
            val componentIds = mergedSleepSessionComponentIds(id)
            if (componentIds == null) {
                support.client().readRecord(SleepSessionRecord::class, id).record.toSleepData()
            } else {
                val client = support.client()
                val sessions = componentIds.map { componentId ->
                    client.readRecord(SleepSessionRecord::class, componentId).record.toSleepData()
                }
                mergeSleepSessions(sessions).firstOrNull()
            }
        }
}
