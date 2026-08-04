package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.MetricDetailSectionBuilder
import tech.mmarca.openvitals.ui.components.renderOrderedMetricDetailSections
import java.time.LocalDate

internal typealias ActivityMetricSectionContext = MetricDetailSectionContext

internal fun LazyListScope.orderedActivityMetricSections(
    context: MetricDetailSectionContext,
    builder: MetricDetailSectionBuilder.() -> Unit,
) {
    renderOrderedMetricDetailSections(context, builder)
}

internal data class ActivityMetricOrderedContentSpec(
    val state: ActivityUiState,
    val display: ActivityMetricDisplay,
    val period: DatePeriod,
    val unitFormatter: UnitFormatter,
    val dateTimeFormatterProvider: DateTimeFormatterProvider,
    val chartDaySelection: ChartDaySelection,
    val sectionContext: MetricDetailSectionContext,
    val metric: ActivityMetric,
    val accentColor: Color,
    val goalIcon: ImageVector,
    val goalFormatter: @Composable (Double) -> DisplayValue,
    val statisticsIcon: ImageVector,
    val comparisonValueFormatter: @Composable (Double) -> DisplayValue,
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
    val display = spec.display
    val goalProgress = display.goalProgress ?: return
    val periodComparison = display.periodComparison ?: return
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
            androidx.compose.foundation.layout.Column {
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
                    activeDays = display.activeDays,
                    comparison = periodComparison,
                    selectedRange = spec.state.selectedRange,
                    comparisonValueFormatter = spec.comparisonValueFormatter,
                    baselineCurrentValue = display.baselineCurrentValue,
                    baselineValues = display.baselineValues,
                    icon = spec.statisticsIcon,
                    accentColor = spec.accentColor,
                )
            }
        }
        section(MetricDetailSectionId.DATA_CONFIDENCE, spec.period.start != spec.period.end) {
            ActivityDataConfidenceCard(
                period = spec.period,
                trackedDates = display.trackedDates,
                sampleCount = display.sampleCount,
                accentColor = spec.accentColor,
            )
        }
        section(MetricDetailSectionId.ENTRIES) {
            spec.entriesContent()
        }
    }
}
