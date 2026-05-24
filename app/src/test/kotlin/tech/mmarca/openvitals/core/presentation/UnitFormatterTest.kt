package tech.mmarca.openvitals.core.presentation

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.core.preferences.UnitSystem

class UnitFormatterTest {

    @Test fun `count uses locale grouping`() {
        assertEquals("12,345", formatter(UnitSystem.METRIC).count(12_345))
    }

    @Test fun `metric distance uses meters below one kilometer`() {
        assertEquals("999 m", formatter(UnitSystem.METRIC).distance(999.0).text)
    }

    @Test fun `metric distance uses kilometers from one kilometer`() {
        assertEquals("1.5 km", formatter(UnitSystem.METRIC).distance(1_500.0).text)
    }

    @Test fun `imperial distance uses miles above threshold`() {
        assertEquals("1.0 mi", formatter(UnitSystem.IMPERIAL).distance(1_609.344).text)
    }

    @Test fun `imperial distance uses feet below threshold`() {
        assertEquals("164 ft", formatter(UnitSystem.IMPERIAL).distance(50.0).text)
    }

    @Test fun `imperial elevation uses feet`() {
        assertEquals("33 ft", formatter(UnitSystem.IMPERIAL).elevation(10.0).text)
    }

    @Test fun `imperial weight uses pounds`() {
        assertEquals("154.3 lb", formatter(UnitSystem.IMPERIAL).weight(70.0).text)
    }

    @Test fun `metric height uses centimeters`() {
        assertEquals("180 cm", formatter(UnitSystem.METRIC).height(180.0).text)
    }

    @Test fun `imperial height uses feet and inches`() {
        assertEquals("5' 11\"", formatter(UnitSystem.IMPERIAL).height(180.0).text)
    }

    @Test fun `imperial hydration uses fluid ounces`() {
        assertEquals("68 fl oz", formatter(UnitSystem.IMPERIAL).hydration(2.0).text)
    }

    @Test fun `metric hydration uses liters`() {
        assertEquals("2.0 L", formatter(UnitSystem.METRIC).hydration(2.0).text)
    }

    @Test fun `imperial temperature uses fahrenheit`() {
        assertEquals("98.6 deg F", formatter(UnitSystem.IMPERIAL).temperature(37.0).text)
    }

    @Test fun `blood pressure is not converted`() {
        assertEquals("120/80 mmHg", formatter(UnitSystem.METRIC).bloodPressure(120, 80).text)
    }

    @Test fun `duration formats hours and padded minutes`() {
        assertEquals("1h 05m", formatter(UnitSystem.METRIC).duration(3_900_000L))
    }

    private fun formatter(unitSystem: UnitSystem): UnitFormatter =
        UnitFormatter(
            unitSystemProvider = { unitSystem },
            localeProvider = { Locale.US },
        )
}
