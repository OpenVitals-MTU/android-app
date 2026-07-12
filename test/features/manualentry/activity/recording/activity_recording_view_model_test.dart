import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/command_state.dart';
import 'package:openvitals/core/presentation/screen_error.dart';
import 'package:openvitals/domain/model/activity_entry_types.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/preferences/activity_recording_dashboard_layout.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_providers.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_view_model.dart';

/// The command lifecycle of the recording view-model — the SCREEN side of the
/// recorder. The device-bound `ActivityRecordingService` is replaced by a fake
/// here on purpose: this file pins the surface the widgets consume (elapsed
/// time, focus mode, start/pause/resume/stop/discard), never the platform.
void main() {
  final gpsType = defaultActivityEntryTypes.first;

  ActivityRecordingSnapshot recordedSnapshot({double distanceMeters = 5000}) =>
      ActivityRecordingSnapshot(
        exerciseType: gpsType.exerciseType,
        startTime: DateTime.utc(2026, 7, 12, 8),
        endTime: DateTime.utc(2026, 7, 12, 9),
        points: const <ExerciseRoutePoint>[],
        pauseIntervals: const <ActivityPauseInterval>[],
        distanceMeters: distanceMeters,
        elevationGainedMeters: 0,
      );

  ProviderContainer containerWith(_FakeRecordingService service) {
    final container = ProviderContainer(
      overrides: [
        activityRecordingControllerProvider.overrideWithValue(service),
      ],
    );
    addTearDown(container.dispose);
    return container;
  }

  group('start', () {
    test('a started recording succeeds and republishes the session', () async {
      final service = _FakeRecordingService(startResult: true);
      final container = containerWith(service);
      final viewModel =
          container.read(activityRecordingViewModelProvider.notifier);

      expect(container.read(activityRecordingViewModelProvider).start,
          isA<CommandIdle<void>>());

      await viewModel.startRecording(gpsType, null);

      final state = container.read(activityRecordingViewModelProvider);
      expect(state.start, isA<CommandSuccess<void>>());
      expect(service.startCalls, 1);
      // The service's session is mirrored onto the screen's state — the
      // view-model never invents one of its own.
      expect(state.recording.status, ActivityRecordingStatus.recording);
    });

    test('a refused start fails with the reason the service gave', () async {
      final service = _FakeRecordingService(
        startResult: false,
        startErrorMessage: 'Waiting for GPS',
      );
      final container = containerWith(service);
      final viewModel =
          container.read(activityRecordingViewModelProvider.notifier);

      await viewModel.startRecording(gpsType, null);

      final state = container.read(activityRecordingViewModelProvider);
      expect(state.start, isA<CommandFailure<void>>());
      expect(
        (state.start as CommandFailure<void>).error,
        const ScreenErrorMessage('Waiting for GPS'),
      );
      expect(state.commandError, const ScreenErrorMessage('Waiting for GPS'));
      // A refusal is not a session: nothing may look like it is recording.
      expect(state.recording.status, ActivityRecordingStatus.idle);
    });

    test('the screen consumes the failure once, then it is gone', () async {
      final service = _FakeRecordingService(
        startResult: false,
        startErrorMessage: 'Location permission required',
      );
      final container = containerWith(service);
      final viewModel =
          container.read(activityRecordingViewModelProvider.notifier);

      await viewModel.startRecording(gpsType, null);
      expect(container.read(activityRecordingViewModelProvider).start,
          isA<CommandFailure<void>>());

      viewModel.onStartHandled();

      final state = container.read(activityRecordingViewModelProvider);
      expect(state.start, isA<CommandIdle<void>>());
      expect(state.commandError, isNull);
    });

    test('a second start while one is running is refused', () async {
      final service = _FakeRecordingService(startResult: true);
      final container = containerWith(service);
      final viewModel =
          container.read(activityRecordingViewModelProvider.notifier);

      // Both are launched before either is awaited: the guard, not the await,
      // is what must stop the recorder being started twice.
      final first = viewModel.startRecording(gpsType, null);
      final second = viewModel.startRecording(gpsType, null);
      await Future.wait([first, second]);

      expect(service.startCalls, 1);
    });
  });

  group('pause / resume', () {
    test('pause and resume reach the service and republish its status', () {
      final service = _FakeRecordingService(startResult: true);
      final container = containerWith(service);
      final viewModel =
          container.read(activityRecordingViewModelProvider.notifier);
      service.emit(const ActivityRecordingState(
        status: ActivityRecordingStatus.recording,
      ));

      viewModel.pauseRecording();
      expect(service.pauseCalls, 1);
      expect(container.read(activityRecordingViewModelProvider).recording.status,
          ActivityRecordingStatus.paused);

      viewModel.resumeRecording();
      expect(service.resumeCalls, 1);
      expect(container.read(activityRecordingViewModelProvider).recording.status,
          ActivityRecordingStatus.recording);
    });
  });

  group('stop', () {
    test('stopping hands back the recorded snapshot', () {
      final snapshot = recordedSnapshot();
      final service = _FakeRecordingService(
        startResult: true,
        snapshot: snapshot,
      );
      final container = containerWith(service);
      final viewModel =
          container.read(activityRecordingViewModelProvider.notifier);
      service.emit(const ActivityRecordingState(
        status: ActivityRecordingStatus.recording,
      ));

      viewModel.stopRecording();

      final state = container.read(activityRecordingViewModelProvider);
      expect(state.save, isA<CommandSuccess<ActivityRecordingSnapshot>>());
      expect(
        (state.save as CommandSuccess<ActivityRecordingSnapshot>).value,
        snapshot,
      );
      // The recorder cleared itself; the screen must see an idle session.
      expect(state.recording.status, ActivityRecordingStatus.idle);
    });

    test('the snapshot is consumed exactly once', () {
      final service = _FakeRecordingService(
        startResult: true,
        snapshot: recordedSnapshot(),
      );
      final container = containerWith(service);
      final viewModel =
          container.read(activityRecordingViewModelProvider.notifier);

      viewModel.stopRecording();
      viewModel.onSaveHandled();

      expect(container.read(activityRecordingViewModelProvider).save,
          isA<CommandIdle<ActivityRecordingSnapshot>>());
    });

    test('stopping with nothing recording fails loudly, not silently', () {
      // The snapshot is the ONLY copy of the workout: a stop that produces
      // none must never look like it worked.
      final service = _FakeRecordingService(startResult: true);
      final container = containerWith(service);
      final viewModel =
          container.read(activityRecordingViewModelProvider.notifier);

      viewModel.stopRecording();

      final state = container.read(activityRecordingViewModelProvider);
      expect(state.save, isA<CommandFailure<ActivityRecordingSnapshot>>());
      expect(
        (state.save as CommandFailure<ActivityRecordingSnapshot>).error,
        const ScreenErrorMessage(kNoActiveRecordingMessage),
      );
    });
  });

  group('discard', () {
    test('discarding clears the session and both commands', () async {
      final service = _FakeRecordingService(
        startResult: false,
        startErrorMessage: 'Waiting for GPS',
      );
      final container = containerWith(service);
      final viewModel =
          container.read(activityRecordingViewModelProvider.notifier);

      await viewModel.startRecording(gpsType, null);
      viewModel.setFocusMode(true);
      expect(container.read(activityRecordingViewModelProvider).start,
          isA<CommandFailure<void>>());

      viewModel.discardRecording();

      final state = container.read(activityRecordingViewModelProvider);
      expect(service.discardCalls, 1);
      expect(state.start, isA<CommandIdle<void>>());
      expect(state.save, isA<CommandIdle<ActivityRecordingSnapshot>>());
      expect(state.isFocusMode, isFalse);
      expect(state.recording.status, ActivityRecordingStatus.idle);
    });
  });

  group('focus mode', () {
    test('focus mode needs a session that can actually use it', () {
      final service = _FakeRecordingService(startResult: true);
      final container = containerWith(service);
      final viewModel =
          container.read(activityRecordingViewModelProvider.notifier);

      viewModel.setFocusMode(true);
      // The flag alone is not enough: an idle recorder has nothing to focus on,
      // and the host would otherwise drop its app bar over a dead screen.
      expect(container.read(activityRecordingViewModelProvider).showFocusMode,
          isFalse);

      service.emit(const ActivityRecordingState(
        status: ActivityRecordingStatus.recording,
      ));
      expect(container.read(activityRecordingViewModelProvider).showFocusMode,
          isTrue);

      // A repetition session has no focus mode in the first place.
      service.emit(const ActivityRecordingState(
        status: ActivityRecordingStatus.recording,
        recordingKind: ActivityRecordingKind.repetition,
      ));
      expect(container.read(activityRecordingViewModelProvider).showFocusMode,
          isFalse);
    });
  });

  group('elapsed time', () {
    test('a repetition session counts its rests, a route does not', () {
      final service = _FakeRecordingService(startResult: true);
      final container = containerWith(service);
      final start = DateTime.now().toUtc().subtract(const Duration(minutes: 10));

      service.emit(ActivityRecordingState(
        status: ActivityRecordingStatus.recording,
        recordingKind: ActivityRecordingKind.gpsRoute,
        startTime: start,
      ));
      final route = container.read(activityRecordingViewModelProvider);
      expect(route.totalTime.inMinutes, 10);

      service.emit(ActivityRecordingState(
        status: ActivityRecordingStatus.recording,
        recordingKind: ActivityRecordingKind.repetition,
        startTime: start,
        accumulatedRestMillis: const Duration(minutes: 2).inMilliseconds,
      ));
      final repetition = container.read(activityRecordingViewModelProvider);
      // Moving time excludes the banked rest; the headline duration adds it
      // back, so the two must differ by exactly that rest.
      expect(repetition.totalTime - repetition.movingTime,
          const Duration(minutes: 2));
    });
  });
}

