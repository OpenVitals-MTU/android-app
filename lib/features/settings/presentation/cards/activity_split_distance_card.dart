import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../domain/preferences/activity_split_distance.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../state/app_providers.dart';
import '../../../activity/presentation/activity_split_distance_label.dart';
import '../../application/activity_split_distance_view_model.dart';
import 'settings_controls.dart';

/// Picks the distance the activity detail screen cuts splits at when the
/// recording carries no laps of its own.
///
/// The presets follow the user's unit system (km or miles) but the VALUE IS
/// SAVED IN METERS — storage is metric here, imperial lives only at the display
/// boundary. A stored value that has no exact preset in the current unit system
/// (say 1 km viewed in imperial) highlights the nearest chip rather than showing
/// nothing selected.
class ActivitySplitDistanceCard extends ConsumerWidget {
  const ActivitySplitDistanceCard({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final unitSystem = ref.watch(unitSystemProvider);
    final formatter = ref.watch(unitFormatterProvider);
    final selectedMeters = ref.watch(activitySplitDistanceCardProvider);
    final notifier = ref.read(activitySplitDistanceCardProvider.notifier);
    final presets = ActivitySplitDistance.presetsFor(unitSystem);

    return SettingsCardShell(
      title: l10n.settingsActivitySplitDistanceTitle,
      body: l10n.settingsActivitySplitDistanceBody,
      children: [
        SettingsSegmentedChoice<double>(
          title: l10n.settingsActivitySplitDistanceChoice,
          options: presets,
          selected:
              ActivitySplitDistance.nearestPreset(selectedMeters, unitSystem),
          labelFor: (meters) => splitDistanceLabel(l10n, formatter, meters),
          onSelect: notifier.select,
        ),
      ],
    );
  }
}
