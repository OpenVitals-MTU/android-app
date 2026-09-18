package tech.mmarca.openvitals.devices.garmin

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The upload conversation against a scripted watch, no radio. */
class GarminFileUploaderTest {

    /** Answers each upload frame the way a watch does, with knobs for every way it can go wrong. */
    private class FakeWatch {
        lateinit var uploader: GarminFileUploader
        val received = mutableListOf<GarminGfdiFrame>()
        val stored = mutableListOf<Byte>()
        var assignedIndex = 7
        var createResult = GarminCreateFileResult.OK
        var createEnvelope = GarminStatus.ACK
        var uploadStatus = GarminUploadStatus.OK
        var startOffset = 0L
        var room = 4096L

        /** Verdicts for successive data frames. Runs out to OK. */
        val verdicts = ArrayDeque<GarminFileTransferStatus>()

        /** Steps the watch stays silent on. */
        val silentOn = mutableSetOf<Int>()
        var lieAboutOffset = false
        private var runningCrc = 0

        fun onFrame(frame: GarminGfdiFrame) {
            received += frame
            if (frame.messageType in silentOn) return
            when (frame.messageType) {
                GarminMessageId.CREATE_FILE -> uploader.onCreateFileStatus(
                    GarminCreateFileStatus(createEnvelope, createResult, assignedIndex),
                )
                GarminMessageId.UPLOAD_REQUEST -> uploader.onUploadRequestStatus(
                    GarminUploadRequestStatus(GarminStatus.ACK, uploadStatus, startOffset, room),
                )
                GarminMessageId.FILE_TRANSFER_DATA -> onData(frame.payload)
            }
        }

        private fun onData(payload: ByteArray) {
            val reader = GarminByteReader(payload)
            reader.readByte()
            val crc = reader.readShort()
            val offset = reader.readInt()
            val data = reader.readBytes(reader.remaining)
            val verdict = verdicts.removeFirstOrNull() ?: GarminFileTransferStatus.OK
            if (verdict == GarminFileTransferStatus.OK) {
                // A real watch checks both, so the fake does too.
                assertEquals("chunk offset", stored.size.toLong(), offset)
                runningCrc = GarminCrc.compute(data, initialCrc = runningCrc)
                assertEquals("running CRC", runningCrc, crc)
                stored += data.toList()
            }
            val reached = if (lieAboutOffset) 1L else stored.size.toLong()
            uploader.onDataStatus(GarminFileTransferDataStatus(GarminStatus.ACK, verdict, reached))
        }

        fun count(messageType: Int) = received.count { it.messageType == messageType }
    }

    private fun uploaderFor(watch: FakeWatch, chunk: Int = 8): GarminFileUploader =
        GarminFileUploader(
            send = { frame -> watch.onFrame(GarminGfdiFrame.parse(frame)) },
            replyTimeout = 10.seconds,
            maxChunkSize = chunk,
            newFileId = { 42L },
        ).also { watch.uploader = it }

    private val file = ByteArray(21) { (it * 7 + 3).toByte() }

    private suspend fun GarminFileUploader.sendFile(maxPacketSize: Int? = null) =
        upload(GarminUploadFileType.LOCATION, file, maxPacketSize)

    @Test fun `a file goes over in chunks with a cumulative CRC, then sync complete`() = runTest {
        val watch = FakeWatch()

        val result = uploaderFor(watch).sendFile()

        assertEquals(GarminUploadResult.Sent, result)
        assertArrayEquals(file, watch.stored.toByteArray())
        assertEquals(3, watch.count(GarminMessageId.FILE_TRANSFER_DATA)) // 8 + 8 + 5
        // The upload request names the slot the watch assigned.
        val request = watch.received.single { it.messageType == GarminMessageId.UPLOAD_REQUEST }
        assertEquals(7, GarminByteReader(request.payload).readShort())
        assertEquals(GarminMessageId.SYSTEM_EVENT, watch.received.last().messageType)
        assertEquals(GarminSystemEventType.SYNC_COMPLETE.ordinal, watch.received.last().payload[0].toInt())
    }

