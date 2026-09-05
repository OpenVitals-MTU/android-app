package tech.mmarca.openvitals.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How the app's charts look, in one place. Layout numbers are plain
 * constants; colours are composable getters, since they derive from the
 * live ColorScheme. The y-axis gutter constants live in `ChartAxis.kt`.
 */
object ChartTokens {

    // Colour.

    val crosshair: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    /** The unfilled part of any bar. One answer, for every bar that had its own. */
    val track: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerHighest

    /** A heatmap cell with no reading, which must not look like a reading of zero. */
    val emptyTrack: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)

    val tooltipSurface: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.inverseSurface

    val onTooltipSurface: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.inverseOnSurface

    /** A grid line is the series colour, faint, so it reads as part of the chart. */
    fun grid(accent: Color): Color = accent.copy(alpha = 0.12f)

    /** The wash under a line, fading to nothing at the baseline. */
    fun areaFill(accent: Color): Brush = Brush.verticalGradient(
        0f to accent.copy(alpha = 0.26f),
        1f to accent.copy(alpha = 0f),
    )

    /** The line a chart sits ON — heavier than a grid line, lighter than the trace. */
    fun baseline(accent: Color): Color = accent.copy(alpha = 0.22f)

    // Layout.

    val heightDay: Dp = 180.dp
    val heightSession: Dp = 180.dp
    val heightPeriodBar: Dp = 120.dp
    val heightLine: Dp = 150.dp
    val heightSchedule: Dp = 232.dp
    val heightBodyEnergy: Dp = 172.dp
    val heightInfluenceStrip: Dp = 44.dp

    /**
     * One hypnogram lane at text scale 1: a label band plus a fixed track
     * band. At a larger font the label band grows and takes the lane with it.
     */
    val heightSleepLane: Dp = 72.dp
    val sleepLaneLabelHeight: Dp = 28.dp
    val sleepLaneTrackHeight: Dp = 26.dp

    val lineStroke: Dp = 3.dp
    val traceStroke: Dp = 2.dp
    val pointRadius: Dp = 3.5.dp

    /** A bar is a pill until it gets fat, then a rounded rectangle. */
    val barRadiusMax: Dp = 8.dp

    /** The corner a bar of [barWidth] gets. One rule for every chart. */
    fun barRadius(barWidth: Dp): Dp = (barWidth / 2).coerceAtMost(barRadiusMax)
}
