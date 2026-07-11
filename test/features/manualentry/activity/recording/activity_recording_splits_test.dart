import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_splits.dart';

/// Port of the Kotlin `ActivityRecordingSplitsTest`.
void main() {
  final start = DateTime.utc(2026, 1, 1, 10);

  ExerciseRoutePoint point(int seconds, double latitude, [double? altitude]) =>
      ExerciseRoutePoint(
        time: start.add(Duration(seconds: seconds)),
        latitude: latitude,
        longitude: 0.0,
        altitudeMeters: altitude,
        horizontalAccuracyMeters: 5.0,
        verticalAccuracyMeters: null,
      );

  test('interval splits are empty for empty and single point routes', () {
    expect(activityRecordingIntervalSplits(const [], const []), isEmpty);
    expect(
      activityRecordingIntervalSplits([point(0, 0.0)], const []),
      isEmpty,
    );
  });

  test('interval splits use route breaks and do not count gap distance', () {
    final points = [
      point(0, 0.0),
      point(60, 0.001),
      point(120, 0.100),
      point(180, 0.101),
    ];

    final splits = activityRecordingIntervalSplits(points, const [2]);

    expect(splits.length, 2);
    expect(splits[0].distanceMeters, closeTo(111.2, 1.0));
    expect(splits[1].distanceMeters, closeTo(111.2, 1.0));
    expect(splits.fold<double>(0, (a, s) => a + s.distanceMeters), lessThan(250.0));
  });

  test('time splits include active incomplete split', () {
    final points = [
      point(0, 0.0, 0.0),
      point(60, 0.001, 2.0),
      point(120, 0.002, 4.0),
      point(180, 0.003, 4.0),
    ];

    final splits = activityRecordingTimeSplits(
      points: points,
      routeBreakIndexes: const [],
      splitMillis: 120000,
    );

    expect(splits.length, 2);
    expect(splits[0].elapsedMillis, 120000);
    expect(splits[1].elapsedMillis, 60000);
    expect(splits[0].climbMeters, closeTo(4.0, 0.1));
  });

  test('time splits do not calculate across route breaks', () {
    final points = [
      point(0, 0.0),
      point(60, 0.001),
      point(120, 0.100),
      point(180, 0.101),
    ];

    final splits = activityRecordingTimeSplits(
      points: points,
      routeBreakIndexes: const [2],
      splitMillis: 3600000,
    );

    expect(splits.length, 1);
    expect(splits.single.distanceMeters, lessThan(250.0));
  });

  test('distance splits create fixed distance buckets with active remainder', () {
    final points = [
      point(0, 0.0),
      point(60, 0.001),
      point(120, 0.002),
    ];

    final splits = activityRecordingDistanceSplits(
      points: points,
      routeBreakIndexes: const [],
      splitMeters: 100.0,
    );

    expect(splits.length, 3);
    expect(splits[0].distanceMeters, closeTo(100.0, 0.1));
    expect(splits[1].distanceMeters, closeTo(100.0, 0.1));
    expect(splits[2].distanceMeters, inInclusiveRange(20.0, 25.0));
  });

  test('split max speed is calculated per bucket', () {
    final points = [
      point(0, 0.0),
      point(100, 0.001),
      point(110, 0.002),
    ];

    final splits = activityRecordingTimeSplits(
      points: points,
      routeBreakIndexes: const [],
      splitMillis: 120000,
    );

    expect(splits.length, 1);
    expect(
      splits.single.maxSpeedMetersPerSecond,
      greaterThan(splits.single.averageSpeedMetersPerSecond),
    );
  });

  test('manual lap splits do not count route break gaps', () {
    final points = [
      point(0, 0.0),
      point(60, 0.001),
      point(120, 0.100),
      point(180, 0.101),
    ];

    final splits = activityRecordingLapSplits(
      laps: [
        ActivityRecordingLap(
          startTime: start,
          endTime: start.add(const Duration(seconds: 180)),
          distanceMeters: null,
        ),
      ],
      points: points,
      routeBreakIndexes: const [2],
      recordingStartTime: start,
    );

    expect(splits.length, 1);
    expect(splits.single.distanceMeters, lessThan(250.0));
  });

  test('route distance helper avoids route break gaps', () {
    final points = [
      point(0, 0.0),
      point(60, 0.001),
      point(120, 0.100),
      point(180, 0.101),
    ];

    final distance = activityRecordingRouteDistanceMeters(
      points: points,
      routeBreakIndexes: const [2],
      startTime: start,
      endTime: start.add(const Duration(seconds: 180)),
    );

    expect(distance, lessThan(250.0));
  });
}
