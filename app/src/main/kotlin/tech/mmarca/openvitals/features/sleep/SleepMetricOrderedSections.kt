package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.metadata.Metadata
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.DailyGoalValue
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.insights.crossMetricInsight
import tech.mmarca.openvitals.domain.insights.dailyGoalProgress
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.domain.insights.sleepTargetInterpretation
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.CrossMetricValue
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.CrossMetricInsightCard
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.DailyGoalCard
import tech.mmarca.openvitals.ui.components.DailyGoalStatistics
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricBarChart
import tech.mmarca.openvitals.ui.components.MetricInterpretationCard
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.PeriodBarAggregation
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.renderOrderedMetricDetailSections
import tech.mmarca.openvitals.ui.theme.SleepColor
import java.time.LocalDate
import kotlin.math.roundToLong

internal fun LazyListScope.renderSleepDayOrderedContent(
    sectionContext: MetricDetailSectionContext,
    state: SleepUiState,
    display: SleepDisplayState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenSleepSession: (String) -> Unit,
    onOpenSleepScore: (() -> Unit)?,
    onOpenSleepEfficiency: (() -> Unit)?,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val summary = display.dailySummary ?: return
    val goalProgress = sleepGoalProgress(state, period, display.durationPoints)

    renderOrderedMetricDetailSections(sectionContext) {
        section(MetricDetailSectionId.INTRADAY_CHART) {
            SleepSessionTimelineCard(
                session = summary,
                selectedDate = state.selectedDate,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                timeRangeText = dailySleepTimeRangeText(
                    sessions = display.dailySessions,
                    selectedDate = state.selectedDate,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                ),
                onClick = display.dailySessions.singleOrNull()?.let { session ->
                    { onOpenSleepSession(session.id) }
                },
                preserveTimelineGaps = display.dailySessions.size > 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        section(
            MetricDetailSectionId.ACTIVITY_SUMMARY,
            !state.isLoading || state.sessions.isNotEmpty(),
        ) {
            SleepOverviewSectionContent(
                summary = display.overviewSummary,
                selectedRange = state.selectedRange,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenSleepScore = onOpenSleepScore,
                onOpenSleepEfficiency = onOpenSleepEfficiency,
            )
        }
        section(MetricDetailSectionId.DAILY_GOAL) {
            DailyGoalCard(
                goal = sleepHoursDisplay(state.dailyGoalHours, unitFormatter),
                progress = goalProgress,
                icon = Icons.Outlined.Bed,
                accentColor = SleepColor,
                onDecreaseGoal = onDecreaseGoal,
                onIncreaseGoal = onIncreaseGoal,
                modifier = metricModifier(),
            )
        }
        section(MetricDetailSectionId.STATISTICS) {
            SleepStatisticsSectionContent(
                state = state,
                display = display,
                period = period,
                unitFormatter = unitFormatter,
                goalProgress = goalProgress,
            )
        }
        section(MetricDetailSectionId.DATA_CONFIDENCE, period.start != period.end) {
            SleepDataConfidenceSectionContent(
                sessions = display.dailySessions,
                durationPoints = display.durationPoints,
                period = period,
            )
        }
        section(MetricDetailSectionId.ENTRIES, display.dailySessions.size > 1) {
            PaginatedEntryList(
                title = stringResource(R.string.section_sleep_sessions),
                entries = display.dailySessions.sortedByDescending { it.endTime },
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

internal fun LazyListScope.renderSleepPeriodOrderedContent(
    sectionContext: MetricDetailSectionContext,
    state: SleepUiState,
    display: SleepDisplayState,
    period: DatePeriod,
    chartDaySelection: ChartDaySelection,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenSleepSession: (String) -> Unit,
    onOpenSleepScore: (() -> Unit)?,
    onOpenSleepEfficiency: (() -> Unit)?,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val selectedDate = chartDaySelection.selectedDate
    val goalProgress = sleepGoalProgress(state, period, display.durationPoints)
    val nightsWithSleep = display.durationPoints.filter { it.hours > 0.0 }
    val averageHours = nightsWithSleep.map { it.hours }.average().takeIf { !it.isNaN() } ?: 0.0

    renderOrderedMetricDetailSections(sectionContext) {
        section(MetricDetailSectionId.ACTIVITY_SUMMARY) {
            SleepOverviewSectionContent(
                summary = display.overviewSummary,
                selectedRange = state.selectedRange,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenSleepScore = onOpenSleepScore,
                onOpenSleepEfficiency = onOpenSleepEfficiency,
            )
        }
        section(MetricDetailSectionId.PERIOD_CHART) {
            MetricBarChart(
                title = stringResource(R.string.metric_sleep),
                values = display.durationPoints.map { PeriodChartValue(date = it.date, value = it.hours) },
                selectedRange = state.selectedRange,
                period = period,
                accentColor = SleepColor,
                accentAlpha = 0.75f,
                summaryValue = "${
                    stringResource(R.string.summary_avg_value, "${unitFormatter.decimal(averageHours, 1)}h")
                } · ${stringResource(R.string.summary_nights, unitFormatter.count(nightsWithSleep.size))}",
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sleep_week_period_content")
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                yearAggregation = PeriodBarAggregation.AVERAGE_NON_ZERO,
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
                valueFormatter = { "${unitFormatter.decimal(it, 1)}h" },
            )
        }
        section(MetricDetailSectionId.SELECTED_DAY_ENTRIES, selectedDate != null) {
            selectedDate?.let { date ->
                val daySessions = display.overviewDays
                    .firstOrNull { it.date == date }
                    ?.sessions
                    .orEmpty()
                PaginatedEntryList(
                    title = entryListTitle(date, dateTimeFormatterProvider),
                    entries = daySessions.sortedByDescending { it.endTime },
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
        section(MetricDetailSectionId.DAILY_GOAL) {
            DailyGoalCard(
                goal = sleepHoursDisplay(state.dailyGoalHours, unitFormatter),
                progress = goalProgress,
                icon = Icons.Outlined.Bed,
                accentColor = SleepColor,
                onDecreaseGoal = onDecreaseGoal,
                onIncreaseGoal = onIncreaseGoal,
                modifier = metricModifier(),
            )
        }
        section(MetricDetailSectionId.STATISTICS) {
            SleepStatisticsSectionContent(
                state = state,
                display = display,
                period = period,
                unitFormatter = unitFormatter,
                goalProgress = goalProgress,
            )
        }
        section(MetricDetailSectionId.DATA_CONFIDENCE, period.start != period.end) {
            SleepDataConfidenceSectionContent(
                sessions = state.sessions,
                durationPoints = display.durationPoints,
                period = period,
            )
        }
        section(MetricDetailSectionId.ENTRIES) {
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
}

private fun sleepGoalProgress(
    state: SleepUiState,
    period: DatePeriod,
    durationPoints: List<SleepDurationPoint>,
) = dailyGoalProgress(
    values = durationPoints.map { DailyGoalValue(date = it.date, value = it.hours) },
    period = period,
    target = state.dailyGoalHours,
    direction = MetricDailyGoalKey.SLEEP_HOURS.direction,
)

@Composable
private fun SleepStatisticsSectionContent(
    state: SleepUiState,
    display: SleepDisplayState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    goalProgress: tech.mmarca.openvitals.domain.insights.DailyGoalProgress,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DailyGoalStatistics(
            progress = goalProgress,
            averageGap = sleepHoursDisplay(goalProgress.averageGapToGoal, unitFormatter),
            unitFormatter = unitFormatter,
            icon = Icons.Outlined.Bed,
            accentColor = SleepColor,
            modifier = metricModifier(),
        )
        val nights = display.durationPoints.filter { it.hours > 0.0 }
        val totalHours = nights.sumOf { it.hours }
        val averageHours = nights.takeIf { it.isNotEmpty() }?.map { it.hours }?.average() ?: 0.0
        val longestHours = nights.maxOfOrNull { it.hours } ?: 0.0
        val previousNights = display.previousDurationPoints.filter { it.hours > 0.0 }
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
                    selectedRange = state.selectedRange,
                    unitFormatter = unitFormatter,
                    valueFormatter = { sleepHoursDisplay(it, unitFormatter) },
                    accentColor = SleepColor,
                ),
            ) + personalBaselineInsightStats(
                insight = personalBaselineInsight(
                    currentValue = averageHours,
                    values = display.baselineDurationPoints.map { BaselineValue(it.date, it.hours) },
                    referenceDate = period.start.minusDays(1),
                ),
                unitFormatter = unitFormatter,
                valueFormatter = { sleepHoursDisplay(it, unitFormatter) },
                accentColor = SleepColor,
            ),
            modifier = metricModifier(),
        )
        SleepTargetContextSectionContent(
            durationPoints = display.durationPoints,
            targetHours = state.dailyGoalHours,
            unitFormatter = unitFormatter,
        )
        SleepHrvInsightSectionContent(
            durationPoints = display.durationPoints,
            hrvValues = display.crossMetricHrvValues,
        )
    }
}

@Composable
private fun SleepTargetContextSectionContent(
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

    SectionHeader(stringResource(R.string.section_metric_context))
    MetricInterpretationCard(
        title = stringResource(R.string.interpretation_sleep_title),
        status = when (interpretation.status) {
            tech.mmarca.openvitals.domain.insights.SleepTargetStatus.BELOW_TARGET ->
                stringResource(R.string.interpretation_sleep_below)
            tech.mmarca.openvitals.domain.insights.SleepTargetStatus.NEAR_TARGET ->
                stringResource(R.string.interpretation_sleep_near)
            tech.mmarca.openvitals.domain.insights.SleepTargetStatus.MET_TARGET ->
                stringResource(R.string.interpretation_sleep_met)
        },
        body = when (interpretation.status) {
            tech.mmarca.openvitals.domain.insights.SleepTargetStatus.BELOW_TARGET ->
                stringResource(R.string.interpretation_sleep_below_body, gapDisplay)
            tech.mmarca.openvitals.domain.insights.SleepTargetStatus.NEAR_TARGET ->
                stringResource(R.string.interpretation_sleep_near_body, averageDisplay, targetDisplay)
            tech.mmarca.openvitals.domain.insights.SleepTargetStatus.MET_TARGET ->
                stringResource(R.string.interpretation_sleep_met_body, averageDisplay, targetDisplay)
        },
        source = stringResource(R.string.interpretation_sleep_source),
        icon = Icons.Outlined.Bed,
        accentColor = SleepColor,
        severity = interpretation.severity,
        modifier = metricModifier(),
    )
}

@Composable
private fun SleepHrvInsightSectionContent(
    durationPoints: List<SleepDurationPoint>,
    hrvValues: List<CrossMetricValue>,
) {
    val insight = crossMetricInsight(
        primaryValues = durationPoints.map { CrossMetricValue(it.date, it.hours) },
        secondaryValues = hrvValues,
    ) ?: return

    SectionHeader(stringResource(R.string.section_cross_metric_insights))
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

@Composable
private fun SleepDataConfidenceSectionContent(
    sessions: List<SleepData>,
    durationPoints: List<SleepDurationPoint>,
    period: DatePeriod,
) {
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
