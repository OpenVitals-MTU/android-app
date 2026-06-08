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
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.insights.BaselineValue
import tech.mmarca.openvitals.core.insights.CardioLoadConfidence
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
import tech.mmarca.openvitals.core.preferences.ActivityWeekMode
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.CaloriesBurnedSource
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

private val ActivityOverviewCardHeight = 132.dp
private val ActivityOverviewChartWidth = 152.dp
private val ActivityOverviewChartHeight = 58.dp
private val ActivityOverviewBarWidth = 10.dp
private val ActivityOverviewBarRadius = 8.dp
private val ActivityOverviewMarkerSize = 38.dp
private const val ActivityWorkoutListPageSize = 10

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
    if (selectedRange != TimeRange.WEEK) {
        item { SectionHeader(stringResource(R.string.activities_key_metrics)) }
        item {
            InsightStatGrid(
                stats = activityOverviewStats(
                    overviewDays = sortedDays,
                    unitFormatter = unitFormatter,
                ),
                modifier = metricModifier(),
            )
        }
        return
    }

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
            subtitle = "$periodTitle / ${cardioLoadConfidenceLabel(totals.cardioLoadConfidence)}",
            icon = Icons.Outlined.Favorite,
            accentColor = HeartColor,
            chartValues = series.values,
            chartStyle = ActivityMetricChartStyle.LINE,
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
            chartStyle = ActivityMetricChartStyle.BAR,
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
            chartStyle = ActivityMetricChartStyle.BAR,
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
            chartStyle = ActivityMetricChartStyle.BAR,
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
            chartStyle = ActivityMetricChartStyle.LINE,
            chartDays = series.dates,
            selectedRange = selectedRange,
            modifier = metricCardModifier(),
            onClick = onOpenHrv,
        )
    }
}

@Composable
private fun activityOverviewStats(
    overviewDays: List<ActivityOverviewDay>,
    unitFormatter: UnitFormatter,
): List<InsightStat> {
    val steps = overviewDays.sumOf { it.steps }
    val distance = overviewDays.sumOf { it.distanceMeters }
    val energyBurned = overviewDays.sumOf { it.energyBurnedKcal }
    val hasEnergyBurnedData = overviewDays.any { it.energyBurnedSource != CaloriesBurnedSource.NO_DATA }
    val cardioLoads = overviewDays
        .filter { it.cardioLoadConfidence != CardioLoadConfidence.NO_DATA }
        .map { it.cardioLoad }
    val hrvAverage = overviewDays
        .mapNotNull { it.hrvRmssdMs }
        .takeIf { it.isNotEmpty() }
        ?.average()
    val energyDisplay = if (hasEnergyBurnedData) {
        unitFormatter.energy(energyBurned)
    } else {
        DisplayValue(stringResource(R.string.no_data), "")
    }
    val hrvDisplay = hrvAverage
        ?.let(unitFormatter::hrv)
        ?: DisplayValue(stringResource(R.string.no_data), "")

    return listOf(
        InsightStat(
            title = stringResource(R.string.metric_steps),
            value = unitFormatter.count(steps),
            unit = stringResource(R.string.unit_steps),
            icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
            accentColor = StepsColor,
        ),
        unitFormatter.distance(distance).toInsightStat(
            title = stringResource(R.string.metric_distance),
            icon = Icons.Outlined.Straighten,
            accentColor = DistanceColor,
        ),
        energyDisplay.toInsightStat(
            title = stringResource(R.string.metric_energy_burned),
            icon = Icons.Outlined.LocalFireDepartment,
            accentColor = CaloriesColor,
        ),
        DisplayValue(
            value = if (cardioLoads.isNotEmpty()) {
                unitFormatter.count(cardioLoads.sum())
            } else {
                stringResource(R.string.no_data)
            },
            unit = "",
        ).toInsightStat(
            title = stringResource(R.string.metric_cardio_load),
            icon = Icons.Outlined.Favorite,
            accentColor = HeartColor,
        ),
        hrvDisplay.toInsightStat(
            title = stringResource(R.string.metric_hrv),
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = HeartColor,
        ),
    )
}

private fun DisplayValue.toInsightStat(
    title: String,
    icon: ImageVector,
    accentColor: Color,
): InsightStat =
    InsightStat(
        title = title,
        value = value,
        unit = unit,
        icon = icon,
        accentColor = accentColor,
    )

@Composable
private fun activityPeriodTitle(
    selectedRange: TimeRange,
    activityWeekMode: ActivityWeekMode,
    period: DatePeriod,
): String =
    if (selectedRange == TimeRange.WEEK && activityWeekMode == ActivityWeekMode.LAST_7_DAYS) {
        stringResource(R.string.settings_activity_week_last_7_days)
    } else {
        localizedPeriodTitle(selectedRange, period)
    }

