package tech.mmarca.openvitals.devices.garmin

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Port of the Flutter build's `garmin_primitives_test.dart` — fixtures identical. */
class GarminPrimitivesTest {

    private fun b(vararg xs: Int) = ByteArray(xs.size) { xs[it].toByte() }

    /** Decodes all frames currently completable in [decoder]. */
    private fun drain(decoder: GarminCobsDecoder): List<ByteArray> {
        val out = mutableListOf<ByteArray>()
        var frame = decoder.pull()
        while (frame != null) {
            out.add(frame)
            frame = decoder.pull()
        }
        return out
    }

    // GarminCrc.

    @Test
    fun `crc of empty data is 0`() {
        assertEquals(0, GarminCrc.compute(b()))
    }

    @Test
    fun `crc is deterministic and stays within 16 bits`() {
        val data = ByteArray(64) { ((it * 37) and 0xFF).toByte() }
        val crc = GarminCrc.compute(data)
        assertEquals(crc, GarminCrc.compute(data))
        assertTrue(crc in 0..0xFFFF)
    }

    @Test
    fun `crc respects offset and length`() {
        val data = b(0xAA, 0x01, 0x02, 0x03, 0xBB)
        // CRC over the middle three bytes must ignore the framing bytes.
        assertEquals(
            GarminCrc.compute(b(0x01, 0x02, 0x03)),
            GarminCrc.compute(data, offset = 1, length = 3),
        )
    }

    // GarminByteReader/Writer round-trip.

    @Test
    fun `little-endian across every width`() {
        val writer = GarminByteWriter()
            .writeByte(0x12)
            .writeShort(0x3456)
            .writeInt(0x789ABCDEL)
            .writeLong(0x0102030405060708L)
            .writeBytes(b(0xDE, 0xAD))
        val reader = GarminByteReader(writer.toBytes())

        assertEquals(0x12, reader.readByte())
        assertEquals(0x3456, reader.readShort())
        assertEquals(0x789ABCDEL, reader.readInt())
        assertEquals(0x0102030405060708L, reader.readLong())
        assertArrayEquals(b(0xDE, 0xAD), reader.readBytes(2))
        assertFalse(reader.hasRemaining)
    }

    @Test
    fun `the writer grows past its initial capacity`() {
        val writer = GarminByteWriter(4)
        for (i in 0 until 100) {
            writer.writeInt(i)
        }
        val reader = GarminByteReader(writer.toBytes())
        for (i in 0 until 100) {
            assertEquals(i.toLong(), reader.readInt())
        }
    }

    @Test
    fun `patchShort backfills a placeholder in place`() {
        val writer = GarminByteWriter()
            .writeShort(0)
            .writeBytes(b(1, 2, 3, 4))
        writer.patchShort(0, writer.length)
        assertEquals(6, GarminByteReader(writer.toBytes()).readShort())
    }

    @Test
    fun `readNullTerminatedString consumes its terminator so the next field reads correctly`() {
        // The notification control channel sends an app id with no length byte, so a leftover NUL desynchronises everything after.
        val reader = GarminByteReader(b(0x61, 0x2E, 0x62, 0x00, 0x2A))
        assertEquals("a.b", reader.readNullTerminatedString())
        assertEquals(0x2A, reader.readByte())
    }

    @Test
    fun `an unterminated string returns the rest of the buffer rather than throwing`() {
        val reader = GarminByteReader(b(0x61, 0x62))
        assertEquals("ab", reader.readNullTerminatedString())
        assertFalse(reader.hasRemaining)
    }

    @Test
    fun `an empty null-terminated string is empty not a skipped field`() {
        val reader = GarminByteReader(b(0x00, 0x2A))
        assertEquals("", reader.readNullTerminatedString())
        assertEquals(0x2A, reader.readByte())
    }

    // GarminCobs round-trip.

    private fun expectRoundTrip(payload: ByteArray) {
        val encoded = GarminCobs.encode(payload)
        // Every frame is bracketed by 0x00 pads.
        assertEquals(0, encoded.first().toInt())
        assertEquals(0, encoded.last().toInt())
        val decoder = GarminCobsDecoder().apply { addBytes(encoded) }
        assertArrayEquals(payload, decoder.pull())
        assertNull(decoder.pull())
    }

    @Test
    fun `cobs data with no zeros`() {
        expectRoundTrip(b(1, 2, 3, 4, 5))
    }

    @Test
    fun `cobs data containing zeros`() {
        expectRoundTrip(b(1, 0, 2, 0, 0, 3))
    }

    @Test
    fun `cobs a payload that ends in zero`() {
        // The case Garmin's extra 0x01 group exists for.
        expectRoundTrip(b(1, 2, 0))
    }

