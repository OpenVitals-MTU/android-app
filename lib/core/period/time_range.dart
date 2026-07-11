import '../time/local_date.dart';

/// The Day/Week/Month/Year selector shared by every period-based screen.
enum TimeRange {
  day('Day', 1),
  week('Week', 7),
  month('Month', 30),
  year('Year', 365);

  const TimeRange(this.label, this.days);

  final String label;
  final int days;
}

enum WeekPeriodMode {
  mondayToSunday,
  last7Days;

  /// Whether periods are rolling day windows anchored on the selected date
  /// (7/30/365 days) instead of calendar week/month/year. Port of the Kotlin
  /// `WeekPeriodMode.usesRollingDates()`.
  bool get usesRollingDates => this == WeekPeriodMode.last7Days;
}

/// An inclusive date range.
class DatePeriod {
  const DatePeriod(this.start, this.end);

  final LocalDate start;
  final LocalDate end;

  @override
  bool operator ==(Object other) =>
      other is DatePeriod && other.start == start && other.end == end;

  @override
  int get hashCode => Object.hash(start, end);

  @override
  String toString() => 'DatePeriod($start..$end)';
}
