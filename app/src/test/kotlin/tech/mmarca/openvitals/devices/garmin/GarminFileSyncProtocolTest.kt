package tech.mmarca.openvitals.devices.garmin

import java.util.zip.Deflater
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GarminFileSyncProtocolTest {

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

        val page = GarminFileSyncProtocol.parseListResponse(smart)

        assertNotNull(page)
        assertEquals(2, page!!.files.size)
        assertEquals("FIT_TYPE_49", page.files[1].typeName)
        assertEquals(GarminFileType.SLEEP, page.files[1].type)
        assertEquals(9L, page.cursorId)
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

        val page = GarminFileSyncProtocol.parseListResponse(smart)!!
        assertEquals(33_737L, page.nextPageId)

        val request = GarminFileSyncProtocol.listRequest(startPageId = page.nextPageId)
        val smartFields = readProtobuf(request)
        val fileSync = protobufField(smartFields, GarminSmartService.FILE_SYNC)!!.bytes!!
        val listRequest = protobufField(readProtobuf(fileSync), 9)!!.bytes!!
        assertEquals(33_737L, protobufField(readProtobuf(listRequest), 2)!!.varint)
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
