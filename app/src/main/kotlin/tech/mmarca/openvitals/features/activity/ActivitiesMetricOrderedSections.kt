package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.metadata.Metadata
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.CrossMetricValue
import tech.mmarca.openvitals.domain.insights.DailyGoalValue
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.insights.WorkoutGuidelineStatus
import tech.mmarca.openvitals.domain.insights.crossMetricInsight
import tech.mmarca.openvitals.domain.insights.dailyGoalProgress
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.domain.insights.workoutGuidelineProgress
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.CrossMetricInsightCard
import tech.mmarca.openvitals.ui.components.DailyGoalCard
import tech.mmarca.openvitals.ui.components.DailyGoalStatistics
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricBarChart
import tech.mmarca.openvitals.ui.components.MetricInterpretationCard
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.renderOrderedMetricDetailSections
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong

internal fun LazyListScope.renderActivitiesOrderedContent(
    sectionContext: MetricDetailSectionContext,
    state: ActivitiesUiState,
    period: DatePeriod,
    chartDaySelection: ChartDaySelection,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit,
    onDeleteActivity: (String) -> Unit,
    onOpenCardioLoad: (() -> Unit)?,
    onOpenSteps: (() -> Unit)?,
    onOpenDistance: (() -> Unit)?,
    onOpenEnergyBurned: (() -> Unit)?,
    onOpenHrv: (() -> Unit)?,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val selectedDate = chartDaySelection.selectedDate
    val goalValues = workoutDailyGoalValues(state.workouts)
    val goalProgress = dailyGoalProgress(
        values = goalValues,
        period = period,
        target = state.dailyGoalMinutes,
        direction = MetricDailyGoalKey.WORKOUT_MINUTES.direction,
    )
    val sortedOverviewDays = state.overviewDays.sortedBy { it.date }
    val overviewTotals = sortedOverviewDays.takeIf { it.isNotEmpty() }?.let(::activityOverviewTotals)
    val hasGuidelineContext = state.workouts.isNotEmpty() &&
        workoutGuidelineProgress(
            if (state.selectedRange == TimeRange.MONTH || state.selectedRange == TimeRange.YEAR) {
                state.workouts.sumOf { it.durationMs.coerceAtLeast(0L) }.toDouble() / 60_000.0 / period.weekCount()
            } else {
                state.workouts.sumOf { it.durationMs.coerceAtLeast(0L) }.toDouble() / 60_000.0
            },
        ) != null
    val hasCrossMetricInsight = state.workouts.isNotEmpty() &&
        crossMetricInsight(
            primaryValues = workoutDailyGoalValues(state.workouts)
                .map { CrossMetricValue(it.date, it.value) },
            secondaryValues = state.crossDailyRestingHR.map { CrossMetricValue(it.date, it.bpm.toDouble()) },
        ) != null

    renderOrderedMetricDetailSections(sectionContext) {
        section(
            MetricDetailSectionId.ACTIVITY_SUMMARY,
            state.workouts.isNotEmpty() || state.plannedWorkouts.isNotEmpty() || !state.isLoading,
        ) {
            val periodTitle = activityPeriodTitle(state.selectedRange, state.activityWeekMode, period)
            Column(modifier = Modifier.fillMaxWidth()) {
                if (state.workouts.isNotEmpty() || !state.isLoading) {
                    ActivityWorkoutListCard(
                        workouts = state.workouts,
                        title = periodTitle,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        onOpenActivity = onOpenActivity,
                        onEditActivity = onEditActivity,
                        onDeleteActivity = onDeleteActivity,
                    )
                }
                if (state.plannedWorkouts.isNotEmpty()) {
                    PlannedWorkoutListCard(
                        plannedWorkouts = state.plannedWorkouts,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                    )
                }
            }
        }
        section(MetricDetailSectionId.ACTIVITY_WEEK_OVERVIEW, sortedOverviewDays.isNotEmpty()) {
            ActivityOverviewPeriodCard(
                days = sortedOverviewDays,
                selectedRange = state.selectedRange,
                activityWeekMode = state.activityWeekMode,
                period = period,
            )
        }
        section(
            MetricDetailSectionId.ACTIVITY_KEY_METRICS,
            sortedOverviewDays.isNotEmpty() && overviewTotals != null,
        ) {
            ActivityKeyMetricsSectionContent(
                sortedDays = sortedOverviewDays,
                totals = overviewTotals!!,
                selectedRange = state.selectedRange,
                activityWeekMode = state.activityWeekMode,
                period = period,
                unitFormatter = unitFormatter,
                onOpenCardioLoad = onOpenCardioLoad,
                onOpenSteps = onOpenSteps,
                onOpenDistance = onOpenDistance,
                onOpenEnergyBurned = onOpenEnergyBurned,
                onOpenHrv = onOpenHrv,
            )
        }
        section(MetricDetailSectionId.PERIOD_CHART, state.workouts.isNotEmpty()) {
            val values = state.workouts
                .groupBy { it.startTime.atZone(ZoneId.systemDefault()).toLocalDate() }
                .map { (date, dayWorkouts) ->
                    PeriodChartValue(
                        date = date,
                        value = dayWorkouts.sumOf { it.durationMs.coerceAtLeast(0L) }.toDouble() / 60_000.0,
                    )
                }
            MetricBarChart(
                title = stringResource(R.string.metric_workout),
                values = values,
                selectedRange = state.selectedRange,
                period = period,
                accentColor = WorkoutColor,
                summaryValue = unitFormatter.duration(state.workouts.sumOf { it.durationMs.coerceAtLeast(0L) }),
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
                valueFormatter = { unitFormatter.minutes(it.roundToLong()).text },
            )
        }
        section(MetricDetailSectionId.SELECTED_DAY_ENTRIES, selectedDate != null && state.workouts.isNotEmpty()) {
            selectedDate?.let { date ->
                val zone = ZoneId.systemDefault()
                PaginatedEntryList(
                    title = entryListTitle(date, dateTimeFormatterProvider),
                    entries = state.workouts.filter { it.startTime.atZone(zone).toLocalDate() == date },
                ) { workout, rowModifier ->
                    ActivityOverviewWorkoutRow(
                        workout = workout,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        onClick = { onOpenActivity(workout.id) },
                        onEdit = workout.editAction(onEditActivity),
                        onDelete = workout.deleteAction(onDeleteActivity),
                        modifier = rowModifier,
                    )
                }
            }
        }
        section(MetricDetailSectionId.DAILY_GOAL, state.workouts.isNotEmpty()) {
            DailyGoalCard(
                goal = unitFormatter.minutes(state.dailyGoalMinutes.roundToLong()),
                progress = goalProgress,
                icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                accentColor = WorkoutColor,
                onDecreaseGoal = onDecreaseGoal,
                onIncreaseGoal = onIncreaseGoal,
                modifier = activityMetricModifier(),
            )
        }
        section(MetricDetailSectionId.STATISTICS, state.workouts.isNotEmpty()) {
            WorkoutStatisticsSectionContent(
                state = state,
                period = period,
                goalProgress = goalProgress,
                unitFormatter = unitFormatter,
            )
        }
        section(MetricDetailSectionId.METRIC_CONTEXT, hasGuidelineContext) {
            WorkoutGuidelineContextSectionContent(
                workouts = state.workouts,
                period = period,
                selectedRange = state.selectedRange,
                unitFormatter = unitFormatter,
            )
        }
        section(MetricDetailSectionId.CROSS_METRIC_INSIGHTS, hasCrossMetricInsight) {
            WorkoutRestingHrInsightSectionContent(
                workouts = state.workouts,
                restingHr = state.crossDailyRestingHR,
            )
        }
        section(MetricDetailSectionId.DATA_CONFIDENCE, state.workouts.isNotEmpty() && period.start != period.end) {
            WorkoutDataConfidenceSectionContent(
                workouts = state.workouts,
                period = period,
            )
        }
    }
}

