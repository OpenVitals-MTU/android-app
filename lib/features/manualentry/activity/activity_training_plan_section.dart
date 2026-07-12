import 'package:flutter/material.dart';

import '../../../domain/model/activity_models.dart';
import '../../../l10n/app_localizations.dart';
import 'activity_entry_view_model.dart';
import 'activity_entry_state.dart';

/// Port of the Kotlin `ActivityTrainingPlanSection.kt`. Both widgets render
/// nothing unless the selected activity type is set/rep based — training plans
/// only exist for those.

/// Kotlin `PlannedExerciseData.displayName()`.
String plannedWorkoutDisplayName(PlannedExerciseData plan, AppLocalizations l10n) =>
    plan.title ?? l10n.activityEntryTrainingPlanUnnamed;

/// Kotlin `PlannedExerciseData.summaryText()` / the plan-button summary.
String plannedWorkoutSummaryText(
  PlannedExerciseData plan,
  AppLocalizations l10n,
) {
  final sets = plannedWorkoutToRepetitionSetInputs(plan);
  var totalReps = 0;
  for (final set in sets) {
    totalReps += int.tryParse(set.repetitionsText) ?? 0;
  }
  if (sets.isEmpty) return l10n.plannedWorkoutBlocks(plan.blockCount);
  if (sets.length == 1) return l10n.activityEntryPlanOneSetSummary(totalReps);
  return l10n.activityEntryPlanSummary(sets.length, totalReps);
}

/// The plans matching [type], ordered by start time — Kotlin filters and sorts
/// the same way in both the section and the picker card.
List<PlannedExerciseData> plannedWorkoutsForActivityType(
  List<PlannedExerciseData> plans,
  String? activityTypeId,
) {
  final matching = [
    for (final plan in plans)
      if (plannedWorkoutToActivityEntryType(plan)?.id == activityTypeId) plan,
  ];
  matching.sort((a, b) => a.startTime.compareTo(b.startTime));
  return matching;
}

/// Kotlin `ActivityTrainingPlanSection`: a read-only dropdown listing "New plan"
/// followed by every plan for the selected activity type.
class ActivityTrainingPlanSection extends StatelessWidget {
  const ActivityTrainingPlanSection({
    super.key,
    required this.state,
    required this.enabled,
    required this.onCreateNewPlannedWorkout,
    required this.onApplyPlannedWorkout,
  });

  final ActivityEntryUiState state;
  final bool enabled;
  final VoidCallback onCreateNewPlannedWorkout;
  final ValueChanged<String> onApplyPlannedWorkout;

  /// The sentinel value for the "New plan" row, which is not a plan id.
  static const String _newPlanValue = '';

  @override
  Widget build(BuildContext context) {
    if (!state.selectedActivityType.supportsSetRepetitions) {
      return const SizedBox.shrink();
    }

    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final plans =
        plannedWorkoutsForActivityType(state.plannedWorkouts, state.selectedActivityType.id);
    final selectedId = state.selectedPlannedWorkoutId;
    final hasSelection = plans.any((plan) => plan.id == selectedId);
    final dropdownEnabled = enabled && !state.isLoadingPlannedWorkouts;

    return DropdownButtonFormField<String>(
      initialValue: hasSelection ? selectedId : _newPlanValue,
      isExpanded: true,
      decoration: InputDecoration(
        labelText: l10n.activityEntryTrainingPlanLabel,
        border: const OutlineInputBorder(),
        // Kotlin shows the loading text in the closed field itself.
        hintText: state.isLoadingPlannedWorkouts
            ? l10n.activityEntryTrainingPlansLoading
            : null,
      ),
      items: [
        DropdownMenuItem(
          value: _newPlanValue,
          child: Text(
            l10n.activityEntryTrainingPlanNew,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ),
        for (final plan in plans)
          DropdownMenuItem(
            value: plan.id,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  plannedWorkoutDisplayName(plan, l10n),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                Text(
                  plannedWorkoutSummaryText(plan, l10n),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ],
            ),
          ),
      ],
      onChanged: dropdownEnabled
          ? (value) {
              if (value == null || value == _newPlanValue) {
                onCreateNewPlannedWorkout();
              } else {
                onApplyPlannedWorkout(value);
              }
            }
          : null,
    );
  }
}

/// Kotlin `ActivityTrainingPlanActions`: "Save plan" while nothing is selected,
/// otherwise "Update plan" — enabled only once the form diverges from the plan.
class ActivityTrainingPlanActions extends StatelessWidget {
  const ActivityTrainingPlanActions({
    super.key,
    required this.state,
    required this.enabled,
    required this.onSavePlannedWorkout,
    required this.onUpdatePlannedWorkout,
  });

  final ActivityEntryUiState state;
  final bool enabled;
  final VoidCallback onSavePlannedWorkout;
  final VoidCallback onUpdatePlannedWorkout;

  @override
  Widget build(BuildContext context) {
    if (!state.selectedActivityType.supportsSetRepetitions) {
      return const SizedBox.shrink();
    }
    final l10n = AppLocalizations.of(context);

    if (state.selectedPlannedWorkoutId == null) {
      return OutlinedButton.icon(
        onPressed: enabled ? onSavePlannedWorkout : null,
        icon: const Icon(Icons.save_outlined, size: 18),
        label: Text(l10n.activityEntrySaveTrainingPlan),
      );
    }
    if (state.hasSelectedPlannedWorkoutChanges) {
      return FilledButton(
        onPressed: enabled ? onUpdatePlannedWorkout : null,
        child: Text(l10n.activityEntryUpdateTrainingPlan),
      );
    }
    // Selected but unchanged: shown disabled so the action stays discoverable.
    return OutlinedButton(
      onPressed: null,
      child: Text(l10n.activityEntryUpdateTrainingPlan),
    );
  }
}