@Composable
private fun ActivityOverviewPeriodCard(
    days: List<ActivityOverviewDay>,
    selectedRange: TimeRange,
    activityWeekMode: ActivityWeekMode,
    period: DatePeriod,
) {
    val stripBuckets = activityOverviewBuckets(
        days = days,
        selectedRange = selectedRange,
        maxBuckets = 7,
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(text = activityPeriodTitle(selectedRange, activityWeekMode, period))
        Card(
            modifier = metricModifier(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column {
                ActivityOverviewStrip(
                    buckets = stripBuckets,
                    selectedRange = selectedRange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f))
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun ActivityWorkoutListCard(
    workouts: List<ExerciseData>,
    title: String,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit,
    onDeleteActivity: (String) -> Unit,
) {
    var visibleCount by remember(workouts) {
        mutableIntStateOf(workouts.size.coerceAtMost(ActivityWorkoutListPageSize))
    }
    val boundedVisibleCount = visibleCount.coerceAtMost(workouts.size)
    val visibleWorkouts = workouts.take(boundedVisibleCount)

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(text = stringResource(R.string.section_activities))
        Card(
            modifier = metricModifier(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            if (workouts.isEmpty()) {
                Text(
                    text = stringResource(R.string.message_no_activities_period),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
                return@Card
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
            )
            visibleWorkouts.forEachIndexed { index, workout ->
                ActivityOverviewWorkoutRow(
                    workout = workout,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onClick = { onOpenActivity(workout.id) },
                    onEdit = workout.editAction(onEditActivity),
                    onDelete = workout.deleteAction(onDeleteActivity),
                )
                if (index < visibleWorkouts.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    )
                }
            }
            if (boundedVisibleCount < workouts.size) {
                OutlinedButton(
                    onClick = {
                        visibleCount = (boundedVisibleCount + ActivityWorkoutListPageSize)
                            .coerceAtMost(workouts.size)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(stringResource(R.string.action_load_more_entries))
                }
            }
        }
    }
}

@Composable
private fun ActivityOverviewStrip(
    buckets: List<ActivityOverviewBucket>,
    selectedRange: TimeRange,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        buckets.forEach { bucket ->
            val workout = bucket.workouts.firstOrNull()
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier.size(ActivityOverviewMarkerSize),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        workout != null -> {
                            Box(
                                modifier = Modifier
                                    .size(ActivityOverviewMarkerSize)
                                    .background(WorkoutColor, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = exerciseTypeIcon(workout.exerciseType),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        bucket.hasActivity -> {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(StepsColor.copy(alpha = 0.86f), CircleShape),
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            )
                        }
                    }
                }
                Text(
                    text = activityOverviewBucketLabel(bucket.date, selectedRange, locale),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
private fun ActivityOverviewWorkoutRow(
    workout: ExerciseData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (onDelete != null) {
        SwipeToDeleteEntryRow(
            onDelete = onDelete,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
        ) {
            ActivityOverviewWorkoutRowContent(
                workout = workout,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onClick = onClick,
                onEdit = onEdit,
                opaqueBackground = true,
            )
        }
    } else {
        ActivityOverviewWorkoutRowContent(
            workout = workout,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onClick = onClick,
            onEdit = onEdit,
            modifier = modifier,
        )
    }
}

@Composable
private fun ActivityOverviewWorkoutRowContent(
    workout: ExerciseData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    opaqueBackground: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = workout.startTime.atZone(zone)
    val rowShape = RoundedCornerShape(8.dp)
    val rowModifier = if (opaqueBackground) {
        modifier.background(MaterialTheme.colorScheme.surfaceContainer, rowShape)
    } else {
        modifier
    }
    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(WorkoutColor.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = exerciseTypeIcon(workout.exerciseType),
                contentDescription = null,
                tint = WorkoutColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = workout.title ?: exerciseTypeLabel(workout.exerciseType),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${localizedDayTitle(start.toLocalDate())} / ${dateTimeFormatterProvider.shortTime().format(start)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = unitFormatter.duration(workout.durationMs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.detail_duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onEdit != null) {
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.cd_edit_entry),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActivityMetricCard(
    title: String,
    value: DisplayValue,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    chartValues: List<Double>,
    chartStyle: ActivityMetricChartStyle,
    chartDays: List<LocalDate>,
    selectedRange: TimeRange,
    modifier: Modifier = Modifier,
    subtitleColor: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .height(ActivityOverviewCardHeight)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
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
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = value.value,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (value.unit.isNotBlank()) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = value.unit,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 5.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ActivityMetricSparkline(
                values = chartValues,
                dates = chartDays,
                selectedRange = selectedRange,
                style = chartStyle,
                accentColor = accentColor,
            )
        }
    }
}

@Composable
private fun ActivityMetricSparkline(
    values: List<Double>,
    dates: List<LocalDate>,
    selectedRange: TimeRange,
    style: ActivityMetricChartStyle,
    accentColor: Color,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (style) {
            ActivityMetricChartStyle.BAR -> ActivityMiniBarChart(values, accentColor)
            ActivityMetricChartStyle.LINE -> ActivityMiniLineChart(values, accentColor)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.width(ActivityOverviewChartWidth),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            dates.forEach { date ->
                Text(
                    text = activityOverviewBucketLabel(date, selectedRange, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
private fun ActivityMiniBarChart(values: List<Double>, accentColor: Color) {
    val maxValue = values.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
    Row(
        modifier = Modifier
            .width(ActivityOverviewChartWidth)
            .height(ActivityOverviewChartHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEach { value ->
            val fraction = (value / maxValue).toFloat().coerceIn(0.12f, 1f)
            Box(
                modifier = Modifier
                    .width(ActivityOverviewBarWidth)
                    .height(ActivityOverviewChartHeight * fraction)
                    .background(accentColor.copy(alpha = 0.82f), RoundedCornerShape(ActivityOverviewBarRadius)),
            )
        }
    }
}

@Composable
private fun ActivityMiniLineChart(values: List<Double>, accentColor: Color) {
    Canvas(
        modifier = Modifier
            .width(ActivityOverviewChartWidth)
            .height(ActivityOverviewChartHeight),
    ) {
        val maxValue = values.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
        val minValue = values.minOrNull()?.takeIf { it < maxValue } ?: 0.0
        val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
        val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width / 2f
        val points = values.mapIndexed { index, value ->
            val yFraction = ((value - minValue) / range).toFloat().coerceIn(0f, 1f)
            Offset(
                x = if (values.size > 1) index * stepX else stepX,
                y = size.height - (yFraction * (size.height * 0.78f)) - (size.height * 0.1f),
            )
        }
        drawLine(
            color = accentColor.copy(alpha = 0.55f),
            start = Offset(0f, size.height * 0.72f),
            end = Offset(size.width, size.height * 0.72f),
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
                radius = 5.dp.toPx(),
                center = point,
                style = Stroke(width = 2.dp.toPx()),
            )
            drawCircle(
                color = accentColor,
                radius = 2.5.dp.toPx(),
                center = point,
            )
        }
    }
}

private fun activityOverviewMetricSeries(
    days: List<ActivityOverviewDay>,
    selectedRange: TimeRange,
    aggregation: ActivityOverviewMetricAggregation,
    valueSelector: (ActivityOverviewDay) -> Double?,
): ActivityOverviewMetricSeries {
    val maxBuckets = if (selectedRange == TimeRange.YEAR) 12 else 7
    val buckets = activityOverviewBuckets(days, selectedRange, maxBuckets)
    return ActivityOverviewMetricSeries(
        dates = buckets.map { it.date },
        values = buckets.map { bucket ->
            val values = bucket.days.mapNotNull(valueSelector)
            when {
                values.isEmpty() -> 0.0
                aggregation == ActivityOverviewMetricAggregation.AVERAGE -> values.average()
                else -> values.sum()
            }
        },
    )
}

private fun activityOverviewBuckets(
    days: List<ActivityOverviewDay>,
    selectedRange: TimeRange,
    maxBuckets: Int,
): List<ActivityOverviewBucket> {
    val sortedDays = days.sortedBy { it.date }
    val rawBuckets = when (selectedRange) {
        TimeRange.DAY,
        TimeRange.WEEK,
        TimeRange.MONTH -> sortedDays.map { day ->
            ActivityOverviewBucket(date = day.date, days = listOf(day))
        }

        TimeRange.YEAR -> sortedDays
            .groupBy { YearMonth.from(it.date) }
            .toSortedMap()
            .map { (_, monthDays) ->
                ActivityOverviewBucket(date = monthDays.first().date, days = monthDays)
            }
    }
    return rawBuckets.limitActivityOverviewBuckets(maxBuckets)
}

private fun List<ActivityOverviewBucket>.limitActivityOverviewBuckets(maxBuckets: Int): List<ActivityOverviewBucket> {
    if (maxBuckets <= 0 || isEmpty()) return emptyList()
    if (size <= maxBuckets) return this

    val chunkSize = ceil(size.toDouble() / maxBuckets.toDouble()).toInt().coerceAtLeast(1)
    return chunked(chunkSize).map { bucketChunk ->
        ActivityOverviewBucket(
            date = bucketChunk.first().date,
            days = bucketChunk.flatMap { it.days },
        )
    }
}

private fun activityOverviewBucketLabel(
    date: LocalDate,
    selectedRange: TimeRange,
    locale: java.util.Locale,
): String = when (selectedRange) {
    TimeRange.DAY,
    TimeRange.WEEK -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).take(1)
    TimeRange.MONTH -> date.dayOfMonth.toString()
    TimeRange.YEAR -> date.month.getDisplayName(TextStyle.SHORT, locale).take(1)
}

private fun activityOverviewTotals(days: List<ActivityOverviewDay>): ActivityOverviewTotals {
    val hrvValues = days.mapNotNull { it.hrvRmssdMs }
    val cardioLoadDays = days.filter { it.cardioLoadConfidence != CardioLoadConfidence.NO_DATA }
    return ActivityOverviewTotals(
        steps = days.sumOf { it.steps },
        distanceMeters = days.sumOf { it.distanceMeters },
        energyBurnedKcal = days.sumOf { it.energyBurnedKcal },
        hasEnergyBurnedData = days.any { it.energyBurnedSource != CaloriesBurnedSource.NO_DATA },
        cardioLoad = cardioLoadDays.sumOf { it.cardioLoad },
        hasCardioLoadData = cardioLoadDays.isNotEmpty(),
        cardioLoadConfidence = aggregateCardioLoadConfidence(cardioLoadDays),
        hrvRmssdMs = hrvValues.takeIf { it.isNotEmpty() }?.average(),
    )
}

private fun aggregateCardioLoadConfidence(days: List<ActivityOverviewDay>): CardioLoadConfidence =
    when {
        days.isEmpty() -> CardioLoadConfidence.NO_DATA
        days.any { it.cardioLoadConfidence == CardioLoadConfidence.LOW } -> CardioLoadConfidence.LOW
        days.any { it.cardioLoadConfidence == CardioLoadConfidence.MEDIUM } -> CardioLoadConfidence.MEDIUM
        else -> CardioLoadConfidence.HIGH
    }

@Composable
private fun cardioLoadDisplayValue(
    score: Int,
    hasData: Boolean,
    unitFormatter: UnitFormatter,
): DisplayValue =
    if (hasData) {
        DisplayValue(unitFormatter.count(score), "")
    } else {
        DisplayValue(stringResource(R.string.no_data), "")
    }

@Composable
private fun cardioLoadConfidenceLabel(confidence: CardioLoadConfidence): String =
    stringResource(
        when (confidence) {
            CardioLoadConfidence.HIGH -> R.string.cardio_load_confidence_high
            CardioLoadConfidence.MEDIUM -> R.string.cardio_load_confidence_medium
            CardioLoadConfidence.LOW -> R.string.cardio_load_confidence_low
            CardioLoadConfidence.NO_DATA -> R.string.cardio_load_confidence_no_data
        }
    )

private enum class ActivityMetricChartStyle {
    BAR,
    LINE,
}

private enum class ActivityOverviewMetricAggregation {
    SUM,
    AVERAGE,
}

private data class ActivityOverviewMetricSeries(
    val dates: List<LocalDate>,
    val values: List<Double>,
)

private data class ActivityOverviewBucket(
    val date: LocalDate,
    val days: List<ActivityOverviewDay>,
) {
    val workouts: List<ExerciseData>
        get() = days.flatMap { it.workouts }.distinctBy { it.id }

    val hasActivity: Boolean
        get() = days.any { it.hasActivity }
}

private data class ActivityOverviewTotals(
    val steps: Long,
    val distanceMeters: Double,
    val energyBurnedKcal: Double,
    val hasEnergyBurnedData: Boolean,
    val cardioLoad: Int,
    val hasCardioLoadData: Boolean,
    val cardioLoadConfidence: CardioLoadConfidence,
    val hrvRmssdMs: Double?,
)

private fun metricCardModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)

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
            modifier = metricModifier(),
        )
    }
}

private fun DatePeriod.weekCount(): Double {
    val days = ChronoUnit.DAYS.between(start, end).toDouble() + 1.0
    return (days / 7.0).coerceAtLeast(1.0 / 7.0)
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

private fun ExerciseData.editAction(onEditActivity: (String) -> Unit): (() -> Unit)? =
    if (isOpenVitalsEntry && id.isNotBlank()) {
        { onEditActivity(id) }
    } else {
        null
    }

private fun ExerciseData.deleteAction(onDeleteActivity: (String) -> Unit): (() -> Unit)? =
    if (isOpenVitalsEntry && id.isNotBlank()) {
        { onDeleteActivity(id) }
    } else {
        null
    }
