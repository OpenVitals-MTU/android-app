package tech.mmarca.openvitals.features.vitals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.SkinTemperatureDeltaSample
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.ui.components.DayTimelineLinePlot
import tech.mmarca.openvitals.ui.components.LineAxisRange
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The day's samples across every record, in time order. A record without samples contributes nothing. */
internal fun skinTemperatureDaySamples(
    entries: List<SkinTemperatureEntry>,
    dayStart: Instant,
    dayEnd: Instant,
): List<SkinTemperatureDeltaSample> =
    entries
        .flatMap { it.deltas }
        .filter { !it.time.isBefore(dayStart) && it.time.isBefore(dayEnd) }
        .sortedBy { it.time }

/**
 * A day of skin temperature as the device recorded it: a sample a minute,
 * zero-centred, with the zero line it varies about. The period chart drew one
 * dot per record at the record's end, which for a five-hour record was one
 * point for five hours.
 */
@Composable
internal fun SkinTemperatureDayChart(
    date: LocalDate,
    samples: List<SkinTemperatureDeltaSample>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val dayStart = remember(date, zone) { date.atStartOfDay(zone).toInstant() }
    val dayEnd = remember(date, zone) { date.plusDays(1).atStartOfDay(zone).toInstant() }
    // One pass per load, not per recomposition.
    val summary = remember(samples) { skinTemperatureDaySummary(samples) } ?: return
    val timeFormatter = dateTimeFormatterProvider.shortTime()
    val title = stringResource(R.string.skin_temperature_variation_title)

    OpenVitalsCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            DayTimelineLinePlot(
                samples = samples,
                dayStart = dayStart,
                dayEnd = dayEnd,
                minValue = summary.axisMin,
                maxValue = summary.axisMax,
                accentColor = temperatureColor,
                valueFormatter = { unitFormatter.temperatureDelta(it).text },
                title = title,
                timeLabel = { at -> timeFormatter.format(at.atZone(zone)) },
                time = { it.time },
                value = { it.deltaCelsius },
                chartHeight = 180.dp,
                pointRadius = 3.dp,
                zoomKey = date,
                guides = skinTemperatureZeroGuides(),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${stringResource(R.string.summary_value_avg, unitFormatter.temperatureDelta(summary.averageCelsius).text)} · ${
                    stringResource(
                        R.string.summary_recorded,
                        timeFormatter.format(summary.first.atZone(zone)),
                        timeFormatter.format(summary.last.atZone(zone)),
                    )
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** What the day card says besides the line, hoisted for JVM tests. Null below two samples: a dot is not a line. */
internal data class SkinTemperatureDaySummary(
    val averageCelsius: Double,
    val axisMin: Double,
    val axisMax: Double,
    val first: Instant,
    val last: Instant,
)

internal fun skinTemperatureDaySummary(samples: List<SkinTemperatureDeltaSample>): SkinTemperatureDaySummary? {
    if (samples.size < 2) return null
    val values = samples.map { it.deltaCelsius }
    val (axisMin, axisMax) = LineAxisRange.ZeroCentred.resolve(values.min(), values.max())
    return SkinTemperatureDaySummary(
        averageCelsius = values.average(),
        axisMin = axisMin,
        axisMax = axisMax,
        first = samples.first().time,
        last = samples.last().time,
    )
}
