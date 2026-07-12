import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/time/local_date.dart';
import '../../../../di/providers.dart';
import '../../../../domain/preferences/caffeine_preferences.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_card.dart';

/// The caffeine-model settings card, a 1:1 port of the Kotlin
/// `CaffeinePreferencesCard` (`SettingsCards.kt`) and the
/// `CaffeinePreferencesEditor` it delegates to (`CaffeinePreferencesEditor.kt`).
///
/// Self-contained: it reads the current [CaffeinePreferences] from the
/// [PreferencesRepository] into a local draft, edits the draft in place, and the
/// Save button writes the whole preferences object back through the repository
/// (mirroring Kotlin's `onSave(draft.copy(profileCompleted = true))`). It does
/// not touch the settings notifier.
///
/// Like the Kotlin editor, the per-field labels and enum labels are literal
/// strings (the Kotlin editor hardcodes them); only the card title/body and the
/// Save action use [AppLocalizations], matching the Kotlin `stringResource`
/// calls in the card wrapper.
class CaffeinePreferencesCard extends ConsumerStatefulWidget {
  const CaffeinePreferencesCard({super.key});

  @override
  ConsumerState<CaffeinePreferencesCard> createState() =>
      _CaffeinePreferencesCardState();
}

class _CaffeinePreferencesCardState
    extends ConsumerState<CaffeinePreferencesCard> {
  late CaffeinePreferences _draft;
  late final TextEditingController _halfLifeController;
  late final TextEditingController _absorptionController;
  late final TextEditingController _sleepThresholdController;
  late final TextEditingController _bedtimeController;

  @override
  void initState() {
    super.initState();
    _draft = ref.read(preferencesRepositoryProvider).caffeinePreferences();
    _halfLifeController =
        TextEditingController(text: _draft.halfLifeMinutes.toString());
    _absorptionController =
        TextEditingController(text: _draft.absorptionMinutes.toString());
    _sleepThresholdController =
        TextEditingController(text: _draft.sleepThresholdMg.toString());
    _bedtimeController = TextEditingController(text: _draft.bedtime.toString());
  }

  @override
  void dispose() {
    _halfLifeController.dispose();
    _absorptionController.dispose();
    _sleepThresholdController.dispose();
    _bedtimeController.dispose();
    super.dispose();
  }

  void _updateDraft(CaffeinePreferences next) => setState(() => _draft = next);

  void _save() {
    final repo = ref.read(preferencesRepositoryProvider);
    repo.setCaffeinePreferences(_draft.copyWith(profileCompleted: true));
    // Reseed from the normalized (clamped) stored value so the fields reflect
    // exactly what was persisted.
    final saved = repo.caffeinePreferences();
    setState(() => _draft = saved);
    _halfLifeController.text = saved.halfLifeMinutes.toString();
    _absorptionController.text = saved.absorptionMinutes.toString();
    _sleepThresholdController.text = saved.sleepThresholdMg.toString();
    _bedtimeController.text = saved.bedtime.toString();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final bodyProfile = ref.read(preferencesRepositoryProvider).bodyProfile();

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: OpenVitalsCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(
                    Icons.local_drink_outlined,
                    size: 20,
                    color: theme.colorScheme.primary,
                  ),
                  const SizedBox(width: 12),
                  Text(
                    l10n.settingsCaffeineTitle,
                    style: theme.textTheme.titleSmall,
                  ),
                ],
              ),
              const SizedBox(height: 4),
              Text(
                l10n.settingsCaffeineBody,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
              const SizedBox(height: 8),
              _NumberField(
                label: 'Half-life',
                suffix: 'min',
                controller: _halfLifeController,
                onValue: (value) =>
                    _updateDraft(_draft.copyWith(halfLifeMinutes: value)),
              ),
              _NumberField(
                label: 'Absorption',
                suffix: 'min',
                controller: _absorptionController,
                onValue: (value) =>
                    _updateDraft(_draft.copyWith(absorptionMinutes: value)),
              ),
              _NumberField(
                label: 'Sleep threshold',
                suffix: 'mg',
                controller: _sleepThresholdController,
                onValue: (value) =>
                    _updateDraft(_draft.copyWith(sleepThresholdMg: value)),
              ),
              _TimeField(
                label: 'Bedtime',
                controller: _bedtimeController,
                onValue: (value) =>
                    _updateDraft(_draft.copyWith(bedtime: value)),
              ),
              _EnumDropdown<CaffeineSleepSensitivity>(
                label: 'Sleep sensitivity',
                selected: _draft.sleepSensitivity,
                values: CaffeineSleepSensitivity.values,
                labelFor: _sleepSensitivityLabel,
                onSelect: (value) =>
                    _updateDraft(_draft.copyWith(sleepSensitivity: value)),
              ),
              _EnumDropdown<CaffeineAlcoholUse>(
                label: 'Alcohol',
                selected: _draft.alcoholUse,
                values: CaffeineAlcoholUse.values,
                labelFor: _alcoholUseLabel,
                onSelect: (value) =>
                    _updateDraft(_draft.copyWith(alcoholUse: value)),
              ),
              _EnumDropdown<CaffeineHabituation>(
                label: 'Caffeine habituation',
                selected: _draft.caffeineHabituation,
                values: CaffeineHabituation.values,
                labelFor: _habituationLabel,
                onSelect: (value) =>
                    _updateDraft(_draft.copyWith(caffeineHabituation: value)),
              ),
              _EnumDropdown<CaffeineGenotype>(
                label: 'CYP1A2',
                selected: _draft.cyp1a2Genotype,
                values: CaffeineGenotype.values,
                labelFor: _genotypeLabel,
                onSelect: (value) =>
                    _updateDraft(_draft.copyWith(cyp1a2Genotype: value)),
              ),
              _EnumDropdown<CaffeineGenotype>(
                label: 'AHR',
                selected: _draft.ahrGenotype,
                values: CaffeineGenotype.values,
                labelFor: _genotypeLabel,
                onSelect: (value) =>
                    _updateDraft(_draft.copyWith(ahrGenotype: value)),
              ),
              _EnumDropdown<CaffeineHormonalStatus>(
                label: 'Hormonal status',
                selected: _draft.hormonalStatus,
                values: CaffeineHormonalStatus.values,
                labelFor: _hormonalStatusLabel,
                onSelect: (value) =>
                    _updateDraft(_draft.copyWith(hormonalStatus: value)),
              ),
              _SwitchRow(
                label: 'Smoker',
                value: _draft.smoker,
                onChanged: (value) =>
                    _updateDraft(_draft.copyWith(smoker: value)),
              ),
              _SwitchRow(
                label: 'Liver impairment',
                value: _draft.liverImpairment,
                onChanged: (value) =>
                    _updateDraft(_draft.copyWith(liverImpairment: value)),
              ),
              _SwitchRow(
                label: 'Medication interaction',
                value: _draft.medicationInteraction,
                onChanged: (value) =>
                    _updateDraft(_draft.copyWith(medicationInteraction: value)),
              ),
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  'Effective half-life '
                  '${_draft.effectiveHalfLifeMinutes(bodyProfile)} min',
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(top: 12),
                child: Align(
                  alignment: Alignment.centerRight,
                  child: FilledButton.tonal(
                    onPressed: _save,
                    child: Text(l10n.actionSave),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// A numeric text field, port of the Kotlin editor's `PreferenceNumberField`.
/// Keeps the last valid value when the field is emptied (Kotlin only forwards a
/// parseable value).
class _NumberField extends StatelessWidget {
  const _NumberField({
    required this.label,
    required this.suffix,
    required this.controller,
    required this.onValue,
  });

  final String label;
  final String suffix;
  final TextEditingController controller;
  final ValueChanged<int> onValue;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: TextField(
        controller: controller,
        keyboardType: TextInputType.number,
        inputFormatters: [
          FilteringTextInputFormatter.digitsOnly,
          LengthLimitingTextInputFormatter(4),
        ],
        decoration: InputDecoration(
          border: const OutlineInputBorder(),
          labelText: label,
          suffixText: suffix,
        ),
        onChanged: (text) {
          final parsed = int.tryParse(text);
          if (parsed != null) onValue(parsed);
        },
      ),
    );
  }
}

/// A bedtime field, port of the Kotlin editor's `PreferenceTimeField`. Accepts
/// `HH:mm` text and forwards a [LocalTime] only when it parses to a valid time.
class _TimeField extends StatelessWidget {
  const _TimeField({
    required this.label,
    required this.controller,
    required this.onValue,
  });

  final String label;
  final TextEditingController controller;
  final ValueChanged<LocalTime> onValue;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: TextField(
        controller: controller,
        inputFormatters: [LengthLimitingTextInputFormatter(5)],
        decoration: InputDecoration(
          border: const OutlineInputBorder(),
          labelText: label,
        ),
        onChanged: (text) {
          final parsed = _parseLocalTime(text);
          if (parsed != null) onValue(parsed);
        },
      ),
    );
  }
}

