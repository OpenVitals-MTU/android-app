package tech.mmarca.openvitals.core.fit

import java.io.ByteArrayOutputStream
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.garmin.GarminCrc

/** Whatever the encoder frames, [FitDecoder] decodes back, field for field. */
class FitEncoderTest {

    @Test fun `every base type round-trips through the app's own decoder`() {
        val encoder = FitEncoder()
        encoder.defineMessage(
            0, 12345,
            listOf(
                FitEncoderField(0, FitBaseType.ENUM),
                FitEncoderField(1, FitBaseType.SINT8),
                FitEncoderField(2, FitBaseType.UINT8),
                FitEncoderField(3, FitBaseType.SINT16),
                FitEncoderField(4, FitBaseType.UINT16),
                FitEncoderField(5, FitBaseType.SINT32),
                FitEncoderField(6, FitBaseType.UINT32),
                FitEncoderField(7, FitBaseType.STRING, size = 16),
                FitEncoderField(253, FitBaseType.UINT32),
            ),
        )
        encoder.writeMessage(
            0,
            values = mapOf(
                0 to 4L,
                1 to -5L,
                2 to 200L,
                3 to -30_000L,
                4 to 60_000L,
                5 to -2_000_000L,
                6 to 4_000_000_000L,
                253 to 1_000_000L,
            ),
            strings = mapOf(7 to "Morning run"),
        )
        val bytes = ByteArrayOutputStream().also(encoder::writeTo).toByteArray()

        val message = FitDecoder.readFile(bytes, startOffset = 0).messages.single()

        assertEquals(12345, message.globalMessageNumber)
        assertEquals(4L, message.values[0])
        assertEquals(-5L, message.values[1])
        assertEquals(200L, message.values[2])
        assertEquals(-30_000L, message.values[3])
        assertEquals(60_000L, message.values[4])
        assertEquals(-2_000_000L, message.values[5])
        assertEquals(4_000_000_000L, message.values[6])
        assertEquals("Morning run", message.strings[7])
        assertEquals(1_000_000L, message.timestamp)
    }

    @Test fun `absent values write invalid sentinels the decoder drops`() {
        val encoder = FitEncoder()
        encoder.defineMessage(
            3, 20,
            listOf(
                FitEncoderField(0, FitBaseType.UINT8),
                FitEncoderField(1, FitBaseType.UINT16),
                FitEncoderField(2, FitBaseType.UINT32),
                FitEncoderField(3, FitBaseType.STRING, size = 8),
            ),
        )
        encoder.writeMessage(3, values = mapOf(0 to 128L))
        val bytes = ByteArrayOutputStream().also(encoder::writeTo).toByteArray()

        val message = FitDecoder.readFile(bytes, startOffset = 0).messages.single()

        assertEquals(128L, message.values[0])
        assertNull(message.values[1])
        assertNull(message.values[2])
        assertNull(message.strings[3])
    }

    @Test fun `the container carries real CRCs`() {
        val encoder = FitEncoder()
        encoder.defineMessage(0, 0, listOf(FitEncoderField(0, FitBaseType.ENUM)))
        encoder.writeMessage(0, values = mapOf(0 to 4L))
        val bytes = ByteArrayOutputStream().also(encoder::writeTo).toByteArray()

        assertEquals(14, bytes[0].toInt())
        assertEquals(".FIT", String(bytes, 8, 4, Charsets.US_ASCII))
        assertEquals(FitCrc.compute(bytes, offset = 0, length = 12), bytes.uint16At(12))
        assertEquals(
            FitCrc.compute(bytes, offset = 0, length = bytes.size - 2),
            bytes.uint16At(bytes.size - 2),
        )
        // The header's data size covers the records alone: no header, no CRC.
        assertEquals((bytes.size - 14 - 2).toLong(), bytes.uint32At(4))
        assertTrue(FitDecoder.isFitFile(bytes))
    }

    @Test fun `an overlong string truncates but stays NUL-terminated`() {
        val encoder = FitEncoder()
        encoder.defineMessage(0, 34, listOf(FitEncoderField(8, FitBaseType.STRING, size = 6)))
        encoder.writeMessage(0, strings = mapOf(8 to "A very long workout title"))
        val bytes = ByteArrayOutputStream().also(encoder::writeTo).toByteArray()

        val message = FitDecoder.readFile(bytes, startOffset = 0).messages.single()

        assertEquals("A ver", message.strings[8])
    }

    @Test fun `fit timestamps invert fitInstant`() {
        val time = Instant.parse("2026-05-26T08:30:00Z")

        assertEquals(time, fitInstant(fitTimestamp(time)))
        // Pre-FIT-epoch clamps rather than going negative.
        assertEquals(0L, fitTimestamp(Instant.parse("1980-01-01T00:00:00Z")))
    }

    @Test fun `GarminCrc still computes the same checksum after the move`() {
        // GFDI packet framing delegates to FitCrc; a drift here would corrupt every packet sent to a watch.
        val data = ByteArray(64) { (it * 37 + 11).toByte() }

        assertEquals(FitCrc.compute(data), GarminCrc.compute(data))
        assertEquals(
            FitCrc.compute(data, offset = 3, length = 40, initialCrc = 77),
            GarminCrc.compute(data, offset = 3, length = 40, initialCrc = 77),
        )
        assertFalse(FitCrc.compute(data) == 0)
    }

    private fun ByteArray.uint16At(offset: Int): Int =
        (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

    private fun ByteArray.uint32At(offset: Int): Long =
        (this[offset].toLong() and 0xFF) or
            ((this[offset + 1].toLong() and 0xFF) shl 8) or
            ((this[offset + 2].toLong() and 0xFF) shl 16) or
            ((this[offset + 3].toLong() and 0xFF) shl 24)
}
