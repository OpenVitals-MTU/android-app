package tech.mmarca.openvitals.features.activity

import tech.mmarca.openvitals.ui.components.OpenVitalsCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Accessible
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.DailyGoalProgress
import tech.mmarca.openvitals.domain.insights.DailyGoalValue
import tech.mmarca.openvitals.domain.insights.dailyGoalProgress
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.DailyGoalCard
import tech.mmarca.openvitals.ui.components.DailyGoalStatistics
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.theme.ActiveCaloriesColor
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.ElevationColor
import tech.mmarca.openvitals.ui.theme.FloorsColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import tech.mmarca.openvitals.ui.theme.WheelchairPushesColor
import java.time.LocalDate
import kotlin.math.roundToLong

internal fun LazyListScope.activityDataConfidence(
    period: DatePeriod,
    trackedDates: Collection<LocalDate>,
    sampleCount: Int,
    accentColor: Color,
) {
    if (period.start == period.end) return

    item {
        ActivityDataConfidenceCard(
            period = period,
            trackedDates = trackedDates,
            sampleCount = sampleCount,
            accentColor = accentColor,
        )
    }
}

@Composable
internal fun ActivityDataConfidenceCard(
    period: DatePeriod,
    trackedDates: Collection<LocalDate>,
    sampleCount: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    DataConfidenceCard(
        confidence = dataConfidence(
            period = period,
            trackedDates = trackedDates,
            sampleCount = sampleCount,
            valueKind = DataValueKind.AGGREGATED,
        ),
        accentColor = accentColor,
        modifier = modifier.then(activityMetricModifier()),
    )
}

internal fun LazyListScope.activityGoal(
    state: ActivityUiState,
    period: DatePeriod,
    values: List<DailyGoalValue>,
    unitFormatter: UnitFormatter,
    icon: ImageVector,
    accentColor: Color,
    direction: tech.mmarca.openvitals.domain.insights.DailyGoalDirection,
    goalFormatter: @Composable (Double) -> DisplayValue,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val progress = dailyGoalProgress(
        values = values,
        period = period,
        target = state.dailyGoal,
        direction = direction,
    )
    item {
        ActivityDailyGoalCard(
            goal = goalFormatter(state.dailyGoal),
            progress = progress,
            icon = icon,
            accentColor = accentColor,
            onDecreaseGoal = onDecreaseGoal,
            onIncreaseGoal = onIncreaseGoal,
        )
    }
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        ActivityGoalStatisticsContent(
            progress = progress,
            averageGap = goalFormatter(progress.averageGapToGoal),
            unitFormatter = unitFormatter,
            icon = icon,
            accentColor = accentColor,
        )
    }
}

@Composable
internal fun ActivityDailyGoalCard(
    goal: DisplayValue,
    progress: DailyGoalProgress,
    icon: ImageVector,
    accentColor: Color,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DailyGoalCard(
        goal = goal,
        progress = progress,
        icon = icon,
        accentColor = accentColor,
        onDecreaseGoal = onDecreaseGoal,
        onIncreaseGoal = onIncreaseGoal,
        modifier = modifier.then(activityMetricModifier()),
    )
}

@Composable
internal fun ActivityGoalStatisticsContent(
    progress: DailyGoalProgress,
    averageGap: DisplayValue,
    unitFormatter: UnitFormatter,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    includeHeader: Boolean = true,
) {
    if (includeHeader) {
        SectionHeader(stringResource(R.string.section_statistics))
    }
    DailyGoalStatistics(
        progress = progress,
        averageGap = averageGap,
        unitFormatter = unitFormatter,
        icon = icon,
        accentColor = accentColor,
        modifier = modifier.then(activityMetricModifier()),
    )
}

internal fun LazyListScope.activityStatistics(
    unitFormatter: UnitFormatter,
    period: DatePeriod,
    total: @Composable () -> DisplayValue,
    average: @Composable () -> DisplayValue,
    best: @Composable () -> DisplayValue,
    activeDays: Int,
    comparison: tech.mmarca.openvitals.domain.insights.PeriodComparison,
    selectedRange: TimeRange,
    comparisonValueFormatter: @Composable (Double) -> DisplayValue,
    baselineCurrentValue: Double,
    baselineValues: List<BaselineValue>,
    icon: ImageVector,
    accentColor: Color,
    includeHeader: Boolean = true,
) {
    if (includeHeader) {
        item { SectionHeader(stringResource(R.string.section_statistics)) }
    }
    item {
        ActivityPeriodStatisticsGrid(
            unitFormatter = unitFormatter,
            period = period,
            total = total,
            average = average,
            best = best,
            activeDays = activeDays,
            comparison = comparison,
            selectedRange = selectedRange,
            comparisonValueFormatter = comparisonValueFormatter,
            baselineCurrentValue = baselineCurrentValue,
            baselineValues = baselineValues,
            icon = icon,
            accentColor = accentColor,
        )
    }
}

