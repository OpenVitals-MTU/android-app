import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/command_state.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../di/providers.dart';
import '../../../domain/model/mindfulness_models.dart';

part 'mindfulness_entry_view_model.freezed.dart';

const int _minSessionMinutes = 1;
const int _maxSessionMinutes = 24 * 60;
const String _defaultSessionTitle = 'Meditation';

/// The timer advances one second per tick (Kotlin `TimerTickMillis`).
const Duration kMindfulnessTimerTick = Duration(seconds: 1);

/// How long a tapped bell / background sound plays in the picker.
const int kMindfulnessBellPreviewMillis = 1500;
const int kMindfulnessBackgroundPreviewMillis = 2000;

/// Why the form refuses to save. Validation and gating only — a write that was
/// attempted and *failed* is not one of these; it lives in
/// [MindfulnessEntryState.save] as a [CommandFailure], carrying the real error.
enum MindfulnessEntryError {
  invalidTimer,
  invalidManualEntry,
  timerTooShort,
  missingWritePermission,
  unavailable,
}

/// A request to ring a bell. The [id] increments on every emission so the screen
/// can distinguish two identical bells — the Compose `LaunchedEffect(event.id)`
/// pattern.
class MindfulnessBellEvent {
  const MindfulnessBellEvent(this.id, this.sound, {this.previewMillis});

  final int id;
  final MindfulnessBellSound sound;

  /// Non-null only for a picker preview, which cuts the clip short.
  final int? previewMillis;
}

/// A request to preview an ambient sound in the picker.
class MindfulnessBackgroundEvent {
  const MindfulnessBackgroundEvent(this.id, this.sound, this.previewMillis);

  final int id;
  final MindfulnessBackgroundSound sound;
  final int previewMillis;
}

/// Riverpod port of the Kotlin `MindfulnessEntryUiState`.
@freezed
abstract class MindfulnessEntryState with _$MindfulnessEntryState {
  const MindfulnessEntryState._();

  const factory MindfulnessEntryState({
    @Default('') String durationMinutesText,
    @Default(false) bool intervalEnabled,
    @Default('') String intervalMinutesText,
    @Default(MindfulnessBellSound.struck) MindfulnessBellSound bellSound,
    @Default(MindfulnessBackgroundSound.none)
    MindfulnessBackgroundSound backgroundSound,
    @Default('') String manualMinutesText,
    @Default(<String>{}) Set<String> writePermissions,
    @Default(false) bool canWrite,
    @Default(true) bool mindfulnessAvailable,
    @Default(true) bool isCheckingPermission,
    @Default(CommandState<void>.idle()) CommandState<void> save,
    @Default(false) bool isTimerRunning,
    @Default(false) bool isTimerPaused,
    @Default(false) bool timerCompleted,
    @Default(0) int remainingSeconds,
    @Default(0) int totalSeconds,
    String? editRecordId,
    DateTime? editStartTime,
    MindfulnessEntryError? entryError,

    /// The edit prefill could not be read — a different thing from a save that
    /// failed, and it blocks the form before the user has done anything.
    ScreenError? prefillError,
    MindfulnessBellEvent? bellEvent,
    MindfulnessBackgroundEvent? backgroundEvent,
  }) = _MindfulnessEntryState;

  bool get isEditMode => editRecordId != null;

  bool get isSavingEntry => save is CommandRunning<void>;

  /// The error the form should be showing, if any: a failed prefill outranks a
  /// failed write, because it means the form was never trustworthy to begin
  /// with.
  ScreenError? get blockingError =>
      prefillError ??
      switch (save) {
        CommandFailure<void>(:final error) => error,
        _ => null,
      };

  /// The timer's fields may only be edited while it is idle — the Kotlin
  /// `canUpdateTimerFields`.
  bool get canEditTimer =>
      !isEditMode && !isTimerRunning && !isTimerPaused && !timerCompleted;
}

