package tech.mmarca.openvitals.features.activity.maps

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
import org.maplibre.geojson.LineString
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

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
    fun `invalid route break indexes are ignored`() {
        val points = routePoints(3)

        val segments = routeSegments(
            points = points,
            routeBreakIndexes = listOf(0, 99),
        )

        assertEquals(listOf(points), segments)
    }

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
