import 'package:flutter/material.dart';

import '../../../core/presentation/unit_formatter.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/components/ov_card.dart';
import 'activity_entry_form_fields.dart';
import 'activity_entry_state.dart';
import '../../../domain/model/activity_entry_types.dart';
import 'activity_entry_ui_text.dart';
import 'activity_entry_write_request_builder.dart'
    show activityEntrySessionRange;
import 'activity_repetition_inputs.dart';
import 'activity_training_plan_section.dart';
import 'recording/activity_recorded_sensor_summary.dart';
import 'routeimport/activity_route_section.dart';

/// The controllers for the free-text fields, owned by the screen and synced
/// from the notifier. Grouped so the card takes one parameter instead of eight.
class ActivityEntryTextControllers {
  ActivityEntryTextControllers();

  final title = TextEditingController();
  final notes = TextEditingController();
  final duration = TextEditingController();
  final distance = TextEditingController();
  final elevation = TextEditingController();
  final activeCalories = TextEditingController();
  final totalCalories = TextEditingController();
  final repetitionTotal = TextEditingController();

  /// Pushes [state] into every controller, leaving the caret alone when the
  /// text already matches (otherwise typing would fight the sync).
  void syncFrom(ActivityEntryUiState state) {
    _set(title, state.titleText);
    _set(notes, state.notesText);
    _set(duration, state.durationMinutesText);
    _set(distance, state.distanceText);
    _set(elevation, state.elevationText);
    _set(activeCalories, state.activeCaloriesText);
    _set(totalCalories, state.totalCaloriesText);
    _set(repetitionTotal, state.repetitionTotalText);
  }

  static void _set(TextEditingController controller, String value) {
    if (controller.text == value) return;
    controller.value = TextEditingValue(
      text: value,
      selection: TextSelection.collapsed(offset: value.length),
    );
  }

  void dispose() {
    for (final controller in [
      title,
      notes,
      duration,
      distance,
      elevation,
      activeCalories,
      totalCalories,
      repetitionTotal,
    ]) {
      controller.dispose();
    }
  }
}

/// Every callback the entry card forwards to the controller. Mirrors the
/// parameter list of the Kotlin `ActivityEntryCard`.
class ActivityEntryCardCallbacks {
  const ActivityEntryCardCallbacks({
    required this.onSelectActivityType,
    required this.onTitleChanged,
    required this.onFeelingChanged,
    required this.onNotesChanged,
    required this.onStartDateChanged,
    required this.onStartTimeChanged,
    required this.onDurationChanged,
    required this.onRepetitionModeChanged,
    required this.onRepetitionTotalChanged,
    required this.onRepetitionSetRepetitionsChanged,
    required this.onRepetitionSetRestChanged,
    required this.onAddRepetitionSet,
    required this.onRemoveRepetitionSet,
    required this.onCreateNewPlannedWorkout,
    required this.onApplyPlannedWorkout,
    required this.onSavePlannedWorkout,
    required this.onUpdatePlannedWorkout,
    required this.onDistanceChanged,
    required this.onElevationChanged,
    required this.onActiveCaloriesChanged,
    required this.onTotalCaloriesChanged,
    required this.onClearRoute,
    required this.onChooseSource,
    required this.onRequestWritePermission,
    required this.onAddEntry,
    required this.onDiscardRecordingDraft,
  });

  final ValueChanged<ActivityEntryType> onSelectActivityType;
  final ValueChanged<String> onTitleChanged;
  final ValueChanged<ActivityEntryFeeling?> onFeelingChanged;
  final ValueChanged<String> onNotesChanged;
  final ValueChanged<String> onStartDateChanged;
  final ValueChanged<String> onStartTimeChanged;
  final ValueChanged<String> onDurationChanged;
  final ValueChanged<ActivityRepetitionEntryMode> onRepetitionModeChanged;
  final ValueChanged<String> onRepetitionTotalChanged;
  final void Function(int index, String text) onRepetitionSetRepetitionsChanged;
  final void Function(int index, String text) onRepetitionSetRestChanged;
  final VoidCallback onAddRepetitionSet;
  final ValueChanged<int> onRemoveRepetitionSet;
  final VoidCallback onCreateNewPlannedWorkout;
  final ValueChanged<String> onApplyPlannedWorkout;
  final VoidCallback onSavePlannedWorkout;
  final VoidCallback onUpdatePlannedWorkout;
  final ValueChanged<String> onDistanceChanged;
  final ValueChanged<String> onElevationChanged;
  final ValueChanged<String> onActiveCaloriesChanged;
  final ValueChanged<String> onTotalCaloriesChanged;
  final VoidCallback onClearRoute;
  final VoidCallback onChooseSource;
  final VoidCallback onRequestWritePermission;
  final VoidCallback onAddEntry;
  final VoidCallback onDiscardRecordingDraft;
}

