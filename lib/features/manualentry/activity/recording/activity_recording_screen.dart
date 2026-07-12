import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import 'package:wakelock_plus/wakelock_plus.dart';

import '../../../../core/presentation/unit_formatter.dart';
import '../../../../domain/model/activity_models.dart';
import '../../../../domain/preferences/activity_recording_dashboard_layout.dart';
import '../../../../domain/preferences/app_theme_mode.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/theme/activity_recording_theme.dart';
import 'activity_recording.dart';
import 'activity_recording_controls.dart';
import 'activity_recording_dashboard.dart';
import 'activity_recording_device_support.dart';
import 'activity_recording_focus_mode.dart';
import 'activity_recording_gps_tabs.dart';
import 'activity_recording_splits_ui.dart';

/// Port of the Kotlin `ActivityRecordingScreen`: the live recording surface,
/// dispatching on how the activity is being measured.
class ActivityRecordingScreen extends ConsumerStatefulWidget {
  const ActivityRecordingScreen({
    super.key,
    required this.state,
    required this.unitFormatter,
    required this.onStartRecording,
    required this.onPauseRecording,
    required this.onResumeRecording,
    required this.onAddLap,
    required this.onAddMarker,
    required this.onUpdateMarker,
    required this.onDeleteMarker,
    required this.onUpdateDashboardLayout,
    required this.onChooseSource,
    required this.onAdjustRepetitionCount,
    required this.onEndRepetitionSet,
    required this.onStartNextRepetitionSet,
    required this.onFinishRecording,
    required this.isFocusMode,
    required this.onFocusModeChanged,
    this.appThemeMode = AppThemeMode.system,
  });

  final ActivityRecordingState state;
  final UnitFormatter unitFormatter;
  final ValueChanged<ActivityRecordingInitialFix?> onStartRecording;
  final VoidCallback onPauseRecording;
  final VoidCallback onResumeRecording;
  final VoidCallback onAddLap;
  final VoidCallback onAddMarker;
  final ValueChanged<ActivityRecordingMarker> onUpdateMarker;
  final ValueChanged<String> onDeleteMarker;
  final ValueChanged<ActivityRecordingDashboardLayout> onUpdateDashboardLayout;
  final VoidCallback onChooseSource;
  final ValueChanged<int> onAdjustRepetitionCount;
  final VoidCallback onEndRepetitionSet;
  final VoidCallback onStartNextRepetitionSet;
  final VoidCallback onFinishRecording;

  /// Focus mode is owned by the host, not by this screen, because the host has
  /// to react to it too: it drops its app bar so focus mode gets the whole
  /// display. Kotlin published the same flag upwards
  /// (`isRecordingFocusMode` in `ActivityEntryScreen.kt`).
  final bool isFocusMode;
  final ValueChanged<bool> onFocusModeChanged;

  /// Kotlin threads `appThemeMode` in so the outdoor theme can decide between
  /// its light and dark high-contrast schemes.
  final AppThemeMode appThemeMode;

  @override
  ConsumerState<ActivityRecordingScreen> createState() =>
      _ActivityRecordingScreenState();
}