@Composable
private fun ActivityKeyMetricsSectionContent(
    sortedDays: List<ActivityOverviewDay>,
    totals: ActivityOverviewTotals,
    selectedRange: TimeRange,
    activityWeekMode: ActivityWeekMode,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    onOpenCardioLoad: (() -> Unit)?,
    onOpenSteps: (() -> Unit)?,
    onOpenDistance: (() -> Unit)?,
    onOpenEnergyBurned: (() -> Unit)?,
    onOpenHrv: (() -> Unit)?,
) {
    val periodTitle = activityPeriodTitle(selectedRange, activityWeekMode, period)
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(text = stringResource(R.string.activities_key_metrics))
        val cardioSeries = activityOverviewMetricSeries(
            days = sortedDays,
            selectedRange = selectedRange,
            aggregation = ActivityOverviewMetricAggregation.SUM,
        ) { day ->
            if (day.cardioLoadConfidence == tech.mmarca.openvitals.domain.insights.CardioLoadConfidence.NO_DATA) {
                null
            } else {
                day.cardioLoad.toDouble()
            }
        }
        ActivityMetricCard(
            title = stringResource(R.string.metric_cardio_load),
            value = cardioLoadDisplayValue(totals.cardioLoad, totals.hasCardioLoadData, unitFormatter),
            subtitle = "$periodTitle / ${activityOverviewCardioLoadConfidenceLabel(totals.cardioLoadConfidence)}",
            icon = Icons.Outlined.Favorite,
            accentColor = HeartColor,
            chartValues = cardioSeries.values,
            chartDays = cardioSeries.dates,
            selectedRange = selectedRange,
            modifier = metricCardModifier(),
            onClick = onOpenCardioLoad,
        )
        val caloriesSeries = activityOverviewMetricSeries(
            days = sortedDays,
            selectedRange = selectedRange,
            aggregation = ActivityOverviewMetricAggregation.SUM,
        ) { day ->
            if (day.energyBurnedSource == CaloriesBurnedSource.NO_DATA) null else day.energyBurnedKcal
        }
        val isEstimatedCalories = sortedDays.any {
            it.energyBurnedSource == CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR
        }
        ActivityMetricCard(
            title = stringResource(R.string.metric_energy_burned),
            value = if (totals.hasEnergyBurnedData) {
                unitFormatter.energy(totals.energyBurnedKcal)
            } else {
                DisplayValue(stringResource(R.string.no_data), "")
            },
            subtitle = if (isEstimatedCalories) {
                stringResource(R.string.calories_estimated_active_bmr)
            } else {
                periodTitle
            },
            subtitleColor = if (isEstimatedCalories) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            icon = Icons.Outlined.LocalFireDepartment,
            accentColor = CaloriesColor,
            chartValues = caloriesSeries.values,
            chartDays = caloriesSeries.dates,
            selectedRange = selectedRange,
            modifier = metricCardModifier(),
            onClick = onOpenEnergyBurned,
        )
        val stepsSeries = activityOverviewMetricSeries(
            days = sortedDays,
            selectedRange = selectedRange,
            aggregation = ActivityOverviewMetricAggregation.SUM,
        ) { it.steps.toDouble() }
        ActivityMetricCard(
            title = stringResource(R.string.metric_steps),
            value = DisplayValue(unitFormatter.count(totals.steps), ""),
            subtitle = periodTitle,
            icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
            accentColor = StepsColor,
            chartValues = stepsSeries.values,
            chartDays = stepsSeries.dates,
            selectedRange = selectedRange,
            modifier = metricCardModifier(),
            onClick = onOpenSteps,
        )
        val distanceSeries = activityOverviewMetricSeries(
            days = sortedDays,
            selectedRange = selectedRange,
            aggregation = ActivityOverviewMetricAggregation.SUM,
        ) { it.distanceMeters }
        ActivityMetricCard(
            title = stringResource(R.string.metric_distance),
            value = unitFormatter.distance(totals.distanceMeters),
            subtitle = periodTitle,
            icon = Icons.Outlined.Straighten,
            accentColor = DistanceColor,
            chartValues = distanceSeries.values,
            chartDays = distanceSeries.dates,
            selectedRange = selectedRange,
            modifier = metricCardModifier(),
            onClick = onOpenDistance,
        )
        val hrvSeries = activityOverviewMetricSeries(
            days = sortedDays,
            selectedRange = selectedRange,
            aggregation = ActivityOverviewMetricAggregation.AVERAGE,
        ) { it.hrvRmssdMs }
        ActivityMetricCard(
            title = stringResource(R.string.metric_hrv),
            value = totals.hrvRmssdMs
                ?.let(unitFormatter::hrv)
                ?: DisplayValue(stringResource(R.string.no_data), ""),
            subtitle = "$periodTitle / ${stringResource(R.string.stat_average)}",
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = HeartColor,
            chartValues = hrvSeries.values,
            chartDays = hrvSeries.dates,
            selectedRange = selectedRange,
            modifier = metricCardModifier(),
            onClick = onOpenHrv,
        )
    }
}

