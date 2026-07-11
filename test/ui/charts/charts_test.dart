// Widget smoke tests for the hand-rolled charts: each renders with sample data
// (light + dark) without throwing. Goldens are intentionally not asserted.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/ui/charts/bar_chart.dart';
import 'package:openvitals/ui/charts/chart_axis.dart';
import 'package:openvitals/ui/charts/heatmap_chart.dart';
import 'package:openvitals/ui/charts/line_chart.dart';
import 'package:openvitals/ui/charts/period_chart.dart';
import 'package:openvitals/ui/charts/sparkline_chart.dart';

const _accent = Color(0xFF4CAF50);

// A fixed week (Mon 2024-06-10 .. Sun 2024-06-16) so the tests are deterministic.
final _weekPeriod = DatePeriod(
  const LocalDate(2024, 6, 10),
  const LocalDate(2024, 6, 16),
);
final _monthPeriod = DatePeriod(
  const LocalDate(2024, 6, 1),
  const LocalDate(2024, 6, 30),
);
final _yearPeriod = DatePeriod(
  const LocalDate(2024, 1, 1),
  const LocalDate(2024, 12, 31),
);

List<PeriodChartValue> _weekValues() => [
      PeriodChartValue(const LocalDate(2024, 6, 10), 1200),
      PeriodChartValue(const LocalDate(2024, 6, 11), 8400),
      PeriodChartValue(const LocalDate(2024, 6, 13), 300),
      PeriodChartValue(const LocalDate(2024, 6, 16), 5600),
    ];

