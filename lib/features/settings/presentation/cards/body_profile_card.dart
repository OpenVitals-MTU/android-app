import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../domain/preferences/body_profile.dart';
import '../../../../domain/preferences/unit_system.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../state/app_providers.dart';
import '../../../../ui/components/ov_card.dart';
import '../../application/body_profile_view_model.dart';
import 'body_energy_zones_section.dart';
import 'metabolism_card.dart';

/// A self-contained "Body profile" settings card. 1:1 port of the Kotlin
/// `BodyProfileCard` (features/settings/BodyProfileCard.kt): four optional
/// numeric fields (birth year, weight, resting HR, max HR) plus a Save button.
///
/// Weight is shown in the active unit system (kg/lb) and stored in kg; the
/// conversion, the parse and [BodyProfile.normalized] live in
/// [BodyProfileViewModel] — the card only holds the four controllers and
/// reseeds them when the stored profile (or the unit) changes.
class BodyProfileCard extends ConsumerStatefulWidget {
  const BodyProfileCard({super.key});

  @override
  ConsumerState<BodyProfileCard> createState() => _BodyProfileCardState();
}

class _BodyProfileCardState extends ConsumerState<BodyProfileCard> {
  /// The heart zones and learned tuning render inside this card, so the screen
  /// carries one header and one Save for one person's facts. The key is how the
  /// single Save reaches the section's form state, which the section owns.
  final _zones = GlobalKey<BodyEnergyZonesSectionState>();

  final _birthYear = TextEditingController();
  final _weight = TextEditingController();
  final _height = TextEditingController();

  BodyProfile? _seededProfile;
  UnitSystem? _seededUnit;

  @override
  void dispose() {
    _birthYear.dispose();
    _weight.dispose();
    _height.dispose();
    super.dispose();
  }

  void _seed(BodyProfile profile, UnitSystem unit) {
    _birthYear.text = profile.birthYear?.toString() ?? '';
    final display = displayWeight(profile.weightKg, unit);
    _weight.text = display != null ? display.toStringAsFixed(1) : '';
    _height.text = profile.heightCm != null
        ? profile.heightCm!.toStringAsFixed(0)
        : '';
    _seededProfile = profile;
    _seededUnit = unit;
  }

  String _weightSuffix(UnitSystem unit) => switch (unit) {
        UnitSystem.metric => 'kg',
        UnitSystem.imperial => 'lb',
      };

  Future<void> _save(UnitSystem unit) async {
    // The profile FIRST, and awaited: the zones section refuses to save when
    // Body Energy has no birth year, and it reads that from the stored profile
    // because the field lives here rather than in the section. Saving the other
    // way round would reject a year the user has just typed.
    await ref.read(bodyProfileCardProvider.notifier).save(
          birthYear: _birthYear.text,
          weight: _weight.text,
          height: _height.text,
          unit: unit,
        );
    _zones.currentState?.save();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final cardState = ref.watch(bodyProfileCardProvider);
    final profile = cardState.profile;
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
              _SourceNote(source: cardState.weightSource),
              _NumberField(
                controller: _height,
                label: l10n.settingsBodyProfileHeight,
                suffix: 'cm',
                maxLength: 3,
              ),
              _SourceNote(source: cardState.heightSource),
              // No rule before the zones: they are the same subject as the
              // fields above and share the Save below, so a line there would
              // separate a thing from itself. The rules in this card mark where
              // one Save's reach ends -- before Metabolism, and inside the
              // section before the tuning it did not ask you for.
              const SizedBox(height: 20),
              BodyEnergyZonesSection(key: _zones, showBirthYear: false),
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
              // Metabolism is the same kind of thing — facts about this person —
              // so it shares the surface, separated by a rule rather than by a
              // second card. It keeps its OWN Save: these are stored under the
              // caffeine keys by a different view model, and one button writing
              // two unrelated stores would make a half-failed save invisible.
              const SizedBox(height: 16),
              const Divider(height: 1),
              const SizedBox(height: 12),
              const MetabolismCard(embedded: true),
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

/// Where the value above came from.
///
/// Without this the merge is invisible: the user cannot tell why editing weight
/// sometimes creates an entry on the Body screen, or why a number they never
/// typed is showing.
class _SourceNote extends StatelessWidget {
  const _SourceNote({required this.source});

  final BodyMetricSource source;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Padding(
      padding: const EdgeInsets.only(top: 4),
      child: Text(
        switch (source) {
          BodyMetricSource.measured => l10n.settingsBodyProfileFromRecord,
          BodyMetricSource.declared => l10n.settingsBodyProfileEnteredHere,
        },
        style: theme.textTheme.bodySmall
            ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
      ),
    );
  }
}
