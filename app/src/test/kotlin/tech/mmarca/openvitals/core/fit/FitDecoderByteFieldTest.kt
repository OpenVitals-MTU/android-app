package tech.mmarca.openvitals.core.fit

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.devices.garmin.wellness.FitW
import tech.mmarca.openvitals.devices.garmin.wellness.fitWrap

/** `byte` fields reach the caller whole; the numeric maps stay as they were. */
class FitDecoderByteFieldTest {

    private val payload = ByteArray(20) { (it * 7).toByte() }

    private fun file(bytesField: ByteArray): ByteArray {
        val data = FitW()
        // Message 274: one 20-byte `byte` field, plus a uint8 array to prove it stays numeric.
        data.def(0, 274, listOf(listOf(0, 20, 0x0D), listOf(1, 2, 0x02)))
        data.u8(0).bytes(bytesField).u8(3).u8(4)
        return fitWrap(data.toBytes())
    }

    @Test
    fun `a byte field is exposed untouched`() {
        val message = FitDecoder.readFile(file(payload), 0).messages.single()

        assertArrayEquals(payload, message.bytes[0])
        // Not decoded as numbers: base type `byte` has no scalar meaning.
        assertNull(message.values[0])
        assertFalse(message.arrays.containsKey(0))
        // The neighbouring array field still decodes as before.
        assertEquals(listOf(3L, 4L), message.arrays[1])
        assertEquals(3L, message.values[1])
    }

    @Test
    fun `an all-invalid byte field is absent`() {
        val message = FitDecoder.readFile(file(ByteArray(20) { 0xFF.toByte() }), 0).messages.single()

        assertFalse(message.bytes.containsKey(0))
    }
}
