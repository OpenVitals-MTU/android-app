package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.DailyGoalDirection
import tech.mmarca.openvitals.domain.insights.DailyGoalValue
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionOrderViewModel
import tech.mmarca.openvitals.domain.insights.dailyGoalProgress
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.DailyGoalCard
import tech.mmarca.openvitals.ui.components.DailyGoalStatistics
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricBarChart
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState
import tech.mmarca.openvitals.ui.theme.ActiveCaloriesColor
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.ElevationColor
import tech.mmarca.openvitals.ui.theme.FloorsColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import java.time.LocalDate
import kotlin.math.roundToLong

enum class ActivityMetric {
    STEPS,
    DISTANCE,
    CALORIES_BURNED,
    ACTIVE_CALORIES,
    FLOORS,
    ELEVATION,
    WHEELCHAIR_PUSHES,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepsScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    ActivityMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = ActivityMetric.STEPS,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun DistanceScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    ActivityMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = ActivityMetric.DISTANCE,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun CaloriesOutScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    ActivityMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = ActivityMetric.CALORIES_BURNED,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun ActiveCaloriesScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    ActivityMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = ActivityMetric.ACTIVE_CALORIES,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun FloorsScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    ActivityMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = ActivityMetric.FLOORS,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun ElevationScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    ActivityMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = ActivityMetric.ELEVATION,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun WheelchairPushesScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    ActivityMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = ActivityMetric.WHEELCHAIR_PUSHES,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ActivityMetricScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    metric: ActivityMetric,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sectionOrderViewModel = hiltViewModel<MetricDetailSectionOrderViewModel>()
    val sectionOrder by sectionOrderViewModel.sectionOrder.collectAsStateWithLifecycle()
    val isEditingSections by sectionOrderViewModel.isEditingSections.collectAsStateWithLifecycle()
    val sectionListState = rememberMetricDetailSectionListState()
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate, metric)
    val sectionContext = ActivityMetricSectionContext(
        listState = sectionListState,
        order = sectionOrder,
        isEditingSections = isEditingSections,
        onMoveSectionToTarget = sectionOrderViewModel::moveSectionToTarget,
        onMoveSection = sectionOrderViewModel::moveSection,
    )

    LaunchedEffect(isEditingSections) {
        onSectionEditStateChanged(isEditingSections, sectionOrderViewModel::toggleSectionEdit)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isEditingSections) {
                sectionOrderViewModel.toggleSectionEdit()
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod()
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.ACTIVITY,
        isLoading = state.isLoading,
        showInlineSyncBanner = false,
    ) { hcUx ->
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
            syncPaused = hcUx.syncPaused,
            sectionListState = sectionListState,
        ) { period ->
        when (metric) {
            ActivityMetric.STEPS -> stepsContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                chartDaySelection,
                sectionContext,
                viewModel::decreaseDailyGoal,
                viewModel::increaseDailyGoal,
            )
            ActivityMetric.DISTANCE -> distanceContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                chartDaySelection,
                sectionContext,
                viewModel::decreaseDailyGoal,
                viewModel::increaseDailyGoal,
            )
            ActivityMetric.CALORIES_BURNED -> caloriesContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                chartDaySelection,
                sectionContext,
                viewModel::decreaseDailyGoal,
                viewModel::increaseDailyGoal,
            )
            ActivityMetric.ACTIVE_CALORIES -> activeCaloriesContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                chartDaySelection,
                sectionContext,
                viewModel::decreaseDailyGoal,
                viewModel::increaseDailyGoal,
            )
            ActivityMetric.FLOORS -> floorsContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                chartDaySelection,
                sectionContext,
                viewModel::decreaseDailyGoal,
                viewModel::increaseDailyGoal,
            )
            ActivityMetric.ELEVATION -> elevationContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                chartDaySelection,
                sectionContext,
                viewModel::decreaseDailyGoal,
                viewModel::increaseDailyGoal,
            )
            ActivityMetric.WHEELCHAIR_PUSHES -> wheelchairPushesContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                chartDaySelection,
                sectionContext,
                viewModel::decreaseDailyGoal,
                viewModel::increaseDailyGoal,
            )
        }
    }
    }
}

