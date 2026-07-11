import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/time/local_date.dart';
import 'caffeine_models.dart';

part 'nutrition_models.freezed.dart';

@freezed
abstract class DailyNutrition with _$DailyNutrition {
  const factory DailyNutrition.build({
    required LocalDate date,
    required double hydrationLiters,
    required double caloriesBurnedKcal,
    required CaloriesBurnedSource caloriesBurnedSource,
    required bool hasCaloriesBurnedData,
  }) = _DailyNutrition;

  factory DailyNutrition({
    required LocalDate date,
    required double hydrationLiters,
    required double caloriesBurnedKcal,
    CaloriesBurnedSource? caloriesBurnedSource,
    bool? hasCaloriesBurnedData,
  }) {
    final resolvedSource = caloriesBurnedSource ??
        (caloriesBurnedKcal > 0.0
            ? CaloriesBurnedSource.recordedTotal
            : CaloriesBurnedSource.noData);
    return DailyNutrition.build(
      date: date,
      hydrationLiters: hydrationLiters,
      caloriesBurnedKcal: caloriesBurnedKcal,
      caloriesBurnedSource: resolvedSource,
      hasCaloriesBurnedData:
          hasCaloriesBurnedData ?? (resolvedSource != CaloriesBurnedSource.noData),
    );
  }
}

enum CaloriesBurnedSource {
  noData('NO_DATA'),
  recordedTotal('RECORDED_TOTAL'),
  estimatedActiveAndBmr('ESTIMATED_ACTIVE_AND_BMR');

