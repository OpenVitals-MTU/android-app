import '../model/caffeine_models.dart';
import '../model/nutrition_models.dart';

/// Faithful port of `BeverageNutritionDefaults` from the Kotlin app.
///
/// Static matching metadata derived from CaffeineHealth GPL-3.0
/// consumable_items.json. OpenVitals uses it as seed/matching metadata for
/// beverages; Health Connect remains the source of truth for logged nutrition
/// records.
class BeverageNutritionDefaults {
  BeverageNutritionDefaults._();

  static Map<NutritionNutrient, double> nutrientValuesFor(
    CaffeineCatalogItem item,
  ) {
    final name = item.name.toLowerCase();
    final volumeMilliliters = item.defaultServingMilliliters ?? 240.0;
    final Map<NutritionNutrient, double> base;
    switch (item.category) {
      case CaffeineSourceCategory.water:
        base = const <NutritionNutrient, double>{};
      case CaffeineSourceCategory.coffee:
        base = _coffeeDefaults(name, volumeMilliliters);
      case CaffeineSourceCategory.tea:
        base = _teaDefaults(name, volumeMilliliters);
      case CaffeineSourceCategory.energyDrink:
        base = _energyDrinkDefaults(name, volumeMilliliters);
      case CaffeineSourceCategory.soda:
        base = _sodaDefaults(name, volumeMilliliters);
      case CaffeineSourceCategory.chocolate:
        base = _chocolateDefaults(name, volumeMilliliters);
      case CaffeineSourceCategory.supplement:
      case CaffeineSourceCategory.other:
        base = const <NutritionNutrient, double>{};
    }
    return _withCaffeine(base, item.typicalCaffeineMg);
  }

  static Map<NutritionNutrient, double> _coffeeDefaults(
    String name,
    double volumeMilliliters,
  ) {
    if (_containsAny(name, ['atkins', 'protein shake'])) {
      return _nutrients(
        energyKcal: 165.0,
        totalFatGrams: 9.0,
        saturatedFatGrams: 1.5,
        totalCarbohydrateGrams: 7.0,
        sugarGrams: 1.0,
        dietaryFiberGrams: 5.0,
        proteinGrams: 15.0,
        sodiumMilligrams: 240.0,
        calciumMilligrams: 350.0,
      );
    } else if (_containsAny(
      name,
      ['mocha', 'white coffee', 'dunkaccino', 'frappe'],
    )) {
      return _nutrients(
        energyKcal: _scaled(290.0, volumeMilliliters, 354.0),
        totalFatGrams: _scaled(12.0, volumeMilliliters, 354.0),
        saturatedFatGrams: _scaled(7.0, volumeMilliliters, 354.0),
        totalCarbohydrateGrams: _scaled(36.0, volumeMilliliters, 354.0),
        sugarGrams: _scaled(28.0, volumeMilliliters, 354.0),
        proteinGrams: _scaled(10.0, volumeMilliliters, 354.0),
        sodiumMilligrams: _scaled(170.0, volumeMilliliters, 354.0),
        potassiumMilligrams: _scaled(250.0, volumeMilliliters, 354.0),
        calciumMilligrams: _scaled(300.0, volumeMilliliters, 354.0),
      );
    } else if (name.contains('cappuccino')) {
      return _nutrients(
        energyKcal: _scaled(100.0, volumeMilliliters, 354.0),
        totalFatGrams: _scaled(4.0, volumeMilliliters, 354.0),
        saturatedFatGrams: _scaled(2.5, volumeMilliliters, 354.0),
        totalCarbohydrateGrams: _scaled(10.0, volumeMilliliters, 354.0),
        sugarGrams: _scaled(9.0, volumeMilliliters, 354.0),
        proteinGrams: _scaled(7.0, volumeMilliliters, 354.0),
        sodiumMilligrams: _scaled(100.0, volumeMilliliters, 354.0),
        potassiumMilligrams: _scaled(300.0, volumeMilliliters, 354.0),
        calciumMilligrams: _scaled(240.0, volumeMilliliters, 354.0),
      );
    } else if (_containsAny(
      name,
      ['latte', 'flat white', 'macchiato', 'cafe au lait', 'caffe au lait'],
    )) {
      return _nutrients(
        energyKcal: _scaled(150.0, volumeMilliliters, 354.0),
        totalFatGrams: _scaled(6.0, volumeMilliliters, 354.0),
        saturatedFatGrams: _scaled(3.5, volumeMilliliters, 354.0),
        totalCarbohydrateGrams: _scaled(15.0, volumeMilliliters, 354.0),
        sugarGrams: _scaled(13.0, volumeMilliliters, 354.0),
        proteinGrams: _scaled(10.0, volumeMilliliters, 354.0),
        sodiumMilligrams: _scaled(125.0, volumeMilliliters, 354.0),
        potassiumMilligrams: _scaled(400.0, volumeMilliliters, 354.0),
        calciumMilligrams: _scaled(300.0, volumeMilliliters, 354.0),
      );
    } else if (_containsAny(
      name,
      ['espresso', 'americano', 'nespresso', 'turkish'],
    )) {
      return _nutrients(
        energyKcal: _scaled(5.0, volumeMilliliters, 60.0),
        totalCarbohydrateGrams: _scaled(1.0, volumeMilliliters, 60.0),
        sodiumMilligrams: _scaled(8.0, volumeMilliliters, 60.0),
        potassiumMilligrams: _scaled(69.0, volumeMilliliters, 60.0),
        calciumMilligrams: _scaled(1.0, volumeMilliliters, 60.0),
      );
    } else {
      return _nutrients(
        energyKcal: _scaled(2.0, volumeMilliliters, 240.0),
        totalFatGrams: _scaled(0.05, volumeMilliliters, 240.0),
        proteinGrams: _scaled(0.3, volumeMilliliters, 240.0),
        sodiumMilligrams: _scaled(5.0, volumeMilliliters, 240.0),
        potassiumMilligrams: _scaled(116.0, volumeMilliliters, 240.0),
        calciumMilligrams: _scaled(5.0, volumeMilliliters, 240.0),
      );
    }
  }

