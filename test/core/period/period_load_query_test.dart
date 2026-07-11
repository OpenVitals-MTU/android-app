import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/period/period_selection_driver.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';

void main() {
  final today = LocalDate(2026, 5, 25);

  test('query clamps future anchor date before creating windows', () {
    final query = PeriodLoadQuery(
      range: TimeRange.week,
      anchorDate: today.plusDays(4),
      today: today,
    );

    expect(query.selectedDate, today);
    expect(query.windows.current.start, LocalDate(2026, 5, 25));
    expect(query.windows.current.end, today);
  });

  test('query creates current previous and baseline windows', () {
    final query = PeriodLoadQuery(
      range: TimeRange.month,
      anchorDate: LocalDate(2026, 4, 14),
      today: today,
      baselineDays: 30,
    );

    expect(query.windows.current,
        DatePeriod(LocalDate(2026, 4, 1), LocalDate(2026, 4, 30)));
    expect(query.windows.previous,
        DatePeriod(LocalDate(2026, 3, 1), LocalDate(2026, 3, 31)));
    expect(query.windows.baseline,
        DatePeriod(LocalDate(2026, 3, 2), LocalDate(2026, 3, 31)));
  });

  test('query uses rolling last seven days when requested', () {
    final query = PeriodLoadQuery(
      range: TimeRange.week,
      anchorDate: today,
      today: today,
      baselineDays: 30,
      weekPeriodMode: WeekPeriodMode.last7Days,
    );

    expect(query.windows.current, DatePeriod(today.minusDays(6), today));
    expect(query.windows.previous,
        DatePeriod(today.minusDays(13), today.minusDays(7)));
    expect(query.windows.baseline,
        DatePeriod(today.minusDays(36), today.minusDays(7)));
  });

  test('query uses rolling last thirty days for month when rolling dates are '
      'selected', () {
    final query = PeriodLoadQuery(
      range: TimeRange.month,
      anchorDate: today,
      today: today,
      baselineDays: 30,
      weekPeriodMode: WeekPeriodMode.last7Days,
    );

    expect(query.windows.current, DatePeriod(today.minusDays(29), today));
    expect(query.windows.previous,
        DatePeriod(today.minusDays(59), today.minusDays(30)));
    expect(query.windows.baseline,
        DatePeriod(today.minusDays(59), today.minusDays(30)));
  });

  test('query uses rolling last three hundred sixty five days for year when '
      'rolling dates are selected', () {
    final query = PeriodLoadQuery(
      range: TimeRange.year,
      anchorDate: today,
      today: today,
      baselineDays: 30,
      weekPeriodMode: WeekPeriodMode.last7Days,
    );

    expect(query.windows.current, DatePeriod(today.minusDays(364), today));
    expect(query.windows.previous,
        DatePeriod(today.minusDays(729), today.minusDays(365)));
    expect(query.windows.baseline,
        DatePeriod(today.minusDays(394), today.minusDays(365)));
  });

  test('query clips current Monday to Sunday load window to today', () {
    final wednesday = LocalDate(2026, 5, 27);
    final query = PeriodLoadQuery(
      range: TimeRange.week,
      anchorDate: wednesday,
      today: wednesday,
      weekPeriodMode: WeekPeriodMode.mondayToSunday,
    );

    expect(query.windows.current,
        DatePeriod(LocalDate(2026, 5, 25), wednesday));
  });

  test('selection driver persists range and clamps next period', () {
    final driver = PeriodSelectionDriver(
      initialRange: TimeRange.month,
      initialDate: LocalDate(2026, 4, 15),
    );

    expect(driver.selectRange(TimeRange.week).selectedRange, TimeRange.week);
    expect(driver.previousPeriod().selectedDate, LocalDate(2026, 4, 8));

    final currentDriver = PeriodSelectionDriver(
      initialRange: TimeRange.week,
      initialDate: LocalDate.now(),
    );

    expect(currentDriver.nextPeriod(), isNull);
  });

  test('selection driver advances unpinned stale day to today on resume', () {
    final startDate = LocalDate.now();
    final driver = PeriodSelectionDriver(
      initialRange: TimeRange.day,
      initialDate: startDate,
    );
    final tomorrow = startDate.plusDays(1);

    final updated = driver.resumeCurrentPeriod(today: tomorrow);

    expect(updated?.selectedDate, tomorrow);
    expect(driver.selection.selectedDate, tomorrow);
  });

  test('selection driver keeps user pinned past day on resume', () {
    final startDate = LocalDate.now();
    final driver = PeriodSelectionDriver(
      initialRange: TimeRange.day,
      initialDate: startDate,
    );
    final yesterday = startDate.minusDays(1);

    driver.previousPeriod();
    final updated = driver.resumeCurrentPeriod(today: startDate.plusDays(1));

    expect(updated, isNull);
    expect(driver.selection.selectedDate, yesterday);
  });
}