@Composable
private fun WorkoutStatisticsSectionContent(
    state: ActivitiesUiState,
    period: DatePeriod,
    goalProgress: tech.mmarca.openvitals.domain.insights.DailyGoalProgress,
    unitFormatter: UnitFormatter,
) {
    val workouts = state.workouts
    Column(modifier = Modifier.fillMaxWidth()) {
        DailyGoalStatistics(
            progress = goalProgress,
            averageGap = unitFormatter.minutes(goalProgress.averageGapToGoal.roundToLong()),
            unitFormatter = unitFormatter,
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            accentColor = WorkoutColor,
            modifier = activityMetricModifier(),
        )
        val totalMs = workouts.sumOf { it.durationMs.coerceAtLeast(0L) }
        val averageMs = workouts.takeIf { it.isNotEmpty() }
            ?.let { totalMs / it.size }
            ?: 0L
        val longestMs = workouts.maxOfOrNull { it.durationMs.coerceAtLeast(0L) } ?: 0L
        val previousTotalMs = state.previousWorkouts.sumOf { it.durationMs.coerceAtLeast(0L) }
        val dailyMinutes = workoutDailyGoalValues(workouts).map { it.value }
        val baselineValues = workoutDailyGoalValues(state.baselineWorkouts)
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
                    selectedRange = state.selectedRange,
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
            modifier = activityMetricModifier(),
        )
    }
}

