package tech.mmarca.openvitals.features.activity.maps

import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import tech.mmarca.openvitals.domain.model.CoMapsCoordinate
import tech.mmarca.openvitals.domain.model.CoMapsRoutePolyline
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

/** Every point CoMaps served: decimating once drew a straight line through buildings. Paid once per revision. */
internal fun plannedRouteFeatureCollection(polyline: CoMapsRoutePolyline?): FeatureCollection =
    FeatureCollection.fromFeatures(
        listOfNotNull(
            polyline
                ?.takeUnless { it.isEmpty }
                ?.let { route ->
                    Feature.fromGeometry(
                        LineString.fromLngLats(
                            (0 until route.pointCount).map { index ->
                                Point.fromLngLat(
                                    route.longitudeAt(index),
                                    route.latitudeAt(index),
                                )
                            },
                        ),
                    )
                },
        ),
    )

/** The bent white shafts of the turn arrows, each following its bend. */
internal fun plannedRouteArrowShaftFeatureCollection(
    arrows: List<PlannedRouteArrow>,
): FeatureCollection =
    FeatureCollection.fromFeatures(
        arrows.map { arrow ->
            Feature.fromGeometry(
                LineString.fromLngLats(
                    (0 until arrow.shaft.size / 2).map { index ->
                        Point.fromLngLat(arrow.shaft[index * 2 + 1], arrow.shaft[index * 2])
                    },
                ),
            )
        },
    )

/** The arrowheads, each carrying the bearing it points. */
internal fun plannedRouteArrowHeadFeatureCollection(
    arrows: List<PlannedRouteArrow>,
): FeatureCollection =
    FeatureCollection.fromFeatures(
        arrows.map { arrow ->
            Feature.fromGeometry(
                Point.fromLngLat(arrow.headLongitude, arrow.headLatitude),
            ).apply {
                addNumberProperty("bearing", arrow.bearingDegrees)
            }
        },
    )

/** The destination CoMaps is navigating to, or nothing to draw. */
internal fun destinationFeatureCollection(destination: CoMapsCoordinate?): FeatureCollection =
    FeatureCollection.fromFeatures(
        listOfNotNull(
            destination?.let {
                Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude))
            },
        ),
    )

/** The device-heading arrow at the current position, or nothing to draw. */
internal fun headingFeatureCollection(
    point: ExerciseRoutePoint?,
    bearingDegrees: Float?,
): FeatureCollection =
    FeatureCollection.fromFeatures(
        listOfNotNull(
            if (point == null || bearingDegrees == null) {
                null
            } else {
                Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude)).apply {
                    addNumberProperty("bearing", bearingDegrees)
                }
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
