package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.BasalBodyTemperatureEntry
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
    val zone = ZoneId.systemDefault()
    val dayFormatter = dateTimeFormatterProvider.chartDay()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (sorted.size >= 2) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                ) {
                    val stepX = size.width / (sorted.size - 1)
                    val points = sorted.mapIndexed { index, entry ->
                        Offset(
                            x = index * stepX,
                            y = size.height * (1f - ((entry.temperatureCelsius - minC) / range).toFloat()),
                        )
                    }
                    for (index in 0 until points.lastIndex) {
                        drawLine(
                            color = CycleColor,
                            start = points[index],
                            end = points[index + 1],
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                    points.forEach { point ->
                        drawCircle(color = CycleColor, radius = 4.dp.toPx(), center = point)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            val latest = sorted.last()
            val latestDate = latest.time.atZone(zone).toLocalDate()
            Text(
                text = "Latest ${unitFormatter.temperature(latest.temperatureCelsius).text} · ${dayFormatter.format(latestDate)}",
                style = MaterialTheme.typography.titleSmall,
                color = CycleColor,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Range ${unitFormatter.temperature(minC).text}-${unitFormatter.temperature(maxC).text} · ${unitFormatter.count(sorted.size)} readings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
