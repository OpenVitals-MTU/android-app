import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/time/local_date.dart';
import '../../../../domain/preferences/caffeine_preferences.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_card.dart';
import '../../application/caffeine_preferences_view_model.dart';

/// The caffeine-model settings card, a 1:1 port of the Kotlin
/// `CaffeinePreferencesCard` (`SettingsCards.kt`) and the
/// `CaffeinePreferencesEditor` it delegates to (`CaffeinePreferencesEditor.kt`).
///
/// The draft and the save live in [CaffeinePreferencesViewModel]; the card owns
/// only its text controllers and reseeds them whenever the view-model bumps
/// `seedRevision` (a load, or a save that clamped a field).
///
/// Only the four settings that are genuinely about the FEATURE remain here:
/// bedtime and the sleep threshold are schedule and reporting choices, and
/// half-life and absorption are expert overrides of the base the physiology
/// multipliers scale. The nine physiological factors moved to
/// Settings > Body profile > Metabolism, where a genotype belongs; they are
/// still stored as [CaffeinePreferences] under their original keys.
///
/// The remaining per-field labels are literal strings, as the Kotlin editor
/// had them; only the card title/body and Save use [AppLocalizations].
class CaffeinePreferencesCard extends ConsumerStatefulWidget {
  const CaffeinePreferencesCard({super.key});

  @override
  ConsumerState<CaffeinePreferencesCard> createState() =>
      _CaffeinePreferencesCardState();
}

class _CaffeinePreferencesCardState
    extends ConsumerState<CaffeinePreferencesCard> {
  final _halfLifeController = TextEditingController();
  final _absorptionController = TextEditingController();
  final _sleepThresholdController = TextEditingController();
  final _bedtimeController = TextEditingController();

  int? _seededRevision;

  @override
  void dispose() {
    _halfLifeController.dispose();
    _absorptionController.dispose();
    _sleepThresholdController.dispose();
    _bedtimeController.dispose();
    super.dispose();
  }

  void _seed(CaffeinePreferences draft, int revision) {
    _halfLifeController.text = draft.halfLifeMinutes.toString();
    _absorptionController.text = draft.absorptionMinutes.toString();
    _sleepThresholdController.text = draft.sleepThresholdMg.toString();
    _bedtimeController.text = draft.bedtime.toString();
    _seededRevision = revision;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final cardState = ref.watch(caffeinePreferencesCardProvider);
    final notifier = ref.read(caffeinePreferencesCardProvider.notifier);
    final draft = cardState.draft;
    final bodyProfile = cardState.bodyProfile;
    if (_seededRevision != cardState.seedRevision) {
      _seed(draft, cardState.seedRevision);
    }

    void updateDraft(CaffeinePreferences next) => notifier.updateDraft(next);

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
                label: l10n.caffeineHalfLifeLabel,
                suffix: 'min',
                controller: _halfLifeController,
                onValue: (value) =>
                    updateDraft(draft.copyWith(halfLifeMinutes: value)),
              ),
              _NumberField(
                label: l10n.caffeineAbsorptionLabel,
                suffix: 'min',
                controller: _absorptionController,
                onValue: (value) =>
                    updateDraft(draft.copyWith(absorptionMinutes: value)),
              ),
              _NumberField(
                label: l10n.caffeineSleepThreshold,
                suffix: 'mg',
                controller: _sleepThresholdController,
                onValue: (value) =>
                    updateDraft(draft.copyWith(sleepThresholdMg: value)),
              ),
              _TimeField(
                label: l10n.caffeineBedtimeLabel,
                controller: _bedtimeController,
                onValue: (value) =>
                    updateDraft(draft.copyWith(bedtime: value)),
              ),
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  '${l10n.caffeineEffectiveHalfLife} '
                  '${draft.effectiveHalfLifeMinutes(bodyProfile)} min',
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
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

/// A label + switch row, port of the Kotlin editor's `PreferenceSwitchRow`.

// Enum display labels, mirroring the Kotlin `displayLabel()` extensions.





