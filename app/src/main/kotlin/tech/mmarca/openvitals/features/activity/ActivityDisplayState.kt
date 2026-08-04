package tech.mmarca.openvitals.features.activity

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.DailyGoalProgress
import tech.mmarca.openvitals.domain.insights.DailyGoalValue
import tech.mmarca.openvitals.domain.insights.PeriodComparison
import java.time.Instant
import java.time.LocalDate

@Immutable
data class ActivityDisplayState(
    val selectedPeriod: DatePeriod = DatePeriod(LocalDate.now(), LocalDate.now()),
    val metric: ActivityMetricDisplay = ActivityMetricDisplay(),
)

@Immutable
data class ActivityMetricDisplay(
    val hasData: Boolean = false,
    val values: List<Double> = emptyList(),
    val goalValues: List<DailyGoalValue> = emptyList(),
    val trackedDates: List<LocalDate> = emptyList(),
    val sampleCount: Int = 0,
    val previousTotal: Double = 0.0,
    val baselineValues: List<BaselineValue> = emptyList(),
    val activeDays: Int = 0,
    val goalProgress: DailyGoalProgress? = null,
    val periodComparison: PeriodComparison? = null,
    val baselineCurrentValue: Double = 0.0,
    val intradayPoints: List<ActivityIntradayPoint> = emptyList(),
    val dayTotal: Double = 0.0,
)

@Immutable
data class ActivityIntradayPoint(
    val time: Instant,
    val value: Double,
)
