package tech.mmarca.openvitals.core.fit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** A bare record stream, as a watch pushes it over GFDI: no file header, definitions kept between reads. */
class FitRecordDecoderTest {

    private fun b(vararg xs: Int) = ByteArray(xs.size) { xs[it].toByte() }

    /** Local type 0 is the `capabilities` message (1) with one uint32z field, number 23. */
    private val definition = b(0x40, 0, 0, 1, 0, 1, 23, 4, 0x8C)

    @Test
    fun `a definition read earlier lays out a data payload read later`() {
        val decoder = FitRecordDecoder()

        assertTrue(decoder.read(definition).isEmpty())
        val messages = decoder.read(b(0x00, 0x08, 0x00, 0x00, 0x00))

        val message = messages.single()
        assertEquals(1, message.globalMessageNumber)
        assertEquals(8L, message.values[23])
    }

    @Test
    fun `data without a definition is an error, not a guess`() {
        assertThrows(IllegalArgumentException::class.java) {
            FitRecordDecoder().read(b(0x00, 0x08, 0x00, 0x00, 0x00))
        }
    }

    @Test
    fun `a file still decodes through the same walk`() {
        val decoder = FitRecordDecoder()
        val stream = definition + b(0x00, 0x08, 0x00, 0x00, 0x00)

        assertEquals(1, decoder.read(stream).size)
        // Bounds are honoured: a read that stops before the data record sees only the definition.
        assertTrue(FitRecordDecoder().read(stream, 0, definition.size).isEmpty())
    }
}
