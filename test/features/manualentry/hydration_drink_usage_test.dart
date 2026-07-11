import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/features/manualentry/hydration_drink_usage.dart';

CustomHydrationDrink _drink(String id, {String? name}) => CustomHydrationDrink(
      id: id,
      name: name ?? id,
      volumeMilliliters: 250,
    );

/// A hydration entry as the native writer emits it:
/// `openvitals_hydration_<epochMs>_drink_<id>_<uuid>`.
HydrationEntry _hydration(String drinkId, DateTime time, {String uuid = 'u1'}) =>
    HydrationEntry(
      startTime: time,
      endTime: time,
      liters: 0.25,
      source: 'openvitals',
      isOpenVitalsEntry: true,
      clientRecordId:
          'openvitals_hydration_${time.millisecondsSinceEpoch}_drink_${drinkId}_$uuid',
    );

NutritionEntry _nutrition(
  DateTime time, {
  String? clientRecordId,
  String? name,
  bool isOpenVitals = true,
}) =>
    NutritionEntry(
      time: time,
      endTime: time,
      mealType: 0,
      name: name,
      energyKcal: null,
      proteinGrams: null,
      carbsGrams: null,
      fatGrams: null,
      fiberGrams: null,
      sugarGrams: null,
      source: 'openvitals',
      clientRecordId: clientRecordId,
      isOpenVitalsEntry: isOpenVitals,
    );

void main() {
  final t0 = DateTime(2026, 1, 1, 8);
  final t1 = DateTime(2026, 1, 2, 8);
  final t2 = DateTime(2026, 1, 3, 8);

  group('hydrationDrinkIdFromClientRecordId', () {
    test('extracts the id between the marker and the next underscore', () {
      expect(
        hydrationDrinkIdFromClientRecordId(
          'openvitals_hydration_1700000000000_drink_abc-123_uuid',
        ),
        'abc-123',
      );
    });

    test('returns null without the prefix, marker or a terminator', () {
      expect(hydrationDrinkIdFromClientRecordId('other_1_drink_a_b'), isNull);
      expect(
        hydrationDrinkIdFromClientRecordId('openvitals_hydration_1_uuid'),
        isNull,
      );
      // No trailing '_' after the id.
      expect(
        hydrationDrinkIdFromClientRecordId('openvitals_hydration_1_drink_abc'),
        isNull,
      );
    });
  });

  group('pairedHydrationClientRecordId', () {
    test('unwraps a paired nutrition record id', () {
      expect(
        pairedHydrationClientRecordId(
          'openvitals_hydration_nutrition_openvitals_hydration_1_drink_a_u',
        ),
        'openvitals_hydration_1_drink_a_u',
      );
    });

    test('returns null for a standalone nutrition record', () {
      expect(pairedHydrationClientRecordId('openvitals_nutrition_1_u'), isNull);
    });
  });

  group('frequentHydrationDrinkOptions', () {
    test('ranks by log count, most frequent first', () {
      final drinks = [_drink('a'), _drink('b')];
      final result = frequentHydrationDrinkOptions(
        drinks: drinks,
        hydrationEntries: [
          _hydration('a', t0),
          _hydration('b', t1, uuid: 'u2'),
          _hydration('b', t2, uuid: 'u3'),
        ],
        nutritionEntries: const [],
      );
      expect([for (final d in result) d.id], ['b', 'a']);
    });

    test('breaks a count tie on the most recent log', () {
      final drinks = [_drink('a'), _drink('b')];
      final result = frequentHydrationDrinkOptions(
        drinks: drinks,
        hydrationEntries: [_hydration('a', t0), _hydration('b', t2)],
        nutritionEntries: const [],
      );
      expect([for (final d in result) d.id], ['b', 'a']);
    });

    test('breaks a count+recency tie on the saved order', () {
      final drinks = [_drink('a'), _drink('b')];
      final result = frequentHydrationDrinkOptions(
        drinks: drinks,
        hydrationEntries: [
          _hydration('b', t0, uuid: 'u1'),
          _hydration('a', t0, uuid: 'u2'),
        ],
        nutritionEntries: const [],
      );
      expect([for (final d in result) d.id], ['a', 'b']);
    });

    test('ignores entries for unknown or deleted drinks', () {
      final result = frequentHydrationDrinkOptions(
        drinks: [_drink('a')],
        hydrationEntries: [_hydration('a', t0), _hydration('gone', t1)],
        nutritionEntries: const [],
      );
      expect([for (final d in result) d.id], ['a']);
    });

    test('does not double-count a hydration record and its paired nutrition',
        () {
      final hydration = _hydration('a', t0);
      final result = frequentHydrationDrinkOptions(
        drinks: [_drink('a'), _drink('b')],
        hydrationEntries: [hydration, _hydration('b', t1, uuid: 'u2')],
        nutritionEntries: [
          _nutrition(
            t0,
            clientRecordId:
                'openvitals_hydration_nutrition_${hydration.clientRecordId}',
          ),
        ],
      );
      // Both logged once; the tie breaks on recency, so 'b' leads.
      expect([for (final d in result) d.id], ['b', 'a']);
    });

    test('counts a paired nutrition record whose hydration half never wrote',
        () {
      // A zero-hydration drink writes nutrition only, so nothing counted the
      // hydration client record id.
      const orphanHydrationId = 'openvitals_hydration_5_drink_a_u9';
      final result = frequentHydrationDrinkOptions(
        drinks: [_drink('a')],
        hydrationEntries: const [],
        nutritionEntries: [
          _nutrition(
            t0,
            clientRecordId: 'openvitals_hydration_nutrition_$orphanHydrationId',
          ),
        ],
      );
      expect([for (final d in result) d.id], ['a']);
    });

    test('falls back to matching a standalone nutrition entry by drink name',
        () {
      final result = frequentHydrationDrinkOptions(
        drinks: [_drink('a', name: 'Cold Brew')],
        hydrationEntries: const [],
        nutritionEntries: [
          _nutrition(
            t0,
            clientRecordId: 'openvitals_nutrition_1_u',
            name: '  cold brew ',
          ),
        ],
      );
      expect([for (final d in result) d.id], ['a']);
    });

    test('ignores entries from other apps', () {
      final result = frequentHydrationDrinkOptions(
        drinks: [_drink('a', name: 'Cola')],
        hydrationEntries: const [],
        nutritionEntries: [
          _nutrition(t0, clientRecordId: 'someone_else', name: 'Cola',
              isOpenVitals: false),
        ],
      );
      expect(result, isEmpty);
    });

    test('caps the list at the frequent-drink limit', () {
      final drinks = [
        for (var i = 0; i < kFrequentHydrationDrinkLimit + 3; i++) _drink('d$i'),
      ];
      final result = frequentHydrationDrinkOptions(
        drinks: drinks,
        hydrationEntries: [
          for (var i = 0; i < drinks.length; i++)
            _hydration('d$i', t0.add(Duration(minutes: i)), uuid: 'u$i'),
        ],
        nutritionEntries: const [],
      );
      expect(result, hasLength(kFrequentHydrationDrinkLimit));
    });

    test('is empty when there are no saved drinks', () {
      expect(
        frequentHydrationDrinkOptions(
          drinks: const [],
          hydrationEntries: [_hydration('a', t0)],
          nutritionEntries: const [],
        ),
        isEmpty,
      );
    });
  });
}
