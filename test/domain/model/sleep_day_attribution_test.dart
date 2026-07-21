import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/sleep_daily_summary.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/domain/preferences/sleep_window.dart';

/// A night is classified by when you FELL ASLEEP (its start), against the
/// configurable night window (default 18:00 → 10:00). Start-based keeps a night
/// on the wake-up date (bed 22:40 → next morning) as before, AND keeps a sleep-in
/// past the morning hour with its night instead of misfiling it as a nap.
/// Sessions that begin outside the window are daytime naps, reported apart —
/// never dropped.
void main() {
  SleepData session(String id, DateTime start, DateTime end) => SleepData(
        id: id,
        startTime: start,
        endTime: end,
        durationMs: end.difference(start).inMilliseconds,
        source: 'test',
        stages: [
          SleepStage(
              startTime: start, endTime: end, stageType: SleepStage.stageLight),
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
        SleepWindow.defaultWindow,
      );

  test('a cross-midnight night is filed under the wake-up date', () {
    expect(forDay(16).map((s) => s.id), ['s'], reason: 'begins early the 16th');
    expect(forDay(17).map((s) => s.id), ['x'], reason: 'begins the 16th evening');
  });

  test('a sleep-in past the morning hour stays that night, not a nap', () {
    // Bed 01:00, up 11:00 — wakes an hour after the 10:00 end. End-based
    // attribution would call it a daytime nap; start-based keeps it the night.
    final sleepIn =
        session('in', DateTime(2026, 7, 18, 1, 0), DateTime(2026, 7, 18, 11, 0));
    final night =
        sleepSessionsForRange([sleepIn], const LocalDate(2026, 7, 18), SleepWindow.defaultWindow);
    expect(night.map((s) => s.id), ['in']);
    expect(dailyNaps([sleepIn], const LocalDate(2026, 7, 18)), isEmpty);
  });

  test('a daytime session becomes a nap on its date and is not dropped', () {
    // Begins 14:00 — in the daytime gap [10:00, 18:00), so a nap, not the night.
    final nap =
        session('nap', DateTime(2026, 7, 18, 14, 0), DateTime(2026, 7, 18, 15, 0));
    expect(
        sleepSessionsForRange([nap], const LocalDate(2026, 7, 18), SleepWindow.defaultWindow),
        isEmpty);
    expect(dailyNaps([nap], const LocalDate(2026, 7, 18)).map((s) => s.id), ['nap']);
  });

  test('custom window hours move the night boundary', () {
    // 20:00 → 09:00. A session begun at 09:30 now falls in the daytime gap.
    const window = SleepWindow(startHour: 20, endHour: 9);
    final late =
        session('l', DateTime(2026, 7, 18, 9, 30), DateTime(2026, 7, 18, 10, 30));
    expect(sleepSessionsForRange([late], const LocalDate(2026, 7, 18), window),
        isEmpty);
    expect(dailyNaps([late], const LocalDate(2026, 7, 18), sleepWindow: window)
        .map((s) => s.id), ['l']);
  });
}
