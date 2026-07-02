package tech.mmarca.openvitals.features.manualentry.hydration

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitSystem

class HydrationEntryFormContentTest {

    @Test
    fun `metric hydration input is milliliters`() {
        assertEquals(
            250.0,
            hydrationInputMilliliters("250", UnitSystem.METRIC) ?: 0.0,
            0.0001,
        )
    }

    @Test
    fun `imperial hydration input converts fluid ounces to milliliters`() {
        assertEquals(
            295.735,
            hydrationInputMilliliters("10", UnitSystem.IMPERIAL) ?: 0.0,
            0.001,
        )
    }

    @Test
    fun `hydration input accepts comma decimal separator`() {
        assertEquals(
            147.868,
            hydrationInputMilliliters("5,0", UnitSystem.IMPERIAL) ?: 0.0,
            0.001,
        )
    }

    @Test
    fun `invalid hydration input returns null`() {
        assertNull(hydrationInputMilliliters("nope", UnitSystem.IMPERIAL))
    }

    @Test
    fun `imperial initial hydration amount displays fluid ounces`() {
        assertEquals(
            "5.1",
            hydrationInputAmountText(150.0, formatter(UnitSystem.IMPERIAL)),
        )
    }

    @Test
    fun `metric initial hydration amount displays milliliters`() {
        assertEquals(
            "150",
            hydrationInputAmountText(150.0, formatter(UnitSystem.METRIC)),
        )
    }

    private fun formatter(unitSystem: UnitSystem): UnitFormatter =
        UnitFormatter(
            unitSystemProvider = { unitSystem },
            localeProvider = { Locale.US },
        )
}
