import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording.dart';

/// Port of the Kotlin `ActivityRecordingStateTest`.
void main() {
  final start = DateTime.utc(2026, 1, 1, 10);

  test('movingDuration excludes open auto idle time', () {
    final state = ActivityRecordingState(
      status: ActivityRecordingStatus.recording,
      startTime: start,
      autoIdleEnabled: true,
      autoIdleTimeoutMillis: 10000,
      lastMovementAt: start,
    );

    expect(
      state.movingDuration(start.add(const Duration(seconds: 30))),
      const Duration(seconds: 10),
    );
  });

  test('movingDuration excludes manual pauses and auto idle', () {
    final state = ActivityRecordingState(
      status: ActivityRecordingStatus.paused,
      startTime: start,
      pausedStartedAt: start.add(const Duration(seconds: 50)),
      totalPausedMillis: 5000,
      autoIdleEnabled: true,
      autoIdleTimeoutMillis: 10000,
      lastMovementAt: start.add(const Duration(seconds: 20)),
      totalIdleMillis: 20000,
    );

    expect(
      state.movingDuration(start.add(const Duration(seconds: 60))),
      const Duration(seconds: 15),
    );
  });

  test('repetition movingDuration excludes recorded and open rest time', () {
    final now = start.add(const Duration(seconds: 90));
    final state = ActivityRecordingState(
      status: ActivityRecordingStatus.resting,
      recordingKind: ActivityRecordingKind.repetition,
      startTime: start,
      accumulatedRestMillis: 20000,
      restStartedAt: start.add(const Duration(seconds: 70)),
      repetitionRestSeconds: 30,
    );

    expect(state.restDuration(now), const Duration(seconds: 40));
    expect(state.movingDuration(now), const Duration(seconds: 50));
    expect(
      state.movingDuration(now) + state.restDuration(now),
      const Duration(seconds: 90),
    );
  });

  test('effective speed is zero while idle or gps is poor', () {
    final idleState = ActivityRecordingState(
      status: ActivityRecordingStatus.recording,
      startTime: start,
      currentSpeedMetersPerSecond: 6.0,
      autoIdleEnabled: true,
      autoIdleTimeoutMillis: 10000,
      lastMovementAt: start,
      gpsStatus: ActivityGpsStatus.fix,
    );
    final poorGpsState = idleState.copyWith(
      lastMovementAt: start.add(const Duration(seconds: 20)),
      gpsStatus: ActivityGpsStatus.poorAccuracy,
    );

    expect(
      idleState.effectiveCurrentSpeedMetersPerSecond(
          start.add(const Duration(seconds: 20))),
      0.0,
    );
    expect(
      poorGpsState.effectiveCurrentSpeedMetersPerSecond(
          start.add(const Duration(seconds: 21))),
      0.0,
    );
    expect(
      idleState.effectiveCurrentSpeedMetersPerSecond(
          start.add(const Duration(seconds: 5))),
      6.0,
    );
  });
}
