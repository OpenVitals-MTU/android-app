package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.ui.components.ChartEmptyState
import tech.mmarca.openvitals.ui.components.ChartTokens
import tech.mmarca.openvitals.ui.components.ChartXAxisWithYAxis
import tech.mmarca.openvitals.ui.components.ChartZoom
import tech.mmarca.openvitals.ui.components.DayAxisLabels
import tech.mmarca.openvitals.ui.components.MetricLinePlot
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.axisFractionOf
import tech.mmarca.openvitals.ui.components.cumulativeDayPlotPoints
import tech.mmarca.openvitals.ui.components.dayEndFraction
import tech.mmarca.openvitals.ui.components.formatCompactAxisValue
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong

/**
 * A cumulative metric across one day, as a card. The x axis is the whole
 * day; today's series stops at now.
 */
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
    val dayStart = selectedDate.atStartOfDay(zone).toInstant()
    val dayEnd = selectedDate.plusDays(1).atStartOfDay(zone).toInstant()
    val isToday = selectedDate == LocalDate.now()
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()
    val maxValue = points.lastOrNull()?.second?.coerceAtLeast(1.0) ?: 1.0
    val dayMillis = Duration.between(dayStart, dayEnd).toMillis().coerceAtLeast(1L)

    // Built outside the zoom content, so the geometry cache holds.
    val chartPoints = remember(points, selectedDate, isToday) {
        cumulativeDayPlotPoints(
            fractions = points.map { (time, value) ->
                axisFractionOf(dayStart, dayEnd, time) to value
            },
            endFraction = dayEndFraction(dayStart, dayEnd, Instant.now()),
        )
    }

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
                // Plot and hour row share the one viewport.
                ChartZoom(selectedDate, points) { zoom ->
                    Column {
                        MetricLinePlot(
                            points = chartPoints,
                            minValue = 0.0,
                            maxValue = maxValue,
                            accentColor = accentColor,
                            chartHeight = ChartTokens.heightDay,
                            valueFormatter = yAxisValueFormatter,
                            lineStrokeWidth = 3.dp,
                            drawPoints = false,
                            viewport = zoom.viewport,
                            multiTouch = zoom.multiTouch,
                            // Drag to read the total and the hour it stood at.
                            scrubLabel = { point ->
                                val at = dayStart.plusMillis(
                                    (point.xFraction.coerceIn(0f, 1f) * dayMillis).roundToLong(),
                                )
                                yAxisValueFormatter(point.value) to timeFormatter.format(at.atZone(zone))
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        ChartXAxisWithYAxis {
                            DayAxisLabels(viewport = zoom.viewport)
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
                ChartEmptyState(
                    message = if (isToday) {
                        stringResource(R.string.summary_empty_today, emptyText)
                    } else {
                        stringResource(R.string.summary_empty_day, emptyText)
                    },
                )
            }
        }
    }
}
