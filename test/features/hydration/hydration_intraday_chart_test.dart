import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/hydration/presentation/hydration_intraday_chart.dart';
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
    expect(values.last, closeTo(0.8, 0.001));
    for (var i = 1; i < values.length; i++) {
      expect(values[i], greaterThanOrEqualTo(values[i - 1]),
          reason: 'The cumulative line went DOWN — the running total is not '
              'running.');
    }
  });

  testWidgets('a drink at 06:00 sits a QUARTER of the way across the DAY',
      (tester) async {
    // The x position IS the information, and it must agree with the axis under it.
    // Kotlin scaled x by the time ELAPSED so far, so on a chart opened at 12:49 a
    // drink at 09:29 landed at 74% of the width — under an axis reading 00:00 /
    // 06:00 / 12:00 / 18:00, which put it at quarter past five. The one thing this
    // chart exists to say is WHEN, and it said the wrong hour.
    await tester.pumpWidget(host([entry(6, 0.25), entry(18, 0.25)]));

    final plot = tester.widget<MetricLinePlot>(find.byType(MetricLinePlot));
    final xs = plot.points.map((p) => p.xFraction).toList();

    // 06:00 is a quarter of a DAY, and 18:00 is three quarters — whatever the hour
    // happens to be when the test runs.
    expect(xs, contains(closeTo(0.25, 0.001)));
    expect(xs, contains(closeTo(0.75, 0.001)));
  });

  testWidgets('the line is the running total, plotted at each drink\'s real hour',
      (tester) async {
    // This used to be a STEP — two points per drink, so the flat stretch between
    // glasses stayed flat and "nothing since nine" was legible at a glance.
    //
    // The lines are smoothed now, and a curve through a step is just the cumulative
    // curve, so hydration is cumulative like every other day chart. The cost, worth
    // stating: between two drinks the line slopes gently upward through hours you
    // drank nothing. The totals and the hours are still exact; only the shape
    // between them is inferred.
    await tester.pumpWidget(host([entry(6, 0.25), entry(18, 0.25)]));

    final plot = tester.widget<MetricLinePlot>(find.byType(MetricLinePlot));

    // Anchored at midnight with nothing drunk.
    expect(plot.points.first.xFraction, 0);
    expect(plot.points.first.value, 0);

    // One point per drink, at its real hour, carrying the running total.
    final atSix = plot.points.where((p) => (p.xFraction - 0.25).abs() < 0.001);
    expect(atSix, hasLength(1));
    expect(atSix.single.value, 0.25);

    final atSix18 = plot.points.where((p) => (p.xFraction - 0.75).abs() < 0.001);
    expect(atSix18.single.value, 0.5, reason: 'the total, not the glass');

    // The total is held to the end of the axis — it never falls.
    expect(plot.points.last.value, 0.5);
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

    // The axis is the whole day either way, so 00:30 sits at 1/48th of the width —
    // near the left edge, where half past midnight belongs.
    final plot = tester.widget<MetricLinePlot>(find.byType(MetricLinePlot));
    expect(plot.points[1].xFraction, closeTo(0.5 / 24, 0.001));

    // But the line STOPS at now rather than running to the right edge: the rest of
    // the day has not happened.
    expect(plot.points.last.xFraction, lessThan(1.0));
  });

  testWidgets('a day with nothing logged says so, and draws no line',
      (tester) async {
    await tester.pumpWidget(host(const []));

    expect(find.byType(MetricLinePlot), findsNothing,
        reason: 'An empty day drew a line from nothing to nothing.');
  });
}
