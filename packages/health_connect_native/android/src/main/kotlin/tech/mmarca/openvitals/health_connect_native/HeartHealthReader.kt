package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Ported from the native OpenVitals app (`healthconnect/HeartHealthReader.kt`).
 *
 * Returns Pigeon `*Msg` types. Everything about how Health Connect *stores*
 * heart rate — that a series record hides its samples behind its own boundary,
 * and that aggregation is the way out when it does — is settled here. Dart asks
 * for a window and gets the samples in it; it does not get a say in how they
 * were found.
 */
internal class HeartHealthReader(
  private val support: HealthConnectReaderSupport,
) {
  suspend fun readAvgHeartRate(start: Instant, end: Instant): Long? =
    support.withNullableLogging("readAvgHeartRate[$start..$end]") {
      support.client().aggregate(
        AggregateRequest(
          metrics = setOf(HeartRateRecord.BPM_AVG),
          timeRangeFilter = TimeRangeFilter.between(start, end),
        ),
      )[HeartRateRecord.BPM_AVG]
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
  suspend fun readRawHeartRateSamples(start: Instant, end: Instant): List<HeartRateSampleMsg> =
    support.withLogging("readRawHeartRateSamples[$start..$end]", emptyList()) {
      val samples = support.client()
        .readSeriesSamples(HeartRateRecord::class, start, end) { record ->
          val source = record.metadata.dataOrigin.packageName
          record.samples.map { sample ->
            TimedSample(
              sample.time,
              HeartRateSampleMsg(
                timeEpochMs = sample.time.toEpochMilli(),
                beatsPerMinute = sample.beatsPerMinute,
                source = source,
              ),
            )
          }
        }
      if (samples.isNotEmpty()) {
        return@withLogging samples
      }
      readHeartRateAggregatedBuckets(start, end, aggregateBucket(start, end).toMillis())
        .map { bucket ->
          HeartRateSampleMsg(
            timeEpochMs = bucket.startEpochMs,
            beatsPerMinute = bucket.avgBpm,
            // No single writer owns an aggregate: Health Connect merged it. The
            // empty source is the same sentinel the Dart side already uses.
            source = "",
          )
        }
    }

  /**
   * A bucket fine enough to read as a trace rather than a flat line, without
   * asking Health Connect for thousands of slices on a long window.
   */
  private fun aggregateBucket(start: Instant, end: Instant): Duration {
    val even = Duration.between(start, end).dividedBy(MaxAggregateBuckets)
    return if (even > MinAggregateBucket) even else MinAggregateBucket
  }

  suspend fun readHeartRateAggregatedBuckets(
    start: Instant,
    end: Instant,
    bucketMs: Long,
  ): List<HeartRateAggBucketMsg> =
    support.withLogging("readHeartRateAggregatedBuckets[$start..$end]", emptyList()) {
      support.client().aggregateGroupByDuration(
        AggregateGroupByDurationRequest(
          metrics = setOf(HeartRateRecord.BPM_AVG),
          timeRangeFilter = TimeRangeFilter.between(start, end),
          timeRangeSlicer = Duration.ofMillis(bucketMs),
        ),
      ).mapNotNull { bucket ->
        val avg = bucket.result[HeartRateRecord.BPM_AVG] ?: return@mapNotNull null
        HeartRateAggBucketMsg(startEpochMs = bucket.startTime.toEpochMilli(), avgBpm = avg)
      }
    }

  suspend fun readDailyHeartRateSummaries(start: Instant, end: Instant): List<HeartRateSummaryMsg> =
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
        ),
      ).mapNotNull { bucket ->
        val avg = bucket.result[HeartRateRecord.BPM_AVG] ?: return@mapNotNull null
        HeartRateSummaryMsg(
          dateEpochMs = bucket.startTime.toEpochMilli(),
          avgBpm = avg,
          minBpm = bucket.result[HeartRateRecord.BPM_MIN] ?: avg,
          maxBpm = bucket.result[HeartRateRecord.BPM_MAX] ?: avg,
        )
      }
    }

  suspend fun readRestingHeartRate(start: Instant, end: Instant): Long? =
    support.withNullableLogging("readRestingHeartRate[$start..$end]") {
      support.client().aggregate(
        AggregateRequest(
          metrics = setOf(RestingHeartRateRecord.BPM_AVG),
          timeRangeFilter = TimeRangeFilter.between(start, end),
        ),
      )[RestingHeartRateRecord.BPM_AVG]
    }

  suspend fun readRestingHeartRateSamples(start: Instant, end: Instant): List<RestingHeartRateSampleMsg> =
    support.withLogging("readRestingHeartRateSamples[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = RestingHeartRateRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
        pageSize = 500,
      ).map { record ->
        RestingHeartRateSampleMsg(
          timeEpochMs = record.time.toEpochMilli(),
          beatsPerMinute = record.beatsPerMinute,
          source = record.metadata.dataOrigin.packageName,
        )
      }
    }

  suspend fun readDailyRestingHR(start: Instant, end: Instant): List<DailyRestingHRMsg> =
    support.withLogging("readDailyRestingHR[$start..$end]", emptyList()) {
      support.client().aggregateGroupByDuration(
        AggregateGroupByDurationRequest(
          metrics = setOf(RestingHeartRateRecord.BPM_AVG),
          timeRangeFilter = TimeRangeFilter.between(start, end),
          timeRangeSlicer = Duration.ofDays(1),
        ),
      ).mapNotNull { bucket ->
        val bpm = bucket.result[RestingHeartRateRecord.BPM_AVG] ?: return@mapNotNull null
        DailyRestingHRMsg(dateEpochMs = bucket.startTime.toEpochMilli(), bpm = bpm)
      }
    }

  suspend fun readHrvSamples(start: Instant, end: Instant): List<HrvSampleMsg> =
    support.withLogging("readHrvSamples[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = HeartRateVariabilityRmssdRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
        pageSize = 100,
      ).map { record ->
        HrvSampleMsg(
          timeEpochMs = record.time.toEpochMilli(),
          rmssdMs = record.heartRateVariabilityMillis,
          source = record.metadata.dataOrigin.packageName,
        )
      }
    }

  suspend fun readDailyHRV(start: Instant, end: Instant): List<DailyHrvMsg> =
    support.withLogging("readDailyHRV[$start..$end]", emptyList()) {
      val zone = ZoneId.systemDefault()
      support.client().readRecordsPaged(
        recordType = HeartRateVariabilityRmssdRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
      ).groupBy { it.time.atZone(zone).toLocalDate() }
        .map { (date, records) ->
          DailyHrvMsg(
            dateEpochMs = date.atStartOfDay(zone).toInstant().toEpochMilli(),
            rmssdMs = records.map { it.heartRateVariabilityMillis }.average(),
          )
        }
    }

  private companion object {
    private val MinAggregateBucket: Duration = Duration.ofSeconds(30)
    private const val MaxAggregateBuckets = 240L
  }
}
