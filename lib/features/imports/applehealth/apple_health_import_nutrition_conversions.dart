part of 'apple_health_import_converter.dart';

/// Nutrition grouping + accumulation, ported from the Kotlin
/// `AppleHealthImportNutritionConversions.kt` and `AppleHealthImportNutritionValues.kt`.
///
/// The Kotlin version accumulates Health Connect `Mass`/`Energy` values; the
/// Dart importer stores each nutrient mass in grams plus energy in kilocalories.
class NutritionValues {
  final Map<String, double> grams = {};
  double? energyKilocalories;
  bool hasAny = false;

  static const Map<String, String> _massNutrientKeys = {
    appleDietaryFatTotal: 'totalFat',
    appleDietaryFatSaturated: 'saturatedFat',
    appleDietaryFatTrans: 'transFat',
    appleDietaryFatMonounsaturated: 'monounsaturatedFat',
    appleDietaryFatPolyunsaturated: 'polyunsaturatedFat',
    appleDietaryCholesterol: 'cholesterol',
    appleDietarySodium: 'sodium',
    appleDietaryCarbohydrates: 'totalCarbohydrate',
    appleDietaryFiber: 'dietaryFiber',
    appleDietarySugar: 'sugar',
    appleDietaryProtein: 'protein',
    appleDietaryCaffeine: 'caffeine',
    appleDietaryCalcium: 'calcium',
    appleDietaryIron: 'iron',
    appleDietaryThiamin: 'thiamin',
    appleDietaryRiboflavin: 'riboflavin',
    appleDietaryNiacin: 'niacin',
    appleDietaryFolate: 'folate',
    appleDietaryBiotin: 'biotin',
    appleDietaryPantothenicAcid: 'pantothenicAcid',
    appleDietaryPhosphorus: 'phosphorus',
    appleDietaryIodine: 'iodine',
    appleDietaryMagnesium: 'magnesium',
    appleDietaryZinc: 'zinc',
    appleDietarySelenium: 'selenium',
    appleDietaryCopper: 'copper',
    appleDietaryManganese: 'manganese',
    appleDietaryChromium: 'chromium',
    appleDietaryMolybdenum: 'molybdenum',
    appleDietaryPotassium: 'potassium',
    appleDietaryVitaminA: 'vitaminA',
    appleDietaryVitaminB6: 'vitaminB6',
    appleDietaryVitaminB12: 'vitaminB12',
    appleDietaryVitaminC: 'vitaminC',
    appleDietaryVitaminD: 'vitaminD',
    appleDietaryVitaminE: 'vitaminE',
    appleDietaryVitaminK: 'vitaminK',
  };

  bool apply(String type, double value, String? unit) {
    bool applied;
    if (type == appleDietaryEnergyConsumed) {
      final kcal = toKilocalories(value, unit);
      if (kcal != null) energyKilocalories = kcal;
      applied = kcal != null;
    } else {
      final key = _massNutrientKeys[type];
      if (key == null) {
        applied = false;
      } else {
        final grams_ = toGrams(value, unit);
        if (grams_ != null) grams[key] = grams_;
        applied = grams_ != null;
      }
    }
    if (applied) hasAny = true;
    return applied;
  }
}

extension AppleHealthImportNutritionConversions on AppleHealthImportConverter {
  List<ConvertedAppleRecord> convertNutrition(
    List<AppleRecord> records, {
    bool trackConsumedRecords = true,
  }) {
    final nutritionRecords = records
        .where((it) =>
            appleNutritionTypes.contains(it.type) && it.type != appleDietaryWater)
        .toList();
    if (nutritionRecords.isEmpty) return const [];

    final grouped = <String, List<AppleRecord>>{};
    for (final record in nutritionRecords) {
      final key = [
        record.sourceName ?? '',
        record.startDate?.instant.toIso8601String() ?? '',
        record.endDate?.instant.toIso8601String() ?? '',
        record.metadata['HKMetadataKeyFoodType'] ?? '',
      ].join('|');
      grouped.putIfAbsent(key, () => []).add(record);
    }

    final result = <ConvertedAppleRecord>[];
    for (final group in grouped.values) {
      AppleDateTime? start;
      for (final record in group) {
        final candidate = record.startDate;
        if (candidate != null &&
            (start == null || candidate.instant.isBefore(start.instant))) {
          start = candidate;
        }
      }
      AppleDateTime? end;
      for (final record in group) {
        final candidate = record.endDate ?? record.startDate;
        if (candidate != null &&
            (end == null || candidate.instant.isAfter(end.instant))) {
          end = candidate;
        }
      }
      if (start == null) {
        for (final record in group) {
          invalidRecord(record, 'Nutrition record is missing startDate.');
        }
        continue;
      }
      final iv = interval(start, end ?? start);
      final nutrients = NutritionValues();
      for (final record in group) {
        final value = record.numericValue;
        final applied =
            value != null && nutrients.apply(record.type, value, record.unit);
        if (applied) {
          if (trackConsumedRecords) {
            consumedRecordFingerprints.add(record.sourceFingerprint);
          }
        } else {
          invalidRecord(
            record,
            'Nutrition value is missing or has an unsupported unit.',
          );
        }
      }
      if (!nutrients.hasAny) continue;
      final fingerprint = buildStableClientRecordId(
        'nutrition',
        group.map((it) => it.stableParts()).toList(),
      );
      String? name;
      for (final record in group) {
        final food = record.metadata['HKMetadataKeyFoodType'];
        if (food != null) {
          name = food;
          break;
        }
      }
      markConverted(appleNutritionSyntheticType);
      result.add(ConvertedAppleRecord(
        appleType: appleNutritionSyntheticType,
        targetType: 'NutritionRecord',
        fingerprint: fingerprint,
        record: NutritionImportRecord(
          clientRecordId: fingerprint,
          startTime: iv.start.instant,
          startZoneOffset: iv.start.offset,
          endTime: iv.end.instant,
          endZoneOffset: iv.end.offset,
          name: name,
          nutrientGrams: Map.of(nutrients.grams),
          energyKilocalories: nutrients.energyKilocalories,
        ),
        sourceTimeRange: AppleImportTimeRange(iv.start.instant, iv.end.instant),
        unit: null,
        value: 'nutrients=${group.length}',
      ));
    }
    return result;
  }
}
