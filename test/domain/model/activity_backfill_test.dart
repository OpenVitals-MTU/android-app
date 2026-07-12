import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/activity_backfill.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/exercise_session_metrics.dart';
import 'package:openvitals/domain/model/heart_models.dart';

final DateTime _epoch = DateTime.fromMillisecondsSinceEpoch(0, isUtc: true);

ExerciseData _workout({
  double? totalDistanceMeters,
  double? elevationGainedMeters,
  int? averageHeartRateBpm,
  double? averageSpeedMetersPerSecond,
  double? averageStepsCadenceRate,
  double? averageCyclingCadenceRpm,
  int? steps,
  double? totalCaloriesKcal,
  double? activeCaloriesKcal,
  int? floorsClimbed,
  int? wheelchairPushes,
  ExerciseRouteData route = const ExerciseRouteData(),
}) =>
    ExerciseData(
      id: 'activity-1',
      title: 'Morning run',
      exerciseType: 56,
      startTime: _epoch,
      endTime: _epoch.add(const Duration(seconds: 3600)),
      durationMs: 3600000,
      source: 'test',
      totalDistanceMeters: totalDistanceMeters,
      elevationGainedMeters: elevationGainedMeters,
      averageHeartRateBpm: averageHeartRateBpm,
      averageSpeedMetersPerSecond: averageSpeedMetersPerSecond,
      averageStepsCadenceRate: averageStepsCadenceRate,
      averageCyclingCadenceRpm: averageCyclingCadenceRpm,
      steps: steps,
      totalCaloriesKcal: totalCaloriesKcal,
      activeCaloriesKcal: activeCaloriesKcal,
      floorsClimbed: floorsClimbed,
      wheelchairPushes: wheelchairPushes,
      route: route,
    );

ExerciseRoutePoint _routePoint({
  required int seconds,
  required double latitude,
  required double longitude,
  double? altitudeMeters,
}) =>
    ExerciseRoutePoint(
      time: _epoch.add(Duration(seconds: seconds)),
      latitude: latitude,
      longitude: longitude,
      altitudeMeters: altitudeMeters,
      horizontalAccuracyMeters: null,
      verticalAccuracyMeters: null,
    );