@Composable
private fun WorkoutGuidelineContextSectionContent(
    workouts: List<ExerciseData>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val totalLoggedMinutes = workouts.sumOf { it.durationMs.coerceAtLeast(0L) }.toDouble() / 60_000.0
    val useWeeklyAverage = selectedRange == TimeRange.MONTH || selectedRange == TimeRange.YEAR
    val guidelineMinutes = if (useWeeklyAverage) {
        totalLoggedMinutes / period.weekCount()
    } else {
        totalLoggedMinutes
    }
    val progress = workoutGuidelineProgress(guidelineMinutes) ?: return

    SectionHeader(stringResource(R.string.section_metric_context))
    MetricInterpretationCard(
        title = stringResource(R.string.interpretation_workout_title),
        status = when (progress.status) {
            WorkoutGuidelineStatus.NO_LOGGED_MINUTES -> stringResource(R.string.interpretation_workout_none)
            WorkoutGuidelineStatus.BELOW_REFERENCE -> stringResource(R.string.interpretation_workout_below)
            WorkoutGuidelineStatus.APPROACHING_REFERENCE -> stringResource(R.string.interpretation_workout_approaching)
            WorkoutGuidelineStatus.MEETS_REFERENCE -> stringResource(R.string.interpretation_workout_met)
        },
        body = stringResource(
            if (useWeeklyAverage) {
                R.string.interpretation_workout_body_weekly_average
            } else {
                R.string.interpretation_workout_body
            },
            unitFormatter.minutes(progress.loggedMinutes.roundToLong()).text,
            unitFormatter.percent(progress.percentOfReference, decimals = 0).text,
        ),
        source = stringResource(R.string.interpretation_workout_source),
        icon = Icons.AutoMirrored.Outlined.DirectionsRun,
        accentColor = WorkoutColor,
        severity = progress.severity,
        modifier = activityMetricModifier(),
    )
}

@Composable
private fun WorkoutRestingHrInsightSectionContent(
    workouts: List<ExerciseData>,
    restingHr: List<DailyRestingHR>,
) {
    val insight = crossMetricInsight(
        primaryValues = workoutDailyGoalValues(workouts)
            .map { CrossMetricValue(it.date, it.value) },
        secondaryValues = restingHr.map { CrossMetricValue(it.date, it.bpm.toDouble()) },
    ) ?: return

    SectionHeader(stringResource(R.string.section_cross_metric_insights))
    CrossMetricInsightCard(
        insight = insight,
        title = stringResource(R.string.cross_workout_resting_hr_title),
        positiveMessage = stringResource(R.string.cross_workout_resting_hr_positive),
        negativeMessage = stringResource(R.string.cross_workout_resting_hr_negative),
        neutralMessage = stringResource(R.string.cross_workout_resting_hr_neutral),
        accentColor = WorkoutColor,
        modifier = activityMetricModifier(),
    )
}

@Composable
private fun WorkoutDataConfidenceSectionContent(
    workouts: List<ExerciseData>,
    period: DatePeriod,
) {
    val zone = ZoneId.systemDefault()
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
        modifier = activityMetricModifier(),
    )
}
