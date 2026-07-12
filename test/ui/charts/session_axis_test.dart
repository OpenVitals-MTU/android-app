import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/ui/charts/chart_axis.dart';
import 'package:openvitals/ui/charts/session_axis.dart';

/// The session counterpart of the day-axis rule: a sample is placed against the
/// whole recorded session, not against the samples that happen to exist.
void main() {
  final start = DateTime(2026, 6, 22, 9);
  final end = DateTime(2026, 6, 22, 10);
  final axis = SessionAxis(start: start, end: end);

  group('SessionAxis', () {
    test('places a sample at its elapsed position in the session', () {
      expect(axis.fractionOf(start), 0.0);
      expect(axis.fractionOf(DateTime(2026, 6, 22, 9, 15)), closeTo(0.25, 1e-9));
      expect(axis.fractionOf(DateTime(2026, 6, 22, 9, 30)), closeTo(0.5, 1e-9));
      expect(axis.fractionOf(end), 1.0);
    });

    test('spans the recorded session, not the samples that exist', () {
      // A trace whose sensor died twenty minutes into an hour-long ride must stop
      // a third of the way across. Normalizing against the samples instead would
      // stretch it to the right edge and imply an hour of readings.
      final lastSample = DateTime(2026, 6, 22, 9, 20);
      expect(axis.fractionOf(lastSample), closeTo(1 / 3, 1e-9));
    });

    test('clamps a sample from outside the session onto it', () {
      expect(axis.fractionOf(DateTime(2026, 6, 22, 8)), 0.0);
      expect(axis.fractionOf(DateTime(2026, 6, 22, 11)), 1.0);
    });

    test('a zero-length session does not divide by zero', () {
      // A recording stopped the instant it started. Rare, but it exists, and the
      // painter would take the whole card down with it.
      final instant = SessionAxis(start: start, end: start);
      expect(instant.fractionOf(start), 0.0);
      expect(instant.durationMs, 1);
    });

    test('labels the quarters in elapsed time', () {
      expect(axis.elapsedLabels, ['0:00', '15:00', '30:00', '45:00', '1:00:00']);
    });
  });

  testWidgets('SessionAxisLabels starts where the plot starts', (tester) async {
    await tester.pumpWidget(
      MaterialApp(home: Scaffold(body: SessionAxisLabels(axis: axis))),
    );

    expect(
      tester.getTopLeft(find.text('0:00')).dx,
      greaterThanOrEqualTo(kChartPlotInset),
    );
  });
}
