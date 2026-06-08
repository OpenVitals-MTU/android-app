package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.metadata.Metadata
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.insights.BaselineValue
import tech.mmarca.openvitals.core.insights.CrossMetricValue
import tech.mmarca.openvitals.core.insights.DataValueKind
import tech.mmarca.openvitals.core.insights.DailyGoalValue
import tech.mmarca.openvitals.core.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.insights.SleepScoreConfidence
import tech.mmarca.openvitals.core.insights.SleepScoreEstimate
import tech.mmarca.openvitals.core.insights.calculateSleepScoreForDate
import tech.mmarca.openvitals.core.insights.crossMetricInsight
import tech.mmarca.openvitals.core.insights.dailyGoalProgress
import tech.mmarca.openvitals.core.insights.dataConfidence
import tech.mmarca.openvitals.core.insights.periodComparison
import tech.mmarca.openvitals.core.insights.personalBaselineInsight
import tech.mmarca.openvitals.core.insights.sleepTargetInterpretation
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.baselinePeriodBefore
import tech.mmarca.openvitals.core.period.displayPeriodFor
import tech.mmarca.openvitals.core.period.previousPeriodFor
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.model.SleepStage
import tech.mmarca.openvitals.data.model.dailySleepSummary
import tech.mmarca.openvitals.data.model.sleepDurationMsFromStages
import tech.mmarca.openvitals.data.model.sleepSessionsForRange
import tech.mmarca.openvitals.ui.components.CrossMetricInsightCard
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.DailyGoalCard
import tech.mmarca.openvitals.ui.components.DailyGoalStatistics
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.MetricInterpretationCard
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.theme.SleepColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MinutesPerDay = 24 * 60
private val SleepOverviewTopCardHeight = 124.dp
private val SleepOverviewMetricCardHeight = 112.dp
private val SleepOverviewChartWidth = 168.dp
private val SleepOverviewChartHeight = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenSleepSession: (String) -> Unit,
    onOpenSleepScore: (() -> Unit)? = null,
    onOpenSleepEfficiency: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate)
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
    val selectedPeriod = remember(state.selectedRange, state.selectedDate, state.weekPeriodMode) {
        displayPeriodFor(
            range = state.selectedRange,
            anchorDate = state.selectedDate,
            weekPeriodMode = state.weekPeriodMode,
        )
    }
    val previousPeriod = remember(state.selectedRange, state.selectedDate, state.weekPeriodMode) {
        previousPeriodFor(
            range = state.selectedRange,
            anchorDate = state.selectedDate,
            weekPeriodMode = state.weekPeriodMode,
        )
    }
    val durationPoints = remember(state.sessions, selectedPeriod, state.sleepRangeMode) {
        sleepDurationPoints(
            sessions = state.sessions,
            period = selectedPeriod,
            sleepRangeMode = state.sleepRangeMode,
        )
    }
    val sleepScoreSessions = remember(state.sessions, state.baselineSessions) {
        (state.baselineSessions + state.sessions).distinctBy { it.id }
    }
    val overviewDays = remember(state.sessions, sleepScoreSessions, selectedPeriod, state.sleepRangeMode) {
        sleepOverviewDays(
            sessions = state.sessions,
            scoreSessions = sleepScoreSessions,
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
        weekPeriodMode = state.weekPeriodMode,
    ) { period ->
        if (!state.isLoading || state.sessions.isNotEmpty()) {
            sleepOverview(
                days = overviewDays,
                selectedRange = state.selectedRange,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenSleepScore = onOpenSleepScore,
                onOpenSleepEfficiency = onOpenSleepEfficiency,
            )
        }

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
                sleepDataConfidence(
                    sessions = dailySessions,
                    durationPoints = durationPoints,
                    period = period,
                )
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
                sleepTargetContext(
                    durationPoints = durationPoints,
                    targetHours = state.dailyGoalHours,
                    unitFormatter = unitFormatter,
                )
                sleepHrvInsight(
                    durationPoints = durationPoints,
                    hrvValues = state.crossDailyHrv.map { CrossMetricValue(it.date, it.rmssdMs) },
                )

                if (dailySessions.size > 1) {
                    item {
                        PaginatedEntryList(
                            title = stringResource(R.string.section_sleep_sessions),
                            entries = dailySessions.sortedByDescending { it.endTime },
                        ) { session, rowModifier ->
                            SleepSessionItem(
                                session = session,
                                unitFormatter = unitFormatter,
                                dateTimeFormatterProvider = dateTimeFormatterProvider,
                                onClick = { onOpenSleepSession(session.id) },
                                modifier = rowModifier,
                            )
                        }
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
                        selectedDate = chartDaySelection.selectedDate,
                        onDateSelected = chartDaySelection.onDateSelected,
                    )
                }
                chartDaySelection.selectedDate?.let { selectedDate ->
                    item {
                        PaginatedEntryList(
                            title = entryListTitle(selectedDate, dateTimeFormatterProvider),
                            entries = sleepSessionsForRange(
                                sessions = state.sessions,
                                selectedDate = selectedDate,
                                sleepRangeMode = state.sleepRangeMode,
                            ).sortedByDescending { it.endTime },
                        ) { session, rowModifier ->
                            SleepSessionItem(
                                session = session,
                                unitFormatter = unitFormatter,
                                dateTimeFormatterProvider = dateTimeFormatterProvider,
                                onClick = { onOpenSleepSession(session.id) },
                                modifier = rowModifier,
                            )
                        }
                    }
                }
                sleepDataConfidence(
                    sessions = state.sessions,
                    durationPoints = durationPoints,
                    period = period,
                )
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
                sleepTargetContext(
                    durationPoints = durationPoints,
                    targetHours = state.dailyGoalHours,
                    unitFormatter = unitFormatter,
                )
                sleepHrvInsight(
                    durationPoints = durationPoints,
                    hrvValues = state.crossDailyHrv.map { CrossMetricValue(it.date, it.rmssdMs) },
                )

                item {
                    PaginatedEntryList(
                        title = stringResource(R.string.section_sleep_sessions),
                        entries = state.sessions.sortedByDescending { it.endTime },
                    ) { session, rowModifier ->
                        SleepSessionItem(
                            session = session,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            onClick = { onOpenSleepSession(session.id) },
                            modifier = rowModifier,
                        )
                    }
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

private fun LazyListScope.sleepOverview(
    days: List<SleepOverviewDay>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenSleepScore: (() -> Unit)?,
    onOpenSleepEfficiency: (() -> Unit)?,
) {
    val summary = days.toSleepOverviewSummary()

    item {
        SleepOverviewTopCards(
            summary = summary,
            selectedRange = selectedRange,
            period = period,
            unitFormatter = unitFormatter,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            onOpenSleepScore = onOpenSleepScore,
        )
    }
    item {
        SectionHeader(
            text = stringResource(R.string.activities_key_metrics),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    item {
        SleepOverviewMetricCard(
            title = stringResource(R.string.recovery_sleep_schedule),
            value = sleepScheduleOrNoData(summary, dateTimeFormatterProvider),
            subtitle = localizedPeriodTitle(selectedRange, period),
            icon = Icons.Outlined.Schedule,
            chartValues = emptyList(),
            dates = summary.dates,
            selectedRange = selectedRange,
            valueEmphasis = SleepOverviewValueEmphasis.Small,
            modifier = metricModifier(),
            chartContent = null,
        )
    }
    item {
        SleepOverviewMetricCard(
            title = stringResource(R.string.recovery_rem_sleep),
            value = durationOrNoData(summary.remDurationMs, unitFormatter),
            subtitle = sleepOverviewAverageSubtitle(selectedRange, period),
            icon = Icons.Outlined.DarkMode,
            chartValues = summary.remValues,
            dates = summary.dates,
            selectedRange = selectedRange,
            modifier = metricModifier(),
        )
    }
    item {
        SleepOverviewMetricCard(
            title = stringResource(R.string.recovery_deep_sleep),
            value = durationOrNoData(summary.deepDurationMs, unitFormatter),
            subtitle = sleepOverviewAverageSubtitle(selectedRange, period),
            icon = Icons.Outlined.Bed,
            chartValues = summary.deepValues,
            dates = summary.dates,
            selectedRange = selectedRange,
            modifier = metricModifier(),
        )
    }
    item {
        SleepOverviewMetricCard(
            title = stringResource(R.string.recovery_sleep_efficiency),
            value = sleepEfficiencyOrNoData(summary, unitFormatter),
            subtitle = sleepOverviewAverageSubtitle(selectedRange, period),
            icon = Icons.Outlined.Speed,
            chartValues = summary.efficiencyValues,
            dates = summary.dates,
            selectedRange = selectedRange,
            modifier = metricModifier(),
            onClick = onOpenSleepEfficiency,
        )
    }
}

@Composable
private fun SleepOverviewTopCards(
    summary: SleepOverviewSummary,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
    onOpenSleepScore: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SleepOverviewTopCard(
            title = stringResource(R.string.recovery_sleep_score),
            value = sleepScoreOrNoData(summary, unitFormatter),
            subtitle = sleepScoreConfidenceLabel(summary.sleepScoreConfidence),
            icon = Icons.Outlined.DarkMode,
            onClick = onOpenSleepScore,
            modifier = Modifier.weight(1f),
        )
        SleepOverviewTopCard(
            title = stringResource(R.string.recovery_sleep_duration),
            value = durationOrNoData(summary.sleepDurationMs, unitFormatter),
            subtitle = sleepOverviewAverageSubtitle(selectedRange, period),
            icon = Icons.Outlined.Bed,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SleepOverviewTopCard(
    title: String,
    value: DisplayValue,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .height(SleepOverviewTopCardHeight)
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(SleepColor.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SleepColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                SleepOverviewValue(
                    value = value,
                    emphasis = SleepOverviewValueEmphasis.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SleepOverviewMetricCard(
    title: String,
    value: DisplayValue,
    subtitle: String,
    icon: ImageVector,
    chartValues: List<Double>,
    dates: List<LocalDate>,
    selectedRange: TimeRange,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    valueEmphasis: SleepOverviewValueEmphasis = SleepOverviewValueEmphasis.Large,
    chartContent: (@Composable () -> Unit)? = {
        SleepOverviewSparkline(
            values = chartValues,
            dates = dates,
            selectedRange = selectedRange,
            accentColor = SleepColor,
        )
    },
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(SleepOverviewMetricCardHeight)
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SleepColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(12.dp))
                SleepOverviewValue(
                    value = value,
                    emphasis = valueEmphasis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            chartContent?.invoke()
        }
    }
}

@Composable
private fun SleepOverviewSparkline(
    values: List<Double>,
    dates: List<LocalDate>,
    selectedRange: TimeRange,
    accentColor: Color,
) {
    val locale = LocalConfiguration.current.locales[0]
    val labelDates = sleepOverviewLabelDates(dates, selectedRange)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .width(SleepOverviewChartWidth)
                .height(SleepOverviewChartHeight),
        ) {
            val maxValue = values.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
            val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width / 2f
            val points = values.mapIndexed { index, value ->
                val yFraction = (value / maxValue).toFloat().coerceIn(0f, 1f)
                Offset(
                    x = if (values.size > 1) index * stepX else stepX,
                    y = size.height - (yFraction * (size.height * 0.72f)) - (size.height * 0.14f),
                )
            }
            drawLine(
                color = accentColor.copy(alpha = 0.22f),
                start = Offset(0f, size.height * 0.75f),
                end = Offset(size.width, size.height * 0.75f),
                strokeWidth = 2.dp.toPx(),
            )
            points.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = accentColor,
                    start = start,
                    end = end,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            points.forEach { point ->
                drawCircle(
                    color = accentColor,
                    radius = 3.dp.toPx(),
                    center = point,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.width(SleepOverviewChartWidth),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labelDates.forEach { date ->
                Text(
                    text = sleepOverviewSparklineLabel(date, selectedRange, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SleepOverviewValue(
    value: DisplayValue,
    emphasis: SleepOverviewValueEmphasis = SleepOverviewValueEmphasis.Large,
) {
    val valueStyle = when (emphasis) {
        SleepOverviewValueEmphasis.Large -> MaterialTheme.typography.headlineMedium
        SleepOverviewValueEmphasis.Medium -> MaterialTheme.typography.headlineSmall
        SleepOverviewValueEmphasis.Small -> MaterialTheme.typography.titleLarge
    }
    val unitStyle = when (emphasis) {
        SleepOverviewValueEmphasis.Large -> MaterialTheme.typography.bodyMedium
        SleepOverviewValueEmphasis.Medium,
        SleepOverviewValueEmphasis.Small -> MaterialTheme.typography.bodySmall
    }
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value.value,
            style = valueStyle,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (value.unit.isNotBlank()) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = value.unit,
                style = unitStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

@Composable
private fun sleepOverviewAverageSubtitle(
    selectedRange: TimeRange,
    period: DatePeriod,
): String =
    if (selectedRange == TimeRange.DAY) {
        localizedPeriodTitle(selectedRange, period)
    } else {
        stringResource(R.string.stat_daily_average)
    }

@Composable
private fun durationOrNoData(durationMs: Long, unitFormatter: UnitFormatter): DisplayValue =
    if (durationMs > 0L) {
        DisplayValue(unitFormatter.duration(durationMs), "")
    } else {
        DisplayValue(stringResource(R.string.no_data), "")
    }

@Composable
private fun sleepScheduleOrNoData(
    summary: SleepOverviewSummary,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): DisplayValue {
    val schedule = summary.schedule ?: return DisplayValue(stringResource(R.string.no_data), "")
    val formatter = dateTimeFormatterProvider.shortTime()
    val start = LocalTime.of(schedule.startMinute / 60, schedule.startMinute % 60)
    val end = LocalTime.of(schedule.endMinute / 60, schedule.endMinute % 60)
    return DisplayValue("${formatter.format(start)} - ${formatter.format(end)}", "")
}

@Composable
private fun sleepScoreOrNoData(summary: SleepOverviewSummary, unitFormatter: UnitFormatter): DisplayValue =
    summary.sleepScore?.let { score ->
        DisplayValue(unitFormatter.count(score), "")
    } ?: DisplayValue(stringResource(R.string.no_data), "")

@Composable
private fun sleepEfficiencyOrNoData(summary: SleepOverviewSummary, unitFormatter: UnitFormatter): DisplayValue =
    summary.sleepEfficiencyPercent?.let { efficiency ->
        unitFormatter.percent(efficiency, 0)
    } ?: DisplayValue(stringResource(R.string.no_data), "")

@Composable
private fun sleepScoreConfidenceLabel(confidence: SleepScoreConfidence): String =
    stringResource(
        when (confidence) {
            SleepScoreConfidence.HIGH -> R.string.sleep_score_confidence_high
            SleepScoreConfidence.MEDIUM -> R.string.sleep_score_confidence_medium
            SleepScoreConfidence.LOW -> R.string.sleep_score_confidence_low
            SleepScoreConfidence.NO_DATA -> R.string.sleep_score_confidence_no_data
        }
    )

private fun sleepOverviewDays(
    sessions: List<SleepData>,
    scoreSessions: List<SleepData>,
    period: DatePeriod,
    sleepRangeMode: SleepRangeMode,
): List<SleepOverviewDay> {
    val zone = ZoneId.systemDefault()
    val dates = datesInPeriod(period)
    val sessionsByDate = dates.associateWith { date ->
        sleepSessionsForRange(
            sessions = sessions,
            selectedDate = date,
            sleepRangeMode = sleepRangeMode,
            zone = zone,
        )
    }

    return dates.map { date ->
        SleepOverviewDay(
            date = date,
            sessions = sessionsByDate[date].orEmpty(),
            sleepScore = calculateSleepScoreForDate(
                selectedDate = date,
                sessions = scoreSessions,
                sleepRangeMode = sleepRangeMode,
                zone = zone,
            ),
        )
    }
}

private fun datesInPeriod(period: DatePeriod): List<LocalDate> =
    generateSequence(period.start) { current ->
        current.plusDays(1).takeUnless { it.isAfter(period.end) }
    }.toList()

private fun List<SleepOverviewDay>.toSleepOverviewSummary(): SleepOverviewSummary {
    val nights = filter { it.sleepDurationMs > 0L }
    val scoredDays = filter { it.sleepScore.confidence != SleepScoreConfidence.NO_DATA }
    val mainSessions = nights.mapNotNull { it.mainSleepSession }
    val averageByNight = size > 1
    val durationSource = if (averageByNight) nights else this

    return SleepOverviewSummary(
        dates = map { it.date },
        sleepScore = scoredDays
            .takeIf { it.isNotEmpty() }
            ?.map { it.sleepScore.score }
            ?.average()
            ?.roundToInt(),
        sleepScoreConfidence = scoredDays.sleepScoreConfidence(),
        sleepDurationMs = durationSource.averageDurationMs { it.sleepDurationMs },
        schedule = mainSessions.averageSchedule(),
        remDurationMs = durationSource.averageDurationMs { it.remDurationMs },
        deepDurationMs = durationSource.averageDurationMs { it.deepDurationMs },
        sleepEfficiencyPercent = scoredDays
            .takeIf { it.isNotEmpty() }
            ?.map { it.sleepScore.sleepEfficiencyPercent }
            ?.average(),
        remValues = map { it.remDurationMs.toDouble() },
        deepValues = map { it.deepDurationMs.toDouble() },
        efficiencyValues = map { day ->
            if (day.sleepScore.confidence == SleepScoreConfidence.NO_DATA) {
                0.0
            } else {
                day.sleepScore.sleepEfficiencyPercent
            }
        },
    )
}

private fun List<SleepOverviewDay>.averageDurationMs(selector: (SleepOverviewDay) -> Long): Long {
    val values = map(selector).filter { it > 0L }
    return values
        .takeIf { it.isNotEmpty() }
        ?.let { (it.sum().toDouble() / it.size).roundToLong() }
        ?: 0L
}

private fun List<SleepOverviewDay>.sleepScoreConfidence(): SleepScoreConfidence = when {
    isEmpty() -> SleepScoreConfidence.NO_DATA
    all { it.sleepScore.confidence == SleepScoreConfidence.HIGH } -> SleepScoreConfidence.HIGH
    any {
        it.sleepScore.confidence == SleepScoreConfidence.HIGH ||
            it.sleepScore.confidence == SleepScoreConfidence.MEDIUM
    } -> SleepScoreConfidence.MEDIUM
    else -> SleepScoreConfidence.LOW
}

private fun List<SleepData>.averageSchedule(): SleepOverviewSchedule? {
    if (isEmpty()) return null
    val zone = ZoneId.systemDefault()
    val startMinute = circularMeanMinutes(
        map { session -> session.startTime.atZone(zone).toLocalTime().toMinuteOfDay() },
    )
    val endMinute = circularMeanMinutes(
        map { session -> session.endTime.atZone(zone).toLocalTime().toMinuteOfDay() },
    )
    return SleepOverviewSchedule(startMinute = startMinute, endMinute = endMinute)
}

private fun List<SleepData>.mainSleepSession(): SleepData? =
    maxByOrNull { sleepDurationMsFromStages(it.stages, it.durationMs) }

private fun List<SleepData>.stageDurationMs(stageType: Int): Long =
    sumOf { session ->
        session.stages
            .filter { it.stageType == stageType }
            .sumOf { it.durationMs.coerceAtLeast(0L) }
    }

private fun circularMeanMinutes(values: List<Int>): Int {
    if (values.isEmpty()) return 0
    val sinMean = values.sumOf { sin(it.toDouble() / MinutesPerDay * 2.0 * PI) } / values.size
    val cosMean = values.sumOf { cos(it.toDouble() / MinutesPerDay * 2.0 * PI) } / values.size
    val angle = atan2(sinMean, cosMean).let { if (it < 0.0) it + 2.0 * PI else it }
    return (angle / (2.0 * PI) * MinutesPerDay).roundToInt() % MinutesPerDay
}

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute

private fun sleepOverviewLabelDates(
    dates: List<LocalDate>,
    selectedRange: TimeRange,
): List<LocalDate> {
    if (dates.isEmpty()) return emptyList()
    if (dates.size <= 3) return dates

    return when (selectedRange) {
        TimeRange.DAY,
        TimeRange.WEEK -> dates
        TimeRange.MONTH,
        TimeRange.YEAR -> listOf(dates.first(), dates[dates.lastIndex / 2], dates.last()).distinct()
    }
}

private fun sleepOverviewSparklineLabel(
    date: LocalDate,
    selectedRange: TimeRange,
    locale: java.util.Locale,
): String = when (selectedRange) {
    TimeRange.YEAR -> date.month.getDisplayName(TextStyle.SHORT, locale).take(3)
    TimeRange.MONTH -> date.dayOfMonth.toString()
    TimeRange.DAY,
    TimeRange.WEEK -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).take(1)
}

private data class SleepOverviewDay(
    val date: LocalDate,
    val sessions: List<SleepData> = emptyList(),
    val sleepScore: SleepScoreEstimate = SleepScoreEstimate.NoData,
) {
    val mainSleepSession: SleepData?
        get() = sessions.mainSleepSession()

    val sleepDurationMs: Long
        get() = sessions.sumOf { sleepDurationMsFromStages(it.stages, it.durationMs) }

    val remDurationMs: Long
        get() = sessions.stageDurationMs(SleepStage.STAGE_REM)

    val deepDurationMs: Long
        get() = sessions.stageDurationMs(SleepStage.STAGE_DEEP)
}

private data class SleepOverviewSummary(
    val dates: List<LocalDate>,
    val sleepScore: Int?,
    val sleepScoreConfidence: SleepScoreConfidence,
    val sleepDurationMs: Long,
    val schedule: SleepOverviewSchedule?,
    val remDurationMs: Long,
    val deepDurationMs: Long,
    val sleepEfficiencyPercent: Double?,
    val remValues: List<Double>,
    val deepValues: List<Double>,
    val efficiencyValues: List<Double>,
)

private data class SleepOverviewSchedule(
    val startMinute: Int,
    val endMinute: Int,
)

private enum class SleepOverviewValueEmphasis {
    Large,
    Medium,
    Small,
}

private fun LazyListScope.sleepDataConfidence(
    sessions: List<SleepData>,
    durationPoints: List<SleepDurationPoint>,
    period: DatePeriod,
) {
    if (period.start == period.end) return

    item {
        DataConfidenceCard(
            confidence = dataConfidence(
                period = period,
                trackedDates = durationPoints.filter { it.hours > 0.0 }.map { it.date },
                sampleCount = sessions.size,
                sources = sessions.map { it.source },
                valueKind = DataValueKind.MEASURED,
                manualEntryCount = sessions.count {
                    it.recordingMethod == Metadata.RECORDING_METHOD_MANUAL_ENTRY
                },
            ),
            accentColor = SleepColor,
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.sleepTargetContext(
    durationPoints: List<SleepDurationPoint>,
    targetHours: Double,
    unitFormatter: UnitFormatter,
) {
    val nights = durationPoints.filter { it.hours > 0.0 }
    val averageHours = nights.takeIf { it.isNotEmpty() }?.map { it.hours }?.average() ?: return
    val interpretation = sleepTargetInterpretation(
        averageHours = averageHours,
        targetHours = targetHours,
    ) ?: return
    val averageDisplay = sleepHoursDisplay(interpretation.averageHours, unitFormatter).text
    val targetDisplay = sleepHoursDisplay(interpretation.targetHours, unitFormatter).text
    val gapDisplay = sleepHoursDisplay(interpretation.gapHours, unitFormatter).text

    item { SectionHeader(stringResource(R.string.section_metric_context)) }
    item {
        MetricInterpretationCard(
            title = stringResource(R.string.interpretation_sleep_title),
            status = when (interpretation.status) {
                tech.mmarca.openvitals.core.insights.SleepTargetStatus.BELOW_TARGET ->
                    stringResource(R.string.interpretation_sleep_below)
                tech.mmarca.openvitals.core.insights.SleepTargetStatus.NEAR_TARGET ->
                    stringResource(R.string.interpretation_sleep_near)
                tech.mmarca.openvitals.core.insights.SleepTargetStatus.MET_TARGET ->
                    stringResource(R.string.interpretation_sleep_met)
            },
            body = when (interpretation.status) {
                tech.mmarca.openvitals.core.insights.SleepTargetStatus.BELOW_TARGET ->
                    stringResource(R.string.interpretation_sleep_below_body, gapDisplay)
                tech.mmarca.openvitals.core.insights.SleepTargetStatus.NEAR_TARGET ->
                    stringResource(R.string.interpretation_sleep_near_body, averageDisplay, targetDisplay)
                tech.mmarca.openvitals.core.insights.SleepTargetStatus.MET_TARGET ->
                    stringResource(R.string.interpretation_sleep_met_body, averageDisplay, targetDisplay)
            },
            source = stringResource(R.string.interpretation_sleep_source),
            icon = Icons.Outlined.Bed,
            accentColor = SleepColor,
            severity = interpretation.severity,
            modifier = metricModifier(),
        )
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
