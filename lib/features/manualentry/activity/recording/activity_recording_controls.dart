import 'package:flutter/material.dart';

import '../../../../core/presentation/display_value.dart';
import '../../../../core/presentation/unit_formatter.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_surface.dart';
import '../activity_entry_types.dart';
import 'activity_recording.dart';
import 'activity_recording_dashboard.dart';
import 'activity_recording_sensor_ui.dart';

/// Port of the Kotlin `ActivityRecordingControls.kt` and
/// `ActivityRecordingRepetitionStats.kt`.

const double _buttonHeight = 48;

/// Kotlin `RecordingStat` (in `ActivityRecordingSplitsUi.kt`): a big value with
/// its unit and a caption beneath.
class RecordingStat extends StatelessWidget {
  const RecordingStat({
    super.key,
    required this.value,
    required this.label,
    this.emphasized = false,
  });

  final DisplayValue value;
  final String label;
  final bool emphasized;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment:
          emphasized ? CrossAxisAlignment.start : CrossAxisAlignment.center,
      spacing: 4,
      children: [
        Row(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Flexible(
              child: FittedBox(
                fit: BoxFit.scaleDown,
                child: Text(
                  value.value,
                  maxLines: 1,
                  style: (emphasized
                          ? theme.textTheme.displayMedium
                          : theme.textTheme.displaySmall)
                      ?.copyWith(
                    color: emphasized
                        ? activityRecordingAccentColor()
                        : theme.colorScheme.onSurface,
                  ),
                ),
              ),
            ),
            if (value.unit.trim().isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(left: 3, bottom: 5),
                child: Text(
                  value.unit,
                  style: theme.textTheme.titleMedium
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ),
          ],
        ),
        Text(
          label.toUpperCase(),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: theme.textTheme.labelMedium
              ?.copyWith(color: activityRecordingAccentColor()),
        ),
      ],
    );
  }
}

/// Kotlin `TimedRecordingControls`: pause/resume, focus, finish.
class TimedRecordingControls extends StatelessWidget {
  const TimedRecordingControls({
    super.key,
    required this.state,
    required this.onPauseRecording,
    required this.onResumeRecording,
    required this.onEnterFocusMode,
    required this.onFinishRecording,
  });

  final ActivityRecordingState state;
  final VoidCallback onPauseRecording;
  final VoidCallback onResumeRecording;
  final VoidCallback onEnterFocusMode;
  final VoidCallback onFinishRecording;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final isPaused = state.status == ActivityRecordingStatus.paused;
    return OpenVitalsSurface(
      contentPadding: const EdgeInsets.all(8),
      borderRadius: const BorderRadius.all(Radius.circular(16)),
      child: Row(
        children: [
          Expanded(
            child: isPaused
                ? _ControlButton(
                    onPressed: onResumeRecording,
                    icon: Icons.play_arrow,
                    label: l10n.actionResume,
                  )
                : _ControlButton(
                    onPressed:
                        state.status == ActivityRecordingStatus.recording
                            ? onPauseRecording
                            : null,
                    icon: Icons.pause,
                    label: l10n.actionPause,
                  ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: _ControlButton(
              onPressed: state.isActive ? onEnterFocusMode : null,
              icon: Icons.fullscreen,
              label: l10n.activityEntryRecordingFocus,
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: _ControlButton(
              onPressed: state.isActive ? onFinishRecording : null,
              icon: Icons.check,
              label: l10n.actionFinish,
            ),
          ),
        ],
      ),
    );
  }
}

/// Kotlin `GpsRecordingControls`. Before the first point it is Start/Cancel;
/// afterwards pause/resume + focus + finish, over lap + marker.
class GpsRecordingControls extends StatelessWidget {
  const GpsRecordingControls({
    super.key,
    required this.state,
    required this.canStartRecording,
    required this.onStartRecording,
    required this.onPauseRecording,
    required this.onResumeRecording,
    required this.onEnterFocusMode,
    required this.onFinishRecording,
    required this.onAddLap,
    required this.onAddMarker,
    required this.onChooseSource,
  });

