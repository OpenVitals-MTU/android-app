@Tags(['golden'])
library;

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/features/sleep/application/sleep_display.dart';
import 'package:openvitals/features/sleep/presentation/sleep_schedule_chart.dart';

import '../../support/golden_harness.dart';

/// [SleepScheduleStageChart] — a week of nights on one clock.
///
/// Its whole reason for existing is the 18:00 anchor: a night that starts at
/// 23:40 and ends at 07:10 crosses midnight, and on a plain 00:00-24:00 axis it
/// would be drawn as two bars at opposite ends of the chart. Anchored at six in
/// the evening, it is one bar. Nothing but a picture shows whether that still
/// holds, and the fixture leans on it — every night here straddles midnight, and
/// one of them (Saturday) goes to bed AFTER it, which is the case that wraps.
///
/// This is also the one chart in the app whose label gutter is on the RIGHT, so
/// its x-axis row is padded on the right rather than inset on the left. The
/// golden is the only thing that would catch someone "fixing" that.
void main() {
  // The week ending on the golden day. `date` is the night's DATE — the morning
  // you woke up — which is why every in-bed window starts on the day before.
  const monday = LocalDate(2026, 6, 16);

  SleepStage stage(int type, DateTime start, Duration length) => SleepStage(
        startTime: start,
        endTime: start.add(length),
        stageType: type,
      );

  /// A night with plausible architecture, laid down from its bedtime.
  List<SleepStage> stagesFrom(DateTime start, Duration total) {
    final stages = <SleepStage>[];
    var cursor = start;
    const cycle = <(int, int)>[
      (SleepStage.stageAwake, 8),
      (SleepStage.stageLight, 45),
      (SleepStage.stageDeep, 50),
      (SleepStage.stageLight, 35),
      (SleepStage.stageRem, 40),
    ];
    var index = 0;
    while (cursor.isBefore(start.add(total))) {
      final (type, minutes) = cycle[index % cycle.length];
      final length = Duration(minutes: minutes);
      final end = cursor.add(length);
      stages.add(
        stage(type, cursor, end.isAfter(start.add(total))
            ? start.add(total).difference(cursor)
            : length),
      );
      cursor = end;
      index++;
    }
    return stages;
  }

  SleepScheduleDay night(
    LocalDate date,
    int bedHour,
    int bedMinute,
    Duration length,
  ) {
    // Bedtime belongs to the EVENING BEFORE the night's date, unless it is past
    // midnight already — the Saturday lie-in below.
    final start = bedHour >= 12
        ? DateTime(date.year, date.month, date.day, bedHour, bedMinute)
            .subtract(const Duration(days: 1))
        : DateTime(date.year, date.month, date.day, bedHour, bedMinute);
    return SleepScheduleDay(
      date: date,
      inBedStart: start,
      inBedEnd: start.add(length),
      stages: stagesFrom(start, length),
    );
  }

  final week = <SleepScheduleDay>[
    night(monday, 23, 20, const Duration(hours: 7, minutes: 40)),
    night(monday.plusDays(1), 23, 5, const Duration(hours: 7, minutes: 55)),
    night(monday.plusDays(2), 0, 10, const Duration(hours: 6, minutes: 50)),
    // Thursday: the tracker recorded a window but no stages at all. The chart
    // draws a solid bar rather than an empty slot — "I know when, not what".
    SleepScheduleDay(
      date: monday.plusDays(3),
      inBedStart: DateTime(2026, 6, 18, 23, 45),
      inBedEnd: DateTime(2026, 6, 19, 7, 0),
    ),
    night(monday.plusDays(4), 23, 55, const Duration(hours: 6, minutes: 30)),
    // Saturday: a late night out, in bed at 01:05, up at 09:40. Past the 18:00
    // anchor by more than a day's worth of minutes — this is the wrap case.
    night(monday.plusDays(5), 1, 5, const Duration(hours: 8, minutes: 35)),
    // Sunday: no session at all. A missing night is a gap, not a zero.
    const SleepScheduleDay(
      date: LocalDate(2026, 6, 22),
      inBedStart: null,
      inBedEnd: null,
    ),
  ];

  testWidgets('a week of nights', (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => SleepScheduleStageChart(
        title: 'Sleep schedule',
        summaryText: 'This week · 7h 24m avg',
        days: week,
        selectedRange: TimeRange.week,
      ),
      name: 'sleep_schedule_chart_week',
    );
  });

  testWidgets('a week with the average bedtime and wake-up marked',
      (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => SleepScheduleStageChart(
        title: 'Sleep schedule',
        summaryText: 'This week · 7h 24m avg',
        days: week,
        selectedRange: TimeRange.week,
        // Minutes of the day, not anchored minutes: 23:35 to bed, up at 07:20.
        // The chart anchors them itself, which is the only reason a bedtime of
        // 1415 and a wake-up of 440 can share one axis.
        averageSchedule: const SleepOverviewSchedule(1415, 440),
        selectedDate: monday.plusDays(4),
        onDateSelected: (_) {},
      ),
      name: 'sleep_schedule_chart_markers',
    );
  });
}
