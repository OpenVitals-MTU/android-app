package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.theme.ActiveCaloriesColor
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.ElevationColor
import tech.mmarca.openvitals.ui.theme.FloorsColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import kotlin.math.roundToLong

enum class ActivityMetric {
    STEPS,
    DISTANCE,
    CALORIES_BURNED,
    ACTIVE_CALORIES,
    FLOORS,
    ELEVATION,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    metric: ActivityMetric = ActivityMetric.STEPS,
) {
    val state by viewModel.uiState.collectAsState()

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
        when (metric) {
            ActivityMetric.STEPS -> stepsContent(state, period, unitFormatter, dateTimeFormatterProvider)
            ActivityMetric.DISTANCE -> distanceContent(state, period, unitFormatter, dateTimeFormatterProvider)
            ActivityMetric.CALORIES_BURNED -> caloriesContent(state, period, unitFormatter, dateTimeFormatterProvider)
            ActivityMetric.ACTIVE_CALORIES -> activeCaloriesContent(state, period, unitFormatter, dateTimeFormatterProvider)
            ActivityMetric.FLOORS -> floorsContent(state, period, unitFormatter, dateTimeFormatterProvider)
            ActivityMetric.ELEVATION -> elevationContent(state, period, unitFormatter, dateTimeFormatterProvider)
        }
    }
}

private fun LazyListScope.stepsContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    if (state.selectedRange == TimeRange.DAY || state.dailySteps.isNotEmpty()) {
        item {
            if (state.selectedRange == TimeRange.DAY) {
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
                    modifier = metricModifier(),
                )
            } else {
                StepsBarChart(
                    data = state.dailySteps,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                )
            }
        }
    } else if (!state.isLoading) {
        noMetricData(R.string.metric_steps, R.string.message_no_step_updates, Icons.AutoMirrored.Outlined.DirectionsWalk, StepsColor)
    }
}

private fun LazyListScope.distanceContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    if (state.selectedRange == TimeRange.DAY || state.dailySteps.any { it.distanceMeters > 0.0 }) {
        item {
            if (state.selectedRange == TimeRange.DAY) {
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
                    modifier = metricModifier(),
                )
            } else {
                DistanceBarChart(
                    data = state.dailySteps,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                )
            }
        }
    } else if (!state.isLoading) {
        noMetricData(R.string.metric_distance, R.string.message_no_distance_updates, Icons.Outlined.Straighten, DistanceColor)
    }
}

private fun LazyListScope.caloriesContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    if (state.selectedRange == TimeRange.DAY || state.nutrition.any { it.caloriesBurnedKcal > 0.0 }) {
        item {
            if (state.selectedRange == TimeRange.DAY) {
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
                    modifier = metricModifier(),
                )
            } else {
                CaloriesBarChart(
                    data = state.nutrition,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                )
            }
        }
    } else if (!state.isLoading) {
        noMetricData(R.string.metric_calories_burned, R.string.message_no_calories_burned, Icons.Outlined.LocalFireDepartment, CaloriesColor)
    }
}

private fun LazyListScope.activeCaloriesContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    if (state.selectedRange == TimeRange.DAY || state.dailySteps.any { it.activeCaloriesKcal != null }) {
        item {
            if (state.selectedRange == TimeRange.DAY) {
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
                    modifier = metricModifier(),
                )
            } else {
                ActiveCaloriesBarChart(
                    data = state.dailySteps,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                )
            }
        }
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
) {
    if (state.selectedRange == TimeRange.DAY || state.dailySteps.any { it.floorsClimbed != null }) {
        item {
            if (state.selectedRange == TimeRange.DAY) {
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
                    modifier = metricModifier(),
                )
            } else {
                FloorsBarChart(
                    data = state.dailySteps,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                )
            }
        }
    } else if (!state.isLoading) {
        noMetricData(R.string.metric_floors_climbed, R.string.message_no_floors_climbed, Icons.Outlined.Stairs, FloorsColor)
    }
}

private fun LazyListScope.elevationContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    if (state.selectedRange == TimeRange.DAY || state.dailySteps.any { it.elevationGainedMeters != null }) {
        item {
            if (state.selectedRange == TimeRange.DAY) {
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
                    modifier = metricModifier(),
                )
            } else {
                ElevationBarChart(
                    data = state.dailySteps,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                )
            }
        }
    } else if (!state.isLoading) {
        noMetricData(R.string.metric_elevation_gained, R.string.message_no_elevation, Icons.Outlined.Terrain, ElevationColor)
    }
}

private fun LazyListScope.noMetricData(
    titleRes: Int,
    messageRes: Int,
    icon: ImageVector,
    accentColor: Color,
) {
    item {
        MetricCardPlaceholder(
            title = stringResource(titleRes),
            icon = icon,
            accentColor = accentColor,
            message = stringResource(messageRes),
            modifier = metricModifier(),
        )
    }
}

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
