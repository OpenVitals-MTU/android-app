package tech.mmarca.openvitals.devices.garmin

import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * Garmin's protobuf `FileSyncService`: the listing, download and mark-synced
 * conversation newer firmware uses instead of the legacy 16-byte directory.
 */
object GarminFileSyncProtocol {
    /** The "already synced" flag id, sent as both fixed64 halves of a file id. */
    private const val ALREADY_SYNCED = 42405L

    /** `FileRequest.unk2` and `unk5`: fixed values the watch accepts; their meaning is unknown. */
    private const val FILE_REQUEST_UNK2 = 24L
    private const val FILE_REQUEST_UNK5 = 15L

    data class FileId(val first: Long, val second: Long)

    data class RemoteFile(
        val id: FileId,
        val typeName: String,
        val typeCode: Int,
        val size: Long,
        val pageId: Long? = null,
    ) {
        val type: GarminFileType? get() = GarminFileType.fromSyncName(typeName)
        val dedupKey: String get() = "sync/${id.first}/${id.second}/$size"
    }

    data class FileListPage(
        val files: List<RemoteFile>,
        val cursorId: Long?,
        val nextPageId: Long?,
    )

    data class FileResponse(val status: Int, val handle: Int?)

    fun listRequest(cursorId: Long? = null, startPageId: Long? = null): ByteArray {
        val flags = fileId(FileId(ALREADY_SYNCED, ALREADY_SYNCED))
        val request = ProtobufWriter().apply {
            cursorId?.let { varint(1, it) }
            if (cursorId == null) startPageId?.let { varint(2, it) }
            nested(4, flags)
            nested(5, flags)
        }.toBytes()
        return smart(ProtobufWriter().nested(9, request).toBytes())
    }

    fun fileRequest(file: RemoteFile): ByteArray {
        val type = ProtobufWriter()
            .string(2, file.typeName)
            .varint(3, file.typeCode)
            .toBytes()
        val remote = ProtobufWriter()
            .nested(1, fileId(file.id))
            .nested(2, type)
            .varint(3, file.size)
            .apply { file.pageId?.let { varint(5, it) } }
            .toBytes()
        val request = ProtobufWriter()
            .nested(1, remote)
            .varint(2, FILE_REQUEST_UNK2)
            .varint(3, 0)
            .varint(4, 0)
            .varint(5, FILE_REQUEST_UNK5)
            .toBytes()
        return smart(ProtobufWriter().nested(1, request).toBytes())
    }

    fun markSynced(file: RemoteFile): ByteArray {
        val flags = fileId(FileId(ALREADY_SYNCED, ALREADY_SYNCED))
        val setFlags = ProtobufWriter()
            .nested(1, fileId(file.id))
            .nested(2, flags)
            .toBytes()
        return smart(ProtobufWriter().nested(15, setFlags).toBytes())
    }

    fun parseListResponse(payload: ByteArray): FileListPage? {
        val service = service(payload) ?: return null
        val response = protobufField(readProtobuf(service), 10)?.bytes ?: return null
        val fields = readProtobuf(response)
        val namesByCode = mutableMapOf<Int, String>()
        val files = mutableListOf<RemoteFile>()
        for (field in fields.filter { it.field == 4 }) {
            val fileBytes = field.bytes ?: continue
            val fileFields = readProtobuf(fileBytes)
            val id = parseId(protobufField(fileFields, 1)?.bytes) ?: continue
            val typeFields = protobufField(fileFields, 2)?.bytes?.let(::readProtobuf) ?: continue
            val code = protobufField(typeFields, 3)?.varint?.toInt() ?: continue
            protobufField(typeFields, 2)?.bytes?.toString(Charsets.ISO_8859_1)?.let {
                namesByCode[code] = it
            }
            val name = namesByCode[code] ?: continue
            files += RemoteFile(
                id = id,
                typeName = name,
                typeCode = code,
                size = protobufField(fileFields, 3)?.varint ?: 0,
                pageId = protobufField(fileFields, 5)?.varint,
            )
        }
        return FileListPage(
            files = files,
            cursorId = protobufField(fields, 2)?.varint,
            nextPageId = protobufField(fields, 3)?.varint,
        )
    }

    fun parseFileResponse(payload: ByteArray): FileResponse? {
        val service = service(payload) ?: return null
        val response = protobufField(readProtobuf(service), 2)?.bytes ?: return null
        val fields = readProtobuf(response)
        return FileResponse(
            status = protobufField(fields, 1)?.varint?.toInt() ?: -1,
            handle = protobufField(fields, 3)?.varint?.toInt(),
        )
    }

    fun isFileSyncMessage(payload: ByteArray): Boolean = service(payload) != null

    /**
     * True for the watch's own "new files" or "start sync" notice. Newer
     * firmware announces this way instead of the legacy SYNCHRONIZATION.
     */
    fun isSyncAnnouncement(payload: ByteArray): Boolean {
        val fields = service(payload)?.let(::readProtobuf) ?: return false
        return protobufField(fields, NEW_FILE_NOTIFICATION) != null ||
            protobufField(fields, START_SYNC_NOTIFICATION) != null
    }

    private const val NEW_FILE_NOTIFICATION = 12
    private const val START_SYNC_NOTIFICATION = 22

    fun inflateFilePayload(bytes: ByteArray): ByteArray? {
        val inflater = Inflater()
        return try {
            inflater.setInput(bytes)
            val output = ByteArrayOutputStream(bytes.size.coerceAtLeast(1024))
            val buffer = ByteArray(8192)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0) {
                    return null
                }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } catch (_: DataFormatException) {
            null
        } finally {
            inflater.end()
        }
    }

    /** Wraps a FileSyncService message in its Smart envelope. Internal for tests. */
    internal fun smart(fileSyncService: ByteArray): ByteArray =
        ProtobufWriter().nested(GarminSmartService.FILE_SYNC, fileSyncService).toBytes()

    private fun service(payload: ByteArray): ByteArray? =
        protobufField(readProtobuf(payload), GarminSmartService.FILE_SYNC)?.bytes

    private fun fileId(id: FileId): ByteArray = ProtobufWriter()
        .fixed64(1, id.first)
        .fixed64(2, id.second)
        .toBytes()

    private fun parseId(bytes: ByteArray?): FileId? {
        val fields = bytes?.let(::readProtobuf) ?: return null
        val first = protobufField(fields, 1)?.fixed64 ?: return null
        val second = protobufField(fields, 2)?.fixed64 ?: return null
        return FileId(first, second)
    }
}
