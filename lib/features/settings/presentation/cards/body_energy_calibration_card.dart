import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_card.dart';
import 'body_energy_zones_section.dart';

/// The current [BodyEnergyCalibration] on its own — the Body Energy detail
/// screen reads it through this card's import path, so it is re-exported here.
export '../../application/body_energy_calibration_view_model.dart'
    show bodyEnergyCalibrationCardProvider;

/// The standalone "Body Energy calibration" card: a header, the zone ladder and
/// learned tuning from [BodyEnergyZonesSection], and a Save.
///
/// This is the Body Energy SETUP GATE — the detail screen shows only this until
/// setup completes — so it stands alone, before any timeline exists, and carries
/// the birth year field because there is no other route to it from here.
///
/// The Body profile section renders the same section INLINE in its own card
/// instead: there, one screen holds one person's facts under one Save, and a
/// second header with a second Save button was two of each for no gain.
class BodyEnergyCalibrationCard extends ConsumerStatefulWidget {
  const BodyEnergyCalibrationCard({super.key});

  @override
  ConsumerState<BodyEnergyCalibrationCard> createState() =>
      _BodyEnergyCalibrationCardState();
}

class _BodyEnergyCalibrationCardState
    extends ConsumerState<BodyEnergyCalibrationCard> {
  final _zones = GlobalKey<BodyEnergyZonesSectionState>();

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
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
              BodyEnergyZonesSection(key: _zones),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () => _zones.currentState?.save(),
                  child: Text(l10n.actionSave),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
