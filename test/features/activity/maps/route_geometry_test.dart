import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/geo/geo_distance.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/features/activity/maps/route_geometry.dart';

ExerciseRoutePoint point(double lat, double lng, {int secondsOffset = 0}) =>
    ExerciseRoutePoint(
      time: DateTime.utc(2026, 6, 1).add(Duration(seconds: secondsOffset)),
      latitude: lat,
      longitude: lng,
      altitudeMeters: null,
      horizontalAccuracyMeters: null,
      verticalAccuracyMeters: null,
    );

List<ExerciseRoutePoint> line(int count) =>
    List.generate(count, (i) => point(52.0 + i, 13.0 + i, secondsOffset: i));

void main() {
  group('routeSegments', () {
    test('break indexes split the route into separate segments', () {
      final segments = routeSegments(line(5), const [2, 4]);

      expect(segments.length, 3);
      expect(segments[0].length, 2);
      expect(segments[1].length, 2);
      expect(segments[2].length, 1);
    });

    test('invalid break indexes are ignored', () {
      final points = line(3);

      final segments = routeSegments(points, const [0, 99]);

      expect(segments, [points]);
    });

    test('non-finite coordinates are dropped', () {
      final points = [point(52.0, 13.0), point(double.nan, 13.1), point(52.2, 13.2)];

      final segments = routeSegments(points, const []);

      expect(segments.single.length, 2);
    });
  });

  group('RouteBounds.fromPoints', () {
    test('computes the tightest box over finite points', () {
      final bounds = RouteBounds.fromPoints([
        point(52.0, 13.0),
        point(52.5, 13.8),
        point(51.8, 13.4),
      ])!;

      expect(bounds.minLatitude, 51.8);
      expect(bounds.maxLatitude, 52.5);
      expect(bounds.minLongitude, 13.0);
      expect(bounds.maxLongitude, 13.8);
      expect(bounds.centerLatitude, closeTo(52.15, 1e-9));
      expect(bounds.isSinglePoint, isFalse);
    });

    test('single repeated point is flagged', () {
      final bounds = RouteBounds.fromPoints([point(52.0, 13.0)])!;
      expect(bounds.isSinglePoint, isTrue);
    });

    test('returns null when there are no finite points', () {
      expect(RouteBounds.fromPoints(const []), isNull);
      expect(
        RouteBounds.fromPoints([point(double.nan, double.infinity)]),
        isNull,
      );
    });
  });

  group('routeTotalDistanceMeters', () {
    test('sums haversine distance between consecutive points', () {
      final a = point(52.0, 13.0);
      final b = point(52.01, 13.0);
      final c = point(52.01, 13.02);

      final expected = haversineMeters(52.0, 13.0, 52.01, 13.0) +
          haversineMeters(52.01, 13.0, 52.01, 13.02);

      expect(
        routeTotalDistanceMeters([a, b, c]),
        closeTo(expected, 1e-6),
      );
    });

    test('does not bridge across a route break', () {
      final points = [
        point(52.0, 13.0),
        point(52.01, 13.0),
        point(60.0, 20.0),
        point(60.01, 20.0),
      ];

      final withBreak =
          routeTotalDistanceMeters(points, routeBreakIndexes: const [2]);
      final withoutBreak = routeTotalDistanceMeters(points);

      expect(withBreak, lessThan(withoutBreak));
      final segmentSum = haversineMeters(52.0, 13.0, 52.01, 13.0) +
          haversineMeters(60.0, 20.0, 60.01, 20.0);
      expect(withBreak, closeTo(segmentSum, 1e-6));
    });

    test('is zero for a single point', () {
      expect(routeTotalDistanceMeters([point(52.0, 13.0)]), 0.0);
    });
  });

  group('simplifyRoutePoints', () {
    test('a route below the display cap is returned unchanged, and identical',
        () {
      // Identity is what makes RouteMapView's memo work: a rebuild must not
      // produce a new list and re-project the whole track.
      final points = line(100);

      expect(identical(simplifyRoutePoints(points), points), isTrue);
    });

    test('a route longer than the display cap keeps its first and last point',
        () {
      final points = line(10000);

      final simplified = simplifyRoutePoints(points);

      expect(simplified.length, lessThanOrEqualTo(maxDisplayedRoutePoints));
      expect(simplified.first, points.first);
      expect(simplified.last, points.last);
    });

    test('decimating for the map never changes the distance the screen reports',
        () {
      // The sharp edge of this optimisation: routeDistanceMeters must keep
      // measuring the FULL track, or a long ride quietly under-reports.
      final points = line(10000);

      expect(
        routeTotalDistanceMeters(points),
        isNot(closeTo(routeTotalDistanceMeters(simplifyRoutePoints(points)), 1)),
        reason: 'the decimated track is measurably shorter, which is exactly '
            'why the display must not measure it',
      );
    });
  });

  group('buildRouteMapGeometry', () {
    test('segments are split at breaks and projected for drawing', () {
      final geometry = buildRouteMapGeometry(
        points: line(5),
        routeBreakIndexes: const [2],
      );

      // The single-point tail segment is dropped: a polyline needs two.
      expect(geometry.segments.length, 2);
      expect(geometry.segments[0].length, 2);
      expect(geometry.segments[0].first.latitude, 52.0);
    });

    test('a route with no finite points yields no bounds and no markers', () {
      final geometry = buildRouteMapGeometry(
        points: [point(double.nan, double.nan)],
      );

      expect(geometry.segments, isEmpty);
      expect(geometry.bounds, isNull);
      expect(geometry.startPoint, isNull);
      expect(geometry.endPoint, isNull);
    });

    test('the current point widens the camera bounds beyond the route', () {
      final geometry = buildRouteMapGeometry(
        points: line(3),
        currentPoint: point(60.0, 20.0),
      );

      expect(geometry.bounds!.maxLatitude, 60.0);
      expect(geometry.currentPoint!.latitude, 60.0);
    });
  });
}
