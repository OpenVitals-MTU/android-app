import 'package:freezed_annotation/freezed_annotation.dart';

import '../model/nutrition_models.dart';

part 'hydration_period_data.freezed.dart';

@freezed
abstract class HydrationPeriodData with _$HydrationPeriodData {
  const factory HydrationPeriodData({
    @Default(<DailyHydration>[]) List<DailyHydration> dailyHydration,
    @Default(<DailyHydration>[]) List<DailyHydration> previousDailyHydration,
    @Default(<DailyHydration>[]) List<DailyHydration> baselineDailyHydration,
    @Default(<HydrationEntry>[]) List<HydrationEntry> hydrationEntries,
  }) = _HydrationPeriodData;
}
