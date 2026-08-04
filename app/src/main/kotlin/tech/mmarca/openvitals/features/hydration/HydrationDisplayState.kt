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
    /** How much of the period the goal was actually met on, clamped to 0..1. */
    val goalProgress: Double = 0.0,
)

@Immutable
data class HydrationPeriodSummary(
    val totalLiters: Double = 0.0,
    val trackedDays: Int = 0,
    val loggedDays: Int = 0,
    /**
     * Days of the period that have actually happened — the whole period, or the part of it up
     * to today. The denominator a person means when they ask "how did I do this week".
     */
    val elapsedDays: Int = 0,
    val averageLiters: Double = 0.0,
    val bestDayLiters: Double = 0.0,
    val goalMetDays: Int = 0,
    val goalSuccessRatePercent: Int = 0,
    val currentTrackedStreakDays: Int = 0,
    val currentGoalStreakDays: Int = 0,
    val longestGoalStreakDays: Int = 0,
)
