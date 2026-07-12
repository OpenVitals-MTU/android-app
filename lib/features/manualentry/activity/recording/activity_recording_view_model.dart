import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../../core/presentation/command_state.dart';
import '../../../../core/presentation/screen_error.dart';
import '../../../../domain/model/activity_entry_types.dart';
import '../activity_entry_providers.dart';
import 'activity_recording.dart';
import 'activity_recording_focus_mode.dart';

part 'activity_recording_view_model.freezed.dart';

/// Shown when [ActivityRecordingController.finishRecording] finds nothing to
/// finish. Same text the activity-entry view-model has always used for that
/// case, because it is the same failure.
const String kNoActiveRecordingMessage =
    'No active activity recording was found.';

/// The SCREEN's view of a live recording.
///
/// [recording] is republished from the device-bound `ActivityRecordingService`
/// — the view-model never mutates it, it only mirrors it — while [now],
/// [isFocusMode] and the two commands are surface the screen owns and the
/// platform knows nothing about.
@freezed
abstract class ActivityRecordingUiState with _$ActivityRecordingUiState {
  const ActivityRecordingUiState._();

  const factory ActivityRecordingUiState({
    /// The last state published by the recording service.
    @Default(ActivityRecordingState()) ActivityRecordingState recording,

    /// The clock the elapsed/moving durations are read against. Ticks once a
    /// second while the session is active, and is otherwise frozen — a widget
    /// reading `DateTime.now()` itself would rebuild against a clock the state
    /// has not seen.
    DateTime? now,

    /// Focus mode is a property of the SCREEN, not of the recording: it must
    /// survive a rebuild but not a process death, and the host drops its app
    /// bar for it.
    @Default(false) bool isFocusMode,

    /// Starting a recording is failable — permissions, a missing GPS fix, an
    /// unsupported activity type — and the failure is the user's to see.
    @Default(CommandState<void>.idle()) CommandState<void> start,

    /// Stopping a recording hands back the snapshot that becomes the entry
    /// form's draft. A stop with no active session fails rather than silently
    /// producing nothing.
    @Default(CommandState<ActivityRecordingSnapshot>.idle())
    CommandState<ActivityRecordingSnapshot> save,
  }) = _ActivityRecordingUiState;

  /// The wall clock the durations are computed against, falling back to the
  /// real one before the first tick.
  DateTime get _clock => now ?? DateTime.now();

  /// Time spent actually moving — pauses removed.
  Duration get movingTime => recording.movingDuration(_clock);

  /// The headline duration. A repetition session counts its rests as part of
  /// the workout; everything else shows wall-clock elapsed time.
  Duration get totalTime => recording.recordingKind ==
          ActivityRecordingKind.repetition
      ? movingTime + recording.restDuration(_clock)
      : recording.elapsedDuration(_clock);

  /// Focus mode only exists for sessions that can use it, so the flag alone is
  /// never enough to render it. The host and the recording screen both gate on
  /// THIS, so they cannot drift into disagreeing about whether the app bar is
  /// showing.
  bool get showFocusMode => isFocusMode && canUseRecordingFocusMode(recording);

  bool get isStarting => start is CommandRunning<void>;

  bool get isSaving => save is CommandRunning<ActivityRecordingSnapshot>;

  /// The error a failed command left behind, if any. The recording's own
  /// [ActivityRecordingState.errorMessage] (a GPS glitch, a missing sensor) is
  /// a live-session warning, not a command failure, and stays where it is.
  ScreenError? get commandError => switch (start) {
        CommandFailure<void>(:final error) => error,
        _ => switch (save) {
            CommandFailure<ActivityRecordingSnapshot>(:final error) => error,
            _ => null,
          },
      };
}

/// The SCREEN side of activity recording.
///
/// Everything that touches the device — the foreground service, the
/// notification buttons, GPS/BLE/motion streams, the draft store — lives in
/// `ActivityRecordingService` (see `activity_recording_service.dart`) and is
/// deliberately NOT reachable from here except through the
/// [ActivityRecordingController] interface. This view-model subscribes to that
/// service, republishes what it emits, and adds the three things only a screen
/// has: a one-second clock, focus mode, and the command lifecycles for the two
/// actions that can fail (start and save).
class ActivityRecordingViewModel extends Notifier<ActivityRecordingUiState> {
  ActivityRecordingController get _service =>
      ref.read(activityRecordingControllerProvider);

  Timer? _ticker;

