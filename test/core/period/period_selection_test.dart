import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/period/period_calculations.dart';
import 'package:openvitals/core/period/period_selection.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';

void main() {
  final today = LocalDate(2026, 4, 26);

  test('selectRange keeps future selected date capped at today', () {
    final selection = PeriodSelection(TimeRange.week, today.plusDays(3));

    final updated = selection.selectRange(TimeRange.month, today: today);

    expect(updated.selectedRange, TimeRange.month);
    expect(updated.selectedDate, today);
  });

  test('previousPeriod moves by selected range', () {
    final selection = PeriodSelection(TimeRange.month, LocalDate(2026, 4, 15));

    expect(selection.previousPeriod().selectedDate, LocalDate(2026, 3, 15));
  });

  test('previousPeriod moves month by thirty days for rolling dates', () {
    final selection = PeriodSelection(TimeRange.month, LocalDate(2026, 4, 15));

    expect(
      selection
          .previousPeriod(weekPeriodMode: WeekPeriodMode.last7Days)
          .selectedDate,
      LocalDate(2026, 3, 16),
    );
  });

  test('previousPeriod moves year by three hundred sixty five days for rolling '
      'dates', () {
    final selection = PeriodSelection(TimeRange.year, LocalDate(2026, 4, 15));

    expect(
      selection
          .previousPeriod(weekPeriodMode: WeekPeriodMode.last7Days)
          .selectedDate,
      LocalDate(2026, 4, 15).minusDays(365),
    );
    expect(selection.previousPeriod().selectedDate, LocalDate(2025, 4, 15));
  });

  test('nextPeriod moves month by thirty days for rolling dates', () {
    final selection = PeriodSelection(TimeRange.month, today.minusDays(60));

    final updated = selection.nextPeriod(
      today: today,
      weekPeriodMode: WeekPeriodMode.last7Days,
    );

    expect(updated.selectedDate, today.minusDays(30));
  });

  test('nextPeriod does not move beyond current period', () {
    final selection = PeriodSelection(TimeRange.week, today);

    expect(selection.nextPeriod(today: today), selection);
  });

  test('nextPeriod moves when the next period is not in the future', () {
    final selection = PeriodSelection(TimeRange.week, today.plusWeeks(-2));

    final updated = selection.nextPeriod(today: today);

    expect(updated.selectedDate, today.plusWeeks(-1));
    expect(updated.period(today: today).end.isAfter(today), isFalse);
  });

  test('previousPeriodFor returns previous calendar period', () {
    final period = previousPeriodFor(
      TimeRange.month,
      LocalDate(2026, 4, 15),
      today: today,
    );

    expect(period.start, LocalDate(2026, 3, 1));
    expect(period.end, LocalDate(2026, 3, 31));
  });

  test('previousPeriodFor returns the previous rolling month window', () {
    final period = previousPeriodFor(
      TimeRange.month,
      today,
      today: today,
      weekPeriodMode: WeekPeriodMode.last7Days,
    );

    expect(period.start, today.minusDays(59));
    expect(period.end, today.minusDays(30));
  });

  test('displayPeriodFor keeps full Monday to Sunday week mid week', () {
    final wednesday = LocalDate(2026, 5, 27);
    final period = displayPeriodFor(
      TimeRange.week,
      wednesday,
      today: wednesday,
      weekPeriodMode: WeekPeriodMode.mondayToSunday,
    );

    expect(period.start, LocalDate(2026, 5, 25));
    expect(period.end, LocalDate(2026, 5, 31));
  });

  test('displayPeriodFor supports rolling last seven days', () {
    final period = displayPeriodFor(
      TimeRange.week,
      today,
      today: today,
      weekPeriodMode: WeekPeriodMode.last7Days,
    );

    expect(period.start, today.minusDays(6));
    expect(period.end, today);
  });

  test('displayPeriodFor supports rolling last thirty days', () {
    final period = displayPeriodFor(
      TimeRange.month,
      today,
      today: today,
      weekPeriodMode: WeekPeriodMode.last7Days,
    );

    expect(period.start, today.minusDays(29));
    expect(period.end, today);
  });

  test('displayPeriodFor supports rolling last three hundred sixty five days',
      () {
    final period = displayPeriodFor(
      TimeRange.year,
      today,
      today: today,
      weekPeriodMode: WeekPeriodMode.last7Days,
    );

    expect(period.start, today.minusDays(364));
    expect(period.end, today);
  });

  test('calendar mode keeps calendar month and year windows', () {
    final month = displayPeriodFor(TimeRange.month, today, today: today);
    final year = displayPeriodFor(TimeRange.year, today, today: today);

    expect(month.start, LocalDate(2026, 4, 1));
    expect(year.start, LocalDate(2026, 1, 1));
  });
}
