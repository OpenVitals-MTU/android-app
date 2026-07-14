import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../di/providers.dart';
import '../../../domain/insights/caffeine_drink_profile.dart';
import 'caffeine_view_model.dart';

/// One drink, worked out from the caffeine screen's own data.
///
/// No second load. The entries are already in [caffeineProvider] — they are what the whole
/// curve is built from — so the detail screen reads the same list the list screen read, and
/// the two can never be showing different coffees.
///
/// Null when the id is not among them: the drink was deleted while its screen was open, or
/// the link was followed from somewhere stale.
final caffeineDrinkProfileProvider =
    Provider.family<CaffeineDrinkProfile?, String>((ref, entryId) {
  final entries = ref.watch(caffeineProvider.select((state) => state.entries));
  final entry = entries.where((entry) => entry.id == entryId).firstOrNull;
  if (entry == null) return null;

  final preferences = ref.watch(preferencesRepositoryProvider);
  return caffeineDrinkProfile(
    entry: entry,
    now: DateTime.now(),
    preferences: preferences.caffeinePreferences(),
    bodyProfile: preferences.bodyProfile(),
  );
});
