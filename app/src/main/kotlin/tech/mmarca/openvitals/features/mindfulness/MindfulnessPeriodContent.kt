package tech.mmarca.openvitals.features.mindfulness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.model.MindfulnessSession
import tech.mmarca.openvitals.ui.components.ChartXAxisWithYAxis
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.CrossMetricInsightCard
import tech.mmarca.openvitals.ui.components.DailyGoalCard
import tech.mmarca.openvitals.ui.components.DailyGoalStatistics
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricBarChart
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricLinePlot
import tech.mmarca.openvitals.ui.components.MetricLinePlotPoint
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.theme.MindfulnessColor
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToLong

internal fun LazyListScope.mindfulnessPeriodContent(
    state: MindfulnessUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    hasNotificationPermission: Boolean,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSelectReminderTime: (LocalTime) -> Unit,
    onEditMindfulnessSession: (String) -> Unit,
    onDeleteMindfulnessSession: (String) -> Unit,
) {
    val display = state.display
    if (!display.hasData && !state.isLoading) {
        item {
            MetricCardPlaceholder(
                title = stringResource(R.string.metric_mindfulness),
                icon = Icons.Outlined.SelfImprovement,
                accentColor = MindfulnessColor,
                message = stringResource(R.string.message_no_mindfulness_period),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        mindfulnessGoalAndReminderItems(
            state = state,
            display = display,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            hasNotificationPermission = hasNotificationPermission,
            onDecreaseGoal = onDecreaseGoal,
            onIncreaseGoal = onIncreaseGoal,
            onToggleReminders = onToggleReminders,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onSelectReminderTime = onSelectReminderTime,
        )
        return
    }

    if (display.hasData) {
        item {
            MindfulnessSummary(
                display = display,
                subtitle = localizedPeriodTitle(state.selectedRange, period),
                unitFormatter = unitFormatter,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item {
            if (state.selectedRange == TimeRange.DAY) {
                MindfulnessIntradayChartCard(
                    selectedDate = state.selectedDate,
                    sessions = state.sessions,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                )
            } else {
                val chartValues = display.dailyMinutes.map {
                    PeriodChartValue(date = it.date, value = it.minutes)
                }
                MetricBarChart(
                    title = stringResource(R.string.metric_mindfulness),
                    values = chartValues,
                    selectedRange = state.selectedRange,
                    period = period,
                    accentColor = MindfulnessColor,
                    summaryValue = unitFormatter.minutes(display.summary.totalMinutes).text,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                    valueFormatter = { unitFormatter.minutes(it.roundToLong()).text },
                )
            }
        }
        chartDaySelection.selectedDate?.let { selectedDate ->
            item {
                val zone = ZoneId.systemDefault()
                PaginatedEntryList(
                    title = entryListTitle(selectedDate, dateTimeFormatterProvider),
                    entries = state.sessions
                        .filter { it.startTime.atZone(zone).toLocalDate() == selectedDate }
                        .sortedByDescending { it.startTime },
                ) { session, rowModifier ->
                    MindfulnessSessionRow(
                        session = session,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        onEdit = session.editAction(onEditMindfulnessSession),
                        onDelete = session.deleteAction(onDeleteMindfulnessSession),
                        modifier = rowModifier,
                    )
                }
            }
        }
        mindfulnessDataConfidence(
            display = display,
            sessions = state.sessions,
            period = period,
        )
        mindfulnessGoalAndReminderItems(
            state = state,
            display = display,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            hasNotificationPermission = hasNotificationPermission,
            onDecreaseGoal = onDecreaseGoal,
            onIncreaseGoal = onIncreaseGoal,
            onToggleReminders = onToggleReminders,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onSelectReminderTime = onSelectReminderTime,
        )
        mindfulnessStatistics(
            display = display,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
            includeHeader = false,
        )
        mindfulnessSleepInsight(display = display)
        item {
            PaginatedEntryList(
                title = stringResource(R.string.section_sessions),
                entries = state.sessions.sortedByDescending { it.startTime },
            ) { session, rowModifier ->
                MindfulnessSessionRow(
                    session = session,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onEdit = session.editAction(onEditMindfulnessSession),
                    onDelete = session.deleteAction(onDeleteMindfulnessSession),
                    modifier = rowModifier,
                )
            }
        }
    }
}

@Composable
private fun MindfulnessIntradayChartCard(
    selectedDate: LocalDate,
    sessions: List<MindfulnessSession>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val dayStart = selectedDate.atStartOfDay(zone).toInstant()
    val isToday = selectedDate == LocalDate.now()
    val chartEnd = if (isToday) Instant.now() else selectedDate.plusDays(1).atStartOfDay(zone).toInstant()
    val elapsedToday = Duration.between(dayStart, chartEnd).toMillis().coerceAtLeast(1L)
    val points = sessions.cumulativeMindfulnessPoints()
    val totalMinutes = points.lastOrNull()?.second ?: 0.0
    val maxValue = totalMinutes.coerceAtLeast(1.0)
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()

    OpenVitalsCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = unitFormatter.minutes(totalMinutes.roundToLong()).text,
                style = MaterialTheme.typography.headlineMedium,
                color = MindfulnessColor,
            )
            Text(
                text = if (isToday) {
                    stringResource(R.string.summary_today, stringResource(R.string.metric_mindfulness))
                } else {
                    stringResource(
                        R.string.summary_on_date,
                        stringResource(R.string.metric_mindfulness),
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
                    add(MetricLinePlotPoint(xFraction = 1f, value = totalMinutes))
                }

                MetricLinePlot(
                    points = chartPoints,
                    minValue = 0.0,
                    maxValue = maxValue,
                    accentColor = MindfulnessColor,
                    chartHeight = 180.dp,
                    valueFormatter = { unitFormatter.minutes(it.roundToLong()).text },
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
                        stringResource(R.string.summary_empty_today, stringResource(R.string.metric_mindfulness))
                    } else {
                        stringResource(R.string.summary_empty_day, stringResource(R.string.metric_mindfulness))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun List<MindfulnessSession>.cumulativeMindfulnessPoints(): List<Pair<Instant, Double>> {
    var cumulativeMinutes = 0.0
    return sortedBy { it.endTime }
        .map { session ->
            cumulativeMinutes += session.durationMs.coerceAtLeast(0L).toDouble() / 60_000.0
            session.endTime to cumulativeMinutes
        }
}

private fun LazyListScope.mindfulnessDataConfidence(
    display: MindfulnessDisplayState,
    sessions: List<MindfulnessSession>,
    period: DatePeriod,
) {
    if (period.start == period.end) return

    item {
        DataConfidenceCard(
            confidence = dataConfidence(
                period = period,
                trackedDates = display.trackedDates,
                sampleCount = display.sampleCount,
                sources = sessions.map { it.source },
                valueKind = DataValueKind.MEASURED,
            ),
            accentColor = MindfulnessColor,
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.mindfulnessGoalAndReminderItems(
    state: MindfulnessUiState,
    display: MindfulnessDisplayState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    hasNotificationPermission: Boolean,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSelectReminderTime: (LocalTime) -> Unit,
) {
    val progress = display.goalProgress ?: return
    item {
        DailyGoalCard(
            goal = unitFormatter.minutes(state.dailyGoalMinutes.roundToLong()),
            progress = progress,
            icon = Icons.Outlined.SelfImprovement,
            accentColor = MindfulnessColor,
            onDecreaseGoal = onDecreaseGoal,
            onIncreaseGoal = onIncreaseGoal,
            modifier = metricModifier(),
        )
    }
    item {
        MindfulnessReminderCard(
            config = state.reminderConfig,
            hasNotificationPermission = hasNotificationPermission,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onToggleReminders = onToggleReminders,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onSelectReminderTime = onSelectReminderTime,
            modifier = metricModifier(),
        )
    }
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        DailyGoalStatistics(
            progress = progress,
            averageGap = unitFormatter.minutes(progress.averageGapToGoal.roundToLong()),
            unitFormatter = unitFormatter,
            icon = Icons.Outlined.SelfImprovement,
            accentColor = MindfulnessColor,
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.mindfulnessStatistics(
    display: MindfulnessDisplayState,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
    includeHeader: Boolean = true,
) {
    if (includeHeader) {
        item { SectionHeader(stringResource(R.string.section_statistics)) }
    }
    val summary = display.summary
    item {
        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.stat_total),
                    value = unitFormatter.duration(summary.totalMs),
                    unit = "",
                    icon = Icons.Outlined.SelfImprovement,
                    accentColor = MindfulnessColor,
                ),
                InsightStat(
                    title = stringResource(R.string.section_sessions),
                    value = unitFormatter.count(summary.sessionCount),
                    unit = "",
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = MindfulnessColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_average_duration),
                    value = unitFormatter.duration(summary.averageDurationMs),
                    unit = "",
                    icon = Icons.Outlined.Star,
                    accentColor = MindfulnessColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_longest_session),
                    value = unitFormatter.duration(summary.longestSessionMs),
                    unit = "",
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = MindfulnessColor,
                ),
                previousPeriodInsightStat(
                    comparison = display.periodComparison,
                    selectedRange = selectedRange,
                    unitFormatter = unitFormatter,
                    valueFormatter = { DisplayValue(unitFormatter.duration(it.roundToLong()), "") },
                    accentColor = MindfulnessColor,
                ),
            ) + personalBaselineInsightStats(
                insight = display.baselineInsight,
                unitFormatter = unitFormatter,
                valueFormatter = { unitFormatter.minutes(it.roundToLong()) },
                accentColor = MindfulnessColor,
            ),
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.mindfulnessSleepInsight(
    display: MindfulnessDisplayState,
) {
    val insight = display.crossMetricInsight ?: return

    item { SectionHeader(stringResource(R.string.section_cross_metric_insights)) }
    item {
        CrossMetricInsightCard(
            insight = insight,
            title = stringResource(R.string.cross_mindfulness_sleep_title),
            positiveMessage = stringResource(R.string.cross_mindfulness_sleep_positive),
            negativeMessage = stringResource(R.string.cross_mindfulness_sleep_negative),
            neutralMessage = stringResource(R.string.cross_mindfulness_sleep_neutral),
            accentColor = MindfulnessColor,
            modifier = metricModifier(),
        )
    }
}

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
