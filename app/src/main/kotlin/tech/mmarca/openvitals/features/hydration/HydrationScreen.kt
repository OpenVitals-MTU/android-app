package tech.mmarca.openvitals.features.hydration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.DailyHydration
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.PeriodHistoryChart
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.HydrationColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HydrationScreen(
    viewModel: HydrationViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val state by viewModel.uiState.collectAsState()

    MetricDetailScaffold(
        isLoading = state.isLoading,
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        error = state.error,
        onRefresh = viewModel::load,
        onSelectRange = viewModel::selectRange,
        onPreviousPeriod = viewModel::previousPeriod,
        onNextPeriod = viewModel::nextPeriod,
        onSelectDate = viewModel::selectDate,
    ) { period ->
        if (state.dailyHydration.none { it.liters > 0.0 }) {
            item {
                MetricCardPlaceholder(
                    title = stringResource(R.string.metric_hydration),
                    icon = Icons.Outlined.LocalDrink,
                    accentColor = HydrationColor,
                    message = stringResource(R.string.message_no_hydration_period),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        } else {
            item {
                HydrationSummary(
                    state = state,
                    period = period,
                    unitFormatter = unitFormatter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                HydrationHistoryChart(
                    data = state.dailyHydration,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                HydrationGoalCard(
                    state = state,
                    unitFormatter = unitFormatter,
                    onDecreaseGoal = viewModel::decreaseDailyGoal,
                    onIncreaseGoal = viewModel::increaseDailyGoal,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item { SectionHeader(stringResource(R.string.section_statistics)) }
            item {
                HydrationStatistics(
                    state = state,
                    unitFormatter = unitFormatter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun HydrationSummary(
    state: HydrationUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val title = if (state.selectedRange == TimeRange.DAY) {
        stringResource(R.string.metric_hydration)
    } else {
        stringResource(R.string.metric_total_hydration)
    }
    val subtitle = if (state.selectedRange == TimeRange.DAY) {
        localizedPeriodTitle(state.selectedRange, period)
    } else {
        stringResource(R.string.summary_daily_average, unitFormatter.hydration(state.averageLiters).text)
    }
    val total = unitFormatter.hydration(state.totalLiters)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            title = title,
            value = total.value,
            unit = total.unit,
            icon = Icons.Outlined.LocalDrink,
            accentColor = HydrationColor,
            subtitle = subtitle,
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            title = stringResource(R.string.metric_logged_days),
            value = unitFormatter.count(state.dailyHydration.count { it.liters > 0.0 }),
            unit = stringResource(R.string.unit_days),
            icon = Icons.Outlined.LocalDrink,
            accentColor = HydrationColor,
            subtitle = stringResource(R.string.summary_days_in_range, unitFormatter.count(state.dailyHydration.size)),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HydrationStatistics(
    state: HydrationUiState,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val total = unitFormatter.hydration(state.totalLiters)
    val average = unitFormatter.hydration(state.averageLiters)
    val bestDay = unitFormatter.hydration(state.bestDayLiters)
    InsightStatGrid(
        stats = listOf(
            InsightStat(
                title = stringResource(R.string.stat_goal_streak),
                value = unitFormatter.count(state.currentGoalStreakDays),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.LocalFireDepartment,
                accentColor = HydrationColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_goals_met),
                value = unitFormatter.count(state.goalMetDays),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.CheckCircle,
                accentColor = HydrationColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_longest_goal_streak),
                value = unitFormatter.count(state.longestGoalStreakDays),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.CalendarMonth,
                accentColor = HydrationColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_success_rate),
                value = unitFormatter.count(state.goalSuccessRatePercent),
                unit = stringResource(R.string.unit_percent_symbol),
                icon = Icons.Outlined.Star,
                accentColor = HydrationColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_daily_average),
                value = average.value,
                unit = average.unit,
                icon = Icons.Outlined.Star,
                accentColor = HydrationColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_total_intake),
                value = total.value,
                unit = total.unit,
                icon = Icons.Outlined.LocalDrink,
                accentColor = HydrationColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_best_day),
                value = bestDay.value,
                unit = bestDay.unit,
                icon = Icons.Outlined.CalendarMonth,
                accentColor = HydrationColor,
            ),
        ),
        modifier = modifier,
    )
}

@Composable
private fun HydrationGoalCard(
    state: HydrationUiState,
    unitFormatter: UnitFormatter,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val goal = unitFormatter.hydration(state.dailyGoalLiters)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalDrink,
                    contentDescription = null,
                    tint = HydrationColor,
                    modifier = Modifier.size(22.dp),
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.hydration_daily_goal),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(
                            R.string.hydration_goal_progress,
                            state.goalMetDays,
                            state.trackedDays,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDecreaseGoal) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = stringResource(R.string.cd_decrease_hydration_goal),
                    )
                }
                IconButton(onClick = onIncreaseGoal) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.cd_increase_hydration_goal),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = goal.value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = goal.unit,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, bottom = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun HydrationHistoryChart(
    data: List<DailyHydration>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val values = data.map { PeriodChartValue(date = it.date, value = it.liters) }
    val summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${
        unitFormatter.hydration(data.sumOf { it.liters }).text
    }"

    PeriodHistoryChart(
        title = stringResource(R.string.metric_hydration_trend),
        values = values,
        selectedRange = selectedRange,
        period = period,
        accentColor = HydrationColor.copy(alpha = 0.85f),
        summaryText = summaryText,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
        valueFormatter = { unitFormatter.hydration(it).text },
    )
}
