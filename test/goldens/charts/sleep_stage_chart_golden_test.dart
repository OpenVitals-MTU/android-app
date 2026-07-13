@Tags(['golden'])
library;

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/sleep/presentation/sleep_stage_chart.dart';

import '../../support/golden_harness.dart';

/// [SleepStagesLaneChart] — the hypnogram.
///
/// The single most intricate painter in the app: four lanes, one [Path] over
/// every segment, diagonal connectors wherever two stages touch, and a vertical
/// gradient stretched across the lane centres so a segment's colour comes from
/// WHERE it is rather than from what it is. Nothing about that survives a unit
/// test. A regression here — a connector that stops connecting, a gradient that
/// collapses to one colour — is invisible except in a picture.
void main() {
  final formatter = UnitFormatter(
    unitSystemProvider: () => UnitSystem.metric,
  );

  // A night that straddles midnight, because every night does, and the chart's
  // x axis is the session rather than the day.
  final bedtime = DateTime(2026, 6, 21, 23, 15);
  final wakeUp = DateTime(2026, 6, 22, 7, 5);

  SleepStage stage(int type, int startMinute, int endMinute) => SleepStage(
        startTime: bedtime.add(Duration(minutes: startMinute)),
        endTime: bedtime.add(Duration(minutes: endMinute)),
        stageType: type,
      );

  // A plausible architecture: deep early, REM lengthening toward morning, a brief
  // wake before dawn. The stages are CONTIGUOUS on purpose — the diagonal
  // connectors are only drawn where one stage ends exactly where the next begins,
  // so a fixture with gaps in it would photograph a chart with no connectors and
  // quietly stop testing them.
  final night = <SleepStage>[
    stage(SleepStage.stageAwake, 0, 10),
    stage(SleepStage.stageLight, 10, 55),
    stage(SleepStage.stageDeep, 55, 110),
    stage(SleepStage.stageLight, 110, 145),
    stage(SleepStage.stageRem, 145, 180),
    stage(SleepStage.stageLight, 180, 225),
    stage(SleepStage.stageDeep, 225, 275),
    stage(SleepStage.stageLight, 275, 315),
    stage(SleepStage.stageRem, 315, 355),
    stage(SleepStage.stageAwake, 355, 363),
    stage(SleepStage.stageLight, 363, 410),
    stage(SleepStage.stageRem, 410, 450),
    stage(SleepStage.stageLight, 450, 470),
  ];

  testWidgets('a night, with the lane totals the detail screen shows',
      (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => SleepStagesLaneChart(
        stages: night,
        formatter: formatter,
        timelineStart: bedtime,
        timelineEnd: wakeUp,
      ),
      name: 'sleep_stage_chart_night',
    );
  });

  testWidgets('the same night on the day card, labels without totals',
      (tester) async {
    // The day card lists the same totals underneath, so repeating them in the
    // lane labels would say everything twice.
    await expectChartGoldenBothThemes(
      tester,
      () => SleepStagesLaneChart(
        stages: night,
        formatter: formatter,
        timelineStart: bedtime,
        timelineEnd: wakeUp,
        showInlineLabels: false,
      ),
      name: 'sleep_stage_chart_no_totals',
    );
  });

  testWidgets('a device that only says "asleep"', (tester) async {
    // The cheap tracker: no stage detail at all, just SLEEPING and AWAKE. It
    // still gets four lanes — the standard set is fixed — and everything it
    // recorded lands in the Light lane, which is where SLEEPING is grouped. Two
    // empty lanes is the honest picture, and it is a picture worth having.
    await expectChartGoldenBothThemes(
      tester,
      () => SleepStagesLaneChart(
        stages: [
          stage(SleepStage.stageAwake, 0, 12),
          stage(SleepStage.stageSleeping, 12, 355),
          stage(SleepStage.stageAwake, 355, 363),
          stage(SleepStage.stageSleeping, 363, 470),
        ],
        formatter: formatter,
        timelineStart: bedtime,
        timelineEnd: wakeUp,
      ),
      name: 'sleep_stage_chart_sleeping_only',
    );
  });
}
