package tech.mmarca.openvitals.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Everything a chart needs to know about how it should look — in one place, so that
 * changing how the app's charts look is one edit and not seventeen.
 *
 * Plain object in the [tech.mmarca.openvitals.ui.theme.Spacing] idiom for the layout
 * numbers — a dp is a dp in every theme. The colours are composable getters instead of
 * constants because they have to derive from the LIVE
 * [androidx.compose.material3.ColorScheme]: the app supports dynamic colour, so a
 * hard-coded grey is wrong on most of the themes it can be looked at in.
 *
 * The y-axis gutter constants ([ChartYAxisWidth], [ChartAxisGap]) already live in
 * `ChartAxis.kt` and are deliberately not duplicated here.
 */
object ChartTokens {

    // ── Colour ──────────────────────────────────────────────────────────────

    val crosshair: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    /** The unfilled part of any bar. One answer, for every bar that had its own. */
    val track: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerHighest

    /**
     * A heatmap cell for a day with no reading — which is not the same as a day with a
     * reading of zero, and must not look like one.
     */
    val emptyTrack: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)

    val tooltipSurface: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.inverseSurface

    val onTooltipSurface: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.inverseOnSurface

    /**
     * A grid line is the SERIES colour, faint. Not a grey: a neutral grid under a
     * coloured line reads as a separate object sitting behind the chart, where a tinted
     * one reads as part of it.
     */
    fun grid(accent: Color): Color = accent.copy(alpha = 0.12f)

    /** The wash under a line, fading to nothing at the baseline. */
    fun areaFill(accent: Color): Brush = Brush.verticalGradient(
        0f to accent.copy(alpha = 0.26f),
        1f to accent.copy(alpha = 0f),
    )

    /** The line a chart sits ON — heavier than a grid line, lighter than the trace. */
    fun baseline(accent: Color): Color = accent.copy(alpha = 0.22f)

    // ── Layout ──────────────────────────────────────────────────────────────
    //
    // This is where the numbers LIVE, not what they are: retuning any of them is a
    // deliberate act, and that is exactly the point of naming them.

    val heightDay: Dp = 180.dp
    val heightSession: Dp = 180.dp
    val heightPeriodBar: Dp = 120.dp
    val heightLine: Dp = 150.dp
    val heightSchedule: Dp = 232.dp
    val heightBodyEnergy: Dp = 172.dp
    val heightInfluenceStrip: Dp = 44.dp

    /**
     * One hypnogram lane at text scale 1: a label band ([sleepLaneLabelHeight]) with the
     * track band under it. The track band is the part that stays FIXED — at a larger
     * system font the label band grows and takes the lane with it, so the track keeps a
     * constant distance below the label instead of the label overrunning the track.
     *
     * The stage chart is still the only thing that draws lanes, but a number nobody else
     * can see is a number nobody else can keep in step.
     */
    val heightSleepLane: Dp = 72.dp
    val sleepLaneLabelHeight: Dp = 28.dp
    val sleepLaneTrackHeight: Dp = 26.dp

    val lineStroke: Dp = 3.dp
    val traceStroke: Dp = 2.dp
    val pointRadius: Dp = 3.5.dp

    /**
     * A bar is a pill until it gets fat, then it is a rounded rectangle — beyond this a
     * "fully rounded" bar just looks like a lozenge.
     */
    val barRadiusMax: Dp = 8.dp

    /** The corner a bar of [barWidth] gets. One rule, so a bar cannot be rounder in one chart than another. */
    fun barRadius(barWidth: Dp): Dp = (barWidth / 2).coerceAtMost(barRadiusMax)
}
