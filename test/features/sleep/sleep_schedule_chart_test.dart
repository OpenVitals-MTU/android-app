import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/features/sleep/application/sleep_display.dart';
import 'package:openvitals/features/sleep/presentation/sleep_schedule_chart.dart';
// The 18:00-anchored minute maths now lives with the other axes.
import 'package:openvitals/ui/charts/schedule_axis.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// The schedule chart's axis is anchored at 18:00 so a night that crosses
/// midnight stays one contiguous bar. These pin that arithmetic.
void main() {
  DateTime at(int day, int hour, [int minute = 0]) =>
      DateTime(2026, 7, day, hour, minute);

  SleepStage stage(DateTime start, DateTime end, int type) =>
      SleepStage(startTime: start, endTime: end, stageType: type);

  SleepData session(DateTime start, DateTime end, {List<SleepStage>? stages}) =>
      SleepData(
        id: '${start.millisecondsSinceEpoch}',
        startTime: start,
        endTime: end,
        durationMs: end.difference(start).inMilliseconds,
        source: 'test',
        stages: stages ?? const [],
      );

  group('anchoredMinutes', () {
    test('the 18:00 anchor is minute zero', () {
      expect(anchoredMinutes(at(5, 18)), 0);
    });

    test('an evening bedtime sits early on the axis', () {
      // 23:00 is five hours after the anchor.
      expect(anchoredMinutes(at(5, 23)), 5 * 60);
    });

    test('a morning wake-up wraps past midnight, not back to the top', () {
      // 07:00 is 13 hours after the 18:00 anchor.
      expect(anchoredMinutes(at(6, 7)), 13 * 60);
    });

    test('is always inside a single day', () {
      for (var hour = 0; hour < 24; hour++) {
        final value = anchoredMinutes(at(5, hour));
        expect(value, greaterThanOrEqualTo(0));
        expect(value, lessThan(kMinutesPerDay));
      }
    });
  });

  group('normalizedEndMinutes', () {
    test('a wake-up after the bedtime stays after it', () {
      final start = at(5, 23);
      final end = at(6, 7);
      expect(
        normalizedEndMinutes(start, end),
        greaterThan(anchoredMinutes(start)),
      );
      // 23:00 → 07:00 is eight hours in bed.
      expect(normalizedEndMinutes(start, end) - anchoredMinutes(start), 8 * 60);
    });

    test('an afternoon nap that crosses the anchor still moves forward', () {
      // 17:00 (just before the anchor) → 19:00 (just after it).
      final start = at(5, 17);
      final end = at(5, 19);
      expect(normalizedEndMinutes(start, end) - anchoredMinutes(start), 2 * 60);
    });
  });

  group('anchoredMinuteToClock', () {
    test('round-trips the anchor and a wrapped morning', () {
      expect(anchoredMinuteToClock(0), (hour: 18, minute: 0));
      expect(anchoredMinuteToClock(6 * 60), (hour: 0, minute: 0));
      expect(anchoredMinuteToClock(13 * 60), (hour: 7, minute: 0));
    });

    test('minuteOfDayToAnchored is its inverse', () {
      for (final minuteOfDay in [0, 6 * 60, 18 * 60, 23 * 60 + 59]) {
        final anchored = minuteOfDayToAnchored(minuteOfDay).toInt();
        final clock = anchoredMinuteToClock(anchored);
        expect(clock.hour * 60 + clock.minute, minuteOfDay);
      }
    });
  });

  group('scheduleAxisRange', () {
    test('is null when no night has a bedtime', () {
      final days = [
        SleepScheduleDay(date: LocalDate(2026, 7, 5), inBedStart: null, inBedEnd: null),
      ];
      expect(scheduleAxisRange(days), isNull);
    });

    test('spans every night, padded to whole hours', () {
      final days = [
        SleepScheduleDay(
          date: LocalDate(2026, 7, 5),
          inBedStart: at(5, 23, 20),
          inBedEnd: at(6, 7, 10),
        ),
        SleepScheduleDay(
          date: LocalDate(2026, 7, 6),
          inBedStart: at(6, 22, 40),
          inBedEnd: at(7, 6, 30),
        ),
      ];
      final axis = scheduleAxisRange(days)!;

      // Earliest bedtime 22:40 → anchored 280 → floored to 240 (22:00).
      expect(axis.min, 4 * 60);
      // Latest wake 07:10 → anchored 790 → ceiled to 840 (08:00).
      expect(axis.max, 14 * 60);
    });

    test('one impossible night does not stretch the axis past a day', () {
      // Reported on a monthly chart (issue: 36-hour sleep chart). A single
      // record running 09:00 to 08:00 the NEXT morning still starts inside the
      // 18:00-10:00 window, so it reaches the chart; normalised it lands at 2280
      // anchored minutes and dragged the range to ~32 hours, wrapping past
      // midnight twice and flattening every real night.
      final days = [
        SleepScheduleDay(
          date: LocalDate(2026, 7, 5),
          inBedStart: at(5, 0, 16),
          inBedEnd: at(5, 6, 29),
        ),
        SleepScheduleDay(
          date: LocalDate(2026, 7, 6),
          inBedStart: at(6, 9, 0),
          inBedEnd: at(7, 8, 0), // ~23 h in bed: not a night, a bad record
        ),
        SleepScheduleDay(
          date: LocalDate(2026, 7, 7),
          inBedStart: at(7, 23, 40),
          inBedEnd: at(8, 7, 10),
        ),
      ];

      final axis = scheduleAxisRange(days)!;

      // Scaled by the two real nights only: 23:40 -> anchored 340, floored to
      // 300; 07:10 -> anchored 790, ceiled to 840.
      expect(axis.min, 5 * 60);
      expect(axis.max, 14 * 60);
      expect(axis.span, lessThan(kMinutesPerDay));
    });

    test('a long but possible lie-in still counts', () {
      // The guard rejects the impossible, not the merely unusual: 14 h in bed is
      // a real thing a person can do and must still set the scale.
      final days = [
        SleepScheduleDay(
          date: LocalDate(2026, 7, 5),
          inBedStart: at(4, 20, 0),
          inBedEnd: at(5, 10, 0),
        ),
      ];

      final axis = scheduleAxisRange(days)!;
      expect(axis.min, 2 * 60); // 20:00 -> anchored 120
      expect(axis.max, 16 * 60); // 10:00 -> anchored 960
    });

    test('is null when every night is impossible', () {
      // Nothing plausible to scale by: the caller falls back to the duration
      // bar chart rather than drawing a nonsense axis.
      final days = [
        SleepScheduleDay(
          date: LocalDate(2026, 7, 6),
          inBedStart: at(6, 9, 0),
          inBedEnd: at(7, 8, 0),
        ),
      ];
      expect(scheduleAxisRange(days), isNull);
    });

    test('label ticks are hourly, thinning to two-hourly over eight hours', () {
      const short = ScheduleAxis(min: 240, max: 600); // 6 h
      expect(short.labelMinutes(), [240, 300, 360, 420, 480, 540, 600]);

      const tall = ScheduleAxis(min: 240, max: 840); // 10 h
      expect(tall.labelMinutes(), [240, 360, 480, 600, 720, 840]);
    });
  });

  group('toSleepScheduleDays', () {
    // The chart now consumes ONE already-merged night per date (built upstream by
    // dailySleepSummary); it only maps span + stages onto the bar.
    test('maps a merged night to its span and stages', () {
      final date = LocalDate(2026, 7, 5);
      final night = session(at(5, 23, 30), at(6, 7), stages: [
        stage(at(5, 23, 30), at(6, 3), SleepStage.stageDeep),
        stage(at(6, 3), at(6, 7), SleepStage.stageRem),
      ]);
      final days = toSleepScheduleDays({date: night});

      expect(days, hasLength(1));
      expect(days.single.inBedStart, at(5, 23, 30));
      expect(days.single.inBedEnd, at(6, 7));
      expect(days.single.stages.map((s) => s.stageType),
          [SleepStage.stageDeep, SleepStage.stageRem]);
    });

    test('a night with no stages carries an empty stage list', () {
      final date = LocalDate(2026, 7, 5);
      final days = toSleepScheduleDays({date: session(at(5, 23), at(6, 7))});
      expect(days.single.inBedStart, at(5, 23));
      expect(days.single.stages, isEmpty);
    });

    test('a date with no night has no bedtime', () {
      final days = toSleepScheduleDays({LocalDate(2026, 7, 5): null});
      expect(days.single.inBedStart, isNull);
      expect(days.single.inBedEnd, isNull);
    });

    test('days come out in date order', () {
      final days = toSleepScheduleDays({
        LocalDate(2026, 7, 6): session(at(6, 23), at(7, 7)),
        LocalDate(2026, 7, 5): session(at(5, 23), at(6, 7)),
      });
      expect(
        [for (final day in days) day.date],
        [LocalDate(2026, 7, 5), LocalDate(2026, 7, 6)],
      );
    });
  });

  group('widget', () {
    Future<void> pumpChart(
      WidgetTester tester,
      List<SleepScheduleDay> days, {
      ValueChanged<LocalDate>? onDateSelected,
    }) async {
      tester.view.physicalSize = const Size(800, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await tester.pumpWidget(
        MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: Scaffold(
            body: SleepScheduleStageChart(
              title: 'Sleep',
              summaryText: 'Avg 7.8h · 2 nights',
              days: days,
              selectedRange: TimeRange.week,
              onDateSelected: onDateSelected,
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();
    }

    testWidgets('renders nothing when no night has a bedtime', (tester) async {
      await pumpChart(tester, [
        SleepScheduleDay(
            date: LocalDate(2026, 7, 5), inBedStart: null, inBedEnd: null),
      ]);
      expect(find.text('Sleep'), findsNothing);
      expect(find.text('Avg 7.8h · 2 nights'), findsNothing);
    });

    testWidgets('draws the chart and its summary', (tester) async {
      await pumpChart(tester, [
        SleepScheduleDay(
          date: LocalDate(2026, 7, 5),
          inBedStart: at(5, 23),
          inBedEnd: at(6, 7),
        ),
      ]);
      expect(tester.takeException(), isNull);
      expect(find.text('Sleep'), findsOneWidget);
      expect(find.text('Avg 7.8h · 2 nights'), findsOneWidget);
    });

    testWidgets('an impossible night still renders, it just does not scale',
        (tester) async {
      // The axis leaves a >16 h record out of its range so it cannot flatten the
      // chart, but the bar is still painted (clipped to the canvas by `yFor`).
      // Dropping it would hide that the day has any data at all.
      await pumpChart(tester, [
        SleepScheduleDay(
          date: LocalDate(2026, 7, 5),
          inBedStart: at(5, 23),
          inBedEnd: at(6, 7),
        ),
        SleepScheduleDay(
          date: LocalDate(2026, 7, 6),
          inBedStart: at(6, 9),
          inBedEnd: at(7, 8),
        ),
      ]);
      expect(tester.takeException(), isNull);
      expect(find.text('Sleep'), findsOneWidget);
    });

    testWidgets('tapping a night reports that night, not its neighbour',
        (tester) async {
      final dates = [
        LocalDate(2026, 7, 5),
        LocalDate(2026, 7, 6),
        LocalDate(2026, 7, 7),
      ];
      LocalDate? tapped;
      await pumpChart(
        tester,
        [
          for (var i = 0; i < dates.length; i++)
            SleepScheduleDay(
              date: dates[i],
              inBedStart: at(5 + i, 23),
              inBedEnd: at(6 + i, 7),
            ),
        ],
        onDateSelected: (date) => tapped = date,
      );

      // Scope to the chart's own canvas: Material draws CustomPaints too.
      final chart = find.descendant(
        of: find.byType(SleepScheduleStageChart),
        matching: find.byType(CustomPaint),
      );
      final rect = tester.getRect(chart.last);
      // A tap two-thirds across lands on the last of three nights.
      await tester.tapAt(Offset(rect.left + rect.width * 0.8, rect.center.dy));
      await tester.pump();
      expect(tapped, dates[2]);

      await tester.tapAt(Offset(rect.left + 4, rect.center.dy));
      await tester.pump();
      expect(tapped, dates[0]);
    });
  });
}
