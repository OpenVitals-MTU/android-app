import 'package:flutter/material.dart';

import '../../../core/presentation/display_value.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/theme/app_colors.dart';

/// Per-nutrient presentation helpers, ported from the Kotlin
/// `NutritionFormatting`.

const Color _proteinColor = Color(0xFF7E57C2);
const Color _carbsColor = Color(0xFF26A69A);
const Color _fatColor = Color(0xFFFFB300);
const Color _vitaminColor = Color(0xFF5E7CE2);
const Color _mineralColor = Color(0xFF8D6E63);
const Color _otherColor = Color(0xFF00897B);

/// Formats [value] for [nutrient] using its unit (energy / mass grams /
/// adaptive mass), mirroring the Kotlin `NutritionNutrient.displayValue`.
DisplayValue nutrientDisplayValue(
  NutritionNutrient nutrient,
  double value,
  UnitFormatter formatter,
) {
  switch (nutrient.unit) {
    case NutritionNutrientUnit.energyKcal:
      return formatter.energy(value);
    case NutritionNutrientUnit.massGrams:
      return DisplayValue(formatter.count(value.round()), 'g');
    case NutritionNutrientUnit.massAdaptive:
      return _adaptiveMassDisplay(value, formatter);
  }
}

DisplayValue _adaptiveMassDisplay(double grams, UnitFormatter formatter) {
  final milligrams = grams * 1000.0;
  final micrograms = grams * 1000000.0;
  if (grams >= 1.0) {
    return DisplayValue(formatter.decimal(grams, grams < 10.0 ? 1 : 0), 'g');
  }
  if (milligrams >= 1.0) {
    return DisplayValue(
      formatter.decimal(milligrams, milligrams < 10.0 ? 1 : 0),
      'mg',
    );
  }
  return DisplayValue(
    formatter.decimal(micrograms, micrograms < 10.0 ? 1 : 0),
    'mcg',
  );
}

/// The accent colour for [nutrient], mirroring the Kotlin
/// `NutritionNutrient.color`.
Color nutrientColor(NutritionNutrient nutrient) {
  switch (nutrient.group) {
    case NutritionNutrientGroup.overview:
      switch (nutrient) {
        case NutritionNutrient.energy:
          return AppColors.nutrition;
        case NutritionNutrient.protein:
          return _proteinColor;
        case NutritionNutrient.totalCarbohydrate:
          return _carbsColor;
        case NutritionNutrient.totalFat:
          return _fatColor;
        default:
          return AppColors.nutrition;
      }
    case NutritionNutrientGroup.carbohydrates:
      return _carbsColor;
    case NutritionNutrientGroup.fats:
      return _fatColor;
    case NutritionNutrientGroup.vitamins:
      return _vitaminColor;
    case NutritionNutrientGroup.minerals:
      return _mineralColor;
    case NutritionNutrientGroup.other:
      return _otherColor;
  }
}

/// The display title for [group], mirroring the Kotlin
/// `NutritionNutrientGroup.titleRes`.
String nutrientGroupTitle(NutritionNutrientGroup group) {
  switch (group) {
    case NutritionNutrientGroup.overview:
      return 'Nutrition';
    case NutritionNutrientGroup.carbohydrates:
      return 'Carbohydrates';
    case NutritionNutrientGroup.fats:
      return 'Fats';
    case NutritionNutrientGroup.vitamins:
      return 'Vitamins';
    case NutritionNutrientGroup.minerals:
      return 'Minerals';
    case NutritionNutrientGroup.other:
      return 'Other nutrients';
  }
}

/// The localized display title for [nutrient]. Port of the Kotlin
/// `NutritionNutrient.titleRes()` — the same `metric_*` strings, so a nutrient
/// reads identically wherever it appears.
String nutrientTitle(NutritionNutrient nutrient, AppLocalizations l10n) =>
    switch (nutrient) {
      NutritionNutrient.energy => l10n.metricCaloriesIn,
      NutritionNutrient.protein => l10n.metricProtein,
      NutritionNutrient.totalCarbohydrate => l10n.metricCarbs,
      NutritionNutrient.totalFat => l10n.metricFat,
      NutritionNutrient.dietaryFiber => l10n.metricDietaryFiber,
      NutritionNutrient.sugar => l10n.metricSugar,
      NutritionNutrient.energyFromFat => l10n.metricEnergyFromFat,
      NutritionNutrient.monounsaturatedFat => l10n.metricMonounsaturatedFat,
      NutritionNutrient.polyunsaturatedFat => l10n.metricPolyunsaturatedFat,
      NutritionNutrient.saturatedFat => l10n.metricSaturatedFat,
      NutritionNutrient.transFat => l10n.metricTransFat,
      NutritionNutrient.unsaturatedFat => l10n.metricUnsaturatedFat,
      NutritionNutrient.cholesterol => l10n.metricCholesterol,
      NutritionNutrient.biotin => l10n.metricBiotin,
      NutritionNutrient.folate => l10n.metricFolate,
      NutritionNutrient.folicAcid => l10n.metricFolicAcid,
      NutritionNutrient.niacin => l10n.metricNiacin,
      NutritionNutrient.pantothenicAcid => l10n.metricPantothenicAcid,
      NutritionNutrient.riboflavin => l10n.metricRiboflavin,
      NutritionNutrient.thiamin => l10n.metricThiamin,
      NutritionNutrient.vitaminA => l10n.metricVitaminA,
      NutritionNutrient.vitaminB12 => l10n.metricVitaminB12,
      NutritionNutrient.vitaminB6 => l10n.metricVitaminB6,
      NutritionNutrient.vitaminC => l10n.metricVitaminC,
      NutritionNutrient.vitaminD => l10n.metricVitaminD,
      NutritionNutrient.vitaminE => l10n.metricVitaminE,
      NutritionNutrient.vitaminK => l10n.metricVitaminK,
      NutritionNutrient.calcium => l10n.metricCalcium,
      NutritionNutrient.chloride => l10n.metricChloride,
      NutritionNutrient.chromium => l10n.metricChromium,
      NutritionNutrient.copper => l10n.metricCopper,
      NutritionNutrient.iodine => l10n.metricIodine,
      NutritionNutrient.iron => l10n.metricIron,
      NutritionNutrient.magnesium => l10n.metricMagnesium,
      NutritionNutrient.manganese => l10n.metricManganese,
      NutritionNutrient.molybdenum => l10n.metricMolybdenum,
      NutritionNutrient.phosphorus => l10n.metricPhosphorus,
      NutritionNutrient.potassium => l10n.metricPotassium,
      NutritionNutrient.selenium => l10n.metricSelenium,
      NutritionNutrient.sodium => l10n.metricSodium,
      NutritionNutrient.zinc => l10n.metricZinc,
      NutritionNutrient.caffeine => l10n.metricCaffeine,
    };
