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
 * A cumulative metric (steps, calories, distance…) across one day, as a whole card.
 *
 * The x axis is the WHOLE day, always. This card used to scale its x positions by
 * the time elapsed so far — so on a chart opened at 12:49 a 09:29 reading landed at
 * 74% of the width — and then drew a fixed `00:00 / 06:00 / 12:00 / 18:00` row
 * underneath, patched with "Now" in place of "24:00". The chart's only job is to
 * say WHEN, and it said the wrong hour. A point now sits at its real hour; today's
 * series simply stops at the now-fraction instead of stretching to the right edge,
 * and midnight is midnight again.
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

    // Built once here, not inside the zoom content: the plotted points do not
    // depend on the viewport (the plot applies it), so recomputing them on every
    // pinch frame would only churn a fresh list and defeat the plot's geometry
    // cache. One list, stable identity, reused across zoom frames.
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
                // Pinch with two fingers to look closer at part of the day. The
                // plot and its hour row are BOTH inside the zoom, and share the
                // one viewport — a chart whose hours disagreed with its line would
                // be worse than one that did not zoom at all.
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
                            // Drag along the chart and it tells you the total and
                            // the hour it stood at that. The number was always in
                            // the data and never on the screen.
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
