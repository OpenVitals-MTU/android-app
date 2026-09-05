package tech.mmarca.openvitals.features.activity.maps

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Dimension
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

/** The Mapsforge decisions: the tile-cache name and the zoom. All were defects. */
class MapsforgeRouteMapSupportTest {

    // Tile cache naming.

    @Test
    fun `the same packs at the same paths reuse their cache`() {
        // Reopening a route must not re-render every tile.
        val packs = listOf(pack("europe", "/data/maps/europe.map"))

        assertThat(mapsforgeTileCacheName(packs)).isEqualTo(mapsforgeTileCacheName(packs))
    }

    @Test
    fun `re-importing a pack under a new path takes a different cache`() {
        // The name keyed on ids alone while the MapView is rebuilt on ids and paths, so the new map
        // opened the old map's cache directory and the outgoing map's destroyAll() deleted it.
        val before = listOf(pack("europe", "/data/maps/europe.map"))
        val after = listOf(pack("europe", "/data/maps/europe-2026.map"))

        assertThat(mapsforgeTileCacheName(before)).isNotEqualTo(mapsforgeTileCacheName(after))
    }

    @Test
    fun `different pack sets do not share a cache`() {
        val one = listOf(pack("europe", "/data/maps/europe.map"))
        val two = listOf(
            pack("europe", "/data/maps/europe.map"),
            pack("iberia", "/data/maps/iberia.map"),
        )
        val reordered = listOf(
            pack("iberia", "/data/maps/iberia.map"),
            pack("europe", "/data/maps/europe.map"),
        )

        assertThat(setOf(
            mapsforgeTileCacheName(one),
            mapsforgeTileCacheName(two),
            mapsforgeTileCacheName(reordered),
        )).hasSize(3)
    }

    @Test
    fun `the cache name is a safe, bounded directory name`() {
        // It becomes a directory under the cache dir, and a pack path can hold anything.
        val name = mapsforgeTileCacheName(
            listOf(pack("a b/c", "/data/maps/Ostróda ‧ 2026.map")),
        )

        assertThat(name).matches("openvitals-[0-9a-f]{16}")
    }

    // Zoom.

    @Test
    fun `a bigger viewport fits the same route at a closer zoom`() {
        // The old ladder compared degrees against fixed thresholds, so a small preview and a tablet map got the same zoom.
        val route = BoundingBox(51.50, -0.13, 51.53, -0.09)

        val preview = mapsforgeZoomForBounds(route, Dimension(540, 540), TILE, RANGE)
        val phone = mapsforgeZoomForBounds(route, Dimension(1080, 1080), TILE, RANGE)
        val tablet = mapsforgeZoomForBounds(route, Dimension(2160, 2160), TILE, RANGE)

        assertThat(preview).isLessThan(phone)
        assertThat(phone).isLessThan(tablet)
    }

    @Test
    fun `the same degree span needs a wider zoom the further north it is`() {
        // Mercator stretches latitude by roughly 1 / cos(latitude): the same 0.2-degree box is 583px
        // at the equator and 2266px at 75N, so it needs a lower zoom further north.
        // The old latitude-blind ladder drew northern routes with their ends off screen.
        val viewport = Dimension(1080, 1080)

        val equator = mapsforgeZoomForBounds(BoundingBox(0.0, 0.0, 0.20, 0.20), viewport, TILE, RANGE)
        val north = mapsforgeZoomForBounds(BoundingBox(60.0, 0.0, 60.20, 0.20), viewport, TILE, RANGE)
        val arctic = mapsforgeZoomForBounds(BoundingBox(75.0, 0.0, 75.20, 0.20), viewport, TILE, RANGE)

        assertThat(north).isLessThan(equator)
        assertThat(arctic).isLessThan(north)
    }

    @Test
    fun `the zoom stays inside what the packs actually hold`() {
        // A level outside the pack's range renders blank. The old ladder was clamped to 7..16 regardless.
        val cityOnly = MapsforgeZoomRange(min = 12, max = 18)
        val tiny = BoundingBox(51.5000, -0.1000, 51.5002, -0.0998)

        val zoom = mapsforgeZoomForBounds(tiny, Dimension(1080, 1080), TILE, cityOnly)

        assertThat(zoom).isAtMost(18)
        assertThat(zoom).isAtLeast(12)
    }

    @Test
    fun `a whole continent clamps to the pack's minimum rather than going below it`() {
        val coarse = MapsforgeZoomRange(min = 5, max = 12)
        val continent = BoundingBox(35.0, -10.0, 60.0, 30.0)

        assertThat(mapsforgeZoomForBounds(continent, Dimension(540, 540), TILE, coarse))
            .isAtLeast(5)
    }

    @Test
    fun `a single point takes the closest zoom the pack supports`() {
        // No zoom fits a zero-area box; a lone marker wants the closest look.
        val single = BoundingBox(51.5, -0.1, 51.5, -0.1)

        assertThat(mapsforgeZoomForBounds(single, Dimension(1080, 1080), TILE, RANGE))
            .isEqualTo(RANGE.max)
    }

    @Test
    fun `an unmeasured viewport does not produce a fitted zoom`() {
        // A zoom chosen against a viewport of unknown size is the whole defect.
        val route = BoundingBox(51.50, -0.13, 51.53, -0.09)

        assertThat(mapsforgeZoomForBounds(route, Dimension(0, 0), TILE, RANGE))
            .isEqualTo(RANGE.max)
    }

    // Bounding box.

    @Test
    fun `the box spans every finite point, including the live one`() {
        val box = routeBoundingBox(
            points = listOf(point(51.50, -0.13), point(51.53, -0.09)),
            currentPoint = point(51.55, -0.20),
        )

        assertThat(box).isNotNull()
        assertThat(box!!.minLatitude).isEqualTo(51.50)
        assertThat(box.maxLatitude).isEqualTo(51.55)
        assertThat(box.minLongitude).isEqualTo(-0.20)
        assertThat(box.maxLongitude).isEqualTo(-0.09)
    }

    @Test
    fun `non-finite coordinates are dropped rather than poisoning the box`() {
        val box = routeBoundingBox(
            points = listOf(
                point(Double.NaN, -0.13),
                point(51.52, Double.POSITIVE_INFINITY),
                point(51.50, -0.10),
            ),
            currentPoint = null,
        )

        assertThat(box).isNotNull()
        assertThat(box!!.minLatitude).isEqualTo(51.50)
        assertThat(box.maxLongitude).isEqualTo(-0.10)
    }

    @Test
    fun `an indoor activity has no box at all`() {
        // Null rather than a zeroed box, which would centre the map in the Gulf of Guinea.
        assertThat(routeBoundingBox(emptyList(), currentPoint = null)).isNull()
        assertThat(routeBoundingBox(listOf(point(Double.NaN, Double.NaN)), null)).isNull()
    }

    private fun pack(id: String, path: String) = OfflineMapPack(
        id = id,
        displayName = id,
        originalFileName = path.substringAfterLast('/'),
        sizeBytes = 1_024L,
        importedAtMillis = 0L,
        path = path,
        format = OfflineMapPackFormat.MAPSFORGE,
    )

    private fun point(latitude: Double, longitude: Double) = ExerciseRoutePoint(
        time = Instant.EPOCH,
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = null,
        horizontalAccuracyMeters = null,
        verticalAccuracyMeters = null,
    )

    private companion object {
        const val TILE = 256
        val RANGE = MapsforgeZoomRange(min = 0, max = 20)
    }
}