/// `mm:ss`, clamped at zero. Port of the Kotlin `formattedTimer`.
String formattedTimer(int seconds) {
  final clamped = seconds < 0 ? 0 : seconds;
  final minutes = clamped ~/ 60;
  final remaining = clamped % 60;
  return '${minutes.toString().padLeft(2, '0')}:'
      '${remaining.toString().padLeft(2, '0')}';
}

/// Parses a positive minute count. Port of the Kotlin `String.toPositiveIntOrNull`.
int? parsePositiveMinutes(String text) {
  final value = int.tryParse(text.trim());
  return (value != null && value > 0) ? value : null;
}

/// Riverpod port of the Kotlin `MindfulnessEntryViewModel`: a guided meditation
/// timer (start / stop / resume / discard / save, with interval bells and an
/// ambient loop) plus the manual duration entry.
///
/// Sounds are emitted as `bellEvent` / `backgroundEvent` state rather than
/// played here, exactly as the Kotlin ViewModel does — the screen owns audio, so
/// the whole state machine is unit-testable with no audio host.
class MindfulnessEntryViewModel extends Notifier<MindfulnessEntryState> {
  MindfulnessEntryViewModel({this.editRecordId, this.tick = kMindfulnessTimerTick});

  final String? editRecordId;

  /// Injectable so tests can run a session without waiting in real time.
  final Duration tick;

  Timer? _timer;
  DateTime? _timerStart;
  DateTime? _completedStart;
  DateTime? _completedEnd;
  int _bellEventId = 0;
  int _backgroundEventId = 0;

  @override
  MindfulnessEntryState build() {
    final config = ref.read(preferencesRepositoryProvider).mindfulnessTimerConfig();
    ref.onDispose(_cancelTimer);
    Future.microtask(() async {
      if (!ref.mounted) return;
      await refreshPermission();
      if (ref.mounted) await _loadEditEntry();
    });
    return MindfulnessEntryState(
      durationMinutesText: config.durationMinutes.toString(),
      intervalEnabled: config.intervalMinutes != null,
      intervalMinutesText: config.intervalMinutes?.toString() ?? '',
      bellSound: config.bellSound,
      backgroundSound: config.backgroundSound,
      remainingSeconds: config.durationMinutes * 60,
      totalSeconds: config.durationMinutes * 60,
      editRecordId: editRecordId,
    );
  }

  // ── Timer configuration ───────────────────────────────────────────────────

  /// Port of the Kotlin `updateTimerFields`: silently ignored unless idle.
  void _updateTimerFields(MindfulnessEntryState Function() update) {
    if (!state.canEditTimer) return;
    state = update().copyWith(
      entryError: null,
      save: const CommandState.idle(),
    );
  }

  void updateDurationMinutes(String text) {
    _updateTimerFields(() {
      final duration = parsePositiveMinutes(text);
      return state.copyWith(
        durationMinutesText: text,
        totalSeconds: duration != null ? duration * 60 : state.totalSeconds,
        remainingSeconds: duration != null ? duration * 60 : state.remainingSeconds,
      );
    });
  }

  void updateIntervalEnabled(bool enabled) =>
      _updateTimerFields(() => state.copyWith(intervalEnabled: enabled));

  void updateIntervalMinutes(String text) =>
      _updateTimerFields(() => state.copyWith(intervalMinutesText: text));

  /// Selecting a bell previews it, so the user hears what they picked.
  void updateBellSound(MindfulnessBellSound sound) {
    if (!state.canEditTimer) return;
    _bellEventId += 1;
    state = state.copyWith(
      bellSound: sound,
      entryError: null,
      bellEvent: MindfulnessBellEvent(
        _bellEventId,
        sound,
        previewMillis: kMindfulnessBellPreviewMillis,
      ),
    );
  }

  void updateBackgroundSound(MindfulnessBackgroundSound sound) {
    if (!state.canEditTimer) return;
    _backgroundEventId += 1;
    state = state.copyWith(
      backgroundSound: sound,
      entryError: null,
      // "None" is silence, not a preview.
      backgroundEvent: sound == MindfulnessBackgroundSound.none
          ? null
          : MindfulnessBackgroundEvent(
              _backgroundEventId,
              sound,
              kMindfulnessBackgroundPreviewMillis,
            ),
    );
  }

