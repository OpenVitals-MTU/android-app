import 'package:flutter/material.dart';

import '../../../domain/model/activity_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/components/ov_card.dart';
import 'activity_entry_notifier.dart';
import 'activity_entry_state.dart';
import '../../../domain/model/activity_entry_types.dart';
import 'activity_entry_ui_text.dart';
import 'activity_training_plan_section.dart';

/// Port of the Kotlin `ActivityPlanPickerCards.kt`: the two steps of the
/// "create from an existing plan" flow — pick an activity type that has plans,
/// then pick one of its plans.

/// Kotlin `MaxPlanPreviewParts`.
const int _maxPlanPreviewParts = 5;

/// Kotlin `ActivityPlanActivityPickerCard`.
class ActivityPlanActivityPickerCard extends StatelessWidget {
  const ActivityPlanActivityPickerCard({
    super.key,
    required this.state,
    required this.onSelectActivity,
    required this.onChooseSource,
  });

  final ActivityEntryUiState state;
  final ValueChanged<String> onSelectActivity;
  final VoidCallback onChooseSource;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    final seen = <String>{};
    final activityTypes = <ActivityEntryType>[];
    for (final plan in state.plannedWorkouts) {
      final type = plannedWorkoutToActivityEntryType(plan);
      if (type != null && seen.add(type.id)) activityTypes.add(type);
    }
    activityTypes.sort((a, b) => a.id.compareTo(b.id));

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          spacing: 12,
          children: [
            Text(l10n.activityEntryPlanActivityPickerTitle,
                style: theme.textTheme.titleMedium),
            if (state.isLoadingPlannedWorkouts)
              Text(
                l10n.activityEntryTrainingPlansLoading,
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              )
            else if (activityTypes.isEmpty)
              Text(
                l10n.activityEntryPlanActivityPickerEmpty,
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              )
            else
              for (final type in activityTypes)
                OutlinedButton.icon(
                  onPressed: () => onSelectActivity(type.id),
                  icon: const Icon(Icons.fitness_center_outlined, size: 18),
                  label: Text(type.label),
                ),
            OutlinedButton.icon(
              onPressed: onChooseSource,
              icon: const Icon(Icons.arrow_back, size: 18),
              label: Text(l10n.activityEntryChooseAnotherSource),
            ),
            ActivityEntryErrorText(state: state),
          ],
        ),
      ),
    );
  }
}

/// Kotlin `ActivityPlanPickerCard`.
class ActivityPlanPickerCard extends StatelessWidget {
  const ActivityPlanPickerCard({
    super.key,
    required this.state,
    required this.onSelectPlan,
    required this.onChooseActivity,
  });

  final ActivityEntryUiState state;
  final ValueChanged<String> onSelectPlan;
  final VoidCallback onChooseActivity;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final plans = plannedWorkoutsForActivityType(
      state.plannedWorkouts,
      state.selectedPlannedWorkoutActivityTypeId,
    );

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          spacing: 12,
          children: [
            Text(l10n.activityEntryPlanPickerTitle,
                style: theme.textTheme.titleMedium),
            if (plans.isEmpty)
              Text(
                l10n.activityEntryPlanPickerEmpty,
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              )
            else
              for (final plan in plans)
                _PlannedWorkoutButton(
                  plan: plan,
                  onPressed: () => onSelectPlan(plan.id),
                ),
            OutlinedButton.icon(
              onPressed: onChooseActivity,
              icon: const Icon(Icons.arrow_back, size: 18),
              label: Text(l10n.activityEntryPlanChooseActivity),
            ),
          ],
        ),
      ),
    );
  }
}

class _PlannedWorkoutButton extends StatelessWidget {
  const _PlannedWorkoutButton({required this.plan, required this.onPressed});

  final PlannedExerciseData plan;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final sets = plannedWorkoutToRepetitionSetInputs(plan);

    return OutlinedButton(
      onPressed: onPressed,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        spacing: 4,
        children: [
          Text(
            plannedWorkoutDisplayName(plan, l10n),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: theme.textTheme.titleSmall,
          ),
          Text(
            plannedWorkoutSummaryText(plan, l10n),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: theme.textTheme.bodyMedium
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
          ),
          _PlanSetPreview(sets: sets),
        ],
      ),
    );
  }
}

/// Kotlin `PlanSetPreview`: "12 reps • rest 60 sec • … • +3 more".
class _PlanSetPreview extends StatelessWidget {
  const _PlanSetPreview({required this.sets});

  final List<ActivityRepetitionSetInput> sets;

  @override
  Widget build(BuildContext context) {
    if (sets.isEmpty) return const SizedBox.shrink();
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    final parts = <String>[];
    for (var index = 0; index < sets.length; index++) {
      final set = sets[index];
      final reps = int.tryParse(set.repetitionsText);
      if (reps != null) parts.add(l10n.activityEntryPlanPreviewReps(reps));
      final rest = int.tryParse(set.restMinutesText);
      // The trailing rest of the last set is not shown: nothing follows it.
      if (rest != null && rest > 0 && index < sets.length - 1) {
        parts.add(l10n.activityEntryPlanPreviewRest(rest));
      }
    }
    if (parts.isEmpty) return const SizedBox.shrink();

    final shown = parts.take(_maxPlanPreviewParts).toList();
    final remaining = parts.length - shown.length;
    final text = StringBuffer(shown.join(' • '));
    if (remaining > 0) {
      text.write(' • ${l10n.activityEntryPlanPreviewMore(remaining)}');
    }

    return Text(
      text.toString(),
      maxLines: 2,
      overflow: TextOverflow.ellipsis,
      style: theme.textTheme.bodySmall
          ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
    );
  }
}
