/// Joins the beverage log's two halves back together.
///
/// Health Connect's `HydrationRecord` has no name field: it is a volume and
/// nothing else. When OpenVitals logs a drink it writes *two* records — the
/// hydration volume, and a `NutritionRecord` carrying the drink's name and
/// nutrients — and ties them together by prefixing the hydration record's
/// `clientRecordId` (see `NutritionHealthReader.hydrationNutritionClientRecordId`
/// / `hydration_drink_usage.dart`). Read back separately, neither half is the
/// whole entry, which is why a hydration entry read straight off the wire has no
/// `displayName` and no `nutrientValues` at all.
///
/// Port of the merge the Kotlin `HydrationViewModel.load()` does — deliberately
/// kept on the Dart side, where the Kotlin ViewModel does it, rather than
/// widening the `HydrationEntryMsg` pigeon: the name is not on the hydration
/// record for the native reader to send, and `NutritionEntryMsg` already carries
/// everything the join needs.
library;

import '../insights/caffeine_health_drink_catalog.dart';
import '../model/nutrition_models.dart';
import 'hydration_drink_usage.dart';

/// The carbs-only nutrition record OpenVitals writes alongside an activity.
/// Never a drink, so it never belongs in the beverage history (Kotlin
/// `OpenVitalsCarbsEntryName`).
const String kOpenVitalsCarbsEntryName = 'OpenVitals carbs';

/// The beverage history for a period: the hydration entries with their drink
/// names/nutrients restored, plus the nutrition-only beverages that carry no
/// hydration volume of their own.
List<HydrationEntry> mergeHydrationAndNutrition({
  required List<HydrationEntry> hydrationEntries,
  required List<NutritionEntry> nutritionEntries,
}) =>
    <HydrationEntry>[
      ...namedHydrationEntries(
        hydrationEntries: hydrationEntries,
        nutritionEntries: nutritionEntries,
      ),
      ...hydrationNutritionOnlyEntries(
        nutritionEntries: nutritionEntries,
        hydrationEntries: hydrationEntries,
      ),
    ];

/// Adopts the paired `NutritionRecord`'s name and nutrients onto each hydration
/// entry it belongs to. Entries with no pair (another app's water log, or an
/// OpenVitals drink saved with no nutrients) are returned untouched, and keep a
/// null [HydrationEntry.displayName].
///
/// DEVIATION from Kotlin, which leaves `displayName` null on every HYDRATION
/// entry: its entry rows title a hydration row by *date*, so it never needs the
/// name, and it has no drink-breakdown card. Our hydration screen does group the
/// period by drink, and the only honest label for a slice is the name on the
/// paired record. The join key is Kotlin's own — this reads nothing new, it just
/// uses what Kotlin already writes.
List<HydrationEntry> namedHydrationEntries({
  required List<HydrationEntry> hydrationEntries,
  required List<NutritionEntry> nutritionEntries,
}) {
  final pairedByHydrationId = <String, NutritionEntry>{};
  for (final entry in nutritionEntries) {
    final clientRecordId = entry.clientRecordId;
    if (clientRecordId == null) continue;
    final hydrationId = pairedHydrationClientRecordId(clientRecordId);
    if (hydrationId == null) continue;
    pairedByHydrationId[hydrationId] = entry;
  }
  if (pairedByHydrationId.isEmpty) return hydrationEntries;

  return [
    for (final entry in hydrationEntries)
      _withPairedNutrition(entry, pairedByHydrationId[entry.clientRecordId]),
  ];
}

HydrationEntry _withPairedNutrition(
  HydrationEntry entry,
  NutritionEntry? paired,
) {
  if (paired == null) return entry;
  return entry.copyWith(
    displayName: _nonBlank(paired.name),
    nutrientValues: paired.nutrientValues,
  );
}

/// The beverages that live *only* as a nutrition record — a drink logged with
/// nutrients but no hydration volume, or a caffeinated drink another app wrote.
/// Surfaced as zero-litre [HydrationEntryRecordType.nutritionOnly] entries so
/// they still show up in the beverage history.
///
/// Port of the Kotlin `List<NutritionEntry>.toHydrationNutritionOnlyEntries`.
List<HydrationEntry> hydrationNutritionOnlyEntries({
  required List<NutritionEntry> nutritionEntries,
  required List<HydrationEntry> hydrationEntries,
}) =>
    [
      for (final entry in nutritionEntries)
        if (_shouldAppearInBeverageHistory(entry) &&
            entry.id.trim().isNotEmpty &&
            entry.name != kOpenVitalsCarbsEntryName &&
            _isStandaloneHydrationNutrition(entry, hydrationEntries))
          HydrationEntry(
            startTime: entry.time,
            endTime: entry.time.add(const Duration(seconds: 1)),
            liters: 0.0,
            source: entry.source,
            id: entry.id,
            clientRecordId: entry.clientRecordId,
            isOpenVitalsEntry: entry.isOpenVitalsEntry,
            recordType: HydrationEntryRecordType.nutritionOnly,
            displayName: _nonBlank(entry.name),
            nutrientValues: entry.nutrientValues,
          ),
    ];

/// Kotlin `NutritionEntry.shouldAppearInBeverageHistory()`: ours, or something
/// caffeinated, or a drink the catalog recognises by name. A plain meal is not a
/// beverage and stays out.
bool _shouldAppearInBeverageHistory(NutritionEntry entry) {
  if (entry.isOpenVitalsEntry) return true;
  final caffeine = entry.valueFor(NutritionNutrient.caffeine);
  if (caffeine != null && caffeine > 0.0 && caffeine.isFinite) return true;
  return CaffeineHealthDrinkCatalog.matchName(entry.name) != null;
}

/// Kotlin `NutritionEntry.isStandaloneHydrationNutrition()`: a nutrition record
/// paired to a hydration record is already represented by that record's entry,
/// so listing it again would double up the drink. Records with no OpenVitals
/// client-record-id fall back to matching on the instant.
bool _isStandaloneHydrationNutrition(
  NutritionEntry entry,
  List<HydrationEntry> hydrationEntries,
) {
  final clientRecordId = entry.clientRecordId;
  if (clientRecordId != null) {
    if (isStandaloneNutritionClientRecordId(clientRecordId)) return true;
    if (pairedHydrationClientRecordId(clientRecordId) != null) return false;
  }
  // Kotlin compares `Instant`s; `isAtSameMomentAs` is the DateTime equivalent
  // that ignores the UTC flag, so a local and a UTC read of the same instant
  // still pair up.
  return !hydrationEntries.any(
    (hydration) =>
        hydration.startTime.isAtSameMomentAs(entry.time) &&
        hydration.isOpenVitalsEntry,
  );
}

String? _nonBlank(String? value) =>
    (value != null && value.trim().isNotEmpty) ? value : null;