/// Stands in for the device-bound `ActivityRecordingService`: it publishes a
/// session the way the real one does (a [ValueListenable] the view-model
/// mirrors) without touching GPS, the foreground service or the draft store.
class _FakeRecordingService implements ActivityRecordingController {
  _FakeRecordingService({
    required this.startResult,
    this.startErrorMessage,
    this.snapshot,
  });

  final bool startResult;
  final String? startErrorMessage;
  final ActivityRecordingSnapshot? snapshot;

  int startCalls = 0;
  int pauseCalls = 0;
  int resumeCalls = 0;
  int discardCalls = 0;

  @override
  final ValueNotifier<ActivityRecordingState> state =
      ValueNotifier(const ActivityRecordingState());

  void emit(ActivityRecordingState next) => state.value = next;

  @override
  Future<bool> startRecording(
    ActivityEntryType activityType,
    ActivityRecordingInitialFix? initialFix, {
    int repetitionRestSeconds = 0,
  }) async {
    startCalls += 1;
    if (startResult) {
      emit(const ActivityRecordingState(
        status: ActivityRecordingStatus.recording,
      ));
    } else {
      // The real service reports why it refused on the recording state; the
      // view-model is what lifts that onto the command.
      emit(ActivityRecordingState(errorMessage: startErrorMessage));
    }
    return startResult;
  }

  @override
  void pauseRecording() {
    pauseCalls += 1;
    emit(state.value.copyWith(status: ActivityRecordingStatus.paused));
  }

  @override
  void resumeRecording() {
    resumeCalls += 1;
    emit(state.value.copyWith(status: ActivityRecordingStatus.recording));
  }

  @override
  ActivityRecordingSnapshot? finishRecording() {
    if (snapshot != null) emit(const ActivityRecordingState());
    return snapshot;
  }

  @override
  void discardRecording() {
    discardCalls += 1;
    emit(const ActivityRecordingState());
  }

  @override
  void prepareRecordingDashboard(ActivityEntryType activityType) {}
  @override
  void updateDashboardLayout(ActivityRecordingDashboardLayout layout) {}
  @override
  void clearPreparedRecording() {}
  @override
  void previewBleConnections() {}
  @override
  void stopBlePreview() {}
  @override
  void addManualLap() {}
  @override
  void addMarker() {}
  @override
  void updateMarker(ActivityRecordingMarker marker) {}
  @override
  void deleteMarker(String markerId) {}
  @override
  void adjustRepetitionCount(int delta) {}
  @override
  void endRepetitionSet() {}
  @override
  void startNextRepetitionSet() {}
}
