import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../di/providers.dart';
import '../../../domain/preferences/body_energy_calibration.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/components/ov_card.dart';

/// The current [BodyEnergyCalibration] from preferences, kept in sync with the
/// repository's listenable. Reactive: re-runs (re-seeding the card) whenever the
/// calibration is saved or reset. Self-contained to this card.
final bodyEnergyCalibrationCardProvider =
    Provider<BodyEnergyCalibration>((ref) {
  final repo = ref.watch(preferencesRepositoryProvider);
  final listenable = repo.bodyEnergyCalibrationListenable;
  void listener() => ref.invalidateSelf();
  listenable.addListener(listener);
  ref.onDispose(() => listenable.removeListener(listener));
  return listenable.value;
});

/// A self-contained "Body Energy calibration" settings card. 1:1 port of the
/// Kotlin `BodyEnergyCalibrationCard` (features/bodyenergy): a "use manual
/// zones" switch that reveals five heart-zone bpm lower-bound fields, a Save
/// button, and a "Use automatic" reset button.
///
/// Save mirrors `SettingsViewModel.updateBodyEnergyCalibration`: it forces
/// `setupCompleted = true` on the persisted calibration. "Use automatic"
/// mirrors `resetBodyEnergyCalibration`, persisting [BodyEnergyCalibration.automatic]
/// (also with `setupCompleted = true`). The onboarding-only "skip" action is
/// intentionally omitted in the settings context.
class BodyEnergyCalibrationCard extends ConsumerStatefulWidget {
  const BodyEnergyCalibrationCard({super.key});

  @override
  ConsumerState<BodyEnergyCalibrationCard> createState() =>
      _BodyEnergyCalibrationCardState();
}

class _BodyEnergyCalibrationCardState
    extends ConsumerState<BodyEnergyCalibrationCard> {
  final _zone1 = TextEditingController();
  final _zone2 = TextEditingController();
  final _zone3 = TextEditingController();
  final _zone4 = TextEditingController();
  final _zone5 = TextEditingController();

  bool _useManualZones = false;
  String? _seededSignature;

  @override
  void dispose() {
    _zone1.dispose();
    _zone2.dispose();
    _zone3.dispose();
    _zone4.dispose();
    _zone5.dispose();
    super.dispose();
  }

  void _seed(BodyEnergyCalibration calibration) {
    final zones = calibration.manualZoneThresholdsBpm;
    _zone1.text = zones?.zone1LowerBpm.toString() ?? '';
    _zone2.text = zones?.zone2LowerBpm.toString() ?? '';
    _zone3.text = zones?.zone3LowerBpm.toString() ?? '';
    _zone4.text = zones?.zone4LowerBpm.toString() ?? '';
    _zone5.text = zones?.zone5LowerBpm.toString() ?? '';
    _useManualZones = calibration.useManualZones;
    _seededSignature = calibration.signature();
  }

  /// Persists a calibration, forcing `setupCompleted = true` — matching the
  /// Kotlin `SettingsViewModel.updateBodyEnergyCalibration`.
  void _update(BodyEnergyCalibration calibration) {
    ref
        .read(preferencesRepositoryProvider)
        .setBodyEnergyCalibration(calibration.copyWith(setupCompleted: true));
  }

  void _save() {
    final calibration = BodyEnergyCalibration(
      manualZoneThresholdsBpm: HeartZoneThresholds(
        zone1LowerBpm: int.tryParse(_zone1.text.trim()) ?? 0,
        zone2LowerBpm: int.tryParse(_zone2.text.trim()) ?? 0,
        zone3LowerBpm: int.tryParse(_zone3.text.trim()) ?? 0,
        zone4LowerBpm: int.tryParse(_zone4.text.trim()) ?? 0,
        zone5LowerBpm: int.tryParse(_zone5.text.trim()) ?? 0,
      ),
      useManualZones: _useManualZones,
    ).normalized();
    _update(calibration);
  }

  void _useAutomatic() => _update(BodyEnergyCalibration.automatic);

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final calibration = ref.watch(bodyEnergyCalibrationCardProvider);
    if (_seededSignature != calibration.signature()) {
      _seed(calibration);
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
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Icon(
                      Icons.battery_charging_full_outlined,
                      color: theme.colorScheme.primary,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          l10n.bodyEnergyCalibrationTitle,
                          style: theme.textTheme.titleSmall,
                        ),
                        const SizedBox(height: 4),
                        Text(
                          l10n.bodyEnergyCalibrationBody,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          l10n.bodyEnergyCalibrationOptionalBody,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          l10n.bodyEnergyCalibrationManualZones,
                          style: theme.textTheme.bodyMedium,
                        ),
                        const SizedBox(height: 2),
                        Text(
                          l10n.bodyEnergyCalibrationManualZonesBody,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 12),
                  Switch(
                    value: _useManualZones,
                    onChanged: (value) =>
                        setState(() => _useManualZones = value),
                  ),
                ],
              ),
              if (_useManualZones) ...[
                _ZoneField(
                  controller: _zone1,
                  label: l10n.bodyEnergyCalibrationZone1,
                ),
                _ZoneField(
                  controller: _zone2,
                  label: l10n.bodyEnergyCalibrationZone2,
                ),
                _ZoneField(
                  controller: _zone3,
                  label: l10n.bodyEnergyCalibrationZone3,
                ),
                _ZoneField(
                  controller: _zone4,
                  label: l10n.bodyEnergyCalibrationZone4,
                ),
                _ZoneField(
                  controller: _zone5,
                  label: l10n.bodyEnergyCalibrationZone5,
                ),
              ],
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: _save,
                  child: Text(l10n.actionSave),
                ),
              ),
              const SizedBox(height: 8),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton(
                  onPressed: _useAutomatic,
                  child: Text(l10n.bodyEnergyCalibrationUseAuto),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ZoneField extends StatelessWidget {
  const _ZoneField({required this.controller, required this.label});

  final TextEditingController controller;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: TextField(
        controller: controller,
        keyboardType: TextInputType.number,
        textInputAction: TextInputAction.next,
        inputFormatters: [
          FilteringTextInputFormatter.allow(RegExp(r'[0-9]')),
          LengthLimitingTextInputFormatter(4),
        ],
        decoration: InputDecoration(
          border: const OutlineInputBorder(),
          labelText: label,
        ),
      ),
    );
  }
}
