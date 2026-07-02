package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.LocalDate

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

data class HrvSample(
    val time: Instant,
    val rmssdMs: Double,
    val source: String,
)

data class HeartRateSummary(
    val date: LocalDate,
    val avgBpm: Long,
    val minBpm: Long,
    val maxBpm: Long,
)

data class DailyRestingHR(
    val date: LocalDate,
    val bpm: Long,
)

data class DailyHrv(
    val date: LocalDate,
    val rmssdMs: Double,
)
