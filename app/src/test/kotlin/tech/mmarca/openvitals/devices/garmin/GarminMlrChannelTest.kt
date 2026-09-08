package tech.mmarca.openvitals.devices.garmin

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GarminMlrChannelTest {

    @Test
    fun `outbound data fragments and advances only after cumulative ACK`() = runTest {
        val writes = mutableListOf<ByteArray>()
        lateinit var channel: GarminMlrChannel
        channel = GarminMlrChannel(
            handle = 3,
            maxPacketSize = 20,
            scope = this,
            write = { packet ->
                writes += packet
                val sequence = packet[1].toInt() and 0x3F
                channel.handlePacket(ack(handle = 3, requestNumber = (sequence + 1) and 0x3F))
            },
            onData = {},
        )

        channel.sendMessage(ByteArray(40) { it.toByte() })

        assertEquals(3, writes.size)
        assertTrue(writes.all { it.size <= 20 })
        assertEquals(listOf(0, 1, 2), writes.map { it[1].toInt() and 0x3F })
    }

    @Test
    fun `inbound data is delivered once and acknowledged`() = runTest {
        val writes = mutableListOf<ByteArray>()
        val delivered = mutableListOf<ByteArray>()
        val channel = GarminMlrChannel(
            handle = 3,
            maxPacketSize = 20,
            scope = this,
            write = { writes += it },
            onData = { delivered += it },
        )
        val data = byteArrayOf(7, 8, 9)

        channel.handlePacket(dataPacket(handle = 3, requestNumber = 0, sequence = 0, data = data))
        channel.handlePacket(dataPacket(handle = 3, requestNumber = 0, sequence = 0, data = data))
        runCurrent()

        assertEquals(1, delivered.size)
        assertArrayEquals(data, delivered.single())
        assertEquals(2, writes.size)
        assertEquals(1, requestNumber(writes.last()))
    }

    @Test
    fun `missing ACK retransmits the same sequence`() = runTest {
        val writes = mutableListOf<ByteArray>()
        lateinit var channel: GarminMlrChannel
        channel = GarminMlrChannel(
            handle = 3,
            maxPacketSize = 20,
            scope = this,
            write = { packet ->
                writes += packet
                if (writes.size == 2) {
                    channel.handlePacket(ack(handle = 3, requestNumber = 1))
                }
            },
            onData = {},
        )

        channel.sendMessage(byteArrayOf(1, 2, 3))

        assertEquals(2, writes.size)
        assertEquals(writes[0][1].toInt() and 0x3F, writes[1][1].toInt() and 0x3F)
    }

    private fun ack(handle: Int, requestNumber: Int): ByteArray =
        header(handle, requestNumber, sequence = 0)

    private fun dataPacket(
        handle: Int,
        requestNumber: Int,
        sequence: Int,
        data: ByteArray,
    ): ByteArray = header(handle, requestNumber, sequence) + data

    private fun header(handle: Int, requestNumber: Int, sequence: Int): ByteArray = byteArrayOf(
        (0x80 or ((handle and 0x07) shl 4) or ((requestNumber ushr 2) and 0x0F)).toByte(),
        (((requestNumber and 0x03) shl 6) or (sequence and 0x3F)).toByte(),
    )

    private fun requestNumber(packet: ByteArray): Int {
        val first = packet[0].toInt() and 0xFF
        val second = packet[1].toInt() and 0xFF
        return ((first and 0x0F) shl 2) or (second ushr 6)
    }
}
