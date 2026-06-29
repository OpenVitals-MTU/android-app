package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.metadata.Metadata
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.CrossMetricValue
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.DailyGoalValue
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.insights.crossMetricInsight
import tech.mmarca.openvitals.domain.insights.dailyGoalProgress
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.domain.insights.sleepTargetInterpretation
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.ui.components.CrossMetricInsightCard
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.DailyGoalCard
import tech.mmarca.openvitals.ui.components.DailyGoalStatistics
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricInterpretationCard
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.theme.SleepColor
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong

internal fun LazyListScope.sleepDataConfidence(
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

internal fun LazyListScope.sleepTargetContext(
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
}

internal fun dailySleepTimeRangeText(
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

internal fun LazyListScope.sleepGoal(
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

internal fun LazyListScope.sleepStatistics(
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

internal fun LazyListScope.sleepInsightSections(
    state: SleepUiState,
    period: DatePeriod,
    confidenceSessions: List<SleepData>,
    display: SleepDisplayState,
    unitFormatter: UnitFormatter,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    sleepDataConfidence(
        sessions = confidenceSessions,
        durationPoints = display.durationPoints,
        period = period,
    )
    sleepGoal(
        state = state,
        period = period,
        durationPoints = display.durationPoints,
        unitFormatter = unitFormatter,
        onDecreaseGoal = onDecreaseGoal,
        onIncreaseGoal = onIncreaseGoal,
    )
    sleepStatistics(
        durationPoints = display.durationPoints,
        previousDurationPoints = display.previousDurationPoints,
        baselineDurationPoints = display.baselineDurationPoints,
        period = period,
        selectedRange = state.selectedRange,
        unitFormatter = unitFormatter,
        includeHeader = false,
    )
    sleepTargetContext(
        durationPoints = display.durationPoints,
        targetHours = state.dailyGoalHours,
        unitFormatter = unitFormatter,
    )
    sleepHrvInsight(
        durationPoints = display.durationPoints,
        hrvValues = display.crossMetricHrvValues,
    )
}

internal fun LazyListScope.sleepHrvInsight(
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

internal fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)

internal fun sleepHoursDisplay(hours: Double, unitFormatter: UnitFormatter): DisplayValue =
    DisplayValue(unitFormatter.duration((hours * 3_600_000).roundToLong()), "")
