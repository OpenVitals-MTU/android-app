package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.MetricBarChart
import tech.mmarca.openvitals.ui.theme.ActiveCaloriesColor
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.ElevationColor
import tech.mmarca.openvitals.ui.theme.FloorsColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import kotlin.math.roundToLong

internal fun LazyListScope.stepsContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: ActivityMetricSectionContext,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val display = state.display.metric
    if (display.hasData) {
        val values = display.values
        renderActivityMetricOrderedContent(
            ActivityMetricOrderedContentSpec(
                state = state,
                display = display,
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
                statisticsIcon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                comparisonValueFormatter = {
                    DisplayValue(unitFormatter.count(it.roundToLong()), stringResource(R.string.unit_steps))
                },
                onDecreaseGoal = onDecreaseGoal,
                onIncreaseGoal = onIncreaseGoal,
                intradayChart = {
                    IntradayActivityChartCard(
                        selectedDate = state.selectedDate,
                        title = stringResource(R.string.metric_steps),
                        valueText = "${unitFormatter.count(display.dayTotal.roundToLong())} ${stringResource(R.string.unit_steps)}",
                        emptyText = stringResource(R.string.message_no_step_updates),
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        points = display.intradayPoints.map { it.time to it.value },
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
                        unitFormatter.count(averageOrZero(values.sum(), display.activeDays).roundToLong()),
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

internal fun LazyListScope.distanceContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: ActivityMetricSectionContext,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val display = state.display.metric
    if (display.hasData) {
        val values = display.values
        renderActivityMetricOrderedContent(
            ActivityMetricOrderedContentSpec(
                state = state,
                display = display,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                sectionContext = sectionContext,
                metric = ActivityMetric.DISTANCE,
                accentColor = DistanceColor,
                goalIcon = Icons.Outlined.Straighten,
                goalFormatter = { unitFormatter.distance(it) },
                statisticsIcon = Icons.Outlined.Straighten,
                comparisonValueFormatter = { unitFormatter.distance(it) },
                onDecreaseGoal = onDecreaseGoal,
                onIncreaseGoal = onIncreaseGoal,
                intradayChart = {
                    IntradayActivityChartCard(
                        selectedDate = state.selectedDate,
                        title = stringResource(R.string.metric_distance),
                        valueText = unitFormatter.distance(display.dayTotal).text,
                        emptyText = stringResource(R.string.message_no_distance_updates),
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        points = display.intradayPoints.map { it.time to it.value },
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
                statisticsAverage = { unitFormatter.distance(averageOrZero(values.sum(), display.activeDays)) },
                statisticsBest = { unitFormatter.distance(values.maxOrNull() ?: 0.0) },
            ),
        )
    } else if (!state.isLoading) {
        noMetricData(R.string.metric_distance, R.string.message_no_distance_updates, Icons.Outlined.Straighten, DistanceColor)
    }
}

internal fun LazyListScope.caloriesContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: ActivityMetricSectionContext,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val display = state.display.metric
    if (display.hasData) {
        val values = display.values
        renderActivityMetricOrderedContent(
            ActivityMetricOrderedContentSpec(
                state = state,
                display = display,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                sectionContext = sectionContext,
                metric = ActivityMetric.CALORIES_BURNED,
                accentColor = CaloriesColor,
                goalIcon = Icons.Outlined.LocalFireDepartment,
                goalFormatter = { unitFormatter.energy(it) },
                statisticsIcon = Icons.Outlined.LocalFireDepartment,
                comparisonValueFormatter = { unitFormatter.energy(it) },
                onDecreaseGoal = onDecreaseGoal,
                onIncreaseGoal = onIncreaseGoal,
                intradayChart = {
                    IntradayActivityChartCard(
                        selectedDate = state.selectedDate,
                        title = stringResource(R.string.metric_calories_burned),
                        valueText = unitFormatter.energy(display.dayTotal).text,
                        emptyText = stringResource(R.string.message_no_calories_burned),
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        points = display.intradayPoints.map { it.time to it.value },
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
                statisticsAverage = { unitFormatter.energy(averageOrZero(values.sum(), display.activeDays)) },
                statisticsBest = { unitFormatter.energy(values.maxOrNull() ?: 0.0) },
            ),
        )
    } else if (!state.isLoading) {
        noMetricData(R.string.metric_calories_burned, R.string.message_no_calories_burned, Icons.Outlined.LocalFireDepartment, CaloriesColor)
    }
}

internal fun LazyListScope.activeCaloriesContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: ActivityMetricSectionContext,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val display = state.display.metric
    if (display.hasData) {
        val values = display.values
        renderActivityMetricOrderedContent(
            ActivityMetricOrderedContentSpec(
                state = state,
                display = display,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                sectionContext = sectionContext,
                metric = ActivityMetric.ACTIVE_CALORIES,
                accentColor = ActiveCaloriesColor,
                goalIcon = Icons.Outlined.LocalFireDepartment,
                goalFormatter = { unitFormatter.energy(it) },
                statisticsIcon = Icons.Outlined.LocalFireDepartment,
                comparisonValueFormatter = { unitFormatter.energy(it) },
                onDecreaseGoal = onDecreaseGoal,
                onIncreaseGoal = onIncreaseGoal,
                intradayChart = {
                    IntradayActivityChartCard(
                        selectedDate = state.selectedDate,
                        title = stringResource(R.string.metric_active_calories),
                        valueText = unitFormatter.energy(display.dayTotal).text,
                        emptyText = stringResource(R.string.message_no_active_calories),
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        points = display.intradayPoints.map { it.time to it.value },
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
                statisticsAverage = { unitFormatter.energy(averageOrZero(values.sum(), display.activeDays)) },
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

internal fun LazyListScope.floorsContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: ActivityMetricSectionContext,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val display = state.display.metric
    if (display.hasData) {
        val values = display.values
        renderActivityMetricOrderedContent(
            ActivityMetricOrderedContentSpec(
                state = state,
                display = display,
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
                statisticsIcon = Icons.Outlined.Stairs,
                comparisonValueFormatter = {
                    DisplayValue(unitFormatter.count(it.roundToLong()), stringResource(R.string.unit_floors))
                },
                onDecreaseGoal = onDecreaseGoal,
                onIncreaseGoal = onIncreaseGoal,
                intradayChart = {
                    IntradayActivityChartCard(
                        selectedDate = state.selectedDate,
                        title = stringResource(R.string.metric_floors_climbed),
                        valueText = "${unitFormatter.count(display.dayTotal.roundToLong())} ${stringResource(R.string.unit_floors)}",
                        emptyText = stringResource(R.string.message_no_floors_climbed),
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        points = display.intradayPoints.map { it.time to it.value },
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
                        unitFormatter.count(averageOrZero(values.sum(), display.activeDays).roundToLong()),
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

internal fun LazyListScope.elevationContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: ActivityMetricSectionContext,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val display = state.display.metric
    if (display.hasData) {
        val values = display.values
        renderActivityMetricOrderedContent(
            ActivityMetricOrderedContentSpec(
                state = state,
                display = display,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                sectionContext = sectionContext,
                metric = ActivityMetric.ELEVATION,
                accentColor = ElevationColor,
                goalIcon = Icons.Outlined.Terrain,
                goalFormatter = { unitFormatter.elevation(it) },
                statisticsIcon = Icons.Outlined.Terrain,
                comparisonValueFormatter = { unitFormatter.elevation(it) },
                onDecreaseGoal = onDecreaseGoal,
                onIncreaseGoal = onIncreaseGoal,
                intradayChart = {
                    IntradayActivityChartCard(
                        selectedDate = state.selectedDate,
                        title = stringResource(R.string.metric_elevation_gained),
                        valueText = unitFormatter.elevation(display.dayTotal).text,
                        emptyText = stringResource(R.string.message_no_elevation),
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        points = display.intradayPoints.map { it.time to it.value },
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
                statisticsAverage = { unitFormatter.elevation(averageOrZero(values.sum(), display.activeDays)) },
                statisticsBest = { unitFormatter.elevation(values.maxOrNull() ?: 0.0) },
            ),
        )
    } else if (!state.isLoading) {
        noMetricData(R.string.metric_elevation_gained, R.string.message_no_elevation, Icons.Outlined.Terrain, ElevationColor)
    }
}