  // ── Timer transport ───────────────────────────────────────────────────────

  /// Validates the timer fields, persists them, and starts counting down.
  void startTimer() {
    final config = _currentTimerConfigOrNull();
    if (config == null) {
      state = state.copyWith(
        entryError: MindfulnessEntryError.invalidTimer,
      );
      return;
    }
    ref.read(preferencesRepositoryProvider).setMindfulnessTimerConfig(config);
    _cancelTimer();
    _timerStart = DateTime.now();
    _completedStart = null;
    _completedEnd = null;
    state = state.copyWith(
      durationMinutesText: config.durationMinutes.toString(),
      intervalEnabled: config.intervalMinutes != null,
      intervalMinutesText: config.intervalMinutes?.toString() ?? '',
      bellSound: config.bellSound,
      backgroundSound: config.backgroundSound,
      remainingSeconds: config.durationMinutes * 60,
      totalSeconds: config.durationMinutes * 60,
      isTimerRunning: true,
      isTimerPaused: false,
      timerCompleted: false,
      entryError: null,
    );
    _runTimer(config);
  }

  /// Pauses, banking the elapsed span so it can be saved as a short session.
  void stopTimer() {
    if (!state.isTimerRunning) return;
    _cancelTimer();

    final elapsed = state.totalSeconds - state.remainingSeconds;
    final elapsedSeconds = elapsed < 0 ? 0 : elapsed;
    final start = _timerStart ??
        DateTime.now().subtract(Duration(seconds: elapsedSeconds));
    _completedStart = start;
    _completedEnd = start.add(Duration(seconds: elapsedSeconds));
    state = state.copyWith(
      isTimerRunning: false,
      isTimerPaused: true,
      timerCompleted: false,
      entryError: null,
    );
  }

  void resumeTimer() {
    if (!state.isTimerPaused) return;
    final config = _currentTimerConfigOrNull();
    if (config == null || state.remainingSeconds <= 0) {
      state = state.copyWith(
        entryError: MindfulnessEntryError.invalidTimer,
      );
      return;
    }
    _completedStart = null;
    _completedEnd = null;
    state = state.copyWith(
      isTimerRunning: true,
      isTimerPaused: false,
      timerCompleted: false,
      entryError: null,
    );
    _runTimer(config);
  }

  /// Throws the session away and rewinds to the configured duration.
  void discardTimer() {
    _cancelTimer();
    _timerStart = null;
    _completedStart = null;
    _completedEnd = null;
    final duration = parsePositiveMinutes(state.durationMinutesText) ?? 0;
    state = state.copyWith(
      isTimerRunning: false,
      isTimerPaused: false,
      timerCompleted: false,
      remainingSeconds: duration * 60,
      totalSeconds: duration * 60,
      entryError: null,
    );
  }

  /// Writes the banked session. A session under a minute is rejected rather than
  /// rounded down to zero.
  Future<void> saveTimerSession() async {
    final start = _completedStart;
    final end = _completedEnd;
    if (start == null || end == null) return;
    if (end.difference(start).inMinutes < _minSessionMinutes) {
      state = state.copyWith(
        entryError: MindfulnessEntryError.timerTooShort,
      );
      return;
    }
    await _writeSession(
      start: start,
      end: end,
      onSuccess: () {
        final duration = parsePositiveMinutes(state.durationMinutesText) ?? 0;
        state = state.copyWith(
          isTimerPaused: false,
          timerCompleted: false,
          remainingSeconds: duration * 60,
          totalSeconds: duration * 60,
          save: const CommandState.success(null),
          entryError: null,
        );
        _completedStart = null;
        _completedEnd = null;
      },
    );
  }