class _ActivityRecordingScreenState
    extends ConsumerState<ActivityRecordingScreen> {
  /// The clock the elapsed/moving durations are computed against. Ticks once a
  /// second while recording, as the Kotlin `LaunchedEffect(state.status)` does.
  DateTime _now = DateTime.now();
  Timer? _ticker;

  bool _isEditingDashboard = false;
  bool _isOutdoorMode = false;
  String? _dashboardEditTypeId;

  /// Whether this screen currently holds the wakelock, so it only touches the
  /// platform when the answer changes and never leaks it on dispose.
  bool _wakelockHeld = false;

  @override
  void initState() {
    super.initState();
    _syncTicker();
    _syncWakelock();
  }

  @override
  void didUpdateWidget(ActivityRecordingScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    _syncTicker();
    _syncWakelock();
  }

  @override
  void dispose() {
    _ticker?.cancel();
    if (_wakelockHeld) {
      _wakelockHeld = false;
      _setWakelock(false);
    }
    super.dispose();
  }

  void _syncTicker() {
    final shouldTick = widget.state.isActive;
    if (shouldTick && _ticker == null) {
      _ticker = Timer.periodic(const Duration(seconds: 1), (_) {
        if (mounted) setState(() => _now = DateTime.now());
      });
    } else if (!shouldTick && _ticker != null) {
      _ticker!.cancel();
      _ticker = null;
    }
  }

  /// Kotlin sets `view.keepScreenOn` while the session is active and the
  /// preference is on, restoring it once either stops being true.
  void _syncWakelock() {
    final shouldHold =
        widget.state.isActive && widget.state.keepScreenOnDuringRecording;
    if (shouldHold == _wakelockHeld) return;
    _wakelockHeld = shouldHold;
    _setWakelock(shouldHold);
  }

  void _setWakelock(bool enable) {
    // Best-effort: keeping the screen awake must never crash a recording (and
    // the plugin is absent under `flutter test`).
    unawaited(
      (enable ? WakelockPlus.enable() : WakelockPlus.disable())
          .catchError((Object _) {}),
    );
  }

  /// Kotlin only allows editing while a GPS recording is idle or paused: the
  /// grid must not shuffle under your thumb mid-run.
  bool get _canEditDashboard =>
      !widget.isFocusMode &&
      widget.state.recordingKind == ActivityRecordingKind.gpsRoute &&
      (widget.state.status == ActivityRecordingStatus.idle ||
          widget.state.status == ActivityRecordingStatus.paused);

  @override
  Widget build(BuildContext context) {
    final state = widget.state;

    // Kotlin resets the edit toggle when recording resumes, when focus mode is
    // entered, and when the activity type changes.
    if (state.status == ActivityRecordingStatus.recording ||
        widget.isFocusMode) {
      _isEditingDashboard = false;
    }
    if (_dashboardEditTypeId != state.activityTypeId) {
      _dashboardEditTypeId = state.activityTypeId;
      _isEditingDashboard = false;
    }

    final movingTime = state.movingDuration(_now);
    final totalTime = state.recordingKind == ActivityRecordingKind.repetition
        ? movingTime + state.restDuration(_now)
        : state.elapsedDuration(_now);

    // Derived, never stored: the host gates on exactly the same function, so the
    // two cannot drift into disagreeing about whether the app bar is showing.
    final showFocusMode =
        widget.isFocusMode && canUseRecordingFocusMode(state);

    // Kotlin wraps everything in `ActivityRecordingTheme` and recolors/hides
    // the system bars through `ActivityRecordingSystemBars` at this level, so
    // outdoor mode restyles both the normal and the focus layout.
    return ActivityRecordingTheme(
      outdoorModeEnabled: _isOutdoorMode,
      appThemeMode: widget.appThemeMode,
      child: ActivityRecordingSystemBars(
        hideSystemBars: showFocusMode,
        outdoorModeEnabled: _isOutdoorMode,
        outdoorUsesLightScheme: recordingOutdoorUsesLightScheme(
          context,
          outdoorModeEnabled: _isOutdoorMode,
          appThemeMode: widget.appThemeMode,
        ),
        child: showFocusMode
            ? PopScope(
                canPop: false,
                onPopInvokedWithResult: (didPop, _) {
                  if (!didPop) widget.onFocusModeChanged(false);
                },
                child: ActivityRecordingFocusMode(
                  state: state,
                  totalTime: totalTime,
                  movingTime: movingTime,
                  now: _now,
                  unitFormatter: widget.unitFormatter,
                  isOutdoorMode: _isOutdoorMode,
                  onOutdoorModeChanged: (value) =>
                      setState(() => _isOutdoorMode = value),
                  appThemeMode: widget.appThemeMode,
                  onPauseRecording: widget.onPauseRecording,
                  onResumeRecording: widget.onResumeRecording,
                  onExitFocusMode: () => widget.onFocusModeChanged(false),
                ),
              )
            // A Builder so `Theme.of` inside resolves against the outdoor
            // theme installed just above, not this widget's outer context.
            : Builder(
                builder: (context) =>
                    _normalModeBody(context, totalTime, movingTime),
              ),
      ),
    );
  }

  Widget _normalModeBody(
    BuildContext context,
    Duration totalTime,
    Duration movingTime,
  ) {
    final theme = Theme.of(context);
    final state = widget.state;

    final column = Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 12,
      children: [
        // Kotlin hoists both controls into the host app bar (via
        // `onDashboardEditStateChanged` and
        // `onActivityRecordingOutdoorModeStateChanged`); here they sit above the
        // grid they act on. Kotlin publishes the outdoor toggle exactly when
        // `isRecordingDashboardVisible && !isRecordingFocusMode`
        // (ActivityEntryScreen.kt:139-141) — which is precisely when this body
        // renders, so no extra visibility condition is needed. Focus mode shows
        // its own copy of the toggle, so outdoor mode stays reachable in both.
        Row(
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            if (_canEditDashboard)
              TextButton.icon(
                onPressed: () =>
                    setState(() => _isEditingDashboard = !_isEditingDashboard),
                icon: Icon(
                    _isEditingDashboard ? Icons.check : Icons.edit_outlined,
                    size: 18),
                label: Text(AppLocalizations.of(context)
                    .activityEntryRecordingDashboardLayout),
              ),
            ActivityRecordingOutdoorModeToggle(
              enabled: _isOutdoorMode,
              onEnabledChange: (value) =>
                  setState(() => _isOutdoorMode = value),
              appThemeMode: widget.appThemeMode,
            ),
          ],
        ),
        ...switch (state.recordingKind) {
          ActivityRecordingKind.repetition => _repetitionBody(theme, totalTime, movingTime),
          ActivityRecordingKind.timed => _timedBody(theme, totalTime, movingTime),
          ActivityRecordingKind.gpsRoute => _gpsBody(totalTime, movingTime),
        },
      ],
    );
    // The dashboard fills the screen rather than sitting in the entry form's
    // padded scroll view, so it carries its own side padding. It goes INSIDE the
    // outdoor ColoredBox below, or outdoor mode would paint the background up to
    // a margin and leave a frame of the ordinary theme colour around it.
    final padded = Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
      child: column,
    );
    if (!_isOutdoorMode) return padded;

    // Kotlin backs the non-focus column with `colorScheme.background` while
    // outdoor mode is on: pure black (dark scheme) or pure white (light).
    return ColoredBox(
      color: recordingOutdoorBackgroundColor(
        outdoorUsesLightScheme: recordingOutdoorUsesLightScheme(
          context,
          outdoorModeEnabled: _isOutdoorMode,
          appThemeMode: widget.appThemeMode,
        ),
      ),
      child: padded,
    );
  }

  List<Widget> _repetitionBody(
    ThemeData theme,
    Duration totalTime,
    Duration movingTime,
  ) =>
      [
        Expanded(
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              spacing: 16,
              children: [
                RepetitionRecordingStats(
                  state: widget.state,
                  totalTime: totalTime,
                  movingTime: movingTime,
                  unitFormatter: widget.unitFormatter,
                  onAdjustRepetitionCount: widget.onAdjustRepetitionCount,
                ),
                _errorText(theme),
              ],
            ),
          ),
        ),
        RepetitionRecordingControls(
          state: widget.state,
          onEndRepetitionSet: widget.onEndRepetitionSet,
          onStartNextRepetitionSet: widget.onStartNextRepetitionSet,
          onFinishRecording: widget.onFinishRecording,
        ),
      ];

  List<Widget> _timedBody(
    ThemeData theme,
    Duration totalTime,
    Duration movingTime,
  ) =>
      [
        Expanded(
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              spacing: 16,
              children: [
                RecordingStatsTab(
                  state: widget.state,
                  totalTime: totalTime,
                  movingTime: movingTime,
                  now: _now,
                  unitFormatter: widget.unitFormatter,
                  isEditingDashboard: false,
                  onUpdateDashboardLayout: widget.onUpdateDashboardLayout,
                ),
                _errorText(theme),
              ],
            ),
          ),
        ),
        TimedRecordingControls(
          state: widget.state,
          onPauseRecording: widget.onPauseRecording,
          onResumeRecording: widget.onResumeRecording,
          onEnterFocusMode: () => widget.onFocusModeChanged(true),
          onFinishRecording: widget.onFinishRecording,
        ),
      ];

  List<Widget> _gpsBody(Duration totalTime, Duration movingTime) {
    // Before the first point, the map still wants somewhere to centre, and Start
    // stays disabled until the fix is precise enough to anchor the session.
    final idleFix = widget.state.status == ActivityRecordingStatus.idle
        ? ref.watch(preRecordingGpsFixProvider(true)).value ??
            const PreRecordingGpsFixState()
        : const PreRecordingGpsFixState();

    return [
      Expanded(
        child: GpsRecordingTabs(
          state: widget.state,
          preStartPoint: _toRoutePoint(idleFix.latestPreciseFix),
          totalTime: totalTime,
          movingTime: movingTime,
          now: _now,
          unitFormatter: widget.unitFormatter,
          isEditingDashboard: _isEditingDashboard,
          onUpdateDashboardLayout: widget.onUpdateDashboardLayout,
        ),
      ),
      GpsRecordingControls(
        state: widget.state,
        canStartRecording: idleFix.latestPreciseFix != null,
        onStartRecording: () => widget.onStartRecording(idleFix.initialFix),
        onPauseRecording: widget.onPauseRecording,
        onResumeRecording: widget.onResumeRecording,
        onEnterFocusMode: () => widget.onFocusModeChanged(true),
        onFinishRecording: widget.onFinishRecording,
        onAddLap: widget.onAddLap,
        onAddMarker: widget.onAddMarker,
        onChooseSource: widget.onChooseSource,
      ),
      _GpsRecordingOverflowContent(
        state: widget.state,
        unitFormatter: widget.unitFormatter,
        onUpdateMarker: widget.onUpdateMarker,
        onDeleteMarker: widget.onDeleteMarker,
      ),
    ];
  }

  Widget _errorText(ThemeData theme) {
    final message = widget.state.errorMessage;
    if (message == null) return const SizedBox.shrink();
    return Text(
      message,
      style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error),
    );
  }

  /// Kotlin `Location.toRoutePoint()`.
  ExerciseRoutePoint? _toRoutePoint(Position? position) {
    if (position == null) return null;
    return ExerciseRoutePoint(
      time: position.timestamp.toUtc(),
      latitude: position.latitude,
      longitude: position.longitude,
      altitudeMeters: position.altitude,
      horizontalAccuracyMeters: position.accuracy > 0 ? position.accuracy : null,
      verticalAccuracyMeters: null,
    );
  }
}

/// Kotlin `GpsRecordingOverflowContent`: markers and the recording error, under
/// the controls.
class _GpsRecordingOverflowContent extends StatelessWidget {
  const _GpsRecordingOverflowContent({
    required this.state,
    required this.unitFormatter,
    required this.onUpdateMarker,
    required this.onDeleteMarker,
  });

  final ActivityRecordingState state;
  final UnitFormatter unitFormatter;
  final ValueChanged<ActivityRecordingMarker> onUpdateMarker;
  final ValueChanged<String> onDeleteMarker;

  @override
  Widget build(BuildContext context) {
    if (state.markers.isEmpty && state.errorMessage == null) {
      return const SizedBox.shrink();
    }
    final theme = Theme.of(context);
    final errorMessage = state.errorMessage;

    return ConstrainedBox(
      constraints: const BoxConstraints(maxHeight: 72),
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          spacing: 12,
          children: [
            RecordingMarkersList(
              markers: state.markers,
              unitFormatter: unitFormatter,
              onUpdateMarker: onUpdateMarker,
              onDeleteMarker: onDeleteMarker,
            ),
            if (errorMessage != null)
              Text(
                errorMessage,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.error),
              ),
          ],
        ),
      ),
    );
  }
}
