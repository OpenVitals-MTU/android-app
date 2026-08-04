package tech.mmarca.openvitals.features.activity.maps

import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

internal fun routeLineFeatureCollection(
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int>,
): FeatureCollection =
    FeatureCollection.fromFeatures(
        routeSegments(points, routeBreakIndexes)
            .filter { segment -> segment.size >= 2 }
            .map { segment ->
                Feature.fromGeometry(
                    LineString.fromLngLats(
                        segment.map { point -> Point.fromLngLat(point.longitude, point.latitude) },
                    ),
                )
            },
    )

internal fun pointFeatureCollection(point: ExerciseRoutePoint?): FeatureCollection =
    FeatureCollection.fromFeatures(
        listOfNotNull(
            point?.let {
                Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude))
            },
        ),
    )

internal fun routeSegments(
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int>,
): List<List<ExerciseRoutePoint>> {
    val validPoints = points.filter { it.latitude.isFinite() && it.longitude.isFinite() }
    if (validPoints.isEmpty()) return emptyList()
    val breakIndexes = routeBreakIndexes
        .filter { it in 1 until validPoints.size }
        .toSet()
    val segments = mutableListOf<MutableList<ExerciseRoutePoint>>()
    validPoints.forEachIndexed { index, point ->
        if (index == 0 || index in breakIndexes) {
            segments += mutableListOf(point)
        } else {
            segments.lastOrNull()?.add(point)
        }
    }
    return segments
}
