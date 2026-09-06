package tech.mmarca.openvitals.core.fit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FitFloat16Test {

    @Test
    fun `decodes normal values`() {
        assertEquals(1.0f, fitFloat16(0x3C00), 0f)
        assertEquals(-2.0f, fitFloat16(0xC000), 0f)
        assertEquals(100.0f, fitFloat16(0x5640), 0f)
        assertEquals(0.5f, fitFloat16(0x3800), 0f)
        assertEquals(69.0f, fitFloat16(0x5450), 0f)
    }

    @Test
    fun `decodes zero and subnormals`() {
        assertEquals(0f, fitFloat16(0x0000), 0f)
        assertEquals(-0f, fitFloat16(0x8000), 0f)
        assertEquals(5.9604645E-8f, fitFloat16(0x0001), 0f)
        assertEquals(6.097555E-5f, fitFloat16(0x03FF), 1e-10f)
    }

    @Test
    fun `decodes infinities and NaN`() {
        assertEquals(Float.POSITIVE_INFINITY, fitFloat16(0x7C00), 0f)
        assertEquals(Float.NEGATIVE_INFINITY, fitFloat16(0xFC00), 0f)
        assertTrue(fitFloat16(0x7E00).isNaN())
    }

    @Test
    fun `reads a little-endian array and ignores a trailing odd byte`() {
        val bytes = byteArrayOf(0x00, 0x3C, 0x50, 0x54, 0x7F)
        val values = bytes.fitFloat16Array()
        assertEquals(2, values.size)
        assertEquals(1.0f, values[0], 0f)
        assertEquals(69.0f, values[1], 0f)
    }
}
