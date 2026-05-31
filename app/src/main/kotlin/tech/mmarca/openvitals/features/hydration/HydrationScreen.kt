package tech.mmarca.openvitals.features.hydration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.insights.BaselineValue
import tech.mmarca.openvitals.core.insights.CrossMetricValue
import tech.mmarca.openvitals.core.insights.DataValueKind
import tech.mmarca.openvitals.core.insights.crossMetricInsight
import tech.mmarca.openvitals.core.insights.dataConfidence
import tech.mmarca.openvitals.core.insights.periodComparison
import tech.mmarca.openvitals.core.insights.personalBaselineInsight
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.DailyHydration
import tech.mmarca.openvitals.data.model.HydrationEntry
import tech.mmarca.openvitals.data.model.WeightEntry
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.ui.components.CrossMetricInsightCard
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.PeriodHistoryChart
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.theme.HydrationColor
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HydrationScreen(
    viewModel: HydrationViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditHydrationEntry: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate)

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
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                )
            }
            chartDaySelection.selectedDate?.let { selectedDate ->
                hydrationEntries(
                    entries = state.hydrationEntries.filter {
                        it.startTime.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
                    },
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    titleDate = selectedDate,
                    onEditHydrationEntry = onEditHydrationEntry,
                )
            }
            item {
                HydrationDataConfidence(
                    data = state.dailyHydration,
                    period = period,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
                    period = period,
                    unitFormatter = unitFormatter,
                    selectedRange = state.selectedRange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            hydrationWeightInsight(
                hydration = state.dailyHydration,
                weightEntries = state.crossWeightEntries,
            )
            hydrationEntries(
                entries = state.hydrationEntries,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEditHydrationEntry = onEditHydrationEntry,
            )
        }
    }
}

@Composable
private fun HydrationDataConfidence(
    data: List<DailyHydration>,
    period: DatePeriod,
    modifier: Modifier = Modifier,
) {
    if (period.start == period.end) return

    val tracked = data.filter { it.liters > 0.0 }
    DataConfidenceCard(
        confidence = dataConfidence(
            period = period,
            trackedDates = tracked.map { it.date },
            sampleCount = tracked.size,
            valueKind = DataValueKind.AGGREGATED,
        ),
        accentColor = HydrationColor,
        modifier = modifier,
    )
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
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    selectedRange: TimeRange,
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
            previousPeriodInsightStat(
                comparison = periodComparison(
                    currentValue = state.totalLiters,
                    previousValue = state.previousDailyHydration.sumOf { it.liters },
                ),
                selectedRange = selectedRange,
                unitFormatter = unitFormatter,
                valueFormatter = { unitFormatter.hydration(it) },
                accentColor = HydrationColor,
            ),
        ) + personalBaselineInsightStats(
            insight = personalBaselineInsight(
                currentValue = state.averageLiters,
                values = state.baselineDailyHydration.map { BaselineValue(it.date, it.liters) },
                referenceDate = period.start.minusDays(1),
            ),
            unitFormatter = unitFormatter,
            valueFormatter = { unitFormatter.hydration(it) },
            accentColor = HydrationColor,
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
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
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
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = { unitFormatter.hydration(it).text },
    )
}

private fun LazyListScope.hydrationWeightInsight(
    hydration: List<DailyHydration>,
    weightEntries: List<WeightEntry>,
) {
    val insight = crossMetricInsight(
        primaryValues = hydration.map { CrossMetricValue(it.date, it.liters) },
        secondaryValues = weightFluctuationValues(weightEntries),
    ) ?: return

    item { SectionHeader(stringResource(R.string.section_cross_metric_insights)) }
    item {
        CrossMetricInsightCard(
            insight = insight,
            title = stringResource(R.string.cross_hydration_weight_title),
            positiveMessage = stringResource(R.string.cross_hydration_weight_positive),
            negativeMessage = stringResource(R.string.cross_hydration_weight_negative),
            neutralMessage = stringResource(R.string.cross_hydration_weight_neutral),
            accentColor = HydrationColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

private fun LazyListScope.hydrationEntries(
    entries: List<HydrationEntry>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    titleDate: LocalDate? = null,
    onEditHydrationEntry: (String) -> Unit = {},
) {
    val sortedEntries = entries.sortedByDescending { it.startTime }
    item {
        PaginatedEntryList(
            title = entryListTitle(titleDate, dateTimeFormatterProvider),
            entries = sortedEntries,
        ) { entry, rowModifier ->
            HydrationEntryRow(
                entry = entry,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEdit = if (entry.isOpenVitalsEntry && entry.id.isNotBlank()) {
                    { onEditHydrationEntry(entry.id) }
                } else {
                    null
                },
                modifier = rowModifier,
            )
        }
    }
}

@Composable
private fun HydrationEntryRow(
    entry: HydrationEntry,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = entry.startTime.atZone(zone)
    val end = entry.endTime.atZone(zone)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateTimeFormatterProvider.mediumDate().format(start),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${dateTimeFormatterProvider.shortTime().format(start)} - ${dateTimeFormatterProvider.shortTime().format(end)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SourceChip(source = entry.source)
            }
            Text(
                text = unitFormatter.hydration(entry.liters).text,
                style = MaterialTheme.typography.titleMedium,
                color = HydrationColor,
            )
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.cd_edit_entry),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun weightFluctuationValues(entries: List<WeightEntry>): List<CrossMetricValue> {
    val zone = ZoneId.systemDefault()
    val dailyWeights = entries
        .groupBy { it.time.atZone(zone).toLocalDate() }
        .mapValues { (_, dayEntries) -> dayEntries.map { it.weightKg }.average() }
        .toSortedMap()

    var previousWeight: Double? = null
    return dailyWeights.mapNotNull { (date, weight) ->
        val previous = previousWeight
        previousWeight = weight
        previous?.let { CrossMetricValue(date, abs(weight - it)) }
    }
}
