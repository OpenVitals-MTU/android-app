@Tags(['golden'])
library;

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/ui/charts/period_chart.dart';
import 'package:openvitals/ui/theme/app_colors.dart';

import '../../support/golden_harness.dart';

/// [MetricBarChart] / [PeriodHistoryChart] — the most-used chart in the app, and
/// the only one that draws three genuinely different pictures from one call.
///
/// WEEK is bars, MONTH is a calendar heatmap, YEAR is a dot heatmap, and the
/// screen (sleep, mindfulness, hydration, steps…) just hands over the same list
/// of dated values and a range. So the range dispatch is what these shoot: three
/// ranges, one fixture, and the year one is the only place `yearAggregation`
/// shows up at all — a year of NIGHTS averages, because summing 365 sleeps would
/// print a number of hours nobody has ever slept.
void main() {
  final formatter = UnitFormatter(
    unitSystemProvider: () => UnitSystem.metric,
  );

  /// Hours slept per night, deterministically shaped: a long weekend lie-in, a
  /// short Wednesday, and the occasional night with no record at all (Health
  /// Connect gaps are the norm, not the exception).
  double sleepHours(LocalDate date) {
    if (date.day % 11 == 0) return 0; // no record that night
    final base = date.dayOfWeek >= 6 ? 8.4 : 7.0;
    return base + ((date.day * 3) % 7) * 0.12;
  }

  List<PeriodChartValue> nights(LocalDate start, LocalDate end) {
    final values = <PeriodChartValue>[];
    var date = start;
    while (!date.isAfter(end)) {
      final hours = sleepHours(date);
      if (hours > 0) values.add(PeriodChartValue(date, hours));
      date = date.plusDays(1);
    }
    return values;
  }

  testWidgets('week — bars', (tester) async {
    const week = DatePeriod(LocalDate(2026, 6, 16), LocalDate(2026, 6, 22));
    await expectChartGoldenBothThemes(
      tester,
      () => MetricBarChart(
        title: 'Sleep',
        values: nights(week.start, week.end),
        selectedRange: TimeRange.week,
        period: week,
        accentColor: AppColors.sleep,
        // Sleep's own alpha, not the 0.85 default — the sleep screen is the
        // caller that discovered this knob and the only one that moves it.
        accentAlpha: 0.75,
        summaryValue: '7h 32m avg',
        valueFormatter: (value) => '${formatter.decimal(value, 1)}h',
      ),
      name: 'period_chart_week',
    );
  });

  testWidgets('week — bars, with the selected day highlighted', (tester) async {
    const week = DatePeriod(LocalDate(2026, 6, 16), LocalDate(2026, 6, 22));
    await expectChartGoldenBothThemes(
      tester,
      () => MetricBarChart(
        title: 'Sleep',
        values: nights(week.start, week.end),
        selectedRange: TimeRange.week,
        period: week,
        accentColor: AppColors.sleep,
        accentAlpha: 0.75,
        summaryValue: '7h 32m avg',
        selectedDate: const LocalDate(2026, 6, 19),
        onDateSelected: (_) {},
        valueFormatter: (value) => '${formatter.decimal(value, 1)}h',
      ),
      name: 'period_chart_week_selected',
    );
  });

  testWidgets('month — the calendar heatmap', (tester) async {
    // The rolling 30-day window the period navigator actually hands over, hence
    // `last7Days`: the summary line has to say "Last 30 days", not "This month",
    // or it disagrees with the header above it.
    const month = DatePeriod(LocalDate(2026, 5, 24), LocalDate(2026, 6, 22));
    await expectChartGoldenBothThemes(
      tester,
      () => MetricBarChart(
        title: 'Sleep',
        values: nights(month.start, month.end),
        selectedRange: TimeRange.month,
        period: month,
        accentColor: AppColors.sleep,
        accentAlpha: 0.75,
        summaryValue: '7h 28m avg',
        weekPeriodMode: WeekPeriodMode.last7Days,
        valueFormatter: (value) => '${formatter.decimal(value, 1)}h',
      ),
      name: 'period_chart_month',
    );
  });

  testWidgets('year — the dot heatmap, twelve averaged months', (tester) async {
    const year = DatePeriod(LocalDate(2025, 7, 1), LocalDate(2026, 6, 22));
    await expectChartGoldenBothThemes(
      tester,
      () => MetricBarChart(
        title: 'Sleep',
        values: nights(year.start, year.end),
        selectedRange: TimeRange.year,
        period: year,
        accentColor: AppColors.sleep,
        accentAlpha: 0.75,
        summaryValue: '7h 30m avg',
        weekPeriodMode: WeekPeriodMode.last7Days,
        // Threaded through exactly as the sleep screen threads it (nights
        // average, they do not sum). The YEAR range dispatches to the heatmap,
        // which colours raw days, so this only bites on the bar path — it is
        // here because the caller sets it, not because the picture needs it.
        yearAggregation: PeriodBarAggregation.averageNonZero,
        valueFormatter: (value) => '${formatter.decimal(value, 1)}h',
      ),
      name: 'period_chart_year',
    );
  });
}
