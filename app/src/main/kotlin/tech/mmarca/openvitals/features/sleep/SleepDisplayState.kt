package tech.mmarca.openvitals.features.sleep

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.insights.CrossMetricValue
import tech.mmarca.openvitals.domain.insights.SleepScoreConfidence
import tech.mmarca.openvitals.domain.insights.SleepScoreEstimate
import tech.mmarca.openvitals.domain.model.SleepData
import java.time.LocalDate

@Immutable
data class SleepDisplayState(
    val dailySessions: List<SleepData> = emptyList(),
    val dailySummary: SleepData? = null,
    val selectedPeriod: DatePeriod = DatePeriod(LocalDate.now(), LocalDate.now()),
    val previousPeriod: DatePeriod = DatePeriod(LocalDate.now(), LocalDate.now()),
    val baselinePeriod: DatePeriod = DatePeriod(LocalDate.now(), LocalDate.now()),
    val durationPoints: List<SleepDurationPoint> = emptyList(),
    val previousDurationPoints: List<SleepDurationPoint> = emptyList(),
    val baselineDurationPoints: List<SleepDurationPoint> = emptyList(),
    val overviewDays: List<SleepOverviewDay> = emptyList(),
    val overviewSummary: SleepOverviewSummary = SleepOverviewSummary(),
    val crossMetricHrvValues: List<CrossMetricValue> = emptyList(),
)

@Immutable
data class SleepDurationPoint(
    val date: LocalDate,
    val hours: Double,
)

@Immutable
data class SleepOverviewDay(
    val date: LocalDate,
    val sessions: List<SleepData> = emptyList(),
    val sleepScore: SleepScoreEstimate = SleepScoreEstimate.NoData,
)

@Immutable
data class SleepOverviewSummary(
    val dates: List<LocalDate> = emptyList(),
    val sleepScore: Int? = null,
    val sleepScoreConfidence: SleepScoreConfidence = SleepScoreConfidence.NO_DATA,
    val sleepDurationMs: Long = 0L,
    val schedule: SleepOverviewSchedule? = null,
    val remDurationMs: Long = 0L,
    val deepDurationMs: Long = 0L,
    val sleepEfficiencyPercent: Double? = null,
    val remValues: List<Double> = emptyList(),
    val deepValues: List<Double> = emptyList(),
    val efficiencyValues: List<Double> = emptyList(),
)

@Immutable
data class SleepOverviewSchedule(
    val startMinute: Int,
    val endMinute: Int,
)
