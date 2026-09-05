package tech.mmarca.openvitals.ui.components

import androidx.compose.runtime.Immutable

/**
 * The slice of a chart's axis on screen. Every chart maps its data onto a
 * `0..1` fraction first; [visibleFraction] turns a data fraction into a
 * plot fraction, and is the one place zooming lives.
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

    /** Where a data [fraction] is drawn, as a plot fraction. Not clamped: painters clip. */
    fun visibleFraction(fraction: Float): Float = (fraction - start) / span

    /** The inverse: the data fraction under a plot [fraction]. */
    fun dataFraction(fraction: Float): Float = start + fraction * span

    /** Zooms by [scale] about [focus], a plot fraction, so the chart stretches under the fingers. */
    fun zoomed(scale: Float, focus: Float): ChartViewport {
        if (!scale.isFinite() || scale <= 0f) return this
        val anchor = dataFraction(focus.coerceIn(0f, 1f))
        val newSpan = (span / scale).coerceIn(MinimumSpan, 1f)
        // Keep the anchor under the fingers.
        return around(anchor, newSpan, focus.coerceIn(0f, 1f))
    }

    /** Slides the window by [delta], a plot fraction, so a drag moves the data by the same distance. */
    fun panned(delta: Float): ChartViewport = shifted(-delta * span)

    private fun around(anchor: Float, newSpan: Float, focus: Float): ChartViewport {
        // Never past the ends.
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

        /** How far you may zoom in: 2% of the axis, under half an hour on a day chart. */
        const val MinimumSpan = 0.02f
    }
}
