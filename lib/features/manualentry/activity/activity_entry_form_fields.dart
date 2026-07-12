import 'package:flutter/material.dart';

import '../../../core/time/local_date.dart';
import '../../../domain/preferences/unit_system.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/components/health_date_picker.dart';
import '../../../ui/theme/app_colors.dart';
import 'activity_entry_state.dart';
import '../../../domain/model/activity_entry_types.dart';
import 'activity_entry_ui_text.dart';

/// Port of the Kotlin `ActivityEntryFormFields.kt`.

/// Kotlin `ActivityEntryHeader`: the run icon, the title, a subtitle that
/// doubles as the permission explainer, and a Grant action when the write
/// permission is missing.
class ActivityEntryHeader extends StatelessWidget {
  const ActivityEntryHeader({
    super.key,
    required this.state,
    required this.onRequestWritePermission,
  });

  final ActivityEntryUiState state;
  final VoidCallback onRequestWritePermission;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        const Icon(Icons.directions_run_outlined,
            size: 22, color: AppColors.workout),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(l10n.manualEntryActivityTitle,
                    style: theme.textTheme.titleSmall),
                Text(
                  state.canWrite
                      ? l10n.activityEntrySubtitle
                      : l10n.activityEntryPermissionNeeded,
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ],
            ),
          ),
        ),
        if (!state.canWrite && !state.isCheckingPermission)
          OutlinedButton(
            onPressed: onRequestWritePermission,
            child: Text(l10n.actionGrant),
          ),
      ],
    );
  }
}

/// Kotlin `ActivityTypeSelector`: a read-only dropdown, not a chip row.
class ActivityTypeSelector extends StatelessWidget {
  const ActivityTypeSelector({
    super.key,
    required this.types,
    required this.selectedType,
    required this.onSelectActivityType,
    required this.errorText,
  });

  final List<ActivityEntryType> types;
  final ActivityEntryType selectedType;
  final ValueChanged<ActivityEntryType> onSelectActivityType;
  final String? errorText;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    // Filtering by imported route can drop the selected type from the list; a
    // DropdownButton whose value is absent from its items asserts.
    final value = types.contains(selectedType) ? selectedType : null;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        DropdownButtonFormField<ActivityEntryType>(
          initialValue: value,
          isExpanded: true,
          decoration: InputDecoration(
            labelText: l10n.activityEntryTypeLabel,
            border: const OutlineInputBorder(),
            errorText: errorText == null ? null : '',
            errorStyle: const TextStyle(height: 0, fontSize: 0),
          ),
          items: [
            for (final type in types)
              DropdownMenuItem(
                value: type,
                child: Text(type.label, overflow: TextOverflow.ellipsis),
              ),
          ],
          onChanged: (type) {
            if (type != null) onSelectActivityType(type);
          },
        ),
        const SizedBox(height: 4),
        FieldErrorText(errorText),
      ],
    );
  }
}

/// Kotlin `ActivityStartDateTimeFields`: two picker buttons side by side, each
/// with its own error line.
class ActivityStartDateTimeFields extends StatelessWidget {
  const ActivityStartDateTimeFields({
    super.key,
    required this.state,
    required this.enabled,
    required this.onStartDateChanged,
    required this.onStartTimeChanged,
  });

  final ActivityEntryUiState state;
  final bool enabled;
  final ValueChanged<String> onStartDateChanged;
  final ValueChanged<String> onStartTimeChanged;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final now = DateTime.now();
    final selectedDate = parseActivityStartDate(state.startDateText) ?? now;
    final selectedTime = parseActivityStartTime(state.startTimeText) ??
        TimeOfDay(hour: now.hour, minute: now.minute);
    final dateError = state.validationErrorText(ActivityEntryField.startDate, l10n);
    final timeError = state.validationErrorText(ActivityEntryField.startTime, l10n);
    final locale = Localizations.localeOf(context).toLanguageTag();

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              ActivityPickerField(
                label: l10n.activityEntryStartDateLabel,
                value: localizedActivityDateText(selectedDate, locale),
                icon: Icons.calendar_month_outlined,
                enabled: enabled,
                isError: dateError != null,
                onTap: () => _pickDate(context, selectedDate),
              ),
              const SizedBox(height: 4),
              FieldErrorText(dateError),
            ],
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              ActivityPickerField(
                label: l10n.activityEntryStartTimeLabel,
                value: localizedActivityTimeText(context, selectedTime),
                icon: Icons.schedule_outlined,
                enabled: enabled,
                isError: timeError != null,
                onTap: () => _pickTime(context, selectedTime),
              ),
              const SizedBox(height: 4),
              FieldErrorText(timeError),
            ],
          ),
        ),
      ],
    );
  }

  Future<void> _pickDate(BuildContext context, DateTime selectedDate) async {
    final picked = await showHealthDatePicker(
      context,
      selectedDate: LocalDate.fromDateTime(selectedDate),
    );
    if (picked == null) return;
    onStartDateChanged(
      '${picked.year.toString().padLeft(4, '0')}-'
      '${picked.month.toString().padLeft(2, '0')}-'
      '${picked.day.toString().padLeft(2, '0')}',
    );
  }

  Future<void> _pickTime(BuildContext context, TimeOfDay selectedTime) async {
    final l10n = AppLocalizations.of(context);
    final picked = await showTimePicker(
      context: context,
      initialTime: selectedTime,
      helpText: l10n.activityEntrySelectTime,
      confirmText: l10n.actionSelect,
      cancelText: l10n.actionCancel,
    );
    if (picked == null) return;
    onStartTimeChanged(formatActivityEntryTime(picked));
  }
}

