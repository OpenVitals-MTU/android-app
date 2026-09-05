package tech.mmarca.openvitals.features.devicesync.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncFrameTest {

    private fun uint32(bytes: ByteArray, offset: Int): Long =
        ((bytes[offset].toLong() and 0xFF) shl 24) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 8) or
            (bytes[offset + 3].toLong() and 0xFF)

    // SyncFrame.encode.

    @Test
    fun `encode lays out big-endian length, type byte, then payload`() {
        val frame = SyncFrame(SyncFrameType.BATCH, byteArrayOf(1, 2, 3))

        val bytes = frame.encode()

        assertEquals(5 + 3, bytes.size)
        assertEquals(3L, uint32(bytes, 0))
        assertEquals(SyncFrameType.BATCH.ordinal, bytes[4].toInt())
        assertArrayEquals(byteArrayOf(1, 2, 3), bytes.copyOfRange(5, 8))
    }

    @Test
    fun `encode writes an empty payload as a 5-byte header`() {
        val bytes = SyncFrame(SyncFrameType.SEND_DONE, ByteArray(0)).encode()

        assertEquals(5, bytes.size)
        assertEquals(0L, uint32(bytes, 0))
    }

    // SyncFrameReader.

    @Test
    fun `round-trips a single frame`() {
        val reader = SyncFrameReader()
        val frame = SyncFrame(SyncFrameType.HELLO, byteArrayOf(9, 8, 7))

        val frames = reader.addChunk(frame.encode())

        assertEquals(1, frames.size)
        assertEquals(SyncFrameType.HELLO, frames.single().type)
        assertArrayEquals(byteArrayOf(9, 8, 7), frames.single().payload)
        assertEquals(0, reader.bufferedBytes)
    }

    @Test
    fun `reassembles a frame split across many chunks`() {
        val reader = SyncFrameReader()
        val encoded = SyncFrame(
            SyncFrameType.AUTH,
            ByteArray(300) { (it % 256).toByte() },
        ).encode()

        // Feed one byte at a time — the worst case for a byte-stream reader.
        val collected = mutableListOf<SyncFrame>()
        encoded.forEach { byte -> collected += reader.addChunk(byteArrayOf(byte)) }

        assertEquals(1, collected.size)
        assertEquals(SyncFrameType.AUTH, collected.single().type)
        assertEquals(300, collected.single().payload.size)
        assertEquals(0, reader.bufferedBytes)
    }

    @Test
    fun `splits multiple frames coalesced into one chunk`() {
        val reader = SyncFrameReader()
        val blob = SyncFrame(SyncFrameType.BATCH, byteArrayOf(1)).encode() +
            SyncFrame(SyncFrameType.BATCH_ACK, byteArrayOf(2, 2)).encode() +
            SyncFrame(SyncFrameType.SEND_DONE, ByteArray(0)).encode()

        val frames = reader.addChunk(blob)

        assertEquals(
            listOf(SyncFrameType.BATCH, SyncFrameType.BATCH_ACK, SyncFrameType.SEND_DONE),
            frames.map { it.type },
        )
        assertArrayEquals(byteArrayOf(2, 2), frames[1].payload)
    }

    @Test
    fun `holds a partial trailing frame until the rest arrives`() {
        val reader = SyncFrameReader()
        val full = SyncFrame(SyncFrameType.BATCH, byteArrayOf(5, 6, 7, 8)).encode()
        val firstHalf = full.copyOfRange(0, 6)
        val secondHalf = full.copyOfRange(6, full.size)

        assertTrue(reader.addChunk(firstHalf).isEmpty())
        assertEquals(firstHalf.size, reader.bufferedBytes)

        val frames = reader.addChunk(secondHalf)
        assertEquals(1, frames.size)
        assertArrayEquals(byteArrayOf(5, 6, 7, 8), frames.single().payload)
    }

    @Test
    fun `rejects an unknown frame type byte`() {
        val reader = SyncFrameReader()
        val bogus = ByteArray(6)
        bogus[3] = 1 // length 1
        bogus[4] = 250.toByte() // no such SyncFrameType

        assertThrows(SyncFrameFormatException::class.java) { reader.addChunk(bogus) }
    }

    @Test
    fun `rejects an oversized length prefix`() {
        val reader = SyncFrameReader()
        val oversized = MAX_SYNC_FRAME_PAYLOAD + 1
        val header = byteArrayOf(
            (oversized ushr 24).toByte(),
            (oversized ushr 16).toByte(),
            (oversized ushr 8).toByte(),
            oversized.toByte(),
            SyncFrameType.BATCH.ordinal.toByte(),
        )

        assertThrows(SyncFrameFormatException::class.java) { reader.addChunk(header) }
    }
}
