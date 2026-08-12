package tech.mmarca.openvitals.devices.garmin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GarminCapabilitiesTest {

    @Test
    fun `the enum order IS the wire order`() {
        // Bit positions are declaration order, so a reorder silently remaps
        // every watch's capabilities. These three are pinned against
        // Gadgetbridge's enum.
        assertEquals(3, GarminCapability.SYNC.bit)
        assertEquals(9, GarminCapability.FIND_MY_WATCH.bit) // byte 1, bit 1
        assertEquals(92, GarminCapability.REALTIME_SETTINGS.bit) // byte 11, bit 4
        assertEquals(120, GarminCapability.entries.size) // 15 bytes exactly
    }

    @Test
    fun `decodes a flag from its byte and bit`() {
        val bits = ByteArray(15)
        bits[11] = (1 shl 4).toByte() // REALTIME_SETTINGS
        assertEquals(setOf(GarminCapability.REALTIME_SETTINGS), GarminCapability.decode(bits))
    }

    @Test
    fun `an all-ones bitmap sets everything`() {
        val bits = ByteArray(15) { 0xFF.toByte() }
        assertEquals(120, GarminCapability.decode(bits).size)
    }

    @Test
    fun `an empty bitmap sets nothing`() {
        assertTrue(GarminCapability.decode(ByteArray(15)).isEmpty())
    }

    @Test
    fun `a short buffer is not an error`() {
        // A future watch may send fewer bytes than we know flags; everything
        // past the end is absent rather than a crash mid-handshake.
        val bits = byteArrayOf(0xFF.toByte(), 0xFF.toByte())
        val decoded = GarminCapability.decode(bits)
        assertTrue(decoded.contains(GarminCapability.SYNC))
        assertFalse(decoded.contains(GarminCapability.REALTIME_SETTINGS))
        assertEquals(16, decoded.size)
    }
}
