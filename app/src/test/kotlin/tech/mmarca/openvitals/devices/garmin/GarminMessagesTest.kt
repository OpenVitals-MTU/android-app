package tech.mmarca.openvitals.devices.garmin

import java.time.Instant
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Port of the Flutter build's `garmin_messages_test.dart` — fixtures identical. */
class GarminMessagesTest {

    private fun b(vararg xs: Int) = ByteArray(xs.size) { xs[it].toByte() }

    /**
     * Round-trips an outgoing message through the frame layer, as the
     * transport will: build → parse → decode.
     */
    private fun roundTrip(wire: ByteArray): GarminInboundMessage =
        decodeGarminMessage(GarminGfdiFrame.parse(wire))

    private fun payloadShort(payload: ByteArray, at: Int = 0): Int =
        (payload[at].toInt() and 0xFF) or ((payload[at + 1].toInt() and 0xFF) shl 8)

    // ── GarminTime ───────────────────────────────────────────────────────────

    @Test
    fun `the Garmin epoch maps to 1989-12-31T000000Z`() {
        assertEquals(Instant.parse("1989-12-31T00:00:00Z"), GarminTime.toInstant(0))
    }

    @Test
    fun `round-trips a real recording instant`() {
        val whenRecorded = Instant.parse("2026-07-22T09:30:00Z")
        assertEquals(
            whenRecorded,
            GarminTime.toInstant(GarminTime.fromInstant(whenRecorded)),
        )
    }

    // ── GarminFileType ───────────────────────────────────────────────────────

    @Test
    fun `maps the FIT sub-types the importer consumes`() {
        assertEquals(GarminFileType.ACTIVITY, GarminFileType.fromCodes(128, 4))
        assertEquals(GarminFileType.SLEEP, GarminFileType.fromCodes(128, 49))
        assertEquals(GarminFileType.HRV_STATUS, GarminFileType.fromCodes(128, 68))
        assertEquals(GarminFileType.MONITOR, GarminFileType.fromCodes(128, 32))
    }

    @Test
    fun `an unmapped sub-type is null not an error`() {
        // Golf scorecard (128,55) — a real type this app does not handle.
        assertNull(GarminFileType.fromCodes(128, 55))
    }

    @Test
    fun `virtual types are not wanted by the downloader`() {
        assertFalse(GarminFileType.DIRECTORY.wanted)
        assertFalse(GarminFileType.DEVICE_XML.wanted)
        assertTrue(GarminFileType.ACTIVITY.wanted)
    }

    // ── GarminDirectory.parse ────────────────────────────────────────────────

    /** Builds one 16-byte directory record. */
    private fun entry(
        index: Int,
        dataType: Int,
        subType: Int,
        number: Int = 1,
        size: Int = 100,
        timestamp: Long = 0,
    ): ByteArray = GarminByteWriter()
        .writeShort(index)
        .writeByte(dataType)
        .writeByte(subType)
        .writeShort(number)
        .writeByte(0)
        .writeByte(0)
        .writeInt(size)
        .writeInt(timestamp)
        .toBytes()

    @Test
    fun `keeps wanted FIT files and resolves their fields`() {
        val ts = GarminTime.fromInstant(Instant.parse("2026-07-20T03:00:00Z"))
        val data = entry(index = 5, dataType = 128, subType = 49, number = 7, size = 2048, timestamp = ts)

        val entries = GarminDirectory.parse(data)

        assertEquals(1, entries.size)
        val e = entries.single()
        assertEquals(5, e.fileIndex)
        assertEquals(GarminFileType.SLEEP, e.type)
        assertEquals(7, e.fileNumber)
        assertEquals(2048L, e.fileSize)
        assertEquals(Instant.parse("2026-07-20T03:00:00Z"), e.fileDate)
        assertEquals("128/49/7", e.dedupKey)
    }

    @Test
    fun `drops unmapped types and the all-zero sentinel`() {
        val data = entry(index = 5, dataType = 128, subType = 49) + // sleep — kept
            entry(index = 6, dataType = 128, subType = 55) + // golf — unmapped
            entry(index = 0, dataType = 0, subType = 0, number = 0, size = 0) // pad

        val entries = GarminDirectory.parse(data)

        assertEquals(listOf(GarminFileType.SLEEP), entries.map { it.type })
    }

    @Test
    fun `a zero wire timestamp becomes a null date not the Garmin epoch`() {
        val data = entry(index = 5, dataType = 128, subType = 4, timestamp = 0)
        assertNull(GarminDirectory.parse(data).single().fileDate)
    }

    @Test
    fun `an unset file number yields NO dedup key`() {
        // A real vívoactive 5 returned two DIFFERENT sleep files both numbered
        // 65535. Keying on that collapsed them into one and would have made
        // every future sleep file look already-synced — silent, permanent data
        // loss.
        val entries = GarminDirectory.parse(
            entry(index = 113, dataType = 128, subType = 49, number = 0xFFFF) +
                entry(index = 116, dataType = 128, subType = 49, number = 0xFFFF) +
                entry(index = 121, dataType = 128, subType = 32, number = 136),
        )

        assertEquals(3, entries.size)
        assertNull(entries[0].dedupKey)
        assertNull(entries[1].dedupKey)
        // A real file number still keys normally.
        assertEquals("128/32/136", entries[2].dedupKey)
    }

