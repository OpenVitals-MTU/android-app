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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.DailyNutrition
import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.ui.components.PeriodBarChart
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.ActiveCaloriesColor
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.ElevationColor
import tech.mmarca.openvitals.ui.theme.FloorsColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun StepsBarChart(
    data: List<DailySteps>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    PeriodBarChart(
        title = stringResource(R.string.metric_steps),
        values = data.map { PeriodChartValue(date = it.date, value = it.steps.toDouble()) },
        selectedRange = selectedRange,
        period = period,
        accentColor = StepsColor.copy(alpha = 0.8f),
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${unitFormatter.count(data.sumOf { it.steps })} ${stringResource(R.string.unit_steps)}",
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
    )
}

@Composable
internal fun DistanceBarChart(
    data: List<DailySteps>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    MetricBarChartCard(
        title = stringResource(R.string.metric_distance),
        values = data.map { PeriodChartValue(date = it.date, value = it.distanceMeters) },
        selectedRange = selectedRange,
        period = period,
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${unitFormatter.distance(data.sumOf { it.distanceMeters }).text}",
        accentColor = DistanceColor,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
    )
}

@Composable
internal fun CaloriesBarChart(
    data: List<DailyNutrition>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    MetricBarChartCard(
        title = stringResource(R.string.metric_calories_burned),
        values = data.map { PeriodChartValue(date = it.date, value = it.caloriesBurnedKcal) },
        selectedRange = selectedRange,
        period = period,
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${unitFormatter.energy(data.sumOf { it.caloriesBurnedKcal }).text}",
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
    accentColor: Color,
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
internal fun FloorsBarChart(
    data: List<DailySteps>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    MetricBarChartCard(
        title = stringResource(R.string.metric_floors_climbed),
        values = data.map { PeriodChartValue(date = it.date, value = it.floorsClimbed?.toDouble() ?: 0.0) },
        selectedRange = selectedRange,
        period = period,
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${unitFormatter.count(data.sumOf { it.floorsClimbed ?: 0 })} ${stringResource(R.string.unit_floors)}",
        accentColor = FloorsColor,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
    )
}

@Composable
internal fun ActiveCaloriesBarChart(
    data: List<DailySteps>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    MetricBarChartCard(
        title = stringResource(R.string.metric_active_calories),
        values = data.map { PeriodChartValue(date = it.date, value = it.activeCaloriesKcal ?: 0.0) },
        selectedRange = selectedRange,
        period = period,
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${unitFormatter.energy(data.sumOf { it.activeCaloriesKcal ?: 0.0 }).text}",
        accentColor = ActiveCaloriesColor,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
    )
}

@Composable
internal fun ElevationBarChart(
    data: List<DailySteps>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val totalMeters = data.sumOf { it.elevationGainedMeters ?: 0.0 }
    MetricBarChartCard(
        title = stringResource(R.string.metric_elevation_gained),
        values = data.map { PeriodChartValue(date = it.date, value = it.elevationGainedMeters ?: 0.0) },
        selectedRange = selectedRange,
        period = period,
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${unitFormatter.elevation(totalMeters).text}",
        accentColor = ElevationColor,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
    )
}

@Composable
internal fun IntradayActivityChartCard(
    selectedDate: LocalDate,
    title: String,
    valueText: String,
    emptyText: String,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    points: List<Pair<Instant, Double>>,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val now = Instant.now()
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
                text = if (isToday) {
                    stringResource(R.string.summary_today, title)
                } else {
                    stringResource(R.string.summary_on_date, title, dateFormatter.format(selectedDate))
                },
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
                    listOf("00:00", "06:00", "12:00", "18:00", if (isToday) stringResource(R.string.summary_now) else "24:00").forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        R.string.summary_last_update,
                        timeFormatter.format(points.last().first.atZone(zone)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = if (isToday) {
                        stringResource(R.string.summary_empty_today, emptyText)
                    } else {
                        stringResource(R.string.summary_empty_day, emptyText)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
