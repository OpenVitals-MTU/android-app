package tech.mmarca.openvitals.devices.garmin

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield

/** Port of the Flutter build's `garmin_protobuf_test.dart` — fixtures identical. */
class GarminProtobufTest {

    private fun b(vararg xs: Int) = ByteArray(xs.size) { xs[it].toByte() }

    /** Wraps [payload] the way the watch does, so the transport sees the real envelope. */
    private fun reply(
        requestId: Int,
        payload: ByteArray,
        offset: Int? = null,
        total: Int? = null,
    ): GarminGfdiFrame {
        val writer = GarminByteWriter()
            .writeShort(requestId)
            .writeInt(offset ?: 0)
            .writeInt(total ?: payload.size)
            .writeInt(payload.size)
            .writeBytes(payload)
        return GarminGfdiFrame.parse(
            GarminGfdiFrame.build(GarminMessageId.PROTOBUF_RESPONSE, writer.toBytes()),
        )
    }

    private fun requestChunk(
        id: Int,
        payload: ByteArray,
        offset: Int,
        total: Int,
    ): GarminGfdiFrame {
        val writer = GarminByteWriter()
            .writeShort(id)
            .writeInt(offset)
            .writeInt(total)
            .writeInt(payload.size)
            .writeBytes(payload)
        return GarminGfdiFrame.parse(
            GarminGfdiFrame.build(GarminMessageId.PROTOBUF_REQUEST, writer.toBytes()),
        )
    }

    // Protobuf encoding.

    @Test
    fun `a varint field encodes key then value`() {
        assertArrayEquals(b(0x08, 0x3C), ProtobufWriter().varint(1, 60).toBytes())
    }

    @Test
    fun `a multi-byte varint is little-endian base-128`() {
        assertArrayEquals(b(0x08, 0xAC, 0x02), ProtobufWriter().varint(1, 300).toBytes())
    }

    @Test
    fun `an empty nested message is not the same as an absent one`() {
        // Garmin uses the empty message as the whole request for actions taking no arguments.
        assertArrayEquals(b(0x1A, 0x00), ProtobufWriter().emptyMessage(3).toBytes())
        assertEquals(0, ProtobufWriter().toBytes().size)
    }

    @Test
    fun `round-trips through the reader`() {
        val inner = ProtobufWriter().varint(1, 60).toBytes()
        val outer = ProtobufWriter().nested(12, inner).toBytes()
        val fields = readProtobuf(outer)
        assertEquals(12, fields.single().field)
        assertEquals(60L, readProtobuf(fields.single().bytes!!).single().varint)
    }

    @Test
    fun `a truncated message yields what was readable not a crash`() {
        assertTrue(readProtobuf(b(0x0A, 0x05, 0x01)).isEmpty())
    }

    // Find my watch.

    @Test
    fun `start carries a 60-second timeout under the find service`() {
        // Smart.find_my_watch_service = 12, FindMyWatchRequest.timeout = 1.
        assertArrayEquals(
            b(0x62, 0x04, 0x0A, 0x02, 0x08, 0x3C),
            GarminFindMyWatch.start(),
        )
    }

    @Test
    fun `cancel is an empty message not a missing one`() {
        assertArrayEquals(b(0x62, 0x02, 0x1A, 0x00), GarminFindMyWatch.cancel())
    }

    @Test
    fun `OK is 100 — a zero status is NOT success`() {
        fun replyWithStatus(status: Int): ByteArray {
            val response = ProtobufWriter().varint(1, status).toBytes()
            val service = ProtobufWriter().nested(2, response).toBytes()
            return ProtobufWriter().nested(12, service).toBytes()
        }

        assertEquals(GarminFindOutcome.OK, GarminFindMyWatch.outcome(replyWithStatus(100)))
        assertEquals(GarminFindOutcome.ERROR, GarminFindMyWatch.outcome(replyWithStatus(200)))
        assertEquals(GarminFindOutcome.UNKNOWN, GarminFindMyWatch.outcome(replyWithStatus(0)))
    }