@Composable
internal fun ActivityPeriodStatisticsGrid(
    unitFormatter: UnitFormatter,
    period: DatePeriod,
    total: @Composable () -> DisplayValue,
    average: @Composable () -> DisplayValue,
    best: @Composable () -> DisplayValue,
    activeDays: Int,
    comparison: tech.mmarca.openvitals.domain.insights.PeriodComparison,
    selectedRange: TimeRange,
    comparisonValueFormatter: @Composable (Double) -> DisplayValue,
    baselineCurrentValue: Double,
    baselineValues: List<BaselineValue>,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val totalValue = total()
    val averageValue = average()
    val bestValue = best()
    InsightStatGrid(
        stats = listOf(
            InsightStat(
                title = stringResource(R.string.stat_total),
                value = totalValue.value,
                unit = totalValue.unit,
                icon = icon,
                accentColor = accentColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_daily_average),
                value = averageValue.value,
                unit = averageValue.unit,
                icon = Icons.Outlined.Star,
                accentColor = accentColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_best_day),
                value = bestValue.value,
                unit = bestValue.unit,
                icon = Icons.Outlined.CalendarMonth,
                accentColor = accentColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_active_days),
                value = unitFormatter.count(activeDays),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.CheckCircle,
                accentColor = accentColor,
            ),
            previousPeriodInsightStat(
                comparison = comparison,
                selectedRange = selectedRange,
                unitFormatter = unitFormatter,
                valueFormatter = comparisonValueFormatter,
                accentColor = accentColor,
            ),
        ) + personalBaselineInsightStats(
            insight = personalBaselineInsight(
                currentValue = baselineCurrentValue,
                values = baselineValues,
                referenceDate = period.start.minusDays(1),
            ),
            unitFormatter = unitFormatter,
            valueFormatter = comparisonValueFormatter,
            accentColor = accentColor,
        ),
        modifier = modifier.then(activityMetricModifier()),
    )
}

internal fun LazyListScope.noMetricData(
    titleRes: Int,
    messageRes: Int,
    icon: ImageVector,
    accentColor: Color,
) {
    item {
        MetricCardPlaceholder(
            title = stringResource(titleRes),
            icon = icon,
            accentColor = accentColor,
            message = stringResource(messageRes),
            modifier = activityMetricModifier(),
        )
    }
}

internal fun <T> LazyListScope.activityDailyEntries(
    entries: List<T>,
    date: (T) -> LocalDate,
    value: @Composable (T) -> DisplayValue,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    accentColor: Color,
    titleDate: LocalDate? = null,
) {
    item {
        ActivityDailyEntriesContent(
            entries = entries,
            date = date,
            value = value,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            accentColor = accentColor,
            titleDate = titleDate,
        )
    }
}

@Composable
internal fun <T> ActivityDailyEntriesContent(
    entries: List<T>,
    date: (T) -> LocalDate,
    value: @Composable (T) -> DisplayValue,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    accentColor: Color,
    titleDate: LocalDate? = null,
    modifier: Modifier = Modifier,
) {
    val sortedEntries = entries.sortedByDescending(date)
    PaginatedEntryList(
        title = entryListTitle(titleDate, dateTimeFormatterProvider),
        entries = sortedEntries,
        modifier = modifier.then(activityMetricModifier()),
    ) { entry, rowModifier ->
        ActivityDailyEntryRow(
            date = date(entry),
            value = value(entry),
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            accentColor = accentColor,
            modifier = rowModifier,
        )
    }
}

@Composable
internal fun ActivityDailyEntryRow(
    date: LocalDate,
    value: DisplayValue,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(
        modifier = modifier,

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateTimeFormatterProvider.mediumDate().format(date),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = value.text,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
            )
        }
    }
}

internal fun activityMetricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)

internal fun averageOrZero(total: Double, activeDays: Int): Double =
    activeDays.takeIf { it > 0 }?.let { total / it } ?: 0.0
