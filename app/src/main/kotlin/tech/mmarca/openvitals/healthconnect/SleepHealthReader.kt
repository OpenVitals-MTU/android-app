package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.time.TimeRangeFilter
import tech.mmarca.openvitals.domain.model.DailySleepDuration
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepReadData
import tech.mmarca.openvitals.domain.model.mergeSleepSessions
import tech.mmarca.openvitals.domain.model.mergedSleepSessionComponentIds
import tech.mmarca.openvitals.domain.model.sleepRangeEndFor
import tech.mmarca.openvitals.domain.model.sleepRangeStartFor
import tech.mmarca.openvitals.domain.model.sleepSessionsForRange
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId

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

    suspend fun readSleepData(
        startDate: LocalDate,
        endDate: LocalDate,
        sleepRangeMode: SleepRangeMode,
    ): SleepReadData {
        if (startDate.isAfter(endDate)) return SleepReadData()

        val sessions = readSleepSessionsForDates(startDate, endDate, sleepRangeMode)
        val durations = readSleepDurationsByLocalDay(
            start = sleepRangeStartFor(startDate, sleepRangeMode),
            end = sleepRangeEndFor(endDate, sleepRangeMode),
        )
        val dailyAggregateDurations = datesBetween(startDate, endDate).map { date ->
            DailySleepDuration(
                date = date,
                durationMs = durations[sleepRangeStartFor(date, sleepRangeMode)] ?: 0L,
            )
        }
        return SleepReadData(
            sessions = sessions,
            dailyAggregateDurations = dailyAggregateDurations,
        )
    }

    private suspend fun readSleepSessionsForDates(
        startDate: LocalDate,
        endDate: LocalDate,
        sleepRangeMode: SleepRangeMode,
    ): List<SleepData> {
        val zone = ZoneId.systemDefault()
        val queryStart = sleepRangeStartFor(startDate, sleepRangeMode)
            .atZone(zone)
            .toInstant()
            .minus(Duration.ofDays(1))
        val queryEnd = sleepRangeEndFor(endDate, sleepRangeMode)
            .atZone(zone)
            .toInstant()
            .plus(Duration.ofDays(1))
        val sessions = readSleepSessions(queryStart, queryEnd)
        return datesBetween(startDate, endDate)
            .flatMap { date ->
                sleepSessionsForRange(
                    sessions = sessions,
                    selectedDate = date,
                    sleepRangeMode = sleepRangeMode,
                    zone = zone,
                )
            }
            .distinctBy { it.id }
            .sortedByDescending { it.endTime }
    }

    private suspend fun readSleepDurationsByLocalDay(
        start: LocalDateTime,
        end: LocalDateTime,
    ): Map<LocalDateTime, Long> =
        support.withLogging("readSleepDurationsByLocalDay[$start..$end]", emptyMap()) {
            support.client().aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Period.ofDays(1),
                )
            ).associate { bucket ->
                bucket.startTime to (bucket.result[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMillis() ?: 0L)
            }
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

private fun datesBetween(startDate: LocalDate, endDate: LocalDate): List<LocalDate> =
    generateSequence(startDate) { current ->
        current.plusDays(1).takeUnless { it.isAfter(endDate) }
    }.toList()
