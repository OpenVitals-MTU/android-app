package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.time.TimeRangeFilter
import tech.mmarca.openvitals.data.model.SleepData
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
            support.client().readRecordsPaged(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(queryStart, end),
                ascendingOrder = false,
                pageSize = 50,
            ).firstOrNull { record ->
                !record.endTime.isBefore(start) && record.endTime.isBefore(end)
            }?.toSleepData()
        }
    }

    suspend fun readLastSleepSession(): SleepData? =
        support.withNullableLogging("readLastSleepSession") {
            support.client().readRecordsPaged(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                ascendingOrder = false,
                pageSize = 1,
                maxRecords = 1,
            ).firstOrNull()?.toSleepData()
        }

    suspend fun readSleepSessions(start: Instant, end: Instant): List<SleepData> =
        support.withLogging("readSleepSessions[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 50,
            ).map { it.toSleepData() }
        }

    suspend fun readSleepSession(id: String): SleepData? =
        support.withNullableLogging("readSleepSession[$id]") {
            support.client().readRecord(SleepSessionRecord::class, id).record.toSleepData()
        }
}
