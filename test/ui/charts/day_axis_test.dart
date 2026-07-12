import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/charts/day_axis.dart';

/// The rule every intraday chart obeys, pinned in one place.
///
/// This is the regression test for a bug that shipped on five screens at once:
/// each card scaled x by the time ELAPSED so far, then drew a fixed
/// `00:00 / 06:00 / 12:00 / 18:00` axis underneath. Opened at 12:49, a drink at
/// 09:29 was drawn at 74% of the width — under the label that says quarter past
/// five. The chart's whole job is to say WHEN, and it said the wrong hour.
void main() {
  const day = LocalDate(2026, 6, 22);
  DateTime at(int hour, [int minute = 0]) =>
      DateTime(2026, 6, 22, hour, minute);

  group('DayAxis', () {
    test('places a time at its real hour, not at its share of the elapsed day',
        () {
      // The chart is open at 12:49 — the exact case from the bug report.
      final axis = DayAxis(day, now: at(12, 49));

      // 09:29 is 39.5% of the way through the DAY. The old maths made it 74% of
      // the way through the part of the day that had happened.
      expect(axis.fractionOf(at(9, 29)), closeTo(0.395, 0.001));
    });

    test('spans the whole day, so the labels under it are true', () {
      final axis = DayAxis(day, now: at(12, 49));
      expect(axis.fractionOf(at(0)), 0.0);
      expect(axis.fractionOf(at(6)), closeTo(0.25, 1e-9));
      expect(axis.fractionOf(at(12)), closeTo(0.5, 1e-9));
      expect(axis.fractionOf(at(18)), closeTo(0.75, 1e-9));
    });

    test("today's series stops at now, rather than claiming the rest of the day",
        () {
      expect(DayAxis(day, now: at(12, 0)).endFraction, closeTo(0.5, 1e-9));
      expect(DayAxis(day, now: at(6, 0)).endFraction, closeTo(0.25, 1e-9));
    });

    test('a past day runs to its right edge', () {
      final axis = DayAxis(day, now: DateTime(2026, 6, 23, 4));
      expect(axis.isToday, isFalse);
      expect(axis.endFraction, 1.0);
    });

    test('clamps a time from outside the day onto it', () {
      final axis = DayAxis(day, now: at(23));
      expect(axis.fractionOf(DateTime(2026, 6, 21, 22)), 0.0);
      expect(axis.fractionOf(DateTime(2026, 6, 23, 2)), 1.0);
    });

    test('honours the injected clock rather than the wall clock', () {
      // `now` in the past: the day under test is not today, whatever the machine
      // running the test happens to think.
      expect(DayAxis(day, now: DateTime(2030, 1, 1)).isToday, isFalse);
      expect(DayAxis(day, now: at(9)).isToday, isTrue);
    });
  });

  testWidgets('DayAxisLabels reads midnight to midnight', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: DayAxisLabels(axis: DayAxis(day, now: at(12, 49))),
        ),
      ),
    );

    for (final label in ['00:00', '06:00', '12:00', '18:00', '24:00']) {
      expect(find.text(label), findsOneWidget);
    }
  });
}