  void _runTimer(MindfulnessTimerConfig config) {
    final totalSeconds = config.durationMinutes * 60;
    final intervalSeconds = config.intervalMinutes == null
        ? null
        : config.intervalMinutes! * 60;
    // Resuming continues from where it paused.
    var remaining = (state.remainingSeconds >= 1 &&
            state.remainingSeconds <= totalSeconds)
        ? state.remainingSeconds
        : totalSeconds;

    _timer = Timer.periodic(tick, (timer) {
      remaining -= 1;
      final elapsed = totalSeconds - remaining;
      state = state.copyWith(remainingSeconds: remaining);

      if (remaining > 0) {
        // An interval bell, but never on the final second — the completion bell
        // covers that.
        if (intervalSeconds != null &&
            elapsed > 0 &&
            elapsed % intervalSeconds == 0) {
          _emitBell(config.bellSound);
        }
        return;
      }

      _cancelTimer();
      final start = _timerStart ??
          DateTime.now().subtract(Duration(minutes: config.durationMinutes));
      _completedStart = start;
      _completedEnd = start.add(Duration(minutes: config.durationMinutes));
      state = state.copyWith(
        isTimerRunning: false,
        isTimerPaused: false,
        timerCompleted: true,
        remainingSeconds: 0,
      );
      _emitBell(config.bellSound);
    });
  }

  void _cancelTimer() {
    _timer?.cancel();
    _timer = null;
  }

  void _emitBell(MindfulnessBellSound sound) {
    _bellEventId += 1;
    state = state.copyWith(bellEvent: MindfulnessBellEvent(_bellEventId, sound));
  }

  /// Port of the Kotlin `currentTimerConfigOrNull`: null when the duration is
  /// out of range, or when interval bells are on but the interval is not a
  /// positive number strictly shorter than the session.
  MindfulnessTimerConfig? _currentTimerConfigOrNull() {
    final duration = parsePositiveMinutes(state.durationMinutesText);
    if (duration == null ||
        duration < _minSessionMinutes ||
        duration > _maxSessionMinutes) {
      return null;
    }
    int? interval;
    if (state.intervalEnabled) {
      interval = parsePositiveMinutes(state.intervalMinutesText);
      if (interval == null ||
          interval < _minSessionMinutes ||
          interval >= duration) {
        return null;
      }
    }
    return MindfulnessTimerConfig(
      durationMinutes: duration,
      intervalMinutes: interval,
      bellSound: state.bellSound,
      backgroundSound: state.backgroundSound,
    );
  }

  Future<void> refreshPermission() async {
    state = state.copyWith(
      isCheckingPermission: true,
      entryError: null,
    );
    // A failed probe comes back as unavailable-with-an-error rather than as a
    // throw, so an unsupported device and an unreachable one both report
    // `unavailable` — never a missing permission. See
    // [CheckMindfulnessWriteAccessUseCase].
    final access = await ref.read(checkMindfulnessWriteAccessUseCaseProvider)();
    if (!ref.mounted) return;
    final error = access.error;
    state = state.copyWith(
      isCheckingPermission: false,
      mindfulnessAvailable: access.available,
      writePermissions: access.permissions,
      canWrite: access.granted,
      entryError: access.available ? null : MindfulnessEntryError.unavailable,
      // A probe that could not answer is surfaced where a failed write is:
      // either way the form cannot promise the save will land.
      save: error == null
          ? const CommandState.idle()
          : CommandState.failure(error.toScreenError()),
    );
  }

  void updateManualMinutes(String text) {
    state = state.copyWith(
      manualMinutesText: text,
      save: const CommandState.idle(),
      entryError: null,
    );
  }

  void updateEntryStartTime(DateTime time) {
    final minutes = parsePositiveMinutes(state.manualMinutesText);
    state = state.copyWith(
      editStartTime: _coerceAtLatestSessionStart(time, minutes),
      save: const CommandState.idle(),
      entryError: null,
    );
  }

