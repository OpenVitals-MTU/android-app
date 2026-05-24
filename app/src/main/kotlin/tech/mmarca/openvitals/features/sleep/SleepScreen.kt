package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.insights.BaselineValue
import tech.mmarca.openvitals.core.insights.CrossMetricValue
import tech.mmarca.openvitals.core.insights.DailyGoalValue
import tech.mmarca.openvitals.core.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.insights.crossMetricInsight
import tech.mmarca.openvitals.core.insights.dailyGoalProgress
import tech.mmarca.openvitals.core.insights.periodComparison
import tech.mmarca.openvitals.core.insights.personalBaselineInsight
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.baselinePeriodBefore
import tech.mmarca.openvitals.core.period.periodFor
import tech.mmarca.openvitals.core.period.previousPeriodFor
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.model.dailySleepSummary
import tech.mmarca.openvitals.data.model.sleepSessionsForRange
import tech.mmarca.openvitals.ui.components.CrossMetricInsightCard
import tech.mmarca.openvitals.ui.components.DailyGoalCard
import tech.mmarca.openvitals.ui.components.DailyGoalStatistics
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.theme.SleepColor
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenSleepSession: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val dailySessions = remember(state.sessions, state.selectedDate, state.sleepRangeMode) {
        sleepSessionsForRange(
            sessions = state.sessions,
            selectedDate = state.selectedDate,
            sleepRangeMode = state.sleepRangeMode,
        )
    }
    val dailySummary = remember(state.sessions, state.selectedDate, state.sleepRangeMode) {
        dailySleepSummary(
            sessions = state.sessions,
            selectedDate = state.selectedDate,
            sleepRangeMode = state.sleepRangeMode,
        )
    }
    val selectedPeriod = remember(state.selectedRange, state.selectedDate) {
        periodFor(state.selectedRange, state.selectedDate)
    }
    val previousPeriod = remember(state.selectedRange, state.selectedDate) {
        previousPeriodFor(state.selectedRange, state.selectedDate)
    }
    val durationPoints = remember(state.sessions, selectedPeriod, state.sleepRangeMode) {
        sleepDurationPoints(
            sessions = state.sessions,
            period = selectedPeriod,
            sleepRangeMode = state.sleepRangeMode,
        )
    }
    val previousDurationPoints = remember(state.previousSessions, previousPeriod, state.sleepRangeMode) {
        sleepDurationPoints(
            sessions = state.previousSessions,
            period = previousPeriod,
            sleepRangeMode = state.sleepRangeMode,
        )
    }
    val baselinePeriod = remember(selectedPeriod) {
        baselinePeriodBefore(selectedPeriod)
    }
    val baselineDurationPoints = remember(state.baselineSessions, baselinePeriod, state.sleepRangeMode) {
        sleepDurationPoints(
            sessions = state.baselineSessions,
            period = baselinePeriod,
            sleepRangeMode = state.sleepRangeMode,
        )
    }

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
        when {
            state.selectedRange == TimeRange.DAY && dailySummary != null -> {
                item {
                    val summary = dailySummary
                    SleepSessionTimelineCard(
                        session = summary,
                        selectedDate = state.selectedDate,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        timeRangeText = dailySleepTimeRangeText(
                            sessions = dailySessions,
                            selectedDate = state.selectedDate,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                        ),
                        onClick = dailySessions.singleOrNull()?.let { session ->
                            { onOpenSleepSession(session.id) }
                        },
                        preserveTimelineGaps = dailySessions.size > 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                sleepGoal(
                    state = state,
                    period = period,
                    durationPoints = durationPoints,
                    unitFormatter = unitFormatter,
                    onDecreaseGoal = viewModel::decreaseDailyGoal,
                    onIncreaseGoal = viewModel::increaseDailyGoal,
                )
                sleepStatistics(
                    durationPoints = durationPoints,
                    previousDurationPoints = previousDurationPoints,
                    baselineDurationPoints = baselineDurationPoints,
                    period = period,
                    selectedRange = state.selectedRange,
                    unitFormatter = unitFormatter,
                    includeHeader = false,
                )
                sleepHrvInsight(
                    durationPoints = durationPoints,
                    hrvValues = state.crossDailyHrv.map { CrossMetricValue(it.date, it.rmssdMs) },
                )

                if (dailySessions.size > 1) {
                    item { SectionHeader(stringResource(R.string.section_sleep_sessions)) }
                    items(dailySessions.sortedByDescending { it.endTime }) { session ->
                        SleepSessionItem(
                            session = session,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            onClick = { onOpenSleepSession(session.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            state.selectedRange != TimeRange.DAY && state.sessions.isNotEmpty() -> {
                item {
                    SleepDurationChart(
                        sessions = state.sessions,
                        selectedRange = state.selectedRange,
                        period = period,
                        sleepRangeMode = state.sleepRangeMode,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        durationPoints = durationPoints,
                    )
                }
                sleepGoal(
                    state = state,
                    period = period,
                    durationPoints = durationPoints,
                    unitFormatter = unitFormatter,
                    onDecreaseGoal = viewModel::decreaseDailyGoal,
                    onIncreaseGoal = viewModel::increaseDailyGoal,
                )
                sleepStatistics(
                    durationPoints = durationPoints,
                    previousDurationPoints = previousDurationPoints,
                    baselineDurationPoints = baselineDurationPoints,
                    period = period,
                    selectedRange = state.selectedRange,
                    unitFormatter = unitFormatter,
                    includeHeader = false,
                )
                sleepHrvInsight(
                    durationPoints = durationPoints,
                    hrvValues = state.crossDailyHrv.map { CrossMetricValue(it.date, it.rmssdMs) },
                )

                item { SectionHeader(stringResource(R.string.section_sleep_sessions)) }
                items(state.sessions.sortedByDescending { it.endTime }) { session ->
                    SleepSessionItem(
                        session = session,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        onClick = { onOpenSleepSession(session.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            !state.isLoading -> {
                item {
                    Text(
                        text = if (state.selectedRange == TimeRange.DAY) {
                            stringResource(R.string.message_no_sleep_day_selected)
                        } else {
                            stringResource(R.string.message_no_sleep_period)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

private fun dailySleepTimeRangeText(
    sessions: List<SleepData>,
    selectedDate: LocalDate,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): String {
    val zone = ZoneId.systemDefault()
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()
    val ranges = sessions
        .sortedWith(compareBy<SleepData> { it.startTime }.thenBy { it.endTime })
        .joinToString(" | ") { session ->
            val start = session.startTime.atZone(zone)
            val end = session.endTime.atZone(zone)
            "${timeFormatter.format(start)} - ${timeFormatter.format(end)}"
        }

    return "${dateFormatter.format(selectedDate)}  ·  $ranges"
}

private fun LazyListScope.sleepGoal(
    state: SleepUiState,
    period: DatePeriod,
    durationPoints: List<SleepDurationPoint>,
    unitFormatter: UnitFormatter,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val goalKey = MetricDailyGoalKey.SLEEP_HOURS
    val progress = dailyGoalProgress(
        values = durationPoints.map { DailyGoalValue(date = it.date, value = it.hours) },
        period = period,
        target = state.dailyGoalHours,
        direction = goalKey.direction,
    )
    item {
        DailyGoalCard(
            goal = sleepHoursDisplay(state.dailyGoalHours, unitFormatter),
            progress = progress,
            icon = Icons.Outlined.Bed,
            accentColor = SleepColor,
            onDecreaseGoal = onDecreaseGoal,
            onIncreaseGoal = onIncreaseGoal,
            modifier = metricModifier(),
        )
    }
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        DailyGoalStatistics(
            progress = progress,
            averageGap = sleepHoursDisplay(progress.averageGapToGoal, unitFormatter),
            unitFormatter = unitFormatter,
            icon = Icons.Outlined.Bed,
            accentColor = SleepColor,
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.sleepStatistics(
    durationPoints: List<SleepDurationPoint>,
    previousDurationPoints: List<SleepDurationPoint>,
    baselineDurationPoints: List<SleepDurationPoint>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
    includeHeader: Boolean = true,
) {
    if (includeHeader) {
        item { SectionHeader(stringResource(R.string.section_statistics)) }
    }
    item {
        val nights = durationPoints.filter { it.hours > 0.0 }
        val totalHours = nights.sumOf { it.hours }
        val averageHours = nights.takeIf { it.isNotEmpty() }?.map { it.hours }?.average() ?: 0.0
        val longestHours = nights.maxOfOrNull { it.hours } ?: 0.0
        val previousNights = previousDurationPoints.filter { it.hours > 0.0 }
        val previousAverageHours = previousNights.takeIf { it.isNotEmpty() }?.map { it.hours }?.average() ?: 0.0

        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.stat_total),
                    value = unitFormatter.duration((totalHours * 3_600_000).roundToLong()),
                    unit = "",
                    icon = Icons.Outlined.Bed,
                    accentColor = SleepColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_daily_average),
                    value = unitFormatter.duration((averageHours * 3_600_000).roundToLong()),
                    unit = "",
                    icon = Icons.Outlined.Star,
                    accentColor = SleepColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_longest_sleep),
                    value = unitFormatter.duration((longestHours * 3_600_000).roundToLong()),
                    unit = "",
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = SleepColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_nights_logged),
                    value = unitFormatter.count(nights.size),
                    unit = stringResource(R.string.unit_nights),
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = SleepColor,
                ),
                previousPeriodInsightStat(
                    comparison = periodComparison(
                        currentValue = averageHours,
                        previousValue = previousAverageHours,
                    ),
                    selectedRange = selectedRange,
                    unitFormatter = unitFormatter,
                    valueFormatter = { sleepHoursDisplay(it, unitFormatter) },
                    accentColor = SleepColor,
                ),
            ) + personalBaselineInsightStats(
                insight = personalBaselineInsight(
                    currentValue = averageHours,
                    values = baselineDurationPoints.map { BaselineValue(it.date, it.hours) },
                    referenceDate = period.start.minusDays(1),
                ),
                unitFormatter = unitFormatter,
                valueFormatter = { sleepHoursDisplay(it, unitFormatter) },
                accentColor = SleepColor,
            ),
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.sleepHrvInsight(
    durationPoints: List<SleepDurationPoint>,
    hrvValues: List<CrossMetricValue>,
) {
    val insight = crossMetricInsight(
        primaryValues = durationPoints.map { CrossMetricValue(it.date, it.hours) },
        secondaryValues = hrvValues,
    ) ?: return

    item { SectionHeader(stringResource(R.string.section_cross_metric_insights)) }
    item {
        CrossMetricInsightCard(
            insight = insight,
            title = stringResource(R.string.cross_sleep_hrv_title),
            positiveMessage = stringResource(R.string.cross_sleep_hrv_positive),
            negativeMessage = stringResource(R.string.cross_sleep_hrv_negative),
            neutralMessage = stringResource(R.string.cross_sleep_hrv_neutral),
            accentColor = SleepColor,
            modifier = metricModifier(),
        )
    }
}

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)

private fun sleepHoursDisplay(hours: Double, unitFormatter: UnitFormatter): DisplayValue =
    DisplayValue(unitFormatter.duration((hours * 3_600_000).roundToLong()), "")
