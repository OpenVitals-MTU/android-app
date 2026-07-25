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

  group('SessionAxis with pauses', () {
    // The ride behind this: 21 minutes wall-clock, 10½ of them paused. The
    // pause took more than half the width of every card, and the elevation
    // trace joined the fixes either side of it with a smooth spline — a climb
    // from -29 m to -10 m that never happened.
    final paused = SessionAxis(
      start: start,
      end: end,
      pauses: [
        SessionPause(DateTime(2026, 6, 22, 9, 10), DateTime(2026, 6, 22, 9, 40)),
      ],
    );

    test('the axis is moving time, not wall-clock', () {
      expect(paused.duration, const Duration(minutes: 30));
    });

    test('the fixes either side of a pause end up next to each other', () {
      // 10 minutes in, then the pause, then the next fix: adjacent on the axis,
      // a third of the way across, rather than a third and two thirds with a
      // fabricated line between them.
      expect(paused.fractionOf(DateTime(2026, 6, 22, 9, 10)), closeTo(1 / 3, 1e-9));
      expect(paused.fractionOf(DateTime(2026, 6, 22, 9, 40)), closeTo(1 / 3, 1e-9));
      expect(paused.fractionOf(end), 1.0);
    });

    test('what was recorded DURING a pause sits where the pause began', () {
      // A strap does not stop because the ride did. Those samples belong at the
      // one point on the axis — they must not stretch the pause back open.
      expect(paused.fractionOf(DateTime(2026, 6, 22, 9, 20)), closeTo(1 / 3, 1e-9));
      expect(paused.fractionOf(DateTime(2026, 6, 22, 9, 39)), closeTo(1 / 3, 1e-9));
    });

    test('the scrubber and the labels agree, both in moving time', () {
      expect(paused.elapsedAt(0.5), const Duration(minutes: 15));
      expect(paused.elapsedLabels,
          ['0:00', '7:30', '15:00', '22:30', '30:00']);
    });

    test('overlapping pauses are counted once', () {
      // A source app is free to write them overlapping; subtracting each in turn
      // would take the shared stretch out twice and run the axis past its end.
      final overlapping = SessionAxis(
        start: start,
        end: end,
        pauses: [
          SessionPause(
              DateTime(2026, 6, 22, 9, 10), DateTime(2026, 6, 22, 9, 40)),
          SessionPause(
              DateTime(2026, 6, 22, 9, 20), DateTime(2026, 6, 22, 9, 30)),
        ],
      );

      expect(overlapping.duration, const Duration(minutes: 30));
      expect(overlapping.fractionOf(end), 1.0);
    });

    test('a pause reaching outside the session is clipped to it', () {
      final overhanging = SessionAxis(
        start: start,
        end: end,
        pauses: [
          SessionPause(DateTime(2026, 6, 22, 8), DateTime(2026, 6, 22, 9, 30)),
        ],
      );

      expect(overhanging.duration, const Duration(minutes: 30));
      expect(overhanging.fractionOf(DateTime(2026, 6, 22, 9, 30)), 0.0);
    });

    test('an entirely paused session does not divide by zero', () {
      final allPaused = SessionAxis(
        start: start,
        end: end,
        pauses: [SessionPause(start, end)],
      );

      expect(allPaused.durationMs, 1);
      expect(allPaused.fractionOf(DateTime(2026, 6, 22, 9, 30)), 0.0);
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
