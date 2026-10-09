package tech.mmarca.openvitals.wear

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** Persists the chosen dashboard tiles and their order on the watch. */
class WearTilePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadTiles(): List<WearMetric> = decodeTiles(prefs.getString(KEY_TILES, null))

    fun saveTiles(tiles: List<WearMetric>) {
        prefs.edit { putString(KEY_TILES, encodeTiles(tiles)) }
    }

    fun resetTiles(): List<WearMetric> {
        prefs.edit { remove(KEY_TILES) }
        return WearMetric.DefaultTiles
    }

    private companion object {
        const val PREFS_NAME = "wear_tile_prefs"
        const val KEY_TILES = "active_tiles"
    }
}

internal fun encodeTiles(tiles: List<WearMetric>): String = tiles.joinToString(",") { it.name }

/**
 * Reads a stored tile list. Nothing stored means the defaults; an empty string
 * means the user switched every tile off. Unknown names are dropped.
 */
internal fun decodeTiles(raw: String?): List<WearMetric> {
    if (raw == null) return WearMetric.DefaultTiles
    return raw.split(",")
        .mapNotNull { name -> WearMetric.entries.firstOrNull { it.name == name.trim() } }
        .distinct()
}

/** Moves [moved] to the position of [target], shifting the tiles in between. */
internal fun List<WearMetric>.withTileMoved(moved: WearMetric, target: WearMetric): List<WearMetric> {
    val targetIndex = indexOf(target)
    if (moved == target || moved !in this || targetIndex < 0) return this
    return toMutableList().apply {
        remove(moved)
        add(targetIndex, moved)
    }
}
