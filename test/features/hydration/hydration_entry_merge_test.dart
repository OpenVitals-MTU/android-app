import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/hydration/hydration_entry_merge.dart';

/// Unit cover for the hydration↔nutrition join — the port of the merge Kotlin's
/// `HydrationViewModel.load()` performs. Health Connect stores a logged drink as
/// two records (the volume, and the named/nutrient-bearing nutrition record);
/// only the join makes an entry whole.
const String _package = 'tech.mmarca.openvitals';

HydrationEntry _hydration(
  DateTime start, {
  double liters = 0.5,
  String id = 'h1',
  String? clientRecordId,
  bool isOpenVitalsEntry = true,
  String source = _package,
}) =>
    HydrationEntry(
      startTime: start,
      endTime: start.add(const Duration(minutes: 1)),
      liters: liters,
      source: source,
      id: id,
      clientRecordId: clientRecordId,
      isOpenVitalsEntry: isOpenVitalsEntry,
    );

NutritionEntry _nutrition(
  DateTime time, {
  String? name,
  String id = 'n1',
  String? clientRecordId,
  bool isOpenVitalsEntry = true,
  String source = _package,
  Map<NutritionNutrient, double> nutrients = const <NutritionNutrient, double>{},
}) =>
    NutritionEntry(
      time: time,
      mealType: 0,
      name: name,
      energyKcal: null,
      proteinGrams: null,
      carbsGrams: null,
      fatGrams: null,
      fiberGrams: null,
      sugarGrams: null,
      source: source,
      nutrientValues: nutrients,
      id: id,
      clientRecordId: clientRecordId,
      isOpenVitalsEntry: isOpenVitalsEntry,
    );

void main() {
  final now = DateTime(2026, 7, 10, 9, 30);
  const hydrationRecordId = 'openvitals_hydration_1700_drink_coffee_abc';
  const pairedRecordId =
      'openvitals_hydration_nutrition_$hydrationRecordId';

  group('mergeHydrationAndNutrition', () {
    test('a hydration entry takes its name and nutrients from its paired '
        'nutrition record', () {
      final merged = mergeHydrationAndNutrition(
        hydrationEntries: [_hydration(now, clientRecordId: hydrationRecordId)],
        nutritionEntries: [
          _nutrition(
            now,
            name: 'Flat white',
            clientRecordId: pairedRecordId,
            nutrients: const {NutritionNutrient.caffeine: 80.0},
          ),
        ],
      );

      expect(merged, hasLength(1));
      expect(merged.single.displayName, 'Flat white');
      expect(merged.single.nutrientValues[NutritionNutrient.caffeine], 80.0);
      expect(merged.single.liters, 0.5);
      expect(merged.single.recordType, HydrationEntryRecordType.hydration);
    });

    test('the paired nutrition record is not also listed on its own', () {
      // Both halves describe one drink; listing the nutrition half separately
      // would double it up in the history.
      final merged = mergeHydrationAndNutrition(
        hydrationEntries: [_hydration(now, clientRecordId: hydrationRecordId)],
        nutritionEntries: [
          _nutrition(now, name: 'Flat white', clientRecordId: pairedRecordId),
        ],
      );

      expect(merged, hasLength(1));
      expect(
        merged.where(
          (e) => e.recordType == HydrationEntryRecordType.nutritionOnly,
        ),
        isEmpty,
      );
    });

    test('a beverage with nutrients but no volume is surfaced as a '
        'nutrition-only entry', () {
      final merged = mergeHydrationAndNutrition(
        hydrationEntries: const [],
        nutritionEntries: [
          _nutrition(
            now,
            name: 'Espresso',
            id: 'n2',
            clientRecordId: 'openvitals_nutrition_1699_xyz',
            nutrients: const {NutritionNutrient.caffeine: 60.0},
          ),
        ],
      );

      expect(merged, hasLength(1));
      final entry = merged.single;
      expect(entry.recordType, HydrationEntryRecordType.nutritionOnly);
      expect(entry.displayName, 'Espresso');
      expect(entry.liters, 0.0);
      expect(entry.nutrientValues[NutritionNutrient.caffeine], 60.0);
      expect(entry.id, 'n2');
    });

    test('another app\'s caffeinated drink joins the beverage history', () {
      final merged = mergeHydrationAndNutrition(
        hydrationEntries: const [],
        nutritionEntries: [
          _nutrition(
            now,
            name: 'Cold brew',
            id: 'x1',
            source: 'com.other.app',
            isOpenVitalsEntry: false,
            nutrients: const {NutritionNutrient.caffeine: 120.0},
          ),
        ],
      );

      expect(merged.single.displayName, 'Cold brew');
      expect(merged.single.recordType, HydrationEntryRecordType.nutritionOnly);
    });

    test('a plain meal is not a beverage', () {
      final merged = mergeHydrationAndNutrition(
        hydrationEntries: const [],
        nutritionEntries: [
          _nutrition(
            now,
            name: 'Chicken salad',
            id: 'm1',
            source: 'com.other.app',
            isOpenVitalsEntry: false,
          ),
        ],
      );

      expect(merged, isEmpty);
    });

    test('the carbs record OpenVitals writes for an activity is excluded', () {
      final merged = mergeHydrationAndNutrition(
        hydrationEntries: const [],
        nutritionEntries: [
          _nutrition(
            now,
            name: kOpenVitalsCarbsEntryName,
            id: 'c1',
            clientRecordId: 'openvitals_nutrition_1699_carbs',
            nutrients: const {NutritionNutrient.totalCarbohydrate: 30.0},
          ),
        ],
      );

      expect(merged, isEmpty);
    });

    test('an unpaired nutrition record at the same instant as an OpenVitals '
        'hydration entry is treated as its other half', () {
      // No client-record-id to join on (an older record), so Kotlin falls back
      // to the instant — the drink is already in the history via its hydration
      // record, and must not be listed twice.
      final merged = mergeHydrationAndNutrition(
        hydrationEntries: [_hydration(now, clientRecordId: null)],
        nutritionEntries: [
          _nutrition(now, name: 'Tea', id: 'n3', clientRecordId: null),
        ],
      );

      expect(merged, hasLength(1));
      expect(merged.single.recordType, HydrationEntryRecordType.hydration);
    });

    test('a hydration entry with no paired record keeps a null name', () {
      // Another app's plain water log: a volume and a package name, nothing
      // else. There is no drink name to show, and the package is not one.
      final merged = mergeHydrationAndNutrition(
        hydrationEntries: [
          _hydration(
            now,
            source: 'com.other.app',
            isOpenVitalsEntry: false,
            clientRecordId: null,
          ),
        ],
        nutritionEntries: const [],
      );

      expect(merged.single.displayName, isNull);
      expect(merged.single.nutrientValues, isEmpty);
    });
  });
}
