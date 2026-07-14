import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/presentation/unit_formatter.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_card.dart';
import '../../../../ui/components/ov_surface.dart';
import '../activity_entry_form_fields.dart';
import '../activity_entry_state.dart';
import '../../../../domain/model/activity_entry_types.dart';
import '../activity_entry_ui_text.dart';
import 'activity_recording.dart';
import 'activity_recording_device_support.dart';
import 'activity_recording_sensor_ui.dart';

/// Port of the Kotlin `ActivityRecordingSetupScreen`: what you see after
/// choosing "Record activity" and before the first GPS point.
class ActivityRecordingSetupScreen extends ConsumerStatefulWidget {
  const ActivityRecordingSetupScreen({
    super.key,
    required this.state,
    required this.recordingState,
    required this.unitFormatter,
    required this.onSelectActivityType,
    required this.onStartRecording,
    required this.onRequestLocationPermission,
    required this.onRequestActivityRecognitionPermission,
    required this.onChooseSource,
    required this.onRequestWritePermission,
  });

  final ActivityEntryUiState state;
  final ActivityRecordingState recordingState;
  final UnitFormatter unitFormatter;
  final ValueChanged<ActivityEntryType> onSelectActivityType;

  /// Kotlin `onStartRecording(Location?, Long)`, plus whether the user asked to record
  /// this GPS-capable activity WITHOUT GPS.
  final void Function(
    ActivityRecordingInitialFix? initialFix,
    int restSeconds,
    bool withoutGps,
  ) onStartRecording;
  final VoidCallback onRequestLocationPermission;
  final VoidCallback onRequestActivityRecognitionPermission;
  final VoidCallback onChooseSource;
  final VoidCallback onRequestWritePermission;

  @override
  ConsumerState<ActivityRecordingSetupScreen> createState() =>
      _ActivityRecordingSetupScreenState();
}

