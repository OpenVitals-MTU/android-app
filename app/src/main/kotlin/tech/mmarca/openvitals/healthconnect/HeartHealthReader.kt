package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HeartRateChartBucketDuration
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.model.HrvSample
import tech.mmarca.openvitals.domain.model.heartRateSampleFromAggregateBucket
import tech.mmarca.openvitals.domain.model.shouldUseAggregatedHeartRateSamples
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal class HeartHealthReader(
    private val support: HealthConnectReaderSupport,
) {
    companion object {
        private val HeartRateRawSampleReadChunkDuration = Duration.ofHours(1)
    }

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
            val range = Duration.between(start, end)
            if (shouldUseAggregatedHeartRateSamples(range)) {
                readAggregatedHeartRateSamples(start, end)
            } else {
                readRawHeartRateSamples(start, end)
            }
        }

    /**
     * Uses Health Connect duration aggregation so high-frequency days (for example Fitbit) are not
     * truncated by [readRecordsPaged] page limits.
     */
    private suspend fun readAggregatedHeartRateSamples(
        start: Instant,
        end: Instant,
    ): List<HeartRateSample> =
        support.client().aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(HeartRateRecord.BPM_AVG),
                timeRangeFilter = TimeRangeFilter.between(start, end),
                timeRangeSlicer = HeartRateChartBucketDuration,
            )
        ).mapNotNull { bucket ->
            val avg = bucket.result[HeartRateRecord.BPM_AVG] ?: return@mapNotNull null
            heartRateSampleFromAggregateBucket(
                startTime = bucket.startTime,
                avgBpm = avg,
            )
        }

    private suspend fun readRawHeartRateSamples(start: Instant, end: Instant): List<HeartRateSample> {
        var chunkStart = start
        val accumulated = mutableListOf<HeartRateSample>()
        while (chunkStart < end) {
            val chunkEnd = minOf(
                chunkStart.plus(HeartRateRawSampleReadChunkDuration),
                end,
            )
            accumulated += readRawHeartRateSamplesChunk(chunkStart, chunkEnd)
            chunkStart = chunkEnd
        }
        return accumulated
    }

    private suspend fun readRawHeartRateSamplesChunk(start: Instant, end: Instant): List<HeartRateSample> =
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
            readHrvSamples(start, end)
                .takeIf { it.isNotEmpty() }
                ?.map { it.rmssdMs }
                ?.average()
        }
    }

    suspend fun readHrvSamples(start: Instant, end: Instant): List<HrvSample> =
        support.withLogging("readHrvSamples[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = HeartRateVariabilityRmssdRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = 100,
            ).map { record ->
                HrvSample(
                    time = record.time,
                    rmssdMs = record.heartRateVariabilityMillis,
                    source = record.metadata.dataOrigin.packageName,
                )
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
