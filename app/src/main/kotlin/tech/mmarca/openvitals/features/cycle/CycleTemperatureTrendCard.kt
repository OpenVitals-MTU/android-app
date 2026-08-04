package tech.mmarca.openvitals.features.cycle

import tech.mmarca.openvitals.ui.components.OpenVitalsCard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.ui.components.MetricLinePlot
import tech.mmarca.openvitals.ui.components.MetricLinePlotPoint
import tech.mmarca.openvitals.ui.theme.CycleColor
import java.time.ZoneId

@Composable
internal fun BasalTemperatureTrendCard(
    entries: List<BasalBodyTemperatureEntry>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val sorted = entries.sortedBy { it.time }
    val minC = sorted.minOfOrNull { it.temperatureCelsius } ?: 35.0
    val maxC = sorted.maxOfOrNull { it.temperatureCelsius } ?: 38.0
    val range = (maxC - minC).coerceAtLeast(0.2)
    val axisMaxC = minC + range
    val zone = ZoneId.systemDefault()
    val dayFormatter = dateTimeFormatterProvider.chartDay()
    val chartHeight = 110.dp

    OpenVitalsCard(
        modifier = modifier,

    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (sorted.size >= 2) {
                val stepX = 1f / (sorted.size - 1).coerceAtLeast(1)
                MetricLinePlot(
                    points = sorted.mapIndexed { index, entry ->
                        MetricLinePlotPoint(
                            xFraction = index * stepX,
                            value = entry.temperatureCelsius,
                        )
                    },
                    minValue = minC,
                    maxValue = axisMaxC,
                    accentColor = CycleColor,
                    chartHeight = chartHeight,
                    valueFormatter = { unitFormatter.temperature(it).text },
                    pointRadius = 4.dp,
                )
                Spacer(Modifier.height(8.dp))
            }
            val latest = sorted.last()
            val latestDate = latest.time.atZone(zone).toLocalDate()
            Text(
                text = stringResource(
                    R.string.summary_latest_temperature,
                    unitFormatter.temperature(latest.temperatureCelsius).text,
                    dayFormatter.format(latestDate),
                ),
                style = MaterialTheme.typography.titleSmall,
                color = CycleColor,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.summary_temperature_range,
                    unitFormatter.temperature(minC).text,
                    unitFormatter.temperature(maxC).text,
                    unitFormatter.count(sorted.size),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
