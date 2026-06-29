package tech.mmarca.openvitals.domain.query

import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary

data class HeartPeriodData(
    val daySamples: List<HeartRateSample> = emptyList(),
    val previousDaySamples: List<HeartRateSample> = emptyList(),
    val dailySummaries: List<HeartRateSummary> = emptyList(),
    val previousDailySummaries: List<HeartRateSummary> = emptyList(),
    val baselineDailySummaries: List<HeartRateSummary> = emptyList(),
    val dayRestingBpm: Long? = null,
    val previousDayRestingBpm: Long? = null,
    val dayHrvMs: Double? = null,
    val previousDayHrvMs: Double? = null,
    val dailyRestingHR: List<DailyRestingHR> = emptyList(),
    val previousDailyRestingHR: List<DailyRestingHR> = emptyList(),
    val baselineDailyRestingHR: List<DailyRestingHR> = emptyList(),
    val dailyHrv: List<DailyHrv> = emptyList(),
    val previousDailyHrv: List<DailyHrv> = emptyList(),
    val baselineDailyHrv: List<DailyHrv> = emptyList(),
)