/// Port of the Kotlin `ActivityEntryCard` (in `ActivityEntryForm.kt`), section
/// for section and in the same order.
class ActivityEntryCard extends StatelessWidget {
  const ActivityEntryCard({
    super.key,
    required this.state,
    required this.unitFormatter,
    required this.controllers,
    required this.callbacks,
  });

  final ActivityEntryUiState state;
  final UnitFormatter unitFormatter;
  final ActivityEntryTextControllers controllers;
  final ActivityEntryCardCallbacks callbacks;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final isEditMode = state.isEditMode;
    final canSave = state.canWrite &&
        !state.isSavingEntry &&
        !state.isCheckingPermission &&
        !state.isImportingRoute;
    final titleError = state.validationErrorText(ActivityEntryField.title, l10n);
    final durationError =
        state.validationErrorText(ActivityEntryField.duration, l10n);

    // A GPS route can only be attached to a type that supports one, so the
    // selector narrows once a route is imported.
    final hasRoute = state.importedRoute?.points.isNotEmpty ?? false;
    final selectableTypes = hasRoute
        ? [for (final type in state.activityTypes) if (type.supportsGpsRoute) type]
        : state.activityTypes;

    // Kotlin: the recorded heart-rate chart spans the session the entry's
    // date / time / duration fields describe.
    final sessionRange = activityEntrySessionRange(state);

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          spacing: 12,
          children: [
            ActivityEntryHeader(
              state: state,
              onRequestWritePermission: callbacks.onRequestWritePermission,
            ),
            if (!isEditMode)
              OutlinedButton(
                onPressed: state.isSavingEntry || state.isImportingRoute
                    ? null
                    : callbacks.onChooseSource,
                child: Text(l10n.activityEntryChooseAnotherSource),
              ),
            ActivityTypeSelector(
              types: selectableTypes,
              selectedType: state.selectedActivityType,
              onSelectActivityType: callbacks.onSelectActivityType,
              errorText:
                  state.validationErrorText(ActivityEntryField.activityType, l10n),
            ),
            ActivityTrainingPlanSection(
              state: state,
              enabled: !state.isSavingEntry && !state.isSavingPlannedWorkout,
              onCreateNewPlannedWorkout: callbacks.onCreateNewPlannedWorkout,
              onApplyPlannedWorkout: callbacks.onApplyPlannedWorkout,
            ),
            TextField(
              controller: controllers.title,
              enabled: !state.isSavingEntry,
              maxLines: 1,
              decoration: InputDecoration(
                labelText: l10n.activityEntryTitleLabel,
                border: const OutlineInputBorder(),
                errorText: titleError,
              ),
              onChanged: callbacks.onTitleChanged,
            ),
            ActivityStartDateTimeFields(
              state: state,
              enabled: !state.isSavingEntry,
              onStartDateChanged: callbacks.onStartDateChanged,
              onStartTimeChanged: callbacks.onStartTimeChanged,
            ),
            TextField(
              controller: controllers.duration,
              enabled: !state.isSavingEntry,
              maxLines: 1,
              keyboardType: TextInputType.number,
              decoration: InputDecoration(
                labelText: l10n.activityEntryDurationLabel,
                border: const OutlineInputBorder(),
                errorText: durationError,
              ),
              onChanged: callbacks.onDurationChanged,
            ),
            ActivityRepetitionInputs(
              state: state,
              enabled: !state.isSavingEntry,
              repetitionTotal: controllers.repetitionTotal,
              onModeChanged: callbacks.onRepetitionModeChanged,
              onTotalChanged: callbacks.onRepetitionTotalChanged,
              onSetRepetitionsChanged: callbacks.onRepetitionSetRepetitionsChanged,
              onSetRestChanged: callbacks.onRepetitionSetRestChanged,
              onAddSet: callbacks.onAddRepetitionSet,
              onRemoveSet: callbacks.onRemoveRepetitionSet,
            ),
            ActivityMetricInputs(
              state: state,
              unitSystem: unitFormatter.unitSystem(),
              enabled: true,
              distance: controllers.distance,
              elevation: controllers.elevation,
              activeCalories: controllers.activeCalories,
              totalCalories: controllers.totalCalories,
              onDistanceChanged: callbacks.onDistanceChanged,
              onElevationChanged: callbacks.onElevationChanged,
              onActiveCaloriesChanged: callbacks.onActiveCaloriesChanged,
              onTotalCaloriesChanged: callbacks.onTotalCaloriesChanged,
            ),
            _ActivityFeelingNotesSection(
              selectedFeeling: state.selectedFeeling,
              notes: controllers.notes,
              enabled: !state.isSavingEntry,
              onFeelingChanged: callbacks.onFeelingChanged,
              onNotesChanged: callbacks.onNotesChanged,
            ),
            ImportedActivityRouteSection(
              state: state,
              unitFormatter: unitFormatter,
              onClearRoute: callbacks.onClearRoute,
            ),
            ActivityRecordedSensorSummary(
              samples: state.recordedBleSamples,
              unitFormatter: unitFormatter,
              sessionStart: sessionRange?.$1,
              sessionEnd: sessionRange?.$2,
              savedHeartRateSamples: state.sessionHeartRateSamples,
            ),
            ActivityTrainingPlanActions(
              state: state,
              enabled: !state.isSavingEntry && !state.isSavingPlannedWorkout,
              onSavePlannedWorkout: callbacks.onSavePlannedWorkout,
              onUpdatePlannedWorkout: callbacks.onUpdatePlannedWorkout,
            ),
            FilledButton.icon(
              onPressed: canSave ? callbacks.onAddEntry : null,
              icon: const Icon(Icons.check, size: 18),
              label: Text(isEditMode ? l10n.actionSave : l10n.activityEntryAdd),
            ),
            if (state.isRecordingDraft && !isEditMode)
              OutlinedButton.icon(
                onPressed: state.isSavingEntry || state.isImportingRoute
                    ? null
                    : callbacks.onDiscardRecordingDraft,
                icon: const Icon(Icons.close, size: 18),
                label: Text(l10n.actionDiscard),
              ),
            ActivityEntryErrorText(state: state),
          ],
        ),
      ),
    );
  }
}

