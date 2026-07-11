import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/preferences/activity_recording_preferences.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording.dart';

/// The live GPS accumulator: every number the recording screen shows while you
/// are moving — distance, elevation, speed, the route and its breaks, and the
/// auto-idle clock — is folded here, one fix at a time.
///
/// It used to live inside the controller's `_acceptPosition`, welded to
/// Geolocator, and so had no tests at all. These are those tests.
void main() {
  final start = DateTime.utc(2026, 1, 1, 10);

  // The accumulator measures geodesically on the WGS84 ellipsoid, where a
  // milli-degree of latitude at the equator is the meridian arc — 110.574 m, not
  // the 111.19 m "mean degree" you get from a sphere.
  const metersPerMilliDegreeLat = 110.574;

  ExerciseRoutePoint point({
    required int seconds,
    double latMilliDegrees = 0.0,
    double? altitudeMeters,
    double? accuracyMeters = 5.0,
  }) =>
      ExerciseRoutePoint(
        time: start.add(Duration(seconds: seconds)),
        latitude: latMilliDegrees * 0.001,
        longitude: 0.0,
        altitudeMeters: altitudeMeters,
        horizontalAccuracyMeters: accuracyMeters,
        verticalAccuracyMeters: null,
      );

  /// Distance and time gates opened up, so a test only fights the gate it means
  /// to. `routeGapMeters: null` means "never break the route".
  const preferences = ActivityRecordingPreferences(
    recordingDistanceIntervalMeters: 5,
    recordingTimeIntervalMillis: 0,
    routeGapMeters: null,
    autoIdleEnabled: false,
  );

  ActivityRecordingState recording({
    List<ExerciseRoutePoint> points = const [],
    bool autoIdleEnabled = false,
    int autoIdleTimeoutMillis = 10000,
    DateTime? lastMovementAt,
    double distanceMeters = 0.0,
    double maxSpeedMetersPerSecond = 0.0,
  }) =>
      ActivityRecordingState(
        status: ActivityRecordingStatus.recording,
        recordingKind: ActivityRecordingKind.gpsRoute,
        startTime: start,
        points: points,
        distanceMeters: distanceMeters,
        maxSpeedMetersPerSecond: maxSpeedMetersPerSecond,
        autoIdleEnabled: autoIdleEnabled,
        autoIdleTimeoutMillis: autoIdleTimeoutMillis,
        lastMovementAt: lastMovementAt,
      );

  ActivityRecordingState accept(
    ActivityRecordingState state,
    ExerciseRoutePoint p, {
    double accuracyMeters = 5.0,
    ActivityRecordingPreferences prefs = preferences,
  }) =>
      withAcceptedLocation(
        state,
        point: p,
        accuracyMeters: accuracyMeters,
        preferences: prefs,
      );

  test('the first fix opens the route but banks no distance or speed', () {
    final result = accept(recording(), point(seconds: 0));

    expect(result.points, hasLength(1));
    expect(result.distanceMeters, 0.0);
    expect(result.currentSpeedMetersPerSecond, 0.0);
    expect(result.gpsStatus, ActivityGpsStatus.fix);
    // There is no leg yet, so there is nothing to be the max of.
    expect(result.maxSpeedMetersPerSecond, 0.0);
  });

  test('a second fix accumulates distance, climb and speed', () {
    final first = accept(recording(), point(seconds: 0, altitudeMeters: 100.0));
    final result = accept(
      first,
      point(seconds: 10, latMilliDegrees: 1.0, altitudeMeters: 110.0),
    );

    expect(result.points, hasLength(2));
    expect(result.distanceMeters, closeTo(metersPerMilliDegreeLat, 0.5));
    expect(result.elevationGainedMeters, closeTo(10.0, 0.001));
    expect(result.elevationLostMeters, 0.0);
    // ~110.6 m in 10 s.
    expect(result.currentSpeedMetersPerSecond, closeTo(11.06, 0.05));
    expect(result.maxSpeedMetersPerSecond, closeTo(11.06, 0.05));
  });

  test('a descent accumulates loss, not gain', () {
    final first = accept(recording(), point(seconds: 0, altitudeMeters: 100.0));
    final result = accept(
      first,
      point(seconds: 10, latMilliDegrees: 1.0, altitudeMeters: 92.0),
    );

    expect(result.elevationGainedMeters, 0.0);
    expect(result.elevationLostMeters, closeTo(8.0, 0.001));
  });

  test('max speed is a high-water mark, not the latest speed', () {
    final first = accept(recording(), point(seconds: 0));
    final fast = accept(first, point(seconds: 10, latMilliDegrees: 1.0));
    // Half the ground in the same time: slower, but the peak must survive.
    final slow = accept(fast, point(seconds: 20, latMilliDegrees: 1.5));

    expect(slow.currentSpeedMetersPerSecond,
        lessThan(fast.currentSpeedMetersPerSecond));
    expect(slow.maxSpeedMetersPerSecond,
        closeTo(fast.maxSpeedMetersPerSecond, 0.001));
  });

  test('a fix that does not advance the clock is dropped', () {
    final first = accept(recording(), point(seconds: 10, latMilliDegrees: 1.0));
    final result = accept(first, point(seconds: 10, latMilliDegrees: 2.0));

    expect(result.points, hasLength(1), reason: 'route must not grow');
    expect(result.distanceMeters, 0.0);
    expect(result.droppedPointCount, 1);
  });

  test('a fix inside the minimum sample distance is shown but not banked', () {
    final first = accept(recording(), point(seconds: 0));
    // 0.01 milli-degrees ≈ 1.1 m, under the 5 m interval.
    final near = point(seconds: 10, latMilliDegrees: 0.01);
    final result = accept(first, near);

    expect(result.points, hasLength(1), reason: 'route must not grow');
    expect(result.distanceMeters, 0.0);
    // ...but the live marker still follows the fix, and it is not a "drop".
    expect(result.latestUiPoint, near);
    expect(result.droppedPointCount, 0);
    expect(result.lastLocationTime, near.time);
  });

  test('a gap wider than routeGapMeters breaks the route and banks no distance',
      () {
    const gapped = ActivityRecordingPreferences(
      recordingDistanceIntervalMeters: 5,
      recordingTimeIntervalMillis: 0,
      routeGapMeters: 50,
      autoIdleEnabled: false,
    );
    final first = accept(recording(), point(seconds: 0), prefs: gapped);
    // ~111 m, well past the 50 m gap.
    final result = accept(
      first,
      point(seconds: 600, latMilliDegrees: 1.0),
      prefs: gapped,
    );

    expect(result.points, hasLength(2), reason: 'the point still joins the route');
    expect(result.routeBreakIndexes, [1], reason: 'the line breaks before it');
    // The whole point of the break: a tunnel is not 111 m of running.
    expect(result.distanceMeters, 0.0);
    expect(result.currentSpeedMetersPerSecond, 0.0);
  });

  test('an implausible jump is dropped rather than banked', () {
    final first = accept(recording(), point(seconds: 0));
    // ~111 m in 1 s = 111 m/s, far past any plausible speed, and far past the
    // combined accuracy of the two fixes.
    final result = accept(first, point(seconds: 1, latMilliDegrees: 1.0));

    expect(result.points, hasLength(1));
    expect(result.distanceMeters, 0.0);
    expect(result.droppedPointCount, 1);
  });

  test('auto-idle charges only the stretch beyond the timeout', () {
    final first = accept(
      recording(autoIdleEnabled: true, lastMovementAt: start),
      point(seconds: 0),
      prefs: preferences.copyWith(autoIdleEnabled: true),
    );
    // Moves again 30 s later, with a 10 s idle timeout: 20 s of that was idle,
    // not 30.
    final result = accept(
      first,
      point(seconds: 30, latMilliDegrees: 1.0),
      prefs: preferences.copyWith(autoIdleEnabled: true),
    );

    expect(result.totalIdleMillis, 20000);
    expect(result.lastMovementAt, start.add(const Duration(seconds: 30)));
  });

  test('moving again inside the timeout accrues no idle at all', () {
    final first = accept(
      recording(autoIdleEnabled: true, lastMovementAt: start),
      point(seconds: 0),
      prefs: preferences.copyWith(autoIdleEnabled: true),
    );
    final result = accept(
      first,
      point(seconds: 8, latMilliDegrees: 1.0),
      prefs: preferences.copyWith(autoIdleEnabled: true),
    );

    expect(result.totalIdleMillis, 0);
  });

  test('a route break does not stop the auto-idle clock', () {
    // Regression guard: the break path must leave lastMovementAt alone, so the
    // stationary stretch that CAUSED the gap is still charged as idle when the
    // next real leg lands.
    const gapped = ActivityRecordingPreferences(
      recordingDistanceIntervalMeters: 5,
      recordingTimeIntervalMillis: 0,
      routeGapMeters: 50,
      autoIdleEnabled: true,
    );
    final first = accept(
      recording(autoIdleEnabled: true, lastMovementAt: start),
      point(seconds: 0),
      prefs: gapped,
    );
    final broken =
        accept(first, point(seconds: 600, latMilliDegrees: 1.0), prefs: gapped);

    expect(broken.routeBreakIndexes, [1]);
    expect(broken.totalIdleMillis, 0, reason: 'the break banks nothing itself');
    expect(broken.lastMovementAt, start, reason: 'the idle clock keeps running');
  });
}
