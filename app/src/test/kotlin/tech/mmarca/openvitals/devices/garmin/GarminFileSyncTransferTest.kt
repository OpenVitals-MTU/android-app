package tech.mmarca.openvitals.devices.garmin

import java.util.zip.Deflater
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The FileSync pull against a fake watch behind both transports: it answers
 * listings and file requests over protobuf, registers transfer services on
 * the ML control channel, and streams a file the way the real one does.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GarminFileSyncTransferTest {

    private fun b(vararg xs: Int) = ByteArray(xs.size) { xs[it].toByte() }

    private class Listed(
        val id: GarminFileSyncProtocol.FileId,
        val typeName: String,
        val typeCode: Int,
        val size: Long,
    )

    private inner class FakeWatch {
        /** List responses handed out in order; a missing one means silence. */
        val listReplies = mutableListOf<ByteArray>()
        var fileStatus = 0
        var fileHandle = 7
        var header = b(0, 0, 0)
        var fileBytes = "garmin-file".repeat(40).toByteArray()

        /** Everything the phone did, in order. */
        val events = mutableListOf<String>()
        val listRequests = mutableListOf<List<ProtobufField>>()

        lateinit var protobuf: GarminProtobufTransport
        lateinit var ml: GarminMlTransport

        suspend fun onGfdi(frame: GarminGfdiFrame) {
            if (frame.messageType != GarminMessageId.PROTOBUF_REQUEST) return
            val reader = GarminByteReader(frame.payload)
            val requestId = reader.readShort()
            reader.readInt() // offset
            reader.readInt() // total
            val length = reader.readInt()
            val bytes = frame.payload.copyOfRange(14, 14 + length.toInt())
            val service = protobufField(readProtobuf(bytes), GarminSmartService.FILE_SYNC)?.bytes ?: return
            val fields = readProtobuf(service)
            when {
                protobufField(fields, 9) != null -> {
                    listRequests += readProtobuf(protobufField(fields, 9)?.bytes ?: return)
                    events += "list"
                    val reply = listReplies.removeFirstOrNull() ?: return
                    protobuf.handleInbound(response(requestId, reply))
                }
                protobufField(fields, 1) != null -> {
                    events += "file"
                    val body = ProtobufWriter().varint(1, fileStatus).varint(3, fileHandle).toBytes()
                    protobuf.handleInbound(response(requestId, GarminFileSyncProtocol.smart(ProtobufWriter().nested(2, body).toBytes())))
                }
                protobufField(fields, 15) != null -> events += "mark"
            }
        }

        suspend fun onMl(packet: ByteArray) {
            if (packet[0].toInt() == 0) {
                val request = packet[1].toInt()
                val serviceCode = (packet[10].toInt() and 0xFF) or ((packet[11].toInt() and 0xFF) shl 8)
                when (request) {
                    0 -> {
                        events += "open $serviceCode"
                        // Plain ML: the watch declines reliable mode, so the test needs no MLR ACKs.
                        ml.handleInbound(control(1, serviceCode, b(0, HANDLE, 0)))
                    }
                    2 -> {
                        events += "close $serviceCode"
                        ml.handleInbound(control(3, serviceCode, b(HANDLE, 0)))
                    }
                }
                return
            }
            // The transfer request on the service handle: stream the file, then close.
            events += "request"
            val stream = header + deflate(fileBytes)
            ml.handleInbound(byteArrayOf(HANDLE.toByte()) + stream.copyOfRange(0, 5))
            ml.handleInbound(byteArrayOf(HANDLE.toByte()) + stream.copyOfRange(5, stream.size))
            ml.handleInbound(control(3, TRANSFER_SERVICE, b(HANDLE, 0)))
        }

        private fun control(response: Int, serviceCode: Int, tail: ByteArray): ByteArray =
            GarminByteWriter().writeByte(0).writeByte(response).writeLong(CLIENT_ID).writeShort(serviceCode).toBytes() + tail

        private fun response(requestId: Int, payload: ByteArray): GarminGfdiFrame {
            val body = GarminByteWriter()
                .writeShort(requestId)
                .writeInt(0)
                .writeInt(payload.size)
                .writeInt(payload.size)
                .writeBytes(payload)
                .toBytes()
            return GarminGfdiFrame.parse(GarminGfdiFrame.build(GarminMessageId.PROTOBUF_RESPONSE, body))
        }
    }

    private class Fixture(val watch: FakeWatch, val transfer: GarminFileSyncTransfer, val kept: List<GarminDownloadedFile>, val proven: () -> Int)

    private fun fixture(): Fixture {
        val watch = FakeWatch()
        val kept = mutableListOf<GarminDownloadedFile>()
        var proven = 0
        watch.protobuf = GarminProtobufTransport(send = { frame -> watch.onGfdi(GarminGfdiFrame.parse(frame)) })
        watch.ml = GarminMlTransport(write = { packet -> watch.onMl(packet) }, onFrame = {})
        val transfer = GarminFileSyncTransfer(
            protobuf = watch.protobuf,
            transport = watch.ml,
            keep = { file ->
                watch.events += "keep"
                kept += file
            },
            onProtocolProven = { proven++ },
            fileDate = { null },
        )
        return Fixture(watch, transfer, kept) { proven }
    }

    private fun listResponse(
        files: List<Listed> = emptyList(),
        cursorId: Long? = null,
        nextPageId: Long? = null,
    ): ByteArray {
        val response = ProtobufWriter()
        cursorId?.let { response.varint(2, it) }
        nextPageId?.let { response.varint(3, it) }
        for (file in files) {
            val id = ProtobufWriter().fixed64(1, file.id.first).fixed64(2, file.id.second).toBytes()
            val type = ProtobufWriter().string(2, file.typeName).varint(3, file.typeCode).toBytes()
            response.nested(4, ProtobufWriter().nested(1, id).nested(2, type).varint(3, file.size).toBytes())
        }
        return GarminFileSyncProtocol.smart(ProtobufWriter().nested(10, response.toBytes()).toBytes())
    }

    private val monitor = Listed(GarminFileSyncProtocol.FileId(11, 22), "FIT_TYPE_32", 32, 440)

    @Test
    fun `downloads a listed file, keeps it, then marks it synced`() = runTest {
        val f = fixture()
        f.watch.listReplies += listResponse(listOf(monitor))

        val files = f.transfer.pull(alreadySynced = emptySet())

        assertEquals(1, files.size)
        assertArrayEquals(f.watch.fileBytes, files.single().bytes)
        assertEquals(GarminFileType.MONITOR, files.single().entry.type)
        assertEquals("sync/11/22/440", files.single().entry.dedupKey)
        // Persist before mark, and the transfer service is closed afterwards.
        assertEquals(
            listOf("list", "file", "open $TRANSFER_SERVICE", "request", "keep", "mark"),
            f.watch.events,
        )
        assertEquals(1, f.proven())
    }

    @Test
    fun `follows a cursor page, then a next page id, and proves the protocol once`() = runTest {
        val f = fixture()
        f.watch.listReplies += listResponse(cursorId = 9)
        f.watch.listReplies += listResponse(nextPageId = 33)
        f.watch.listReplies += listResponse(listOf(monitor))

        val files = f.transfer.pull(alreadySynced = emptySet())

        assertEquals(1, files.size)
        val requests = f.watch.listRequests
        assertEquals(3, requests.size)
        assertEquals(null, protobufField(requests[0], 1))
        assertEquals(9L, protobufField(requests[1], 1)?.varint)
        assertEquals(33L, protobufField(requests[2], 2)?.varint)
        assertEquals(1, f.proven())
    }

    @Test
    fun `a repeated page token ends the listing instead of looping`() = runTest {
        val f = fixture()
        f.watch.listReplies += listResponse(nextPageId = 5)
        f.watch.listReplies += listResponse(nextPageId = 5)
        f.watch.listReplies += listResponse(nextPageId = 5)

        assertTrue(f.transfer.pull(alreadySynced = emptySet()).isEmpty())
        assertEquals(2, f.watch.listRequests.size)
    }

    @Test
    fun `an invalid listing fails the pull`() = runTest {
        val f = fixture()
        f.watch.listReplies += GarminFileSyncProtocol.smart(ProtobufWriter().varint(7, 1).toBytes())

        try {
            f.transfer.pull(alreadySynced = emptySet())
            fail("expected GarminFileSyncException")
        } catch (expected: GarminFileSyncException) {
            assertTrue(expected.message.orEmpty().contains("invalid"))
        }
        assertEquals(0, f.proven())
    }

    @Test
    fun `a silent watch fails the pull as a timeout, not a cancellation`() = runTest {
        val f = fixture()

        val pull = async { runCatching { f.transfer.pull(alreadySynced = emptySet()) } }
        advanceTimeBy(9.seconds)
        runCurrent()

        val failure = pull.await().exceptionOrNull()
        assertTrue(failure is GarminFileSyncException)
        assertTrue(failure?.message.orEmpty().contains("timed out"))
    }

    @Test
    fun `a refused file is skipped without a download`() = runTest {
        val f = fixture()
        f.watch.listReplies += listResponse(listOf(monitor))
        f.watch.fileStatus = 1

        assertTrue(f.transfer.pull(alreadySynced = emptySet()).isEmpty())
        assertEquals(listOf("list", "file"), f.watch.events)
    }

    @Test
    fun `a wrong header discards the file and still closes the service`() = runTest {
        val f = fixture()
        f.watch.listReplies += listResponse(listOf(monitor))
        f.watch.header = b(1, 0, 0)

        assertTrue(f.transfer.pull(alreadySynced = emptySet()).isEmpty())
        assertTrue(f.watch.events.none { it == "keep" || it == "mark" })
        assertTrue(f.kept.isEmpty())
    }

    @Test
    fun `already synced and unwanted files are not requested`() = runTest {
        val f = fixture()
        val unknown = Listed(GarminFileSyncProtocol.FileId(1, 2), "FIT_TYPE_255", 255, 10)
        f.watch.listReplies += listResponse(listOf(monitor, unknown))

        val files = f.transfer.pull(alreadySynced = setOf("sync/11/22/440"))

        assertTrue(files.isEmpty())
        assertEquals(listOf("list"), f.watch.events)
    }

    private companion object {
        const val CLIENT_ID = 2L
        const val HANDLE = 5
        const val TRANSFER_SERVICE = 0x2018

        fun deflate(bytes: ByteArray): ByteArray {
            val deflater = Deflater()
            return try {
                deflater.setInput(bytes)
                deflater.finish()
                val output = ByteArray(bytes.size + 64)
                output.copyOf(deflater.deflate(output))
            } finally {
                deflater.end()
            }
        }
    }
}
