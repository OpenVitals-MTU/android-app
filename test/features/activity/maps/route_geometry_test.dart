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
}
