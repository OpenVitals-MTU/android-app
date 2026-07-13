@Tags(['golden'])
library;

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/ui/charts/bar_chart.dart';
import 'package:openvitals/ui/theme/app_colors.dart';

import '../../support/golden_harness.dart';

/// [PeriodBarChart] — the bars, and the value labels ON the bars.
///
/// The labels are the interesting part. `layoutBarLabel` measures the number
/// against the whole SLOT and steps the font down a point at a time rather than
/// dropping it, because the old rule ("does it fit the bar? no? bin it") silently
/// erased the label of the biggest day on the chart — the one day you opened the
/// screen to read. That logic has no picture anywhere, so it gets three: a week
/// where the labels fit, a month where the slots are too narrow to try, and a
/// week at 1.5× text where every label has to step down or die.
void main() {
  final formatter = UnitFormatter(
    unitSystemProvider: () => UnitSystem.metric,
  );

  // The week ending on the golden day. A meditation week: minutes, because the
  // "20 min" label is exactly the two-token string `splitBarLabel` exists to
  // break over two lines.
  const weekStart = LocalDate(2026, 6, 16);
  const weekEnd = LocalDate(2026, 6, 22);
  const week = DatePeriod(weekStart, weekEnd);

  final weekMinutes = <PeriodChartValue>[
    const PeriodChartValue(LocalDate(2026, 6, 16), 20),
    const PeriodChartValue(LocalDate(2026, 6, 17), 35),
    // Wednesday was missed. A zero-value bucket is drawn as nothing at all, not
    // as a stub — the gap in the week is the point.
    const PeriodChartValue(LocalDate(2026, 6, 19), 15),
    const PeriodChartValue(LocalDate(2026, 6, 20), 45),
    const PeriodChartValue(LocalDate(2026, 6, 21), 10),
    const PeriodChartValue(LocalDate(2026, 6, 22), 25),
  ];

  testWidgets('a week — seven slots, and a number on every bar', (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => PeriodBarChart(
        title: 'Mindfulness',
        values: weekMinutes,
        selectedRange: TimeRange.week,
        period: week,
        accentColor: AppColors.mindfulness,
        summaryText: 'This week · 2h 30m',
        valueFormatter: (value) => formatter.minutes(value.round()).text,
      ),
      name: 'period_bar_chart_week',
    );
  });

  testWidgets('a week with a day selected', (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => PeriodBarChart(
        title: 'Mindfulness',
        values: weekMinutes,
        selectedRange: TimeRange.week,
        period: week,
        accentColor: AppColors.mindfulness,
        summaryText: 'This week · 2h 30m',
        selectedDate: const LocalDate(2026, 6, 20),
        // The selection highlight is only drawn when the chart can be tapped —
        // a `selectedDate` with no `onDateSelected` paints nothing.
        onDateSelected: (_) {},
        valueFormatter: (value) => formatter.minutes(value.round()).text,
      ),
      name: 'period_bar_chart_week_selected',
    );
  });

  testWidgets('a month — 31 slots, too narrow for a label to survive',
      (tester) async {
    const month = DatePeriod(LocalDate(2026, 6, 1), LocalDate(2026, 6, 30));
    await expectChartGoldenBothThemes(
      tester,
      () => PeriodBarChart(
        title: 'Mindfulness',
        values: [
          for (var day = 1; day <= 30; day++)
            // A deterministic sawtooth rather than a random walk: a golden that
            // reshuffles its own data every run is not a regression test.
            PeriodChartValue(
              LocalDate(2026, 6, day),
              day % 4 == 0 ? 0 : (10 + (day * 7) % 40).toDouble(),
            ),
        ],
        selectedRange: TimeRange.month,
        period: month,
        accentColor: AppColors.mindfulness,
        summaryText: 'June · 11h 05m',
        valueFormatter: (value) => formatter.minutes(value.round()).text,
      ),
      name: 'period_bar_chart_month',
    );
  });

  testWidgets('a week at 1.5× text — the labels step down instead of vanishing',
      (tester) async {
    // The single subtlest thing in the chart library. At this scale the base
    // 11pt label no longer fits a seventh of 360px, so every bar's number has to
    // walk down toward the 8pt floor. If a future change re-introduces "drop it
    // if it does not fit", this golden goes blank and says so.
    await expectChartGoldenBothThemes(
      tester,
      () => PeriodBarChart(
        title: 'Mindfulness',
        values: weekMinutes,
        selectedRange: TimeRange.week,
        period: week,
        accentColor: AppColors.mindfulness,
        summaryText: 'This week · 2h 30m',
        valueFormatter: (value) => formatter.minutes(value.round()).text,
      ),
      name: 'period_bar_chart_week_text150',
      textScale: 1.5,
    );
  });
}
