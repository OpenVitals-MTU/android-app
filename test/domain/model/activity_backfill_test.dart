import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/activity_backfill.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/heart_models.dart';

final DateTime _epoch = DateTime.fromMillisecondsSinceEpoch(0, isUtc: true);

ExerciseData _workout({
  double? totalDistanceMeters,
  double? elevationGainedMeters,
  int? averageHeartRateBpm,
  double? averageSpeedMetersPerSecond,
  double? averageStepsCadenceRate,
  double? averageCyclingCadenceRpm,
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
}
