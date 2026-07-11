import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/features/sleep/sleep_schedule_chart.dart';
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

    test('label ticks are hourly, thinning to two-hourly over eight hours', () {
      const short = ScheduleAxis(min: 240, max: 600); // 6 h
      expect(short.labelMinutes(), [240, 300, 360, 420, 480, 540, 600]);

      const tall = ScheduleAxis(min: 240, max: 840); // 10 h
      expect(tall.labelMinutes(), [240, 360, 480, 600, 720, 840]);
    });
  });

  group('toSleepScheduleDays', () {
    test('takes the earliest bedtime and latest wake of each night', () {
      final date = LocalDate(2026, 7, 5);
      final days = toSleepScheduleDays({
        date: [
          session(at(5, 23, 30), at(6, 3)),
          session(at(6, 3, 30), at(6, 7)),
        ],
      });

      expect(days, hasLength(1));
      expect(days.single.inBedStart, at(5, 23, 30));
      expect(days.single.inBedEnd, at(6, 7));
    });

    test('merges and sorts every session stage', () {
      final date = LocalDate(2026, 7, 5);
      final days = toSleepScheduleDays({
        date: [
          session(at(6, 3), at(6, 7), stages: [
            stage(at(6, 3), at(6, 7), SleepStage.stageRem),
          ]),
          session(at(5, 23), at(6, 3), stages: [
            stage(at(5, 23), at(6, 3), SleepStage.stageDeep),
          ]),
        ],
      });

      final stages = days.single.stages;
      expect(stages, hasLength(2));
      expect(stages.first.stageType, SleepStage.stageDeep);
      expect(stages.last.stageType, SleepStage.stageRem);
    });

    test('a night with no sessions has no bedtime', () {
      final days = toSleepScheduleDays({LocalDate(2026, 7, 5): const []});
      expect(days.single.inBedStart, isNull);
      expect(days.single.inBedEnd, isNull);
    });

    test('days come out in date order', () {
      final days = toSleepScheduleDays({
        LocalDate(2026, 7, 6): [session(at(6, 23), at(7, 7))],
        LocalDate(2026, 7, 5): [session(at(5, 23), at(6, 7))],
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
