package tech.mmarca.openvitals.features.homewidgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The value labels on the widget's Body Energy plot: where they go, and when the plot is too
 * small to hold them. Ascent is negative, descent positive, as on Android.
 */
class BodyEnergyPlotLabelTest {

    private val ascent = -10f
    private val descent = 3f
    private val gap = 3f

    // Whether the labels fit.

    @Test
    fun `labels fit when they take at most half the width`() {
        assertTrue(
            bodyEnergyEdgeLabelsFit(
                plotWidth = 100f,
                plotHeight = 60f,
                startTextWidth = 20f,
                endTextWidth = 30f,
                textHeight = 13f,
            )
        )
    }

    @Test
    fun `labels are skipped on a plot too narrow to annotate`() {
        assertFalse(
            bodyEnergyEdgeLabelsFit(
                plotWidth = 90f,
                plotHeight = 60f,
                startTextWidth = 25f,
                endTextWidth = 25f,
                textHeight = 13f,
            )
        )
    }

    @Test
    fun `labels are skipped on a plot too short to sit clear of the curve`() {
        assertFalse(
            bodyEnergyEdgeLabelsFit(
                plotWidth = 200f,
                plotHeight = 30f,
                startTextWidth = 20f,
                endTextWidth = 20f,
                textHeight = 13f,
            )
        )
    }

    @Test
    fun `a degenerate plot never fits labels`() {
        assertFalse(bodyEnergyEdgeLabelsFit(0f, 60f, 10f, 10f, 13f))
        assertFalse(bodyEnergyEdgeLabelsFit(100f, 0f, 10f, 10f, 13f))
        assertFalse(bodyEnergyEdgeLabelsFit(100f, 60f, 10f, 10f, 0f))
    }

    // Horizontal placement.

    @Test
    fun `the start label is flush with the curve's left edge`() {
        assertEquals(0f, bodyEnergyEdgeLabelX(20f, 200f, alignEnd = false), 0f)
    }

    @Test
    fun `the end label is right-aligned to the curve's right edge`() {
        assertEquals(180f, bodyEnergyEdgeLabelX(20f, 200f, alignEnd = true), 0f)
    }

    @Test
    fun `an end label wider than the plot pins to the left edge, not past it`() {
        assertEquals(0f, bodyEnergyEdgeLabelX(250f, 200f, alignEnd = true), 0f)
    }

    // Vertical placement.

    @Test
    fun `the label prefers sitting above the curve, clear by the gap`() {
        val baseline = bodyEnergyEdgeLabelBaseline(
            curveTopY = 40f,
            curveBottomY = 45f,
            ascent = ascent,
            descent = descent,
            plotHeight = 80f,
            gap = gap,
        )

        // Baseline such that the descent stays `gap` above the curve's top.
        assertEquals(40f - gap - descent, baseline, 0f)
        // And every pixel of the text is inside the bitmap.
        assertTrue(baseline + ascent >= 0f)
        assertTrue(baseline + descent <= 80f)
    }

    @Test
    fun `a curve at the top of the plot pushes the label below itself`() {
        val baseline = bodyEnergyEdgeLabelBaseline(
            curveTopY = 5f,
            curveBottomY = 12f,
            ascent = ascent,
            descent = descent,
            plotHeight = 80f,
            gap = gap,
        )

        // Below the span's lowest point, so it clears the curve it covers.
        assertEquals(12f + gap - ascent, baseline, 0f)
        assertTrue(baseline + ascent >= 0f)
        assertTrue(baseline + descent <= 80f)
    }

    @Test
    fun `a below-the-curve label cannot fall out of the bitmap`() {
        val plotHeight = 40f
        val baseline = bodyEnergyEdgeLabelBaseline(
            curveTopY = 2f,
            curveBottomY = 38f,
            ascent = ascent,
            descent = descent,
            plotHeight = plotHeight,
            gap = gap,
        )

        // The flip lands past the bottom; the clamp pulls it back inside.
        assertEquals(plotHeight - descent, baseline, 0f)
        assertTrue(baseline + ascent >= 0f)
    }

    @Test
    fun `an above-the-curve label cannot poke out of the top`() {
        val baseline = bodyEnergyEdgeLabelBaseline(
            curveTopY = 11f,
            curveBottomY = 11f,
            ascent = ascent,
            descent = descent,
            plotHeight = 80f,
            gap = gap,
        )

        // 11 - 3 - 3 = 5 puts the text top at -5; the flip goes below instead.
        assertEquals(11f + gap - ascent, baseline, 0f)
        assertTrue(baseline + ascent >= 0f)
    }
}