    @Test
    fun `cobs a payload that starts in zero`() {
        expectRoundTrip(b(0, 1, 2))
    }

    @Test
    fun `cobs a run longer than one max group`() {
        expectRoundTrip(ByteArray(600) { ((it % 255) + 1).toByte() })
    }

    @Test
    fun `cobs a 254-byte zero-free run at the group boundary`() {
        expectRoundTrip(ByteArray(254) { 0x41 })
    }

    // GarminCobsDecoder streaming.

    @Test
    fun `reassembles a frame split across arbitrary chunks`() {
        val payload = b(9, 0, 8, 7, 0, 6)
        val encoded = GarminCobs.encode(payload)
        val decoder = GarminCobsDecoder()
        // Feed one byte at a time: no frame until the trailing delimiter lands.
        for (i in encoded.indices) {
            decoder.addBytes(encoded.copyOfRange(i, i + 1))
            val frame = decoder.pull()
            if (i == encoded.size - 1) {
                assertArrayEquals(payload, frame)
            } else {
                assertNull(frame)
            }
        }
    }

    @Test
    fun `pulls two frames concatenated in one buffer`() {
        val decoder = GarminCobsDecoder().apply {
            addBytes(GarminCobs.encode(b(1, 2, 3)))
            addBytes(GarminCobs.encode(b(4, 5, 6)))
        }
        val frames = drain(decoder)
        assertEquals(2, frames.size)
        assertArrayEquals(b(1, 2, 3), frames[0])
        assertArrayEquals(b(4, 5, 6), frames[1])
    }

    @Test
    fun `resynchronises when the buffer does not start with a pad`() {
        val decoder = GarminCobsDecoder().apply { addBytes(b(0x05, 0x06, 0x07, 0x00)) }
        assertNull(decoder.pull())
        // A clean frame after the garbage still decodes.
        decoder.addBytes(GarminCobs.encode(b(1, 2)))
        assertArrayEquals(b(1, 2), decoder.pull())
    }

    // GarminGfdiFrame.

    @Test
    fun `build then parse preserves type and payload`() {
        val payload = b(0xAA, 0xBB, 0xCC)
        val frame = GarminGfdiFrame.parse(GarminGfdiFrame.build(5024, payload))
        assertEquals(5024, frame.messageType)
        assertArrayEquals(payload, frame.payload)
    }

    @Test
    fun `the length field equals the whole frame`() {
        val wire = GarminGfdiFrame.build(5031, b(1, 2, 3, 4))
        // 2 (len) + 2 (type) + 4 (payload) + 2 (crc).
        assertEquals(10, wire.size)
        assertEquals(10, GarminByteReader(wire).readShort())
    }

    @Test
    fun `a flipped payload byte fails the CRC check`() {
        val wire = GarminGfdiFrame.build(5024, b(1, 2, 3))
        wire[4] = (wire[4].toInt() xor 0xFF).toByte() // Corrupt the first payload byte.
        try {
            GarminGfdiFrame.parse(wire)
            fail("expected GarminGfdiFrameException")
        } catch (expected: GarminGfdiFrameException) {
            // Expected.
        }
    }

    @Test
    fun `a wrong length field is rejected`() {
        val wire = GarminGfdiFrame.build(5024, b(1, 2, 3))
        wire[0] = 0xFF.toByte() // Bogus length low byte.
        try {
            GarminGfdiFrame.parse(wire)
            fail("expected GarminGfdiFrameException")
        } catch (expected: GarminGfdiFrameException) {
            // Expected.
        }
    }

    @Test
    fun `an incoming status type has its high bit remapped to the 5000 range`() {
        // 0x8000 or 0x00 => RESPONSE (5000), per GFDIMessage.parseIncoming.
        val builder = GarminByteWriter()
            .writeShort(0) // length placeholder
            .writeShort(0x8000) // status type, high bit set
            .writeBytes(b(0x42))
        builder.patchShort(0, builder.length + 2)
        val crc = GarminCrc.compute(builder.toBytes())
        builder.writeShort(crc)

        val frame = GarminGfdiFrame.parse(builder.toBytes())
        assertEquals(5000, frame.messageType)
        assertArrayEquals(b(0x42), frame.payload)
    }

    @Test
    fun `survives a COBS round-trip (the real transport path)`() {
        val wire = GarminGfdiFrame.build(5002, b(0, 1, 0, 2, 0))
        val decoder = GarminCobsDecoder().apply { addBytes(GarminCobs.encode(wire)) }
        val decoded = decoder.pull()!!
        val frame = GarminGfdiFrame.parse(decoded)
        assertEquals(5002, frame.messageType)
        assertArrayEquals(b(0, 1, 0, 2, 0), frame.payload)
    }
}
