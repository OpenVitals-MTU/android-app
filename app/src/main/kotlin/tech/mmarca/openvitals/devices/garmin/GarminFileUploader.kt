package tech.mmarca.openvitals.devices.garmin

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/** Why the watch would not take a file. The wording is the screen's. */
enum class GarminUploadRefusal {
    DUPLICATE,
    NO_SPACE,
    UNSUPPORTED,
    NO_SLOTS,
    NOT_WRITEABLE,
    NOT_READY,
    TRANSFER_ABORTED,
    CRC_MISMATCH,
    OFFSET_MISMATCH,
    SYNC_PAUSED,
    RESEND_LIMIT,
    INVALID_REPLY,
}

/** The three conversations of an upload, for saying where one went quiet. */
enum class GarminUploadStep { CREATE, REQUEST, DATA }

sealed interface GarminUploadResult {
    data object Sent : GarminUploadResult
    data class Refused(val reason: GarminUploadRefusal) : GarminUploadResult
    data class NoAnswer(val step: GarminUploadStep) : GarminUploadResult
    data object LinkLost : GarminUploadResult

    /** Another upload is running on this session. */
    data object Busy : GarminUploadResult
}

/**
 * Writes a file to the watch: create a slot, announce the upload, stream the
 * chunks, each waiting for the watch's verdict. The flow is upstream's
 * `FileTransferHandler.Upload` (AGPLv3). Transport-free: the session feeds the
 * replies in, as it does for [GarminProtobufTransport].
 */
class GarminFileUploader(
    private val send: suspend (ByteArray) -> Unit,
    private val replyTimeout: Duration = REPLY_TIMEOUT,
    private val maxChunkSize: Int = MAX_CHUNK_SIZE,
    private val newFileId: () -> Long = { Random.nextLong() },
) {

    companion object {
        /** As the notification upload: the protocol's ceiling. The transport fragments frames. */
        const val MAX_CHUNK_SIZE = 300

        /** A data frame's own bytes, which the watch counts against its packet size. */
        const val CHUNK_OVERHEAD = 13

        /** Resends of one chunk before giving up. One covers a dropped write. */
        const val MAX_RESENDS = 1

        val REPLY_TIMEOUT: Duration = 10.seconds
    }

    private class Pending(
        val step: GarminUploadStep,
        /** Null means the link went away. */
        val reply: CompletableDeferred<GarminInboundMessage?> = CompletableDeferred(),
    )

    private val running = AtomicBoolean(false)

    @Volatile
    private var pending: Pending? = null

    val isActive: Boolean get() = running.get()

    /**
     * Uploads [bytes] as a new [type] file. [maxPacketSize] is the watch's
     * own limit from its device information, when known.
     */
    suspend fun upload(
        type: GarminUploadFileType,
        bytes: ByteArray,
        maxPacketSize: Int?,
    ): GarminUploadResult {
        if (!running.compareAndSet(false, true)) return GarminUploadResult.Busy
        return try {
            transfer(type, bytes, chunkSize(maxPacketSize)).also {
                GarminLog.log("[GARMIN-SEND] ${type.name.lowercase()} upload: $it")
            }
        } finally {
            pending = null
            running.set(false)
        }
    }

    fun onCreateFileStatus(message: GarminCreateFileStatus) = deliver(GarminUploadStep.CREATE, message)

    fun onUploadRequestStatus(message: GarminUploadRequestStatus) = deliver(GarminUploadStep.REQUEST, message)

    fun onDataStatus(message: GarminFileTransferDataStatus) = deliver(GarminUploadStep.DATA, message)

    /** The link is gone: the waiting step ends now, not at its timeout. */
    fun abort() {
        pending?.reply?.complete(null)
    }

    private suspend fun transfer(type: GarminUploadFileType, bytes: ByteArray, chunkSize: Int): GarminUploadResult {
        GarminLog.log("[GARMIN-SEND] → create ${type.name.lowercase()} (${bytes.size}B, chunks of $chunkSize)")
        val created = exchange<GarminCreateFileStatus>(
            GarminUploadStep.CREATE,
            buildCreateFile(bytes.size, type, newFileId()),
        ).orReturn { return it }
        GarminLog.log("[GARMIN-SEND] ← create ${created.status}/${created.result} index=${created.fileIndex}")
        if (!created.canProceed) return GarminUploadResult.Refused(created.refusal())

        val accepted = exchange<GarminUploadRequestStatus>(
            GarminUploadStep.REQUEST,
            buildUploadRequest(created.fileIndex, bytes.size),
        ).orReturn { return it }
        GarminLog.log(
            "[GARMIN-SEND] ← upload ${accepted.status}/${accepted.uploadStatus} " +
                "offset=${accepted.dataOffset} room=${accepted.maxFileSize}",
        )
        if (!accepted.canProceed) return GarminUploadResult.Refused(accepted.refusal())
        // A resume is unverified, so only a fresh upload goes ahead.
        if (accepted.dataOffset != 0L) return GarminUploadResult.Refused(GarminUploadRefusal.OFFSET_MISMATCH)
        // Upstream ignores the room. Zero is taken as "not stated".
        if (accepted.maxFileSize in 1 until bytes.size) return GarminUploadResult.Refused(GarminUploadRefusal.NO_SPACE)

        var offset = 0
        var runningCrc = 0
        while (offset < bytes.size) {
            val chunk = bytes.copyOfRange(offset, minOf(offset + chunkSize, bytes.size))
            val chunkCrc = GarminCrc.compute(chunk, initialCrc = runningCrc)
            sendChunk(chunk, offset, chunkCrc)?.let { return it }
            offset += chunk.size
            runningCrc = chunkCrc
        }
        // A session that does not sync files never says this itself.
        send(buildSystemEvent(GarminSystemEventType.SYNC_COMPLETE))
        return GarminUploadResult.Sent
    }

    /** Null when the watch took the chunk. */
    private suspend fun sendChunk(chunk: ByteArray, offset: Int, runningCrc: Int): GarminUploadResult? {
        var resends = 0
        while (true) {
            val verdict = exchange<GarminFileTransferDataStatus>(
                GarminUploadStep.DATA,
                buildFileTransferData(chunk, offset, runningCrc),
            ).orReturn { return it }
            GarminLog.log(
                "[GARMIN-SEND] ← data ${verdict.transferStatus} at $offset+${chunk.size}, watch has ${verdict.nextOffset}",
            )
            when (verdict.transferStatus) {
                GarminFileTransferStatus.OK ->
                    return if (verdict.nextOffset == (offset + chunk.size).toLong()) {
                        null
                    } else {
                        GarminUploadResult.Refused(GarminUploadRefusal.OFFSET_MISMATCH)
                    }
                GarminFileTransferStatus.RESEND -> {
                    if (resends >= MAX_RESENDS) return GarminUploadResult.Refused(GarminUploadRefusal.RESEND_LIMIT)
                    resends++
                }
                GarminFileTransferStatus.ABORT ->
                    return GarminUploadResult.Refused(GarminUploadRefusal.TRANSFER_ABORTED)
                GarminFileTransferStatus.CRC_MISMATCH ->
                    return GarminUploadResult.Refused(GarminUploadRefusal.CRC_MISMATCH)
                GarminFileTransferStatus.OFFSET_MISMATCH ->
                    return GarminUploadResult.Refused(GarminUploadRefusal.OFFSET_MISMATCH)
                GarminFileTransferStatus.SYNC_PAUSED ->
                    return GarminUploadResult.Refused(GarminUploadRefusal.SYNC_PAUSED)
            }
        }
    }

    private class Answer(val message: GarminInboundMessage?)

    private sealed interface Reply<out T> {
        class Got<T>(val message: T) : Reply<T>
        class Failed(val result: GarminUploadResult) : Reply<Nothing>
    }

    private inline fun <T> Reply<T>.orReturn(onFailure: (GarminUploadResult) -> Nothing): T =
        when (this) {
            is Reply.Got -> message
            is Reply.Failed -> onFailure(result)
        }

    /** Sends [frame] and waits for the reply to [step]. The slot is set first: a reply can beat the send's return. */
    private suspend inline fun <reified T : GarminInboundMessage> exchange(
        step: GarminUploadStep,
        frame: ByteArray,
    ): Reply<T> {
        val waiting = Pending(step)
        pending = waiting
        send(frame)
        // Wrapped, so a timeout (null) differs from a lost link (a null inside).
        val answered = withTimeoutOrNull(replyTimeout) { Answer(waiting.reply.await()) }
        pending = null
        return when {
            answered == null -> Reply.Failed(GarminUploadResult.NoAnswer(step))
            answered.message == null -> Reply.Failed(GarminUploadResult.LinkLost)
            else -> Reply.Got(answered.message as T)
        }
    }

    /** A reply nothing waits for is dropped: the phone's own download acks share the data reply's shape. */
    private fun deliver(step: GarminUploadStep, message: GarminInboundMessage) {
        pending?.takeIf { it.step == step }?.reply?.complete(message)
    }

    private fun chunkSize(maxPacketSize: Int?): Int {
        val room = (maxPacketSize ?: 0) - CHUNK_OVERHEAD
        return if (room > 0) minOf(room, maxChunkSize) else maxChunkSize
    }
}