private fun LazyListScope.stepsContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: ActivityMetricSectionContext,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    if (state.selectedRange == TimeRange.DAY || state.dailySteps.isNotEmpty()) {
        val values = state.dailySteps.map { it.steps.toDouble() }
        renderActivityMetricOrderedContent(
            ActivityMetricOrderedContentSpec(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                sectionContext = sectionContext,
                metric = ActivityMetric.STEPS,
                accentColor = StepsColor,
                goalIcon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                goalFormatter = {
                    DisplayValue(unitFormatter.count(it.roundToLong()), stringResource(R.string.unit_steps))
                },
                goalValues = state.dailySteps.map { DailyGoalValue(it.date, it.steps.toDouble()) },
                trackedDates = state.dailySteps.filter { it.steps > 0L }.map { it.date },
                sampleCount = if (state.selectedRange == TimeRange.DAY) {
                    state.activityProgress.count { it.totalSteps > 0L }
                } else {
                    values.count { it > 0.0 }
                },
                values = values,
                previousTotal = state.previousDailySteps.sumOf { it.steps }.toDouble(),
                baselineValues = state.baselineDailySteps.map { BaselineValue(it.date, it.steps.toDouble()) },
                statisticsIcon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                comparisonValueFormatter = {
                    DisplayValue(unitFormatter.count(it.roundToLong()), stringResource(R.string.unit_steps))
                },
                activeDays = values.count { it > 0.0 },
                onDecreaseGoal = onDecreaseGoal,
                onIncreaseGoal = onIncreaseGoal,
                intradayChart = {
                    IntradayActivityChartCard(
                        selectedDate = state.selectedDate,
                        title = stringResource(R.string.metric_steps),
                        valueText = "${unitFormatter.count(state.dailySteps.firstOrNull()?.steps ?: 0L)} ${stringResource(R.string.unit_steps)}",
                        emptyText = stringResource(R.string.message_no_step_updates),
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        points = state.activityProgress.map { point ->
                            point.time to point.totalSteps.toDouble()
                        },
                        accentColor = StepsColor,
                        yAxisValueFormatter = { unitFormatter.count(it.roundToLong()) },
                        modifier = activityMetricModifier(),
                    )
                },
                periodChart = {
                    MetricBarChart(
                        title = stringResource(R.string.metric_steps),
                        data = state.dailySteps,
                        selectedRange = state.selectedRange,
                        period = period,
                        accentColor = StepsColor,
                        summaryValue = "${unitFormatter.count(state.dailySteps.sumOf { it.steps })} ${stringResource(R.string.unit_steps)}",
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = activityMetricModifier(),
                        selectedDate = chartDaySelection.selectedDate,
                        onDateSelected = chartDaySelection.onDateSelected,
                        date = { it.date },
                        value = { it.steps.toDouble() },
                        valueFormatter = { unitFormatter.count(it.roundToLong()) },
                    )
                },
                selectedDayEntriesContent = { selectedDate ->
                    ActivityDailyEntriesContent(
                        entries = state.dailySteps.filter { it.steps > 0L && it.date == selectedDate },
                        date = { it.date },
                        value = { DisplayValue(unitFormatter.count(it.steps), stringResource(R.string.unit_steps)) },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = StepsColor,
                        titleDate = selectedDate,
                    )
                },
                entriesContent = {
                    ActivityDailyEntriesContent(
                        entries = state.dailySteps.filter { it.steps > 0L },
                        date = { it.date },
                        value = { DisplayValue(unitFormatter.count(it.steps), stringResource(R.string.unit_steps)) },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = StepsColor,
                    )
                },
                statisticsTotal = {
                    DisplayValue(unitFormatter.count(values.sum().roundToLong()), stringResource(R.string.unit_steps))
                },
                statisticsAverage = {
                    DisplayValue(
                        unitFormatter.count(averageOrZero(values.sum(), values.count { it > 0.0 }).roundToLong()),
                        stringResource(R.string.unit_steps),
                    )
                },
                statisticsBest = {
                    DisplayValue(unitFormatter.count((values.maxOrNull() ?: 0.0).roundToLong()), stringResource(R.string.unit_steps))
                },
            ),
        )
    } else if (!state.isLoading) {
        noMetricData(R.string.metric_steps, R.string.message_no_step_updates, Icons.AutoMirrored.Outlined.DirectionsWalk, StepsColor)
    }
}

