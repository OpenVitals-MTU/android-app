import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/presentation/unit_formatter.dart';
import '../../../../domain/preferences/app_theme_mode.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/theme/activity_recording_theme.dart';
import 'activity_recording.dart';
import 'activity_recording_dashboard.dart';

/// Port of the Kotlin `ActivityRecordingFocusMode.kt`: the full-screen,
/// glanceable view you switch to mid-activity.

/// Whether focus mode is available at all for what is being recorded.
///
/// Top-level, and not a method on the screen, because the HOST has to answer the
/// same question: it hides its app bar for focus mode, and if the two ever
/// disagreed you would get a screen with no app bar and no exit button on it.
bool canUseRecordingFocusMode(ActivityRecordingState state) =>
    state.isActive &&
    state.recordingKind != ActivityRecordingKind.repetition;

/// Kotlin `TwentyFourHourClockFormatter`.
String formatFocusModeClock(DateTime now) =>
    '${now.hour.toString().padLeft(2, '0')}:'
    '${now.minute.toString().padLeft(2, '0')}';

/// Hides and recolors the system bars for as long as it is mounted, restoring
/// them on the way out. Kotlin does the same through
/// `WindowInsetsControllerCompat` and `window.statusBarColor` /
/// `window.navigationBarColor`.
class ActivityRecordingSystemBars extends StatefulWidget {
  const ActivityRecordingSystemBars({
    super.key,
    required this.hideSystemBars,
    this.outdoorModeEnabled = false,
    this.outdoorUsesLightScheme = false,
    required this.child,
  });

  final bool hideSystemBars;
  final bool outdoorModeEnabled;
  final bool outdoorUsesLightScheme;
  final Widget child;

  @override
  State<ActivityRecordingSystemBars> createState() =>
      _ActivityRecordingSystemBarsState();
}

class _ActivityRecordingSystemBarsState
    extends State<ActivityRecordingSystemBars> {
  bool? _appliedHideSystemBars;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // Gesture insets come from MediaQuery, so (re-)apply here rather than in
    // initState/didUpdateWidget.
    _apply();
  }

  @override
  void didUpdateWidget(ActivityRecordingSystemBars oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.hideSystemBars != widget.hideSystemBars) _apply();
  }

  /// Kotlin `Context.isGestureNavigationMode()`. Dart cannot read the
  /// `config_navBarInteractionMode` resource, so it uses the standard
  /// heuristic: gesture navigation reserves horizontal system-gesture insets
  /// for the back gesture, three-button navigation does not.
  bool get _isGestureNavigationMode {
    final gestureInsets = MediaQuery.systemGestureInsetsOf(context);
    return gestureInsets.left > 0 && gestureInsets.right > 0;
  }

  void _apply() {
    if (widget.hideSystemBars) {
      if (_isGestureNavigationMode) {
        // Kotlin hides only the status bar under gesture navigation: the
        // gesture "bar" is unobtrusive and hiding it forces a two-swipe exit.
        SystemChrome.setEnabledSystemUIMode(
          SystemUiMode.manual,
          overlays: const [SystemUiOverlay.bottom],
        );
      } else {
        // Transient: a swipe brings the bars back, as on Android.
        SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
      }
    } else if (_appliedHideSystemBars != false) {
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    }
    _appliedHideSystemBars = widget.hideSystemBars;
  }

  @override
  void dispose() {
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    super.dispose();
  }

  /// Kotlin recolors the bars white-with-dark-icons for the outdoor light
  /// scheme and black-with-light-icons for the dark one. `AnnotatedRegion`
  /// restores the ambient style by itself once outdoor mode is off or the
  /// widget unmounts.
  SystemUiOverlayStyle get _outdoorOverlayStyle => widget.outdoorUsesLightScheme
      ? const SystemUiOverlayStyle(
          statusBarColor: Colors.white,
          statusBarIconBrightness: Brightness.dark,
          statusBarBrightness: Brightness.light,
          systemNavigationBarColor: Colors.white,
          systemNavigationBarIconBrightness: Brightness.dark,
        )
      : const SystemUiOverlayStyle(
          statusBarColor: Colors.black,
          statusBarIconBrightness: Brightness.light,
          statusBarBrightness: Brightness.dark,
          systemNavigationBarColor: Colors.black,
          systemNavigationBarIconBrightness: Brightness.light,
        );

  @override
  Widget build(BuildContext context) {
    if (!widget.outdoorModeEnabled) return widget.child;
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: _outdoorOverlayStyle,
      child: widget.child,
    );
  }
}