    @Test
    fun `an EMPTY response is acceptance — the real watch sends no status`() {
        // From a vívoactive 5: `62 02 12 00` is find_response with no status, and the watch was ringing.
        assertEquals(
            GarminFindOutcome.OK,
            GarminFindMyWatch.outcome(b(0x62, 0x02, 0x12, 0x00)),
        )
        // And the cancel it answers with, field 4.
        assertEquals(
            GarminFindOutcome.OK,
            GarminFindMyWatch.outcome(b(0x62, 0x02, 0x22, 0x00)),
        )
    }

    @Test
    fun `an unreadable reply is UNKNOWN never a refusal`() {
        // Only an explicit ERROR means the watch declined; everything else leaves the alert stoppable.
        assertEquals(GarminFindOutcome.UNKNOWN, GarminFindMyWatch.outcome(null))
        assertEquals(GarminFindOutcome.UNKNOWN, GarminFindMyWatch.outcome(ByteArray(0)))
        // Service present, but no response field inside it.
        assertEquals(GarminFindOutcome.UNKNOWN, GarminFindMyWatch.outcome(b(0x62, 0x00)))
        assertEquals(GarminFindOutcome.UNKNOWN, GarminFindMyWatch.outcome(b(0xFF, 0xFF)))
        for (outcome in listOf(GarminFindOutcome.OK, GarminFindOutcome.UNKNOWN)) {
            assertFalse(outcome.declined)
        }
        assertTrue(GarminFindOutcome.ERROR.declined)
    }

    // Protobuf transport.

    @Test
    fun `matches a reply to its request by id`() = runTest {
        val sent = mutableListOf<GarminGfdiFrame>()
        lateinit var transport: GarminProtobufTransport
        transport = GarminProtobufTransport(send = { frame ->
            val parsed = GarminGfdiFrame.parse(frame)
            // Answer the request only; replying to the transport's own acknowledgements recurses.
            if (parsed.messageType != GarminMessageId.PROTOBUF_REQUEST) return@GarminProtobufTransport
            sent.add(parsed)
            val requestId = (parsed.payload[0].toInt() and 0xFF) or
                ((parsed.payload[1].toInt() and 0xFF) shl 8)
            transport.handleInbound(reply(requestId, b(0x62, 0x00)))
        })

        val result = transport.request(GarminFindMyWatch.start())
        assertArrayEquals(b(0x62, 0x00), result)
        assertEquals(GarminMessageId.PROTOBUF_REQUEST, sent.single().messageType)
    }

    @Test
    fun `request ids advance so two requests cannot be confused`() = runTest {
        val ids = mutableListOf<Int>()
        lateinit var transport: GarminProtobufTransport
        transport = GarminProtobufTransport(send = { frame ->
            val parsed = GarminGfdiFrame.parse(frame)
            if (parsed.messageType != GarminMessageId.PROTOBUF_REQUEST) return@GarminProtobufTransport
            val id = (parsed.payload[0].toInt() and 0xFF) or
                ((parsed.payload[1].toInt() and 0xFF) shl 8)
            ids.add(id)
            transport.handleInbound(reply(id, b(0x00)))
        })

        transport.request(GarminFindMyWatch.start())
        transport.request(GarminFindMyWatch.cancel())
        assertEquals(listOf(1, 2), ids)
    }

    @Test
    fun `a COMPLETE message is acknowledged by request id not generically`() = runTest {
        // The watch also wants to hear the protobuf message was kept. Without that it retransmitted
        // every message every five seconds, so a stale reply was in flight during a different request.
        val acks = mutableListOf<GarminGfdiFrame>()
        val transport = GarminProtobufTransport(send = { frame ->
            val parsed = GarminGfdiFrame.parse(frame)
            if (parsed.messageType == GarminMessageId.RESPONSE) acks.add(parsed)
        })

        transport.handleInbound(reply(4242, b(0x62, 0x00)))

        val ack = acks.single().payload
        // [u16 acked type][u8 ACK][u16 requestId][u32 offset][kept][no error]
        assertEquals(11, ack.size)
        assertEquals(4242, (ack[3].toInt() and 0xFF) or ((ack[4].toInt() and 0xFF) shl 8))
        assertArrayEquals(b(0, 0, 0, 0), ack.copyOfRange(5, 9))
        assertArrayEquals(b(0, 0), ack.copyOfRange(9, 11))
    }

