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

    /** Writes [file] and returns its path. Throws, so the caller can refuse to archive. */
    suspend fun save(file: GarminDownloadedFile, now: Instant): String {
        val directory = resolveDirectory()
        directory.mkdirs()
        // The timestamp keeps a re-download from clobbering an earlier copy: several files share number 65535.
        val name = "${file.entry.type.label}_${file.entry.fileIndex}_" +
            "${now.toEpochMilli()}.fit"
        val target = File(directory, name)
        target.writeBytes(file.bytes)
        GarminLog.log("[GARMIN-STORE] saved $name (${file.bytes.size}B)")
        return target.path
    }

    /** Deletes files older than [retention]. Best-effort. */
    suspend fun prune(now: Instant) {
        val cutoff = now.minusMillis(retention.inWholeMilliseconds)
        try {
            val directory = resolveDirectory()
            if (!directory.exists()) return
            for (entity in directory.listFiles().orEmpty()) {
                if (!entity.isFile || !entity.name.endsWith(".fit")) continue
                if (Instant.ofEpochMilli(entity.lastModified()).isBefore(cutoff)) {
                    entity.delete()
                    GarminLog.log("[GARMIN-STORE] pruned ${entity.path}")
                }
            }
        } catch (error: Exception) {
            GarminLog.log("[GARMIN-STORE] prune failed: $error")
        }
    }
}
