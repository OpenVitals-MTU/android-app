package tech.mmarca.openvitals.features.activity.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Tag
import org.mapsforge.core.model.Tile
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.datastore.MapReadResult
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.datastore.Way

/**
 * The merge policy the base map builds its [MultiMapDataStore] with. A one-word choice
 * (`DataPolicy.DEDUPLICATE`) whose consequence is invisible until two packs meet.
 *
 * `supportsTile` is only a box check, and a pack's box is a rectangle around a region-shaped
 * extract. Under RETURN_FIRST the first pack claims every tile in its rectangle, answers empty,
 * and the pack that covers the ground is never asked. That shipped a blank wedge across a seam.
 */
class MultiMapMergePolicyTest {

    /** A tile inside both packs' boxes: the seam. */
    private val seamTile = Tile(8710, 5620, 14, 256)

    // Two packs whose boxes both swallow the tile. Only the second has anything to draw there.
    private val seamBox: BoundingBox = seamTile.boundingBox.let { box ->
        BoundingBox(
            box.minLatitude - 1,
            box.minLongitude - 1,
            box.maxLatitude + 1,
            box.maxLongitude + 1,
        )
    }
    private val emptyHere = FakeMapDataStore(seamBox, ways = emptyList())
    private val holdsTheData = FakeMapDataStore(
        boundingBox = seamBox,
        ways = listOf(way(seamTile.boundingBox.minLatitude, seamTile.boundingBox.minLongitude)),
    )

    @Test
    fun `a tile at the seam draws data from whichever pack actually holds it`() {
        val datastore = MultiMapDataStore(MultiMapDataStore.DataPolicy.DEDUPLICATE)
        datastore.addMapDataStore(emptyHere, false, false)
        datastore.addMapDataStore(holdsTheData, false, false)

        val result = datastore.readMapData(seamTile)

        // The pack that covers the ground must be read even when an earlier pack claims the tile.
        assertEquals(1, result.ways.size)
        assertEquals(1, holdsTheData.reads)
    }

    @Test
    fun `every pack whose bounding box covers a tile is read, not just the first`() {
        // The direct statement of the bug: RETURN_FIRST stops at the first pack.
        val datastore = MultiMapDataStore(MultiMapDataStore.DataPolicy.DEDUPLICATE)
        datastore.addMapDataStore(emptyHere, false, false)
        datastore.addMapDataStore(holdsTheData, false, false)

        datastore.readMapData(seamTile)

        assertEquals(1, emptyHere.reads)
        assertEquals(1, holdsTheData.reads)
    }

    @Test
    fun `a tile only one pack covers reads only that pack`() {
        // A tile deep inside one region never touches the other.
        val datastore = MultiMapDataStore(MultiMapDataStore.DataPolicy.DEDUPLICATE)
        val farAway = FakeMapDataStore(BoundingBox(-40.0, -40.0, -30.0, -30.0), ways = emptyList())
        datastore.addMapDataStore(farAway, false, false)
        datastore.addMapDataStore(holdsTheData, false, false)

        val result = datastore.readMapData(seamTile)

        assertEquals(1, result.ways.size)
        assertEquals(0, farAway.reads)
    }

    @Test
    fun `a tile no pack covers stays empty rather than drawing a blank`() {
        val datastore = MultiMapDataStore(MultiMapDataStore.DataPolicy.DEDUPLICATE)
        val farAway = FakeMapDataStore(BoundingBox(-40.0, -40.0, -30.0, -30.0), ways = emptyList())
        datastore.addMapDataStore(farAway, false, false)

        val result = datastore.readMapData(seamTile)

        // Nothing is drawn: no ways, no POIs, and the pack is never read.
        assertTrue(result.ways.isEmpty())
        assertTrue(result.pois.isEmpty())
        assertEquals(0, farAway.reads)
    }

    private fun way(latitude: Double, longitude: Double): Way = Way(
        0,
        emptyList<Tag>(),
        arrayOf(arrayOf(LatLong(latitude, longitude), LatLong(latitude + 0.001, longitude))),
        null,
    )

    /** Answers `supportsTile` as `MapFile` does, from the box and zoom range, while [ways] models what it holds. */
    private class FakeMapDataStore(
        private val boundingBox: BoundingBox,
        private val ways: List<Way>,
    ) : MapDataStore() {

        var reads = 0
            private set

        override fun boundingBox(): BoundingBox = boundingBox

        override fun close() = Unit

        override fun getDataTimestamp(tile: Tile): Long = 0L

        override fun readMapData(tile: Tile): MapReadResult {
            reads++
            return MapReadResult().also { result -> result.ways.addAll(ways) }
        }

        override fun readPoiData(tile: Tile): MapReadResult = MapReadResult()

        override fun startPosition(): LatLong? = null

        override fun startZoomLevel(): Byte? = null

        override fun supportsTile(tile: Tile): Boolean =
            boundingBox.intersects(tile.boundingBox)

        override fun supportsFullTile(tile: Tile): Boolean =
            boundingBox.contains(tile.boundingBox)

        override fun supportsArea(boundingBox: BoundingBox, zoomLevel: Byte): Boolean =
            this.boundingBox.intersects(boundingBox)

        override fun supportsFullArea(boundingBox: BoundingBox, zoomLevel: Byte): Boolean =
            this.boundingBox.contains(boundingBox)

        private fun BoundingBox.contains(other: BoundingBox): Boolean =
            minLatitude <= other.minLatitude &&
                maxLatitude >= other.maxLatitude &&
                minLongitude <= other.minLongitude &&
                maxLongitude >= other.maxLongitude
    }
}
