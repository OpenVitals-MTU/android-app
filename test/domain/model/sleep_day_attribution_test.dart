import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/sleep_daily_summary.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/domain/preferences/sleep_range_mode.dart';

/// Sleep is attributed to the WAKE-UP date, not the bedtime date: a night begun
/// at 22:40 that ends at 06:17 the next morning belongs to that morning. This
/// is the (deliberate) Garmin/Fitbit convention, and differs from Health
/// Connect, which files the same night under the bedtime date. Regression guard
/// for a "my sleep is on the wrong day" report where both were, in fact,
/// consistent — the two apps just label cross-midnight nights differently.
void main() {
  SleepData session(String id, DateTime start, DateTime end) => SleepData(
        id: id,
        startTime: start,
        endTime: end,
        durationMs: end.difference(start).inMilliseconds,
        source: 'test',
        stages: [
          SleepStage(startTime: start, endTime: end, stageType: SleepStage.stageLight),
        ],
      );

  // Bed 22:40 on the 16th, up 06:17 on the 17th — a cross-midnight night.
  final crossMidnight =
      session('x', DateTime(2026, 7, 16, 22, 40), DateTime(2026, 7, 17, 6, 17));
  // Bed 01:38 on the 16th, up 07:32 on the 16th — begins and ends the same day.
  final sameDay =
      session('s', DateTime(2026, 7, 16, 1, 38), DateTime(2026, 7, 16, 7, 32));

  List<SleepData> forDay(int day) => sleepSessionsForRange(
        [crossMidnight, sameDay],
        LocalDate(2026, 7, day),
        SleepRangeMode.evening18h,
      );

  test('a cross-midnight night is filed under the wake-up date', () {
    expect(forDay(16).map((s) => s.id), ['s'], reason: 'ends on the 16th');
    expect(forDay(17).map((s) => s.id), ['x'], reason: 'ends on the 17th');
  });

  test('the 17th is not empty just because Health Connect labels it the 16th',
      () {
    final summary = dailySleepSummary([crossMidnight, sameDay], const LocalDate(2026, 7, 17));
    expect(summary, isNotNull);
    expect(summary!.startTime, DateTime(2026, 7, 16, 22, 40));
    expect(summary.endTime, DateTime(2026, 7, 17, 6, 17));
  });
}
