package tech.mmarca.openvitals.devices.garmin

import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** Port of the Flutter build's `garmin_ml_transport_test.dart` — fixtures identical. */
class GarminMlTransportTest {

    private companion object {
        const val CLIENT_ID = 2
        const val GFDI_SERVICE_CODE = 1
    }

    private fun b(vararg xs: Int) = ByteArray(xs.size) { xs[it].toByte() }

    private lateinit var written: MutableList<ByteArray>
    private lateinit var frames: MutableList<GarminGfdiFrame>
    private lateinit var logs: MutableList<String>
    private var gfdiClosedCount = 0
    private lateinit var transport: GarminMlTransport

    @Before
    fun setUp() {
        written = mutableListOf()
        frames = mutableListOf()
        logs = mutableListOf()
        gfdiClosedCount = 0
        transport = GarminMlTransport(
            write = { packet -> written.add(packet) },
            onFrame = { frames.add(it) },
            onGfdiClosed = { gfdiClosedCount++ },
            onLog = { logs.add(it) },
        )
    }

    /**
     * The close-handle response — also sent UNREQUESTED when the watch shuts
     * a service down. Field order differs from registration:
     * `[handle 0][CLOSE_HANDLE_RESP][u64 client][u16 service][handle][status]`
     */
    private fun closeResponse(
        handle: Int = 3,
        status: Int = 0,
        serviceCode: Int = GFDI_SERVICE_CODE,
        clientId: Long = CLIENT_ID.toLong(),
    ): ByteArray = GarminByteWriter()
        .writeByte(0)
        .writeByte(3) // CLOSE_HANDLE_RESP
        .writeLong(clientId)
        .writeShort(serviceCode)
        .writeByte(handle)
        .writeByte(status)
        .toBytes()

    /**
     * The control response the watch sends to grant a handle:
     * `[handle 0][REGISTER_ML_RESP][u64 client][u16 service][status][handle][reliable]`
     */
    private fun registerResponse(
        handle: Int = 3,
        status: Int = 0,
        serviceCode: Int = GFDI_SERVICE_CODE,
        clientId: Long = CLIENT_ID.toLong(),
    ): ByteArray = GarminByteWriter()
        .writeByte(0)
        .writeByte(1) // REGISTER_ML_RESP
        .writeLong(clientId)
        .writeShort(serviceCode)
        .writeByte(status)
        .writeByte(handle)
        .writeByte(0)
        .toBytes()

