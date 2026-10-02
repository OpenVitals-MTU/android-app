package tech.mmarca.openvitals.wear.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** A bare trend line: no axes, no labels. Draws nothing below two samples. */
@Composable
fun Sparkline(
    samples: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier.drawWithCache {
            // Built once per size and sample list, not once per frame.
            val path = Path()
            if (samples.size >= 2) {
                val min = samples.min()
                val span = (samples.max() - min).coerceAtLeast(1.0)
                val stroke = 2.dp.toPx()
                val drawableHeight = size.height - stroke
                val stepX = size.width / (samples.size - 1)
                samples.forEachIndexed { index, sample ->
                    val x = index * stepX
                    val y = stroke / 2 + drawableHeight * (1f - ((sample - min) / span).toFloat())
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
            }
            onDrawBehind {
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        },
    )
}
