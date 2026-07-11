import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/homewidgets/home_widget_beverage.dart';

CustomHydrationDrink drink(
  String id, {
  String? name,
  double volumeMilliliters = 250,
  CaffeineSourceCategory? category,
  bool isPreloaded = false,
  double hydrationMultiplier = 1.0,
  Map<NutritionNutrient, double> nutrientValues =
      const <NutritionNutrient, double>{},
}) =>
    CustomHydrationDrink(
      id: id,
      name: name ?? id,
      volumeMilliliters: volumeMilliliters,
      hydrationMultiplier: hydrationMultiplier,
      category: category,
      isPreloaded: isPreloaded,
      nutrientValues: nutrientValues,
    );

void main() {
  group('quickBeverageWidgetDrinkOptions', () {
    test('frequent drinks come first, in their own ranking', () {
      final water = drink('water', category: CaffeineSourceCategory.water);
      final tea = drink('tea', category: CaffeineSourceCategory.tea);
      final cola = drink('cola', category: CaffeineSourceCategory.soda);

      final options = quickBeverageWidgetDrinkOptions(
        drinks: [water, tea, cola],
        // Deliberately not in category order: the frequency ranking wins.
        frequentDrinks: [cola, tea],
      );

      expect(options.map((d) => d.id), ['cola', 'tea', 'water']);
    });

    test('a frequent drink that is no longer in the catalog is dropped', () {
      final water = drink('water', category: CaffeineSourceCategory.water);

      final options = quickBeverageWidgetDrinkOptions(
        drinks: [water],
        frequentDrinks: [drink('deleted')],
      );

      expect(options.map((d) => d.id), ['water']);
    });

    test('user drinks come before the preloaded catalog', () {
      final preloadedWater = drink(
        'preloaded_water',
        category: CaffeineSourceCategory.water,
        isPreloaded: true,
      );
      // A user drink in the *last* category still outranks a preloaded one in
      // the first: the custom/preloaded split is the outer sort key.
      final userOther = drink('user_other', category: CaffeineSourceCategory.other);

      final options = quickBeverageWidgetDrinkOptions(
        drinks: [preloadedWater, userOther],
        frequentDrinks: const [],
      );

      expect(options.map((d) => d.id), ['user_other', 'preloaded_water']);
    });

    test('sorts each group by category, then name, then id', () {
      final drinks = [
        drink('z_soda', name: 'Soda', category: CaffeineSourceCategory.soda),
        drink('b_coffee', name: 'beta', category: CaffeineSourceCategory.coffee),
        drink('a_coffee', name: 'Alpha', category: CaffeineSourceCategory.coffee),
        drink('tea', name: 'Tea', category: CaffeineSourceCategory.tea),
        drink('energy', name: 'Energy', category: CaffeineSourceCategory.energyDrink),
        drink('water', name: 'Water', category: CaffeineSourceCategory.water),
        drink('choc', name: 'Choc', category: CaffeineSourceCategory.chocolate),
        drink('supp', name: 'Supp', category: CaffeineSourceCategory.supplement),
        drink('none', name: 'None'),
      ];

      final options = quickBeverageWidgetDrinkOptions(
        drinks: drinks,
        frequentDrinks: const [],
      );

      // WATER 0, COFFEE 1, ENERGY_DRINK 2, TEA 3, CHOCOLATE 4, SODA 5,
      // SUPPLEMENT/OTHER/null 6 — and inside a category, name lowercased
      // ("Alpha" < "beta", which a case-sensitive sort would get backwards).
      expect(options.map((d) => d.id), [
        'water',
        'a_coffee',
        'b_coffee',
        'energy',
        'tea',
        'choc',
        'z_soda',
        // Same category order (6) and same name comparison, so the id breaks
        // the tie between the null-category and the supplement drink.
        'none',
        'supp',
      ]);
    });

    test('breaks a same-name tie on the id', () {
      final options = quickBeverageWidgetDrinkOptions(
        drinks: [
          drink('b', name: 'Coffee', category: CaffeineSourceCategory.coffee),
          drink('a', name: 'coffee', category: CaffeineSourceCategory.coffee),
        ],
        frequentDrinks: const [],
      );

      expect(options.map((d) => d.id), ['a', 'b']);
    });

    test('an empty catalog yields no options', () {
      expect(
        quickBeverageWidgetDrinkOptions(
          drinks: const [],
          frequentDrinks: [drink('water')],
        ),
        isEmpty,
      );
    });
  });

  group('quickBeverageAmountLabel', () {
    final metric = UnitFormatter(unitSystemProvider: () => UnitSystem.metric);
    final imperial =
        UnitFormatter(unitSystemProvider: () => UnitSystem.imperial);

    test('metric sub-litre volumes read in millilitres, without a space', () {
      // The widget's compact form — the entry screen's `hydrationAmountLabel`
      // renders the same drink as "250 ml".
      expect(quickBeverageAmountLabel(drink('a'), metric), '250ml');
      expect(
        quickBeverageAmountLabel(drink('a', volumeMilliliters: 330), metric),
        '330ml',
      );
    });

    test('metric volumes of a litre and up read in litres', () {
      expect(
        quickBeverageAmountLabel(drink('a', volumeMilliliters: 1000), metric),
        '1.00 L',
      );
      expect(
        quickBeverageAmountLabel(drink('a', volumeMilliliters: 1500), metric),
        '1.50 L',
      );
    });

    test('imperial always reads through the formatter', () {
      // No millilitre special case: even a sub-litre drink is fluid ounces.
      expect(quickBeverageAmountLabel(drink('a'), imperial), '8 fl oz');
      expect(
        quickBeverageAmountLabel(drink('a', volumeMilliliters: 1000), imperial),
        '34 fl oz',
      );
    });
  });

  group('drink payload', () {
    test('round-trips the fields the log callback needs', () {
      final espresso = drink(
        'espresso',
        name: 'Espresso',
        volumeMilliliters: 30,
        hydrationMultiplier: 0.5,
        category: CaffeineSourceCategory.coffee,
        nutrientValues: const {
          NutritionNutrient.caffeine: 63.0,
          NutritionNutrient.energy: 2.0,
        },
      );

      final decoded = decodeQuickBeverageDrink(
        encodeQuickBeverageDrink(espresso),
      );

      expect(decoded, isNotNull);
      expect(decoded!.id, 'espresso');
      expect(decoded.name, 'Espresso');
      expect(decoded.volumeMilliliters, 30);
      expect(decoded.hydrationMultiplier, 0.5);
      // The whole point of caching the payload: the nutrients survive, so a
      // widget tap cannot silently drop the drink's caffeine.
      expect(decoded.nutrientValues, {
        NutritionNutrient.caffeine: 63.0,
        NutritionNutrient.energy: 2.0,
      });
    });

    test('refuses a malformed, empty or unloggable payload', () {
      expect(decodeQuickBeverageDrink(null), isNull);
      expect(decodeQuickBeverageDrink(''), isNull);
      expect(decodeQuickBeverageDrink('not json'), isNull);
      expect(decodeQuickBeverageDrink('[1,2,3]'), isNull);
      expect(decodeQuickBeverageDrink('{"id":"a"}'), isNull);
      // A volume outside the loggable range is not a drink.
      expect(
        decodeQuickBeverageDrink(
          '{"id":"a","name":"A","volumeMilliliters":0.0}',
        ),
        isNull,
      );
    });

    test('drops an unknown nutrient rather than the whole drink', () {
      final decoded = decodeQuickBeverageDrink(
        '{"id":"a","name":"A","volumeMilliliters":250.0,'
        '"hydrationMultiplier":1.0,'
        '"nutrients":{"CAFFEINE":63.0,"UNOBTAINIUM":1.0}}',
      );

      expect(decoded, isNotNull);
      expect(decoded!.nutrientValues, {NutritionNutrient.caffeine: 63.0});
    });
  });
}
