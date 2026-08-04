package tech.mmarca.openvitals.features.activity.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineMapPackFormatTest {

    @Test
    fun `detects supported offline map file extensions`() {
        assertEquals(OfflineMapPackFormat.PMTILES, OfflineMapPackFormat.fromFileName("estonia.pmtiles"))
        assertEquals(OfflineMapPackFormat.MAPSFORGE, OfflineMapPackFormat.fromFileName("estonia.map"))
        assertEquals(OfflineMapPackFormat.MAPSFORGE, OfflineMapPackFormat.fromFileName("estonia.maps"))
        assertNull(OfflineMapPackFormat.fromFileName("estonia.osm.pbf"))
    }
}
