import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/charts/chart_axis.dart';
import 'package:openvitals/ui/charts/chart_viewport.dart';
import 'package:openvitals/ui/charts/chart_zoom.dart';
import 'package:openvitals/ui/charts/day_axis.dart';
import 'package:openvitals/ui/charts/metric_day_chart.dart';

/// Zooming a real day chart, hour row and all.
///
/// The hour row is the reason this test exists. [DayAxisLabels] was written to kill a bug
/// where the plot said one hour and the labels underneath said another — six screens drew
/// `00:00 / 06:00 / 12:00 / 18:00` under a plot that was not showing the whole day. A zoom
/// that moved the line but left the labels fixed would be that exact bug, reintroduced by
/// the very feature meant to let you look closer.

void main() {
  group('dayAxisLabelsFor', () {
    test('the whole day is the five labels it always was', () {
      expect(
        dayAxisLabelsFor(ChartViewport.full),
        ['00:00', '06:00', '12:00', '18:00', '24:00'],
      );
    });

    test('zoomed in, the row says the hours actually on the plot', () {
      // Noon to six: a quarter of the day, starting halfway through it.
      const view = ChartViewport(start: 0.5, end: 0.75);

      expect(
        dayAxisLabelsFor(view),
        ['12:00', '13:30', '15:00', '16:30', '18:00'],
      );
    });
  });

  testWidgets('pinching a day chart zooms the plot AND its hours', (tester) async {
    final date = LocalDate(2026, 7, 14);
    final axis = DayAxis(date, now: DateTime(2026, 7, 14, 23, 59));

    await tester.pumpWidget(
      MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: MetricDayChart(
            axis: axis,
            samples: [
              for (var hour = 0; hour < 24; hour++)
                (
                  time: DateTime(2026, 7, 14, hour),
                  value: hour.toDouble() * 100,
                ),
            ],
            shape: DaySeriesShape.cumulative,
            range: const ChartRange(0, 2400),
            accentColor: const Color(0xFF00AAFF),
            metricName: 'Steps',
            emptyLabel: 'steps',
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    // Before: the whole day.
    expect(find.text('12:00'), findsOneWidget);
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

    // After: a slice of it. The row must have moved with the plot — if 24:00 were still
    // sitting at the right edge of a chart showing four hours of the afternoon, the chart
    // would be lying about when everything on it happened.
    expect(find.text('24:00'), findsNothing);
    expect(find.text('00:00'), findsNothing);
  });
}