LocalTime? _parseLocalTime(String text) {
  final match = RegExp(r'^(\d{1,2}):(\d{2})$').firstMatch(text.trim());
  if (match == null) return null;
  final hour = int.parse(match.group(1)!);
  final minute = int.parse(match.group(2)!);
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null;
  return LocalTime(hour, minute);
}

/// A read-only dropdown field, port of the Kotlin editor's
/// `PreferenceEnumDropdown`.
class _EnumDropdown<T> extends StatelessWidget {
  const _EnumDropdown({
    required this.label,
    required this.selected,
    required this.values,
    required this.labelFor,
    required this.onSelect,
  });

  final String label;
  final T selected;
  final List<T> values;
  final String Function(T) labelFor;
  final ValueChanged<T> onSelect;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: DropdownButtonFormField<T>(
        initialValue: selected,
        isExpanded: true,
        decoration: InputDecoration(
          border: const OutlineInputBorder(),
          labelText: label,
        ),
        items: [
          for (final value in values)
            DropdownMenuItem(value: value, child: Text(labelFor(value))),
        ],
        onChanged: (value) {
          if (value != null) onSelect(value);
        },
      ),
    );
  }
}

/// A label + switch row, port of the Kotlin editor's `PreferenceSwitchRow`.
class _SwitchRow extends StatelessWidget {
  const _SwitchRow({
    required this.label,
    required this.value,
    required this.onChanged,
  });