private fun LazyListScope.distanceContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: ActivityMetricSectionContext,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    if (state.selectedRange == TimeRange.DAY || state.dailySteps.any { it.distanceMeters > 0.0 }) {
        val values = state.dailySteps.map { it.distanceMeters }
        renderActivityMetricOrderedContent(
            ActivityMetricOrderedContentSpec(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                sectionContext = sectionContext,
                metric = ActivityMetric.DISTANCE,
                accentColor = DistanceColor,
                goalIcon = Icons.Outlined.Straighten,
                goalFormatter = { unitFormatter.distance(it) },
                goalValues = state.dailySteps.map { DailyGoalValue(it.date, it.distanceMeters) },
                trackedDates = state.dailySteps.filter { it.distanceMeters > 0.0 }.map { it.date },
                sampleCount = if (state.selectedRange == TimeRange.DAY) {
                    state.activityProgress.count { it.totalDistanceMeters != null }
                } else {
                    values.count { it > 0.0 }
                },
                values = values,
                previousTotal = state.previousDailySteps.sumOf { it.distanceMeters },
                baselineValues = state.baselineDailySteps.map { BaselineValue(it.date, it.distanceMeters) },
                statisticsIcon = Icons.Outlined.Straighten,
                comparisonValueFormatter = { unitFormatter.distance(it) },
                activeDays = values.count { it > 0.0 },
                onDecreaseGoal = onDecreaseGoal,
                onIncreaseGoal = onIncreaseGoal,
                intradayChart = {
                    val distanceTotal = state.dailySteps.firstOrNull()?.distanceMeters ?: 0.0
                    IntradayActivityChartCard(
                        selectedDate = state.selectedDate,
                        title = stringResource(R.string.metric_distance),
                        valueText = unitFormatter.distance(distanceTotal).text,
                        emptyText = stringResource(R.string.message_no_distance_updates),
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        points = state.activityProgress.mapNotNull { point ->
                            point.totalDistanceMeters?.let { point.time to it }
                        },
                        accentColor = DistanceColor,
                        yAxisValueFormatter = { unitFormatter.distance(it).text },
                        modifier = activityMetricModifier(),
                    )
                },
                periodChart = {
                    MetricBarChart(
                        title = stringResource(R.string.metric_distance),
                        data = state.dailySteps,
                        selectedRange = state.selectedRange,
                        period = period,
                        summaryValue = unitFormatter.distance(state.dailySteps.sumOf { it.distanceMeters }).text,
                        accentColor = DistanceColor,
                        accentAlpha = 0.8f,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = activityMetricModifier(),
                        selectedDate = chartDaySelection.selectedDate,
                        onDateSelected = chartDaySelection.onDateSelected,
                        date = { it.date },
                        value = { it.distanceMeters },
                        valueFormatter = { unitFormatter.distance(it).text },
                    )
                },
                selectedDayEntriesContent = { selectedDate ->
                    ActivityDailyEntriesContent(
                        entries = state.dailySteps.filter { it.distanceMeters > 0.0 && it.date == selectedDate },
                        date = { it.date },
                        value = { unitFormatter.distance(it.distanceMeters) },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = DistanceColor,
                        titleDate = selectedDate,
                    )
                },
                entriesContent = {
                    ActivityDailyEntriesContent(
                        entries = state.dailySteps.filter { it.distanceMeters > 0.0 },
                        date = { it.date },
                        value = { unitFormatter.distance(it.distanceMeters) },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = DistanceColor,
                    )
                },
                statisticsTotal = { unitFormatter.distance(values.sum()) },
                statisticsAverage = { unitFormatter.distance(averageOrZero(values.sum(), values.count { it > 0.0 })) },
                statisticsBest = { unitFormatter.distance(values.maxOrNull() ?: 0.0) },
            ),
        )
    } else if (!state.isLoading) {
        noMetricData(R.string.metric_distance, R.string.message_no_distance_updates, Icons.Outlined.Straighten, DistanceColor)
    }
}

