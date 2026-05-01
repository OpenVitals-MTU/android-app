package tech.mmarca.openvitals.features.body

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.BodyFatEntry
import tech.mmarca.openvitals.data.model.WeightEntry
import tech.mmarca.openvitals.ui.theme.BodyFatColor
import tech.mmarca.openvitals.ui.theme.WeightColor

@Composable
internal fun BodyFatLineChart(
    entries: List<BodyFatEntry>,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val sorted = entries.sortedBy { it.time }
    val maxPct = sorted.maxOfOrNull { it.percent } ?: 30.0
    val minPct = sorted.minOfOrNull { it.percent } ?: 0.0
    val range = (maxPct - minPct).coerceAtLeast(0.5)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
            ) {
                if (sorted.size < 2) return@Canvas
                val stepX = size.width / (sorted.size - 1)
                val points = sorted.mapIndexed { i, entry ->
                    val x = i * stepX
                    val y = size.height * (1f - ((entry.percent - minPct) / range).toFloat())
                    Offset(x, y)
                }
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = BodyFatColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 2.dp.toPx(),
                    )
                }
                points.forEach { pt ->
                    drawCircle(color = BodyFatColor, radius = 4.dp.toPx(), center = pt)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${unitFormatter.percent(minPct).text} - ${unitFormatter.percent(maxPct).text} · ${
                    stringResource(R.string.summary_entries, unitFormatter.count(sorted.size))
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun WeightLineChart(
    entries: List<WeightEntry>,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val sorted = entries.sortedBy { it.time }
    val maxKg = sorted.maxOfOrNull { it.weightKg } ?: 100.0
    val minKg = sorted.minOfOrNull { it.weightKg } ?: 50.0
    val range = (maxKg - minKg).coerceAtLeast(0.5)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
            ) {
                if (sorted.size < 2) return@Canvas
                val stepX = size.width / (sorted.size - 1)
                val points = sorted.mapIndexed { i, entry ->
                    val x = i * stepX
                    val y = size.height * (1f - ((entry.weightKg - minKg) / range).toFloat())
                    Offset(x, y)
                }
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = WeightColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 2.dp.toPx(),
                    )
                }
                points.forEach { pt ->
                    drawCircle(color = WeightColor, radius = 4.dp.toPx(), center = pt)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${unitFormatter.weight(minKg).text} - ${unitFormatter.weight(maxKg).text} · ${
                    stringResource(R.string.summary_entries, unitFormatter.count(sorted.size))
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
