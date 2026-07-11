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
 * Returns Pigeon `*Msg` types. The adaptive raw-vs-aggregated decision for chart
 * samples stays on the Dart side (`shouldUseAggregatedHeartRateSamples`); this
 * reader exposes both the raw (chunked) and aggregate-bucket reads plus the
 * aggregate-based avg / daily summaries.
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

  suspend fun readRawHeartRateSamples(start: Instant, end: Instant): List<HeartRateSampleMsg> =
    support.withLogging("readRawHeartRateSamples[$start..$end]", emptyList()) {
      var chunkStart = start
      val accumulated = mutableListOf<HeartRateSampleMsg>()
      while (chunkStart < end) {
        val chunkEnd = minOf(chunkStart.plus(RawSampleChunk), end)
        accumulated += support.client().readRecordsPaged(
          recordType = HeartRateRecord::class,
          timeRangeFilter = TimeRangeFilter.between(chunkStart, chunkEnd),
          ascendingOrder = true,
          pageSize = 500,
        ).flatMap { record ->
          val source = record.metadata.dataOrigin.packageName
          record.samples.map { sample ->
            HeartRateSampleMsg(
              timeEpochMs = sample.time.toEpochMilli(),
              beatsPerMinute = sample.beatsPerMinute,
              source = source,
            )
          }
        }
        chunkStart = chunkEnd
      }
      accumulated
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
    private val RawSampleChunk = Duration.ofHours(1)
  }
}