private fun LazyListScope.caloriesContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: ActivityMetricSectionContext,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    if (state.selectedRange == TimeRange.DAY || state.nutrition.any { it.hasCaloriesBurnedData }) {
        val values = state.nutrition.map { it.caloriesBurnedKcal }
        renderActivityMetricOrderedContent(
            ActivityMetricOrderedContentSpec(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                sectionContext = sectionContext,
                metric = ActivityMetric.CALORIES_BURNED,
                accentColor = CaloriesColor,
                goalIcon = Icons.Outlined.LocalFireDepartment,
                goalFormatter = { unitFormatter.energy(it) },
                goalValues = state.nutrition.map { DailyGoalValue(it.date, it.caloriesBurnedKcal) },
                trackedDates = state.nutrition.filter { it.hasCaloriesBurnedData }.map { it.date },
                sampleCount = if (state.selectedRange == TimeRange.DAY) {
                    state.activityProgress.count { it.totalCaloriesBurnedKcal != null }
                } else {
                    state.nutrition.count { it.hasCaloriesBurnedData }
                },
                values = values,
                previousTotal = state.previousNutrition.sumOf { it.caloriesBurnedKcal },
                baselineValues = state.baselineNutrition.map { BaselineValue(it.date, it.caloriesBurnedKcal) },
                statisticsIcon = Icons.Outlined.LocalFireDepartment,
                comparisonValueFormatter = { unitFormatter.energy(it) },
                activeDays = values.count { it > 0.0 },
                onDecreaseGoal = onDecreaseGoal,
                onIncreaseGoal = onIncreaseGoal,
                intradayChart = {
                    val caloriesTotal = state.nutrition.firstOrNull()?.caloriesBurnedKcal ?: 0.0
                    IntradayActivityChartCard(
                        selectedDate = state.selectedDate,
                        title = stringResource(R.string.metric_calories_burned),
                        valueText = unitFormatter.energy(caloriesTotal).text,
                        emptyText = stringResource(R.string.message_no_calories_burned),
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        points = state.activityProgress.mapNotNull { point ->
                            point.totalCaloriesBurnedKcal?.let { point.time to it }
                        },
                        accentColor = CaloriesColor,
                        yAxisValueFormatter = { unitFormatter.energy(it).text },
                        modifier = activityMetricModifier(),
                    )
                },
                periodChart = {
                    MetricBarChart(
                        title = stringResource(R.string.metric_calories_burned),
                        data = state.nutrition,
                        selectedRange = state.selectedRange,
                        period = period,
                        summaryValue = unitFormatter.energy(state.nutrition.sumOf { it.caloriesBurnedKcal }).text,
                        accentColor = CaloriesColor,
                        accentAlpha = 0.8f,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = activityMetricModifier(),
                        selectedDate = chartDaySelection.selectedDate,
                        onDateSelected = chartDaySelection.onDateSelected,
                        date = { it.date },
                        value = { it.caloriesBurnedKcal },
                        valueFormatter = { unitFormatter.energy(it).text },
                    )
                },
                selectedDayEntriesContent = { selectedDate ->
                    ActivityDailyEntriesContent(
                        entries = state.nutrition.filter { it.hasCaloriesBurnedData && it.date == selectedDate },
                        date = { it.date },
                        value = { unitFormatter.energy(it.caloriesBurnedKcal) },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = CaloriesColor,
                        titleDate = selectedDate,
                    )
                },
                entriesContent = {
                    ActivityDailyEntriesContent(
                        entries = state.nutrition.filter { it.hasCaloriesBurnedData },
                        date = { it.date },
                        value = { unitFormatter.energy(it.caloriesBurnedKcal) },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = CaloriesColor,
                    )
                },
                statisticsTotal = { unitFormatter.energy(values.sum()) },
                statisticsAverage = { unitFormatter.energy(averageOrZero(values.sum(), values.count { it > 0.0 })) },
                statisticsBest = { unitFormatter.energy(values.maxOrNull() ?: 0.0) },
            ),
        )
    } else if (!state.isLoading) {
        noMetricData(R.string.metric_calories_burned, R.string.message_no_calories_burned, Icons.Outlined.LocalFireDepartment, CaloriesColor)
    }
}

