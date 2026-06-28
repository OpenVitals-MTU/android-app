package tech.mmarca.openvitals.features.activity.maps

import androidx.annotation.StringRes
import java.io.File
import tech.mmarca.openvitals.R

data class OfflineMapPack(
    val id: String,
    val displayName: String,
    val originalFileName: String,
    val sizeBytes: Long,
    val importedAtMillis: Long,
    val path: String,
    val format: OfflineMapPackFormat = OfflineMapPackFormat.PMTILES,
) {
    val file: File get() = File(path)
}

enum class OfflineMapPackFormat(
    val fileExtension: String,
    private val acceptedFileExtensions: Set<String> = setOf(fileExtension),
) {
    PMTILES(".pmtiles"),
    MAPSFORGE(".map", setOf(".map", ".maps")),
    ;

    companion object {
        fun fromFileName(fileName: String): OfflineMapPackFormat? =
            entries.firstOrNull { format ->
                format.extensionForFileName(fileName) != null
            }
    }

    fun extensionForFileName(fileName: String): String? =
        acceptedFileExtensions.firstOrNull { extension ->
            fileName.endsWith(extension, ignoreCase = true)
        }
}

data class OfflineMapLibraryState(
    val mapPacks: List<OfflineMapPack> = emptyList(),
    val activeFormat: OfflineMapPackFormat? = null,
) {
    val activeMapPacks: List<OfflineMapPack>
        get() = activeFormat
            ?.let { format -> mapPacks.filter { it.format == format } }
            .orEmpty()
}

enum class OfflineMapImportPhase {
    QUEUED,
    COPYING,
    COMPLETE,
}

data class OfflineMapImportProgress(
    val phase: OfflineMapImportPhase = OfflineMapImportPhase.QUEUED,
    val bytesCopied: Long = 0L,
    val totalBytes: Long = 0L,
) {
    val percent: Int?
        get() = totalBytes
            .takeIf { it > 0L }
            ?.let { total -> ((bytesCopied.coerceAtMost(total) * 100L) / total).toInt() }
}

data class OfflineMapImportResult(
    val mapId: String,
    val displayName: String,
    val sizeBytes: Long,
    val format: OfflineMapPackFormat = OfflineMapPackFormat.PMTILES,
)

internal val OfflineMapImportPhase.labelRes: Int
    @StringRes
    get() = when (this) {
        OfflineMapImportPhase.QUEUED -> R.string.settings_offline_maps_import_progress_queued
        OfflineMapImportPhase.COPYING -> R.string.settings_offline_maps_import_progress_copying
        OfflineMapImportPhase.COMPLETE -> R.string.settings_offline_maps_import_progress_complete
    }
