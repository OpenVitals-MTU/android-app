import '../model/nutrition_models.dart';

/// Port of the Kotlin `HydrationDrinkUsage`.
///
/// Ranks saved drinks by how often they were logged, derived entirely from the
/// Health Connect entries themselves: OpenVitals encodes the drink id into the
/// hydration record's `clientRecordId` at write time, and pairs a nutrition
/// record to it by prefixing that id. There is no separate usage store.
const int kFrequentHydrationDrinkLimit = 6;
const int kFrequentHydrationDrinkLookbackDays = 90;

const String _hydrationClientRecordPrefix = 'openvitals_hydration_';
const String _hydrationDrinkClientRecordMarker = '_drink_';
const String _standaloneNutritionPrefix = 'openvitals_nutrition_';
const String _pairedHydrationNutritionPrefix = 'openvitals_hydration_nutrition_';

class _UsageScore {
  const _UsageScore(this.count, this.latestTime);
  final int count;
  final DateTime latestTime;
}

/// The drink id embedded in a hydration `clientRecordId`, or null.
///
/// The native writer emits `openvitals_hydration_<epochMs>_drink_<id>_<uuid>`,
/// where `<id>` is stripped to letters, digits and `-`, so the first `_` after
/// the marker terminates it.
String? hydrationDrinkIdFromClientRecordId(String clientRecordId) {
  if (!clientRecordId.startsWith(_hydrationClientRecordPrefix)) return null;
  final markerStart = clientRecordId.indexOf(_hydrationDrinkClientRecordMarker);
  if (markerStart < 0) return null;
  final idStart = markerStart + _hydrationDrinkClientRecordMarker.length;
  final idEnd = clientRecordId.indexOf('_', idStart);
  if (idEnd <= idStart) return null;
  final id = clientRecordId.substring(idStart, idEnd);
  return id.trim().isEmpty ? null : id;
}

/// The hydration record a paired nutrition record belongs to, or null.
String? pairedHydrationClientRecordId(String clientRecordId) =>
    clientRecordId.startsWith(_pairedHydrationNutritionPrefix)
        ? clientRecordId.substring(_pairedHydrationNutritionPrefix.length)
        : null;

/// True when OpenVitals wrote this nutrition record on its own — a beverage
/// logged with nutrients but no hydration volume (e.g. a zero-multiplier drink),
/// rather than the nutrition half of a hydration entry. The two prefixes are
/// disjoint: a paired record starts `openvitals_hydration_nutrition_`.
bool isStandaloneNutritionClientRecordId(String clientRecordId) =>
    clientRecordId.startsWith(_standaloneNutritionPrefix);

String _normalizedDrinkName(String name) => name.trim().toLowerCase();

bool _isOpenVitalsHydrationEntry(HydrationEntry entry) =>
    entry.isOpenVitalsEntry ||
    (entry.clientRecordId?.startsWith(_hydrationClientRecordPrefix) ?? false);

bool _isOpenVitalsNutritionEntry(NutritionEntry entry) =>
    entry.isOpenVitalsEntry ||
    (entry.clientRecordId?.startsWith(_standaloneNutritionPrefix) ?? false) ||
    (entry.clientRecordId?.startsWith(_pairedHydrationNutritionPrefix) ?? false);

/// The most-logged [drinks], most frequent first. Ties break on the most recent
/// log, then on the drink's saved order. Port of `frequentHydrationDrinkOptions`.
List<CustomHydrationDrink> frequentHydrationDrinkOptions({
  required List<CustomHydrationDrink> drinks,
  required List<HydrationEntry> hydrationEntries,
  required List<NutritionEntry> nutritionEntries,
}) {
  if (drinks.isEmpty) return const <CustomHydrationDrink>[];

  final drinkById = {for (final drink in drinks) drink.id: drink};
  final drinkOrder = {
    for (var i = 0; i < drinks.length; i++) drinks[i].id: i,
  };
  // First drink wins a duplicated name, matching Kotlin's `putIfAbsent`.
  final drinkIdByName = <String, String>{};
  for (final drink in drinks) {
    drinkIdByName.putIfAbsent(_normalizedDrinkName(drink.name), () => drink.id);
  }

  final scores = <String, _UsageScore>{};
  final countedHydrationClientRecordIds = <String>{};

  void increment(String drinkId, DateTime time) {
    if (!drinkById.containsKey(drinkId)) return;
    final current = scores[drinkId];
    scores[drinkId] = _UsageScore(
      (current?.count ?? 0) + 1,
      current == null || time.isAfter(current.latestTime)
          ? time
          : current.latestTime,
    );
  }

  for (final entry in hydrationEntries) {
    if (!_isOpenVitalsHydrationEntry(entry)) continue;
    final clientRecordId = entry.clientRecordId;
    if (clientRecordId == null) continue;
    final drinkId = hydrationDrinkIdFromClientRecordId(clientRecordId);
    if (drinkId == null) continue;
    increment(drinkId, entry.startTime);
    countedHydrationClientRecordIds.add(clientRecordId);
  }

  for (final entry in nutritionEntries) {
    if (!_isOpenVitalsNutritionEntry(entry)) continue;
    final clientRecordId = entry.clientRecordId;
    final pairedId = clientRecordId == null
        ? null
        : pairedHydrationClientRecordId(clientRecordId);
    final pairedDrinkId =
        pairedId == null ? null : hydrationDrinkIdFromClientRecordId(pairedId);
    if (pairedDrinkId != null) {
      // Only count the pair once: the hydration half already scored it unless
      // the drink writes no hydration at all (a zero-multiplier drink).
      if (!countedHydrationClientRecordIds.contains(pairedId)) {
        increment(pairedDrinkId, entry.time);
      }
      continue;
    }
    final name = entry.name;
    if (name == null) continue;
    final normalized = _normalizedDrinkName(name);
    if (normalized.isEmpty) continue;
    final drinkId = drinkIdByName[normalized];
    if (drinkId == null) continue;
    increment(drinkId, entry.time);
  }

  final ranked = scores.keys.toList()
    ..sort((a, b) {
      final byCount = scores[b]!.count.compareTo(scores[a]!.count);
      if (byCount != 0) return byCount;
      final byRecency =
          scores[b]!.latestTime.compareTo(scores[a]!.latestTime);
      if (byRecency != 0) return byRecency;
      return (drinkOrder[a] ?? 1 << 30).compareTo(drinkOrder[b] ?? 1 << 30);
    });

  return [
    for (final id in ranked.take(kFrequentHydrationDrinkLimit))
      if (drinkById[id] != null) drinkById[id]!,
  ];
}
