package tech.mmarca.openvitals.devices.garmin

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The upload messages, byte for byte against upstream's layouts. */
class GarminUploadMessagesTest {

    private fun b(vararg xs: Int) = ByteArray(xs.size) { xs[it].toByte() }

    private fun response(vararg payload: Int): GarminInboundMessage =
        decodeGarminMessage(GarminGfdiFrame.parse(GarminGfdiFrame.build(GarminMessageId.RESPONSE, b(*payload))))

    // 5005 = 0x138D, 5003 = 0x138B, 5004 = 0x138C.

    @Test fun `create file carries size, type, any-number mask and the id`() {
        val frame = GarminGfdiFrame.parse(
            buildCreateFile(size = 0x0102, type = GarminUploadFileType.LOCATION, fileId = 0x1122334455667788L),
        )

        assertEquals(GarminMessageId.CREATE_FILE, frame.messageType)
        assertArrayEquals(
            b(
                0x02, 0x01, 0x00, 0x00, // size
                128, 8, // data type, sub-type
                0x00, 0x00, // file index
                0x00, // reserved
                0x00, // sub-type mask
                0xFF, 0xFF, // number mask
                0x00, 0x00, // path length
                0x88, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11, // id
            ),
            frame.payload,
        )
    }

    @Test fun `upload request carries the index and size, offset and seed zero`() {
        val frame = GarminGfdiFrame.parse(buildUploadRequest(fileIndex = 0x0A0B, size = 0x0102))

        assertEquals(GarminMessageId.UPLOAD_REQUEST, frame.messageType)
        assertArrayEquals(
            b(0x0B, 0x0A, 0x02, 0x01, 0x00, 0x00, 0, 0, 0, 0, 0, 0),
            frame.payload,
        )
    }

    @Test fun `a data chunk carries flags, running CRC, offset, then the bytes`() {
        val frame = GarminGfdiFrame.parse(
            buildFileTransferData(chunk = b(9, 8, 7), dataOffset = 0x0100, runningCrc = 0xBEEF),
        )

        assertEquals(GarminMessageId.FILE_TRANSFER_DATA, frame.messageType)
        assertArrayEquals(b(0x00, 0xEF, 0xBE, 0x00, 0x01, 0x00, 0x00, 9, 8, 7), frame.payload)
    }

    @Test fun `a create reply gives the slot`() {
        // status ACK, create OK, index 0x0203, type 128/8, number 1
        val message = response(0x8D, 0x13, 0, 0, 0x03, 0x02, 128, 8, 1, 0) as GarminCreateFileStatus

        assertTrue(message.canProceed)
        assertEquals(0x0203, message.fileIndex)
    }

    @Test fun `a create refusal keeps its reason, even when it stops short`() {
        val full = response(0x8D, 0x13, 0, 2, 0, 0, 128, 8, 0, 0) as GarminCreateFileStatus
        val short = response(0x8D, 0x13, 0, 3) as GarminCreateFileStatus

        assertEquals(GarminCreateFileResult.NO_SPACE, full.result)
        assertEquals(GarminCreateFileResult.UNSUPPORTED, short.result)
        assertFalse(full.canProceed || short.canProceed)
    }

    @Test fun `a broken create reply is invalid, never a crash`() {
        val nak = response(0x8D, 0x13, 1) as GarminCreateFileStatus
        val empty = response(0x8D, 0x13) as GarminCreateFileStatus
        val okWithoutIndex = response(0x8D, 0x13, 0, 0) as GarminCreateFileStatus
        val unknownCode = response(0x8D, 0x13, 0, 42, 1, 0) as GarminCreateFileStatus

        assertEquals(GarminStatus.NAK, nak.status)
        listOf(nak, empty, okWithoutIndex, unknownCode).forEach {
            assertEquals(GarminCreateFileResult.INVALID, it.result)
            assertFalse(it.canProceed)
        }
    }

    @Test fun `an upload reply gives the offset and the room`() {
        // ACK, OK, offset 0, max size 0x00010000, seed 0
        val message = response(0x8B, 0x13, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0) as GarminUploadRequestStatus

        assertTrue(message.canProceed)
        assertEquals(0L, message.dataOffset)
        assertEquals(0x10000L, message.maxFileSize)
    }

    @Test fun `an upload refusal or a short reply cannot proceed`() {
        val refused = response(0x8B, 0x13, 0, 3) as GarminUploadRequestStatus
        val okButShort = response(0x8B, 0x13, 0, 0, 0, 0) as GarminUploadRequestStatus
        val nak = response(0x8B, 0x13, 1) as GarminUploadRequestStatus

        assertEquals(GarminUploadStatus.NO_SPACE_LEFT, refused.uploadStatus)
        assertEquals(GarminUploadStatus.INVALID, okButShort.uploadStatus)
        assertFalse(refused.canProceed || okButShort.canProceed || nak.canProceed)
    }

    @Test fun `a chunk reply gives the verdict and how far the watch got`() {
        val ok = response(0x8C, 0x13, 0, 0, 0x00, 0x01, 0, 0) as GarminFileTransferDataStatus
        val resend = response(0x8C, 0x13, 0, 1, 0, 0, 0, 0) as GarminFileTransferDataStatus

        assertEquals(GarminFileTransferStatus.OK, ok.transferStatus)
        assertEquals(0x100L, ok.nextOffset)
        assertEquals(GarminFileTransferStatus.RESEND, resend.transferStatus)
    }

    @Test fun `a chunk reply that cannot be trusted aborts`() {
        val nak = response(0x8C, 0x13, 1) as GarminFileTransferDataStatus
        val okWithoutOffset = response(0x8C, 0x13, 0, 0) as GarminFileTransferDataStatus
        val unknownCode = response(0x8C, 0x13, 0, 9, 0, 0, 0, 0) as GarminFileTransferDataStatus

        listOf(nak, okWithoutOffset, unknownCode).forEach {
            assertEquals(GarminFileTransferStatus.ABORT, it.transferStatus)
        }
    }

    @Test fun `the phone's own download ack decodes as a chunk reply`() {
        // Same shape, so the session must ignore one that no upload is waiting for.
        val message = decodeGarminMessage(GarminGfdiFrame.parse(buildFileTransferDataAck(1024)))

        assertEquals(
            GarminFileTransferDataStatus(GarminStatus.ACK, GarminFileTransferStatus.OK, nextOffset = 1024),
            message,
        )
    }
}
