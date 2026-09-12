package tech.mmarca.openvitals.features.activity.elevation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.geo.HgtTileKey
import tech.mmarca.openvitals.core.performance.DispatcherProvider

/** The user's imported SRTM tiles. Nothing is downloaded: the app has no network access. */
@Singleton
class ElevationTileRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) {
    private val library = HgtTileLibrary(File(context.filesDir, TilesDirectoryName))
    private val _state = MutableStateFlow(ElevationTileLibraryState(library.scan()))
    val state: StateFlow<ElevationTileLibraryState> = _state.asStateFlow()

    /** Read on every import, so it must not touch the disk. */
    val hasTiles: Boolean get() = state.value.tiles.isNotEmpty()

    fun refresh() {
        _state.value = ElevationTileLibraryState(library.scan())
    }

    suspend fun importTile(uri: Uri): ElevationTile = withContext(dispatchers.io) {
        val displayName = queryDisplayName(uri)
            ?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment
            ?: ""
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Unable to open the selected elevation tile.")
        val tile = input.use { library.importTile(displayName, it) }
        refresh()
        tile
    }

    suspend fun deleteTile(key: HgtTileKey) = withContext(dispatchers.io) {
        library.deleteTile(key)
        refresh()
    }

    /** Bilinear elevation in meters, or null when no imported tile covers the position. */
    fun elevationAt(latitude: Double, longitude: Double): Double? =
        library.elevationAt(latitude, longitude)

    private fun queryDisplayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 } ?: return@use null
                cursor.getString(index)
            }

    private companion object {
        const val TilesDirectoryName = "elevation_tiles"
    }
}
