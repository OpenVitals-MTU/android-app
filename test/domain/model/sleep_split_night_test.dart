import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/sleep_daily_summary.dart';
import 'package:openvitals/domain/model/sleep_models.dart';

/// A night split by a wake between 60 min and 3 h (the screenshot's 01:22–05:18
/// | 07:34–09:38) is one night whose segments must combine into a continuous
/// stage timeline — the gap filled as Awake — or the day view hides the
/// hypnogram ("only partly staged") and the week/month bar shows a hole.
void main() {
  final segment1Start = DateTime(2026, 7, 19, 1, 22);
  final segment1End = DateTime(2026, 7, 19, 5, 18);
  final segment2Start = DateTime(2026, 7, 19, 7, 34); // 2h16 wake gap
  final segment2End = DateTime(2026, 7, 19, 9, 38);

  SleepStage stage(int type, DateTime from, DateTime to) =>
      SleepStage(startTime: from, endTime: to, stageType: type);

  SleepData segment(String id, DateTime start, DateTime end,
          List<SleepStage> stages) =>
      SleepData(
        id: id,
        startTime: start,
        endTime: end,
        durationMs: end.difference(start).inMilliseconds,
        source: 'nodomain.freeyourgadget',
        stages: stages,
      );

  final segment1 = segment('s1', segment1Start, segment1End, [
    stage(SleepStage.stageLight, segment1Start,
        segment1Start.add(const Duration(hours: 2, minutes: 58))),
    stage(SleepStage.stageDeep,
        segment1Start.add(const Duration(hours: 2, minutes: 58)), segment1End),
  ]);
  final segment2 = segment('s2', segment2Start, segment2End, [
    stage(SleepStage.stageLight, segment2Start, segment2End),
  ]);

  test('combineNightStages fills the wake gap with Awake', () {
    final stages =
        combineNightStages([segment1, segment2], maxGap: kSleepNapGap);

    final awake =
        stages.where((s) => s.stageType == SleepStage.stageAwake).toList();
    expect(awake, hasLength(1));
    expect(awake.single.startTime, segment1End);
    expect(awake.single.endTime, segment2Start);
    // The combined stages now span the whole night, gap included.
    expect(stages.first.startTime, segment1Start);
    expect(stages.last.endTime, segment2End);
  });

  test('a gap larger than maxGap (a daytime nap) is not bridged', () {
    final nap = segment('nap', DateTime(2026, 7, 19, 14, 0),
        DateTime(2026, 7, 19, 14, 40), [
      stage(SleepStage.stageLight, DateTime(2026, 7, 19, 14, 0),
          DateTime(2026, 7, 19, 14, 40)),
    ]);
    final stages =
        combineNightStages([segment1, nap], maxGap: kSleepNapGap);
    // Only the night's own stages — no Awake spanning the >3h gap to the nap.
    expect(stages.any((s) => s.stageType == SleepStage.stageAwake), isFalse);
  });

  test('the split night is reliable once its gap is filled', () {
    // Before the fix the combined night spanned the gap without a stage there,
    // so coverage fell below 0.5 and the hypnogram was hidden.
    final summary = dailySleepSummary(
      [segment1, segment2],
      const LocalDate(2026, 7, 19),
    )!;
    expect(sleepSessionHasReliableStages(summary), isTrue,
        reason: 'gap-filled stages cover the span');
    expect(
      summary.stages.any((s) => s.stageType == SleepStage.stageAwake),
      isTrue,
    );
  });
}
