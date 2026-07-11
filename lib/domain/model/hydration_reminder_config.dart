import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/time/local_date.dart';

part 'hydration_reminder_config.freezed.dart';

@freezed
abstract class HydrationReminderConfig with _$HydrationReminderConfig {
  const HydrationReminderConfig._();

  const factory HydrationReminderConfig({
    @Default(false) bool enabled,
    @Default(HydrationReminderConfig.defaultIntervalMinutes) int intervalMinutes,
    @Default(LocalTime(7, 0)) LocalTime activeStartTime,
    @Default(LocalTime(23, 0)) LocalTime activeEndTime,
  }) = _HydrationReminderConfig;

  HydrationReminderConfig normalized() =>
      copyWith(intervalMinutes: normalizeIntervalMinutes(intervalMinutes));

  static const int defaultIntervalMinutes = 120;
  static const int minIntervalMinutes = 30;
  static const int maxIntervalMinutes = 240;
  static const int intervalStepMinutes = 30;
  static const LocalTime defaultActiveStartTime = LocalTime(7, 0);
  static const LocalTime defaultActiveEndTime = LocalTime(23, 0);

  static int normalizeIntervalMinutes(int value) {
    final rounded = (value ~/ intervalStepMinutes) * intervalStepMinutes;
    return rounded.clamp(minIntervalMinutes, maxIntervalMinutes).toInt();
  }
}
