import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/time/local_date.dart';

part 'mindfulness_reminder_config.freezed.dart';

@freezed
abstract class MindfulnessReminderConfig with _$MindfulnessReminderConfig {
  const MindfulnessReminderConfig._();

  const factory MindfulnessReminderConfig({
    @Default(false) bool enabled,
    @Default(LocalTime(18, 0)) LocalTime reminderTime,
  }) = _MindfulnessReminderConfig;

  MindfulnessReminderConfig normalized() => this;

  static const LocalTime defaultReminderTime = LocalTime(18, 0);
}
