import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/sleep_daily_summary.dart';
import 'package:openvitals/domain/model/sleep_models.dart';

SleepData _s(String id, DateTime start, DateTime end, {int? durationMs}) =>
    SleepData(
      id: id,
      startTime: start,
      endTime: end,
      durationMs: durationMs ?? end.difference(start).inMilliseconds,
      source: 'fitbit',
    );

DateTime _t(int y, int m, int d, int h, int min) => DateTime(y, m, d, h, min);

void main() {
  group('splitNightAndNaps', () {
    test('keeps a night broken by a short early-morning wake together', () {
      // 23:43→05:37 then 07:17→09:33: a 1h40m wake, one night.
      final a = _s('a', _t(2026, 7, 11, 23, 43), _t(2026, 7, 12, 5, 37));
      final b = _s('b', _t(2026, 7, 12, 7, 17), _t(2026, 7, 12, 9, 33));

      final split = splitNightAndNaps([b, a]);

      expect(split.night.map((s) => s.id), ['a', 'b']);
      expect(split.naps, isEmpty);
    });

    test('splits an afternoon nap from the night', () {
      final night = _s('night', _t(2026, 7, 10, 23, 18), _t(2026, 7, 11, 7, 25));
      final nap = _s('nap', _t(2026, 7, 11, 16, 10), _t(2026, 7, 11, 16, 45));

      final split = splitNightAndNaps([nap, night]);

      expect(split.night.map((s) => s.id), ['night']);
      expect(split.naps.map((s) => s.id), ['nap']);
    });

    test('a single session is the night', () {
      final split =
          splitNightAndNaps([_s('x', _t(2026, 7, 11, 23, 0), _t(2026, 7, 12, 6, 0))]);
      expect(split.night.single.id, 'x');
      expect(split.naps, isEmpty);
    });

    test('empty input yields no night and no naps', () {
      final split = splitNightAndNaps([]);
      expect(split.night, isEmpty);
      expect(split.naps, isEmpty);
    });
  });

  group('dailySleepSummary — night only, wall-clock', () {
    test('sums the night segments (wall-clock) and excludes a nap', () {
      final a = _s('a', _t(2026, 7, 11, 23, 43), _t(2026, 7, 12, 5, 37)); // 5h54
      final b = _s('b', _t(2026, 7, 12, 7, 17), _t(2026, 7, 12, 9, 33)); // 2h16
      final nap = _s('nap', _t(2026, 7, 12, 15, 0), _t(2026, 7, 12, 15, 40));

      final summary = dailySleepSummary([a, b, nap], const LocalDate(2026, 7, 12));

      // The bug reported 1h43m here; the night is 5h54 + 2h16 = 8h10.
      expect(summary, isNotNull);
      expect(summary!.durationMs,
          const Duration(hours: 8, minutes: 10).inMilliseconds);
      expect(dailyNaps([a, b, nap], const LocalDate(2026, 7, 12)).map((s) => s.id),
          ['nap']);
    });

    test('duration is wall-clock, not the stored time-asleep durationMs', () {
      // Stored durationMs (1h) differs from the 7h span; the summary uses the span.
      final only = _s('x', _t(2026, 7, 11, 23, 0), _t(2026, 7, 12, 6, 0),
          durationMs: const Duration(hours: 1).inMilliseconds);

      final summary = dailySleepSummary([only], const LocalDate(2026, 7, 12));

      expect(summary!.durationMs, const Duration(hours: 7).inMilliseconds);
    });

    test('overlapping night sessions count shared time once (union, not sum)',
        () {
      // The reported pair, had it slipped past dedup: 1:15-6:40 and 1:16-7:28.
      // Sum would be 11h37m; the union (1:15-7:28) is 6h13m.
      final a = _s('a', _t(2026, 7, 14, 1, 15), _t(2026, 7, 14, 6, 40));
      final b = _s('b', _t(2026, 7, 14, 1, 16), _t(2026, 7, 14, 7, 28));

      final summary = dailySleepSummary([a, b], const LocalDate(2026, 7, 14));

      expect(summary!.durationMs,
          const Duration(hours: 6, minutes: 13).inMilliseconds);
    });
  });

  group('sleepSessionsUnionMs', () {
    test('overlapping intervals count their shared time once', () {
      final a = _s('a', _t(2026, 7, 14, 1, 15), _t(2026, 7, 14, 6, 40));
      final b = _s('b', _t(2026, 7, 14, 1, 16), _t(2026, 7, 14, 7, 28));
      expect(sleepSessionsUnionMs([a, b]),
          const Duration(hours: 6, minutes: 13).inMilliseconds);
    });

    test('disjoint intervals equal the sum of their spans', () {
      final a = _s('a', _t(2026, 7, 14, 1, 0), _t(2026, 7, 14, 3, 0)); // 2h
      final b = _s('b', _t(2026, 7, 14, 5, 0), _t(2026, 7, 14, 6, 30)); // 1h30
      expect(sleepSessionsUnionMs([a, b]),
          const Duration(hours: 3, minutes: 30).inMilliseconds);
    });

    test('adjacent (touching) intervals merge without a gap', () {
      final a = _s('a', _t(2026, 7, 14, 1, 0), _t(2026, 7, 14, 3, 0));
      final b = _s('b', _t(2026, 7, 14, 3, 0), _t(2026, 7, 14, 4, 0));
      expect(sleepSessionsUnionMs([a, b]), const Duration(hours: 3).inMilliseconds);
    });

    test('a fully-contained interval adds nothing', () {
      final outer = _s('outer', _t(2026, 7, 14, 1, 0), _t(2026, 7, 14, 9, 0));
      final inner = _s('inner', _t(2026, 7, 14, 3, 0), _t(2026, 7, 14, 4, 0));
      expect(sleepSessionsUnionMs([outer, inner]),
          const Duration(hours: 8).inMilliseconds);
    });

    test('empty input is zero', () {
      expect(sleepSessionsUnionMs(const []), 0);
    });
  });
}