/// Kotlin `ActivityRecordingOutdoorModeToggle`: a high-contrast mode for direct
/// sunlight.
class ActivityRecordingOutdoorModeToggle extends StatelessWidget {
  const ActivityRecordingOutdoorModeToggle({
    super.key,
    required this.enabled,
    required this.onEnabledChange,
    this.appThemeMode = AppThemeMode.system,
  });

  final bool enabled;
  final ValueChanged<bool> onEnabledChange;
  final AppThemeMode appThemeMode;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final scheme = Theme.of(context).colorScheme;
    return IconButton(
      onPressed: () => onEnabledChange(!enabled),
      tooltip: l10n.cdToggleRecordingOutdoorMode,
      icon: Icon(
        enabled ? Icons.light_mode : Icons.wb_sunny_outlined,
        // Kotlin tints the enabled icon with the accent of the *app* theme:
        // it must stay legible whichever scheme the toggle switches to.
        color: enabled
            ? recordingOutdoorAccentForAppTheme(context, appThemeMode)
            : scheme.onSurfaceVariant,
      ),
    );
  }
}

/// Kotlin `ActivityRecordingFocusMode`: the clock, the dashboard filling the
/// screen, and one big pause/resume button.
class ActivityRecordingFocusMode extends StatelessWidget {
  const ActivityRecordingFocusMode({
    super.key,
    required this.state,
    required this.totalTime,
    required this.movingTime,
    required this.now,
    required this.unitFormatter,
    required this.isOutdoorMode,
    required this.onOutdoorModeChanged,
    this.appThemeMode = AppThemeMode.system,
    required this.onPauseRecording,
    required this.onResumeRecording,
    required this.onExitFocusMode,
  });

  final ActivityRecordingState state;
  final Duration totalTime;
  final Duration movingTime;
  final DateTime now;
  final UnitFormatter unitFormatter;
  final bool isOutdoorMode;
  final ValueChanged<bool> onOutdoorModeChanged;
  final AppThemeMode appThemeMode;
  final VoidCallback onPauseRecording;
  final VoidCallback onResumeRecording;
  final VoidCallback onExitFocusMode;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final availableFields = availableRecordingDashboardFields(state);
    final layout = state.dashboardLayout.withAvailableFields(availableFields);
    final stats = recordingDashboardStats(
      state: state,
      totalTime: totalTime,
      movingTime: movingTime,
      now: now,
      unitFormatter: unitFormatter,
      l10n: l10n,
    );
    final isPaused = state.status == ActivityRecordingStatus.paused;
    final canToggle =
        state.status == ActivityRecordingStatus.recording || isPaused;

    // Kotlin fills the focus column with `colorScheme.background`: pure
    // black/white under the outdoor theme, the normal surface otherwise. The
    // system bars are hidden/recolored by the screen-level
    // [ActivityRecordingSystemBars], as in Kotlin.
    final backgroundColor = isOutdoorMode
        ? recordingOutdoorBackgroundColor(
            outdoorUsesLightScheme: recordingOutdoorUsesLightScheme(
              context,
              outdoorModeEnabled: isOutdoorMode,
              appThemeMode: appThemeMode,
            ),
          )
        : theme.colorScheme.surface;

    return ColoredBox(
      color: backgroundColor,
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            spacing: 16,
            children: [
              Stack(
                alignment: Alignment.center,
                children: [
                  Text(
                    formatFocusModeClock(now),
                    maxLines: 1,
                    style: theme.textTheme.displayLarge
                        ?.copyWith(color: theme.colorScheme.onSurface),
                  ),
                  Align(
                    alignment: Alignment.centerLeft,
                    child: ActivityRecordingOutdoorModeToggle(
                      enabled: isOutdoorMode,
                      onEnabledChange: onOutdoorModeChanged,
                      appThemeMode: appThemeMode,
                    ),
                  ),
                  Align(
                    alignment: Alignment.centerRight,
                    child: IconButton(
                      onPressed: onExitFocusMode,
                      tooltip: l10n.cdExitRecordingFocusMode,
                      icon: Icon(Icons.fullscreen_exit,
                          color: theme.colorScheme.onSurfaceVariant),
                    ),
                  ),
                ],
              ),
              Expanded(
                child: RecordingDashboardGrid(
                  layout: layout,
                  stats: stats,
                  isEditingDashboard: false,
                  onUpdateLayout: (_) {},
                  fillHeight: true,
                ),
              ),
              SizedBox(
                height: 56,
                child: FilledButton.icon(
                  onPressed: canToggle
                      ? (isPaused ? onResumeRecording : onPauseRecording)
                      : null,
                  icon: Icon(isPaused ? Icons.play_arrow : Icons.pause, size: 22),
                  label: Text(isPaused ? l10n.actionResume : l10n.actionPause),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
