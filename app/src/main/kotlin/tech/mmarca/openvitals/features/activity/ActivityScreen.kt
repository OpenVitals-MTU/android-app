package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.theme.ActiveCaloriesColor
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.ElevationColor
import tech.mmarca.openvitals.ui.theme.FloorsColor
import tech.mmarca.openvitals.ui.theme.StepsColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
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
        if (state.selectedRange == TimeRange.DAY || state.dailySteps.isNotEmpty()) {
            item {
                if (state.selectedRange == TimeRange.DAY) {
                    IntradayActivityChartCard(
                        selectedDate = state.selectedDate,
                        title = "Steps",
                        valueText = "${unitFormatter.count(state.dailySteps.firstOrNull()?.steps ?: 0L)} steps",
                        emptyText = "No step updates were recorded",
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        points = state.activityProgress.map { point ->
                            point.time to point.totalSteps.toDouble()
                        },
                        accentColor = StepsColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    StepsBarChart(
                        data = state.dailySteps,
                        selectedRange = state.selectedRange,
                        period = period,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            if (state.selectedRange == TimeRange.DAY || state.dailySteps.isNotEmpty()) {
                item {
                    if (state.selectedRange == TimeRange.DAY) {
                        val distanceTotal = state.dailySteps.firstOrNull()?.distanceMeters ?: 0.0
                        IntradayActivityChartCard(
                            selectedDate = state.selectedDate,
                            title = "Distance",
                            valueText = unitFormatter.distance(distanceTotal).text,
                            emptyText = "No distance updates were recorded",
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            points = state.activityProgress.mapNotNull { point ->
                                point.totalDistanceMeters?.let { point.time to it }
                            },
                            accentColor = DistanceColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    } else {
                        DistanceBarChart(
                            data = state.dailySteps,
                            selectedRange = state.selectedRange,
                            period = period,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            if (state.selectedRange == TimeRange.DAY || state.nutrition.isNotEmpty()) {
                item {
                    if (state.selectedRange == TimeRange.DAY) {
                        val caloriesTotal = state.nutrition.firstOrNull()?.caloriesBurnedKcal ?: 0.0
                        IntradayActivityChartCard(
                            selectedDate = state.selectedDate,
                            title = "Calories burned",
                            valueText = unitFormatter.energy(caloriesTotal).text,
                            emptyText = "No calories burned data was recorded",
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            points = state.activityProgress.mapNotNull { point ->
                                point.totalCaloriesBurnedKcal?.let { point.time to it }
                            },
                            accentColor = CaloriesColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    } else {
                        CaloriesBarChart(
                            data = state.nutrition,
                            selectedRange = state.selectedRange,
                            period = period,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            if (state.dailySteps.any { it.floorsClimbed != null }) {
                item {
                    if (state.selectedRange == TimeRange.DAY) {
                        val floorsTotal = state.dailySteps.firstOrNull()?.floorsClimbed ?: 0
                        IntradayActivityChartCard(
                            selectedDate = state.selectedDate,
                            title = "Floors climbed",
                            valueText = "${unitFormatter.count(floorsTotal)} floors",
                            emptyText = "No floors climbed data was recorded",
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            points = state.activityProgress.mapNotNull { point ->
                                point.totalFloorsClimbed?.let { point.time to it.toDouble() }
                            },
                            accentColor = FloorsColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    } else {
                        FloorsBarChart(
                            data = state.dailySteps,
                            selectedRange = state.selectedRange,
                            period = period,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            if (state.dailySteps.any { it.activeCaloriesKcal != null }) {
                item {
                    if (state.selectedRange == TimeRange.DAY) {
                        val activeCaloriesTotal = state.dailySteps.firstOrNull()?.activeCaloriesKcal ?: 0.0
                        IntradayActivityChartCard(
                            selectedDate = state.selectedDate,
                            title = "Active calories",
                            valueText = unitFormatter.energy(activeCaloriesTotal).text,
                            emptyText = "No active calories data was recorded",
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            points = state.activityProgress.mapNotNull { point ->
                                point.totalActiveCaloriesKcal?.let { point.time to it }
                            },
                            accentColor = ActiveCaloriesColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    } else {
                        ActiveCaloriesBarChart(
                            data = state.dailySteps,
                            selectedRange = state.selectedRange,
                            period = period,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            if (state.dailySteps.any { it.elevationGainedMeters != null }) {
                item {
                    if (state.selectedRange == TimeRange.DAY) {
                        val elevationTotal = state.dailySteps.firstOrNull()?.elevationGainedMeters ?: 0.0
                        IntradayActivityChartCard(
                            selectedDate = state.selectedDate,
                            title = "Elevation gained",
                            valueText = unitFormatter.elevation(elevationTotal).text,
                            emptyText = "No elevation data was recorded",
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            points = state.activityProgress.mapNotNull { point ->
                                point.totalElevationGainedMeters?.let { point.time to it }
                            },
                            accentColor = ElevationColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    } else {
                        ElevationBarChart(
                            data = state.dailySteps,
                            selectedRange = state.selectedRange,
                            period = period,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
