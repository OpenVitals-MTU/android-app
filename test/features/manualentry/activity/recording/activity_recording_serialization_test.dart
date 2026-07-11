import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_serialization.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Round-trip test for the ported `ActivityRecordingStore` /
/// `ActivityRecordingStoreSerialization` persistence.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('recording state survives a SharedPreferences round-trip', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = ActivityRecordingStore(prefs);

    final start = DateTime.utc(2026, 5, 26, 8, 30);
    final state = ActivityRecordingState(
      status: ActivityRecordingStatus.recording,
      recordingKind: ActivityRecordingKind.gpsRoute,
      activityTypeId: 'push_ups',
      exerciseType: 12,
      startTime: start,
      totalPausedMillis: 5000,
      pauseIntervals: [
        ActivityPauseInterval(
          startTime: start.add(const Duration(seconds: 60)),
          endTime: start.add(const Duration(seconds: 90)),
        ),
      ],
      points: [
        ExerciseRoutePoint(
          time: start,
          latitude: 59.0,
          longitude: 24.0,
          altitudeMeters: 10.0,
          horizontalAccuracyMeters: 5.0,
          verticalAccuracyMeters: null,
        ),
        ExerciseRoutePoint(
          time: start.add(const Duration(seconds: 60)),
          latitude: 59.001,
          longitude: 24.002,
          altitudeMeters: 18.0,
          horizontalAccuracyMeters: 4.0,
          verticalAccuracyMeters: 6.0,
        ),
      ],
      routeBreakIndexes: const [1],
      markers: [
        ActivityRecordingMarker(
          id: 'marker-1',
          time: start.add(const Duration(seconds: 30)),
          latitude: 59.0005,
          longitude: 24.001,
          altitudeMeters: 12.0,
          name: 'Water stop',
          note: 'refill',
        ),
      ],
      distanceMeters: 111.2,
      elevationGainedMeters: 8.0,
      repetitionCount: 4,
      repetitionSets: const [
        ActivityRecordedRepetitionSet(
            repetitions: 8, restSeconds: 60, activeMillis: 120000),
      ],
      lastAccuracyMeters: 4.0,
      lastLocationTime: start.add(const Duration(seconds: 60)),
    );

    await store.storeMetadata(state);
    final restored = store.restore();

    expect(restored.status, ActivityRecordingStatus.recording);
    expect(restored.activityTypeId, 'push_ups');
    expect(restored.exerciseType, 12);
    expect(restored.startTime, start);
    expect(restored.totalPausedMillis, 5000);
    expect(restored.pauseIntervals.length, 1);
    expect(restored.points.length, 2);
    expect(restored.points[1].latitude, closeTo(59.001, 1e-9));
    expect(restored.points[1].verticalAccuracyMeters, 6.0);
    expect(restored.routeBreakIndexes, [1]);
    expect(restored.markers.length, 1);
    expect(restored.markers.first.name, 'Water stop');
    expect(restored.markers.first.note, 'refill');
    expect(restored.distanceMeters, closeTo(111.2, 1e-6));
    expect(restored.elevationGainedMeters, closeTo(8.0, 1e-6));
    expect(restored.repetitionCount, 4);
    expect(restored.repetitionSets.single.restSeconds, 60);
    expect(restored.lastAccuracyMeters, 4.0);
  });

  test('idle state clears persisted recording keys', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final store = ActivityRecordingStore(prefs);

    await store.storeMetadata(const ActivityRecordingState(
      status: ActivityRecordingStatus.recording,
      activityTypeId: 'run',
    ));
    expect(store.restore().status, ActivityRecordingStatus.recording);

    await store.storeMetadata(const ActivityRecordingState());
    expect(store.restore().status, ActivityRecordingStatus.idle);
  });
}
