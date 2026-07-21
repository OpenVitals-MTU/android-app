import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/model/recording_method.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/domain/preferences/sleep_window.dart';
import 'package:openvitals/domain/usecase/load_sleep_period_use_case.dart';
import 'package:openvitals/features/sleep/application/sleep_display.dart';

/// The derivation the screen used to do in its build path — the duration
/// points, the nights, the averages, the stage shares, the schedule days, the
/// goal progress — now a pure function the view-model calls once per load, so
/// it can be tested with no widget at all.

/// One night, ending at 07:00 on [date] and running [hours] backwards from it.
SleepData _night(
  LocalDate date, {
  double hours = 8,
  String? id,
  int? recordingMethod,
}) {
  final end = DateTime(date.year, date.month, date.day, 7);
  final durationMs = (hours * 3600000).round();
  final start = end.subtract(Duration(milliseconds: durationMs));
  return SleepData(
    id: id ?? 'night-$date',
    startTime: start,
    endTime: end,
    durationMs: durationMs,
    source: 'test',
    recordingMethod: recordingMethod,
    stages: [
      SleepStage(
        startTime: start,
        endTime: start.add(Duration(milliseconds: durationMs ~/ 2)),
        stageType: SleepStage.stageLight,
      ),
      SleepStage(
        startTime: start.add(Duration(milliseconds: durationMs ~/ 2)),
        endTime: end,
        stageType: SleepStage.stageDeep,
      ),
    ],
  );
}

SleepDisplay _display({
  SleepPeriodLoadResult result = const SleepPeriodLoadResult(),
  TimeRange selectedRange = TimeRange.week,
  required LocalDate selectedDate,
  double dailyGoalHours = 8.0,
}) =>
    buildSleepDisplay(
      result: result,
      selectedRange: selectedRange,
      selectedDate: selectedDate,
      sleepWindow: SleepWindow.defaultWindow,
      weekPeriodMode: WeekPeriodMode.mondayToSunday,
      dailyGoalHours: dailyGoalHours,
    );

