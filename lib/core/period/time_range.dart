import '../time/local_date.dart';

/// The Day/Week/Month/Year selector shared by every period-based screen.
///
/// The user-facing label is NOT here — core carries no UI copy. The presentation
/// layer maps each value through AppLocalizations (see `timeRangeLabel`).
enum TimeRange {
  day(1),
  week(7),
  month(30),
  year(365);

  const TimeRange(this.days);

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
