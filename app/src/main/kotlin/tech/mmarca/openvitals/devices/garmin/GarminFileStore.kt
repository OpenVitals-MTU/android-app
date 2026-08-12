package tech.mmarca.openvitals.devices.garmin

import java.io.File
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Keeps the raw FIT files pulled off a watch.
 *
 * **This exists because archiving is destructive from our side.** Telling the
 * watch a file is archived makes it stop offering that file forever; if the
 * importer then mis-reads it — as it demonstrably can — the data cannot be
 * fetched again to re-import, because it is gone from the watch and was never
 * kept. Exactly that happened to a night of sleep during development.
 *
 * So the contract is: bytes land on disk BEFORE the archive flag is sent, and
 * a failure to write means the file is not archived and the watch offers it
 * again next time. Gadgetbridge keeps the same guarantee by exporting every
 * download.
 *
 * A side benefit is diagnosis: a parser bug can be reproduced against the
 * exact bytes that caused it, with no watch in the loop.
 */
class GarminFileStore(
    /**
     * Resolves where files are written, on first use. A suspend callback
     * rather than a [File] so the caller can defer directory resolution, and
     * so a test can point it at a temp dir.
     */
    private val resolveDirectory: suspend () -> File,
    /**
     * How long a file is kept before [prune] removes it. FIT files are small
     * (a night of sleep is under a kilobyte, a day of monitoring a few) but
     * the watch produces them daily and forever is not a retention policy.
     */
    private val retention: Duration = 30.days,
) {

    /**
     * Writes [file] and returns its path.
     *
     * Throws on failure — deliberately, because the caller's whole reason to
     * await this is to decide whether archiving is safe.
     */
    suspend fun save(file: GarminDownloadedFile, now: Instant): String {
        val directory = resolveDirectory()
        directory.mkdirs()
        // Type and index identify it; the timestamp keeps a re-download from
        // clobbering an earlier copy, since several files share file number
        // 65535.
        val name = "${file.entry.type.label}_${file.entry.fileIndex}_" +
            "${now.toEpochMilli()}.fit"
        val target = File(directory, name)
        target.writeBytes(file.bytes)
        GarminLog.log("[GARMIN-STORE] saved $name (${file.bytes.size}B)")
        return target.path
    }

    /**
     * Deletes files older than [retention]. Best-effort: housekeeping must
     * never fail a sync.
     */
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
