package tech.mmarca.openvitals.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The value label on a bar — the pure half of the decision, mirrored from
 * Flutter's `bar_label_test.dart`.
 *
 * The Dart layout steps the font size down a point at a time until the label fits
 * and drops it only below a readability floor; Kotlin measures at ONE size and
 * drops on overflow, so the two shrink-to-fit cases are not portable here. What
 * the ports below pin is the logic the two apps share: the unit-on-its-own-line
 * split, the fits-therefore-drawn rule, and the drop rule — against a
 * deterministic fake measurer (six px per character, ten px a line) standing in
 * for Compose text measurement, the same way the Dart test leans on the test
 * font's fixed-square glyphs.
 */
class BarLabelTest {

    /** Measures with the fake measurer: six px per character, ten px a line. */
    private fun measure(text: String, maxWidth: Float): BarLabelMeasurement<String>? =
        barLabelLines(text)?.let { lines ->
            measureBarLabelLines(
                lines = lines,
                maxWidth = maxWidth,
                lineGap = 1,
                measure = { it },
                lineWidth = { it.length * 6 },
                lineHeight = { 10 },
            )
        }

    @Test
    fun `a label that fits is drawn at full size`() {
        val result = measure("21,104 steps", maxWidth = 80f)

        // Kotlin measures at exactly one size — "full size" is the only size — so
        // the assertion is the fits-therefore-drawn half of the Dart rule.
        assertNotNull(result)
        assertTrue(result!!.width <= 80)
        assertEquals(2, result.lines.size)
    }

    @Test
    fun `the unit goes on its own line, so the number keeps the room`() {
        // Kotlin also strips the grouping separators off the number line — a
        // deliberate divergence from Dart's ['21,104', 'steps'].
        assertEquals(listOf("21104", "steps"), splitBarValueLabel("21,104 steps"))
        assertEquals(2, measure("21,104 steps", maxWidth = 80f)!!.lines.size)
        // Nothing to split: one line.
        assertNull(splitBarValueLabel("21,104"))
        assertEquals(1, measure("21,104", maxWidth = 80f)!!.lines.size)
    }

    @Test
    fun `a label nobody could read is still dropped`() {
        // A month chart gives 31 bars a few pixels each. Kotlin has no smaller
        // size to step down to: a label that overruns its slot is dropped, and
        // drawing it anyway would read worse than the gap.
        assertNull(measure("21,104 steps", maxWidth = 10f))
        assertNull(measure("    ", maxWidth = 100f))
        assertNull(measure("21,104 steps", maxWidth = 0f))
    }

    @Test
    fun `the measured label reports the widest line and the stacked height`() {
        val result = measure("21,104 steps", maxWidth = 80f)!!

        // "21104" and "steps" are both five characters: 30 px wide, and two
        // 10 px lines with the 1 px gap between them.
        assertEquals(30, result.width)
        assertEquals(21, result.height)
    }
}
