package tech.mmarca.openvitals.devices.garmin

import java.io.ByteArrayOutputStream
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GarminV1TransportTest {

    @Test
    fun `writes COBS frames without a multi-link handle`() = runTest {
        val writes = mutableListOf<ByteArray>()
        val transport = GarminV1Transport(write = { writes += it }, onFrame = {})
        val frame = GarminGfdiFrame.build(5024, ByteArray(80) { it.toByte() })

        transport.sendFrame(frame)

        assertTrue(writes.size > 1)
        val joined = ByteArrayOutputStream().apply {
            writes.forEach { write(it) }
        }.toByteArray()
        val decoder = GarminCobsDecoder().apply { addBytes(joined) }
        assertArrayEquals(frame, decoder.pull())
    }

    @Test
    fun `reassembles inbound COBS packets`() {
        val frames = mutableListOf<GarminGfdiFrame>()
        val transport = GarminV1Transport(write = { _ -> }, onFrame = { frames += it })
        val frame = GarminGfdiFrame.build(5037, byteArrayOf(1, 2, 3))
        val encoded = GarminCobs.encode(frame)

        transport.handleInbound(encoded.copyOfRange(0, encoded.size / 2))
        transport.handleInbound(encoded.copyOfRange(encoded.size / 2, encoded.size))

        assertEquals(1, frames.size)
        assertEquals(5037, frames.single().messageType)
        assertArrayEquals(byteArrayOf(1, 2, 3), frames.single().payload)
    }

    @Test
    fun `concurrent multi-write frames stay atomic at MTU 23`() = runTest {
        val writes = mutableListOf<ByteArray>()
        val transport = GarminV1Transport(
            write = { packet -> writes += packet; yield() },
            onFrame = {},
        )
        val first = GarminGfdiFrame.build(5004, ByteArray(120) { 0x11 })
        val second = GarminGfdiFrame.build(5043, ByteArray(120) { 0x22 })

        val a = async { transport.sendFrame(first) }
        yield()
        val b = async { transport.sendFrame(second) }
        a.await()
        b.await()

        val decoder = GarminCobsDecoder().apply {
            writes.forEach(::addBytes)
        }
        assertArrayEquals(first, decoder.pull())
        assertArrayEquals(second, decoder.pull())
    }
}
