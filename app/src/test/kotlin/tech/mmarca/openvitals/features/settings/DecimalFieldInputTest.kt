package tech.mmarca.openvitals.features.settings

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecimalFieldInputTest {

    private val systemLocale = Locale.getDefault()

    @After
    fun tearDown() {
        Locale.setDefault(systemLocale)
    }

    @Test
    fun `typing a number key by key never rewrites the text`() {
        var text = ""
        "80.5".forEach { key ->
            text = filterDecimalFieldInput(text + key)
            val value = parseDecimalFieldText(text)

            // The old field re-keyed on the value here and printed "8.0" after the first key.
            assertFalse("after '$key' the text was '$text'", decimalFieldTextIsStale(text, value))
        }
        assertEquals("80.5", text)
        assertEquals(80.5, parseDecimalFieldText(text)!!, 0.0)
    }

    @Test
    fun `a decimal comma is accepted and the prefill never uses one`() {
        Locale.setDefault(Locale.GERMANY)

        assertEquals("72.5", decimalFieldText(72.5))
        assertEquals(72.5, parseDecimalFieldText(filterDecimalFieldInput("72,5"))!!, 0.0)
    }

    @Test
    fun `a unit round trip that is not exact does not count as a new value`() {
        // 154.3 lb to kg and back.
        val roundTripped = 154.3 * 0.45359237 / 0.45359237 + 1e-9

        assertFalse(decimalFieldTextIsStale("154.3", roundTripped))
    }

    @Test
    fun `a value from elsewhere replaces the text`() {
        assertTrue(decimalFieldTextIsStale("80.5", 72.0)) // A reload.
        assertTrue(decimalFieldTextIsStale("", 72.0)) // First load into an empty field.
        assertTrue(decimalFieldTextIsStale("80.5", null)) // A reset.
    }

    @Test
    fun `clearing the field is not a stale text`() {
        assertFalse(decimalFieldTextIsStale("", null))
        assertFalse(decimalFieldTextIsStale(".", null))
    }
}
