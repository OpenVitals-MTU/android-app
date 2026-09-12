package tech.mmarca.openvitals.features.activity.elevation

import java.io.File
import tech.mmarca.openvitals.core.geo.HgtResolution
import tech.mmarca.openvitals.core.geo.HgtTileKey

/** One imported SRTM tile. The file name is the key and the size is the resolution. */
data class ElevationTile(
    val key: HgtTileKey,
    val resolution: HgtResolution,
    val sizeBytes: Long,
    val importedAtMillis: Long,
    val path: String,
) {
    val file: File get() = File(path)

    /** `N45E007`: the cell, without the extension. */
    val displayName: String get() = key.fileName.removeSuffix(HgtTileKey.FileExtension)
}

data class ElevationTileLibraryState(
    val tiles: List<ElevationTile> = emptyList(),
)
