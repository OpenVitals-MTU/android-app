import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/period/period_titles.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/l10n/app_localizations_en.dart';

void main() {
  final today = LocalDate(2026, 6, 10);
  final l10n = AppLocalizationsEn();

  test('day titles use relative labels for today and yesterday', () {
    expect(
      periodTitle(l10n, TimeRange.day, DatePeriod(today, today), today: today),
      'Today',
    );
    expect(
      periodTitle(
        l10n,
        TimeRange.day,
        DatePeriod(today.minusDays(1), today.minusDays(1)),
        today: today,
      ),
      'Yesterday',
    );
  });

  test('period titles use current labels when period contains today', () {
    expect(
      periodTitle(
        l10n,
        TimeRange.week,
        DatePeriod(LocalDate(2026, 6, 8), today),
        today: today,
      ),
      'This week',
    );
    expect(
      periodTitle(
        l10n,
        TimeRange.month,
        DatePeriod(LocalDate(2026, 6, 1), today),
        today: today,
      ),
      'This month',
    );
    expect(
      periodTitle(
        l10n,
        TimeRange.year,
        DatePeriod(LocalDate(2026, 1, 1), today),
        today: today,
      ),
      'This year',
    );
  });

  test('past period titles use dated labels', () {
    expect(
      periodTitle(
        l10n,
        TimeRange.week,
        DatePeriod(LocalDate(2026, 6, 1), LocalDate(2026, 6, 7)),
        today: today,
      ),
      'Week of Mon 1 Jun',
    );
    expect(
      periodTitle(
        l10n,
        TimeRange.month,
        DatePeriod(LocalDate(2026, 5, 1), LocalDate(2026, 5, 31)),
        today: today,
      ),
      'May 2026',
    );
    expect(
      periodTitle(
        l10n,
        TimeRange.year,
        DatePeriod(LocalDate(2025, 1, 1), LocalDate(2025, 12, 31)),
        today: today,
      ),
      '2025',
    );
  });

  test('rolling period titles use fixed day window labels', () {
    expect(
      periodTitle(
        l10n,
        TimeRange.week,
        DatePeriod(today.minusDays(6), today),
        today: today,
        weekPeriodMode: WeekPeriodMode.last7Days,
      ),
      'Last 7 days',
    );
    expect(
      periodTitle(
        l10n,
        TimeRange.month,
        DatePeriod(today.minusDays(29), today),
        today: today,
        weekPeriodMode: WeekPeriodMode.last7Days,
      ),
      'Last 30 days',
    );
    expect(
      periodTitle(
        l10n,
        TimeRange.year,
        DatePeriod(today.minusDays(364), today),
        today: today,
        weekPeriodMode: WeekPeriodMode.last7Days,
      ),
      'Last 365 days',
    );
  });

  test('past rolling periods read as the dated span they cover', () {
    // A rolling month that no longer ends today is a 30-day span, not the single
    // calendar month its start falls in ("April 2026" for a mostly-May window).
    expect(
      periodTitle(
        l10n,
        TimeRange.month,
        DatePeriod(today.minusDays(59), today.minusDays(30)),
        today: today,
        weekPeriodMode: WeekPeriodMode.last7Days,
      ),
      '12 Apr – 11 May 2026',
    );
    expect(
      periodTitle(
        l10n,
        TimeRange.week,
        DatePeriod(today.minusDays(13), today.minusDays(7)),
        today: today,
        weekPeriodMode: WeekPeriodMode.last7Days,
      ),
      '28 May – 3 Jun 2026',
    );
  });

  test('a past rolling span that straddles a year shows both years', () {
    expect(
      periodTitle(
        l10n,
        TimeRange.year,
        DatePeriod(LocalDate(2024, 12, 20), LocalDate(2025, 12, 19)),
        today: today,
        weekPeriodMode: WeekPeriodMode.last7Days,
      ),
      '20 Dec 2024 – 19 Dec 2025',
    );
  });
}
