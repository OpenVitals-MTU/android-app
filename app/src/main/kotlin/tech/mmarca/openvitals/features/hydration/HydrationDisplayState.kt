package tech.mmarca.openvitals.features.hydration

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.CrossMetricInsight
import tech.mmarca.openvitals.domain.insights.PeriodComparison
import java.time.LocalDate

@Immutable
data class HydrationDisplayState(
    val selectedPeriod: DatePeriod = DatePeriod(LocalDate.now(), LocalDate.now()),
    val hasData: Boolean = false,
    val summary: HydrationPeriodSummary = HydrationPeriodSummary(),
    val periodComparison: PeriodComparison = PeriodComparison(0.0, 0.0),
    val previousTotalLiters: Double = 0.0,
    val baselineValues: List<BaselineValue> = emptyList(),
    val crossMetricInsight: CrossMetricInsight? = null,
    val trackedDates: List<LocalDate> = emptyList(),
    val sampleCount: Int = 0,
    val dayLiters: Double = 0.0,
)

@Immutable
data class HydrationPeriodSummary(
    val totalLiters: Double = 0.0,
    val trackedDays: Int = 0,
    val loggedDays: Int = 0,
    val averageLiters: Double = 0.0,
    val bestDayLiters: Double = 0.0,
    val goalMetDays: Int = 0,
    val goalSuccessRatePercent: Int = 0,
    val currentTrackedStreakDays: Int = 0,
    val currentGoalStreakDays: Int = 0,
    val longestGoalStreakDays: Int = 0,
)
