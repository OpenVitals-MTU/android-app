import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../domain/preferences/caffeine_preferences.dart';
import '../../../../l10n/app_localizations.dart';
import '../../application/caffeine_preferences_view_model.dart';
import 'settings_controls.dart';

/// The physiological half of the caffeine model, surfaced under Body profile.
///
/// These nine settings are facts about the person — a genotype, a clinical
/// condition, a pregnancy — not preferences about how a feature should behave.
/// They lived under Settings › Nutrition › Caffeine model, which is why the
/// app's only pregnancy input was three taps deep inside a coffee screen.
///
/// The storage deliberately did NOT move with them: they are still
/// [CaffeinePreferences] behind their original `caffeine_*` keys. Renaming keys
/// to match a menu is churn with a real downside — `CaffeineStore` carries a
/// "never rename one" warning, and a botched migration silently loses someone's
/// genotype and hormonal status. The on-disk name is invisible; the section a
/// control appears in is not.
class MetabolismCard extends ConsumerWidget {
  const MetabolismCard({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(caffeinePreferencesCardProvider);
    final notifier = ref.read(caffeinePreferencesCardProvider.notifier);
    final draft = state.draft;

    void update(CaffeinePreferences next) => notifier.updateDraft(next);

    return SettingsCardShell(
      title: l10n.settingsMetabolismTitle,
      body: l10n.settingsMetabolismBody,
      children: [
        SettingsEnumDropdown<CaffeineSleepSensitivity>(
          label: l10n.settingsMetabolismSleepSensitivity,
          selected: draft.sleepSensitivity,
          values: CaffeineSleepSensitivity.values,
          labelFor: (value) => _sensitivityLabel(l10n, value),
          onSelect: (value) =>
              update(draft.copyWith(sleepSensitivity: value)),
        ),
        SettingsEnumDropdown<CaffeineAlcoholUse>(
          label: l10n.settingsMetabolismAlcohol,
          selected: draft.alcoholUse,
          values: CaffeineAlcoholUse.values,
          labelFor: (value) => _alcoholLabel(l10n, value),
          onSelect: (value) => update(draft.copyWith(alcoholUse: value)),
        ),
        SettingsEnumDropdown<CaffeineHabituation>(
          label: l10n.settingsMetabolismHabituation,
          selected: draft.caffeineHabituation,
          values: CaffeineHabituation.values,
          labelFor: (value) => _habituationLabel(l10n, value),
          onSelect: (value) =>
              update(draft.copyWith(caffeineHabituation: value)),
        ),
        SettingsEnumDropdown<CaffeineGenotype>(
          label: l10n.settingsMetabolismCyp1a2,
          selected: draft.cyp1a2Genotype,
          values: CaffeineGenotype.values,
          labelFor: (value) => _genotypeLabel(l10n, value),
          onSelect: (value) => update(draft.copyWith(cyp1a2Genotype: value)),
        ),
        SettingsEnumDropdown<CaffeineGenotype>(
          label: l10n.settingsMetabolismAhr,
          selected: draft.ahrGenotype,
          values: CaffeineGenotype.values,
          labelFor: (value) => _genotypeLabel(l10n, value),
          onSelect: (value) => update(draft.copyWith(ahrGenotype: value)),
        ),
        SettingsEnumDropdown<CaffeineHormonalStatus>(
          label: l10n.settingsMetabolismHormonalStatus,
          selected: draft.hormonalStatus,
          values: CaffeineHormonalStatus.values,
          labelFor: (value) => _hormonalLabel(l10n, value),
          onSelect: (value) => update(draft.copyWith(hormonalStatus: value)),
        ),
        SettingsSwitchRow(
          title: l10n.settingsMetabolismSmoker,
          value: draft.smoker,
          onChanged: (value) => update(draft.copyWith(smoker: value)),
        ),
        SettingsSwitchRow(
          title: l10n.settingsMetabolismLiverImpairment,
          value: draft.liverImpairment,
          onChanged: (value) => update(draft.copyWith(liverImpairment: value)),
        ),
        SettingsSwitchRow(
          title: l10n.settingsMetabolismMedicationInteraction,
          value: draft.medicationInteraction,
          onChanged: (value) =>
              update(draft.copyWith(medicationInteraction: value)),
        ),
        Padding(
          padding: const EdgeInsets.only(top: 12),
          child: Align(
            alignment: Alignment.centerRight,
            child: FilledButton.tonal(
              onPressed: notifier.save,
              child: Text(l10n.actionSave),
            ),
          ),
        ),
      ],
    );
  }
}

// The enum labels. `sleepSensitivity` is presented as caffeine SENSITIVITY
// rather than sleep sensitivity on purpose: it never touches the sleep
// threshold, it only multiplies the elimination half-life, so the old name
// promised something the model does not do.

String _sensitivityLabel(AppLocalizations l10n, CaffeineSleepSensitivity v) =>
    switch (v) {
      CaffeineSleepSensitivity.low => l10n.settingsMetabolismSensitivityLow,
      CaffeineSleepSensitivity.normal =>
        l10n.settingsMetabolismSensitivityNormal,
      CaffeineSleepSensitivity.high => l10n.settingsMetabolismSensitivityHigh,
      CaffeineSleepSensitivity.insomnia =>
        l10n.settingsMetabolismSensitivityInsomnia,
    };

String _alcoholLabel(AppLocalizations l10n, CaffeineAlcoholUse v) => switch (v) {
      CaffeineAlcoholUse.none => l10n.settingsMetabolismAlcoholNone,
      CaffeineAlcoholUse.occasional => l10n.settingsMetabolismAlcoholOccasional,
      CaffeineAlcoholUse.regular => l10n.settingsMetabolismAlcoholRegular,
    };

String _habituationLabel(AppLocalizations l10n, CaffeineHabituation v) =>
    switch (v) {
      CaffeineHabituation.low => l10n.settingsMetabolismHabituationLow,
      CaffeineHabituation.moderate =>
        l10n.settingsMetabolismHabituationModerate,
      CaffeineHabituation.high => l10n.settingsMetabolismHabituationHigh,
    };

String _genotypeLabel(AppLocalizations l10n, CaffeineGenotype v) => switch (v) {
      CaffeineGenotype.unknown => l10n.settingsMetabolismGenotypeUnknown,
      CaffeineGenotype.fast => l10n.settingsMetabolismGenotypeFast,
      CaffeineGenotype.normal => l10n.settingsMetabolismGenotypeNormal,
      CaffeineGenotype.slow => l10n.settingsMetabolismGenotypeSlow,
    };

String _hormonalLabel(AppLocalizations l10n, CaffeineHormonalStatus v) =>
    switch (v) {
      CaffeineHormonalStatus.none => l10n.settingsMetabolismHormonalNone,
      CaffeineHormonalStatus.oralContraceptive =>
        l10n.settingsMetabolismHormonalOralContraceptive,
      CaffeineHormonalStatus.pregnant => l10n.settingsMetabolismHormonalPregnant,
    };
