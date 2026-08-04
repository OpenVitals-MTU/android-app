package tech.mmarca.openvitals.features.activity.maps

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.geojson.LineString
import tech.mmarca.openvitals.core.geo.haversineMeters
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.features.manualentry.activity.recording.activityRecordingRouteDistanceMeters
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.routeDistanceMeters

class OfflineRouteGeoJsonTest {

    @Test
    fun `route break indexes split route line features`() {
        val points = routePoints(5)

        val collection = routeLineFeatureCollection(
            points = points,
            routeBreakIndexes = listOf(2, 4),
        )

        val features = collection.features().orEmpty()
        assertEquals(2, features.size)
        assertEquals(2, (features[0].geometry() as LineString).coordinates().size)
        assertEquals(2, (features[1].geometry() as LineString).coordinates().size)
    }

    @Test
    fun `break indexes split the route into separate segments, tail included`() {
        // The drawing layer drops a one-point tail (a polyline needs two), but
        // `routeSegments` keeps it — losing it here would lose the point itself.
        val segments = routeSegments(routePoints(5), routeBreakIndexes = listOf(2, 4))

        assertEquals(3, segments.size)
        assertEquals(listOf(2, 2, 1), segments.map { it.size })
    }

    @Test
    fun `segments are split at breaks and keep their own coordinates`() {
        val features = routeLineFeatureCollection(
            points = routePoints(5),
            routeBreakIndexes = listOf(2),
        ).features().orEmpty()

        // Two drawable segments: [0,1] and [2,3,4]. The one-point tail case is
        // covered above; here the coordinates themselves must survive the trip.
        assertEquals(2, features.size)
        val first = (features[0].geometry() as LineString).coordinates()
        assertEquals(2, first.size)
        assertEquals(52.0, first[0].latitude(), 1e-9)
        assertEquals(13.0, first[0].longitude(), 1e-9)
        val second = (features[1].geometry() as LineString).coordinates()
        assertEquals(3, second.size)
        assertEquals(52.0 + 2, second[0].latitude(), 1e-9)
    }

    @Test
    fun `invalid route break indexes are ignored`() {
        val points = routePoints(3)

        val segments = routeSegments(
            points = points,
            routeBreakIndexes = listOf(0, 99),
        )

        assertEquals(listOf(points), segments)
    }

    @Test
    fun `non-finite coordinates are dropped`() {
        val points = listOf(
            routePoint(0, latitude = 52.0, longitude = 13.0),
            routePoint(1, latitude = Double.NaN, longitude = 13.1),
            routePoint(2, latitude = 52.2, longitude = 13.2),
        )

        val segments = routeSegments(points, routeBreakIndexes = emptyList())

        assertEquals(1, segments.size)
        assertEquals(2, segments.single().size)
    }

    @Test
    fun `a route with no finite points yields no line and no markers`() {
        val points = listOf(
            routePoint(0, latitude = Double.NaN, longitude = Double.NaN),
        )

        assertTrue(
            routeLineFeatureCollection(points, routeBreakIndexes = emptyList())
                .features()
                .orEmpty()
                .isEmpty()
        )
        assertTrue(pointFeatureCollection(null).features().orEmpty().isEmpty())
    }

    @Test
    fun `route distance sums haversine distance between consecutive points`() {
        val a = routePoint(0, latitude = 52.0, longitude = 13.0)
        val b = routePoint(1, latitude = 52.01, longitude = 13.0)
        val c = routePoint(2, latitude = 52.01, longitude = 13.02)
        val expected = haversineMeters(52.0, 13.0, 52.01, 13.0) +
            haversineMeters(52.01, 13.0, 52.01, 13.02)

        assertEquals(expected, routeDistanceMeters(listOf(a, b, c)), 1e-6)
    }

    @Test
    fun `route distance does not bridge across a route break`() {
        val points = listOf(
            routePoint(0, latitude = 52.0, longitude = 13.0),
            routePoint(1, latitude = 52.01, longitude = 13.0),
            routePoint(2, latitude = 60.0, longitude = 20.0),
            routePoint(3, latitude = 60.01, longitude = 20.0),
        )
        val window = points.first().time.minusSeconds(1) to points.last().time.plusSeconds(1)

        val withBreak = activityRecordingRouteDistanceMeters(
            points = points,
            routeBreakIndexes = listOf(2),
            startTime = window.first,
            endTime = window.second,
        )
        val withoutBreak = activityRecordingRouteDistanceMeters(
            points = points,
            routeBreakIndexes = emptyList(),
            startTime = window.first,
            endTime = window.second,
        )

        // The 900 km jump between the two legs is not distance travelled.
        assertTrue(withBreak < withoutBreak)
        val segmentSum = haversineMeters(52.0, 13.0, 52.01, 13.0) +
            haversineMeters(60.0, 20.0, 60.01, 20.0)
        assertEquals(segmentSum, withBreak, 1e-6)
    }

    @Test
    fun `route distance is zero for a single point`() {
        assertEquals(
            0.0,
            routeDistanceMeters(listOf(routePoint(0, latitude = 52.0, longitude = 13.0))),
            0.0,
        )
    }

    private fun routePoint(index: Int, latitude: Double, longitude: Double) =
        ExerciseRoutePoint(
            time = Instant.ofEpochSecond(index.toLong()),
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = null,
            horizontalAccuracyMeters = null,
            verticalAccuracyMeters = null,
        )

    private fun routePoints(count: Int): List<ExerciseRoutePoint> =
        List(count) { index ->
            ExerciseRoutePoint(
                time = Instant.ofEpochSecond(index.toLong()),
                latitude = 52.0 + index,
                longitude = 13.0 + index,
                altitudeMeters = null,
                horizontalAccuracyMeters = null,
                verticalAccuracyMeters = null,
            )
        }
}
