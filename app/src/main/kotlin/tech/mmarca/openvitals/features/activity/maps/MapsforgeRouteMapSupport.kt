package tech.mmarca.openvitals.features.activity.maps

import android.app.Application
import java.security.MessageDigest
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Dimension
import org.mapsforge.core.util.LatLongUtils
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

/** The pure and process-wide parts of the Mapsforge map, testable without a `MapView`. */

@Volatile
private var graphicsFactoryInstalled = false
private val graphicsFactoryLock = Any()

/**
 * Installs Mapsforge's graphics factory once per process. A second call
 * swaps the factory out from under whatever is drawing. Deferred until the
 * first map is built.
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
 * The tile-cache directory name for a set of packs: a digest of the same
 * string the `MapView` is keyed on. Keying on ids alone let a re-imported
 * pack's new map share, and lose, the outgoing map's cache.
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

/** The smallest box containing every finite point, or null. Never fall back to 0,0. */
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
 * The zoom that fits [boundingBox] into [viewport], via
 * `LatLongUtils.zoomForBounds`, which accounts for the viewport and for
 * Mercator's latitude stretch. A zero-area box takes the pack's maximum.
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
