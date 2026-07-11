import '../../../core/geo/geo_distance.dart';
import '../../../domain/model/activity_models.dart';

/// Pure route geometry helpers shared by [RouteMapView] and its tests.
///
/// Ported from the segment logic in the Kotlin `OfflineRouteGeoJson` /
/// `ActivityRoutePreview`, plus bounds + haversine total-distance helpers. No
/// plugin (or `flutter_map`) imports, so it is fully unit-testable.

/// Splits [points] into contiguous polyline segments, breaking at each index in
/// [routeBreakIndexes] (a break starts a new segment). Non-finite coordinates
/// are dropped first; break indexes outside `1..length-1` are ignored.
List<List<ExerciseRoutePoint>> routeSegments(
  List<ExerciseRoutePoint> points,
  List<int> routeBreakIndexes,
) {
  final validPoints = points
      .where((point) => point.latitude.isFinite && point.longitude.isFinite)
      .toList();
  if (validPoints.isEmpty) return const <List<ExerciseRoutePoint>>[];

  final breakIndexes = routeBreakIndexes
      .where((index) => index >= 1 && index < validPoints.length)
      .toSet();
  final segments = <List<ExerciseRoutePoint>>[];
  for (var index = 0; index < validPoints.length; index++) {
    if (index == 0 || breakIndexes.contains(index)) {
      segments.add(<ExerciseRoutePoint>[validPoints[index]]);
    } else {
      segments.last.add(validPoints[index]);
    }
  }
  return segments;
}

/// A lat/long bounding box around a set of route points.
class RouteBounds {
  const RouteBounds({
    required this.minLatitude,
    required this.maxLatitude,
    required this.minLongitude,
    required this.maxLongitude,
  });

  final double minLatitude;
  final double maxLatitude;
  final double minLongitude;
  final double maxLongitude;

  double get centerLatitude => (minLatitude + maxLatitude) / 2.0;
  double get centerLongitude => (minLongitude + maxLongitude) / 2.0;

  /// True when every corner collapses to a single coordinate.
  bool get isSinglePoint =>
      minLatitude == maxLatitude && minLongitude == maxLongitude;

  /// The tightest box containing all finite [points], or null if there are none.
  static RouteBounds? fromPoints(Iterable<ExerciseRoutePoint> points) {
    double? minLat, maxLat, minLng, maxLng;
    for (final point in points) {
      if (!point.latitude.isFinite || !point.longitude.isFinite) continue;
      minLat = (minLat == null || point.latitude < minLat) ? point.latitude : minLat;
      maxLat = (maxLat == null || point.latitude > maxLat) ? point.latitude : maxLat;
      minLng = (minLng == null || point.longitude < minLng) ? point.longitude : minLng;
      maxLng = (maxLng == null || point.longitude > maxLng) ? point.longitude : maxLng;
    }
    if (minLat == null || maxLat == null || minLng == null || maxLng == null) {
      return null;
    }
    return RouteBounds(
      minLatitude: minLat,
      maxLatitude: maxLat,
      minLongitude: minLng,
      maxLongitude: maxLng,
    );
  }
}

/// Total travelled distance in meters: the sum of great-circle distances
/// between consecutive points, computed per segment so a route break does not
/// contribute a spurious straight-line jump.
double routeTotalDistanceMeters(
  List<ExerciseRoutePoint> points, {
  List<int> routeBreakIndexes = const <int>[],
}) {
  var total = 0.0;
  for (final segment in routeSegments(points, routeBreakIndexes)) {
    for (var index = 1; index < segment.length; index++) {
      final previous = segment[index - 1];
      final current = segment[index];
      total += haversineMeters(
        previous.latitude,
        previous.longitude,
        current.latitude,
        current.longitude,
      );
    }
  }
  return total;
}
