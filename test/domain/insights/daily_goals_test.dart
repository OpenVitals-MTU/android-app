import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/daily_goals.dart';

void main() {
  test('at least goals count tracked days and streaks', () {
    final progress = dailyGoalProgress(
      [
        DailyGoalValue(date: LocalDate(2026, 1, 1), value: 8.0),
        DailyGoalValue(date: LocalDate(2026, 1, 2), value: 6.0),
        DailyGoalValue(date: LocalDate(2026, 1, 4), value: 10.0),
        DailyGoalValue(date: LocalDate(2026, 1, 5), value: 12.0),
      ],
      DatePeriod(LocalDate(2026, 1, 1), LocalDate(2026, 1, 5)),
      10.0,
      DailyGoalDirection.atLeast,
    );

    expect(progress.trackedDays, 4);
    expect(progress.goalMetDays, 2);
    expect(progress.successRatePercent, 50);
    expect(progress.currentStreakDays, 2);
    expect(progress.longestStreakDays, 2);
    expect(progress.averageGapToGoal, closeTo(1.5, 0.01));
  });

  test('at most goals ignore missing days and count only logged values', () {
    final progress = dailyGoalProgress(
      [
        DailyGoalValue(date: LocalDate(2026, 1, 1), value: 1500.0),
        DailyGoalValue(date: LocalDate(2026, 1, 2), value: 2500.0),
        DailyGoalValue(date: LocalDate(2026, 1, 4), value: 1800.0),
      ],
      DatePeriod(LocalDate(2026, 1, 1), LocalDate(2026, 1, 4)),
      2000.0,
      DailyGoalDirection.atMost,
    );

    final missingDay =
        progress.days.singleWhere((day) => day.date == LocalDate(2026, 1, 3));
    expect(missingDay.isTracked, isFalse);
    expect(missingDay.isMet, isFalse);
    expect(progress.trackedDays, 3);
    expect(progress.goalMetDays, 2);
    expect(progress.successRatePercent, 67);
    expect(progress.currentStreakDays, 1);
    expect(progress.longestStreakDays, 1);
    expect(progress.averageGapToGoal, closeTo(166.67, 0.01));
  });

  test('values on the same day are summed before goal evaluation', () {
    final progress = dailyGoalProgress(
      [
        DailyGoalValue(date: LocalDate(2026, 1, 1), value: 3.0),
        DailyGoalValue(date: LocalDate(2026, 1, 1), value: 4.0),
      ],
      DatePeriod(LocalDate(2026, 1, 1), LocalDate(2026, 1, 1)),
      6.0,
      DailyGoalDirection.atLeast,
    );

    expect(progress.days.single.value, closeTo(7.0, 0.01));
    expect(progress.days.single.isMet, isTrue);
  });
}