class _ActivityRecordingSetupScreenState
    extends ConsumerState<ActivityRecordingSetupScreen> {
  final _restSeconds = TextEditingController();
  bool _withoutGps = false;

  /// Kotlin resets the field with `rememberSaveable(selectedType.id)`.
  String? _restSecondsTypeId;

  @override
  void dispose() {
    _restSeconds.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final state = widget.state;
    final selectedType = state.selectedActivityType;

    if (_restSecondsTypeId != selectedType.id) {
      _restSecondsTypeId = selectedType.id;
      _restSeconds.clear();
    }

    final baseEnabled = state.canWrite &&
        !state.isCheckingPermission &&
        !state.isImportingRoute &&
        !state.isSavingEntry;

    final readiness = ref
        .watch(recordingSensorReadinessProvider(selectedType))
        .value;
    final gpsFix = selectedType.supportsGpsRoute
        ? ref
                .watch(preRecordingGpsFixProvider(
                    baseEnabled && selectedType.supportsGpsRoute))
                .value ??
            const PreRecordingGpsFixState()
        : const PreRecordingGpsFixState();

    // Kotlin: a GPS activity needs either a precise fix or no permission yet (so
    // the button can ask for one); everything else needs its sensor.
    // Recording without GPS waits for nothing: there is no fix to acquire, no location
    // permission to ask for, and no sensor to require. A duration IS a recording -- which
    // is the whole point, and the reason this was worth doing rather than telling people
    // to keep calling their runs treadmills.
    final recordingWithoutGps = selectedType.supportsGpsRoute && _withoutGps;
    final enabled = baseEnabled &&
        (recordingWithoutGps
            ? true
            : selectedType.supportsGpsRoute
                ? !gpsFix.hasPrecisePermission || gpsFix.latestPreciseFix != null
                : readiness?.hasRequiredSensor ?? false);

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          spacing: 12,
          children: [
            ActivityEntryHeader(
              state: state,
              onRequestWritePermission: widget.onRequestWritePermission,
            ),
            Text(
              l10n.activityEntryRecordingReadyBody,
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
            ActivityTypeSelector(
              types: [
                for (final type in state.activityTypes)
                  if (type.supportsLiveRecording) type,
              ],
              selectedType: selectedType,
              onSelectActivityType: widget.onSelectActivityType,
              errorText: state.validationErrorText(
                  ActivityEntryField.activityType, l10n),
            ),
            RecordingGuidancePanel(
              activityType: selectedType,
              readiness: readiness,
            ),
            ..._sensorSection(selectedType, baseEnabled, gpsFix),
            FilledButton.icon(
              onPressed: enabled ? () => _onStart(selectedType, readiness, gpsFix) : null,
              icon: const Icon(Icons.play_arrow, size: 18),
              label: Text(l10n.actionStart),
            ),
            OutlinedButton(
              onPressed: state.isSavingEntry || state.isImportingRoute
                  ? null
                  : widget.onChooseSource,
              child: Text(l10n.activityEntryChooseAnotherSource),
            ),
            ActivityEntryErrorText(state: state),
          ],
        ),
      ),
    );
  }

  /// The middle of the card depends on how this activity is measured.
  List<Widget> _sensorSection(
    ActivityEntryType selectedType,
    bool baseEnabled,
    PreRecordingGpsFixState gpsFix,
  ) {
    final l10n = AppLocalizations.of(context);

    if (selectedType.supportsGpsRoute) {
      return [
        SwitchListTile(
          contentPadding: EdgeInsets.zero,
          value: _withoutGps,
          onChanged: baseEnabled
              ? (value) => setState(() => _withoutGps = value)
              : null,
          title: Text(l10n.activityRecordingWithoutGpsTitle),
          subtitle: Text(l10n.activityRecordingWithoutGpsBody),
        ),
        // Shown once the switch is ON, so it reads as the consequence of a choice the
        // user has just made rather than as a scare in front of one they have not.
        //
        // It says what will be LOST, in full, because everything in that list is worked
        // out from a position and there will not be one: no map, no distance, no pace, no
        // elevation, no splits, and no steps for a type that counts them. A recording
        // that quietly came back missing half its statistics would feel like the app had
        // failed, and the user would have no way of knowing they had asked for it.
        if (_withoutGps)
          RecordingWithoutGpsWarning(countsSteps: selectedType.supportsStepCounting),
        // The fix status is about GPS, so it goes away with GPS. Leaving "waiting for a
        // fix" on screen under a recording that will never use one would be telling the
        // user to wait for something that is not coming.
        if (!_withoutGps)
          Align(
            alignment: Alignment.centerLeft,
            child: PreRecordingGpsFixStatus(state: gpsFix),
          ),
        ActivityRecordingLiveSensorStats(
          state: widget.recordingState,
          unitFormatter: widget.unitFormatter,
        ),
      ];
    }
    if (selectedType.isRepetitionLike) {
      return [
        TextField(
          controller: _restSeconds,
          enabled: baseEnabled,
          maxLines: 1,
          keyboardType: TextInputType.number,
          decoration: InputDecoration(
            labelText: l10n.activityEntryRecordingRestSecondsLabel,
            border: const OutlineInputBorder(),
          ),
        ),
        ActivityRecordingSensorStatusCard(
            deviceStatuses: widget.recordingState.bleDeviceStatuses),
      ];
    }
    if (selectedType.recordingSensor == ActivityRecordingSensor.ble) {
      return [
        ActivityRecordingSensorStatusCard(
            deviceStatuses: widget.recordingState.bleDeviceStatuses),
      ];
    }
    return const [];
  }

  /// Kotlin's Start button is really three actions: ask for the permission the
  /// activity needs, or — once nothing is missing — start.
  void _onStart(
    ActivityEntryType selectedType,
    RecordingSensorReadiness? readiness,
    PreRecordingGpsFixState gpsFix,
  ) {
    final withoutGps = selectedType.supportsGpsRoute && _withoutGps;

    if (selectedType.supportsStepCounting &&
        !(readiness?.hasActivityRecognitionPermission ?? false)) {
      widget.onRequestActivityRecognitionPermission();
      return;
    }
    // Not asked for when the user has said they do not want GPS. Demanding the location
    // permission for a recording that will never look at a location is exactly the kind
    // of thing that makes people distrust a health app.
    if (selectedType.supportsGpsRoute &&
        !withoutGps &&
        !gpsFix.hasPrecisePermission) {
      widget.onRequestLocationPermission();
      return;
    }
    final restSeconds = int.tryParse(_restSeconds.text.trim()) ?? 0;
    widget.onStartRecording(
      (selectedType.supportsGpsRoute && !withoutGps) ? gpsFix.initialFix : null,
      restSeconds < 0 ? 0 : restSeconds,
      withoutGps,
    );
  }
}

/// Kotlin `RecordingGuidancePanel`: how the phone counts this activity, and
/// whether it actually can on this device.
class RecordingGuidancePanel extends StatelessWidget {
  const RecordingGuidancePanel({
    super.key,
    required this.activityType,
    required this.readiness,
  });

  final ActivityEntryType activityType;
  final RecordingSensorReadiness? readiness;

