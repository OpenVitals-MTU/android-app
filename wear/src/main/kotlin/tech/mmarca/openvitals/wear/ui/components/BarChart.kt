package tech.mmarca.openvitals.wear.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.wear.ui.theme.Emphasis

/**
 * Bare bars scaled to the largest value, each centred in an equal slot so a
 * row of equally weighted labels lines up underneath. A zero still draws a
 * stub, so an empty hour reads as "nothing" rather than as a gap in the data.
 */
@Composable
fun BarChart(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    highlightLast: Boolean = false,
) {
    Spacer(
        modifier.drawWithCache {
            val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
            val slot = if (values.isEmpty()) 0f else size.width / values.size
            val barWidth = slot * 0.6f
            val stub = 2.dp.toPx()
            val radius = CornerRadius(minOf(barWidth / 2, 3.dp.toPx()))
            val heights = values.map { maxOf((it / max * size.height).toFloat(), stub) }
            onDrawBehind {
                heights.forEachIndexed { index, height ->
                    val dimmed = highlightLast && index != heights.lastIndex
                    drawRoundRect(
                        color = if (dimmed) color.copy(alpha = Emphasis.fill) else color,
                        topLeft = Offset(index * slot + (slot - barWidth) / 2, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = radius,
                    )
                }
            }
        },
    )
}
