package tech.mmarca.openvitals.features.activity.maps

import android.app.Application
import java.security.MessageDigest
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Dimension
import org.mapsforge.core.util.LatLongUtils
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

/**
 * The pure and process-wide parts of the Mapsforge map, kept out of the
 * composable so they can be reasoned about — and in two cases tested — without
 * a `MapView` behind them.
 */

@Volatile
private var graphicsFactoryInstalled = false
private val graphicsFactoryLock = Any()

/**
 * Installs Mapsforge's graphics factory once per process.
 *
 * `AndroidGraphicFactory.createInstance` writes a public static `INSTANCE`
 * unconditionally: no null check, no synchronization, and the field is not
 * volatile. It is also read from the `MapWorkerPool` render threads. Calling it
 * a second time therefore swaps the factory out from under whatever is drawing.
 *
 * The call used to sit inside the composable's `remember(context, mapPacksKey)`,
 * so importing or deleting a map pack re-ran it — while a live map was using
 * the old instance. Mapsforge's own contract is one call, in
 * `Application.onCreate`; this is that, deferred until the first map is
 * actually built so an install that never opens a map pays nothing.
 */
internal fun ensureMapsforgeGraphicsFactory(application: Application) {
    if (graphicsFactoryInstalled) return
    synchronized(graphicsFactoryLock) {
        if (graphicsFactoryInstalled) return
        AndroidGraphicFactory.createInstance(application)
        graphicsFactoryInstalled = true
    }
}

/**
 * The on-disk tile-cache directory name for a set of packs.
 *
 * Two properties matter, and the previous `"openvitals-" + ids.hashCode()` had
 * neither. It keyed on pack **ids** only, while the composable rebuilds the
 * `MapView` on ids **and paths** — so re-importing a pack under the same id at
 * a new path produced a new `MapView` with the same cache name, and
 * `AndroidUtil.createTileCache` is called with `persistent = false`, meaning
 * the outgoing map's `destroyAll()` deletes the directory the incoming one just
 * opened. And `String.hashCode` is 32-bit and trivially collidable, so two
 * genuinely different pack sets could share a cache and serve each other's
 * tiles.
 *
 * A digest of the same string the `MapView` is keyed on fixes both: it varies
 * with anything the map varies with, and collisions are not a practical
 * concern.
 */
internal fun mapsforgeTileCacheName(mapPacks: List<OfflineMapPack>): String =
    "openvitals-" + sha256Hex(mapsforgeMapPacksKey(mapPacks)).take(TileCacheNameHexLength)

/** The identity of a pack set: what the map is rebuilt on, and cached under. */
internal fun mapsforgeMapPacksKey(mapPacks: List<OfflineMapPack>): String =
    mapPacks.joinToString(separator = "|") { pack -> "${pack.id}:${pack.path}" }

private fun sha256Hex(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

/** The zoom levels the loaded packs actually carry tiles for. */
internal data class MapsforgeZoomRange(val min: Byte, val max: Byte) {
    fun clamp(zoom: Byte): Byte = zoom.coerceIn(min, max)

    companion object {
        /** What a pack that would not report its own range gets. */
        val Default = MapsforgeZoomRange(min = 0, max = 20)
    }
}

/**
 * The smallest box containing every finite point.
 *
 * Null when nothing is plottable — an indoor activity, or a route whose
 * coordinates are all NaN. Callers must not fall back to a default box for
 * that: centring the map on 0,0 is worse than leaving it where the pack's own
 * start position put it.
 */
internal fun routeBoundingBox(
    points: List<ExerciseRoutePoint>,
    currentPoint: ExerciseRoutePoint?,
): BoundingBox? {
    var minLatitude = Double.MAX_VALUE
    var maxLatitude = -Double.MAX_VALUE
    var minLongitude = Double.MAX_VALUE
    var maxLongitude = -Double.MAX_VALUE
    var any = false
    for (point in points + listOfNotNull(currentPoint)) {
        if (!point.latitude.isFinite() || !point.longitude.isFinite()) continue
        any = true
        if (point.latitude < minLatitude) minLatitude = point.latitude
        if (point.latitude > maxLatitude) maxLatitude = point.latitude
        if (point.longitude < minLongitude) minLongitude = point.longitude
        if (point.longitude > maxLongitude) maxLongitude = point.longitude
    }
    if (!any) return null
    return BoundingBox(minLatitude, minLongitude, maxLatitude, maxLongitude)
}

/**
 * The zoom that fits [boundingBox] into a [viewport] of [tileSize] tiles.
 *
 * Delegated to `LatLongUtils.zoomForBounds`, which does the Web Mercator maths
 * the hand-rolled ladder this replaces did not. That one took the larger
 * lat/lon span in DEGREES and compared it against fixed thresholds, which is
 * wrong twice over:
 *
 *  - It ignored the viewport. The same route fitted identically on a 540px
 *    preview and a 2160px tablet, when those are three zoom levels apart.
 *  - It ignored latitude. Longitude maps to x independently of latitude, but
 *    Mercator stretches latitude into y by roughly `1 / cos(latitude)` — so the
 *    same 0.2° box is 583px tall at the equator, 1169px at 60°N and 2266px at
 *    75°N. The ladder returned one zoom for all three. At 60°N it picked one
 *    level too close and at 75°N two, and a route zoomed past its own bounds is
 *    a route with its ends off screen.
 *
 * It was also clamped to a fixed 7..16 regardless of what the packs hold, so a
 * city extract could not reach the detail it was imported for.
 *
 * A zero-area box — a single point, or a route that never moved — has no zoom
 * that "fits" it, so it takes the pack's maximum: the closest look the data
 * supports.
 */
internal fun mapsforgeZoomForBounds(
    boundingBox: BoundingBox,
    viewport: Dimension,
    tileSize: Int,
    zoomRange: MapsforgeZoomRange,
): Byte {
    if (viewport.width <= 0 || viewport.height <= 0) return zoomRange.max
    val degenerate = boundingBox.maxLatitude <= boundingBox.minLatitude &&
        boundingBox.maxLongitude <= boundingBox.minLongitude
    if (degenerate) return zoomRange.max
    return zoomRange.clamp(LatLongUtils.zoomForBounds(viewport, boundingBox, tileSize))
}

private const val TileCacheNameHexLength = 16
