package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.DailyNutrition
import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.data.model.TimeRange
import tech.mmarca.openvitals.ui.components.DatePeriod
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.PeriodBarChart
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.periodTitle
import tech.mmarca.openvitals.ui.theme.ActiveCaloriesColor
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.ElevationColor
import tech.mmarca.openvitals.ui.theme.FloorsColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

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

@Composable
fun StepsBarChart(
    data: List<DailySteps>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    PeriodBarChart(
        title = "Steps",
        values = data.map { PeriodChartValue(date = it.date, value = it.steps.toDouble()) },
        selectedRange = selectedRange,
        period = period,
        accentColor = StepsColor.copy(alpha = 0.8f),
        summaryText = "${periodTitle(selectedRange, period)} · ${unitFormatter.count(data.sumOf { it.steps })} steps",
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
    )
}

@Composable
private fun DistanceBarChart(
    data: List<DailySteps>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    MetricBarChartCard(
        title = "Distance",
        values = data.map { PeriodChartValue(date = it.date, value = it.distanceMeters) },
        selectedRange = selectedRange,
        period = period,
        summaryText = "${periodTitle(selectedRange, period)} · ${unitFormatter.distance(data.sumOf { it.distanceMeters }).text}",
        accentColor = DistanceColor,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
    )
}

@Composable
private fun CaloriesBarChart(
    data: List<DailyNutrition>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    MetricBarChartCard(
        title = "Calories burned",
        values = data.map { PeriodChartValue(date = it.date, value = it.caloriesBurnedKcal) },
        selectedRange = selectedRange,
        period = period,
        summaryText = "${periodTitle(selectedRange, period)} · ${unitFormatter.energy(data.sumOf { it.caloriesBurnedKcal }).text}",
        accentColor = CaloriesColor,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
    )
}

@Composable
private fun MetricBarChartCard(
    title: String,
    values: List<PeriodChartValue>,
    selectedRange: TimeRange,
    period: DatePeriod,
    summaryText: String,
    accentColor: androidx.compose.ui.graphics.Color,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    PeriodBarChart(
        title = title,
        values = values,
        selectedRange = selectedRange,
        period = period,
        accentColor = accentColor.copy(alpha = 0.8f),
        summaryText = summaryText,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
    )
}

@Composable
private fun FloorsBarChart(
    data: List<DailySteps>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    MetricBarChartCard(
        title = "Floors climbed",
        values = data.map { PeriodChartValue(date = it.date, value = it.floorsClimbed?.toDouble() ?: 0.0) },
        selectedRange = selectedRange,
        period = period,
        summaryText = "${periodTitle(selectedRange, period)} · ${unitFormatter.count(data.sumOf { it.floorsClimbed ?: 0 })} floors",
        accentColor = FloorsColor,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
    )
}

@Composable
private fun ActiveCaloriesBarChart(
    data: List<DailySteps>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    MetricBarChartCard(
        title = "Active calories",
        values = data.map { PeriodChartValue(date = it.date, value = it.activeCaloriesKcal ?: 0.0) },
        selectedRange = selectedRange,
        period = period,
        summaryText = "${periodTitle(selectedRange, period)} · ${unitFormatter.energy(data.sumOf { it.activeCaloriesKcal ?: 0.0 }).text}",
        accentColor = ActiveCaloriesColor,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
    )
}

@Composable
private fun ElevationBarChart(
    data: List<DailySteps>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val totalMeters = data.sumOf { it.elevationGainedMeters ?: 0.0 }
    MetricBarChartCard(
        title = "Elevation gained",
        values = data.map { PeriodChartValue(date = it.date, value = it.elevationGainedMeters ?: 0.0) },
        selectedRange = selectedRange,
        period = period,
        summaryText = "${periodTitle(selectedRange, period)} · ${unitFormatter.elevation(totalMeters).text}",
        accentColor = ElevationColor,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
    )
}

@Composable
private fun IntradayActivityChartCard(
    selectedDate: LocalDate,
    title: String,
    valueText: String,
    emptyText: String,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    points: List<Pair<java.time.Instant, Double>>,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val now = java.time.Instant.now()
    val dayStart = selectedDate.atStartOfDay(zone).toInstant()
    val isToday = selectedDate == LocalDate.now()
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()
    val chartEnd = if (isToday) now else selectedDate.plusDays(1).atStartOfDay(zone).toInstant()
    val elapsedToday = Duration.between(dayStart, chartEnd).toMillis().coerceAtLeast(1L)
    val maxValue = points.lastOrNull()?.second?.coerceAtLeast(1.0) ?: 1.0

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.headlineMedium,
                color = accentColor,
            )
            Text(
                text = if (isToday) "$title today" else "$title on ${dateFormatter.format(selectedDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            if (points.isNotEmpty()) {
                val chartPoints = buildList {
                    add(Offset(0f, 1f))
                    points.forEach { point ->
                        val elapsed = Duration.between(dayStart, point.first)
                            .toMillis()
                            .coerceIn(0L, elapsedToday)
                        add(
                            Offset(
                                x = elapsed.toFloat() / elapsedToday.toFloat(),
                                y = 1f - (point.second.toFloat() / maxValue.toFloat()),
                            )
                        )
                    }
                    add(
                        Offset(
                            x = 1f,
                            y = 1f - (points.last().second.toFloat() / maxValue.toFloat()),
                        )
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                ) {
                    repeat(4) { index ->
                        val y = size.height * index / 3f
                        drawLine(
                            color = accentColor.copy(alpha = 0.12f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    val scaledPoints = chartPoints.map { point ->
                        Offset(
                            x = point.x * size.width,
                            y = point.y * size.height,
                        )
                    }

                    for (i in 0 until scaledPoints.size - 1) {
                        drawLine(
                            color = accentColor,
                            start = scaledPoints[i],
                            end = scaledPoints[i + 1],
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf("00:00", "06:00", "12:00", "18:00", if (isToday) "Now" else "24:00").forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Last update ${timeFormatter.format(points.last().first.atZone(zone))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = if (isToday) {
                        "$emptyText yet today."
                    } else {
                        "$emptyText on this day."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
