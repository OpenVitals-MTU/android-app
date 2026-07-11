import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/sleep_score.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/domain/preferences/sleep_range_mode.dart';

SleepData _sleepSession(LocalDate date) {
  final end = date.atTimeInstant(7);
  final start = end.subtract(const Duration(hours: 8));
  final deepEnd = start.add(const Duration(hours: 2));
  final remEnd = deepEnd.add(const Duration(minutes: 90));
  final awakeEnd = remEnd.add(const Duration(minutes: 30));
  final lightEnd = awakeEnd.add(const Duration(hours: 4));
  return SleepData(
    id: 'sleep-$date',
    startTime: start,
    endTime: end,
    durationMs: end.difference(start).inMilliseconds,
    source: 'test',
    stages: [
      SleepStage(
          startTime: start, endTime: deepEnd, stageType: SleepStage.stageDeep),
      SleepStage(
          startTime: deepEnd, endTime: remEnd, stageType: SleepStage.stageRem),
      SleepStage(
          startTime: remEnd,
          endTime: awakeEnd,
          stageType: SleepStage.stageAwake),
      SleepStage(
          startTime: awakeEnd,
          endTime: lightEnd,
          stageType: SleepStage.stageLight),
    ],
  );
}

void main() {
  test('date score uses previous daily sleep summaries as regularity baseline',
      () {
    final today = LocalDate(2026, 6, 8);
    final estimate = calculateSleepScoreForDate(
      today,
      [
        for (var offset = 0; offset <= 3; offset++)
          _sleepSession(today.minusDays(offset)),
      ],
      SleepRangeMode.evening18h,
    );

    expect(estimate.confidence, SleepScoreConfidence.high);
    expect(estimate.regularityDifferenceMinutes ?? -1.0, closeTo(0.0, 0.001));
  });
}