/// Kotlin `ActivityFeelingNotesSection`: four equal-width emoji chips (the label
/// is the accessibility text, not visible) over a multi-line notes field.
class _ActivityFeelingNotesSection extends StatelessWidget {
  const _ActivityFeelingNotesSection({
    required this.selectedFeeling,
    required this.notes,
    required this.enabled,
    required this.onFeelingChanged,
    required this.onNotesChanged,
  });

  final ActivityEntryFeeling? selectedFeeling;
  final TextEditingController notes;
  final bool enabled;
  final ValueChanged<ActivityEntryFeeling?> onFeelingChanged;
  final ValueChanged<String> onNotesChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 8,
      children: [
        Text(
          l10n.activityEntryFeelingLabel,
          style: theme.textTheme.labelMedium
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
        Row(
          children: [
            for (final feeling in ActivityEntryFeeling.values) ...[
              if (feeling != ActivityEntryFeeling.values.first)
                const SizedBox(width: 8),
              Expanded(
                child: Semantics(
                  label: activityFeelingLabel(feeling, l10n),
                  child: FilterChip(
                    selected: selectedFeeling == feeling,
                    onSelected: enabled
                        // Tapping the selected chip clears it.
                        ? (_) => onFeelingChanged(
                            feeling == selectedFeeling ? null : feeling)
                        : null,
                    label: Center(
                      child: Text(feeling.emoji,
                          style: theme.textTheme.titleLarge),
                    ),
                  ),
                ),
              ),
            ],
          ],
        ),
        TextField(
          controller: notes,
          enabled: enabled,
          minLines: 2,
          maxLines: 4,
          decoration: InputDecoration(
            labelText: l10n.activityEntryNotesLabel,
            border: const OutlineInputBorder(),
          ),
          onChanged: onNotesChanged,
        ),
      ],
    );
  }
}