private fun LazyListScope.activeCaloriesContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: ActivityMetricSectionContext,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    if (state.selectedRange == TimeRange.DAY || state.dailySteps.any { it.activeCaloriesKcal != null }) {
        val values = state.dailySteps.map { it.activeCaloriesKcal ?: 0.0 }
        renderActivityMetricOrderedContent(
            ActivityMetricOrderedContentSpec(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                sectionContext = sectionContext,
                metric = ActivityMetric.ACTIVE_CALORIES,
                accentColor = ActiveCaloriesColor,
                goalIcon = Icons.Outlined.LocalFireDepartment,
                goalFormatter = { unitFormatter.energy(it) },
                goalValues = state.dailySteps.map { DailyGoalValue(it.date, it.activeCaloriesKcal ?: 0.0) },
                trackedDates = state.dailySteps.filter { (it.activeCaloriesKcal ?: 0.0) > 0.0 }.map { it.date },
                sampleCount = if (state.selectedRange == TimeRange.DAY) {
                    state.activityProgress.count { it.totalActiveCaloriesKcal != null }
                } else {
                    values.count { it > 0.0 }
                },
                values = values,
                previousTotal = state.previousDailySteps.sumOf { it.activeCaloriesKcal ?: 0.0 },
                baselineValues = state.baselineDailySteps.map { BaselineValue(it.date, it.activeCaloriesKcal ?: 0.0) },
                statisticsIcon = Icons.Outlined.LocalFireDepartment,
                comparisonValueFormatter = { unitFormatter.energy(it) },
                activeDays = values.count { it > 0.0 },
                onDecreaseGoal = onDecreaseGoal,
                onIncreaseGoal = onIncreaseGoal,
                intradayChart = {
                    val activeCaloriesTotal = state.dailySteps.firstOrNull()?.activeCaloriesKcal ?: 0.0
                    IntradayActivityChartCard(
                        selectedDate = state.selectedDate,
                        title = stringResource(R.string.metric_active_calories),
                        valueText = unitFormatter.energy(activeCaloriesTotal).text,
                        emptyText = stringResource(R.string.message_no_active_calories),
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        points = state.activityProgress.mapNotNull { point ->
                            point.totalActiveCaloriesKcal?.let { point.time to it }
                        },
                        accentColor = ActiveCaloriesColor,
                        yAxisValueFormatter = { unitFormatter.energy(it).text },
                        modifier = activityMetricModifier(),
                    )
                },
                periodChart = {
                    MetricBarChart(
                        title = stringResource(R.string.metric_active_calories),
                        data = state.dailySteps,
                        selectedRange = state.selectedRange,
                        period = period,
                        summaryValue = unitFormatter.energy(state.dailySteps.sumOf { it.activeCaloriesKcal ?: 0.0 }).text,
                        accentColor = ActiveCaloriesColor,
                        accentAlpha = 0.8f,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = activityMetricModifier(),
                        selectedDate = chartDaySelection.selectedDate,
                        onDateSelected = chartDaySelection.onDateSelected,
                        date = { it.date },
                        value = { it.activeCaloriesKcal ?: 0.0 },
                        valueFormatter = { unitFormatter.energy(it).text },
                    )
                },
                selectedDayEntriesContent = { selectedDate ->
                    ActivityDailyEntriesContent(
                        entries = state.dailySteps.filter { it.activeCaloriesKcal != null && it.date == selectedDate },
                        date = { it.date },
                        value = { unitFormatter.energy(it.activeCaloriesKcal ?: 0.0) },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = ActiveCaloriesColor,
                        titleDate = selectedDate,
                    )
                },
                entriesContent = {
                    ActivityDailyEntriesContent(
                        entries = state.dailySteps.filter { it.activeCaloriesKcal != null },
                        date = { it.date },
                        value = { unitFormatter.energy(it.activeCaloriesKcal ?: 0.0) },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = ActiveCaloriesColor,
                    )
                },
                statisticsTotal = { unitFormatter.energy(values.sum()) },
                statisticsAverage = { unitFormatter.energy(averageOrZero(values.sum(), values.count { it > 0.0 })) },
                statisticsBest = { unitFormatter.energy(values.maxOrNull() ?: 0.0) },
            ),
        )
    } else if (!state.isLoading) {
        noMetricData(
            R.string.metric_active_calories,
            R.string.message_no_active_calories,
            Icons.Outlined.LocalFireDepartment,
            ActiveCaloriesColor,
        )
    }
}

