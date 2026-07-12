import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../l10n/app_localizations.dart';
import '../../../../domain/model/activity_entry_types.dart';
import '../../application/favorite_activity_view_model.dart';
import 'settings_controls.dart';

/// The default-activity picker card, a 1:1 port of the Kotlin
/// `FavoriteActivityCard` (`SettingsCards.kt`): a dropdown over the GPS-route
/// activity types plus a "use latest" (`null`) entry, persisting
/// `favoriteActivityExerciseType`.
class FavoriteActivityCard extends ConsumerWidget {
  const FavoriteActivityCard({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final selected = ref.watch(favoriteActivityExerciseTypeProvider);
    final notifier = ref.read(favoriteActivityExerciseTypeProvider.notifier);

    // Kotlin: DefaultActivityEntryTypes.filter { it.supportsGpsRoute }.
    final gpsRouteTypes = [
      for (final type in defaultActivityEntryTypes)
        if (type.supportsGpsRoute) type,
    ];
    // Guard against a stored type that no longer supports a GPS route so the
    // dropdown always has a matching value (falls back to "use latest").
    final selectedValue = gpsRouteTypes.any((t) => t.exerciseType == selected)
        ? selected
        : null;

    return SettingsCardShell(
      title: l10n.settingsFavoriteActivityTitle,
      body: l10n.settingsFavoriteActivityBody,
      children: [
        InputDecorator(
          decoration: const InputDecoration(
            border: OutlineInputBorder(),
            contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 4),
          ),
          child: DropdownButtonHideUnderline(
            child: DropdownButton<int?>(
              value: selectedValue,
              isExpanded: true,
              items: [
                DropdownMenuItem<int?>(
                  value: null,
                  child: Text(l10n.settingsFavoriteActivityLatest),
                ),
                for (final type in gpsRouteTypes)
                  DropdownMenuItem<int?>(
                    value: type.exerciseType,
                    child: Text(type.label),
                  ),
              ],
              onChanged: notifier.select,
            ),
          ),
        ),
      ],
    );
  }
}