  static Map<NutritionNutrient, double> _teaDefaults(
    String name,
    double volumeMilliliters,
  ) {
    if (_containsAny(name, ['guayak', 'yerba'])) {
      return _nutrients(
        energyKcal: _scaled(120.0, volumeMilliliters, 458.0),
        totalCarbohydrateGrams: _scaled(31.0, volumeMilliliters, 458.0),
        sugarGrams: _scaled(28.0, volumeMilliliters, 458.0),
        proteinGrams: _scaled(1.0, volumeMilliliters, 458.0),
        sodiumMilligrams: _scaled(15.0, volumeMilliliters, 458.0),
      );
    } else if (_containsAny(name, ['iced', 'lipton', 'fuze', 'nestea'])) {
      return _nutrients(
        energyKcal: _scaled(100.0, volumeMilliliters, 500.0),
        totalCarbohydrateGrams: _scaled(25.0, volumeMilliliters, 500.0),
        sugarGrams: _scaled(25.0, volumeMilliliters, 500.0),
        sodiumMilligrams: _scaled(90.0, volumeMilliliters, 500.0),
      );
    } else if (_containsAny(name, ['green', 'jasmine', 'matcha'])) {
      return _nutrients(
        energyKcal: _scaled(2.0, volumeMilliliters, 240.0),
        totalCarbohydrateGrams: _scaled(0.5, volumeMilliliters, 240.0),
        sodiumMilligrams: _scaled(5.0, volumeMilliliters, 240.0),
        potassiumMilligrams: _scaled(70.0, volumeMilliliters, 240.0),
        calciumMilligrams: _scaled(2.0, volumeMilliliters, 240.0),
      );
    } else if (name.contains('herbal')) {
      return _nutrients(
        energyKcal: _scaled(1.0, volumeMilliliters, 240.0),
        potassiumMilligrams: _scaled(20.0, volumeMilliliters, 240.0),
        calciumMilligrams: _scaled(2.0, volumeMilliliters, 240.0),
      );
    } else {
      return _nutrients(
        energyKcal: _scaled(2.0, volumeMilliliters, 240.0),
        totalCarbohydrateGrams: _scaled(0.7, volumeMilliliters, 240.0),
        sodiumMilligrams: _scaled(7.0, volumeMilliliters, 240.0),
        potassiumMilligrams: _scaled(70.0, volumeMilliliters, 240.0),
      );
    }
  }

  static Map<NutritionNutrient, double> _sodaDefaults(
    String name,
    double volumeMilliliters,
  ) {
    if (_containsAny(name, ['diet', 'zero', 'max', 'crystal light'])) {
      return _nutrients(
        sodiumMilligrams: _scaled(35.0, volumeMilliliters, 355.0),
      );
    } else {
      return _nutrients(
        energyKcal: _scaled(150.0, volumeMilliliters, 355.0),
        totalCarbohydrateGrams: _scaled(39.0, volumeMilliliters, 355.0),
        sugarGrams: _scaled(39.0, volumeMilliliters, 355.0),
        sodiumMilligrams: _scaled(85.0, volumeMilliliters, 355.0),
        potassiumMilligrams: _scaled(10.0, volumeMilliliters, 355.0),
        calciumMilligrams: _scaled(5.0, volumeMilliliters, 355.0),
      );
    }
  }

  static Map<NutritionNutrient, double> _energyDrinkDefaults(
    String name,
    double volumeMilliliters,
  ) {
    if (_containsAny(name, ['5 hour', 'eternal energy'])) {
      return _nutrients(
        energyKcal: 4.0,
        sodiumMilligrams: 15.0,
      );
    } else if (_containsAny(name, ['v8', 'juice', 'rehab', 'kickstart'])) {
      return _nutrients(
        energyKcal: _scaled(50.0, volumeMilliliters, 237.0),
        totalCarbohydrateGrams: _scaled(12.0, volumeMilliliters, 237.0),
        sugarGrams: _scaled(10.0, volumeMilliliters, 237.0),
        sodiumMilligrams: _scaled(60.0, volumeMilliliters, 237.0),
      );
    } else if (_containsAny(name, [
      'zero',
      'sugarfree',
      'sugar free',
      'diet',
      'ultra',
      'bang',
      'reign',
      'c4',
      'celsius',
      'nocco',
      'ghost',
      'gorilla',
      'jocko',
      'gorgie',
      'true north',
    ])) {
      return _nutrients(
        sodiumMilligrams: _scaled(60.0, volumeMilliliters, 473.0),
      );
    } else {
      return _nutrients(
        energyKcal: _scaled(110.0, volumeMilliliters, 250.0),
        totalCarbohydrateGrams: _scaled(27.0, volumeMilliliters, 250.0),
        sugarGrams: _scaled(27.0, volumeMilliliters, 250.0),
        sodiumMilligrams: _scaled(105.0, volumeMilliliters, 250.0),
      );
    }
  }

