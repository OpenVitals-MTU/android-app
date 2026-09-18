package tech.mmarca.openvitals.core.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SharedGeoPointTest {

    private fun assertPosition(point: SharedGeoPoint?, latitude: Double, longitude: Double) {
        assertNotNull(point)
        assertEquals(latitude, point!!.latitude!!, 1e-6)
        assertEquals(longitude, point.longitude!!, 1e-6)
    }

    // geo: links

    @Test fun `a plain geo link`() {
        val point = GeoUri.parse("geo:48.8584,2.2945")

        assertPosition(point, 48.8584, 2.2945)
        assertNull(point!!.name)
        assertNull(point.altitudeMeters)
    }

    @Test fun `altitude, parameters and zoom`() {
        val point = GeoUri.parse("geo:-33.8568,151.2153,12.5;crs=wgs84;u=35?z=17")

        assertPosition(point, -33.8568, 151.2153)
        assertEquals(12.5, point!!.altitudeMeters!!, 1e-6)
    }

    @Test fun `a zero path takes its position and label from the query`() {
        val point = GeoUri.parse("geo:0,0?q=48.8584,2.2945(Eiffel+Tower)")

        assertPosition(point, 48.8584, 2.2945)
        assertEquals("Eiffel Tower", point!!.name)
    }

    @Test fun `a real path wins, the query still names it`() {
        val point = GeoUri.parse("geo:48.8584,2.2945?q=48.0,2.0(Caf%C3%A9%20du%20coin)")

        assertPosition(point, 48.8584, 2.2945)
        assertEquals("Café du coin", point!!.name)
    }

    @Test fun `a bracketed parameter is a label`() {
        val point = GeoUri.parse("geo:37.78918,-122.40335?z=14&(Wikimedia+Foundation)")

        assertPosition(point, 37.78918, -122.40335)
        assertEquals("Wikimedia Foundation", point!!.name)
    }

    @Test fun `a search-only link has a name and no position`() {
        val point = GeoUri.parse("geo:0,0?q=Eiffel+Tower,+Paris")

        assertEquals("Eiffel Tower, Paris", point!!.name)
        assertFalse(point.hasPosition)
    }

    @Test fun `what is not a usable geo link is null`() {
        assertNull(GeoUri.parse("https://example.org"))
        assertNull(GeoUri.parse("geo:"))
        assertNull(GeoUri.parse("geo:91,2"))
        assertNull(GeoUri.parse("geo:abc"))
        // A broken escape must not throw.
        assertNotNull(GeoUri.parse("geo:1,2?q=%E0%A4%A"))
    }

    // shared text

    @Test fun `a geo link inside shared text`() {
        val point = SharedLocationText.parse("Meet here: geo:48.8584,2.2945?z=16 see you")

        assertPosition(point, 48.8584, 2.2945)
    }

    @Test fun `Google Maps URLs`() {
        assertPosition(
            SharedLocationText.parse("https://www.google.com/maps/search/?api=1&query=48.8584,2.2945"),
            48.8584, 2.2945,
        )
        assertPosition(
            SharedLocationText.parse("https://maps.google.com/?q=-33.8568%2C151.2153"),
            -33.8568, 151.2153,
        )
        assertPosition(
            SharedLocationText.parse("https://www.google.com/maps/@48.8584,2.2945,17z"),
            48.8584, 2.2945,
        )
        // The pin, not the map centre.
        assertPosition(
            SharedLocationText.parse(
                "https://www.google.com/maps/place/X/@48.80,2.20,15z/data=!3m1!4b1!4m6!3m5!8m2!3d48.8584!4d2.2945",
            ),
            48.8584, 2.2945,
        )
    }

    @Test fun `OpenStreetMap and OsmAnd URLs`() {
        assertPosition(
            SharedLocationText.parse("https://www.openstreetmap.org/?mlat=48.8584&mlon=2.2945#map=17/48.85/2.29"),
            48.8584, 2.2945,
        )
        assertPosition(
            SharedLocationText.parse("https://www.openstreetmap.org/#map=17/48.8584/2.2945"),
            48.8584, 2.2945,
        )
        assertPosition(
            SharedLocationText.parse("https://osmand.net/map?pin=48.8584,2.2945#16/48.85/2.29"),
            48.8584, 2.2945,
        )
        assertPosition(
            SharedLocationText.parse("https://osmand.net/map/#16/48.8584/2.2945"),
            48.8584, 2.2945,
        )
    }

    @Test fun `the first plain line names the point`() {
        val point = SharedLocationText.parse("Eiffel Tower\nhttps://maps.google.com/?q=48.8584,2.2945")

        assertPosition(point, 48.8584, 2.2945)
        assertEquals("Eiffel Tower", point!!.name)
    }

    @Test fun `bare coordinates in the text`() {
        val point = SharedLocationText.parse("GC12345 Old mill\nN 48° 51.504 E 002° 17.670")

        assertPosition(point, 48.8584, 2.2945)
        assertEquals("GC12345 Old mill", point!!.name)
    }

    @Test fun `a short link keeps the name and has no position`() {
        val point = SharedLocationText.parse("Eiffel Tower\nhttps://maps.app.goo.gl/AbCdEf123")

        assertEquals("Eiffel Tower", point!!.name)
        assertFalse(point.hasPosition)
    }

    @Test fun `text with nothing useful is null`() {
        assertNull(SharedLocationText.parse("https://example.org/article/12345"))
        assertNull(SharedLocationText.parse("   "))
    }
}
