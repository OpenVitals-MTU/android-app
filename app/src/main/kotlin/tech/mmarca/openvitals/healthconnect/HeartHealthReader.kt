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
import tech.mmarca.openvitals.domain.model.RestingHeartRateSample
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
        private val MinAggregateBucket: Duration = Duration.ofSeconds(30)
        private const val MaxAggregateBuckets = 240L
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
                readAggregatedHeartRateSamples(start, end, HeartRateChartBucketDuration)
            } else {
                readRawOrAggregatedFallback(start, end)
            }
        }

    /**
     * Every heart-rate sample in `[start, end)`, however the writer grouped it.
     *
     * [readSeriesSamples] handles the record-boundary problem (see its docs). What
     * is left here is the last resort for the case it cannot reach: a record so
     * long that even the widened read misses it. Aggregation slices by TIME rather
     * than by record, so it cannot be hidden the same way — it costs a resolution
     * of one bucket instead of one beat, which is still a heart-rate trace where
     * the alternative is a blank chart.
     */
    suspend fun readRawHeartRateSamples(start: Instant, end: Instant): List<HeartRateSample> =
        support.withLogging("readRawHeartRateSamples[$start..$end]", emptyList()) {
            readRawOrAggregatedFallback(start, end)
        }

    private suspend fun readRawOrAggregatedFallback(start: Instant, end: Instant): List<HeartRateSample> {
        val samples = support.client()
            .readSeriesSamples(HeartRateRecord::class, start, end) { record ->
                val source = SyncedSourceOverlay.displaySource(record.metadata)
                record.samples.map { sample ->
                    TimedSample(
                        sample.time,
                        HeartRateSample(
                            time = sample.time,
                            beatsPerMinute = sample.beatsPerMinute,
                            source = source,
                        ),
                    )
                }
            }
        if (samples.isNotEmpty()) return samples
        return readAggregatedHeartRateSamples(start, end, aggregateBucket(start, end))
    }

    /**
     * Uses Health Connect duration aggregation so high-frequency days (for example Fitbit) are not
     * truncated by [readRecordsPaged] page limits.
     */
    private suspend fun readAggregatedHeartRateSamples(
        start: Instant,
        end: Instant,
        bucket: Duration,
    ): List<HeartRateSample> =
        support.client().aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(HeartRateRecord.BPM_AVG),
                timeRangeFilter = TimeRangeFilter.between(start, end),
                timeRangeSlicer = bucket,
            )
        ).mapNotNull { bucket ->
            val avg = bucket.result[HeartRateRecord.BPM_AVG] ?: return@mapNotNull null
            heartRateSampleFromAggregateBucket(
                startTime = bucket.startTime,
                avgBpm = avg,
            )
        }

    /**
     * A bucket fine enough to read as a trace rather than a flat line, without
     * asking Health Connect for thousands of slices on a long window.
     */
    private fun aggregateBucket(start: Instant, end: Instant): Duration {
        val even = Duration.between(start, end).dividedBy(MaxAggregateBuckets)
        return if (even > MinAggregateBucket) even else MinAggregateBucket
    }

    suspend fun readDailyHeartRateSummaries(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<HeartRateSummary> {
        val zone = ZoneId.systemDefault()
        // Chunked like the other day-bucketed aggregates: a year of daily
        // buckets with three metrics each returns in one Binder parcel and can
        // blow the shared 1 MB buffer (see DailyAggregateMaxQueryDays).
        return dailyAggregateDateChunks(startDate, endDate).flatMap { (chunkStart, chunkEnd) ->
            val start = chunkStart.atStartOfDay(zone).toInstant()
            val end = chunkEnd.plusDays(1).atStartOfDay(zone).toInstant()
            support.withLogging("readDailyHeartRateSummaries[$start..$end]", emptyList()) {
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
                ).byLocalDate(zone).mapNotNull { day ->
                    val avg = day.weightedAverage { it[HeartRateRecord.BPM_AVG] } ?: return@mapNotNull null
                    HeartRateSummary(
                        date = day.date,
                        avgBpm = avg,
                        minBpm = day.lowest { it[HeartRateRecord.BPM_MIN] } ?: avg,
                        maxBpm = day.highest { it[HeartRateRecord.BPM_MAX] } ?: avg,
                    )
                }
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

    suspend fun readRestingHeartRateSamples(start: Instant, end: Instant): List<RestingHeartRateSample> =
        support.withLogging("readRestingHeartRateSamples[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = RestingHeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = 500,
            ).map { record ->
                RestingHeartRateSample(
                    time = record.time,
                    beatsPerMinute = record.beatsPerMinute,
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                )
            }
        }

    suspend fun readDailyRestingHR(startDate: LocalDate, endDate: LocalDate): List<DailyRestingHR> {
        val zone = ZoneId.systemDefault()
        return dailyAggregateDateChunks(startDate, endDate).flatMap { (chunkStart, chunkEnd) ->
            val start = chunkStart.atStartOfDay(zone).toInstant()
            val end = chunkEnd.plusDays(1).atStartOfDay(zone).toInstant()
            support.withLogging("readDailyRestingHR[$start..$end]", emptyList()) {
                support.client().aggregateGroupByDuration(
                    AggregateGroupByDurationRequest(
                        metrics = setOf(RestingHeartRateRecord.BPM_AVG),
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                        timeRangeSlicer = Duration.ofDays(1),
                    )
                ).byLocalDate(zone).mapNotNull { day ->
                    val bpm = day.weightedAverage { it[RestingHeartRateRecord.BPM_AVG] }
                        ?: return@mapNotNull null
                    DailyRestingHR(
                        date = day.date,
                        bpm = bpm,
                    )
                }
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
                    source = SyncedSourceOverlay.displaySource(record.metadata),
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
