import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/features/sleep/application/sleep_display.dart';

SleepStage _stage(int type, DateTime start, DateTime end) =>
    SleepStage(startTime: start, endTime: end, stageType: type);

void main() {
  // A night in bed 22:00 -> 06:30 (8.5h), of which 1.5h is awake within the
  // session, so 7.0h asleep.
  final night = SleepData(
    id: 'n1',
    startTime: DateTime(2026, 6, 1, 22),
    endTime: DateTime(2026, 6, 2, 6, 30),
    durationMs: const Duration(hours: 8, minutes: 30).inMilliseconds,
    source: 'test',
    stages: [
      _stage(SleepStage.stageAwake, DateTime(2026, 6, 1, 22), DateTime(2026, 6, 1, 23)),
      _stage(SleepStage.stageDeep, DateTime(2026, 6, 1, 23), DateTime(2026, 6, 2, 2)),
      _stage(SleepStage.stageLight, DateTime(2026, 6, 2, 2), DateTime(2026, 6, 2, 6)),
      _stage(SleepStage.stageAwake, DateTime(2026, 6, 2, 6), DateTime(2026, 6, 2, 6, 30)),
    ],
  );

  const date = LocalDate(2026, 6, 2);

  test('excludes awake time within the session from the sleep duration', () {
    // 7h asleep, not the 8.5h spent in bed.
    expect(nightAsleepHours([night], date), closeTo(7.0, 1e-9));
  });

  test('falls back to time in bed when the night has no stage data', () {
    // Nothing to subtract awake from, so the whole session counts.
    expect(
      nightAsleepHours([night.copyWith(stages: const [])], date),
      closeTo(8.5, 1e-9),
    );
  });

  test('is zero when there is no night for the date', () {
    expect(nightAsleepHours(const [], date), 0.0);
  });
}
