import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../domain/preferences/body_energy_calibration.dart';
import '../../../../domain/preferences/body_profile.dart';
import '../../../../l10n/app_localizations.dart';
import '../../application/body_energy_calibration_view_model.dart';
import '../../application/body_profile_view_model.dart';

/// The heart-zone ladder and the learned tuning, with no card chrome of its own.
///
/// Rendered in two places that want different framing around the same controls:
///
///  * [BodyEnergyCalibrationCard] wraps it in a card with a header and its own
///    Save. That is the Body Energy setup gate, reached before the timeline
///    exists, and it is the only route to the birth year automatic zones need.
///  * The Body profile card renders it inline, under the birth year, weight and
///    height it already owns, behind a single Save. Two cards there meant two
///    headers and two Save buttons for one screenful of one person's facts.
///
/// Hence [showBirthYear] and a [save] the parent calls: the section owns the
/// form state either way, but not the decision about what a Save means around
/// it.
class BodyEnergyZonesSection extends ConsumerStatefulWidget {
  const BodyEnergyZonesSection({super.key, this.showBirthYear = true});

  /// Whether to draw the birth year field.
  ///
  /// False where the surrounding card already has one — two boxes for one
  /// number, disagreeing until you notice they are the same number. The
  /// REQUIREMENT does not depend on this: [save] still refuses without a birth
  /// year, and reads it from the stored profile when the field is not here.
  final bool showBirthYear;

  @override
  ConsumerState<BodyEnergyZonesSection> createState() =>
      BodyEnergyZonesSectionState();
}

class BodyEnergyZonesSectionState
    extends ConsumerState<BodyEnergyZonesSection> {
  final _zone1 = TextEditingController();
  final _zone2 = TextEditingController();
  final _zone3 = TextEditingController();
  final _zone4 = TextEditingController();
  final _zone5 = TextEditingController();
  final _birthYear = TextEditingController();

  bool _useManualZones = false;
  bool _birthYearMissing = false;
  String? _seededSignature;
  BodyProfile? _seededProfile;

  @override
  void dispose() {
    _zone1.dispose();
    _zone2.dispose();
    _zone3.dispose();
    _zone4.dispose();
    _zone5.dispose();
    _birthYear.dispose();
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

  void _seedProfile(BodyProfile profile) {
    _birthYear.text = profile.birthYear?.toString() ?? '';
    _seededProfile = profile;
  }

  /// Commits the zones, and the birth year when this section owns the field.
  ///
  /// Returns false when Body Energy needs a birth year and has none, having
  /// shown the reason inline. Manual zones ARE the ladder, so they need no age;
  /// automatic zones cannot be derived without one, and guessing produces a
  /// confidently wrong score rather than an honest gap.
  ///
  /// A parent that owns the birth year field must persist it BEFORE calling
  /// this, or the check below reads the profile as it was before the edit and
  /// refuses a year the user has just typed.
  bool save() {
    final birthYear = widget.showBirthYear
        ? int.tryParse(_birthYear.text.trim())
        : ref.read(bodyProfileCardProvider).profile.birthYear;
    final missing = !_useManualZones &&
        (birthYear == null ||
            birthYear < BodyProfile.minBirthYear ||
            birthYear > DateTime.now().year);
    if (mounted) setState(() => _birthYearMissing = missing);
    if (missing) return false;

    if (widget.showBirthYear) {
      ref.read(bodyProfileCardProvider.notifier).saveBirthYear(_birthYear.text);
    }
    ref.read(bodyEnergyCalibrationSettingsProvider.notifier).save(
          zone1: _zone1.text,
          zone2: _zone2.text,
          zone3: _zone3.text,
          zone4: _zone4.text,
          zone5: _zone5.text,
          useManualZones: _useManualZones,
        );
    return true;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final calibration =
        ref.watch(bodyEnergyCalibrationSettingsProvider).calibration;
    if (_seededSignature != calibration.signature()) {
      _seed(calibration);
    }
    final profile = ref.watch(bodyProfileCardProvider).profile;
    if (_seededProfile != profile) {
      _seedProfile(profile);
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (widget.showBirthYear)
          BodyEnergyNumberField(
            controller: _birthYear,
            label: l10n.bodyEnergyCalibrationBirthYear,
          ),
        if (_birthYearMissing)
          Padding(
            padding: const EdgeInsets.only(top: 4),
            child: Text(
              l10n.bodyEnergyCalibrationBirthYearRequired,
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.error),
            ),
          ),
        if (widget.showBirthYear || _birthYearMissing)
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
              onChanged: (value) => setState(() => _useManualZones = value),
            ),
          ],
        ),
        if (_useManualZones) ...[
          BodyEnergyNumberField(
            controller: _zone1,
            label: l10n.bodyEnergyCalibrationZone1,
          ),
          BodyEnergyNumberField(
            controller: _zone2,
            label: l10n.bodyEnergyCalibrationZone2,
          ),
          BodyEnergyNumberField(
            controller: _zone3,
            label: l10n.bodyEnergyCalibrationZone3,
          ),
          BodyEnergyNumberField(
            controller: _zone4,
            label: l10n.bodyEnergyCalibrationZone4,
          ),
          BodyEnergyNumberField(
            controller: _zone5,
            label: l10n.bodyEnergyCalibrationZone5,
          ),
        ],
        if (calibration.hasPersonalGains ||
            calibration.hasWatchObservations) ...[
          const SizedBox(height: 16),
          const Divider(height: 1),
          const SizedBox(height: 12),
          Text(
            l10n.bodyEnergyPersonalizationTitle,
            style: theme.textTheme.bodyMedium,
          ),
          const SizedBox(height: 2),
          Text(
            l10n.bodyEnergyPersonalizationWatchBody(
              calibration.watchObservationCount,
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
      ],
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

/// A bpm/year box: digits only, bounded length, outlined like the Body card's
/// own fields so the two read as one form rather than two pasted together.
class BodyEnergyNumberField extends StatelessWidget {
  const BodyEnergyNumberField({
    super.key,
    required this.controller,
    required this.label,
  });

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
