import 'package:freezed_annotation/freezed_annotation.dart';

import '../model/sleep_models.dart';

part 'sleep_period_data.freezed.dart';

@freezed
abstract class SleepPeriodData with _$SleepPeriodData {
  const factory SleepPeriodData({
    @Default(<SleepData>[]) List<SleepData> sessions,
    @Default(<SleepData>[]) List<SleepData> previousSessions,
    @Default(<SleepData>[]) List<SleepData> baselineSessions,
    @Default(<DailySleepDuration>[]) List<DailySleepDuration> dailyDurations,
    @Default(<DailySleepDuration>[])
    List<DailySleepDuration> previousDailyDurations,
    @Default(<DailySleepDuration>[])
    List<DailySleepDuration> baselineDailyDurations,
  }) = _SleepPeriodData;
}
