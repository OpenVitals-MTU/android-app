package tech.mmarca.openvitals.devices.garmin

import java.util.zip.Deflater
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GarminFileSyncProtocolTest {

    private fun b(vararg xs: Int) = ByteArray(xs.size) { xs[it].toByte() }

    @Test
    fun `list response preserves inherited type names and fixed64 ids`() {
        val id1 = ProtobufWriter().fixed64(1, 11).fixed64(2, 22).toBytes()
        val typeNamed = ProtobufWriter().string(2, "FIT_TYPE_49").varint(3, 7).toBytes()
        val typeInherited = ProtobufWriter().varint(3, 7).toBytes()
        val first = ProtobufWriter()
            .nested(1, id1)
            .nested(2, typeNamed)
            .varint(3, 100)
            .toBytes()
        val id2 = ProtobufWriter().fixed64(1, 33).fixed64(2, 44).toBytes()
        val second = ProtobufWriter()
            .nested(1, id2)
            .nested(2, typeInherited)
            .varint(3, 200)
            .toBytes()
        val response = ProtobufWriter()
            .varint(2, 9)
            .nested(4, first)
            .nested(4, second)
            .toBytes()
        val service = ProtobufWriter().nested(10, response).toBytes()
        val smart = ProtobufWriter().nested(GarminSmartService.FILE_SYNC, service).toBytes()

        val page = checkNotNull(GarminFileSyncProtocol.parseListResponse(smart))

        assertEquals(2, page.files.size)
        assertEquals("FIT_TYPE_49", page.files[1].typeName)
        assertEquals(GarminFileType.SLEEP, page.files[1].type)
        assertEquals(9L, page.cursorId)
    }

    @Test
    fun `new-file and start-sync notices are announcements and a listing is not`() {
        assertTrue(GarminFileSyncProtocol.isSyncAnnouncement(smartFileSync(12)))
        assertTrue(GarminFileSyncProtocol.isSyncAnnouncement(smartFileSync(22)))
        assertFalse(GarminFileSyncProtocol.isSyncAnnouncement(smartFileSync(10)))
        assertFalse(GarminFileSyncProtocol.isSyncAnnouncement(ProtobufWriter().varint(8, 1).toBytes()))
    }

    @Test
    fun `file request bytes match the FileRequest wire format`() {
        val file = GarminFileSyncProtocol.RemoteFile(
            id = GarminFileSyncProtocol.FileId(1, 2),
            typeName = "FIT_TYPE_4",
            typeCode = 4,
            size = 10,
        )

        // Smart{43: FileSyncService{1: FileRequest{1: File{1: FileId{1,2}, 2: Type{"FIT_TYPE_4", 4}, 3: 10},
        // 2: 24, 3: 0, 4: 0, 5: 15}}}, encoded by hand.
        assertArrayEquals(
            b(
                0xDA, 0x02, 0x32, 0x0A, 0x30, 0x0A, 0x26, 0x0A, 0x12,
                0x09, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x11, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x12, 0x0E, 0x12, 0x0A, 0x46, 0x49, 0x54, 0x5F, 0x54, 0x59, 0x50, 0x45, 0x5F, 0x34, 0x18, 0x04,
                0x18, 0x0A, 0x10, 0x18, 0x18, 0x00, 0x20, 0x00, 0x28, 0x0F,
            ),
            GarminFileSyncProtocol.fileRequest(file),
        )
    }

    @Test
    fun `mark-synced bytes carry the FLAGS_SYNCED id twice`() {
        val file = GarminFileSyncProtocol.RemoteFile(
            id = GarminFileSyncProtocol.FileId(1, 2),
            typeName = "FIT_TYPE_4",
            typeCode = 4,
            size = 10,
        )

        // Smart{43: FileSyncService{15: FileSetFlags{1: FileId{1,2}, 2: FileId{42405, 42405}}}}.
        assertArrayEquals(
            b(
                0xDA, 0x02, 0x2A, 0x7A, 0x28,
                0x0A, 0x12, 0x09, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x11, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x12, 0x12, 0x09, 0xA5, 0xA5, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x11, 0xA5, 0xA5, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            ),
            GarminFileSyncProtocol.markSynced(file),
        )
    }

    @Test
    fun `file response status and handle are read from FileResponse`() {
        // Smart{43: FileSyncService{2: FileResponse{1: status 0, 3: handle 7}}}.
        val response = GarminFileSyncProtocol.parseFileResponse(
            b(0xDA, 0x02, 0x06, 0x12, 0x04, 0x08, 0x00, 0x18, 0x07),
        )

        assertNotNull(response)
        assertEquals(0, response?.status)
        assertEquals(7, response?.handle)
        assertNull(GarminFileSyncProtocol.parseFileResponse(b(0x08, 0x01)))
    }

    @Test
    fun `requests carry the FileSyncService smart field`() {
        val request = GarminFileSyncProtocol.listRequest()
        assertTrue(GarminFileSyncProtocol.isFileSyncMessage(request))
    }

    @Test
    fun `next page id is parsed and sent back as start page`() {
        val response = ProtobufWriter().varint(3, 33_737).toBytes()
        val service = ProtobufWriter().nested(10, response).toBytes()
        val smart = ProtobufWriter().nested(GarminSmartService.FILE_SYNC, service).toBytes()

        val page = checkNotNull(GarminFileSyncProtocol.parseListResponse(smart))
        assertEquals(33_737L, page.nextPageId)

        val request = GarminFileSyncProtocol.listRequest(startPageId = page.nextPageId)
        val smartFields = readProtobuf(request)
        val fileSync = checkNotNull(protobufField(smartFields, GarminSmartService.FILE_SYNC)?.bytes)
        val listRequest = checkNotNull(protobufField(readProtobuf(fileSync), 9)?.bytes)
        assertEquals(33_737L, protobufField(readProtobuf(listRequest), 2)?.varint)
    }

    @Test
    fun `file payload inflation succeeds for ordinary zlib data`() {
        val original = "garmin-file".repeat(100).toByteArray()

        assertArrayEquals(original, GarminFileSyncProtocol.inflateFilePayload(deflate(original)))
    }

    @Test
    fun `file payload inflation rejects streams that require a dictionary`() {
        val dictionary = "garmin-dictionary".toByteArray()
        val original = "garmin-dictionary-payload".repeat(20).toByteArray()

        assertNull(
            GarminFileSyncProtocol.inflateFilePayload(
                deflate(original, dictionary = dictionary),
            ),
        )
    }

    private fun deflate(bytes: ByteArray, dictionary: ByteArray? = null): ByteArray {
        val deflater = Deflater()
        return try {
            dictionary?.let(deflater::setDictionary)
            deflater.setInput(bytes)
            deflater.finish()
            val output = ByteArray(bytes.size * 2)
            output.copyOf(deflater.deflate(output))
        } finally {
            deflater.end()
        }
    }
}
