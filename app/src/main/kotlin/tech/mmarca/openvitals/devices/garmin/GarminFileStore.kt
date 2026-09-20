package tech.mmarca.openvitals.devices.garmin

import java.io.File
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Keeps the raw FIT files pulled off a watch. Archiving is destructive:
 * the watch never offers an archived file again, so bytes land on disk
 * before the archive flag is sent. Also lets a parser bug be reproduced.
 */
class GarminFileStore(
    /** Resolves the directory on first use. A callback, so a test can point it at a temp dir. */
    private val resolveDirectory: suspend () -> File,
    /** How long a file is kept before [prune] removes it. */
    private val retention: Duration = 30.days,
) {

    /** The note beside each saved file that is not imported yet, by the file object it belongs to. */
    private val pendingNotes = java.util.Collections.synchronizedMap(
        java.util.IdentityHashMap<GarminDownloadedFile, File>(),
    )

    /**
     * Writes [file] and returns its path. Throws, so the caller can refuse to archive.
     *
     * Also leaves a note that the file awaits import for [deviceId]. The watch archives a
     * file once it is saved and never offers it again, and the import runs only after the
     * whole pull. A crash or an exception in between used to lose the file: nothing read
     * the store back. [pending] does, and [markImported] removes the note.
     */
    suspend fun save(file: GarminDownloadedFile, now: Instant, deviceId: String): String {
        val directory = resolveDirectory()
        directory.mkdirs()
        // The timestamp keeps a re-download from clobbering an earlier copy: several files share number 65535.
        val name = "${file.entry.type.label}_${file.entry.fileIndex}_" +
            "${now.toEpochMilli()}.fit"
        val target = File(directory, name)
        target.writeBytes(file.bytes)
        val note = File(directory, name + PENDING_SUFFIX)
        note.writeText(file.entry.toPendingNote(deviceId))
        pendingNotes[file] = note
        GarminLog.log("[GARMIN-STORE] saved $name (${file.bytes.size}B)")
        return target.path
    }

    /** Files saved for [deviceId] that no import has finished with, oldest first. */
    suspend fun pending(deviceId: String): List<GarminDownloadedFile> {
        val directory = resolveDirectory()
        val notes = directory.listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(PENDING_SUFFIX) }
            .sortedBy { it.lastModified() }
        return notes.mapNotNull { note ->
            val fields = runCatching { note.readLines() }.getOrNull()
                ?.mapNotNull { line -> line.split('=', limit = 2).takeIf { it.size == 2 } }
                ?.associate { it[0] to it[1] }
            if (fields == null || fields["device"] != deviceId) return@mapNotNull null
            val data = File(directory, note.name.removeSuffix(PENDING_SUFFIX))
            val entry = fields.toDirectoryEntry()
            if (entry == null || !data.isFile) {
                // Nothing to import from: the bytes are gone or the note is unreadable.
                note.delete()
                return@mapNotNull null
            }
            GarminDownloadedFile(entry, data.readBytes()).also { pendingNotes[it] = note }
        }
    }

    /** The import is done with [files], whatever it made of them. Their notes go. */
    fun markImported(files: List<GarminDownloadedFile>) {
        files.forEach { file -> pendingNotes.remove(file)?.delete() }
    }

    /** Deletes files older than [retention]. Best-effort. */
    suspend fun prune(now: Instant) {
        val cutoff = now.minusMillis(retention.inWholeMilliseconds)
        try {
            val directory = resolveDirectory()
            if (!directory.exists()) return
            for (entity in directory.listFiles().orEmpty()) {
                if (!entity.isFile) continue
                if (!entity.name.endsWith(".fit") && !entity.name.endsWith(PENDING_SUFFIX)) continue
                if (Instant.ofEpochMilli(entity.lastModified()).isBefore(cutoff)) {
                    entity.delete()
                    GarminLog.log("[GARMIN-STORE] pruned ${entity.path}")
                }
            }
        } catch (error: Exception) {
            GarminLog.log("[GARMIN-STORE] prune failed: $error")
        }
    }

    private companion object {
        const val PENDING_SUFFIX = ".pending"
    }
}

private fun GarminDirectoryEntry.toPendingNote(deviceId: String): String = listOf(
    "device" to deviceId,
    "dataType" to type.dataType,
    "subType" to type.subType,
    "fileIndex" to fileIndex,
    "fileNumber" to fileNumber,
    "specificFlags" to specificFlags,
    "fileFlags" to fileFlags,
    "fileSize" to fileSize,
    "fileDate" to (fileDate?.epochSecond ?: ""),
    "remoteDedupKey" to (remoteDedupKey ?: ""),
).joinToString("\n") { (key, value) -> "$key=$value" }

private fun Map<String, String>.toDirectoryEntry(): GarminDirectoryEntry? {
    val type = GarminFileType.fromCodes(
        get("dataType")?.toIntOrNull() ?: return null,
        get("subType")?.toIntOrNull() ?: return null,
    ) ?: return null
    return GarminDirectoryEntry(
        fileIndex = get("fileIndex")?.toIntOrNull() ?: return null,
        type = type,
        fileNumber = get("fileNumber")?.toIntOrNull() ?: return null,
        specificFlags = get("specificFlags")?.toIntOrNull() ?: 0,
        fileFlags = get("fileFlags")?.toIntOrNull() ?: 0,
        fileSize = get("fileSize")?.toLongOrNull() ?: return null,
        fileDate = get("fileDate")?.toLongOrNull()?.let(Instant::ofEpochSecond),
        remoteDedupKey = get("remoteDedupKey")?.takeIf { it.isNotEmpty() },
    )
}
