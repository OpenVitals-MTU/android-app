import 'package:flutter/material.dart';

import '../../../l10n/app_localizations.dart';
import '../../../ui/components/ov_surface.dart';
import 'activity_entry_state.dart';
import 'activity_entry_types.dart';
import 'activity_entry_ui_text.dart';

/// Port of the Kotlin `ActivityRepetitionInputs.kt`.
///
/// Renders nothing unless the selected type is repetition-like. Step-counted
/// types (walking, treadmill) get a single total field; rep-counted ones get the
/// Total/Sets switch and, in Sets mode, one reps+rest row per set.
class ActivityRepetitionInputs extends StatelessWidget {
  const ActivityRepetitionInputs({
    super.key,
    required this.state,
    required this.enabled,
    required this.repetitionTotal,
    required this.onModeChanged,
    required this.onTotalChanged,
    required this.onSetRepetitionsChanged,
    required this.onSetRestChanged,
    required this.onAddSet,
    required this.onRemoveSet,
  });

  final ActivityEntryUiState state;
  final bool enabled;
  final TextEditingController repetitionTotal;
  final ValueChanged<ActivityRepetitionEntryMode> onModeChanged;
  final ValueChanged<String> onTotalChanged;
  final void Function(int index, String text) onSetRepetitionsChanged;
  final void Function(int index, String text) onSetRestChanged;
  final VoidCallback onAddSet;
  final ValueChanged<int> onRemoveSet;

  @override
  Widget build(BuildContext context) {
    final type = state.selectedActivityType;
    if (!type.isRepetitionLike) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final errorText =
        state.validationErrorText(ActivityEntryField.repetitions, l10n);
    final isSteps = type.repetitionUnit == ActivityRepetitionUnit.steps;

    return OpenVitalsSurface(
      style: OpenVitalsSurfaceStyle.metric,
      contentPadding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        spacing: 10,
        children: [
          Text(
            isSteps
                ? l10n.activityEntryStepsTitle
                : l10n.activityEntryRepetitionsTitle,
            style: theme.textTheme.titleSmall,
          ),
          if (isSteps)
            _RepetitionField(
              controller: repetitionTotal,
              label: l10n.activityEntryStepsLabel,
              enabled: enabled,
              isError: errorText != null,
              onChanged: onTotalChanged,
            )
          else ...[
            _RepetitionModeButtons(
              selectedMode: state.repetitionMode,
              enabled: enabled,
              onModeChanged: onModeChanged,
            ),
            if (state.repetitionMode == ActivityRepetitionEntryMode.total)
              _RepetitionField(
                controller: repetitionTotal,
                label: l10n.activityEntryRepetitionsLabel,
                enabled: enabled,
                isError: errorText != null,
                onChanged: onTotalChanged,
              )
            else ...[
              for (var index = 0; index < state.repetitionSets.length; index++)
                _RepetitionSetRow(
                  // Keyed by position so a removal rebuilds the row below it
                  // with the right text rather than reusing a stale controller.
                  key: ValueKey('activity-repetition-set-$index'),
                  index: index,
                  input: state.repetitionSets[index],
                  enabled: enabled,
                  isError: errorText != null,
                  canRemove: enabled && state.repetitionSets.length > 1,
                  onRepetitionsChanged: (text) =>
                      onSetRepetitionsChanged(index, text),
                  onRestChanged: (text) => onSetRestChanged(index, text),
                  onRemove: () => onRemoveSet(index),
                ),
              OutlinedButton.icon(
                onPressed: enabled ? onAddSet : null,
                icon: const Icon(Icons.add, size: 18),
                label: Text(l10n.activityEntryAddSet),
              ),
            ],
          ],
          FieldErrorText(errorText),
        ],
      ),
    );
  }
}

class _RepetitionField extends StatelessWidget {
  const _RepetitionField({
    required this.controller,
    required this.label,
    required this.enabled,
    required this.isError,
    required this.onChanged,
  });

