package tech.mmarca.openvitals.core.export

import java.io.File
import java.io.OutputStream

/** Staged exports older than this are swept on the next staging call. */
private const val ExportRetentionMillis = 24 * 60 * 60 * 1000L

/**
 * Stages one export under this feature cache directory: creates it, prunes the
 * copies older than a day, then writes [fileName] through [write] — overwriting
 * any earlier copy of the same name rather than stacking up.
 *
 * The receiver is a per-feature directory under `Context.cacheDir` (e.g.
 * `route_exports/`, `report_exports/`) — the directory name is the only thing
 * keeping features' equally-named files apart, and each must have a matching
 * `<cache-path>` entry in `res/xml/file_paths.xml` before a FileProvider URI
 * can hand it to another app.
 */
fun File.stageExport(fileName: String, write: (OutputStream) -> Unit): File {
    mkdirs()
    deleteOldExports()
    val exportFile = File(this, fileName)
    exportFile.outputStream().use(write)
    return exportFile
}

/** Best-effort: the user asked for an export, not for cache hygiene. */
private fun File.deleteOldExports() {
    val cutoffMillis = System.currentTimeMillis() - ExportRetentionMillis
    listFiles()
        ?.filter { file -> file.isFile && file.lastModified() < cutoffMillis }
        ?.forEach { file -> runCatching { file.delete() } }
}