private fun LazyListScope.floorsContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: ActivityMetricSectionContext,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    if (state.selectedRange == TimeRange.DAY || state.dailySteps.any { it.floorsClimbed != null }) {
        val values = state.dailySteps.map { (it.floorsClimbed ?: 0).toDouble() }
        renderActivityMetricOrderedContent(
            ActivityMetricOrderedContentSpec(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                sectionContext = sectionContext,
                metric = ActivityMetric.FLOORS,
                accentColor = FloorsColor,
                goalIcon = Icons.Outlined.Stairs,
                goalFormatter = {
                    DisplayValue(unitFormatter.count(it.roundToLong()), stringResource(R.string.unit_floors))
                },
                goalValues = state.dailySteps.map { DailyGoalValue(it.date, (it.floorsClimbed ?: 0).toDouble()) },
                trackedDates = state.dailySteps.filter { (it.floorsClimbed ?: 0) > 0 }.map { it.date },
                sampleCount = if (state.selectedRange == TimeRange.DAY) {
                    state.activityProgress.count { it.totalFloorsClimbed != null }
                } else {
                    values.count { it > 0.0 }
                },
                values = values,
                previousTotal = state.previousDailySteps.sumOf { (it.floorsClimbed ?: 0).toDouble() },
                baselineValues = state.baselineDailySteps.map { BaselineValue(it.date, (it.floorsClimbed ?: 0).toDouble()) },
                statisticsIcon = Icons.Outlined.Stairs,
                comparisonValueFormatter = {
                    DisplayValue(unitFormatter.count(it.roundToLong()), stringResource(R.string.unit_floors))
                },
                activeDays = values.count { it > 0.0 },
                onDecreaseGoal = onDecreaseGoal,
                onIncreaseGoal = onIncreaseGoal,
                intradayChart = {
                    val floorsTotal = state.dailySteps.firstOrNull()?.floorsClimbed ?: 0
                    IntradayActivityChartCard(
                        selectedDate = state.selectedDate,
                        title = stringResource(R.string.metric_floors_climbed),
                        valueText = "${unitFormatter.count(floorsTotal)} ${stringResource(R.string.unit_floors)}",
                        emptyText = stringResource(R.string.message_no_floors_climbed),
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        points = state.activityProgress.mapNotNull { point ->
                            point.totalFloorsClimbed?.let { point.time to it.toDouble() }
                        },
                        accentColor = FloorsColor,
                        yAxisValueFormatter = { unitFormatter.count(it.roundToLong()) },
                        modifier = activityMetricModifier(),
                    )
                },
                periodChart = {
                    MetricBarChart(
                        title = stringResource(R.string.metric_floors_climbed),
                        data = state.dailySteps,
                        selectedRange = state.selectedRange,
                        period = period,
                        summaryValue = "${unitFormatter.count(state.dailySteps.sumOf { it.floorsClimbed ?: 0 })} ${stringResource(R.string.unit_floors)}",
                        accentColor = FloorsColor,
                        accentAlpha = 0.8f,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = activityMetricModifier(),
                        selectedDate = chartDaySelection.selectedDate,
                        onDateSelected = chartDaySelection.onDateSelected,
                        date = { it.date },
                        value = { it.floorsClimbed?.toDouble() ?: 0.0 },
                        valueFormatter = { unitFormatter.count(it.roundToLong()) },
                    )
                },
                selectedDayEntriesContent = { selectedDate ->
                    ActivityDailyEntriesContent(
                        entries = state.dailySteps.filter { it.floorsClimbed != null && it.date == selectedDate },
                        date = { it.date },
                        value = {
                            DisplayValue(unitFormatter.count((it.floorsClimbed ?: 0).toLong()), stringResource(R.string.unit_floors))
                        },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = FloorsColor,
                        titleDate = selectedDate,
                    )
                },
                entriesContent = {
                    ActivityDailyEntriesContent(
                        entries = state.dailySteps.filter { it.floorsClimbed != null },
                        date = { it.date },
                        value = {
                            DisplayValue(unitFormatter.count((it.floorsClimbed ?: 0).toLong()), stringResource(R.string.unit_floors))
                        },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = FloorsColor,
                    )
                },
                statisticsTotal = {
                    DisplayValue(unitFormatter.count(values.sum().roundToLong()), stringResource(R.string.unit_floors))
                },
                statisticsAverage = {
                    DisplayValue(
                        unitFormatter.count(averageOrZero(values.sum(), values.count { it > 0.0 }).roundToLong()),
                        stringResource(R.string.unit_floors),
                    )
                },
                statisticsBest = {
                    DisplayValue(unitFormatter.count((values.maxOrNull() ?: 0.0).roundToLong()), stringResource(R.string.unit_floors))
                },
            ),
        )
    } else if (!state.isLoading) {
        noMetricData(R.string.metric_floors_climbed, R.string.message_no_floors_climbed, Icons.Outlined.Stairs, FloorsColor)
    }
}