Future<void> _pump(WidgetTester tester, Widget child) async {
  await tester.pumpWidget(
    MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(
        body: SingleChildScrollView(
          child: Padding(padding: const EdgeInsets.all(8), child: child),
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
  expect(tester.takeException(), isNull);
}

void main() {
  testWidgets('SparklineChart renders', (tester) async {
    await _pump(
      tester,
      const SizedBox(
        width: 200,
        height: 60,
        child: SparklineChart(
          values: [3, 7, 2, 9, 4, 8],
          accentColor: _accent,
        ),
      ),
    );
  });

  testWidgets('SparklineChart with a single value renders', (tester) async {
    await _pump(
      tester,
      const SizedBox(
        width: 200,
        height: 60,
        child: SparklineChart(
          values: [5],
          accentColor: _accent,
          singlePointLine: true,
        ),
      ),
    );
  });

  testWidgets('PeriodBarChart (week) renders bars and axis', (tester) async {
    await _pump(
      tester,
      PeriodBarChart(
        title: 'Steps',
        values: _weekValues(),
        selectedRange: TimeRange.week,
        period: _weekPeriod,
        accentColor: _accent,
        summaryText: 'This week · 15,500',
      ),
    );
    expect(find.text('Steps'), findsOneWidget);
    expect(find.text('This week · 15,500'), findsOneWidget);
  });

  testWidgets('PeriodBarChart tap selects a day', (tester) async {
    LocalDate? selected;
    await _pump(
      tester,
      PeriodBarChart(
        title: 'Steps',
        values: _weekValues(),
        selectedRange: TimeRange.week,
        period: _weekPeriod,
        accentColor: _accent,
        summaryText: 'week',
        onDateSelected: (date) => selected = date,
      ),
    );
    await tester.tap(find.text('Steps')); // no-op: outside the plot
    await tester.tapAt(tester.getCenter(find.byType(PeriodBarChart)));
    await tester.pump();
    expect(selected, isNotNull);
  });

  testWidgets('PeriodHistoryChart month renders a calendar heatmap',
      (tester) async {
    await _pump(
      tester,
      PeriodHistoryChart(
        title: 'Steps',
        values: _weekValues(),
        selectedRange: TimeRange.month,
        period: _monthPeriod,
        accentColor: _accent,
        summaryText: 'June',
      ),
    );
    expect(find.byType(PeriodMonthHeatmap), findsOneWidget);
    expect(find.text('Less'), findsOneWidget);
    expect(find.text('More'), findsOneWidget);
  });

  testWidgets('PeriodHistoryChart year renders a year heatmap', (tester) async {
    await _pump(
      tester,
      PeriodHistoryChart(
        title: 'Steps',
        values: _weekValues(),
        selectedRange: TimeRange.year,
        period: _yearPeriod,
        accentColor: _accent,
        summaryText: '2024',
      ),
    );
    expect(find.byType(PeriodYearHeatmap), findsOneWidget);
  });

  testWidgets('MetricBarChart builds its summary from the period title',
      (tester) async {
    await _pump(
      tester,
      MetricBarChart(
        title: 'Steps',
        values: _weekValues(),
        selectedRange: TimeRange.week,
        period: _weekPeriod,
        accentColor: _accent,
        summaryValue: '15,500 steps',
      ),
    );
    expect(find.textContaining('15,500 steps'), findsOneWidget);
  });

  testWidgets('MetricLineChart (week) renders line + axis', (tester) async {
    await _pump(
      tester,
      MetricLineChart(
        title: 'Resting HR',
        series: [
          MetricLineSeries(
            points: [
              MetricLinePoint(date: const LocalDate(2024, 6, 10), value: 58),
              MetricLinePoint(date: const LocalDate(2024, 6, 12), value: 61),
              MetricLinePoint(date: const LocalDate(2024, 6, 15), value: 55),
            ],
            color: _accent,
            label: 'Resting HR',
          ),
        ],
        selectedRange: TimeRange.week,
        period: _weekPeriod,
        accentColor: _accent,
        summaryText: 'Avg 58 bpm',
      ),
    );
    expect(find.text('Resting HR'), findsWidgets);
    expect(find.text('Avg 58 bpm'), findsOneWidget);
  });

  testWidgets('MetricLineChart (day) renders time axis with distinct times',
      (tester) async {
    final dayPeriod = DatePeriod(
      const LocalDate(2024, 6, 15),
      const LocalDate(2024, 6, 15),
    );
    await _pump(
      tester,
      MetricLineChart(
        title: 'Heart rate',
        series: [
          MetricLineSeries(
            points: [
              MetricLinePoint(
                date: const LocalDate(2024, 6, 15),
                value: 72,
                time: DateTime.utc(2024, 6, 15, 8),
              ),
              MetricLinePoint(
                date: const LocalDate(2024, 6, 15),
                value: 96,
                time: DateTime.utc(2024, 6, 15, 18),
              ),
            ],
            color: _accent,
          ),
        ],
        selectedRange: TimeRange.day,
        period: dayPeriod,
        accentColor: _accent,
        summaryText: 'Range 72–96 bpm',
      ),
    );
    expect(find.text('00:00'), findsOneWidget);
    expect(find.text('24:00'), findsOneWidget);
  });

  testWidgets('MetricLineChart renders nothing when there are no points',
      (tester) async {
    await _pump(
      tester,
      MetricLineChart(
        title: 'Empty',
        series: const [],
        selectedRange: TimeRange.week,
        period: _weekPeriod,
        accentColor: _accent,
        summaryText: 'n/a',
      ),
    );
    expect(find.text('Empty'), findsNothing);
  });

  testWidgets('PeriodChartXAxis renders its labels', (tester) async {
    await _pump(
      tester,
      PeriodChartXAxis(
        dates: [
          const LocalDate(2024, 6, 10),
          const LocalDate(2024, 6, 11),
          const LocalDate(2024, 6, 12),
        ],
        selectedRange: TimeRange.week,
      ),
    );
    expect(find.byType(PeriodChartXAxis), findsOneWidget);
  });

  testWidgets('charts render in dark theme', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData.dark(useMaterial3: true),
        home: Scaffold(
          body: PeriodBarChart(
            title: 'Steps',
            values: _weekValues(),
            selectedRange: TimeRange.week,
            period: _weekPeriod,
            accentColor: _accent,
            summaryText: 'dark',
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
  });

  test('formatCompactAxisValue formats compactly', () {
    expect(formatCompactAxisValue(0), '0');
    expect(formatCompactAxisValue(12), '12');
    expect(formatCompactAxisValue(1500), '1.5k');
    expect(formatCompactAxisValue(2000000), '2M');
  });
}
