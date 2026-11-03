package tech.mmarca.openvitals.wear

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DistanceUnitsTest {

    private val original = Locale.getDefault()

    @Before
    fun english() = Locale.setDefault(Locale.UK)

    @After
    fun restore() = Locale.setDefault(original)

    @Test
    fun `two decimals under ten, one from ten up, in the unit the phone shows`() {
        assertEquals("4.89", DistanceUnits.KILOMETRES.format(4_890.0))
        assertEquals("12.4", DistanceUnits.KILOMETRES.format(12_400.0))
        assertEquals("3.04", DistanceUnits.MILES.format(4_890.0))
    }

    @Test
    fun `a value that rounds up to ten is printed with one decimal`() {
        assertEquals("10.0", DistanceUnits.KILOMETRES.format(9_996.0))
    }

    @Test
    fun `a goal reads as the user set it`() {
        assertEquals("5", DistanceUnits.KILOMETRES.formatGoal(5_000))
        assertEquals("3.1", DistanceUnits.MILES.formatGoal(5_000))
    }

    @Test
    fun `the decimal separator follows the watch's language`() {
        Locale.setDefault(Locale.GERMANY)

        assertEquals("4,89", DistanceUnits.KILOMETRES.format(4_890.0))
    }
}
