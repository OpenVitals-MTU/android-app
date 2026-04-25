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
import tech.mmarca.openvitals.data.model.DailyNutrition
import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.data.model.TimeRange
import tech.mmarca.openvitals.ui.components.DatePeriod
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.periodTitle
import tech.mmarca.openvitals.ui.theme.ActiveCaloriesColor
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.ElevationColor
import tech.mmarca.openvitals.ui.theme.FloorsColor
import tech.mmarca.openvitals.ui.theme.HydrationColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val dayFormatter = DateTimeFormatter.ofPattern("EEE d")
private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(viewModel: ActivityViewModel) {
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
                        valueText = "%,d steps".format(state.dailySteps.firstOrNull()?.steps ?: 0L),
                        emptyText = "No step updates were recorded",
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
                            valueText = if (distanceTotal >= 1000) {
                                "%.1f km".format(distanceTotal / 1000)
                            } else {
                                "%d m".format(distanceTotal.roundToInt())
                            },
                            emptyText = "No distance updates were recorded",
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
                            valueText = "%,d kcal".format(caloriesTotal.roundToInt()),
                            emptyText = "No calories burned data was recorded",
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            if (state.selectedRange != TimeRange.DAY && state.nutrition.any { it.hydrationLiters > 0 }) {
                item {
                    HydrationBarChart(
                        data = state.nutrition,
                        selectedRange = state.selectedRange,
                        period = period,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            if (state.selectedRange != TimeRange.DAY && state.dailySteps.any { it.floorsClimbed != null }) {
                item {
                    FloorsBarChart(
                        data = state.dailySteps,
                        selectedRange = state.selectedRange,
                        period = period,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            if (state.selectedRange != TimeRange.DAY && state.dailySteps.any { it.activeCaloriesKcal != null }) {
                item {
                    ActiveCaloriesBarChart(
                        data = state.dailySteps,
                        selectedRange = state.selectedRange,
                        period = period,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            if (state.selectedRange != TimeRange.DAY && state.dailySteps.any { it.elevationGainedMeters != null }) {
                item {
                    ElevationBarChart(
                        data = state.dailySteps,
                        selectedRange = state.selectedRange,
                        period = period,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
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
    modifier: Modifier = Modifier,
) {
    val maxSteps = data.maxOfOrNull { it.steps } ?: 1L
    val labelStride = when (selectedRange) {
        TimeRange.DAY,
        TimeRange.WEEK -> 1
        TimeRange.MONTH -> 5
        TimeRange.YEAR -> 30
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Steps",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                data.forEachIndexed { index, day ->
                    val fraction = if (maxSteps > 0) day.steps.toFloat() / maxSteps else 0f
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((100 * fraction + 4).dp),
                        ) {
                            drawRoundRect(
                                color = StepsColor.copy(alpha = 0.8f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                            )
                        }
                        if (
                            selectedRange == TimeRange.DAY ||
                            index % labelStride == 0 ||
                            index == data.lastIndex
                        ) {
                            Text(
                                text = dayFormatter.format(day.date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        } else {
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${periodTitle(selectedRange, period)} · %,d steps".format(data.sumOf { it.steps }),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DistanceBarChart(
    data: List<DailySteps>,
    selectedRange: TimeRange,
    period: DatePeriod,
    modifier: Modifier = Modifier,
) {
    MetricBarChartCard(
        title = "Distance",
        values = data.map { it.distanceMeters },
        labels = data.map { dayFormatter.format(it.date) },
        selectedRange = selectedRange,
        summaryText = "${periodTitle(selectedRange, period)} · ${
            if (data.sumOf { it.distanceMeters } >= 1000) {
                "%.1f km".format(data.sumOf { it.distanceMeters } / 1000)
            } else {
                "%d m".format(data.sumOf { it.distanceMeters }.roundToInt())
            }
        }",
        accentColor = DistanceColor,
        modifier = modifier,
    )
}

@Composable
private fun CaloriesBarChart(
    data: List<DailyNutrition>,
    selectedRange: TimeRange,
    period: DatePeriod,
    modifier: Modifier = Modifier,
) {
    MetricBarChartCard(
        title = "Calories burned",
        values = data.map { it.caloriesBurnedKcal },
        labels = data.map { dayFormatter.format(it.date) },
        selectedRange = selectedRange,
        summaryText = "${periodTitle(selectedRange, period)} · %,d kcal".format(
            data.sumOf { it.caloriesBurnedKcal }.roundToInt(),
        ),
        accentColor = CaloriesColor,
        modifier = modifier,
    )
}

@Composable
private fun HydrationBarChart(
    data: List<DailyNutrition>,
    selectedRange: TimeRange,
    period: DatePeriod,
    modifier: Modifier = Modifier,
) {
    MetricBarChartCard(
        title = "Hydration",
        values = data.map { it.hydrationLiters },
        labels = data.map { dayFormatter.format(it.date) },
        selectedRange = selectedRange,
        summaryText = "${periodTitle(selectedRange, period)} · %.1f L".format(
            data.sumOf { it.hydrationLiters },
        ),
        accentColor = HydrationColor,
        modifier = modifier,
    )
}

@Composable
private fun MetricBarChartCard(
    title: String,
    values: List<Double>,
    labels: List<String>,
    selectedRange: TimeRange,
    summaryText: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val maxValue = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val labelStride = when (selectedRange) {
        TimeRange.DAY,
        TimeRange.WEEK -> 1
        TimeRange.MONTH -> 5
        TimeRange.YEAR -> 30
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                values.forEachIndexed { index, value ->
                    val fraction = if (maxValue > 0) (value / maxValue).toFloat() else 0f
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((100 * fraction + 4).dp),
                        ) {
                            drawRoundRect(
                                color = accentColor.copy(alpha = 0.8f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                            )
                        }
                        if (index % labelStride == 0 || index == values.lastIndex) {
                            Text(
                                text = labels[index],
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        } else {
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FloorsBarChart(
    data: List<DailySteps>,
    selectedRange: TimeRange,
    period: DatePeriod,
    modifier: Modifier = Modifier,
) {
    MetricBarChartCard(
        title = "Floors climbed",
        values = data.map { it.floorsClimbed?.toDouble() ?: 0.0 },
        labels = data.map { dayFormatter.format(it.date) },
        selectedRange = selectedRange,
        summaryText = "${periodTitle(selectedRange, period)} · %,d floors".format(
            data.sumOf { it.floorsClimbed ?: 0 },
        ),
        accentColor = FloorsColor,
        modifier = modifier,
    )
}

@Composable
private fun ActiveCaloriesBarChart(
    data: List<DailySteps>,
    selectedRange: TimeRange,
    period: DatePeriod,
    modifier: Modifier = Modifier,
) {
    MetricBarChartCard(
        title = "Active calories",
        values = data.map { it.activeCaloriesKcal ?: 0.0 },
        labels = data.map { dayFormatter.format(it.date) },
        selectedRange = selectedRange,
        summaryText = "${periodTitle(selectedRange, period)} · %,d kcal".format(
            data.sumOf { it.activeCaloriesKcal ?: 0.0 }.roundToInt(),
        ),
        accentColor = ActiveCaloriesColor,
        modifier = modifier,
    )
}

@Composable
private fun ElevationBarChart(
    data: List<DailySteps>,
    selectedRange: TimeRange,
    period: DatePeriod,
    modifier: Modifier = Modifier,
) {
    val totalMeters = data.sumOf { it.elevationGainedMeters ?: 0.0 }
    MetricBarChartCard(
        title = "Elevation gained",
        values = data.map { it.elevationGainedMeters ?: 0.0 },
        labels = data.map { dayFormatter.format(it.date) },
        selectedRange = selectedRange,
        summaryText = if (totalMeters >= 1000) {
            "${periodTitle(selectedRange, period)} · %.1f km".format(totalMeters / 1000)
        } else {
            "${periodTitle(selectedRange, period)} · %d m".format(totalMeters.roundToInt())
        },
        accentColor = ElevationColor,
        modifier = modifier,
    )
}

@Composable
private fun IntradayActivityChartCard(
    selectedDate: LocalDate,
    title: String,
    valueText: String,
    emptyText: String,
    points: List<Pair<java.time.Instant, Double>>,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val now = java.time.Instant.now()
    val dayStart = selectedDate.atStartOfDay(zone).toInstant()
    val isToday = selectedDate == LocalDate.now()
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