  const CaloriesBurnedSource(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static CaloriesBurnedSource? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

@freezed
abstract class CaloriesBurnedValue with _$CaloriesBurnedValue {
  const factory CaloriesBurnedValue({
    required double kcal,
    required CaloriesBurnedSource source,
  }) = _CaloriesBurnedValue;
}

@freezed
abstract class DailyHydration with _$DailyHydration {
  const factory DailyHydration({
    required LocalDate date,
    required double liters,
  }) = _DailyHydration;
}

@freezed
abstract class HydrationEntry with _$HydrationEntry {
  const factory HydrationEntry({
    required DateTime startTime,
    required DateTime endTime,
    required double liters,
    required String source,
    @Default('') String id,
    String? clientRecordId,
    @Default(false) bool isOpenVitalsEntry,
    @Default(HydrationEntryRecordType.hydration)
    HydrationEntryRecordType recordType,
    String? displayName,
    @Default(<NutritionNutrient, double>{})
    Map<NutritionNutrient, double> nutrientValues,
  }) = _HydrationEntry;
}

enum HydrationEntryRecordType {
  hydration('HYDRATION'),
  nutritionOnly('NUTRITION_ONLY');

  const HydrationEntryRecordType(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static HydrationEntryRecordType? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

@freezed
abstract class HydrationWriteRequest with _$HydrationWriteRequest {
  const factory HydrationWriteRequest({
    required DateTime time,
    required double volumeLiters,
    String? drinkId,
  }) = _HydrationWriteRequest;
}

@freezed
abstract class CustomHydrationDrink with _$CustomHydrationDrink {
  const CustomHydrationDrink._();

  const factory CustomHydrationDrink({
    required String id,
    required String name,
    required double volumeMilliliters,
    @Default(1.0) double hydrationMultiplier,
    @Default(<NutritionNutrient, double>{})
    Map<NutritionNutrient, double> nutrientValues,
    CaffeineSourceCategory? category,
    @Default(false) bool isPreloaded,
  }) = _CustomHydrationDrink;

  double get volumeLiters => volumeMilliliters / 1000.0;

  double get effectiveHydrationLiters => volumeLiters * hydrationMultiplier;
}

@freezed
abstract class NutritionEntry with _$NutritionEntry {
  const factory NutritionEntry.build({
    required DateTime time,
    required DateTime endTime,
    required int mealType,
    required String? name,
    required double? energyKcal,
    required double? proteinGrams,
    required double? carbsGrams,
    required double? fatGrams,
    required double? fiberGrams,
    required double? sugarGrams,
    required String source,
    required Map<NutritionNutrient, double> nutrientValues,
    required String id,
    required String? clientRecordId,
    required bool isOpenVitalsEntry,
  }) = _NutritionEntry;

  factory NutritionEntry({
    required DateTime time,
    DateTime? endTime,
    required int mealType,
    required String? name,
    required double? energyKcal,
    required double? proteinGrams,
    required double? carbsGrams,
    required double? fatGrams,
    required double? fiberGrams,
    required double? sugarGrams,
    required String source,
    Map<NutritionNutrient, double> nutrientValues =
        const <NutritionNutrient, double>{},
    String id = '',
    String? clientRecordId,
    bool isOpenVitalsEntry = false,
  }) =>
      NutritionEntry.build(
        time: time,
        endTime: endTime ?? time,
        mealType: mealType,
        name: name,
        energyKcal: energyKcal,
        proteinGrams: proteinGrams,
        carbsGrams: carbsGrams,
        fatGrams: fatGrams,
        fiberGrams: fiberGrams,
        sugarGrams: sugarGrams,
        source: source,
        nutrientValues: nutrientValues,
        id: id,
        clientRecordId: clientRecordId,
        isOpenVitalsEntry: isOpenVitalsEntry,
      );
}

@freezed
abstract class NutritionWriteRequest with _$NutritionWriteRequest {
  const NutritionWriteRequest._();

  const factory NutritionWriteRequest({
    required DateTime time,
    required Map<NutritionNutrient, double> nutrientValues,
    String? name,
    String? associatedHydrationClientRecordId,
  }) = _NutritionWriteRequest;

  factory NutritionWriteRequest.carbs(DateTime time, double carbsGrams) =>
      NutritionWriteRequest(
        time: time,
        nutrientValues: {NutritionNutrient.totalCarbohydrate: carbsGrams},
        name: 'OpenVitals carbs',
      );

  double get carbsGrams =>
      nutrientValues[NutritionNutrient.totalCarbohydrate] ?? 0.0;
}

@freezed
abstract class DailyMacros with _$DailyMacros {
  const factory DailyMacros.build({
    required LocalDate date,
    required Map<NutritionNutrient, double> nutrientValues,
    required double energyKcal,
    required double proteinGrams,
    required double carbsGrams,
    required double fatGrams,
  }) = _DailyMacros;

  factory DailyMacros({
    required LocalDate date,
    Map<NutritionNutrient, double> nutrientValues =
        const <NutritionNutrient, double>{},
    double? energyKcal,
    double? proteinGrams,
    double? carbsGrams,
    double? fatGrams,
  }) =>
      DailyMacros.build(
        date: date,
        nutrientValues: nutrientValues,
        energyKcal: energyKcal ?? (nutrientValues[NutritionNutrient.energy] ?? 0.0),
        proteinGrams:
            proteinGrams ?? (nutrientValues[NutritionNutrient.protein] ?? 0.0),
        carbsGrams: carbsGrams ??
            (nutrientValues[NutritionNutrient.totalCarbohydrate] ?? 0.0),
        fatGrams:
            fatGrams ?? (nutrientValues[NutritionNutrient.totalFat] ?? 0.0),
      );
}

enum NutritionNutrient {
  energy('ENERGY', NutritionNutrientUnit.energyKcal, NutritionNutrientGroup.overview),
  protein('PROTEIN', NutritionNutrientUnit.massGrams, NutritionNutrientGroup.overview),
  totalCarbohydrate('TOTAL_CARBOHYDRATE', NutritionNutrientUnit.massGrams,
      NutritionNutrientGroup.overview),
  totalFat('TOTAL_FAT', NutritionNutrientUnit.massGrams,
      NutritionNutrientGroup.overview),
  dietaryFiber('DIETARY_FIBER', NutritionNutrientUnit.massGrams,
      NutritionNutrientGroup.carbohydrates),
  sugar('SUGAR', NutritionNutrientUnit.massGrams,
      NutritionNutrientGroup.carbohydrates),
  energyFromFat('ENERGY_FROM_FAT', NutritionNutrientUnit.energyKcal,
      NutritionNutrientGroup.fats),
  monounsaturatedFat('MONOUNSATURATED_FAT', NutritionNutrientUnit.massGrams,
      NutritionNutrientGroup.fats),
  polyunsaturatedFat('POLYUNSATURATED_FAT', NutritionNutrientUnit.massGrams,
      NutritionNutrientGroup.fats),
  saturatedFat('SATURATED_FAT', NutritionNutrientUnit.massGrams,
      NutritionNutrientGroup.fats),
  transFat('TRANS_FAT', NutritionNutrientUnit.massGrams,
      NutritionNutrientGroup.fats),
  unsaturatedFat('UNSATURATED_FAT', NutritionNutrientUnit.massGrams,
      NutritionNutrientGroup.fats),
  cholesterol('CHOLESTEROL', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.fats),
  biotin('BIOTIN', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.vitamins),
  folate('FOLATE', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.vitamins),
  folicAcid('FOLIC_ACID', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.vitamins),
  niacin('NIACIN', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.vitamins),
  pantothenicAcid('PANTOTHENIC_ACID', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.vitamins),
  riboflavin('RIBOFLAVIN', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.vitamins),
  thiamin('THIAMIN', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.vitamins),
  vitaminA('VITAMIN_A', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.vitamins),
  vitaminB12('VITAMIN_B12', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.vitamins),
  vitaminB6('VITAMIN_B6', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.vitamins),
  vitaminC('VITAMIN_C', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.vitamins),
  vitaminD('VITAMIN_D', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.vitamins),
  vitaminE('VITAMIN_E', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.vitamins),
  vitaminK('VITAMIN_K', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.vitamins),
  calcium('CALCIUM', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.minerals),
  chloride('CHLORIDE', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.minerals),
  chromium('CHROMIUM', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.minerals),
  copper('COPPER', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.minerals),
  iodine('IODINE', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.minerals),
  iron('IRON', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.minerals),
  magnesium('MAGNESIUM', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.minerals),
  manganese('MANGANESE', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.minerals),
  molybdenum('MOLYBDENUM', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.minerals),
  phosphorus('PHOSPHORUS', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.minerals),
  potassium('POTASSIUM', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.minerals),
  selenium('SELENIUM', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.minerals),
  sodium('SODIUM', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.minerals),
  zinc('ZINC', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.minerals),
  caffeine('CAFFEINE', NutritionNutrientUnit.massAdaptive,
      NutritionNutrientGroup.other);

  const NutritionNutrient(this.storageName, this.unit, this.group);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;
  final NutritionNutrientUnit unit;
  final NutritionNutrientGroup group;

  static NutritionNutrient? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

enum NutritionNutrientUnit {
  energyKcal('ENERGY_KCAL'),
  massGrams('MASS_GRAMS'),
  massAdaptive('MASS_ADAPTIVE');

  const NutritionNutrientUnit(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static NutritionNutrientUnit? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

enum NutritionNutrientGroup {
  overview('OVERVIEW'),
  carbohydrates('CARBOHYDRATES'),
  fats('FATS'),
  vitamins('VITAMINS'),
  minerals('MINERALS'),
  other('OTHER');

  const NutritionNutrientGroup(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static NutritionNutrientGroup? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

extension DailyMacrosValueFor on DailyMacros {
  double valueFor(NutritionNutrient nutrient) =>
      nutrientValues[nutrient] ??
      switch (nutrient) {
        NutritionNutrient.energy => energyKcal,
        NutritionNutrient.protein => proteinGrams,
        NutritionNutrient.totalCarbohydrate => carbsGrams,
        NutritionNutrient.totalFat => fatGrams,
        _ => 0.0,
      };
}

extension NutritionEntryValueFor on NutritionEntry {
  double? valueFor(NutritionNutrient nutrient) =>
      nutrientValues[nutrient] ??
      switch (nutrient) {
        NutritionNutrient.energy => energyKcal,
        NutritionNutrient.protein => proteinGrams,
        NutritionNutrient.totalCarbohydrate => carbsGrams,
        NutritionNutrient.totalFat => fatGrams,
        NutritionNutrient.dietaryFiber => fiberGrams,
        NutritionNutrient.sugar => sugarGrams,
        _ => null,
      };
}
