import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../di/providers.dart';
import '../../../../domain/preferences/body_profile.dart';
import '../../../../domain/preferences/unit_system.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../state/app_providers.dart';
import '../../../../ui/components/ov_card.dart';

/// Kilograms-to-pounds factor, matching the Kotlin `PoundsPerKilogram` constant
/// used by `BodyProfileCard.kt` for the weight field round-trip.
const double _poundsPerKilogram = 2.2046226218;

/// The current [BodyProfile] from preferences, kept in sync with the
/// repository's listenable. Reactive: the provider re-runs (re-seeding the card
/// draft) whenever the profile is saved. Self-contained to this card so it does
/// not depend on `settings_view_model.dart`.
final bodyProfileCardProvider = Provider<BodyProfile>((ref) {
  final repo = ref.watch(preferencesRepositoryProvider);
  final listenable = repo.bodyProfileListenable;
  void listener() => ref.invalidateSelf();
  listenable.addListener(listener);
  ref.onDispose(() => listenable.removeListener(listener));
  return listenable.value;
});

/// A self-contained "Body profile" settings card. 1:1 port of the Kotlin
/// `BodyProfileCard` (features/settings/BodyProfileCard.kt): four optional
/// numeric fields (birth year, weight, resting HR, max HR) plus a Save button.
///
/// Weight is shown in the active unit system (kg/lb) and stored in kg; all
/// fields are nullable and normalised through [BodyProfile.normalized] before
/// being persisted, mirroring the Kotlin `onSave(draft.normalized())`.
class BodyProfileCard extends ConsumerStatefulWidget {
  const BodyProfileCard({super.key});

  @override
  ConsumerState<BodyProfileCard> createState() => _BodyProfileCardState();
}

class _BodyProfileCardState extends ConsumerState<BodyProfileCard> {
  final _birthYear = TextEditingController();
  final _weight = TextEditingController();
  final _restingHr = TextEditingController();
  final _maxHr = TextEditingController();

  BodyProfile? _seededProfile;
  UnitSystem? _seededUnit;

  @override
  void dispose() {
    _birthYear.dispose();
    _weight.dispose();
    _restingHr.dispose();
    _maxHr.dispose();
    super.dispose();
  }

  void _seed(BodyProfile profile, UnitSystem unit) {
    _birthYear.text = profile.birthYear?.toString() ?? '';
    final display = _displayWeight(profile.weightKg, unit);
    _weight.text = display != null ? display.toStringAsFixed(1) : '';
    _restingHr.text = profile.restingHeartRateBpm?.toString() ?? '';
    _maxHr.text = profile.maxHeartRateBpm?.toString() ?? '';
    _seededProfile = profile;
    _seededUnit = unit;
  }

  static double? _displayWeight(double? weightKg, UnitSystem unit) {
    if (weightKg == null) return null;
    return switch (unit) {
      UnitSystem.metric => weightKg,
      UnitSystem.imperial => weightKg * _poundsPerKilogram,
    };
  }

  static double? _storedWeightKg(double? weight, UnitSystem unit) {
    if (weight == null) return null;
    return switch (unit) {
      UnitSystem.metric => weight,
      UnitSystem.imperial => weight / _poundsPerKilogram,
    };
  }

  String _weightSuffix(UnitSystem unit) => switch (unit) {
        UnitSystem.metric => 'kg',
        UnitSystem.imperial => 'lb',
      };

  void _save(UnitSystem unit) {
    final profile = BodyProfile(
      birthYear: int.tryParse(_birthYear.text.trim()),
      weightKg: _storedWeightKg(double.tryParse(_weight.text.trim()), unit),
      restingHeartRateBpm: int.tryParse(_restingHr.text.trim()),
      maxHeartRateBpm: int.tryParse(_maxHr.text.trim()),
    ).normalized();
    ref.read(preferencesRepositoryProvider).setBodyProfile(profile);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final profile = ref.watch(bodyProfileCardProvider);
    final unit = ref.watch(unitSystemProvider);
    if (_seededProfile != profile || _seededUnit != unit) {
      _seed(profile, unit);
    }

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
                    Icons.person_outline,
                    size: 20,
                    color: theme.colorScheme.primary,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      l10n.settingsBodyProfileTitle,
                      style: theme.textTheme.titleSmall,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 4),
              Text(
                l10n.settingsBodyProfileBody,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
              _NumberField(
                controller: _birthYear,
                label: l10n.bodyEnergyCalibrationBirthYear,
                maxLength: 4,
              ),
              _NumberField(
                controller: _weight,
                label: l10n.settingsBodyProfileWeight,
                suffix: _weightSuffix(unit),
                maxLength: 5,
                allowDecimal: true,
              ),
              _NumberField(
                controller: _restingHr,
                label: l10n.bodyEnergyCalibrationRestingHr,
                suffix: 'bpm',
                maxLength: 4,
              ),
              _NumberField(
                controller: _maxHr,
                label: l10n.bodyEnergyCalibrationMaxHr,
                suffix: 'bpm',
                maxLength: 4,
              ),
              Padding(
                padding: const EdgeInsets.only(top: 12),
                child: Align(
                  alignment: Alignment.centerRight,
                  child: FilledButton.tonal(
                    onPressed: () => _save(unit),
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

class _NumberField extends StatelessWidget {
  const _NumberField({
    required this.controller,
    required this.label,
    this.suffix,
    required this.maxLength,
    this.allowDecimal = false,
  });

  final TextEditingController controller;
  final String label;
  final String? suffix;
  final int maxLength;
  final bool allowDecimal;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: TextField(
        controller: controller,
        keyboardType: allowDecimal
            ? const TextInputType.numberWithOptions(decimal: true)
            : TextInputType.number,
        inputFormatters: [
          FilteringTextInputFormatter.allow(
            allowDecimal ? RegExp(r'[0-9.]') : RegExp(r'[0-9]'),
          ),
          LengthLimitingTextInputFormatter(maxLength),
        ],
        decoration: InputDecoration(
          border: const OutlineInputBorder(),
          labelText: label,
          suffixText: suffix,
        ),
      ),
    );
  }
}
