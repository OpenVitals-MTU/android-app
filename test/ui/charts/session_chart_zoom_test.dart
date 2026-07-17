import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/charts/chart_axis.dart';
import 'package:openvitals/ui/charts/chart_viewport.dart';
import 'package:openvitals/ui/charts/chart_zoom.dart';
import 'package:openvitals/ui/charts/metric_session_chart.dart';
import 'package:openvitals/ui/charts/session_axis.dart';

/// Zooming a recorded session — the sprint, the climb, the minute the heart rate spiked.
///
/// Same axis, same arithmetic, same trap: the elapsed-time row has to move with the trace.
/// A row still reading `0:00 … 1:00:00` under a plot showing the last ten minutes would be
/// describing a chart that is not there.

final _start = DateTime.utc(2026, 7, 14, 9, 0);
final _end = DateTime.utc(2026, 7, 14, 10, 0);

void main() {
  group('elapsedLabelsFor', () {
    final axis = SessionAxis(start: _start, end: _end);

    test('the whole session is the five labels it always was', () {
      expect(axis.elapsedLabelsFor(), axis.elapsedLabels);
      expect(axis.elapsedLabelsFor().first, '0:00');
    });

    test('zoomed in, the row says the minutes actually on the plot', () {
      // The last quarter of an hour-long session: 45:00 to 1:00:00.
      const view = ChartViewport(start: 0.75, end: 1.0);
      final labels = axis.elapsedLabelsFor(view);

      expect(labels.first, '45:00');
      expect(labels.last, axis.elapsedLabels.last);
      expect(labels.first, isNot('0:00'),
          reason: 'a row still starting at zero would name a moment not on the plot');
    });
  });

  testWidgets('pinching a session chart zooms the trace AND its elapsed row',
      (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: MetricSessionChart(
            title: 'Heart rate',
            axis: SessionAxis(start: _start, end: _end),
            samples: [
              for (var minute = 0; minute <= 60; minute++)
                (
                  time: _start.add(Duration(minutes: minute)),
                  value: 120.0 + minute,
                ),
            ],
            range: const ChartRange(100, 200),
            accentColor: const Color(0xFFFF3366),
            valueFormatter: (value) => '${value.round()}',
            countText: '61',
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('0:00'), findsOneWidget);

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

    // The row moved with the trace. If 0:00 were still under the left edge of a plot
    // showing the middle of the ride, every reading on it would be mistimed.
    expect(find.text('0:00'), findsNothing);
  });
}