/// Kotlin `ActivityPickerField`: an outlined button that reads like a text
/// field — a small label over the current value, tinted red when invalid.
class ActivityPickerField extends StatelessWidget {
  const ActivityPickerField({
    super.key,
    required this.label,
    required this.value,
    required this.icon,
    required this.enabled,
    required this.isError,
    required this.onTap,
  });

  final String label;
  final String value;
  final IconData icon;
  final bool enabled;
  final bool isError;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final theme = Theme.of(context);
    final contentColor = isError ? scheme.error : scheme.onSurfaceVariant;
    return OutlinedButton(
      onPressed: enabled ? onTap : null,
      style: OutlinedButton.styleFrom(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        side: BorderSide(color: isError ? scheme.error : scheme.outline),
      ),
      child: Row(
        children: [
          Icon(icon, size: 20, color: contentColor),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(left: 8),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    label,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.labelSmall?.copyWith(color: contentColor),
                  ),
                  Text(
                    value,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodyMedium
                        ?.copyWith(color: scheme.onSurface),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Kotlin `ActivityMetricInputs`: distance and elevation only when the selected
/// type supports them, then the two calorie fields.
///
/// Distance/elevation are typed in the user's unit and canonicalized to
/// metres by the write-request builder, matching Kotlin.
class ActivityMetricInputs extends StatelessWidget {
  const ActivityMetricInputs({
    super.key,
    required this.state,
    required this.unitSystem,
    required this.enabled,
    required this.distance,
    required this.elevation,
    required this.activeCalories,
    required this.totalCalories,
    required this.onDistanceChanged,
    required this.onElevationChanged,
    required this.onActiveCaloriesChanged,
    required this.onTotalCaloriesChanged,
  });

  final ActivityEntryUiState state;
  final UnitSystem unitSystem;
  final bool enabled;
  final TextEditingController distance;
  final TextEditingController elevation;
  final TextEditingController activeCalories;
  final TextEditingController totalCalories;
  final ValueChanged<String> onDistanceChanged;
  final ValueChanged<String> onElevationChanged;
  final ValueChanged<String> onActiveCaloriesChanged;
  final ValueChanged<String> onTotalCaloriesChanged;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final type = state.selectedActivityType;
    final fieldsEnabled = enabled && !state.isSavingEntry;
    final imperial = unitSystem == UnitSystem.imperial;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (type.supportsDistance || type.supportsElevation) ...[
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (type.supportsDistance)
                Expanded(
                  child: _DecimalField(
                    controller: distance,
                    label: l10n.activityEntryDistanceLabel(imperial ? 'mi' : 'km'),
                    enabled: fieldsEnabled,
                    errorText: state.validationErrorText(
                        ActivityEntryField.distance, l10n),
                    onChanged: onDistanceChanged,
                  ),
                ),
              if (type.supportsDistance && type.supportsElevation)
                const SizedBox(width: 8),
              if (type.supportsElevation)
                Expanded(
                  child: _DecimalField(
                    controller: elevation,
                    label: l10n.activityEntryElevationLabel(imperial ? 'ft' : 'm'),
                    enabled: fieldsEnabled,
                    errorText: state.validationErrorText(
                        ActivityEntryField.elevation, l10n),
                    onChanged: onElevationChanged,
                  ),
                ),
            ],
          ),
          const SizedBox(height: 12),
        ],
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: _DecimalField(
                controller: activeCalories,
                label: l10n.metricActiveCalories,
                enabled: fieldsEnabled,
                errorText: state.validationErrorText(
                    ActivityEntryField.activeCalories, l10n),
                onChanged: onActiveCaloriesChanged,
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _DecimalField(
                controller: totalCalories,
                label: l10n.metricCaloriesBurned,
                enabled: fieldsEnabled,
                errorText: state.validationErrorText(
                    ActivityEntryField.totalCalories, l10n),
                onChanged: onTotalCaloriesChanged,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _DecimalField extends StatelessWidget {
  const _DecimalField({
    required this.controller,
    required this.label,
    required this.enabled,
    required this.errorText,
    required this.onChanged,
  });

  final TextEditingController controller;
  final String label;
  final bool enabled;
  final String? errorText;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      enabled: enabled,
      maxLines: 1,
      keyboardType: const TextInputType.numberWithOptions(decimal: true),
      decoration: InputDecoration(
        labelText: label,
        border: const OutlineInputBorder(),
        errorText: errorText,
      ),
      onChanged: onChanged,
    );
  }
}
