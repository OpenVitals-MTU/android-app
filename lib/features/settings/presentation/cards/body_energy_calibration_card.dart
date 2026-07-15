import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../domain/preferences/body_energy_calibration.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_card.dart';
import '../../application/body_energy_calibration_view_model.dart';

/// The current [BodyEnergyCalibration] on its own — the Body Energy detail
/// screen reads it through this card's import path, so it is re-exported here.
export '../../application/body_energy_calibration_view_model.dart'
    show bodyEnergyCalibrationCardProvider;

/// A self-contained "Body Energy calibration" settings card. 1:1 port of the
/// Kotlin `BodyEnergyCalibrationCard` (features/bodyenergy): a "use manual
/// zones" switch that reveals five heart-zone bpm lower-bound fields, a Save
/// button, and a "Use automatic" reset button.
///
/// The two writes live in [BodyEnergyCalibrationViewModel]; the card holds only
/// its five zone controllers and the manual-zones switch (form state, not
/// persisted until Save). The onboarding-only "skip" action is intentionally
/// omitted in the settings context.
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

  void _save() {
    ref
        .read(bodyEnergyCalibrationSettingsProvider.notifier)
        .save(
          zone1: _zone1.text,
          zone2: _zone2.text,
          zone3: _zone3.text,
          zone4: _zone4.text,
          zone5: _zone5.text,
          useManualZones: _useManualZones,
        );
  }

  void _useAutomatic() =>
      ref.read(bodyEnergyCalibrationSettingsProvider.notifier).useAutomatic();

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final calibration = ref
        .watch(bodyEnergyCalibrationSettingsProvider)
        .calibration;
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
              if (calibration.hasPersonalGains ||
                  calibration.feelCheckCount > 0) ...[
                const SizedBox(height: 16),
                const Divider(height: 1),
                const SizedBox(height: 12),
                Text(
                  l10n.bodyEnergyPersonalizationTitle,
                  style: theme.textTheme.bodyMedium,
                ),
                const SizedBox(height: 2),
                Text(
                  l10n.bodyEnergyPersonalizationBody(
                    calibration.feelCheckCount,
                  ),
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 10),
                _GainRow(
                  label: l10n.bodyEnergyGainActivity,
                  value: calibration.activityDrainGain,
                ),
                _GainRow(
                  label: l10n.bodyEnergyGainSleep,
                  value: calibration.sleepChargeGain,
                ),
                _GainRow(
                  label: l10n.bodyEnergyGainBasal,
                  value: calibration.basalDrainGain,
                ),
                _GainRow(
                  label: l10n.bodyEnergyGainStress,
                  value: calibration.stressDrainGain,
                ),
                const SizedBox(height: 8),
                Align(
                  alignment: Alignment.centerLeft,
                  child: TextButton(
                    onPressed: () => ref
                        .read(bodyEnergyCalibrationSettingsProvider.notifier)
                        .resetPersonalization(),
                    child: Text(l10n.bodyEnergyPersonalizationReset),
                  ),
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

/// One learned gain, shown as a plain multiplier the user can read.
class _GainRow extends StatelessWidget {
  const _GainRow({required this.label, required this.value});

  final String label;
  final double value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Row(
        children: [
          Expanded(child: Text(label, style: theme.textTheme.bodySmall)),
          Text(
            '${value.toStringAsFixed(2)}×',
            style: theme.textTheme.bodySmall?.copyWith(
              fontWeight: FontWeight.w600,
              fontFeatures: const [FontFeature.tabularFigures()],
            ),
          ),
        ],
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