    @Test fun `the watch's packet size caps a chunk, the ceiling still applies`() = runTest {
        val small = FakeWatch()
        uploaderFor(small, chunk = 300).sendFile(maxPacketSize = 13 + 4)
        assertEquals(6, small.count(GarminMessageId.FILE_TRANSFER_DATA)) // 21 bytes in fours

        val large = FakeWatch()
        uploaderFor(large, chunk = 8).sendFile(maxPacketSize = 500)
        assertEquals(3, large.count(GarminMessageId.FILE_TRANSFER_DATA))

        val nonsense = FakeWatch()
        uploaderFor(nonsense, chunk = 8).sendFile(maxPacketSize = 5)
        assertEquals(3, nonsense.count(GarminMessageId.FILE_TRANSFER_DATA))
    }

    @Test fun `each create refusal is reported and nothing more is sent`() = runTest {
        val expected = mapOf(
            GarminCreateFileResult.DUPLICATE to GarminUploadRefusal.DUPLICATE,
            GarminCreateFileResult.NO_SPACE to GarminUploadRefusal.NO_SPACE,
            GarminCreateFileResult.NO_SPACE_FOR_TYPE to GarminUploadRefusal.NO_SPACE,
            GarminCreateFileResult.UNSUPPORTED to GarminUploadRefusal.UNSUPPORTED,
            GarminCreateFileResult.NO_SLOTS to GarminUploadRefusal.NO_SLOTS,
            GarminCreateFileResult.INVALID to GarminUploadRefusal.INVALID_REPLY,
        )
        expected.forEach { (result, refusal) ->
            val watch = FakeWatch().apply { createResult = result }

            assertEquals(GarminUploadResult.Refused(refusal), uploaderFor(watch).sendFile())
            assertEquals(1, watch.received.size)
        }
    }

    @Test fun `a watch that does not know the message is unsupported`() = runTest {
        val watch = FakeWatch().apply {
            createEnvelope = GarminStatus.UNSUPPORTED
            createResult = GarminCreateFileResult.INVALID
        }

        assertEquals(
            GarminUploadResult.Refused(GarminUploadRefusal.UNSUPPORTED),
            uploaderFor(watch).sendFile(),
        )
    }

    @Test fun `an upload refusal, a resume offset or too little room stop before any data`() = runTest {
        val refused = FakeWatch().apply { uploadStatus = GarminUploadStatus.NO_SPACE_LEFT }
        val resuming = FakeWatch().apply { startOffset = 8 }
        val tight = FakeWatch().apply { room = 20 }
        val unstated = FakeWatch().apply { room = 0 }

        assertEquals(GarminUploadResult.Refused(GarminUploadRefusal.NO_SPACE), uploaderFor(refused).sendFile())
        assertEquals(GarminUploadResult.Refused(GarminUploadRefusal.OFFSET_MISMATCH), uploaderFor(resuming).sendFile())
        assertEquals(GarminUploadResult.Refused(GarminUploadRefusal.NO_SPACE), uploaderFor(tight).sendFile())
        listOf(refused, resuming, tight).forEach {
            assertEquals(0, it.count(GarminMessageId.FILE_TRANSFER_DATA))
        }
        // Zero room means the watch did not say.
        assertEquals(GarminUploadResult.Sent, uploaderFor(unstated).sendFile())
    }

    @Test fun `one resend repeats the chunk byte for byte`() = runTest {
        val watch = FakeWatch().apply {
            verdicts += listOf(GarminFileTransferStatus.OK, GarminFileTransferStatus.RESEND)
        }

        assertEquals(GarminUploadResult.Sent, uploaderFor(watch).sendFile())

        val data = watch.received.filter { it.messageType == GarminMessageId.FILE_TRANSFER_DATA }
        assertEquals(4, data.size)
        assertArrayEquals(data[1].payload, data[2].payload)
        assertArrayEquals(file, watch.stored.toByteArray())
    }

    @Test fun `a second resend of the same chunk gives up`() = runTest {
        val watch = FakeWatch().apply {
            verdicts += listOf(GarminFileTransferStatus.RESEND, GarminFileTransferStatus.RESEND)
        }

        assertEquals(
            GarminUploadResult.Refused(GarminUploadRefusal.RESEND_LIMIT),
            uploaderFor(watch).sendFile(),
        )
        assertEquals(0, watch.count(GarminMessageId.SYSTEM_EVENT))
    }

