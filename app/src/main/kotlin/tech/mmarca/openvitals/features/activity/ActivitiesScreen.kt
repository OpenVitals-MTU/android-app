package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
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
import androidx.health.connect.client.records.metadata.Metadata
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.insights.BaselineValue
import tech.mmarca.openvitals.core.insights.CrossMetricValue
import tech.mmarca.openvitals.core.insights.DataValueKind
import tech.mmarca.openvitals.core.insights.DailyGoalValue
import tech.mmarca.openvitals.core.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.insights.WorkoutGuidelineStatus
import tech.mmarca.openvitals.core.insights.crossMetricInsight
import tech.mmarca.openvitals.core.insights.dailyGoalProgress
import tech.mmarca.openvitals.core.insights.dataConfidence
import tech.mmarca.openvitals.core.insights.periodComparison
import tech.mmarca.openvitals.core.insights.personalBaselineInsight
import tech.mmarca.openvitals.core.insights.workoutGuidelineProgress
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.DailyRestingHR
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.ui.components.CrossMetricInsightCard
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.DailyGoalCard
import tech.mmarca.openvitals.ui.components.DailyGoalStatistics
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.MetricInterpretationCard
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
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    viewModel: ActivitiesViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit = {},
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
        if (state.workouts.isNotEmpty()) {
            item {
                WorkoutHistoryChart(
                    workouts = state.workouts,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                )
            }
            chartDaySelection.selectedDate?.let { selectedDate ->
                item {
                    val zone = ZoneId.systemDefault()
                    PaginatedEntryList(
                        title = entryListTitle(selectedDate, dateTimeFormatterProvider),
                        entries = state.workouts.filter { it.startTime.atZone(zone).toLocalDate() == selectedDate },
                    ) { workout, rowModifier ->
                        WorkoutListItem(
                            workout = workout,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            onClick = { onOpenActivity(workout.id) },
                            onEdit = workout.editAction(onEditActivity),
                            modifier = rowModifier,
                        )
                    }
                }
            }
            workoutDataConfidence(
                workouts = state.workouts,
                period = period,
            )
            workoutGoal(
                state = state,
                period = period,
                values = workoutDailyGoalValues(state.workouts),
                unitFormatter = unitFormatter,
                onDecreaseGoal = viewModel::decreaseDailyGoal,
                onIncreaseGoal = viewModel::increaseDailyGoal,
            )
            workoutStatistics(
                workouts = state.workouts,
                previousWorkouts = state.previousWorkouts,
                baselineWorkouts = state.baselineWorkouts,
                period = period,
                selectedRange = state.selectedRange,
                unitFormatter = unitFormatter,
                includeHeader = false,
            )
            workoutGuidelineContext(
                workouts = state.workouts,
                unitFormatter = unitFormatter,
            )
            workoutRestingHrInsight(
                workouts = state.workouts,
                restingHr = state.crossDailyRestingHR,
            )
            item {
                PaginatedEntryList(
                    title = stringResource(R.string.section_activities),
                    entries = state.workouts,
                ) { workout, rowModifier ->
                    WorkoutListItem(
                        workout = workout,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        onClick = { onOpenActivity(workout.id) },
                        onEdit = workout.editAction(onEditActivity),
                        modifier = rowModifier,
                    )
                }
            }
        } else if (!state.isLoading) {
            item {
                Text(
                    text = stringResource(R.string.message_no_activities_period),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

private fun LazyListScope.workoutDataConfidence(
    workouts: List<ExerciseData>,
    period: DatePeriod,
) {
    if (period.start == period.end) return

    val zone = ZoneId.systemDefault()
    item {
        DataConfidenceCard(
            confidence = dataConfidence(
                period = period,
                trackedDates = workouts.map { it.startTime.atZone(zone).toLocalDate() },
                sampleCount = workouts.size,
                sources = workouts.map { it.source },
                valueKind = DataValueKind.MEASURED,
                manualEntryCount = workouts.count {
                    it.recordingMethod == Metadata.RECORDING_METHOD_MANUAL_ENTRY
                },
            ),
            accentColor = WorkoutColor,
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.workoutGuidelineContext(
    workouts: List<ExerciseData>,
    unitFormatter: UnitFormatter,
) {
    val loggedMinutes = workouts.sumOf { it.durationMs.coerceAtLeast(0L) }.toDouble() / 60_000.0
    val progress = workoutGuidelineProgress(loggedMinutes) ?: return
    item { SectionHeader(stringResource(R.string.section_metric_context)) }
    item {
        MetricInterpretationCard(
            title = stringResource(R.string.interpretation_workout_title),
            status = when (progress.status) {
                WorkoutGuidelineStatus.NO_LOGGED_MINUTES -> stringResource(R.string.interpretation_workout_none)
                WorkoutGuidelineStatus.BELOW_REFERENCE -> stringResource(R.string.interpretation_workout_below)
                WorkoutGuidelineStatus.APPROACHING_REFERENCE -> stringResource(R.string.interpretation_workout_approaching)
                WorkoutGuidelineStatus.MEETS_REFERENCE -> stringResource(R.string.interpretation_workout_met)
            },
            body = stringResource(
                R.string.interpretation_workout_body,
                unitFormatter.minutes(progress.loggedMinutes.roundToLong()).text,
                unitFormatter.percent(progress.percentOfReference, decimals = 0).text,
            ),
            source = stringResource(R.string.interpretation_workout_source),
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            accentColor = WorkoutColor,
            severity = progress.severity,
            modifier = metricModifier(),
        )
    }
}

@Composable
private fun WorkoutHistoryChart(
    workouts: List<ExerciseData>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val zone = ZoneId.systemDefault()
    val values = workouts
        .groupBy { it.startTime.atZone(zone).toLocalDate() }
        .map { (date, dayWorkouts) ->
            PeriodChartValue(
                date = date,
                value = dayWorkouts.sumOf { it.durationMs.coerceAtLeast(0L) }.toDouble() / 60_000.0,
            )
        }
    val totalMs = workouts.sumOf { it.durationMs.coerceAtLeast(0L) }

    PeriodHistoryChart(
        title = stringResource(R.string.metric_workout),
        values = values,
        selectedRange = selectedRange,
        period = period,
        accentColor = WorkoutColor.copy(alpha = 0.85f),
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${unitFormatter.duration(totalMs)}",
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier.fillMaxWidth(),
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = { unitFormatter.minutes(it.roundToLong()).text },
    )
}

private fun LazyListScope.workoutGoal(
    state: ActivitiesUiState,
    period: DatePeriod,
    values: List<DailyGoalValue>,
    unitFormatter: UnitFormatter,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val goalKey = MetricDailyGoalKey.WORKOUT_MINUTES
    val progress = dailyGoalProgress(
        values = values,
        period = period,
        target = state.dailyGoalMinutes,
        direction = goalKey.direction,
    )
    item {
        DailyGoalCard(
            goal = unitFormatter.minutes(state.dailyGoalMinutes.roundToLong()),
            progress = progress,
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            accentColor = WorkoutColor,
            onDecreaseGoal = onDecreaseGoal,
            onIncreaseGoal = onIncreaseGoal,
            modifier = metricModifier(),
        )
    }
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        DailyGoalStatistics(
            progress = progress,
            averageGap = unitFormatter.minutes(progress.averageGapToGoal.roundToLong()),
            unitFormatter = unitFormatter,
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            accentColor = WorkoutColor,
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.workoutStatistics(
    workouts: List<ExerciseData>,
    previousWorkouts: List<ExerciseData>,
    baselineWorkouts: List<ExerciseData>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
    includeHeader: Boolean = true,
) {
    if (includeHeader) {
        item { SectionHeader(stringResource(R.string.section_statistics)) }
    }
    item {
        val totalMs = workouts.sumOf { it.durationMs.coerceAtLeast(0L) }
        val averageMs = workouts.takeIf { it.isNotEmpty() }
            ?.let { totalMs / it.size }
            ?: 0L
        val longestMs = workouts.maxOfOrNull { it.durationMs.coerceAtLeast(0L) } ?: 0L
        val previousTotalMs = previousWorkouts.sumOf { it.durationMs.coerceAtLeast(0L) }
        val dailyMinutes = workoutDailyGoalValues(workouts).map { it.value }
        val baselineValues = workoutDailyGoalValues(baselineWorkouts)
            .map { BaselineValue(it.date, it.value) }

        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.stat_total),
                    value = unitFormatter.duration(totalMs),
                    unit = "",
                    icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                    accentColor = WorkoutColor,
                ),
                InsightStat(
                    title = stringResource(R.string.section_activities),
                    value = unitFormatter.count(workouts.size),
                    unit = "",
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = WorkoutColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_average_duration),
                    value = unitFormatter.duration(averageMs),
                    unit = "",
                    icon = Icons.Outlined.Star,
                    accentColor = WorkoutColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_longest_workout),
                    value = unitFormatter.duration(longestMs),
                    unit = "",
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = WorkoutColor,
                ),
                previousPeriodInsightStat(
                    comparison = periodComparison(
                        currentValue = totalMs.toDouble(),
                        previousValue = previousTotalMs.toDouble(),
                    ),
                    selectedRange = selectedRange,
                    unitFormatter = unitFormatter,
                    valueFormatter = { DisplayValue(unitFormatter.duration(it.roundToLong()), "") },
                    accentColor = WorkoutColor,
                ),
            ) + personalBaselineInsightStats(
                insight = personalBaselineInsight(
                    currentValue = dailyMinutes.takeIf { it.isNotEmpty() }?.average() ?: 0.0,
                    values = baselineValues,
                    referenceDate = period.start.minusDays(1),
                ),
                unitFormatter = unitFormatter,
                valueFormatter = { unitFormatter.minutes(it.roundToLong()) },
                accentColor = WorkoutColor,
            ),
            modifier = metricModifier(),
        )
    }
}

private fun workoutDailyGoalValues(workouts: List<ExerciseData>): List<DailyGoalValue> {
    val zone = ZoneId.systemDefault()
    return workouts
        .groupBy { it.startTime.atZone(zone).toLocalDate() }
        .map { (date, dayWorkouts) ->
            DailyGoalValue(
                date = date,
                value = dayWorkouts.sumOf { it.durationMs.coerceAtLeast(0L) }.toDouble() / 60_000.0,
            )
        }
}

private fun LazyListScope.workoutRestingHrInsight(
    workouts: List<ExerciseData>,
    restingHr: List<DailyRestingHR>,
) {
    val insight = crossMetricInsight(
        primaryValues = workoutDailyGoalValues(workouts)
            .map { CrossMetricValue(it.date, it.value) },
        secondaryValues = restingHr.map { CrossMetricValue(it.date, it.bpm.toDouble()) },
    ) ?: return

    item { SectionHeader(stringResource(R.string.section_cross_metric_insights)) }
    item {
        CrossMetricInsightCard(
            insight = insight,
            title = stringResource(R.string.cross_workout_resting_hr_title),
            positiveMessage = stringResource(R.string.cross_workout_resting_hr_positive),
            negativeMessage = stringResource(R.string.cross_workout_resting_hr_negative),
            neutralMessage = stringResource(R.string.cross_workout_resting_hr_neutral),
            accentColor = WorkoutColor,
            modifier = metricModifier(),
        )
    }
}

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutListItem(
    workout: ExerciseData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = workout.startTime.atZone(zone)
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()

    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                imageVector = exerciseTypeIcon(workout.exerciseType),
                contentDescription = null,
                tint = WorkoutColor,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.title ?: exerciseTypeLabel(workout.exerciseType),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${dateFormatter.format(start)}  ·  ${timeFormatter.format(start)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                workout.totalDistanceMeters?.let { d ->
                    Text(
                        text = unitFormatter.distance(d).text,
                        style = MaterialTheme.typography.bodySmall,
                        color = DistanceColor,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = unitFormatter.duration(workout.durationMs),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(4.dp))
                SourceChip(source = workout.source)
            }
            Spacer(Modifier.width(8.dp))
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.cd_edit_entry),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun ExerciseData.editAction(onEditActivity: (String) -> Unit): (() -> Unit)? =
    if (isOpenVitalsEntry && id.isNotBlank()) {
        { onEditActivity(id) }
    } else {
        null
    }
