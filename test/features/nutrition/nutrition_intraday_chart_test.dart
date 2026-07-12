import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/nutrition/nutrition_sections.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/charts/metric_line_plot.dart';

NutritionEntry _entry(DateTime time, {required double energyKcal}) =>
    NutritionEntry(
      time: time,
      mealType: 0,
      name: 'Meal',
      energyKcal: energyKcal,
      proteinGrams: null,
      carbsGrams: null,
      fatGrams: null,
      fiberGrams: null,
      sugarGrams: null,
      source: 'Test source',
    );

Widget _harness(Widget child) => MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(body: SingleChildScrollView(child: child)),
    );

void main() {
  final formatter = UnitFormatter(unitSystemProvider: () => UnitSystem.metric);
  final series = NutritionSeries(NutritionNutrient.energy, const []);

  group('cumulativeNutritionPoints (Kotlin parity)', () {
    test('sorts by time, accumulates, and drops non-positive readings', () {
      final day = DateTime(2026, 3, 4);
      final points = cumulativeNutritionPoints(
        [
          _entry(day.add(const Duration(hours: 13)), energyKcal: 700),
          _entry(day.add(const Duration(hours: 8)), energyKcal: 300),
          // Zero and null readings never enter the curve.
          _entry(day.add(const Duration(hours: 10)), energyKcal: 0),
        ],
        NutritionNutrient.energy,
      );

      expect(points.map((p) => p.value).toList(), [300.0, 1000.0]);
      expect(points.first.time.hour, 8);
      expect(points.last.time.hour, 13);
    });

    test('an entry with no value for the nutrient is skipped', () {
      final points = cumulativeNutritionPoints(
        [_entry(DateTime(2026, 3, 4, 9), energyKcal: 500)],
        NutritionNutrient.protein,
      );
      expect(points, isEmpty);
    });
  });

  group('NutritionIntradayChartCard', () {
    testWidgets('plots the cumulative curve bracketed by 0 and the total',
        (tester) async {
      final day = LocalDate(2026, 3, 4);
      final dayStart = DateTime(2026, 3, 4);
      await tester.pumpWidget(_harness(NutritionIntradayChartCard(
        day: day,
        series: series,
        entries: [
          _entry(dayStart.add(const Duration(hours: 6)), energyKcal: 400),
          _entry(dayStart.add(const Duration(hours: 18)), energyKcal: 600),
        ],
        formatter: formatter,
        // A past day: the axis runs the full 24h, so 06:00 sits at 0.25.
        now: () => DateTime(2026, 3, 10),
      )));

      final plot = tester.widget<MetricLinePlot>(find.byType(MetricLinePlot));
      expect(plot.points.first.xFraction, 0);
      expect(plot.points.first.value, 0);
      expect(plot.points.last.xFraction, 1);
      expect(plot.points.last.value, 1000);
      expect(plot.points[1].xFraction, closeTo(0.25, 0.001));
      expect(plot.points[1].value, 400);
      expect(plot.points[2].value, 1000);
      expect(plot.minValue, 0);
      expect(plot.maxValue, 1000);

      // Past day: the right-hand axis label is midnight, not "Now".
      expect(find.text('24:00'), findsOneWidget);
      expect(find.text('00:00'), findsOneWidget);
    });

    testWidgets('today plots a meal at its real hour and stops the line at now',
        (tester) async {
      final dayStart = DateTime(2026, 3, 4);
      await tester.pumpWidget(_harness(NutritionIntradayChartCard(
        day: LocalDate(2026, 3, 4),
        series: series,
        entries: [_entry(dayStart.add(const Duration(hours: 6)), energyKcal: 400)],
        formatter: formatter,
        now: () => dayStart.add(const Duration(hours: 12)),
      )));

      final plot = tester.widget<MetricLinePlot>(find.byType(MetricLinePlot));

      // Breakfast at 06:00 is a quarter of the way through the DAY, and that is
      // where it is drawn. This test used to assert 0.5 — it scaled against the
      // twelve hours that had elapsed, so the meal was drawn at noon, under an
      // axis whose labels said otherwise. The bug was pinned, not caught.
      expect(plot.points[1].xFraction, closeTo(0.25, 0.001));

      // The line stops at now. It does not run to the right edge drawing an
      // afternoon that has not happened.
      expect(plot.points.last.xFraction, closeTo(0.5, 0.001));
      expect(plot.points.last.value, 400);

      // The axis is the whole day, today included.
      expect(find.text('24:00'), findsOneWidget);
    });

    testWidgets('renders the empty-day message and no plot without entries',
        (tester) async {
      await tester.pumpWidget(_harness(NutritionIntradayChartCard(
        day: LocalDate(2026, 3, 4),
        series: series,
        entries: const [],
        formatter: formatter,
        now: () => DateTime(2026, 3, 10),
      )));

      expect(find.byType(MetricLinePlot), findsNothing);
      expect(tester.takeException(), isNull);
    });
  });
}