void main() {
  test('route backfill fills missing distance and elevation', () {
    final workout = _workout(
      route: ExerciseRouteData(
        status: ExerciseRouteStatus.data,
        points: [
          _routePoint(
              seconds: 0, latitude: 0.0, longitude: 0.0, altitudeMeters: 10.0),
          _routePoint(
              seconds: 60, latitude: 0.0, longitude: 0.01, altitudeMeters: 12.0),
          _routePoint(
              seconds: 120,
              latitude: 0.01,
              longitude: 0.01,
              altitudeMeters: 11.0),
          _routePoint(
              seconds: 180,
              latitude: 0.01,
              longitude: 0.02,
              altitudeMeters: 14.5),
        ],
      ),
    );

    final result = workout.withRouteBackfilledMetrics();

    expect(result.totalDistanceMeters ?? 0.0, closeTo(3335.85, 0.1));
    expect(result.elevationGainedMeters ?? 0.0, closeTo(5.5, 0.001));
  });

  test('route backfill replaces empty zero summaries with route values', () {
    final workout = _workout(
      totalDistanceMeters: 0.0,
      elevationGainedMeters: 0.0,
      route: ExerciseRouteData(
        status: ExerciseRouteStatus.data,
        points: [
          _routePoint(
              seconds: 0, latitude: 0.0, longitude: 0.0, altitudeMeters: 10.0),
          _routePoint(
              seconds: 60, latitude: 0.0, longitude: 0.01, altitudeMeters: 13.0),
        ],
      ),
    );

    final result = workout.withRouteBackfilledMetrics();

    expect(result.totalDistanceMeters ?? 0.0, closeTo(1111.95, 0.1));
    expect(result.elevationGainedMeters ?? 0.0, closeTo(3.0, 0.001));
  });

  test('route backfill preserves recorded summaries', () {
    final workout = _workout(
      totalDistanceMeters: 500.0,
      elevationGainedMeters: 20.0,
      route: ExerciseRouteData(
        status: ExerciseRouteStatus.data,
        points: [
          _routePoint(
              seconds: 0, latitude: 0.0, longitude: 0.0, altitudeMeters: 10.0),
          _routePoint(
              seconds: 60, latitude: 0.0, longitude: 0.01, altitudeMeters: 13.0),
        ],
      ),
    );

    final result = workout.withRouteBackfilledMetrics();

    expect(result.totalDistanceMeters ?? 0.0, closeTo(500.0, 0.001));
    expect(result.elevationGainedMeters ?? 0.0, closeTo(20.0, 0.001));
  });

  test('route backfill leaves elevation missing without altitude data', () {
    final workout = _workout(
      route: ExerciseRouteData(
        status: ExerciseRouteStatus.data,
        points: [
          _routePoint(seconds: 0, latitude: 0.0, longitude: 0.0),
          _routePoint(seconds: 60, latitude: 0.0, longitude: 0.01),
        ],
      ),
    );

    final result = workout.withRouteBackfilledMetrics();

    expect(result.totalDistanceMeters ?? 0.0, closeTo(1111.95, 0.1));
    expect(result.elevationGainedMeters, isNull);
  });

  test('sample backfill fills missing averages', () {
    final workout = _workout();

    final result = workout.withSampleBackfilledMetrics(
      heartRateSamples: [
        HeartRateSample(time: _epoch, beatsPerMinute: 100, source: 'test'),
        HeartRateSample(
            time: _epoch.add(const Duration(seconds: 1)),
            beatsPerMinute: 110,
            source: 'test'),
      ],
      speedSamples: [
        SpeedSample(time: _epoch, metersPerSecond: 2.0, source: 'test'),
        SpeedSample(
            time: _epoch.add(const Duration(seconds: 1)),
            metersPerSecond: 4.0,
            source: 'test'),
      ],
      cadenceSamples: [
        ActivityCadenceSample(
            time: _epoch,
            rate: 160.0,
            kind: ActivityCadenceKind.steps,
            source: 'test'),
        ActivityCadenceSample(
            time: _epoch.add(const Duration(seconds: 1)),
            rate: 180.0,
            kind: ActivityCadenceKind.steps,
            source: 'test'),
        ActivityCadenceSample(
            time: _epoch,
            rate: 80.0,
            kind: ActivityCadenceKind.cycling,
            source: 'test'),
        ActivityCadenceSample(
            time: _epoch.add(const Duration(seconds: 1)),
            rate: 100.0,
            kind: ActivityCadenceKind.cycling,
            source: 'test'),
      ],
    );

    expect(result.averageHeartRateBpm, 105);
    expect(result.averageSpeedMetersPerSecond ?? 0.0, closeTo(3.0, 0.001));
    expect(result.averageStepsCadenceRate ?? 0.0, closeTo(170.0, 0.001));
    expect(result.averageCyclingCadenceRpm ?? 0.0, closeTo(90.0, 0.001));
  });

  test('sample backfill preserves recorded averages', () {
    final workout = _workout(
      averageHeartRateBpm: 130,
      averageSpeedMetersPerSecond: 5.0,
      averageStepsCadenceRate: 190.0,
      averageCyclingCadenceRpm: 95.0,
    );

    final result = workout.withSampleBackfilledMetrics(
      heartRateSamples: [
        HeartRateSample(time: _epoch, beatsPerMinute: 100, source: 'test'),
      ],
      speedSamples: [
        SpeedSample(time: _epoch, metersPerSecond: 2.0, source: 'test'),
      ],
      cadenceSamples: [
        ActivityCadenceSample(
            time: _epoch,
            rate: 160.0,
            kind: ActivityCadenceKind.steps,
            source: 'test'),
        ActivityCadenceSample(
            time: _epoch,
            rate: 80.0,
            kind: ActivityCadenceKind.cycling,
            source: 'test'),
      ],
    );

    expect(result.averageHeartRateBpm, 130);
    expect(result.averageSpeedMetersPerSecond ?? 0.0, closeTo(5.0, 0.001));
    expect(result.averageStepsCadenceRate ?? 0.0, closeTo(190.0, 0.001));
    expect(result.averageCyclingCadenceRpm ?? 0.0, closeTo(95.0, 0.001));
  });

  // The watch bug: the session record carries a duration and nothing else,
  // because the walk's steps/distance/calories/elevation were written as
  // separate records over the same window.
  test('session-metrics backfill fills the totals the session never carried',
      () {
    final workout = _workout();

    final result = workout.withSessionMetricsBackfilled(
      const ExerciseSessionMetrics(
        totalDistanceMeters: 4500.0,
        averageSpeedMetersPerSecond: 1.25,
        steps: 6200,
        totalCaloriesKcal: 320.0,
        activeCaloriesKcal: 210.0,
        elevationGainedMeters: 48.0,
        floorsClimbed: 3,
        wheelchairPushes: 0,
        averagePowerWatts: 214.5,
      ),
    );

    expect(result.totalDistanceMeters ?? 0.0, closeTo(4500.0, 0.001));
    expect(result.averageSpeedMetersPerSecond ?? 0.0, closeTo(1.25, 0.001));
    expect(result.steps, 6200);
    expect(result.totalCaloriesKcal ?? 0.0, closeTo(320.0, 0.001));
    expect(result.activeCaloriesKcal ?? 0.0, closeTo(210.0, 0.001));
    expect(result.elevationGainedMeters ?? 0.0, closeTo(48.0, 0.001));
    expect(result.floorsClimbed, 3);
    // Power reaches the workout at all. It never used to: nothing read it, so
    // the "Average power" row — which only earns its place by HAVING a value —
    // simply never appeared, on any ride, for anyone with a power meter.
    expect(result.averagePowerWatts ?? 0.0, closeTo(214.5, 0.001));
    // A zero total is an empty summary, not a measurement — the same rule the
    // route backfill applies. Nobody wants "Wheelchair pushes: 0" on a walk.
    expect(result.wheelchairPushes, isNull);
  });

  test('session-metrics backfill preserves what the session did record', () {
    final workout = _workout(
      totalDistanceMeters: 5000.0,
      steps: 7000,
      totalCaloriesKcal: 400.0,
      elevationGainedMeters: 60.0,
    );

    final result = workout.withSessionMetricsBackfilled(
      const ExerciseSessionMetrics(
        totalDistanceMeters: 4500.0,
        steps: 6200,
        totalCaloriesKcal: 320.0,
        elevationGainedMeters: 48.0,
        activeCaloriesKcal: 210.0,
      ),
    );

    expect(result.totalDistanceMeters ?? 0.0, closeTo(5000.0, 0.001));
    expect(result.steps, 7000);
    expect(result.totalCaloriesKcal ?? 0.0, closeTo(400.0, 0.001));
    expect(result.elevationGainedMeters ?? 0.0, closeTo(60.0, 0.001));
    // ...and still fills the one it did not.
    expect(result.activeCaloriesKcal ?? 0.0, closeTo(210.0, 0.001));
  });

  test('an ungranted or unrecorded metric stays missing, never zero', () {
    final result = _workout().withSessionMetricsBackfilled(
      ExerciseSessionMetrics.none,
    );

    expect(result.totalDistanceMeters, isNull);
    expect(result.steps, isNull);
    expect(result.totalCaloriesKcal, isNull);
    expect(result.elevationGainedMeters, isNull);
  });

  // The treadmill/watch case: speed is recorded but no DistanceRecord is ever
  // written, so the session has no distance at all — while the splits card,
  // integrating these very samples, cheerfully reports "every 1 km".
  test('sample backfill integrates a distance from speed when none was written',
      () {
    final workout = _workout();

    final result = workout.withSampleBackfilledMetrics(
      heartRateSamples: const [],
      speedSamples: [
        for (var i = 0; i <= 600; i++)
          SpeedSample(
            time: _epoch.add(Duration(seconds: i)),
            metersPerSecond: 2.0,
            source: 'watch',
          ),
      ],
      cadenceSamples: const [],
    );

    // 2 m/s held for 600 s.
    expect(result.totalDistanceMeters ?? 0.0, closeTo(1200.0, 1.0));
  });

  test('a recorded distance beats one integrated from speed', () {
    final workout = _workout(totalDistanceMeters: 1000.0);

    final result = workout.withSampleBackfilledMetrics(
      heartRateSamples: const [],
      speedSamples: [
        for (var i = 0; i <= 600; i++)
          SpeedSample(
            time: _epoch.add(Duration(seconds: i)),
            metersPerSecond: 2.0,
            source: 'watch',
          ),
      ],
      cadenceSamples: const [],
    );

    expect(result.totalDistanceMeters ?? 0.0, closeTo(1000.0, 0.001));
  });
}
