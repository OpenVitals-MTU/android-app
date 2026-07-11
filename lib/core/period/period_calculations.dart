import '../time/local_date.dart';
import 'period_load_query.dart';
import 'period_selection.dart';
import 'time_range.dart';

const int defaultBaselineDays = 90;

extension TimeRangeShift on TimeRange {
  LocalDate shift(
    LocalDate anchorDate,
    int steps, {
    WeekPeriodMode weekPeriodMode = WeekPeriodMode.mondayToSunday,
  }) {
    switch (this) {
      case TimeRange.day:
        return anchorDate.plusDays(steps);
      case TimeRange.week:
        return anchorDate.plusWeeks(steps);
      case TimeRange.month:
        return weekPeriodMode.usesRollingDates
            ? anchorDate.plusDays(TimeRange.month.days * steps)
            : anchorDate.plusMonths(steps);
      case TimeRange.year:
        return weekPeriodMode.usesRollingDates
            ? anchorDate.plusDays(TimeRange.year.days * steps)
            : anchorDate.plusYears(steps);
    }
  }
}

DatePeriod periodFor(
  TimeRange range,
  LocalDate anchorDate, {
  LocalDate? today,
  WeekPeriodMode weekPeriodMode = WeekPeriodMode.mondayToSunday,
  bool clipCurrentWeekToToday = true,
}) {
  final resolvedToday = today ?? LocalDate.now();
  switch (range) {
    case TimeRange.day:
      return DatePeriod(anchorDate, anchorDate);
    case TimeRange.week:
      return _weekPeriodFor(
        anchorDate,
        resolvedToday,
        weekPeriodMode,
        clipCurrentWeekToToday,
      );
    case TimeRange.month:
      if (weekPeriodMode.usesRollingDates) {
        return DatePeriod(
          anchorDate.minusDays(TimeRange.month.days - 1),
          anchorDate,
        );
      }
      final start = anchorDate.withDayOfMonth(1);
      final end = anchorDate
          .withDayOfMonth(anchorDate.lengthOfMonth)
          .coerceAtMost(resolvedToday);
      return DatePeriod(start, end);
    case TimeRange.year:
      if (weekPeriodMode.usesRollingDates) {
        return DatePeriod(
          anchorDate.minusDays(TimeRange.year.days - 1),
          anchorDate,
        );
      }
      final start = anchorDate.withDayOfYear(1);
      final end = anchorDate
          .withDayOfYear(anchorDate.lengthOfYear)
          .coerceAtMost(resolvedToday);
      return DatePeriod(start, end);
  }
}

DatePeriod displayPeriodFor(
  TimeRange range,
  LocalDate anchorDate, {
  LocalDate? today,
  WeekPeriodMode weekPeriodMode = WeekPeriodMode.mondayToSunday,
}) =>
    periodFor(
      range,
      anchorDate,
      today: today,
      weekPeriodMode: weekPeriodMode,
      clipCurrentWeekToToday: false,
    );

DatePeriod previousPeriodFor(
  TimeRange range,
  LocalDate anchorDate, {
  LocalDate? today,
  WeekPeriodMode weekPeriodMode = WeekPeriodMode.mondayToSunday,
}) {
  final resolvedToday = today ?? LocalDate.now();
  return PeriodSelection(range, anchorDate.coerceAtMost(resolvedToday))
      .previousPeriod(weekPeriodMode: weekPeriodMode)
      .period(today: resolvedToday, weekPeriodMode: weekPeriodMode);
}

DatePeriod baselinePeriodBefore(DatePeriod period, {int days = 90}) =>
    DatePeriod(period.start.minusDays(days), period.start.minusDays(1));

PeriodWindows periodWindowsFor(
  TimeRange range,
  LocalDate anchorDate, {
  LocalDate? today,
  int baselineDays = defaultBaselineDays,
  WeekPeriodMode weekPeriodMode = WeekPeriodMode.mondayToSunday,
}) {
  final resolvedToday = today ?? LocalDate.now();
  final current = periodFor(
    range,
    anchorDate.coerceAtMost(resolvedToday),
    today: resolvedToday,
    weekPeriodMode: weekPeriodMode,
  );
  final previous = previousPeriodFor(
    range,
    anchorDate,
    today: resolvedToday,
    weekPeriodMode: weekPeriodMode,
  );
  final baseline = baselinePeriodBefore(current, days: baselineDays);
  return PeriodWindows(
    current: current,
    previous: previous,
    baseline: baseline,
  );
}

DatePeriod _weekPeriodFor(
  LocalDate anchorDate,
  LocalDate today,
  WeekPeriodMode weekPeriodMode,
  bool clipCurrentWeekToToday,
) {
  switch (weekPeriodMode) {
    case WeekPeriodMode.mondayToSunday:
      final start = anchorDate.previousOrSame(DateTime.monday);
      final rawEnd = start.plusDays(6);
      final end = clipCurrentWeekToToday ? rawEnd.coerceAtMost(today) : rawEnd;
      return DatePeriod(start, end);
    case WeekPeriodMode.last7Days:
      return DatePeriod(anchorDate.minusDays(6), anchorDate);
  }
}
