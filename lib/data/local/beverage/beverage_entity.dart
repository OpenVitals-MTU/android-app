import '../../../domain/insights/caffeine_health_drink_catalog.dart';
import '../../../domain/model/caffeine_models.dart';
import '../../../domain/model/nutrition_models.dart';

/// Row model for the `beverages` table.
///
/// This mirrors the Kotlin Room `BeverageEntity` one-to-one and doubles as the
/// drift row class (via `@UseRowClass`), so its constructor parameters must
/// match the table columns exactly.
class BeverageEntity {
  const BeverageEntity({
    required this.id,
    required this.name,
    required this.category,
    required this.volumeMilliliters,
    required this.hydrationMultiplier,
    required this.isPreloaded,
    this.isDeleted = false,
    required this.sortOrder,
    this.energyKcal,
    this.proteinGrams,
    this.totalCarbohydrateGrams,
    this.totalFatGrams,
    this.dietaryFiberGrams,
    this.sugarGrams,
    this.saturatedFatGrams,
    this.sodiumGrams,
    this.potassiumGrams,
    this.calciumGrams,
    this.caffeineGrams,
  });

  final String id;
  final String name;
  final String? category;
  final double volumeMilliliters;
  final double hydrationMultiplier;
  final bool isPreloaded;
  final bool isDeleted;
  final int sortOrder;
  final double? energyKcal;
  final double? proteinGrams;
  final double? totalCarbohydrateGrams;
  final double? totalFatGrams;
  final double? dietaryFiberGrams;
  final double? sugarGrams;
  final double? saturatedFatGrams;
  final double? sodiumGrams;
  final double? potassiumGrams;
  final double? calciumGrams;
  final double? caffeineGrams;

  CustomHydrationDrink toDomain() => CustomHydrationDrink(
        id: id,
        name: name,
        volumeMilliliters: volumeMilliliters,
        hydrationMultiplier: hydrationMultiplier,
        nutrientValues: _nutrientValues(),
        category: category == null
            ? null
            : CaffeineSourceCategory.fromStorage(category!),
        isPreloaded: isPreloaded,
      );

  Map<NutritionNutrient, double> _nutrientValues() {
    final values = <NutritionNutrient, double>{};
    void putPositive(NutritionNutrient nutrient, double? value) {
      if (value != null && value > 0.0 && value.isFinite) {
        values[nutrient] = value;
      }
    }

    putPositive(NutritionNutrient.energy, energyKcal);
    putPositive(NutritionNutrient.protein, proteinGrams);
    putPositive(NutritionNutrient.totalCarbohydrate, totalCarbohydrateGrams);
    putPositive(NutritionNutrient.totalFat, totalFatGrams);
    putPositive(NutritionNutrient.dietaryFiber, dietaryFiberGrams);
    putPositive(NutritionNutrient.sugar, sugarGrams);
    putPositive(NutritionNutrient.saturatedFat, saturatedFatGrams);
    putPositive(NutritionNutrient.sodium, sodiumGrams);
    putPositive(NutritionNutrient.potassium, potassiumGrams);
    putPositive(NutritionNutrient.calcium, calciumGrams);
    putPositive(NutritionNutrient.caffeine, caffeineGrams);
    return values;
  }

  BeverageEntity copyWith({
    String? id,
    String? name,
    String? category,
    double? volumeMilliliters,
    double? hydrationMultiplier,
    bool? isPreloaded,
    bool? isDeleted,
    int? sortOrder,
  }) =>
      BeverageEntity(
        id: id ?? this.id,
        name: name ?? this.name,
        category: category ?? this.category,
        volumeMilliliters: volumeMilliliters ?? this.volumeMilliliters,
        hydrationMultiplier: hydrationMultiplier ?? this.hydrationMultiplier,
        isPreloaded: isPreloaded ?? this.isPreloaded,
        isDeleted: isDeleted ?? this.isDeleted,
        sortOrder: sortOrder ?? this.sortOrder,
        energyKcal: energyKcal,
        proteinGrams: proteinGrams,
        totalCarbohydrateGrams: totalCarbohydrateGrams,
        totalFatGrams: totalFatGrams,
        dietaryFiberGrams: dietaryFiberGrams,
        sugarGrams: sugarGrams,
        saturatedFatGrams: saturatedFatGrams,
        sodiumGrams: sodiumGrams,
        potassiumGrams: potassiumGrams,
        calciumGrams: calciumGrams,
        caffeineGrams: caffeineGrams,
      );

  static const String beveragePresetIdPrefix = 'caffeinehealth-';

  static List<BeverageEntity> preloadedDefaults() {
    final presets = CaffeineHealthDrinkCatalog.beveragePresets();
    return <BeverageEntity>[
      ..._waterDefaults(),
      for (var index = 0; index < presets.length; index++)
        BeverageEntity.fromDomain(
          drink: presets[index],
          sortOrder: index + 2,
          isPreloaded: true,
          category: presets[index].category,
        ),
    ];
  }

  static List<BeverageEntity> _waterDefaults() => <BeverageEntity>[
        _waterDefault(
          id: 'openvitals-still-water',
          name: 'Still water',
          sortOrder: 0,
        ),
        _waterDefault(
          id: 'openvitals-gasified-water',
          name: 'Gasified water',
          sortOrder: 1,
        ),
      ];

  static BeverageEntity _waterDefault({
    required String id,
    required String name,
    required int sortOrder,
  }) =>
      BeverageEntity.fromDomain(
        drink: CustomHydrationDrink(
          id: id,
          name: name,
          volumeMilliliters: 100.0,
          hydrationMultiplier: 1.0,
          nutrientValues: const <NutritionNutrient, double>{},
          category: CaffeineSourceCategory.water,
          isPreloaded: true,
        ),
        sortOrder: sortOrder,
        isPreloaded: true,
        category: CaffeineSourceCategory.water,
      );

  /// Mirrors the Kotlin `fromDomain`, whose `isPreloaded`/`category`
  /// parameters default to `drink.isPreloaded`/`drink.category`. Every call
  /// site passes these explicitly, so the `?? drink.*` fallback only supplies
  /// the Kotlin default when a caller omits them.
  static BeverageEntity fromDomain({
    required CustomHydrationDrink drink,
    required int sortOrder,
    bool? isPreloaded,
    CaffeineSourceCategory? category,
  }) {
    final resolvedCategory = category ?? drink.category;
    final nutrients = drink.nutrientValues;
    return BeverageEntity(
      id: drink.id,
      name: drink.name,
      category: resolvedCategory?.storageName,
      volumeMilliliters: drink.volumeMilliliters,
      hydrationMultiplier: drink.hydrationMultiplier,
      isPreloaded: isPreloaded ?? drink.isPreloaded,
      sortOrder: sortOrder,
      energyKcal: nutrients[NutritionNutrient.energy],
      proteinGrams: nutrients[NutritionNutrient.protein],
      totalCarbohydrateGrams: nutrients[NutritionNutrient.totalCarbohydrate],
      totalFatGrams: nutrients[NutritionNutrient.totalFat],
      dietaryFiberGrams: nutrients[NutritionNutrient.dietaryFiber],
      sugarGrams: nutrients[NutritionNutrient.sugar],
      saturatedFatGrams: nutrients[NutritionNutrient.saturatedFat],
      sodiumGrams: nutrients[NutritionNutrient.sodium],
      potassiumGrams: nutrients[NutritionNutrient.potassium],
      calciumGrams: nutrients[NutritionNutrient.calcium],
      caffeineGrams: nutrients[NutritionNutrient.caffeine],
    );
  }
}
