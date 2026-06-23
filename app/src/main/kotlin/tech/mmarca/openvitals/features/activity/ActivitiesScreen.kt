package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.metadata.Metadata
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.CardioLoadConfidence
import tech.mmarca.openvitals.domain.insights.CrossMetricValue
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.DailyGoalValue
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.insights.WorkoutGuidelineStatus
import tech.mmarca.openvitals.domain.insights.crossMetricInsight
import tech.mmarca.openvitals.domain.insights.dailyGoalProgress
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.domain.insights.workoutGuidelineProgress
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.toWeekPeriodMode
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.ui.components.AutoResizeText
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
import tech.mmarca.openvitals.ui.components.SwipeToDeleteEntryRow
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.localizedDayTitle
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToLong

@Composable
fun ActivitiesScreen(
    viewModel: ActivitiesViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit = {},
    onOpenCardioLoad: (() -> Unit)? = null,
    onOpenSteps: (() -> Unit)? = null,
    onOpenDistance: (() -> Unit)? = null,
    onOpenEnergyBurned: (() -> Unit)? = null,
    onOpenHrv: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load()
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
        weekPeriodMode = state.activityWeekMode.toWeekPeriodMode(),
        periodOverride = {
            activityDisplayPeriod(
                selectedRange = state.selectedRange,
                selectedDate = state.selectedDate,
                activityWeekMode = state.activityWeekMode,
            )
        },
        periodTitle = { period ->
            activityPeriodTitle(state.selectedRange, state.activityWeekMode, period)
        },
    ) { period ->
        activityWorkoutListSection(
            workouts = state.workouts,
            isLoading = state.isLoading,
            selectedRange = state.selectedRange,
            activityWeekMode = state.activityWeekMode,
            period = period,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onOpenActivity = onOpenActivity,
            onEditActivity = onEditActivity,
            onDeleteActivity = viewModel::deleteActivityEntry,
        )
        plannedWorkoutListSection(
            plannedWorkouts = state.plannedWorkouts,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )
        activityPeriodOverview(
            overviewDays = state.overviewDays,
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
                        ActivityOverviewWorkoutRow(
                            workout = workout,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            onClick = { onOpenActivity(workout.id) },
                            onEdit = workout.editAction(onEditActivity),
                            onDelete = workout.deleteAction(viewModel::deleteActivityEntry),
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
                period = period,
                selectedRange = state.selectedRange,
                unitFormatter = unitFormatter,
            )
            workoutRestingHrInsight(
                workouts = state.workouts,
                restingHr = state.crossDailyRestingHR,
            )
        }
    }
}

private fun LazyListScope.activityWorkoutListSection(
    workouts: List<ExerciseData>,
    isLoading: Boolean,
    selectedRange: TimeRange,
    activityWeekMode: ActivityWeekMode,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit,
    onDeleteActivity: (String) -> Unit,
) {
    if (workouts.isEmpty() && isLoading) return

    item {
        ActivityWorkoutListCard(
            workouts = workouts,
            title = activityPeriodTitle(selectedRange, activityWeekMode, period),
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onOpenActivity = onOpenActivity,
            onEditActivity = onEditActivity,
            onDeleteActivity = onDeleteActivity,
        )
    }
}

private fun LazyListScope.activityPeriodOverview(
    overviewDays: List<ActivityOverviewDay>,
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
    if (overviewDays.isEmpty()) return

    val sortedDays = overviewDays.sortedBy { it.date }
    val totals = activityOverviewTotals(sortedDays)

    item {
        ActivityOverviewPeriodCard(
            days = sortedDays,
            selectedRange = selectedRange,
            activityWeekMode = activityWeekMode,
            period = period,
        )
    }
    item {
        SectionHeader(
            text = stringResource(R.string.activities_key_metrics),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    item {
        val periodTitle = activityPeriodTitle(selectedRange, activityWeekMode, period)
        val series = activityOverviewMetricSeries(
            days = sortedDays,
            selectedRange = selectedRange,
            aggregation = ActivityOverviewMetricAggregation.SUM,
        ) { day ->
            if (day.cardioLoadConfidence == CardioLoadConfidence.NO_DATA) null else day.cardioLoad.toDouble()
        }
        ActivityMetricCard(
            title = stringResource(R.string.metric_cardio_load),
            value = cardioLoadDisplayValue(totals.cardioLoad, totals.hasCardioLoadData, unitFormatter),
            subtitle = "$periodTitle / ${activityOverviewCardioLoadConfidenceLabel(totals.cardioLoadConfidence)}",
            icon = Icons.Outlined.Favorite,
            accentColor = HeartColor,
            chartValues = series.values,
            chartDays = series.dates,
            selectedRange = selectedRange,
            modifier = metricCardModifier(),
            onClick = onOpenCardioLoad,
        )
    }
    item {
        val periodTitle = activityPeriodTitle(selectedRange, activityWeekMode, period)
        val series = activityOverviewMetricSeries(
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
            chartValues = series.values,
            chartDays = series.dates,
            selectedRange = selectedRange,
            modifier = metricCardModifier(),
            onClick = onOpenEnergyBurned,
        )
    }
    item {
        val periodTitle = activityPeriodTitle(selectedRange, activityWeekMode, period)
        val series = activityOverviewMetricSeries(
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
            chartValues = series.values,
            chartDays = series.dates,
            selectedRange = selectedRange,
            modifier = metricCardModifier(),
            onClick = onOpenSteps,
        )
    }
    item {
        val periodTitle = activityPeriodTitle(selectedRange, activityWeekMode, period)
        val series = activityOverviewMetricSeries(
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
            chartValues = series.values,
            chartDays = series.dates,
            selectedRange = selectedRange,
            modifier = metricCardModifier(),
            onClick = onOpenDistance,
        )
    }
    item {
        val periodTitle = activityPeriodTitle(selectedRange, activityWeekMode, period)
        val series = activityOverviewMetricSeries(
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
            chartValues = series.values,
            chartDays = series.dates,
            selectedRange = selectedRange,
            modifier = metricCardModifier(),
            onClick = onOpenHrv,
        )
    }
}

