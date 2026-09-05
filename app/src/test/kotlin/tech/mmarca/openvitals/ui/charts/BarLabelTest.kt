package tech.mmarca.openvitals.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pure half of the bar value label. Kotlin measures at one size and drops on overflow,
 * so the Dart shrink-to-fit cases are not portable. The fake measurer is six px per character, ten px a line.
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

        // Kotlin measures at one size, so this is the fits-therefore-drawn half of the rule.
        assertNotNull(result)
        assertTrue(result!!.width <= 80)
        assertEquals(2, result.lines.size)
    }

    @Test
    fun `the unit goes on its own line, so the number keeps the room`() {
        // Kotlin strips the grouping separators, a deliberate divergence from Dart.
        assertEquals(listOf("21104", "steps"), splitBarValueLabel("21,104 steps"))
        assertEquals(2, measure("21,104 steps", maxWidth = 80f)!!.lines.size)
        // Nothing to split: one line.
        assertNull(splitBarValueLabel("21,104"))
        assertEquals(1, measure("21,104", maxWidth = 80f)!!.lines.size)
    }

    @Test
    fun `a label nobody could read is still dropped`() {
        // A month chart gives 31 bars a few pixels each. A label that overruns its slot is dropped.
        assertNull(measure("21,104 steps", maxWidth = 10f))
        assertNull(measure("    ", maxWidth = 100f))
        assertNull(measure("21,104 steps", maxWidth = 0f))
    }

    @Test
    fun `the measured label reports the widest line and the stacked height`() {
        val result = measure("21,104 steps", maxWidth = 80f)!!

        // Both lines are five characters: 30 px wide, two 10 px lines with a 1 px gap.
        assertEquals(30, result.width)
        assertEquals(21, result.height)
    }
}
