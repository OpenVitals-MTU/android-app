package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.time.TimeRangeFilter
import tech.mmarca.openvitals.data.model.MindfulnessSession
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalMindfulnessSessionApi::class)
internal class MindfulnessHealthReader(
    private val support: HealthConnectReaderSupport,
) {
    suspend fun readMindfulnessSessions(start: Instant, end: Instant): List<MindfulnessSession> =
        support.withLogging("readMindfulnessSessions[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = MindfulnessSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 200,
            ).map { it.toMindfulnessSession() }
        }

    suspend fun readMindfulnessMinutes(date: LocalDate): Int {
        val (start, end) = support.dayRange(date)
        return readMindfulnessSessions(start, end).sumOf { it.durationMinutes }.toInt()
    }
}
