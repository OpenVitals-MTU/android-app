import '../time/local_date.dart';
import 'period_calculations.dart';
import 'time_range.dart';

class PeriodSelection {
  const PeriodSelection(this.selectedRange, this.selectedDate);

  final TimeRange selectedRange;
  final LocalDate selectedDate;

  PeriodSelection selectRange(TimeRange range, {LocalDate? today}) {
    final resolvedToday = today ?? LocalDate.now();
    return PeriodSelection(range, selectedDate.coerceAtMost(resolvedToday));
  }

  PeriodSelection previousPeriod({
    WeekPeriodMode weekPeriodMode = WeekPeriodMode.mondayToSunday,
  }) =>
      PeriodSelection(
        selectedRange,
        selectedRange.shift(
          selectedDate,
          -1,
          weekPeriodMode: weekPeriodMode,
        ),
      );

  PeriodSelection nextPeriod({
    LocalDate? today,
    WeekPeriodMode weekPeriodMode = WeekPeriodMode.mondayToSunday,
  }) {
    final resolvedToday = today ?? LocalDate.now();
    final nextDate = selectedRange.shift(
      selectedDate,
      1,
      weekPeriodMode: weekPeriodMode,
    );
    final next = periodFor(
      selectedRange,
      nextDate,
      today: resolvedToday,
      weekPeriodMode: weekPeriodMode,
    );
    if (next.start.isAfter(resolvedToday) || next.end.isAfter(resolvedToday)) {
      return this;
    }
    return PeriodSelection(selectedRange, nextDate);
  }

  PeriodSelection selectDate(LocalDate date, {LocalDate? today}) {
    final resolvedToday = today ?? LocalDate.now();
    return PeriodSelection(selectedRange, date.coerceAtMost(resolvedToday));
  }

  DatePeriod period({
    LocalDate? today,
    WeekPeriodMode weekPeriodMode = WeekPeriodMode.mondayToSunday,
  }) =>
      periodFor(
        selectedRange,
        selectedDate,
        today: today,
        weekPeriodMode: weekPeriodMode,
      );

  @override
  bool operator ==(Object other) =>
      other is PeriodSelection &&
      other.selectedRange == selectedRange &&
      other.selectedDate == selectedDate;

  @override
  int get hashCode => Object.hash(selectedRange, selectedDate);
}
