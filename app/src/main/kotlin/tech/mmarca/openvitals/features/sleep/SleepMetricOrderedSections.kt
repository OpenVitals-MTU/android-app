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
import tech.mmarca.openvitals.domain.model.RecordingMethod
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
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
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
import java.time.ZoneId
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
            Column {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                SleepStageShareCard(
                    durations = sleepStageDurationsOf(display.overviewSummary),
                    unitFormatter = unitFormatter,
                    modifier = metricModifier(),
                )
                if (display.dayNaps.isNotEmpty()) {
                    SectionHeader(stringResource(R.string.sleep_naps))
                    display.dayNaps.forEach { nap ->
                        SleepSessionItem(
                            session = nap,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            onClick = { onOpenSleepSession(nap.id) },
                            modifier = metricModifier(),
                        )
                    }
                }
            }
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
                entries = sleepEntriesNewestFirst(display.dailySessions),
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
    val scheduleDays = display.overviewDays.toSleepScheduleDays()
    val scheduleAxis = SleepScheduleAxis.range(
        days = scheduleDays,
        zone = ZoneId.systemDefault(),
        anchorMinute = state.sleepWindow.startHour * 60,
    )
    val useScheduleChart = useSleepScheduleChart(state.selectedRange, scheduleDays, scheduleAxis)

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
            val summaryValue = "${
                stringResource(R.string.summary_avg_value, "${unitFormatter.decimal(averageHours, 1)}h")
            } · ${stringResource(R.string.summary_nights, unitFormatter.count(nightsWithSleep.size))}"
            val chartModifier = Modifier
                .fillMaxWidth()
                .testTag("sleep_week_period_content")
                .padding(horizontal = 16.dp, vertical = 8.dp)
            Column {
                if (useScheduleChart) {
                    SleepScheduleStageChart(
                        title = stringResource(R.string.metric_sleep),
                        summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · $summaryValue",
                        days = scheduleDays,
                        sleepWindow = state.sleepWindow,
                        selectedRange = state.selectedRange,
                        period = period,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        averageSchedule = display.overviewSummary.schedule,
                        selectedDate = chartDaySelection.selectedDate,
                        onDateSelected = chartDaySelection.onDateSelected,
                        modifier = chartModifier,
                    )
                } else {
                    MetricBarChart(
                        title = stringResource(R.string.metric_sleep),
                        values = display.durationPoints.map { PeriodChartValue(date = it.date, value = it.hours) },
                        selectedRange = state.selectedRange,
                        period = period,
                        accentColor = SleepColor,
                        accentAlpha = 0.75f,
                        summaryValue = summaryValue,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = chartModifier,
                        yearAggregation = PeriodBarAggregation.AVERAGE_NON_ZERO,
                        selectedDate = chartDaySelection.selectedDate,
                        onDateSelected = chartDaySelection.onDateSelected,
                        valueFormatter = { "${unitFormatter.decimal(it, 1)}h" },
                    )
                }
                SleepStageShareCard(
                    durations = sleepStageDurationsOf(display.overviewSummary),
                    unitFormatter = unitFormatter,
                    modifier = metricModifier(),
                )
            }
        }
        section(MetricDetailSectionId.SELECTED_DAY_ENTRIES, selectedDate != null) {
            selectedDate?.let { date ->
                val daySessions = display.overviewDays
                    .firstOrNull { it.date == date }
                    ?.sessions
                    .orEmpty()
                PaginatedEntryList(
                    title = entryListTitle(date, dateTimeFormatterProvider),
                    entries = sleepEntriesNewestFirst(daySessions),
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
                entries = sleepEntriesNewestFirst(state.sessions),
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

internal fun sleepStageDurationsOf(summary: SleepOverviewSummary) = SleepStageDurations(
    awakeMs = summary.awakeDurationMs,
    remMs = summary.remDurationMs,
    lightMs = summary.coreDurationMs,
    deepMs = summary.deepDurationMs,
)

/**
 * Week and month draw the time-aligned schedule chart, but only when at least one night knows its
 * bedtime and the plausible nights yield an axis; otherwise (and always on day/year) the plain
 * duration bar chart is used.
 */
internal fun useSleepScheduleChart(
    selectedRange: TimeRange,
    scheduleDays: List<SleepScheduleDay>,
    scheduleAxis: SleepScheduleAxis.Range?,
): Boolean = selectedRange in setOf(TimeRange.WEEK, TimeRange.MONTH) &&
    scheduleDays.any { it.inBedStart != null } &&
    scheduleAxis != null

/** Entry lists come out newest night first. */
internal fun sleepEntriesNewestFirst(sessions: List<SleepData>): List<SleepData> =
    sessions.sortedByDescending { it.endTime }

/**
 * Health Connect RECORDING_METHOD_MANUAL_ENTRY sessions only — an actively-recorded night is not
 * a manual entry.
 */
internal fun sleepManualEntryCount(sessions: List<SleepData>): Int =
    sessions.count { it.recordingMethod == RecordingMethod.MANUAL_ENTRY }

/**
 * Period totals over the duration points: only the nights that recorded sleep count as nights, and
 * an empty period derives zeroes rather than NaN. Hoisted out of the statistics composable so the
 * arithmetic is testable on the JVM.
 */
internal data class SleepPeriodTotals(
    val nights: Int,
    val totalHours: Double,
    val averageHours: Double,
    val longestHours: Double,
)

internal fun sleepPeriodTotals(durationPoints: List<SleepDurationPoint>): SleepPeriodTotals {
    val nights = durationPoints.filter { it.hours > 0.0 }
    return SleepPeriodTotals(
        nights = nights.size,
        totalHours = nights.sumOf { it.hours },
        averageHours = nights.takeIf { it.isNotEmpty() }?.map { it.hours }?.average() ?: 0.0,
        longestHours = nights.maxOfOrNull { it.hours } ?: 0.0,
    )
}

internal fun sleepGoalProgress(
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
        val totals = sleepPeriodTotals(display.durationPoints)
        val totalHours = totals.totalHours
        val averageHours = totals.averageHours
        val longestHours = totals.longestHours
        val previousNights = display.previousDurationPoints.filter { it.hours > 0.0 }
        val previousAverageHours = previousNights.takeIf { it.isNotEmpty() }?.map { it.hours }?.average() ?: 0.0
        val isDay = period.start == period.end
        InsightStatGrid(
            stats = listOfNotNull(
                InsightStat(
                    title = stringResource(R.string.stat_total),
                    value = unitFormatter.duration((totalHours * 3_600_000).roundToLong()),
                    unit = "",
                    icon = Icons.Outlined.Bed,
                    accentColor = SleepColor,
                ),
                if (!isDay) {
                    InsightStat(
                        title = stringResource(R.string.stat_daily_average),
                        value = unitFormatter.duration((averageHours * 3_600_000).roundToLong()),
                        unit = "",
                        icon = Icons.Outlined.Star,
                        accentColor = SleepColor,
                    )
                } else {
                    null
                },
                if (!isDay) {
                    InsightStat(
                        title = stringResource(R.string.stat_longest_sleep),
                        value = unitFormatter.duration((longestHours * 3_600_000).roundToLong()),
                        unit = "",
                        icon = Icons.Outlined.CalendarMonth,
                        accentColor = SleepColor,
                    )
                } else {
                    null
                },
                InsightStat(
                    title = stringResource(R.string.stat_nights_logged),
                    value = unitFormatter.count(totals.nights),
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
internal fun SleepHrvInsightSectionContent(
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
            manualEntryCount = sleepManualEntryCount(sessions),
        ),
        accentColor = SleepColor,
        modifier = metricModifier(),
    )
}
