import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/charts/bar_chart.dart';
import 'package:openvitals/ui/charts/chart_zoom.dart';

/// Zooming the weekly bar chart.
///
/// A bar is a slot on the same 0..1 axis every other chart uses — the nth of n — so the
/// arithmetic is the same one the day charts use, and the slots simply get wider as fewer
/// of them are on screen.
///
/// The thing that could quietly break is the TAP. The week chart lets you select a day by
/// tapping its bar, and the finger lands on the plot, not on the data: tapping the third
/// bar you can SEE has to select the third bar you can see, not the third bar of the week.

final _monday = LocalDate(2026, 7, 13);

Future<void> _pump(
  WidgetTester tester, {
  LocalDate? selected,
  void Function(LocalDate)? onDateSelected,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(
        body: PeriodBarChart(
          title: 'Steps',
          values: [
            for (var day = 0; day < 7; day++)
              PeriodChartValue(_monday.plusDays(day), (day + 1) * 1000.0),
          ],
          selectedRange: TimeRange.week,
          period: DatePeriod(_monday, _monday.plusDays(6)),
          accentColor: const Color(0xFF00AAFF),
          summaryText: '28,000 steps',
          selectedDate: selected,
          onDateSelected: onDateSelected,
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> _pinchOpen(WidgetTester tester, {Offset at = Offset.zero}) async {
  final center = tester.getCenter(find.byType(ChartZoom)) + at;
  final left = await tester.startGesture(center - const Offset(30, 0));
  final right = await tester.startGesture(center + const Offset(30, 0));
  await tester.pump();
  await left.moveBy(const Offset(-90, 0));
  await right.moveBy(const Offset(90, 0));
  await tester.pump();
  await left.up();
  await right.up();
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('a week chart draws every day until it is pinched', (tester) async {
    await _pump(tester);

    // Seven days, seven labels.
    expect(find.byType(PeriodBarChart), findsOneWidget);
    final before = tester
        .widgetList<Text>(find.byType(Text))
        .map((text) => text.data)
        .whereType<String>()
        .toList();
    expect(before, contains('Steps'));

    await _pinchOpen(tester);

    // Zoomed, the days that have scrolled off are not drawn at all — a label with no bar
    // under it names nothing.
    final after = tester
        .widgetList<Text>(find.byType(Text))
        .map((text) => text.data)
        .whereType<String>()
        .toList();
    expect(after.length, lessThan(before.length));
  });

  testWidgets('tapping a bar on a ZOOMED chart selects the day under the finger',
      (tester) async {
    LocalDate? tapped;
    await _pump(tester, onDateSelected: (date) => tapped = date);

    // Unzoomed, the far left of the plot is Monday.
    final plot = tester.getRect(find.byType(ChartZoom));
    await tester.tapAt(Offset(plot.left + 4, plot.top + 20));
    await tester.pumpAndSettle();
    expect(tapped, _monday);

    // Now zoom in about the middle of the week and tap the left edge again. It is no
    // longer Monday that is there — and if the tap still said Monday, the chart would be
    // selecting a day you cannot even see.
    await _pinchOpen(tester);
    tapped = null;
    await tester.tapAt(Offset(plot.left + 4, plot.top + 20));
    await tester.pumpAndSettle();

    expect(tapped, isNotNull);
    expect(tapped, isNot(_monday),
        reason: 'the tap must resolve against what is ON SCREEN, not against the week');
  });
}