    @Test
    fun `a reply for an unknown id is consumed not mistaken for ours`() = runTest {
        val transport = GarminProtobufTransport(send = { })
        // The watch starts conversations of its own; an unmatched id must not resolve somebody else's request.
        assertTrue(transport.handleInbound(reply(999, b(0x01))))
    }

    @Test
    fun `reassembles a chunked reply`() = runTest {
        lateinit var transport: GarminProtobufTransport
        transport = GarminProtobufTransport(send = { frame ->
            val parsed = GarminGfdiFrame.parse(frame)
            // Answer the request only; replying to chunk acknowledgements recurses.
            if (parsed.messageType != GarminMessageId.PROTOBUF_REQUEST) return@GarminProtobufTransport
            val id = parsed.payload[0].toInt() and 0xFF
            transport.handleInbound(reply(id, b(1, 2, 3), offset = 0, total = 6))
            transport.handleInbound(reply(id, b(4, 5, 6), offset = 3, total = 6))
        })

        assertArrayEquals(
            b(1, 2, 3, 4, 5, 6),
            transport.request(GarminFindMyWatch.start()),
        )
    }

    @Test
    fun `a dropped link fails the request instead of hanging on it`() = runTest {
        val transport = GarminProtobufTransport(send = { })
        var failure: Throwable? = null
        val job = launch {
            try {
                transport.request(GarminFindMyWatch.start())
            } catch (error: IllegalStateException) {
                failure = error
            }
        }
        yield() // Let the request register itself.
        transport.abort()
        job.join()
        assertTrue(failure is IllegalStateException)
    }

    @Test
    fun `an oversized payload is refused rather than truncated`() = runTest {
        val transport = GarminProtobufTransport(send = { })
        try {
            transport.request(ByteArray(400))
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // Expected.
        }
    }

    // Unsolicited chunking.

    @Test
    fun `reassembles a message the watch sent under its OWN id`() = runTest {
        // The watch answers a settings request with its own id, so accumulation cannot be keyed on waiting.
        val delivered = mutableListOf<ByteArray>()
        val acks = mutableListOf<GarminGfdiFrame>()
        val transport = GarminProtobufTransport(
            send = { frame -> acks.add(GarminGfdiFrame.parse(frame)) },
            onUnsolicited = { delivered.add(it) },
        )

        transport.handleInbound(requestChunk(324, b(1, 2, 3), 0, 6))
        assertTrue("incomplete must not be delivered", delivered.isEmpty())
        transport.handleInbound(requestChunk(324, b(4, 5, 6), 3, 6))

        assertArrayEquals(b(1, 2, 3, 4, 5, 6), delivered.single())
    }

    @Test
    fun `acknowledges a chunk with the offset IT declared`() = runTest {
        // Not the next offset. Echoing `dataOffset + chunkLength` made the watch resend chunk zero forever.
        val acks = mutableListOf<GarminGfdiFrame>()
        val transport = GarminProtobufTransport(
            send = { frame -> acks.add(GarminGfdiFrame.parse(frame)) },
            onUnsolicited = { },
        )

        transport.handleInbound(requestChunk(324, b(4, 5, 6), 3, 6))

        val payload = acks.single().payload
        assertEquals(GarminMessageId.RESPONSE, acks.single().messageType)
        // [u16 originalType][u8 status][u16 requestId][u32 dataOffset][u8][u8]
        val requestId = (payload[3].toInt() and 0xFF) or ((payload[4].toInt() and 0xFF) shl 8)
        val offset = (payload[5].toInt() and 0xFF) or
            ((payload[6].toInt() and 0xFF) shl 8) or
            ((payload[7].toInt() and 0xFF) shl 16) or
            ((payload[8].toInt() and 0xFF) shl 24)
        assertEquals(324, requestId)
        assertEquals("the offset received, not the next expected", 3, offset)
    }
}
