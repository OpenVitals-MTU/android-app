package tech.mmarca.openvitals.features.activity

import tech.mmarca.openvitals.ui.components.OpenVitalsCard

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.ui.components.ChartXAxisWithYAxis
import tech.mmarca.openvitals.ui.components.MetricLinePlot
import tech.mmarca.openvitals.ui.components.MetricLinePlotPoint
import tech.mmarca.openvitals.ui.components.formatCompactAxisValue
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
    yAxisValueFormatter: (Double) -> String = ::formatCompactAxisValue,
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
    val chartHeight = 180.dp

    OpenVitalsCard(
        modifier = modifier,

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
                    add(MetricLinePlotPoint(xFraction = 0f, value = 0.0))
                    points.forEach { point ->
                        val elapsed = Duration.between(dayStart, point.first)
                            .toMillis()
                            .coerceIn(0L, elapsedToday)
                        add(
                            MetricLinePlotPoint(
                                xFraction = elapsed.toFloat() / elapsedToday.toFloat(),
                                value = point.second,
                            )
                        )
                    }
                    add(
                        MetricLinePlotPoint(
                            xFraction = 1f,
                            value = points.last().second,
                        )
                    )
                }

                MetricLinePlot(
                    points = chartPoints,
                    minValue = 0.0,
                    maxValue = maxValue,
                    accentColor = accentColor,
                    chartHeight = chartHeight,
                    valueFormatter = yAxisValueFormatter,
                    lineStrokeWidth = 3.dp,
                    drawPoints = false,
                )

                Spacer(Modifier.height(8.dp))
                ChartXAxisWithYAxis {
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
