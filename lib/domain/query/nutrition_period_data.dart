import 'package:freezed_annotation/freezed_annotation.dart';

import '../model/nutrition_models.dart';

part 'nutrition_period_data.freezed.dart';

@freezed
abstract class NutritionPeriodData with _$NutritionPeriodData {
  const factory NutritionPeriodData({
    @Default(<DailyMacros>[]) List<DailyMacros> dailyMacros,
    @Default(<DailyMacros>[]) List<DailyMacros> previousDailyMacros,
    @Default(<DailyMacros>[]) List<DailyMacros> baselineDailyMacros,
    @Default(<NutritionEntry>[]) List<NutritionEntry> entries,
  }) = _NutritionPeriodData;
}