  @override
  ActivityRecordingUiState build() {
    final service = _service;

    void listener() {
      final recording = service.state.value;
      state = state.copyWith(recording: recording, now: DateTime.now());
      _syncTicker(recording);
    }

    service.state.addListener(listener);
    ref.onDispose(() {
      service.state.removeListener(listener);
      _ticker?.cancel();
      _ticker = null;
    });

    final restored = service.state.value;
    // A recording restored from disk after process death is already running by
    // the time the screen asks for it, so its clock has to be ticking already.
    _syncTicker(restored);
    return ActivityRecordingUiState(
      recording: restored,
      now: DateTime.now(),
    );
  }

  /// The clock only runs while there is something to count. Mirrors the
  /// `LaunchedEffect(state.status)` the Kotlin screen ticked on.
  void _syncTicker(ActivityRecordingState recording) {
    final shouldTick = recording.isActive;
    if (shouldTick && _ticker == null) {
      _ticker = Timer.periodic(const Duration(seconds: 1), (_) {
        if (!ref.mounted) return;
        state = state.copyWith(now: DateTime.now());
      });
    } else if (!shouldTick && _ticker != null) {
      _ticker!.cancel();
      _ticker = null;
    }
  }

  // ── Commands ─────────────────────────────────────────────────────────────

  /// Starts a session. The service decides whether it can (permissions, fix
  /// quality, activity type) and puts its reason on the recording state; this
  /// lifts that reason onto the command so the screen shows it once and clears
  /// it, instead of leaving it smeared across the form.
  Future<void> startRecording(
    ActivityEntryType activityType,
    ActivityRecordingInitialFix? initialFix, {
    int repetitionRestSeconds = 0,
  }) async {
    if (state.isStarting) return;
    final service = _service;
    state = state.copyWith(start: const CommandState.running());
    final started = await service.startRecording(
      activityType,
      initialFix,
      repetitionRestSeconds: repetitionRestSeconds,
    );
    if (!ref.mounted) return;
    final recording = service.state.value;
    state = state.copyWith(
      recording: recording,
      start: started
          ? const CommandState.success(null)
          : CommandState.failure(
              ScreenErrorMessage(
                recording.errorMessage ?? kNoActiveRecordingMessage,
              ),
            ),
    );
    _syncTicker(recording);
  }

  void pauseRecording() => _service.pauseRecording();

  void resumeRecording() => _service.resumeRecording();

  /// Stops the session and hands the snapshot to whoever is listening — it is
  /// the ONLY copy of the workout that was just recorded, so a failure to
  /// produce one is a failure the user must see, never a silent no-op.
  void stopRecording() {
    final service = _service;
    state = state.copyWith(save: const CommandState.running());
    final snapshot = service.finishRecording();
    final recording = service.state.value;
    state = state.copyWith(
      recording: recording,
      isFocusMode: snapshot == null ? state.isFocusMode : false,
      save: snapshot == null
          ? const CommandState.failure(
              ScreenErrorMessage(kNoActiveRecordingMessage),
            )
          : CommandState.success(snapshot),
    );
    _syncTicker(recording);
  }

  /// Throws the session away. Both commands go back to rest: there is nothing
  /// left for the screen to report the outcome of.
  void discardRecording() {
    final service = _service;
    service.discardRecording();
    final recording = service.state.value;
    state = state.copyWith(
      recording: recording,
      isFocusMode: false,
      start: const CommandState.idle(),
      save: const CommandState.idle(),
    );
    _syncTicker(recording);
  }

  // ── Screen-owned surface ─────────────────────────────────────────────────

  void setFocusMode(bool enabled) {
    if (enabled == state.isFocusMode) return;
    state = state.copyWith(isFocusMode: enabled);
  }

  /// The screen consumed the start outcome, so the command returns to rest —
  /// otherwise re-entering the route would replay a stale failure.
  void onStartHandled() {
    state = state.copyWith(start: const CommandState.idle());
  }

  /// The screen took the snapshot (it is now the entry form's draft), so the
  /// command returns to rest. Consuming it twice would hand the same workout
  /// to the form again.
  void onSaveHandled() {
    state = state.copyWith(save: const CommandState.idle());
  }
}

/// App-lifetime on purpose: a recording outlives the route that started it, and
/// must still be running when the user comes back to it.
final activityRecordingViewModelProvider = NotifierProvider<
    ActivityRecordingViewModel, ActivityRecordingUiState>(
  ActivityRecordingViewModel.new,
);