  final ActivityRecordingState state;
  final bool canStartRecording;
  final VoidCallback onStartRecording;
  final VoidCallback onPauseRecording;
  final VoidCallback onResumeRecording;
  final VoidCallback onEnterFocusMode;
  final VoidCallback onFinishRecording;
  final VoidCallback onAddLap;
  final VoidCallback onAddMarker;
  final VoidCallback onChooseSource;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    if (state.status == ActivityRecordingStatus.idle) {
      return OpenVitalsSurface(
        contentPadding: const EdgeInsets.all(8),
        borderRadius: const BorderRadius.all(Radius.circular(16)),
        child: Row(
          children: [
            Expanded(
              child: SizedBox(
                height: _buttonHeight,
                child: FilledButton.icon(
                  onPressed: canStartRecording ? onStartRecording : null,
                  icon: const Icon(Icons.play_arrow, size: 18),
                  label: Text(l10n.actionStart),
                ),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _ControlButton(
                onPressed: onChooseSource,
                label: l10n.actionCancel,
              ),
            ),
          ],
        ),
      );
    }

    final isPaused = state.status == ActivityRecordingStatus.paused;
    return OpenVitalsSurface(
      contentPadding: const EdgeInsets.all(8),
      borderRadius: const BorderRadius.all(Radius.circular(16)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        spacing: 8,
        children: [
          Row(
            children: [
              Expanded(
                child: isPaused
                    ? _ControlButton(
                        onPressed: onResumeRecording,
                        icon: Icons.play_arrow,
                        label: l10n.actionResume,
                      )
                    : _ControlButton(
                        onPressed: onPauseRecording,
                        icon: Icons.pause,
                        label: l10n.actionPause,
                      ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _ControlButton(
                  onPressed: onEnterFocusMode,
                  icon: Icons.fullscreen,
                  label: l10n.activityEntryRecordingFocus,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _ControlButton(
                  onPressed: onFinishRecording,
                  icon: Icons.check,
                  label: l10n.actionFinish,
                ),
              ),
            ],
          ),
          Row(
            children: [
              Expanded(
                child: _ControlButton(
                  // A lap needs two points to have a distance between them.
                  onPressed: state.points.length >= 2 ? onAddLap : null,
                  icon: Icons.flag_outlined,
                  label: l10n.activityEntryRecordingLap,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _ControlButton(
                  onPressed:
                      state.latestUiPoint != null || state.points.isNotEmpty
                          ? onAddMarker
                          : null,
                  icon: Icons.place_outlined,
                  label: l10n.activityEntryRecordingMarker,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _ControlButton extends StatelessWidget {
  const _ControlButton({
    required this.onPressed,
    required this.label,
    this.icon,
  });

  final VoidCallback? onPressed;
  final String label;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final style = OutlinedButton.styleFrom(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
    );
    final text = Text(label, maxLines: 1, overflow: TextOverflow.ellipsis);
    return SizedBox(
      height: _buttonHeight,
      child: icon == null
          ? OutlinedButton(onPressed: onPressed, style: style, child: text)
          : OutlinedButton.icon(
              onPressed: onPressed,
              style: style,
              icon: Icon(icon, size: 18),
              label: text,
            ),
    );
  }
}

/// Kotlin `RepetitionRecordingStats`: the live rep counter for this set, the
/// +/- correction, and the three durations.
class RepetitionRecordingStats extends StatelessWidget {
  const RepetitionRecordingStats({
    super.key,
    required this.state,
    required this.totalTime,
    required this.movingTime,
    required this.unitFormatter,
    required this.onAdjustRepetitionCount,
  });

  final ActivityRecordingState state;
  final Duration totalTime;
  final Duration movingTime;
  final UnitFormatter unitFormatter;
  final ValueChanged<int> onAdjustRepetitionCount;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final activityType = activityEntryTypeById(state.activityTypeId);
    final countLabel =
        activityType?.repetitionUnit == ActivityRepetitionUnit.steps
            ? l10n.activityEntryStepsTitle
            : l10n.activityEntryRepetitionsTitle;
    final isRecording = state.status == ActivityRecordingStatus.recording;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 16,
      children: [
        OpenVitalsSurface(
          style: OpenVitalsSurfaceStyle.metric,
          contentPadding: const EdgeInsets.all(16),
          child: Column(
            spacing: 14,
            children: [
              Text(
                countLabel,
                style: theme.textTheme.labelLarge
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
              Text(
                unitFormatter.count(state.currentSetRepetitionCount),
                style: theme.textTheme.displayMedium
                    ?.copyWith(color: theme.colorScheme.onSurface),
              ),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed:
                          isRecording && state.currentSetRepetitionCount > 0
                              ? () => onAdjustRepetitionCount(-1)
                              : null,
                      child: const Icon(Icons.remove, size: 18),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: OutlinedButton(
                      onPressed:
                          isRecording ? () => onAdjustRepetitionCount(1) : null,
                      child: const Icon(Icons.add, size: 18),
                    ),
                  ),
                ],
              ),
              Text(
                l10n.activityEntryRecordingRepetitionCorrectionHint,
                textAlign: TextAlign.center,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
              if (state.status == ActivityRecordingStatus.resting)
                Text(
                  l10n.activityEntryRecordingRestRemaining(
                      formatRecordingElapsed(state.restRemainingDuration())),
                  style: theme.textTheme.titleMedium
                      ?.copyWith(color: theme.colorScheme.primary),
                ),
            ],
          ),
        ),
        Row(
          children: [
            Expanded(
              child: RecordingStat(
                value: DisplayValue(formatRecordingElapsed(totalTime), ''),
                label: l10n.activityEntryRecordingTotalTime,
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: RecordingStat(
                value: DisplayValue(formatRecordingElapsed(movingTime), ''),
                label: l10n.activityEntryRecordingMovingTime,
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: RecordingStat(
                value: DisplayValue(
                    formatRecordingElapsed(state.restDuration()), ''),
                label: l10n.activityEntryRecordingRestTime,
              ),
            ),
          ],
        ),
        if (state.bleDeviceStatuses.isNotEmpty)
          ActivityRecordingLiveSensorStats(
            state: state,
            unitFormatter: unitFormatter,
          ),
      ],
    );
  }
}

/// Kotlin `RepetitionRecordingControls`: end the set (or start the next one
/// while resting), and finish the session.
class RepetitionRecordingControls extends StatelessWidget {
  const RepetitionRecordingControls({
    super.key,
    required this.state,
    required this.onEndRepetitionSet,
    required this.onStartNextRepetitionSet,
    required this.onFinishRecording,
  });

  final ActivityRecordingState state;
  final VoidCallback onEndRepetitionSet;
  final VoidCallback onStartNextRepetitionSet;
  final VoidCallback onFinishRecording;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final isResting = state.status == ActivityRecordingStatus.resting;

    return OpenVitalsSurface(
      contentPadding: const EdgeInsets.all(12),
      borderRadius: const BorderRadius.all(Radius.circular(16)),
      child: Row(
        children: [
          Expanded(
            child: SizedBox(
              height: 56,
              child: isResting
                  ? FilledButton.icon(
                      onPressed: onStartNextRepetitionSet,
                      icon: const Icon(Icons.play_arrow, size: 18),
                      label: Text(l10n.activityEntryRecordingStartNextSet),
                    )
                  : FilledButton.icon(
                      onPressed: state.currentSetRepetitionCount > 0
                          ? onEndRepetitionSet
                          : null,
                      icon: const Icon(Icons.stop, size: 18),
                      label: Text(l10n.activityEntryRecordingEndSet),
                    ),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: SizedBox(
              height: 56,
              child: FilledButton.icon(
                onPressed: onFinishRecording,
                icon: const Icon(Icons.check, size: 18),
                label: Text(l10n.activityEntryRecordingEndSession),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
