import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/features/manualentry/application/hydration_entry_view_model.dart';

void main() {
  group('customHydrationDrinkFromInput', () {
    test('normalizes the name and sorts nutrients by enum-constant name', () {
      final drink = customHydrationDrinkFromInput(
        const CustomHydrationDrinkInput(
          name: '  Flat white  ',
          volumeMilliliters: 250,
          nutrientValues: {
            NutritionNutrient.protein: 3,
            NutritionNutrient.energy: 120,
            NutritionNutrient.caffeine: 0.08,
          },
        ),
        id: 'drink-1',
      );

      expect(drink, isNotNull);
      expect(drink!.name, 'Flat white');
      expect(drink.id, 'drink-1');
      // Sorted by `NutritionNutrient.name`: caffeine, energy, protein.
      expect(
        drink.nutrientValues.keys.toList(),
        [
          NutritionNutrient.caffeine,
          NutritionNutrient.energy,
          NutritionNutrient.protein,
        ],
      );
    });

    test('rejects a blank name and an out-of-range volume', () {
      expect(
        customHydrationDrinkFromInput(
          const CustomHydrationDrinkInput(name: '   ', volumeMilliliters: 250),
          id: 'x',
        ),
        isNull,
      );
      expect(
        customHydrationDrinkFromInput(
          const CustomHydrationDrinkInput(name: 'Tea', volumeMilliliters: 0),
          id: 'x',
        ),
        isNull,
      );
    });

    test('one invalid nutrient rejects the whole drink, not just that nutrient',
        () {
      // Kotlin compares the filtered map's size to the input's, so a single bad
      // value fails the drink rather than silently dropping the nutrient.
      final drink = customHydrationDrinkFromInput(
        const CustomHydrationDrinkInput(
          name: 'Soda',
          volumeMilliliters: 330,
          nutrientValues: {
            NutritionNutrient.energy: 140,
            NutritionNutrient.sugar: -1,
          },
        ),
        id: 'x',
      );
      expect(drink, isNull);
    });

    test('rejects a nutrient value above the maximum', () {
      expect(
        customHydrationDrinkFromInput(
          CustomHydrationDrinkInput(
            name: 'Soda',
            volumeMilliliters: 330,
            nutrientValues: {
              NutritionNutrient.energy: kMaxCustomDrinkNutrientValue + 1,
            },
          ),
          id: 'x',
        ),
        isNull,
      );
    });

    test('rejects a hydration multiplier outside [0, 1]', () {
      expect(
        customHydrationDrinkFromInput(
          const CustomHydrationDrinkInput(
            name: 'Beer',
            volumeMilliliters: 330,
            hydrationMultiplier: 1.5,
          ),
          id: 'x',
        ),
        isNull,
      );
    });

    test('keeps a zero-hydration drink (nutrients only)', () {
      final drink = customHydrationDrinkFromInput(
        const CustomHydrationDrinkInput(
          name: 'Espresso shot',
          volumeMilliliters: 30,
          hydrationMultiplier: 0,
          category: CaffeineSourceCategory.coffee,
        ),
        id: 'x',
      );
      expect(drink, isNotNull);
      expect(drink!.hydrationMultiplier, 0);
      expect(drink.category, CaffeineSourceCategory.coffee);
    });
  });

  group('hydration impact', () {
    test('maps a multiplier back onto its option', () {
      expect(hydrationImpactOptionForMultiplier(1.0), HydrationImpactOption.full);
      expect(hydrationImpactOptionForMultiplier(0.0), HydrationImpactOption.none);
      expect(
        hydrationImpactOptionForMultiplier(0.5),
        HydrationImpactOption.partial,
      );
    });

    test('partial percent parses only strictly between 0 and 100', () {
      expect(hydrationImpactMultiplier(HydrationImpactOption.partial, '50'), 0.5);
      expect(hydrationImpactMultiplier(HydrationImpactOption.partial, '0,5'),
          closeTo(0.005, 1e-9));
      expect(hydrationImpactMultiplier(HydrationImpactOption.partial, '0'), isNull);
      expect(
        hydrationImpactMultiplier(HydrationImpactOption.partial, '100'),
        isNull,
      );
      expect(
        hydrationImpactMultiplier(HydrationImpactOption.partial, 'abc'),
        isNull,
      );
    });

    test('full and none ignore the percent text', () {
      expect(hydrationImpactMultiplier(HydrationImpactOption.full, 'abc'), 1.0);
      expect(hydrationImpactMultiplier(HydrationImpactOption.none, 'abc'), 0.0);
    });

    test('percent text falls back to the default outside the partial range', () {
      expect(hydrationImpactPercentText(0.42), '42');
      expect(hydrationImpactPercentText(1.0),
          kDefaultPartialHydrationImpactPercent.toString());
      expect(hydrationImpactPercentText(0.0),
          kDefaultPartialHydrationImpactPercent.toString());
      // Clamped into [1, 99] rather than rounding to 0 or 100.
      expect(hydrationImpactPercentText(0.001), '1');
      expect(hydrationImpactPercentText(0.999), '99');
    });
  });

  group('isValidCustomDrinkNutrientValue', () {
    test('accepts (0, max] and nothing else', () {
      expect(isValidCustomDrinkNutrientValue(0), isFalse);
      expect(isValidCustomDrinkNutrientValue(0.1), isTrue);
      expect(isValidCustomDrinkNutrientValue(kMaxCustomDrinkNutrientValue), isTrue);
      expect(
        isValidCustomDrinkNutrientValue(kMaxCustomDrinkNutrientValue + 0.1),
        isFalse,
      );
      expect(isValidCustomDrinkNutrientValue(double.nan), isFalse);
      expect(isValidCustomDrinkNutrientValue(double.infinity), isFalse);
    });
  });
}