  @override
  Widget build(BuildContext context) {
    if (!activityType.isRepetitionLike) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final guidance = switch (activityType.id) {
      'push_ups' => l10n.activityRecordingGuidancePushUps,
      'pull_ups' => l10n.activityRecordingGuidancePullUps,
      'rope_skipping' => l10n.activityRecordingGuidanceRopeSkipping,
      'trampoline_jumping' => l10n.activityRecordingGuidanceTrampolineJumping,
      'treadmill' => l10n.activityRecordingGuidanceTreadmill,
      _ => null,
    };
    if (guidance == null) return const SizedBox.shrink();

    // While readiness is still loading, say nothing rather than flash an error.
    final current = readiness;
    final sensorMissing = current != null && !current.hasRequiredSensor;
    final recognitionMissing = current != null &&
        activityType.supportsStepCounting &&
        !current.hasActivityRecognitionPermission;

    final String? statusText;
    final Color? statusColor;
    if (current == null) {
      statusText = null;
      statusColor = null;
    } else if (sensorMissing) {
      statusText = l10n.activityRecordingSensorUnavailableManual;
      statusColor = theme.colorScheme.error;
    } else if (recognitionMissing) {
      statusText = l10n.activityRecordingActivityRecognitionMissing;
      statusColor = theme.colorScheme.error;
    } else {
      statusText = l10n.activityRecordingSensorReady;
      statusColor = theme.colorScheme.primary;
    }

    return OpenVitalsSurface(
      style: OpenVitalsSurfaceStyle.metric,
      contentPadding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        spacing: 6,
        children: [
          Text(l10n.activityRecordingHowItWorks,
              style: theme.textTheme.titleSmall),
          Text(
            guidance,
            style: theme.textTheme.bodyMedium
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
          ),
          if (statusText != null)
            Text(statusText,
                style: theme.textTheme.bodySmall?.copyWith(color: statusColor)),
        ],
      ),
    );
  }
}

/// Kotlin `PreRecordingGpsFixStatus`: a location pin, tinted by whether the fix
/// is good enough to start on.
class PreRecordingGpsFixStatus extends StatelessWidget {
  const PreRecordingGpsFixStatus({super.key, required this.state});

  final PreRecordingGpsFixState state;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final isReady = state.latestPreciseFix != null;
    return OpenVitalsSurface(
      style: OpenVitalsSurfaceStyle.metric,
      borderRadius: const BorderRadius.all(Radius.circular(999)),
      contentPadding: const EdgeInsets.all(10),
      child: Tooltip(
        message: isReady
            ? l10n.activityEntryRecordingGpsFix
            : l10n.activityEntryRecordingGpsWaiting,
        child: Icon(
          Icons.my_location_outlined,
          size: 24,
          semanticLabel: isReady
              ? l10n.activityEntryRecordingGpsFix
              : l10n.activityEntryRecordingGpsWaiting,
          color: isReady
              ? activityRecordingAccentColor()
              : Theme.of(context).colorScheme.error,
        ),
      ),
    );
  }
}

/// What an activity recorded without GPS will not have.
///
/// Every line of this is derived from a position, and there will not be one. Saying so
/// before the run rather than discovering it after is the difference between a choice the
/// user made and an app that looks like it lost half their data.
///
/// It is not framed as an error. Recording without GPS is a legitimate thing to want —
/// people were already doing it by calling their runs treadmills — so the card states the
/// cost and ends with what survives, rather than trying to talk them out of it.
class RecordingWithoutGpsWarning extends StatelessWidget {
  const RecordingWithoutGpsWarning({super.key, required this.countsSteps});

  /// Whether this activity type counts steps while recording. It still does without GPS:
  /// the step detector reads the accelerometer, and never needed a position.
  final bool countsSteps;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final scheme = theme.colorScheme;

    return DecoratedBox(
      decoration: BoxDecoration(
        color: scheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          spacing: 12,
          children: [
            Icon(Icons.info_outline, size: 20, color: scheme.onSurfaceVariant),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    l10n.activityRecordingWithoutGpsWarningTitle,
                    style: theme.textTheme.labelLarge
                        ?.copyWith(fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    l10n.activityRecordingWithoutGpsWarningBody,
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: scheme.onSurfaceVariant),
                  ),
                  const SizedBox(height: 6),
                  // The barometer and the step detector never needed a position, so they
                  // keep running. What is lost is only what is genuinely derived from
                  // location.
                  Text(
                    countsSteps
                        ? l10n.activityRecordingWithoutGpsWarningKeptSteps
                        : l10n.activityRecordingWithoutGpsWarningKept,
                    style: theme.textTheme.bodySmall,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