    @Test fun `every other verdict stops the upload with its reason`() = runTest {
        val expected = mapOf(
            GarminFileTransferStatus.ABORT to GarminUploadRefusal.TRANSFER_ABORTED,
            GarminFileTransferStatus.CRC_MISMATCH to GarminUploadRefusal.CRC_MISMATCH,
            GarminFileTransferStatus.OFFSET_MISMATCH to GarminUploadRefusal.OFFSET_MISMATCH,
            GarminFileTransferStatus.SYNC_PAUSED to GarminUploadRefusal.SYNC_PAUSED,
        )
        expected.forEach { (verdict, refusal) ->
            val watch = FakeWatch().apply { verdicts += verdict }

            assertEquals(GarminUploadResult.Refused(refusal), uploaderFor(watch).sendFile())
            assertEquals(1, watch.count(GarminMessageId.FILE_TRANSFER_DATA))
            assertEquals(0, watch.count(GarminMessageId.SYSTEM_EVENT))
        }
    }

    @Test fun `an OK that reports the wrong offset is not trusted`() = runTest {
        val watch = FakeWatch().apply { lieAboutOffset = true }

        assertEquals(
            GarminUploadResult.Refused(GarminUploadRefusal.OFFSET_MISMATCH),
            uploaderFor(watch).sendFile(),
        )
    }

    @Test fun `silence at any step ends at the timeout and names the step`() = runTest {
        val steps = mapOf(
            GarminMessageId.CREATE_FILE to GarminUploadStep.CREATE,
            GarminMessageId.UPLOAD_REQUEST to GarminUploadStep.REQUEST,
            GarminMessageId.FILE_TRANSFER_DATA to GarminUploadStep.DATA,
        )
        steps.forEach { (messageType, step) ->
            val watch = FakeWatch().apply { silentOn += messageType }
            val started = testScheduler.currentTime

            val result = uploaderFor(watch).sendFile()

            assertEquals(GarminUploadResult.NoAnswer(step), result)
            assertEquals(10_000L, testScheduler.currentTime - started)
        }
    }

    @Test fun `a lost link ends the wait at once`() = runTest {
        val watch = FakeWatch().apply { silentOn += GarminMessageId.UPLOAD_REQUEST }
        val uploader = uploaderFor(watch)

        val result = async { uploader.sendFile() }
        runCurrent()
        uploader.abort()

        assertEquals(GarminUploadResult.LinkLost, result.await())
        assertEquals(0L, testScheduler.currentTime)
        assertFalse(uploader.isActive)
    }

    @Test fun `a second upload while one runs is busy, and the first is unharmed`() = runTest {
        val watch = FakeWatch().apply { silentOn += GarminMessageId.CREATE_FILE }
        val uploader = uploaderFor(watch)

        val first = async { uploader.sendFile() }
        runCurrent()
        assertTrue(uploader.isActive)
        assertEquals(GarminUploadResult.Busy, uploader.sendFile())

        // The first still waits; answer it and it carries on.
        watch.silentOn.clear()
        uploader.onCreateFileStatus(GarminCreateFileStatus(GarminStatus.ACK, GarminCreateFileResult.OK, 7))
        assertEquals(GarminUploadResult.Sent, first.await())
    }

    @Test fun `replies nobody waits for are dropped`() = runTest {
        val watch = FakeWatch()
        val uploader = uploaderFor(watch)

        // The phone's own download acks decode to this shape.
        uploader.onDataStatus(GarminFileTransferDataStatus(GarminStatus.ACK, GarminFileTransferStatus.OK, 1024))
        uploader.onCreateFileStatus(GarminCreateFileStatus(GarminStatus.ACK, GarminCreateFileResult.OK, 1))

        assertEquals(GarminUploadResult.Sent, uploader.sendFile())
    }

    @Test fun `a reply to the wrong step does not move the upload on`() = runTest {
        val watch = FakeWatch().apply { silentOn += GarminMessageId.CREATE_FILE }
        val uploader = uploaderFor(watch)

        val result = async { uploader.sendFile() }
        runCurrent()
        uploader.onDataStatus(GarminFileTransferDataStatus(GarminStatus.ACK, GarminFileTransferStatus.OK, 8))
        advanceTimeBy(11.seconds)

        assertEquals(GarminUploadResult.NoAnswer(GarminUploadStep.CREATE), result.await())
    }
}
