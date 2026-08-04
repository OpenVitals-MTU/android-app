package tech.mmarca.openvitals.ui.components

import androidx.compose.runtime.Immutable

/**
 * The slice of a chart's horizontal axis that is actually on screen.
 *
 * Every chart maps its data onto the same normalised axis first — a fraction in `0..1`,
 * where 0 is the left edge of what the chart is ABOUT (midnight, the start of the
 * session, the first bucket of the week) and 1 is the right edge.
 *
 * That shared step is the whole reason zooming can live in one place. A viewport is a
 * sub-range of that fraction, and [visibleFraction] is the ONE function that turns a
 * data fraction into the fraction of the plot it should be drawn at. A chart that goes
 * through it zooms; a chart that does not, does not. Nothing else about a chart painter
 * has to know that zooming exists.
 */
@Immutable
data class ChartViewport(
    val start: Float = 0f,
    val end: Float = 1f,
) {
    init {
        require(start >= 0f && start < end && end <= 1f) {
            "Invalid viewport $start..$end"
        }
    }

    val span: Float get() = end - start

    val isZoomed: Boolean get() = span < 1f - 1e-6f

    /**
     * Where a point at [fraction] of the DATA should be drawn, as a fraction of the PLOT.
     *
     * Outside `0..1` when the point is off-screen — deliberately not clamped, because a
     * line leaving the left edge has to keep going to its real position or it would bend
     * upwards into the corner. Painters clip; they do not clamp.
     */
    fun visibleFraction(fraction: Float): Float = (fraction - start) / span

    /**
     * The inverse: which data fraction is under a point [fraction] of the way across the
     * plot. The scrubber needs it, and so does a pinch, which zooms around the point
     * between the fingers.
     */
    fun dataFraction(fraction: Float): Float = start + fraction * span

    /**
     * Zooms by [scale] about [focus], a fraction of the PLOT (0 = left edge, 1 = right).
     *
     * Zooming about the fingers rather than the centre is what makes it feel like the
     * chart is being stretched under them rather than replaced.
     */
    fun zoomed(scale: Float, focus: Float): ChartViewport {
        if (!scale.isFinite() || scale <= 0f) return this
        val anchor = dataFraction(focus.coerceIn(0f, 1f))
        val newSpan = (span / scale).coerceIn(MinimumSpan, 1f)
        // Keep the anchor under the fingers: it stays the same fraction across the plot.
        return around(anchor, newSpan, focus.coerceIn(0f, 1f))
    }

    /**
     * Slides the window by [delta], a fraction of the PLOT — so a drag moves the data
     * under the finger by the same distance whatever the zoom.
     */
    fun panned(delta: Float): ChartViewport = shifted(-delta * span)

    private fun around(anchor: Float, newSpan: Float, focus: Float): ChartViewport {
        // Never past the ends. Panning off the edge of a chart shows nothing and is only
        // ever a mistake, so the window stops rather than emptying.
        val newStart = (anchor - focus * newSpan).coerceIn(0f, 1f - newSpan)
        return ChartViewport(start = newStart, end = newStart + newSpan)
    }

    private fun shifted(shift: Float): ChartViewport {
        val newStart = (start + shift).coerceIn(0f, 1f - span)
        return ChartViewport(start = newStart, end = newStart + span)
    }

    companion object {
        /** The whole chart, which is what every chart starts as. */
        val Full = ChartViewport()

        /**
         * How far you may zoom in: 2% of the axis. On a one-day chart that is under half
         * an hour across the plot, which is as fine as any of this data is worth reading —
         * the samples underneath are minutes apart at best.
         */
        const val MinimumSpan = 0.02f
    }
}
