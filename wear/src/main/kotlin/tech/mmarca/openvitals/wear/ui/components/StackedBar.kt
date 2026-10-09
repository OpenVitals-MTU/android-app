package tech.mmarca.openvitals.wear.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import tech.mmarca.openvitals.wear.ui.theme.Emphasis

/** One coloured part of a [StackedBar]. Only the ratio between values matters. */
data class BarSegment(val value: Float, val color: Color)

/** A horizontal bar split into [segments] by their share of the total, e.g. zones or sleep stages. */
@Composable
fun StackedBar(segments: List<BarSegment>, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = Emphasis.subtle)
    Canvas(
        modifier = modifier
            .height(8.dp)
            .clip(CircleShape),
    ) {
        val total = segments.sumOf { it.value.toDouble() }.toFloat()
        if (total <= 0f) {
            drawRect(trackColor)
            return@Canvas
        }
        var x = 0f
        segments.forEach { segment ->
            val width = size.width * segment.value / total
            drawRect(segment.color, topLeft = Offset(x, 0f), size = Size(width, size.height))
            x += width
        }
    }
}

/** A legend line under a [StackedBar]: colour dot, label, value. */
@Composable
fun LegendRow(color: Color, label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Canvas(Modifier.size(8.dp)) { drawCircle(color) }
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.labelMedium)
    }
}
