package tech.mmarca.openvitals.features.hydration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.ChartXAxisWithYAxis
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.CrossMetricInsightCard
import tech.mmarca.openvitals.ui.components.MetricBarChart
import tech.mmarca.openvitals.ui.components.MetricLinePlot
import tech.mmarca.openvitals.ui.components.MetricLinePlotPoint
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.renderOrderedMetricDetailSections
import tech.mmarca.openvitals.ui.theme.HydrationColor
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

internal fun LazyListScope.hydrationPeriodContent(
    sectionContext: MetricDetailSectionContext,
    state: HydrationUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    hasNotificationPermission: Boolean,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onDecreaseInterval: () -> Unit,
    onIncreaseInterval: () -> Unit,
    onSelectActiveStartTime: (LocalTime) -> Unit,
    onSelectActiveEndTime: (LocalTime) -> Unit,
    onEditHydrationEntry: (String) -> Unit,
    onDeleteHydrationEntry: (String) -> Unit,
) {
    val display = state.display
    val selectedDate = chartDaySelection.selectedDate
    val selectedEntries = selectedDate?.let { date ->
        state.hydrationEntries.filter {
            it.startTime.atZone(ZoneId.systemDefault()).toLocalDate() == date
        }
    }.orEmpty()

    if (!display.hasData) {
        renderOrderedMetricDetailSections(sectionContext) {
            section(MetricDetailSectionId.ACTIVITY_SUMMARY) {
                MetricCardPlaceholder(
                    title = stringResource(R.string.metric_hydration),
                    icon = Icons.Outlined.LocalDrink,
                    accentColor = HydrationColor,
                    message = stringResource(R.string.message_no_hydration_period),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            section(MetricDetailSectionId.DAILY_GOAL) {
                HydrationGoalAndReminderContent(
                    state = state,
                    display = display,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    hasNotificationPermission = hasNotificationPermission,
                    onDecreaseGoal = onDecreaseGoal,
                    onIncreaseGoal = onIncreaseGoal,
                    onToggleReminders = onToggleReminders,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onDecreaseInterval = onDecreaseInterval,
                    onIncreaseInterval = onIncreaseInterval,
                    onSelectActiveStartTime = onSelectActiveStartTime,
                    onSelectActiveEndTime = onSelectActiveEndTime,
                )
            }
        }
        return
    }

    renderOrderedMetricDetailSections(sectionContext) {
        section(MetricDetailSectionId.ACTIVITY_SUMMARY) {
            HydrationSummary(
                state = state,
                display = display,
                period = period,
                unitFormatter = unitFormatter,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        section(
            if (state.selectedRange == TimeRange.DAY) {
                MetricDetailSectionId.INTRADAY_CHART
            } else {
                MetricDetailSectionId.PERIOD_CHART
            },
        ) {
            HydrationTrendContent(
                state = state,
                display = display,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
            )
        }
        section(MetricDetailSectionId.SELECTED_DAY_ENTRIES, selectedDate != null && selectedEntries.isNotEmpty()) {
            selectedDate?.let { date ->
                HydrationEntriesContent(
                    entries = selectedEntries,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    titleDate = date,
                    onEditHydrationEntry = onEditHydrationEntry,
                    onDeleteHydrationEntry = onDeleteHydrationEntry,
                )
            }
        }
        section(MetricDetailSectionId.DATA_CONFIDENCE, period.start != period.end) {
            HydrationDataConfidence(
                display = display,
                period = period,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        section(MetricDetailSectionId.DAILY_GOAL) {
            HydrationGoalAndReminderContent(
                state = state,
                display = display,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                hasNotificationPermission = hasNotificationPermission,
                onDecreaseGoal = onDecreaseGoal,
                onIncreaseGoal = onIncreaseGoal,
                onToggleReminders = onToggleReminders,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onDecreaseInterval = onDecreaseInterval,
                onIncreaseInterval = onIncreaseInterval,
                onSelectActiveStartTime = onSelectActiveStartTime,
                onSelectActiveEndTime = onSelectActiveEndTime,
            )
        }
        section(MetricDetailSectionId.STATISTICS) {
            SectionHeader(stringResource(R.string.section_statistics))
            HydrationStatistics(
                display = display,
                period = period,
                unitFormatter = unitFormatter,
                selectedRange = state.selectedRange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        section(MetricDetailSectionId.CROSS_METRIC_INSIGHTS, display.crossMetricInsight != null) {
            HydrationWeightInsightContent(display = display)
        }
        section(MetricDetailSectionId.ENTRIES, state.hydrationEntries.isNotEmpty()) {
            HydrationEntriesContent(
                entries = state.hydrationEntries,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEditHydrationEntry = onEditHydrationEntry,
                onDeleteHydrationEntry = onDeleteHydrationEntry,
            )
        }
    }
}

@Composable
private fun HydrationTrendContent(
    state: HydrationUiState,
    display: HydrationDisplayState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    selectedDate: java.time.LocalDate?,
    onDateSelected: (java.time.LocalDate) -> Unit,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    if (state.selectedRange == TimeRange.DAY) {
        HydrationIntradayChartCard(
            selectedDate = state.selectedDate,
            entries = state.hydrationEntries,
            dailyGoalLiters = state.dailyGoalLiters,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            modifier = modifier,
        )
    } else {
        val useWeekAccent = state.selectedRange == TimeRange.WEEK
        MetricBarChart(
            title = stringResource(R.string.metric_hydration_trend),
            data = state.dailyHydration,
            selectedRange = state.selectedRange,
            period = period,
            accentColor = if (useWeekAccent) HydrationWeekChartColor else HydrationColor,
            accentAlpha = if (useWeekAccent) 1f else 0.85f,
            summaryValue = unitFormatter.hydration(display.summary.totalLiters).text,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            date = { it.date },
            value = { it.liters },
            modifier = modifier.then(
                if (state.selectedRange == TimeRange.WEEK) {
                    Modifier.testTag("hydration_week_period_content")
                } else {
                    Modifier
                },
            ),
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            valueFormatter = { unitFormatter.hydration(it).text },
        )
    }
}

@Composable
private fun HydrationIntradayChartCard(
    selectedDate: LocalDate,
    entries: List<HydrationEntry>,
    dailyGoalLiters: Double,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val dayStart = selectedDate.atStartOfDay(zone).toInstant()
    val isToday = selectedDate == LocalDate.now()
    val chartEnd = if (isToday) Instant.now() else selectedDate.plusDays(1).atStartOfDay(zone).toInstant()
    val elapsedToday = Duration.between(dayStart, chartEnd).toMillis().coerceAtLeast(1L)
    val points = entries.cumulativeHydrationPoints()
    val totalLiters = points.lastOrNull()?.second ?: 0.0
    val maxValue = maxOf(totalLiters, dailyGoalLiters, 0.5)
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()

    OpenVitalsCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = unitFormatter.hydration(totalLiters).text,
                style = MaterialTheme.typography.headlineMedium,
                color = HydrationColor,
            )
            Text(
                text = if (isToday) {
                    stringResource(R.string.summary_today, stringResource(R.string.metric_hydration))
                } else {
                    stringResource(
                        R.string.summary_on_date,
                        stringResource(R.string.metric_hydration),
                        dateFormatter.format(selectedDate),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            if (points.isNotEmpty()) {
                val chartPoints = buildList {
                    add(MetricLinePlotPoint(xFraction = 0f, value = 0.0))
                    points.forEach { point ->
                        val elapsed = Duration.between(dayStart, point.first)
                            .toMillis()
                            .coerceIn(0L, elapsedToday)
                        add(
                            MetricLinePlotPoint(
                                xFraction = elapsed.toFloat() / elapsedToday.toFloat(),
                                value = point.second,
                            )
                        )
                    }
                    add(MetricLinePlotPoint(xFraction = 1f, value = totalLiters))
                }

                MetricLinePlot(
                    points = chartPoints,
                    minValue = 0.0,
                    maxValue = maxValue,
                    accentColor = HydrationColor,
                    chartHeight = 180.dp,
                    valueFormatter = { unitFormatter.hydration(it).text },
                    lineStrokeWidth = 3.dp,
                )
                Spacer(Modifier.height(8.dp))
                ChartXAxisWithYAxis {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        listOf(
                            "00:00",
                            "06:00",
                            "12:00",
                            "18:00",
                            if (isToday) stringResource(R.string.summary_now) else "24:00",
                        ).forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        R.string.summary_last_update,
                        timeFormatter.format(points.last().first.atZone(zone)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = if (isToday) {
                        stringResource(R.string.summary_empty_today, stringResource(R.string.metric_hydration))
                    } else {
                        stringResource(R.string.summary_empty_day, stringResource(R.string.metric_hydration))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun List<HydrationEntry>.cumulativeHydrationPoints(): List<Pair<Instant, Double>> {
    var cumulativeLiters = 0.0
    return sortedBy { it.endTime }
        .map { entry ->
            cumulativeLiters += entry.liters
            entry.endTime to cumulativeLiters
        }
}

@Composable
private fun HydrationGoalAndReminderContent(
    state: HydrationUiState,
    display: HydrationDisplayState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    hasNotificationPermission: Boolean,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onDecreaseInterval: () -> Unit,
    onIncreaseInterval: () -> Unit,
    onSelectActiveStartTime: (LocalTime) -> Unit,
    onSelectActiveEndTime: (LocalTime) -> Unit,
) {
    HydrationGoalCard(
        display = display,
        dailyGoalLiters = state.dailyGoalLiters,
        unitFormatter = unitFormatter,
        onDecreaseGoal = onDecreaseGoal,
        onIncreaseGoal = onIncreaseGoal,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    HydrationReminderCard(
        config = state.reminderConfig,
        hasNotificationPermission = hasNotificationPermission,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        onToggleReminders = onToggleReminders,
        onRequestNotificationPermission = onRequestNotificationPermission,
        onDecreaseInterval = onDecreaseInterval,
        onIncreaseInterval = onIncreaseInterval,
        onSelectActiveStartTime = onSelectActiveStartTime,
        onSelectActiveEndTime = onSelectActiveEndTime,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun HydrationWeightInsightContent(display: HydrationDisplayState) {
    val insight = display.crossMetricInsight ?: return

    SectionHeader(stringResource(R.string.section_cross_metric_insights))
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