    @Test
    fun `diagnostics distinguish empty from filtered-out`() {
        // "0 entries" has several causes; the listing must say which.
        val empty = GarminDirectory.parseWithDiagnostics(ByteArray(0))
        assertEquals(0, empty.totalRecords)
        assertTrue(empty.entries.isEmpty())

        val filtered = GarminDirectory.parseWithDiagnostics(
            entry(index = 6, dataType = 128, subType = 55) + // unmapped
                entry(index = 7, dataType = 8, subType = 255), // known but not wanted
        )
        assertEquals(2, filtered.totalRecords)
        assertTrue(filtered.entries.isEmpty())
        // The raw codes are what make an unmapped type diagnosable on a
        // device, and the INDEX is what lets an entry be matched against the
        // files the watch announces separately over the protobuf
        // FileSyncService.
        assertTrue(filtered.skipped.contains("6:128/55?"))
        assertTrue(filtered.skipped.contains("7:deviceXml!"))
        assertEquals(listOf(6, 7), filtered.allIndexes)
    }

    @Test
    fun `a trailing partial record is ignored`() {
        val data = entry(index = 5, dataType = 128, subType = 4) +
            b(0x01, 0x02, 0x03) // 3 stray bytes — not a whole record
        assertEquals(1, GarminDirectory.parse(data).size)
    }

    // ── outbound messages round-trip through the frame layer ─────────────────

    @Test
    fun `download request carries the file index and fresh type`() {
        val status = roundTrip(buildDownloadRequest(fileIndex = 5))
        // The watch replies with a status; the request itself is not inbound,
        // so assert on the raw frame instead.
        assertTrue(status is GarminUnhandledMessage)
        val frame = GarminGfdiFrame.parse(buildDownloadRequest(fileIndex = 5))
        assertEquals(GarminMessageId.DOWNLOAD_REQUEST, frame.messageType)
        // fileIndex is the first short of the payload.
        assertEquals(5, payloadShort(frame.payload))
    }

    @Test
    fun `file-transfer ack names FILE_TRANSFER_DATA with ACK and OK`() {
        val frame = GarminGfdiFrame.parse(buildFileTransferDataAck(1024))
        assertEquals(GarminMessageId.RESPONSE, frame.messageType)
        // [short originalType][byte ACK][byte OK][int offset]
        assertEquals(GarminMessageId.FILE_TRANSFER_DATA, payloadShort(frame.payload))
        assertEquals(GarminStatus.ACK.code, frame.payload[2].toInt())
        assertEquals(0, frame.payload[3].toInt())
    }

    @Test
    fun `archive flag is 0x10`() {
        val frame = GarminGfdiFrame.parse(buildSetFileFlags(5, GarminFileFlag.ARCHIVE))
        assertEquals(GarminMessageId.SET_FILE_FLAGS, frame.messageType)
        assertEquals(0x10, frame.payload.last().toInt())
    }

    @Test
    fun `SYNC_READY encodes as system-event ordinal 8`() {
        val frame = GarminGfdiFrame.parse(buildSystemEvent(GarminSystemEventType.SYNC_READY))
        assertEquals(GarminMessageId.SYSTEM_EVENT, frame.messageType)
        assertEquals(8, frame.payload[0].toInt())
    }

    // ── inbound message decoding ─────────────────────────────────────────────

    /** Builds a RESPONSE (5000) frame carrying a download-request status. */
    private fun downloadStatusFrame(
        status: Int,
        downloadStatus: Int,
        maxFileSize: Int,
    ): ByteArray {
        val payload = GarminByteWriter()
            .writeShort(GarminMessageId.DOWNLOAD_REQUEST)
            .writeByte(status)
            .writeByte(downloadStatus)
            .writeInt(maxFileSize)
        return GarminGfdiFrame.build(GarminMessageId.RESPONSE, payload.toBytes())
    }

    @Test
    fun `a proceed-able download status carries the file size`() {
        val msg = roundTrip(
            downloadStatusFrame(
                status = GarminStatus.ACK.code,
                downloadStatus = 0, // OK
                maxFileSize = 4096,
            ),
        )
        assertTrue(msg is GarminDownloadRequestStatus)
        val status = msg as GarminDownloadRequestStatus
        assertTrue(status.canProceed)
        assertEquals(4096L, status.maxFileSize)
    }

    @Test
    fun `a non-OK download status does not proceed`() {
        val msg = roundTrip(
            downloadStatusFrame(
                status = GarminStatus.ACK.code,
                downloadStatus = 1, // INDEX_UNKNOWN
                maxFileSize = 0,
            ),
        ) as GarminDownloadRequestStatus
        assertFalse(msg.canProceed)
        assertEquals(GarminDownloadStatus.INDEX_UNKNOWN, msg.downloadStatus)
    }

    @Test
    fun `a file-transfer data chunk exposes offset crc and payload`() {
        val payload = GarminByteWriter()
            .writeByte(0) // flags
            .writeShort(0xBEEF) // crc
            .writeInt(2048) // dataOffset
            .writeBytes(b(1, 2, 3, 4))
        val msg = roundTrip(
            GarminGfdiFrame.build(GarminMessageId.FILE_TRANSFER_DATA, payload.toBytes()),
        )
        assertTrue(msg is GarminFileTransferData)
        val chunk = msg as GarminFileTransferData
        assertEquals(2048L, chunk.dataOffset)
        assertEquals(0xBEEF, chunk.crc)
        assertArrayEquals(b(1, 2, 3, 4), chunk.data)
    }

    @Test
    fun `an out-of-vocabulary message decodes to unhandled not an error`() {
        // Music control (5041) — a message the watch sends that a read-only
        // sync ignores.
        val msg = roundTrip(GarminGfdiFrame.build(5041, b(1, 2, 3)))
        assertTrue(msg is GarminUnhandledMessage)
        assertEquals(5041, (msg as GarminUnhandledMessage).messageType)
    }
}
