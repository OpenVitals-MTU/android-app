package tech.mmarca.openvitals.devices.garmin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The capability bitmap a watch declares as FIT records. */
class GarminFitCapabilitiesTest {

    private fun b(vararg xs: Int) = ByteArray(xs.size) { xs[it].toByte() }

    /** Local type 0 is `capabilities` (1): `connectivity_supported` (23) as a uint32z. */
    private val definition = b(0x40, 0, 0, 1, 0, 1, 23, 4, 0x8C)

    private fun data(mask: Long) = b(0x00) + ByteArray(4) { ((mask ushr (it * 8)) and 0xFF).toByte() }

    @Test
    fun `bit i of connectivity_supported is capability i`() {
        val caps = GarminFitCapabilities()
        caps.define(definition)

        val declared = caps.capabilitiesIn(data((1L shl GarminCapability.SYNC.bit) or (1L shl GarminCapability.WEATHER_CONDITIONS.bit)))

        assertEquals(setOf(GarminCapability.SYNC, GarminCapability.WEATHER_CONDITIONS), declared)
    }

    @Test
    fun `a record that is not capabilities, or data before its definition, declares nothing`() {
        val caps = GarminFitCapabilities()
        // Nothing defined yet: the data cannot be laid out.
        assertNull(caps.capabilitiesIn(data(1)))

        // Local type 1 is some other message (global 2) with one uint8 field.
        caps.define(b(0x41, 0, 0, 2, 0, 1, 0, 1, 0x02))
        assertNull(caps.capabilitiesIn(b(0x01, 0x05)))
    }

    @Test
    fun `a zero mask is the uint32z invalid value and declares nothing`() {
        val caps = GarminFitCapabilities()
        caps.define(definition)

        assertNull(caps.capabilitiesIn(data(0)))
    }
}