private fun LazyListScope.elevationContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: ActivityMetricSectionContext,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    if (state.selectedRange == TimeRange.DAY || state.dailySteps.any { it.elevationGainedMeters != null }) {
        val values = state.dailySteps.map { it.elevationGainedMeters ?: 0.0 }
        renderActivityMetricOrderedContent(
            ActivityMetricOrderedContentSpec(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                sectionContext = sectionContext,
                metric = ActivityMetric.ELEVATION,
                accentColor = ElevationColor,
                goalIcon = Icons.Outlined.Terrain,
                goalFormatter = { unitFormatter.elevation(it) },
                goalValues = state.dailySteps.map { DailyGoalValue(it.date, it.elevationGainedMeters ?: 0.0) },
                trackedDates = state.dailySteps.filter { (it.elevationGainedMeters ?: 0.0) > 0.0 }.map { it.date },
                sampleCount = if (state.selectedRange == TimeRange.DAY) {
                    state.activityProgress.count { it.totalElevationGainedMeters != null }
                } else {
                    values.count { it > 0.0 }
                },
                values = values,
                previousTotal = state.previousDailySteps.sumOf { it.elevationGainedMeters ?: 0.0 },
                baselineValues = state.baselineDailySteps.map { BaselineValue(it.date, it.elevationGainedMeters ?: 0.0) },
                statisticsIcon = Icons.Outlined.Terrain,
                comparisonValueFormatter = { unitFormatter.elevation(it) },
                activeDays = values.count { it > 0.0 },
                onDecreaseGoal = onDecreaseGoal,
                onIncreaseGoal = onIncreaseGoal,
                intradayChart = {
                    val elevationTotal = state.dailySteps.firstOrNull()?.elevationGainedMeters ?: 0.0
                    IntradayActivityChartCard(
                        selectedDate = state.selectedDate,
                        title = stringResource(R.string.metric_elevation_gained),
                        valueText = unitFormatter.elevation(elevationTotal).text,
                        emptyText = stringResource(R.string.message_no_elevation),
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        points = state.activityProgress.mapNotNull { point ->
                            point.totalElevationGainedMeters?.let { point.time to it }
                        },
                        accentColor = ElevationColor,
                        yAxisValueFormatter = { unitFormatter.elevation(it).text },
                        modifier = activityMetricModifier(),
                    )
                },
                periodChart = {
                    MetricBarChart(
                        title = stringResource(R.string.metric_elevation_gained),
                        data = state.dailySteps,
                        selectedRange = state.selectedRange,
                        period = period,
                        summaryValue = unitFormatter.elevation(state.dailySteps.sumOf { it.elevationGainedMeters ?: 0.0 }).text,
                        accentColor = ElevationColor,
                        accentAlpha = 0.8f,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = activityMetricModifier(),
                        selectedDate = chartDaySelection.selectedDate,
                        onDateSelected = chartDaySelection.onDateSelected,
                        date = { it.date },
                        value = { it.elevationGainedMeters ?: 0.0 },
                        valueFormatter = { unitFormatter.elevation(it).text },
                    )
                },
                selectedDayEntriesContent = { selectedDate ->
                    ActivityDailyEntriesContent(
                        entries = state.dailySteps.filter { it.elevationGainedMeters != null && it.date == selectedDate },
                        date = { it.date },
                        value = { unitFormatter.elevation(it.elevationGainedMeters ?: 0.0) },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = ElevationColor,
                        titleDate = selectedDate,
                    )
                },
                entriesContent = {
                    ActivityDailyEntriesContent(
                        entries = state.dailySteps.filter { it.elevationGainedMeters != null },
                        date = { it.date },
                        value = { unitFormatter.elevation(it.elevationGainedMeters ?: 0.0) },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = ElevationColor,
                    )
                },
                statisticsTotal = { unitFormatter.elevation(values.sum()) },
                statisticsAverage = { unitFormatter.elevation(averageOrZero(values.sum(), values.count { it > 0.0 })) },
                statisticsBest = { unitFormatter.elevation(values.maxOrNull() ?: 0.0) },
            ),
        )
    } else if (!state.isLoading) {
        noMetricData(R.string.metric_elevation_gained, R.string.message_no_elevation, Icons.Outlined.Terrain, ElevationColor)
    }
}


