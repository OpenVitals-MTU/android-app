package tech.mmarca.openvitals.features.heart

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.PeriodComparison
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import java.time.LocalDate

@Immutable
data class HeartDisplayState(
    val selectedPeriod: DatePeriod = DatePeriod(LocalDate.now(), LocalDate.now()),
    val metric: HeartMetricDisplay = HeartMetricDisplay(),
)

@Immutable
data class HeartMetricDisplay(
    val hasData: Boolean = false,
    val hasDayHeartRateSamples: Boolean = false,
    val hasPeriodHeartRateSummaries: Boolean = false,
    val showDayHeartRateTimeline: Boolean = false,
    val sortedDailySummaries: List<HeartRateSummary> = emptyList(),
    val heartRateRangeSummary: LongRangeSummary? = null,
    val heartRateTrackedDates: List<LocalDate> = emptyList(),
    val heartRateSampleCount: Int = 0,
    val hasDayRestingRate: Boolean = false,
    val hasPeriodRestingRate: Boolean = false,
    val restingRangeSummary: LongRangeSummary? = null,
    val restingDayComparison: PeriodComparison? = null,
    val restingPeriodAverageBpm: Long? = null,
    val restingBaselineValues: List<BaselineValue> = emptyList(),
    val hasDayHrv: Boolean = false,
    val hasPeriodHrv: Boolean = false,
    val hrvRangeSummary: DoubleRangeSummary? = null,
    val hrvDayComparison: PeriodComparison? = null,
    val hrvBaselineValues: List<BaselineValue> = emptyList(),
    val hasVitalsEntries: Boolean = false,
    val vitalsSampleCount: Int = 0,
    val vitalsTrackedDates: List<LocalDate> = emptyList(),
)
