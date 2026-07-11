import 'package:freezed_annotation/freezed_annotation.dart';

import '../model/heart_models.dart';

part 'heart_period_data.freezed.dart';

@freezed
abstract class HeartPeriodData with _$HeartPeriodData {
  const factory HeartPeriodData({
    @Default(<HeartRateSample>[]) List<HeartRateSample> daySamples,
    @Default(<HeartRateSample>[]) List<HeartRateSample> previousDaySamples,
    @Default(<HeartRateSummary>[]) List<HeartRateSummary> dailySummaries,
    @Default(<HeartRateSummary>[]) List<HeartRateSummary> previousDailySummaries,
    @Default(<HeartRateSummary>[]) List<HeartRateSummary> baselineDailySummaries,
    @Default(<RestingHeartRateSample>[])
    List<RestingHeartRateSample> dayRestingSamples,
    int? dayRestingBpm,
    int? previousDayRestingBpm,
    @Default(<HrvSample>[]) List<HrvSample> dayHrvSamples,
    double? dayHrvMs,
    double? previousDayHrvMs,
    @Default(<DailyRestingHR>[]) List<DailyRestingHR> dailyRestingHR,
    @Default(<DailyRestingHR>[]) List<DailyRestingHR> previousDailyRestingHR,
    @Default(<DailyRestingHR>[]) List<DailyRestingHR> baselineDailyRestingHR,
    @Default(<DailyHrv>[]) List<DailyHrv> dailyHrv,
    @Default(<DailyHrv>[]) List<DailyHrv> previousDailyHrv,
    @Default(<DailyHrv>[]) List<DailyHrv> baselineDailyHrv,
  }) = _HeartPeriodData;
}
