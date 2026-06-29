package tech.mmarca.openvitals.features.mindfulness

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.CrossMetricInsight
import tech.mmarca.openvitals.domain.insights.DailyGoalProgress
import tech.mmarca.openvitals.domain.insights.PeriodComparison
import tech.mmarca.openvitals.domain.insights.PersonalBaselineInsight
import java.time.LocalDate

@Immutable
data class MindfulnessDisplayState(
    val selectedPeriod: DatePeriod = DatePeriod(LocalDate.now(), LocalDate.now()),
    val hasData: Boolean = false,
    val summary: MindfulnessPeriodSummary = MindfulnessPeriodSummary(),
    val dailyMinutes: List<MindfulnessDayValue> = emptyList(),
    val goalProgress: DailyGoalProgress? = null,
    val periodComparison: PeriodComparison = PeriodComparison(0.0, 0.0),
    val previousTotalMs: Long = 0L,
    val baselineValues: List<BaselineValue> = emptyList(),
    val baselineInsight: PersonalBaselineInsight? = null,
    val crossMetricInsight: CrossMetricInsight? = null,
    val trackedDates: List<LocalDate> = emptyList(),
    val sampleCount: Int = 0,
)

@Immutable
data class MindfulnessDayValue(
    val date: LocalDate,
    val minutes: Double,
)

@Immutable
data class MindfulnessPeriodSummary(
    val totalMinutes: Long = 0L,
    val totalMs: Long = 0L,
    val sessionCount: Int = 0,
    val averageDurationMs: Long = 0L,
    val longestSessionMs: Long = 0L,
)