  final String label;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: Row(
        children: [
          Expanded(
            child: Text(label, style: theme.textTheme.bodyMedium),
          ),
          Switch(value: value, onChanged: onChanged),
        ],
      ),
    );
  }
}

// Enum display labels, mirroring the Kotlin `displayLabel()` extensions.

String _sleepSensitivityLabel(CaffeineSleepSensitivity value) {
  switch (value) {
    case CaffeineSleepSensitivity.low:
      return 'Low';
    case CaffeineSleepSensitivity.normal:
      return 'Normal';
    case CaffeineSleepSensitivity.high:
      return 'High';
    case CaffeineSleepSensitivity.insomnia:
      return 'Insomnia';
  }
}

String _alcoholUseLabel(CaffeineAlcoholUse value) {
  switch (value) {
    case CaffeineAlcoholUse.none:
      return 'None';
    case CaffeineAlcoholUse.occasional:
      return 'Occasional';
    case CaffeineAlcoholUse.regular:
      return 'Regular';
  }
}

String _habituationLabel(CaffeineHabituation value) {
  switch (value) {
    case CaffeineHabituation.low:
      return 'Low';
    case CaffeineHabituation.moderate:
      return 'Moderate';
    case CaffeineHabituation.high:
      return 'High';
  }
}

String _genotypeLabel(CaffeineGenotype value) {
  switch (value) {
    case CaffeineGenotype.unknown:
      return 'Unknown';
    case CaffeineGenotype.fast:
      return 'Fast';
    case CaffeineGenotype.normal:
      return 'Normal';
    case CaffeineGenotype.slow:
      return 'Slow';
  }
}

String _hormonalStatusLabel(CaffeineHormonalStatus value) {
  switch (value) {
    case CaffeineHormonalStatus.none:
      return 'None';
    case CaffeineHormonalStatus.oralContraceptive:
      return 'Oral contraceptive';
    case CaffeineHormonalStatus.pregnant:
      return 'Pregnant';
  }
}
