package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.DailyGoalValue
import tech.mmarca.openvitals.domain.insights.dailyGoalProgress
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.MetricDetailSectionBuilder
import tech.mmarca.openvitals.ui.components.MetricDetailSectionListState
import tech.mmarca.openvitals.ui.components.orderedMetricDetailSections
import java.time.LocalDate

internal data class ActivityMetricSectionContext(
    val listState: MetricDetailSectionListState,
    val order: List<MetricDetailSectionId>,
    val isEditingSections: Boolean,
    val onMoveSectionToTarget: (MetricDetailSectionId, MetricDetailSectionId) -> Unit,
    val onMoveSection: (MetricDetailSectionId, Int) -> Unit,
)

internal fun LazyListScope.orderedActivityMetricSections(
    context: ActivityMetricSectionContext,
    builder: MetricDetailSectionBuilder.() -> Unit,
) {
    orderedMetricDetailSections(
        listState = context.listState,
        order = context.order,
        isEditingSections = context.isEditingSections,
        onMoveSectionToTarget = context.onMoveSectionToTarget,
        onMoveSection = context.onMoveSection,
        builder = builder,
    )
}

internal data class ActivityMetricOrderedContentSpec(
    val state: ActivityUiState,
    val period: DatePeriod,
    val unitFormatter: UnitFormatter,
    val dateTimeFormatterProvider: DateTimeFormatterProvider,
    val chartDaySelection: ChartDaySelection,
    val sectionContext: ActivityMetricSectionContext,
    val metric: ActivityMetric,
    val accentColor: Color,
    val goalIcon: ImageVector,
    val goalFormatter: @Composable (Double) -> DisplayValue,
    val goalValues: List<DailyGoalValue>,
    val trackedDates: List<LocalDate>,
    val sampleCount: Int,
    val values: List<Double>,
    val previousTotal: Double,
    val baselineValues: List<BaselineValue>,
    val statisticsIcon: ImageVector,
    val comparisonValueFormatter: @Composable (Double) -> DisplayValue,
    val activeDays: Int,
    val onDecreaseGoal: () -> Unit,
    val onIncreaseGoal: () -> Unit,
    val intradayChart: @Composable () -> Unit,
    val periodChart: @Composable () -> Unit,
    val selectedDayEntriesContent: @Composable (LocalDate) -> Unit,
    val entriesContent: @Composable () -> Unit,
    val statisticsTotal: @Composable () -> DisplayValue,
    val statisticsAverage: @Composable () -> DisplayValue,
    val statisticsBest: @Composable () -> DisplayValue,
)

internal fun LazyListScope.renderActivityMetricOrderedContent(
    spec: ActivityMetricOrderedContentSpec,
) {
    val goalProgress = dailyGoalProgress(
        values = spec.goalValues,
        period = spec.period,
        target = spec.state.dailyGoal,
        direction = spec.metric.dailyGoalKey.direction,
    )
    val selectedDate = spec.chartDaySelection.selectedDate
    orderedActivityMetricSections(spec.sectionContext) {
        section(MetricDetailSectionId.INTRADAY_CHART, spec.state.selectedRange == TimeRange.DAY) {
            spec.intradayChart()
        }
        section(MetricDetailSectionId.PERIOD_CHART, spec.state.selectedRange != TimeRange.DAY) {
            spec.periodChart()
        }
        section(MetricDetailSectionId.SELECTED_DAY_ENTRIES, selectedDate != null) {
            selectedDate?.let { spec.selectedDayEntriesContent(it) }
        }
        section(MetricDetailSectionId.DAILY_GOAL) {
            ActivityDailyGoalCard(
                goal = spec.goalFormatter(spec.state.dailyGoal),
                progress = goalProgress,
                icon = spec.goalIcon,
                accentColor = spec.accentColor,
                onDecreaseGoal = spec.onDecreaseGoal,
                onIncreaseGoal = spec.onIncreaseGoal,
            )
        }
        section(MetricDetailSectionId.STATISTICS) {
            Column {
                ActivityGoalStatisticsContent(
                    progress = goalProgress,
                    averageGap = spec.goalFormatter(goalProgress.averageGapToGoal),
                    unitFormatter = spec.unitFormatter,
                    icon = spec.goalIcon,
                    accentColor = spec.accentColor,
                )
                ActivityPeriodStatisticsGrid(
                    unitFormatter = spec.unitFormatter,
                    period = spec.period,
                    total = spec.statisticsTotal,
                    average = spec.statisticsAverage,
                    best = spec.statisticsBest,
                    activeDays = spec.activeDays,
                    comparison = periodComparison(
                        currentValue = spec.values.sum(),
                        previousValue = spec.previousTotal,
                    ),
                    selectedRange = spec.state.selectedRange,
                    comparisonValueFormatter = spec.comparisonValueFormatter,
                    baselineCurrentValue = averageOrZero(spec.values.sum(), spec.activeDays),
                    baselineValues = spec.baselineValues,
                    icon = spec.statisticsIcon,
                    accentColor = spec.accentColor,
                )
            }
        }
        section(MetricDetailSectionId.DATA_CONFIDENCE, spec.period.start != spec.period.end) {
            ActivityDataConfidenceCard(
                period = spec.period,
                trackedDates = spec.trackedDates,
                sampleCount = spec.sampleCount,
                accentColor = spec.accentColor,
            )
        }
        section(MetricDetailSectionId.ENTRIES) {
            spec.entriesContent()
        }
    }
}
