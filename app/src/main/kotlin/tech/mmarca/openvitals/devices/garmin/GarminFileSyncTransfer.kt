package tech.mmarca.openvitals.devices.garmin

import java.io.ByteArrayOutputStream
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/** The FileSyncService listing failed or timed out. Support stays unknown, not refused. */
class GarminFileSyncException(message: String) : Exception(message)

/**
 * One pull over the protobuf FileSyncService: list every page, download each
 * wanted file over a temporary reliable ML channel, persist it, then tell the
 * watch it is synced. Protocol only: the owner decides when to run it and
 * what the files become.
 */
class GarminFileSyncTransfer(
    private val protobuf: GarminProtobufTransport,
    private val transport: GarminMlTransport,
    /** Persists a file. Runs before the watch is told the file is synced. */
    private val keep: suspend (GarminDownloadedFile) -> Unit,
    /** The watch answered a listing, so it speaks FileSync. */
    private val onProtocolProven: () -> Unit = {},
    private val onProgress: ((filesTotal: Int, filesDone: Int) -> Unit)? = null,
    /** Dates a downloaded file from its content. Swappable for tests. */
    private val fileDate: (ByteArray) -> Instant? = ::garminFitFileDate,
) {

    /**
     * Every wanted file not in [alreadySynced], persisted. Throws
     * [GarminFileSyncException] when the listing fails.
     */
    suspend fun pull(alreadySynced: Set<String>): List<GarminDownloadedFile> {
        val wanted = listAll().filter { file ->
            file.type?.wanted == true && file.dedupKey !in alreadySynced
        }
        val downloaded = mutableListOf<GarminDownloadedFile>()
        for ((index, remote) in wanted.withIndex()) {
            onProgress?.invoke(wanted.size, index)
            try {
                downloaded += download(remote) ?: continue
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // One broken transfer ends the pull; what landed is kept.
                GarminLog.log("[GARMIN-SYNC] file transfer interrupted: $error")
                break
            }
        }
        return downloaded
    }

    /** Follows cursor pages, then next-page ids, until the watch stops offering more. */
    private suspend fun listAll(): List<GarminFileSyncProtocol.RemoteFile> {
        val files = mutableListOf<GarminFileSyncProtocol.RemoteFile>()
        var cursorId: Long? = null
        var startPageId: Long? = null
        val seenPages = mutableSetOf<Pair<Long?, Long?>>()
        var proven = false
        repeat(MAX_PAGES) {
            val pageToken = cursorId to startPageId
            if (!seenPages.add(pageToken)) {
                GarminLog.log("[GARMIN-SYNC] file list repeated page token $pageToken")
                return files
            }
            val reply = protobuf.request(
                GarminFileSyncProtocol.listRequest(cursorId = cursorId, startPageId = startPageId),
                label = "file-sync list",
                timeout = REPLY_TIMEOUT,
                acceptUnmatched = { GarminFileSyncProtocol.parseListResponse(it) != null },
            ) ?: throw GarminFileSyncException("file-list response timed out")
            val page = GarminFileSyncProtocol.parseListResponse(reply)
                ?: throw GarminFileSyncException("file-list response was invalid")
            if (!proven) {
                proven = true
                onProtocolProven()
            }
            files += page.files
            if (page.cursorId != null) {
                cursorId = page.cursorId
                startPageId = null
            } else {
                cursorId = null
                startPageId = page.nextPageId
            }
            if (cursorId == null && startPageId == null) return files
        }
        return files
    }

    /** Asks for [remote], streams, persists and marks it synced. Null when the watch declined. */
    private suspend fun download(remote: GarminFileSyncProtocol.RemoteFile): GarminDownloadedFile? {
        val reply = protobuf.request(
            GarminFileSyncProtocol.fileRequest(remote),
            label = "file-sync file ${remote.typeName}",
            timeout = REPLY_TIMEOUT,
            acceptUnmatched = { GarminFileSyncProtocol.parseFileResponse(it) != null },
        ) ?: return null
        val response = GarminFileSyncProtocol.parseFileResponse(reply) ?: return null
        val handle = response.handle
        if (response.status != 0 || handle == null) return null
        val type = remote.type ?: return null
        val bytes = receive(handle) ?: return null
        val file = GarminDownloadedFile(
            entry = GarminDirectoryEntry(
                fileIndex = handle,
                type = type,
                fileNumber = GarminDirectoryEntry.UNSET_FILE_NUMBER,
                specificFlags = 0,
                fileFlags = 0,
                fileSize = bytes.size.toLong(),
                fileDate = runCatching { fileDate(bytes) }.getOrNull(),
                remoteDedupKey = remote.dedupKey,
            ),
            bytes = bytes,
        )
        // Persist first, mark second: marking is irreversible.
        keep(file)
        protobuf.sendUnanswered(
            GarminFileSyncProtocol.markSynced(remote),
            label = "mark file synced",
        )
        return file
    }

    /**
     * Streams file [handle] over a transfer service. The watch sends three
     * zero bytes, then the zlib payload, then closes the service. Null when
     * no service is free, the header is wrong, or the watch never closes.
     */
    private suspend fun receive(handle: Int): ByteArray? {
        val serviceCode = transport.freeService(TRANSFER_SERVICES) ?: return null
        val compressed = ByteArrayOutputStream()
        val header = ByteArrayOutputStream(HEADER_SIZE)
        val closed = CompletableDeferred<Unit>()
        var valid = true
        return transport.useServiceChannel(
            serviceCode = serviceCode,
            reliable = true,
            onData = { payload ->
                var body = payload
                if (header.size() < HEADER_SIZE) {
                    val take = (HEADER_SIZE - header.size()).coerceAtMost(payload.size)
                    header.write(payload, 0, take)
                    if (header.size() == HEADER_SIZE) {
                        valid = header.toByteArray().all { it == 0.toByte() }
                    }
                    body = payload.copyOfRange(take, payload.size)
                }
                if (valid) compressed.write(body)
            },
            onClosed = { closed.complete(Unit) },
        ) { channel ->
            channel.send(transferRequest(handle))
            val finished = withTimeoutOrNull(TRANSFER_TIMEOUT) { closed.await() } != null
            if (finished && valid) {
                GarminFileSyncProtocol.inflateFilePayload(compressed.toByteArray())
            } else {
                null
            }
        }
    }

    /** The request shape: `[0][0][u16 handle][0][0]`. */
    private fun transferRequest(handle: Int): ByteArray = GarminByteWriter(6)
        .writeByte(0)
        .writeByte(0)
        .writeShort(handle)
        .writeByte(0)
        .writeByte(0)
        .toBytes()

    private companion object {
        /**
         * The ML service codes Garmin reserves for one-off transfers. 0x8018 is
         * missing: its high bit is the MLR flag.
         */
        val TRANSFER_SERVICES = intArrayOf(0x2018, 0x4018, 0x6018, 0xA018, 0xC018, 0xE018)

        /** The most pages one listing may take: a guard against a watch that never ends. */
        const val MAX_PAGES = 20

        /** The watch prefixes every file with this many zero bytes. */
        const val HEADER_SIZE = 3

        /** Shorter than the transport's default: a probe on a legacy watch costs this much. */
        val REPLY_TIMEOUT = 8.seconds

        /** One compressed file; the watch closes the service when it is done. */
        val TRANSFER_TIMEOUT = 90.seconds
    }
}
