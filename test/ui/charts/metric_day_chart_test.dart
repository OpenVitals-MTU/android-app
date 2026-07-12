import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/charts/chart_axis.dart';
import 'package:openvitals/ui/charts/day_axis.dart';
import 'package:openvitals/ui/charts/metric_day_chart.dart';
import 'package:openvitals/ui/charts/metric_line_plot.dart';

/// The one day card, and the three shapes a day's readings can take.
///
/// Six features hand-built this card, and every one of them re-derived the shapes
/// inline. They are properties of the data, not the screen — so they are pinned
/// once, here, instead of six times, badly.
void main() {
  const day = LocalDate(2026, 6, 22);
  DateTime at(int hour, [int minute = 0]) =>
      DateTime(2026, 6, 22, hour, minute);
  DaySample sample(int hour, double value) => (time: at(hour), value: value);

  // Noon on the day itself: today, half elapsed.
  final today = DayAxis(day, now: at(12));
  final pastDay = DayAxis(day, now: DateTime(2026, 6, 30));

  group('DaySeriesShape', () {
    test('raw plots the readings and invents nothing', () {
      final points = DaySeriesShape.raw
          .plot([sample(6, 70), sample(18, 80)], pastDay);

      // No midnight anchor, no trailing hold: a weight at 06:00 says nothing about
      // midnight, and nothing about tonight.
      expect(points, hasLength(2));
      expect(points.first.xFraction, closeTo(0.25, 1e-9));
      expect(points.first.value, 70);
      expect(points.last.xFraction, closeTo(0.75, 1e-9));
    });

    test('cumulative anchors at midnight and holds the total to the end', () {
      final points = DaySeriesShape.cumulative
          .plot([sample(6, 400), sample(18, 1000)], pastDay);

      expect(points.first.xFraction, 0);
      expect(points.first.value, 0);
      expect(points[1].xFraction, closeTo(0.25, 1e-9));
      expect(points.last.xFraction, 1.0);
      expect(points.last.value, 1000, reason: 'holds the total, does not drop');
    });

    test('cumulative on today stops at now, not at the right edge', () {
      final points = DaySeriesShape.cumulative.plot([sample(6, 400)], today);

      // Held out to noon — the afternoon has not happened, and a line drawn across
      // it would be a claim about the future.
      expect(points.last.xFraction, closeTo(0.5, 1e-9));
      expect(points.last.value, 400);
    });

    test('step jumps at each entry instead of ramping between them', () {
      final points =
          DaySeriesShape.step.plot([sample(6, 0.3), sample(18, 0.8)], pastDay);

      // Two points per entry at the same instant: the total before, and after. A
      // ramp would draw you sipping steadily all morning and hide the fact that you
      // have had nothing since six.
      expect(points[1].xFraction, closeTo(0.25, 1e-9));
      expect(points[1].value, 0.0, reason: 'the total before the first drink');
      expect(points[2].xFraction, closeTo(0.25, 1e-9));
      expect(points[2].value, 0.3, reason: 'and immediately after it');

      expect(points[3].value, 0.3, reason: 'flat until the next drink');
      expect(points[4].value, 0.8);
    });

    test('every shape survives an empty day', () {
      for (final shape in DaySeriesShape.values) {
        expect(shape.plot(const [], today), isEmpty);
      }
    });
  });

  group('ChartRange.padded', () {
    test('clears the data without dropping below the floor', () {
      final range = ChartRange.padded([70.0, 74.0], floor: 0);
      expect(range.min, greaterThan(0));
      expect(range.min, lessThan(70));
      expect(range.max, greaterThan(74));
    });

    test('a flat series still gets an axis to breathe in', () {
      // A steady 70 kg has no span to take a percentage of. Padding by zero would
      // collapse the axis onto the line.
      final range = ChartRange.padded([70.0, 70.0]);
      expect(range.max - range.min, greaterThan(0));
    });

    test('an empty series does not divide by nothing', () {
      expect(ChartRange.padded(const []).max, 1);
    });
  });

  group('MetricDayChart', () {
    Future<void> pump(WidgetTester tester, Widget card) async {
      await tester.pumpWidget(MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(body: SingleChildScrollView(child: card)),
      ));
      expect(tester.takeException(), isNull);
    }

    MetricDayChart card({
      required DayAxis axis,
      List<DaySample> samples = const [],
      Widget? header,
      Widget? footer,
    }) =>
        MetricDayChart(
          axis: axis,
          samples: samples,
          shape: DaySeriesShape.cumulative,
          range: const ChartRange(0, 1000),
          accentColor: Colors.teal,
          metricName: 'Water',
          emptyLabel: 'Water',
          header: header,
          footer: footer,
        );

    testWidgets('an empty day says so, and draws no plot', (tester) async {
      await pump(tester, card(axis: pastDay));

      expect(find.byType(MetricLinePlot), findsNothing);
      expect(find.byType(DayAxisLabels), findsNothing);
    });

    testWidgets('a day with readings draws the plot and the hour row',
        (tester) async {
      await pump(tester, card(axis: pastDay, samples: [sample(9, 500)]));

      expect(find.byType(MetricLinePlot), findsOneWidget);
      expect(find.text('12:00'), findsOneWidget);
    });

    testWidgets('sorts the samples it is handed', (tester) async {
      // Callers pass whatever the repository gave them. The card must not trust the
      // order — an out-of-order list would draw the line doubling back on itself.
      await pump(
        tester,
        card(axis: pastDay, samples: [sample(18, 900), sample(6, 200)]),
      );

      final plot = tester.widget<MetricLinePlot>(find.byType(MetricLinePlot));
      final xs = [for (final point in plot.points) point.xFraction];
      expect(xs, orderedEquals([...xs]..sort()));
    });

    testWidgets('the header and footer slots replace the defaults',
        (tester) async {
      // This is what lets the heart timeline — statistics above, recording window
      // below — be the same card as hydration rather than a sixth copy of it.
      await pump(
        tester,
        card(
          axis: pastDay,
          samples: [sample(9, 500)],
          header: const Text('avg 62 bpm'),
          footer: const Text('07:12 – 22:40'),
        ),
      );

      expect(find.text('avg 62 bpm'), findsOneWidget);
      expect(find.text('07:12 – 22:40'), findsOneWidget);
      expect(find.byType(DayChartHeader), findsNothing);
    });
  });
}