private fun GarminCreateFileStatus.refusal(): GarminUploadRefusal = when (result) {
    GarminCreateFileResult.DUPLICATE -> GarminUploadRefusal.DUPLICATE
    GarminCreateFileResult.NO_SPACE,
    GarminCreateFileResult.NO_SPACE_FOR_TYPE -> GarminUploadRefusal.NO_SPACE
    GarminCreateFileResult.UNSUPPORTED -> GarminUploadRefusal.UNSUPPORTED
    GarminCreateFileResult.NO_SLOTS -> GarminUploadRefusal.NO_SLOTS
    // A watch that does not know the message at all says so in the envelope.
    GarminCreateFileResult.OK,
    GarminCreateFileResult.INVALID ->
        if (status == GarminStatus.UNSUPPORTED) GarminUploadRefusal.UNSUPPORTED else GarminUploadRefusal.INVALID_REPLY
}

private fun GarminUploadRequestStatus.refusal(): GarminUploadRefusal = when (uploadStatus) {
    GarminUploadStatus.INDEX_UNKNOWN,
    GarminUploadStatus.INDEX_NOT_WRITEABLE -> GarminUploadRefusal.NOT_WRITEABLE
    GarminUploadStatus.NO_SPACE_LEFT -> GarminUploadRefusal.NO_SPACE
    GarminUploadStatus.NOT_READY -> GarminUploadRefusal.NOT_READY
    GarminUploadStatus.CRC_INCORRECT -> GarminUploadRefusal.CRC_MISMATCH
    GarminUploadStatus.OK,
    GarminUploadStatus.INVALID -> GarminUploadRefusal.INVALID_REPLY
}
