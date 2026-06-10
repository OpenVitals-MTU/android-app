package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal class HeartHealthReader(
    private val support: HealthConnectReaderSupport,
) {
    suspend fun readAvgHeartRate(date: LocalDate): Long? {
        val (start, end) = support.dayRange(date)
        return support.withNullableLogging("readAvgHeartRate[$date][$start..$end]") {
            support.client().aggregate(
                AggregateRequest(
                    metrics = setOf(HeartRateRecord.BPM_AVG),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[HeartRateRecord.BPM_AVG]
        }
    }

    suspend fun readAvgHeartRateToday(): Long? = readAvgHeartRate(LocalDate.now())

    suspend fun readHeartRateSamples(start: Instant, end: Instant): List<HeartRateSample> =
        support.withLogging("readHeartRateSamples[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = 500,
            ).flatMap { record ->
                val source = record.metadata.dataOrigin.packageName
                record.samples.map { sample ->
                    HeartRateSample(
                        time = sample.time,
                        beatsPerMinute = sample.beatsPerMinute,
                        source = source,
                    )
                }
            }
        }

    suspend fun readDailyHeartRateSummaries(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<HeartRateSummary> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withLogging("readDailyHeartRateSummaries[$start..$end]", emptyList()) {
            support.client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(
                        HeartRateRecord.BPM_AVG,
                        HeartRateRecord.BPM_MIN,
                        HeartRateRecord.BPM_MAX,
                    ),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofDays(1),
                )
            ).mapNotNull { bucket ->
                val avg = bucket.result[HeartRateRecord.BPM_AVG] ?: return@mapNotNull null
                HeartRateSummary(
                    date = bucket.startTime.atZone(zone).toLocalDate(),
                    avgBpm = avg,
                    minBpm = bucket.result[HeartRateRecord.BPM_MIN] ?: avg,
                    maxBpm = bucket.result[HeartRateRecord.BPM_MAX] ?: avg,
                )
            }
        }
    }

    suspend fun readRestingHeartRate(date: LocalDate): Long? {
        val (start, end) = support.dayRange(date)
        return support.withNullableLogging("readRestingHeartRate[$date][$start..$end]") {
            support.client().aggregate(
                AggregateRequest(
                    metrics = setOf(RestingHeartRateRecord.BPM_AVG),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[RestingHeartRateRecord.BPM_AVG]
        }
    }

    suspend fun readDailyRestingHR(startDate: LocalDate, endDate: LocalDate): List<DailyRestingHR> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withLogging("readDailyRestingHR[$start..$end]", emptyList()) {
            support.client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(RestingHeartRateRecord.BPM_AVG),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofDays(1),
                )
            ).mapNotNull { bucket ->
                val bpm = bucket.result[RestingHeartRateRecord.BPM_AVG] ?: return@mapNotNull null
                DailyRestingHR(
                    date = bucket.startTime.atZone(zone).toLocalDate(),
                    bpm = bpm,
                )
            }
        }
    }

    suspend fun readHrvRmssd(date: LocalDate): Double? {
        val (start, end) = support.dayRange(date)
        return support.withNullableLogging("readHrvRmssd[$date][$start..$end]") {
            val records = support.client().readRecordsPaged(
                recordType = HeartRateVariabilityRmssdRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = 100,
            )
            if (records.isEmpty()) null
            else records.map { it.heartRateVariabilityMillis }.average()
        }
    }

    suspend fun readDailyHRV(startDate: LocalDate, endDate: LocalDate): List<DailyHrv> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withLogging("readDailyHRV[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = HeartRateVariabilityRmssdRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
            )
                .groupBy { it.time.atZone(zone).toLocalDate() }
                .map { (date, records) ->
                    DailyHrv(
                        date = date,
                        rmssdMs = records.map { it.heartRateVariabilityMillis }.average(),
                    )
                }
        }
    }
}