  static Map<NutritionNutrient, double> _chocolateDefaults(
    String name,
    double volumeMilliliters,
  ) {
    if (name.contains('dunkin')) {
      return _nutrients(
        energyKcal: _scaled(330.0, volumeMilliliters, 414.0),
        totalFatGrams: _scaled(11.0, volumeMilliliters, 414.0),
        saturatedFatGrams: _scaled(9.0, volumeMilliliters, 414.0),
        totalCarbohydrateGrams: _scaled(59.0, volumeMilliliters, 414.0),
        sugarGrams: _scaled(46.0, volumeMilliliters, 414.0),
        proteinGrams: _scaled(3.0, volumeMilliliters, 414.0),
        sodiumMilligrams: _scaled(320.0, volumeMilliliters, 414.0),
        potassiumMilligrams: _scaled(250.0, volumeMilliliters, 414.0),
        calciumMilligrams: _scaled(40.0, volumeMilliliters, 414.0),
      );
    } else {
      return _nutrients(
        energyKcal: _scaled(90.0, volumeMilliliters, 240.0),
        totalFatGrams: _scaled(2.0, volumeMilliliters, 240.0),
        saturatedFatGrams: _scaled(1.5, volumeMilliliters, 240.0),
        totalCarbohydrateGrams: _scaled(16.0, volumeMilliliters, 240.0),
        sugarGrams: _scaled(14.0, volumeMilliliters, 240.0),
        dietaryFiberGrams: _scaled(1.0, volumeMilliliters, 240.0),
        proteinGrams: _scaled(1.0, volumeMilliliters, 240.0),
        sodiumMilligrams: _scaled(170.0, volumeMilliliters, 240.0),
        calciumMilligrams: _scaled(40.0, volumeMilliliters, 240.0),
      );
    }
  }

  static Map<NutritionNutrient, double> _withCaffeine(
    Map<NutritionNutrient, double> base,
    double caffeineMilligrams,
  ) {
    if (caffeineMilligrams > 0.0 && caffeineMilligrams.isFinite) {
      return <NutritionNutrient, double>{
        ...base,
        NutritionNutrient.caffeine: caffeineMilligrams / _milligramsPerGram,
      };
    }
    return base;
  }

  static Map<NutritionNutrient, double> _nutrients({
    double? energyKcal,
    double? totalFatGrams,
    double? saturatedFatGrams,
    double? totalCarbohydrateGrams,
    double? sugarGrams,
    double? dietaryFiberGrams,
    double? proteinGrams,
    double? sodiumMilligrams,
    double? potassiumMilligrams,
    double? calciumMilligrams,
  }) {
    final result = <NutritionNutrient, double>{};
    _putPositive(result, NutritionNutrient.energy, energyKcal);
    _putPositive(result, NutritionNutrient.totalFat, totalFatGrams);
    _putPositive(result, NutritionNutrient.saturatedFat, saturatedFatGrams);
    _putPositive(
      result,
      NutritionNutrient.totalCarbohydrate,
      totalCarbohydrateGrams,
    );
    _putPositive(result, NutritionNutrient.sugar, sugarGrams);
    _putPositive(result, NutritionNutrient.dietaryFiber, dietaryFiberGrams);
    _putPositive(result, NutritionNutrient.protein, proteinGrams);
    _putPositive(
      result,
      NutritionNutrient.sodium,
      sodiumMilligrams == null ? null : sodiumMilligrams / _milligramsPerGram,
    );
    _putPositive(
      result,
      NutritionNutrient.potassium,
      potassiumMilligrams == null
          ? null
          : potassiumMilligrams / _milligramsPerGram,
    );
    _putPositive(
      result,
      NutritionNutrient.calcium,
      calciumMilligrams == null ? null : calciumMilligrams / _milligramsPerGram,
    );
    return result;
  }

  static void _putPositive(
    Map<NutritionNutrient, double> target,
    NutritionNutrient nutrient,
    double? value,
  ) {
    if (value != null && value > 0.0 && value.isFinite) {
      target[nutrient] = value;
    }
  }

  static double _scaled(
    double value,
    double volumeMilliliters,
    double sourceMilliliters,
  ) =>
      value * volumeMilliliters / sourceMilliliters;

  static bool _containsAny(String value, List<String> needles) =>
      needles.any(value.contains);

  static const double _milligramsPerGram = 1000.0;
}
