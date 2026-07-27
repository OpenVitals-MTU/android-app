import 'package:latlong2/latlong.dart';
import 'package:meta/meta.dart';

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

/// Everything [RouteMapView] needs to draw a route, derived once per distinct
/// point list instead of once per build.
///
/// The map sits near the bottom of a `ListView` on a screen that rebuilds
/// whenever anything it watches changes, and during a live recording a ticker
/// rebuilds it every second. Each rebuild used to re-run the segment split, the
/// bounds fold, the marker filter AND — because flutter_map drops its projection
/// cache unconditionally in `didUpdateWidget` — a full Web-Mercator projection
/// plus Douglas-Peucker pass over every point.
@immutable
class RouteMapGeometry {
  const RouteMapGeometry({
    required this.segments,
    required this.bounds,
    required this.startPoint,
    required this.endPoint,
    required this.currentPoint,
  });

  /// Drawable polyline segments — already projected to [LatLng], already
  /// filtered to those with at least two points.
  final List<List<LatLng>> segments;

  /// The camera box over the route plus the current point, or null when there
  /// is nothing finite to fit.
  final RouteBounds? bounds;

  final LatLng? startPoint;
  final LatLng? endPoint;
  final LatLng? currentPoint;
}

/// Derives [RouteMapGeometry] from raw route points. Pure; no widget or
/// `flutter_map` types.
RouteMapGeometry buildRouteMapGeometry({
  required List<ExerciseRoutePoint> points,
  List<int> routeBreakIndexes = const <int>[],
  ExerciseRoutePoint? currentPoint,
}) {
  final segments = <List<LatLng>>[
    for (final segment in routeSegments(points, routeBreakIndexes))
      if (segment.length >= 2)
        [for (final point in segment) LatLng(point.latitude, point.longitude)],
  ];
  final valid = points
      .where((point) => point.latitude.isFinite && point.longitude.isFinite)
      .toList();
  final hasCurrent = currentPoint != null &&
      currentPoint.latitude.isFinite &&
      currentPoint.longitude.isFinite;
  return RouteMapGeometry(
    segments: segments,
    bounds: RouteBounds.fromPoints(<ExerciseRoutePoint>[...points, ?currentPoint]),
    startPoint: valid.isEmpty
        ? null
        : LatLng(valid.first.latitude, valid.first.longitude),
    endPoint: valid.length > 1
        ? LatLng(valid.last.latitude, valid.last.longitude)
        : null,
    currentPoint: hasCurrent
        ? LatLng(currentPoint.latitude, currentPoint.longitude)
        : null,
  );
}

/// The most route points worth drawing.
///
/// A 240dp-tall map cannot show more, and everything downstream of the point
/// list is per-point work: the projection, the simplification pass, the polyline
/// itself. Imported route files have been capped here since the importer was
/// written; routes read back from Health Connect were not, and a 1Hz-sampled
/// long ride carries ten thousand points or more.
const int maxDisplayedRoutePoints = 2000;

/// Uniform-stride decimation to at most [maxPoints], returning [points]
/// unchanged (and identical) when it is already short enough — which is what
/// lets a caller memoize on list identity.
///
/// Deliberately stride-based rather than Douglas-Peucker: this runs on a raw
/// track before anything knows the zoom, and flutter_map applies its own
/// tolerance-based simplification per zoom afterwards.
List<ExerciseRoutePoint> simplifyRoutePoints(
  List<ExerciseRoutePoint> points, {
  int maxPoints = maxDisplayedRoutePoints,
}) {
  if (points.length <= maxPoints) return points;
  final lastIndex = points.length - 1;
  final step = lastIndex / (maxPoints - 1);
  final seen = <int>{};
  final result = <ExerciseRoutePoint>[];
  for (var index = 0; index < maxPoints; index++) {
    final pickRaw = (index * step).toInt();
    final pick = pickRaw < 0 ? 0 : (pickRaw > lastIndex ? lastIndex : pickRaw);
    final point = points[pick];
    if (seen.add(point.time.microsecondsSinceEpoch)) {
      result.add(point);
    }
  }
  return result;
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
