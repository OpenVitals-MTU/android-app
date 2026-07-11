import '../../core/period/time_range.dart';

enum ActivityWeekMode {
  mondayToSunday,
  last7Days,
}

extension ActivityWeekModeMapping on ActivityWeekMode {
  WeekPeriodMode toWeekPeriodMode() {
    switch (this) {
      case ActivityWeekMode.mondayToSunday:
        return WeekPeriodMode.mondayToSunday;
      case ActivityWeekMode.last7Days:
        return WeekPeriodMode.last7Days;
    }
  }
}
