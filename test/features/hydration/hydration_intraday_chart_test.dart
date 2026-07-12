import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/hydration/hydration_intraday_chart.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/charts/metric_line_plot.dart';

/// The Day view was drawing the WEEK chart with a single day in it: one fat bar
/// labelled "Sun 12". It restated a number the two cards above it already show, and
/// threw away the only thing a day chart is for — WHEN you drank it.
///
/// A day is a line that climbs from nothing at midnight to whatever you have drunk
/// by now. Its shape is the point: it tells you that you drank everything before
/// 9am and nothing since.
void main() {
  // A fixed date in the PAST, on purpose. For today the chart correctly ends at
  // NOW rather than at midnight — a line drawn across the whole 24 hours at 2pm
  // would show a flat run into a future that has not happened — so a "today"
  // fixture would make every x-position depend on the wall clock, and the test
  // would drift through the day. (It did: a 06:00 drink landed at 0.47 of an
  // elapsed 12.7-hour day, which is right, and told me the fixture was wrong.)
  final day = LocalDate(2020, 1, 15);

  HydrationEntry entry(int hour, double liters) {
    final start = DateTime(2020, 1, 15, hour);
    return HydrationEntry(
      id: 'e$hour',
      startTime: start,
      endTime: start.add(const Duration(minutes: 1)),
      liters: liters,
      source: 'test',
    );
  }

  Widget host(List<HydrationEntry> entries) => MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: Center(
            child: SizedBox(
              width: 400,
              child: HydrationIntradayChartCard(
                selectedDate: day,
                entries: entries,
                dailyGoalLiters: 2.0,
                formatter: UnitFormatter(
                  unitSystemProvider: () => UnitSystem.metric,
                ),
              ),
            ),
          ),
        ),
      );

  testWidgets('the day is drawn as a LINE, with an hour axis', (tester) async {
    await tester.pumpWidget(host([entry(8, 0.3), entry(13, 0.5)]));

    expect(find.byType(MetricLinePlot), findsOneWidget,
        reason: 'The day view is not drawing a line. If it fell back to the bar '
            'chart it would show one bar for the whole day, which is the bug.');
    // The hours are what make it a day chart rather than a total.
    expect(find.text('00:00'), findsOneWidget);
    expect(find.text('12:00'), findsOneWidget);
  });

  testWidgets('the line is CUMULATIVE and anchored at both ends', (tester) async {
    await tester.pumpWidget(host([entry(8, 0.3), entry(13, 0.5)]));

    final plot = tester.widget<MetricLinePlot>(find.byType(MetricLinePlot));
    final values = plot.points.map((p) => p.value).toList();

    // Starts at zero (midnight), climbs, and holds the total to the right edge.
    expect(values.first, 0.0);
    expect(plot.points.first.xFraction, 0.0);
    expect(plot.points.last.xFraction, 1.0);

    // Cumulative: 0.3 then 0.8 — never the per-entry 0.3 then 0.5, which would draw
    // a line that goes DOWN after a smaller second drink.
    expect(values, containsAllInOrder([0.0, 0.3, 0.8, 0.8]));
    for (var i = 1; i < values.length; i++) {
      expect(values[i], greaterThanOrEqualTo(values[i - 1]),
          reason: 'The cumulative line went DOWN — the running total is not '
              'running.');
    }
  });

  testWidgets('a drink at 06:00 sits a quarter of the way across',
      (tester) async {
    // The x position IS the information. If every point landed at the same
    // fraction, the chart would be a vertical line and tell you nothing about when.
    await tester.pumpWidget(host([entry(6, 0.25), entry(18, 0.25)]));

    final plot = tester.widget<MetricLinePlot>(find.byType(MetricLinePlot));
    // points[0] is the midnight anchor; [1] and [2] are the drinks.
    expect(plot.points[1].xFraction, closeTo(0.25, 0.01));
    expect(plot.points[2].xFraction, closeTo(0.75, 0.01));
  });

  testWidgets('TODAY ends the chart at now, not at midnight', (tester) async {
    // Otherwise a chart opened at 2pm would draw a flat line across ten hours that
    // have not happened yet, and every drink would look like it came early.
    final today = LocalDate.now();
    final morning = DateTime(today.year, today.month, today.day, 0, 30);

    await tester.pumpWidget(MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(
        body: SizedBox(
          width: 400,
          child: HydrationIntradayChartCard(
            selectedDate: today,
            entries: [
              HydrationEntry(
                id: 'e',
                startTime: morning,
                endTime: morning.add(const Duration(minutes: 1)),
                liters: 0.25,
                source: 'test',
              ),
            ],
            dailyGoalLiters: 2.0,
            formatter: UnitFormatter(unitSystemProvider: () => UnitSystem.metric),
          ),
        ),
      ),
    ));

    // The axis says "Now" rather than 24:00, and the elapsed day is shorter than a
    // full one — so a 00:30 drink sits further right than the 0.02 a 24h day gives.
    expect(find.text('24:00'), findsNothing);
    final plot = tester.widget<MetricLinePlot>(find.byType(MetricLinePlot));
    expect(plot.points[1].xFraction, greaterThan(0.0));
  });

  testWidgets('a day with nothing logged says so, and draws no line',
      (tester) async {
    await tester.pumpWidget(host(const []));

    expect(find.byType(MetricLinePlot), findsNothing,
        reason: 'An empty day drew a line from nothing to nothing.');
  });
}
