import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/model/sleep_models.dart';

/// [sleepSessionHasReliableStages] — telling a fully-staged night from one a
/// device (or an in-progress sync) only staged part of, so a near-empty hypnogram
/// is replaced by a note instead of drawn.
void main() {
  final start = DateTime(2026, 7, 18, 0, 20);
  final end = DateTime(2026, 7, 18, 8, 14); // a 7h54m night

  SleepData session(List<SleepStage> stages) => SleepData(
        id: 's',
        startTime: start,
        endTime: end,
        durationMs: end.difference(start).inMilliseconds,
        source: 'test',
        stages: stages,
      );

  SleepStage stage(int type, int fromMin, int toMin) => SleepStage(
        startTime: start.add(Duration(minutes: fromMin)),
        endTime: start.add(Duration(minutes: toMin)),
        stageType: type,
      );

  test('a fully-staged night is reliable', () {
    // Stages wall-to-wall across the 474-minute span.
    final full = session([
      stage(SleepStage.stageLight, 0, 120),
      stage(SleepStage.stageDeep, 120, 240),
      stage(SleepStage.stageRem, 240, 360),
      stage(SleepStage.stageLight, 360, 474),
    ]);
    expect(sleepSessionHasReliableStages(full), isTrue);
  });

  test('a tail-only session is not reliable', () {
    // The real symptom: an 8h span with stages only in the last ~90 minutes,
    // which draws as an empty chart with a fragment at the right edge.
    final partial = session([
      stage(SleepStage.stageLight, 384, 466), // 06:44–08:06
    ]);
    expect(sleepSessionHasReliableStages(partial), isFalse);
  });

  test('a session with no stages is not reliable', () {
    expect(sleepSessionHasReliableStages(session(const [])), isFalse);
  });

  test('coverage is measured against the span, not the stages own extent', () {
    // Stages that together cover just over half the night pass; just under, fail.
    final justOver = session([stage(SleepStage.stageLight, 0, 240)]); // ~51%
    final justUnder = session([stage(SleepStage.stageLight, 0, 230)]); // ~49%
    expect(sleepSessionHasReliableStages(justOver), isTrue);
    expect(sleepSessionHasReliableStages(justUnder), isFalse);
  });

  test('a zero-length session never divides by zero', () {
    final instant = SleepData(
      id: 's',
      startTime: start,
      endTime: start,
      durationMs: 0,
      source: 'test',
      stages: [stage(SleepStage.stageLight, 0, 0)],
    );
    expect(sleepSessionHasReliableStages(instant), isFalse);
  });
}
