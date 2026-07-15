import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/ui/charts/chart_axis.dart';
import 'package:openvitals/ui/charts/chart_zoom.dart';
import 'package:openvitals/ui/charts/line_chart.dart';

/// The Heart & Vitals cards render through [MetricLineChart]. A DAY range must
/// pinch to zoom like the other day charts — and the hour row has to move with
/// the line, or the labels would name hours the plot is no longer showing.
void main() {
  testWidgets('pinching a day MetricLineChart zooms the line and its hours',
      (tester) async {
    const day = LocalDate(2024, 6, 10);
    final series = MetricLineSeries(
      color: const Color(0xFF2196F3),
      points: [
        for (var h = 0; h <= 23; h++)
          MetricLinePoint(
            date: day,
            value: 60 + (h % 5) * 3.0,
            time: DateTime(2024, 6, 10, h),
          ),
      ],
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(
            child: MetricLineChart(
              title: 'Heart rate',
              series: [series],
              selectedRange: TimeRange.day,
              period: DatePeriod(day, day),
              accentColor: const Color(0xFF2196F3),
              summaryText: 'Avg 63 bpm',
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    // Before: the whole day is on show, so its end labels are present.
    expect(find.byType(ChartZoom), findsOneWidget);
    expect(find.text('00:00'), findsOneWidget);
    expect(find.text('24:00'), findsOneWidget);

    final center = tester.getCenter(find.byType(ChartZoom));
    final left = await tester.startGesture(center - const Offset(30, 0));
    final right = await tester.startGesture(center + const Offset(30, 0));
    await tester.pump();
    await left.moveBy(const Offset(-90, 0));
    await right.moveBy(const Offset(90, 0));
    await tester.pump();
    await left.up();
    await right.up();
    await tester.pumpAndSettle();

    // After: only a slice remains, so the day's ends are no longer on the row.
    expect(find.text('00:00'), findsNothing);
    expect(find.text('24:00'), findsNothing);
  });

  testWidgets('pinching a year MetricLineChart zooms it too', (tester) async {
    final series = MetricLineSeries(
      color: const Color(0xFF2196F3),
      points: [
        for (var month = 1; month <= 12; month++)
          MetricLinePoint(
            date: LocalDate(2024, month, 15),
            value: 60 + (month % 4) * 3.0,
          ),
      ],
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(
            child: MetricLineChart(
              title: 'Resting heart rate',
              series: [series],
              selectedRange: TimeRange.year,
              period: DatePeriod(
                const LocalDate(2024, 1, 1),
                const LocalDate(2024, 12, 31),
              ),
              accentColor: const Color(0xFF2196F3),
              summaryText: 'Avg 63 bpm',
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    // The year chart pinches like the day one. Unzoomed the date axis is an even
    // Row of slots; zoomed it lays its surviving labels out in a Stack so each
    // sits over its own slot.
    expect(find.byType(ChartZoom), findsOneWidget);
    expect(
      find.descendant(
          of: find.byType(PeriodChartXAxis), matching: find.byType(Stack)),
      findsNothing,
    );

    final center = tester.getCenter(find.byType(ChartZoom));
    final left = await tester.startGesture(center - const Offset(30, 0));
    final right = await tester.startGesture(center + const Offset(30, 0));
    await tester.pump();
    await left.moveBy(const Offset(-90, 0));
    await right.moveBy(const Offset(90, 0));
    await tester.pump();
    await left.up();
    await right.up();
    await tester.pumpAndSettle();

    expect(
      find.descendant(
          of: find.byType(PeriodChartXAxis), matching: find.byType(Stack)),
      findsOneWidget,
    );
  });

  testWidgets('switching the year resets a zoom rather than carrying it over',
      (tester) async {
    Widget chartForYear(int year) {
      final series = MetricLineSeries(
        color: const Color(0xFF2196F3),
        points: [
          for (var month = 1; month <= 12; month++)
            MetricLinePoint(date: LocalDate(year, month, 15), value: 60.0 + month),
        ],
      );
      return MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(
            child: MetricLineChart(
              title: 'Resting heart rate',
              series: [series],
              selectedRange: TimeRange.year,
              period:
                  DatePeriod(LocalDate(year, 1, 1), LocalDate(year, 12, 31)),
              accentColor: const Color(0xFF2196F3),
              summaryText: 'Avg',
            ),
          ),
        ),
      );
    }

    await tester.pumpWidget(chartForYear(2024));
    await tester.pumpAndSettle();

    final center = tester.getCenter(find.byType(ChartZoom));
    final left = await tester.startGesture(center - const Offset(30, 0));
    final right = await tester.startGesture(center + const Offset(30, 0));
    await tester.pump();
    await left.moveBy(const Offset(-90, 0));
    await right.moveBy(const Offset(90, 0));
    await tester.pump();
    await left.up();
    await right.up();
    await tester.pumpAndSettle();

    // Zoomed now (Stack layout).
    expect(
      find.descendant(
          of: find.byType(PeriodChartXAxis), matching: find.byType(Stack)),
      findsOneWidget,
    );

    // Switch to another year: the zoom must NOT carry over.
    await tester.pumpWidget(chartForYear(2023));
    await tester.pumpAndSettle();
    expect(
      find.descendant(
          of: find.byType(PeriodChartXAxis), matching: find.byType(Stack)),
      findsNothing,
    );
  });
}