  /// Validates the manual minutes + writes (or updates) a mindfulness session.
  Future<void> addManualEntry() async {
    final minutes = parsePositiveMinutes(state.manualMinutesText);
    if (minutes == null ||
        minutes < _minSessionMinutes ||
        minutes > _maxSessionMinutes) {
      state = state.copyWith(
        entryError: MindfulnessEntryError.invalidManualEntry,
      );
      return;
    }

    final base = state.editStartTime ??
        DateTime.now().subtract(Duration(minutes: minutes));
    final start = _coerceAtLatestSessionStart(base, minutes);
    final end = start.add(Duration(minutes: minutes));

    await _writeSession(
      start: start,
      end: end,
      onSuccess: () {
        state = state.copyWith(
          // An edit keeps the value on screen; a new entry clears the field.
          manualMinutesText: state.isEditMode ? state.manualMinutesText : '',
          save: const CommandState.success(null),
          entryError: null,
        );
      },
    );
  }

  /// Writes (or updates) a session, gating on availability then permission —
  /// the Kotlin `writeSession` order, so an unsupported device reports
  /// `unavailable` rather than a missing permission.
  Future<void> _writeSession({
    required DateTime start,
    required DateTime end,
    required void Function() onSuccess,
  }) async {
    if (!state.mindfulnessAvailable) {
      state = state.copyWith(
        entryError: MindfulnessEntryError.unavailable,
      );
      return;
    }
    if (!state.canWrite) {
      state = state.copyWith(
        entryError: MindfulnessEntryError.missingWritePermission,
      );
      return;
    }

    state = state.copyWith(
      save: const CommandState.running(),
      entryError: null,
    );
    final request = MindfulnessSessionWriteRequest(
      title: _defaultSessionTitle,
      startTime: start,
      endTime: end,
    );
    final result = await ref.read(saveMindfulnessSessionUseCaseProvider)(
      request,
      editRecordId: editRecordId,
    );
    if (!ref.mounted) return;
    switch (result) {
      case Ok():
        onSuccess();
      case Err(:final failure):
        state = state.copyWith(
          save: CommandState.failure(failure.toScreenError()),
          entryError: null,
        );
    }
  }

  /// The screen consumed the success (it showed the toast and left), so the
  /// command returns to rest — otherwise re-entering the route would fire it
  /// again.
  void onSaveCompletedHandled() {
    state = state.copyWith(save: const CommandState.idle());
  }

  /// Prefills the form from the session being edited. A failure here is a
  /// *read* failure — the form has nothing to correct — so it lands on
  /// [MindfulnessEntryState.prefillError], not on the save command.
  Future<void> _loadEditEntry() async {
    final recordId = editRecordId;
    if (recordId == null) return;
    final result =
        await ref.read(loadMindfulnessSessionForEditUseCaseProvider)(recordId);
    if (!ref.mounted) return;
    switch (result) {
      case Ok(:final value):
        // Null covers both "no such session" and "not ours to edit".
        if (value == null) {
          state = state.copyWith(
            prefillError: const ScreenErrorMessage(
              'Only OpenVitals entries can be edited.',
            ),
          );
          return;
        }
        final rawMinutes =
            value.endTime.difference(value.startTime).inMinutes;
        final minutes = rawMinutes
            .clamp(_minSessionMinutes, _maxSessionMinutes);
        state = state.copyWith(
          manualMinutesText: minutes.toString(),
          editStartTime: _coerceAtLatestSessionStart(value.startTime, minutes),
          entryError: null,
          prefillError: null,
        );
      case Err(:final failure):
        state = state.copyWith(prefillError: failure.toScreenError());
    }
  }

  /// Port of the Kotlin `Instant.coerceAtLatestSessionStart`: the session start
  /// cannot be later than now minus its duration (so the whole session fits
  /// before the present moment).
  DateTime _coerceAtLatestSessionStart(DateTime time, int? minutes) {
    final durationMinutes = (minutes != null &&
            minutes >= _minSessionMinutes &&
            minutes <= _maxSessionMinutes)
        ? minutes
        : _minSessionMinutes;
    final latest = DateTime.now().subtract(Duration(minutes: durationMinutes));
    return time.isAfter(latest) ? latest : time;
  }
}
