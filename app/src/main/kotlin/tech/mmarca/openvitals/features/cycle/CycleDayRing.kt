package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.ui.theme.CycleColor

private val RingDiameter: Dp = 132.dp
private val RingStroke: Dp = 7.dp
private const val GapDegrees = 20f
private const val MaxFontScale = 1.8f

/**
 * Four arcs around the recorded cycle day. The arcs carry no data on purpose:
 * a ring that filled would need a total, and the only candidates are a 28-day
 * assumption or an estimate. The day is reported as a number.
 */
@Composable
internal fun CycleDayRing(
    dayOfCycle: Int,
    accessibleLabel: String,
    modifier: Modifier = Modifier,
) {
    // The numeral scales with the font setting, so the frame grows with it.
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, MaxFontScale)
    val diameter = RingDiameter * fontScale
    Box(
        modifier = modifier
            .size(diameter)
            .clearAndSetSemantics { contentDescription = accessibleLabel },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val strokePx = RingStroke.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)
            val style = Stroke(width = strokePx, cap = StrokeCap.Round)
            val sweep = 90f - GapDegrees
            listOf(270f, 0f, 90f, 180f).forEach { center ->
                drawArc(
                    color = CycleColor,
                    startAngle = center - sweep / 2f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = style,
                )
            }
        }
        Text(
            text = dayOfCycle.toString(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
