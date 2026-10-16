package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToLong
import tech.mmarca.openvitals.core.stats.timeBucketedAverageOrNull

data class HeartRateSample(
    val time: Instant,
    val beatsPerMinute: Long,
    val source: String,
)

data class RestingHeartRateSample(
    val time: Instant,
    val beatsPerMinute: Long,
    val source: String,
)

/** A day's resting rate: the minute-bucketed mean of its samples, rounded. */
fun List<RestingHeartRateSample>.dayAverageBpm(): Long? =
    timeBucketedAverageOrNull(time = { it.time }, value = { it.beatsPerMinute.toDouble() })?.roundToLong()

data class HrvSample(
    val time: Instant,
    val rmssdMs: Double,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class HeartRateSummary(
    val date: LocalDate,
    val avgBpm: Long,
    val minBpm: Long,
    val maxBpm: Long,
)

/**
 * One local day of hourly heart-rate aggregates. Inside an hour Health Connect
 * weights by sample, so [summary]'s average leans toward 1 Hz workouts.
 * [signature] changes when any hour's count, average, low or high does.
 */
data class HeartRateDayAggregate(
    val summary: HeartRateSummary,
    val sampleCount: Long,
    val signature: String,
)

data class DailyRestingHR(
    val date: LocalDate,
    val bpm: Long,
)

data class DailyHrv(
    val date: LocalDate,
    val rmssdMs: Double,
)