  final TextEditingController controller;
  final String label;
  final bool enabled;
  final bool isError;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      enabled: enabled,
      maxLines: 1,
      keyboardType: TextInputType.number,
      decoration: InputDecoration(
        labelText: label,
        border: const OutlineInputBorder(),
        // The message itself lives in the section's FieldErrorText, as in
        // Kotlin, where the field only flips its `isError` colours.
        errorText: isError ? '' : null,
        errorStyle: const TextStyle(height: 0, fontSize: 0),
      ),
      onChanged: onChanged,
    );
  }
}

/// Kotlin `RepetitionModeButtons`: the selected mode is a filled button, the
/// other an outlined one — not a SegmentedButton.
class _RepetitionModeButtons extends StatelessWidget {
  const _RepetitionModeButtons({
    required this.selectedMode,
    required this.enabled,
    required this.onModeChanged,
  });

  final ActivityRepetitionEntryMode selectedMode;
  final bool enabled;
  final ValueChanged<ActivityRepetitionEntryMode> onModeChanged;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Row(
      children: [
        Expanded(
          child: _ModeButton(
            label: l10n.activityEntryRepetitionModeTotal,
            selected: selectedMode == ActivityRepetitionEntryMode.total,
            enabled: enabled,
            onPressed: () => onModeChanged(ActivityRepetitionEntryMode.total),
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: _ModeButton(
            label: l10n.activityEntryRepetitionModeSets,
            selected: selectedMode == ActivityRepetitionEntryMode.sets,
            enabled: enabled,
            onPressed: () => onModeChanged(ActivityRepetitionEntryMode.sets),
          ),
        ),
      ],
    );
  }
}

class _ModeButton extends StatelessWidget {
  const _ModeButton({
    required this.label,
    required this.selected,
    required this.enabled,
    required this.onPressed,
  });

  final String label;
  final bool selected;
  final bool enabled;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final onTap = enabled ? onPressed : null;
    return selected
        ? FilledButton(onPressed: onTap, child: Text(label))
        : OutlinedButton(onPressed: onTap, child: Text(label));
  }
}

/// One set: reps, rest, and a delete button that is disabled for the last set.
class _RepetitionSetRow extends StatefulWidget {
  const _RepetitionSetRow({
    super.key,
    required this.index,
    required this.input,
    required this.enabled,
    required this.isError,
    required this.canRemove,
    required this.onRepetitionsChanged,
    required this.onRestChanged,
    required this.onRemove,
  });

  final int index;
  final ActivityRepetitionSetInput input;
  final bool enabled;
  final bool isError;
  final bool canRemove;
  final ValueChanged<String> onRepetitionsChanged;
  final ValueChanged<String> onRestChanged;
  final VoidCallback onRemove;

  @override
  State<_RepetitionSetRow> createState() => _RepetitionSetRowState();
}

class _RepetitionSetRowState extends State<_RepetitionSetRow> {
  late final TextEditingController _repetitions =
      TextEditingController(text: widget.input.repetitionsText);
  late final TextEditingController _rest =
      TextEditingController(text: widget.input.restMinutesText);

  @override
  void didUpdateWidget(_RepetitionSetRow oldWidget) {
    super.didUpdateWidget(oldWidget);
    // Only overwrite when the notifier's value diverged from what is typed —
    // assigning unconditionally would fight the cursor on every keystroke.
    _syncField(_repetitions, widget.input.repetitionsText);
    _syncField(_rest, widget.input.restMinutesText);
  }

  void _syncField(TextEditingController controller, String value) {
    if (controller.text == value) return;
    controller.value = TextEditingValue(
      text: value,
      selection: TextSelection.collapsed(offset: value.length),
    );
  }

  @override
  void dispose() {
    _repetitions.dispose();
    _rest.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Expanded(
          child: _RepetitionField(
            controller: _repetitions,
            label: l10n.activityEntrySetRepetitionsLabel(widget.index + 1),
            enabled: widget.enabled,
            isError: widget.isError,
            onChanged: widget.onRepetitionsChanged,
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: _RepetitionField(
            controller: _rest,
            label: l10n.activityEntrySetRestLabel,
            enabled: widget.enabled,
            isError: widget.isError,
            onChanged: widget.onRestChanged,
          ),
        ),
        IconButton(
          onPressed: widget.canRemove ? widget.onRemove : null,
          tooltip: l10n.cdDeleteEntry,
          icon: const Icon(Icons.delete_outline),
        ),
      ],
    );
  }
}