void main() {
  // A Wednesday, safely in the past: the week is a whole, unclipped 7 days.
  const wednesday = LocalDate(2026, 3, 4);
  const tuesday = LocalDate(2026, 3, 3);

  test('an empty period derives zeroes, not nulls', () {
    final display = _display(selectedDate: wednesday);

    // Every night of the week is still a point — a zero-hour one.
    expect(display.durationPoints.length, 7);
    expect(display.durationPoints.every((p) => p.hours == 0.0), isTrue);
    expect(display.nights, isEmpty);
    expect(display.totalHours, 0.0);
    expect(display.averageHours, 0.0);
    expect(display.longestHours, 0.0);
    expect(display.periodSessions, isEmpty);
    expect(display.stageShares, isEmpty);
    expect(display.overviewSummary.sleepScore, isNull);
    expect(display.overviewSummary.schedule, isNull);
    // No night, no reading: the sleep-target card and the HRV card self-hide.
    expect(display.targetInterpretation, isNull);
    expect(display.hrvInsight, isNull);
    expect(display.dayTimeRangeText, isNull);
    // A week with no bedtimes has nothing to put on a clock axis.
    expect(display.useScheduleChart, isFalse);
    expect(display.goalProgress.target, 8.0);
  });

  test('only the nights that recorded sleep count as nights', () {
    final display = _display(
      selectedDate: wednesday,
      result: SleepPeriodLoadResult(sessions: [
        _night(tuesday, hours: 6),
        _night(wednesday, hours: 8),
      ]),
    );

    expect(display.durationPoints.length, 7);
    expect(display.nights.length, 2);
    expect(display.totalHours, 14.0);
    expect(display.averageHours, 7.0);
    expect(display.longestHours, 8.0);
    // The bar series is one value per night in the period, zeroes included.
    expect(display.chartValues.length, 7);
  });

  test('the stage shares split the recorded stage time, and only it', () {
    final display = _display(
      selectedDate: wednesday,
      selectedRange: TimeRange.day,
      result: SleepPeriodLoadResult(sessions: [_night(wednesday, hours: 8)]),
    );

    // Half light, half deep — and awake/REM, never recorded, get no row.
    expect(display.stageShares.length, 2);
    expect(display.stageShares.map((s) => s.stageType),
        containsAll([SleepStage.stageLight, SleepStage.stageDeep]));
    expect(display.stageShares.every((s) => s.percent == 50), isTrue);
    expect(display.stageShares.every((s) => s.fraction == 0.5), isTrue);
  });

  test('the entry lists come out newest night first', () {
    final display = _display(
      selectedDate: wednesday,
      result: SleepPeriodLoadResult(sessions: [
        _night(tuesday, hours: 6, id: 'older'),
        _night(wednesday, hours: 8, id: 'newer'),
      ]),
    );

    // The merged night per date, newest first.
    expect(display.periodNights.first.id, 'newer');
    expect(display.periodNights.last.id, 'older');
    // ...while the raw period list stays in date order, as the schedule and
    // confidence readings expect it.
    expect(display.periodSessions.first.id, 'older');
  });

  test('a week whose nights know their bedtimes gets the schedule chart', () {
    final display = _display(
      selectedDate: wednesday,
      result: SleepPeriodLoadResult(sessions: [_night(wednesday, hours: 8)]),
    );

    expect(display.useScheduleChart, isTrue);
    expect(display.scheduleDays.length, 7);
    expect(
      display.scheduleDays.where((d) => d.inBedStart != null).length,
      1,
    );

    // ...but the DAY view never does: it draws the night's own hypnogram.
    final day = _display(
      selectedDate: wednesday,
      selectedRange: TimeRange.day,
      result: SleepPeriodLoadResult(sessions: [_night(wednesday, hours: 8)]),
    );
    expect(day.isDay, isTrue);
    expect(day.useScheduleChart, isFalse);
    expect(day.dailySummary, isNotNull);
    expect(day.dayTimeRangeText, '23:00 - 07:00');
    // One session in the day: the timeline card can open it.
    expect(day.openableDailySessionId, isNotNull);
  });

  test('a night short of the goal reads as below target', () {
    final display = _display(
      selectedDate: wednesday,
      dailyGoalHours: 8.0,
      result: SleepPeriodLoadResult(sessions: [_night(wednesday, hours: 5)]),
    );

    expect(display.targetInterpretation, isNotNull);
    expect(display.targetInterpretation!.targetHours, 8.0);
    expect(display.targetInterpretation!.averageHours, 5.0);
    expect(display.goalProgress.goalMetDays, 0);
  });

  test('sleep and HRV only correlate once enough nights pair up', () {
    final oneNight = _display(
      selectedDate: wednesday,
      result: SleepPeriodLoadResult(
        sessions: [_night(wednesday, hours: 8)],
        crossDailyHrv: [DailyHrv(date: wednesday, rmssdMs: 45)],
      ),
    );
    expect(oneNight.hrvInsight, isNull);

    final wholeWeek = _display(
      selectedDate: wednesday,
      result: SleepPeriodLoadResult(
        sessions: [
          for (var i = 0; i < 5; i++)
            _night(wednesday.minusDays(i), hours: 6 + i.toDouble(), id: 'n$i'),
        ],
        crossDailyHrv: [
          for (var i = 0; i < 5; i++)
            DailyHrv(date: wednesday.minusDays(i), rmssdMs: 40.0 + i * 3),
        ],
      ),
    );
    expect(wholeWeek.hrvInsight, isNotNull);
  });

  group('manual-entry confidence', () {
    // Health Connect's RECORDING_METHOD_MANUAL_ENTRY is 3. This file used to
    // compare against 1, which is ACTIVELY_RECORDED — so a night your watch
    // recorded was reported to you as one you had typed in by hand, and a night
    // you really did type in was never counted at all.
    test('an actively-recorded night is not a manual entry', () {
      final display = _display(
        selectedDate: wednesday,
        result: SleepPeriodLoadResult(
          sessions: [
            _night(wednesday,
                recordingMethod: RecordingMethod.activelyRecorded),
          ],
        ),
      );

      expect(display.dataConfidence.manualEntryCount, 0);
    });

    test('a hand-typed night is', () {
      final display = _display(
        selectedDate: wednesday,
        result: SleepPeriodLoadResult(
          sessions: [
            _night(wednesday, recordingMethod: RecordingMethod.manualEntry),
          ],
        ),
      );

      expect(display.dataConfidence.manualEntryCount, 1);
    });
  });
}