    /** Wraps [frame] as the watch would: COBS, then handle-prefixed packets. */
    private fun inboundPackets(frame: ByteArray, handle: Int, chunkSize: Int): List<ByteArray> {
        val encoded = GarminCobs.encode(frame)
        val packets = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < encoded.size) {
            val end = (offset + chunkSize).coerceAtMost(encoded.size)
            packets.add(
                GarminByteWriter()
                    .writeByte(handle)
                    .writeBytes(encoded.copyOfRange(offset, end))
                    .toBytes(),
            )
            offset += chunkSize
        }
        return packets
    }

    private suspend fun openChannel(handle: Int = 3) {
        transport.open()
        transport.handleInbound(registerResponse(handle = handle))
        transport.ready.await()
    }

    // ── opening the GFDI channel ─────────────────────────────────────────────

    @Test
    fun `closes stale handles before registering`() = runTest {
        transport.open()

        assertEquals(2, written.size)
        // Both are 13-byte control packets on handle 0.
        assertTrue(written.all { it.size == 13 })
        assertTrue(written.all { it[0].toInt() == 0 })
        assertEquals(5, written[0][1].toInt()) // CLOSE_ALL_REQ
        assertEquals(0, written[1][1].toInt()) // REGISTER_ML_REQ
    }

    @Test
    fun `the register request names GFDI and asks for plain ML`() = runTest {
        transport.open()

        val register = written[1]
        // [handle][req][u64 client][u16 service][trailing]
        assertEquals(CLIENT_ID, register[2].toInt()) // little-endian u64, low byte first
        assertEquals(
            GFDI_SERVICE_CODE,
            (register[10].toInt() and 0xFF) or ((register[11].toInt() and 0xFF) shl 8),
        )
        // 0 = plain ML; 2 would request reliable.
        assertEquals(0, register[12].toInt())
    }

    @Test
    fun `becomes ready when the watch grants a handle`() = runTest {
        transport.open()
        assertFalse(transport.isReady)

        transport.handleInbound(registerResponse(handle = 3))

        transport.ready.await()
        assertTrue(transport.isReady)
    }

    @Test
    fun `a refused registration surfaces as an error not a hang`() = runTest {
        transport.open()

        transport.handleInbound(registerResponse(status = 1))

        try {
            transport.ready.await()
            fail("expected IllegalStateException")
        } catch (expected: IllegalStateException) {
            // Expected.
        }
    }

    @Test
    fun `ignores control traffic belonging to another client`() = runTest {
        transport.open()

        transport.handleInbound(registerResponse(clientId = 99))

        assertFalse(transport.isReady)
        assertTrue(logs.any { it.contains("client 99") })
    }

    @Test
    fun `sending before the channel opens is an error not a silent drop`() = runTest {
        try {
            transport.sendFrame(GarminGfdiFrame.build(5031, ByteArray(0)))
            fail("expected IllegalStateException")
        } catch (expected: IllegalStateException) {
            // Expected.
        }
    }

    // ── sending frames ───────────────────────────────────────────────────────

    @Test
    fun `prefixes every write with the granted handle`() = runTest {
        openChannel()
        written.clear()

        transport.sendFrame(GarminGfdiFrame.build(5031, ByteArray(0)))

        assertTrue(written.isNotEmpty())
        assertTrue(written.all { it[0].toInt() == 3 })
    }

    @Test
    fun `a small frame fits one write and round-trips through COBS`() = runTest {
        openChannel()
        written.clear()
        val frame = GarminGfdiFrame.build(5002, b(1, 2, 3, 4))

        transport.sendFrame(frame)

        assertEquals(1, written.size)
        // Strip the handle byte and COBS-decode: the original frame must return.
        val decoder = GarminCobsDecoder().apply {
            addBytes(written.single().copyOfRange(1, written.single().size))
        }
        assertArrayEquals(frame, decoder.pull())
    }

    @Test
    fun `a frame larger than the MTU is split and reassembles exactly`() = runTest {
        openChannel()
        written.clear()
        // Default MTU gives 20-byte writes, so 19 payload bytes each.
        val frame = GarminGfdiFrame.build(
            5004,
            ByteArray(200) { ((it * 7) and 0xFF).toByte() },
        )

        transport.sendFrame(frame)

        assertTrue(written.size > 1)
        assertTrue(written.all { it.size <= 20 })
        // Concatenate the payloads and decode: byte-identical to what went in.
        val joined = ByteArrayOutputStream()
        for (packet in written) {
            joined.write(packet, 1, packet.size - 1)
        }
        val decoder = GarminCobsDecoder().apply { addBytes(joined.toByteArray()) }
        assertArrayEquals(frame, decoder.pull())
    }

    @Test
    fun `a negotiated MTU widens the writes`() = runTest {
        openChannel()
        written.clear()
        transport.onMtuChanged(515)
        val frame = GarminGfdiFrame.build(
            5004,
            ByteArray(200) { (it and 0xFF).toByte() },
        )

        transport.sendFrame(frame)

        // 512-byte writes now hold the whole thing.
        assertEquals(1, written.size)
    }

    @Test
    fun `MTU is clamped to the spec floor and ceiling`() = runTest {
        transport.onMtuChanged(9999)
        assertTrue(logs.last().contains("maxWrite=512"))
        transport.onMtuChanged(5)
        assertTrue(logs.last().contains("maxWrite=20"))
    }

    // ── receiving frames ─────────────────────────────────────────────────────

    @Test
    fun `reassembles a frame split across several packets`() = runTest {
        openChannel()
        val payload = ByteArray(100) { ((it * 3) and 0xFF).toByte() }
        val frame = GarminGfdiFrame.build(5004, payload)

        for (packet in inboundPackets(frame, 3, 15)) {
            transport.handleInbound(packet)
        }

        assertEquals(1, frames.size)
        assertEquals(5004, frames.single().messageType)
        assertArrayEquals(payload, frames.single().payload)
    }

    @Test
    fun `emits two frames delivered back to back`() = runTest {
        openChannel()
        val a = GarminGfdiFrame.build(5024, b(1))
        val bFrame = GarminGfdiFrame.build(5101, b(2))

        for (packet in inboundPackets(a, 3, 64) + inboundPackets(bFrame, 3, 64)) {
            transport.handleInbound(packet)
        }

        assertEquals(listOf(5024, 5101), frames.map { it.messageType })
    }

    @Test
    fun `a packet for an unknown handle is dropped not misrouted`() = runTest {
        openChannel()
        val frame = GarminGfdiFrame.build(5024, b(1, 2))

        for (packet in inboundPackets(frame, 7, 64)) {
            transport.handleInbound(packet)
        }

        assertTrue(frames.isEmpty())
        assertTrue(logs.any { it.contains("unknown handle 7") })
    }

    @Test
    fun `a corrupt frame is dropped and the stream keeps running`() = runTest {
        openChannel()
        val bad = GarminGfdiFrame.build(5024, b(1, 2, 3))
        bad[4] = (bad[4].toInt() xor 0xFF).toByte() // Break the CRC.
        val good = GarminGfdiFrame.build(5101, b(9))

        for (packet in inboundPackets(bad, 3, 64)) {
            transport.handleInbound(packet)
        }
        for (packet in inboundPackets(good, 3, 64)) {
            transport.handleInbound(packet)
        }

        // One bad packet must not take the sync down with it.
        assertTrue(logs.any { it.contains("dropped bad frame") })
        assertEquals(listOf(5101), frames.map { it.messageType })
    }

    @Test
    fun `an empty packet is ignored`() = runTest {
        transport.handleInbound(ByteArray(0))
        assertTrue(frames.isEmpty())
    }

    // ── the watch closing GFDI mid-session ───────────────────────────────────

    @Test
    fun `a watch-initiated GFDI close deafens the channel and reports it`() = runTest {
        openChannel(handle = 3)

        transport.handleInbound(closeResponse(handle = 3))

        // The held link would otherwise keep writing to a dead handle — the
        // silent-deafness failure Gadgetbridge fixed in CommunicatorV2.
        assertFalse(transport.isReady)
        assertEquals(1, gfdiClosedCount)
        try {
            transport.sendFrame(GarminGfdiFrame.build(5001, b(1)))
            fail("sendFrame should throw once the watch closed the handle")
        } catch (expected: IllegalStateException) {
        }
    }

    @Test
    fun `a close for a stale handle changes nothing`() = runTest {
        openChannel(handle = 3)

        // Our own CLOSE_ALL on open provokes closes for handles from previous
        // sessions; those must not kill the channel just granted.
        transport.handleInbound(closeResponse(handle = 9))

        assertTrue(transport.isReady)
        assertEquals(0, gfdiClosedCount)
    }

    @Test
    fun `a close for another service is ignored`() = runTest {
        openChannel(handle = 3)

        transport.handleInbound(closeResponse(handle = 3, serviceCode = 2))

        assertTrue(transport.isReady)
        assertEquals(0, gfdiClosedCount)
    }

    @Test
    fun `reopen asks for a new handle and the grant restores the channel`() = runTest {
        openChannel(handle = 3)
        transport.handleInbound(closeResponse(handle = 3))
        written.clear()

        transport.reopenGfdi()

        // Registration only — a CLOSE_ALL here would tear down whatever else
        // the watch is running, and is only right on a cold open.
        assertEquals(1, written.size)
        assertEquals(0, written.single()[1].toInt()) // REGISTER_ML_REQ

        transport.handleInbound(registerResponse(handle = 7))
        assertTrue(transport.isReady)

        // Frames flow again, prefixed with the NEW handle.
        transport.sendFrame(GarminGfdiFrame.build(5001, b(1)))
        assertEquals(7, written.last()[0].toInt())
    }

    @Test
    fun `a full send-receive loop survives the real chunking both ways`() = runTest {
        openChannel(handle = 5)
        written.clear()

        // Send a large frame, then feed its own bytes back as if echoed by
        // the watch — end-to-end proof that chunking and reassembly agree.
        val payload = ByteArray(500) { ((it * 11) and 0xFF).toByte() }
        val frame = GarminGfdiFrame.build(5004, payload)
        transport.sendFrame(frame)

        for (packet in written) {
            transport.handleInbound(packet)
        }

        assertEquals(1, frames.size)
        assertArrayEquals(payload, frames.single().payload)
    }
}
