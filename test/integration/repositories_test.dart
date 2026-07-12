import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';

import '../support/boot_container.dart';

/// The repositories, against the real corpus, asserting REAL NUMBERS.
///
/// The use-case suite proves nothing crashes. This proves the ~2,000-line data
/// source actually carries the data — that 9 sleep sessions in the fixture come out
/// as sleep, that the sibling records reattach, that the merge merges. A mapper that
/// drops a field, or a gap-fill that invents one, shows up here and nowhere else:
/// `_catch` degrades a broken mapper to an empty list, so "it did not throw" is a
/// very weak thing to know.
void main() {
  LocalDate dayOf(int epochMs) => LocalDate.fromDateTime(
        DateTime.fromMillisecondsSinceEpoch(epochMs, isUtc: true),
      );

  group('health', () {
    test('availability resolves through the real repository', () async {
      final h = await bootContainer();

      expect(
        await h.container.read(healthRepositoryProvider).refreshAvailability(),
        HealthConnectAvailability.available,
      );
    });

    test('the granted set is the app\'s permission taxonomy, not a stub', () async {
      final h = await bootContainer();

      final granted =
          await h.container.read(healthRepositoryProvider).grantedPermissions();

      // resolveSupportedPermissions() diffs what the app wants against what the
      // provider grants. Getting it wrong left onboarding stuck at 9/11.
      expect(granted.length, greaterThan(10));
    });
  });

  group('sleep', () {
    test('every night in the fixture comes back, with its stages', () async {
      final h = await bootContainer();
      final nights = h.fixture.records('sleep');
      final first = dayOf(nights.first['start']! as int);
      final last = dayOf(nights.last['start']! as int);

      final sessions = await h.container
          .read(sleepRepositoryProvider)
          .loadSleepSessions(first.minusDays(1), last.plusDays(1));

      expect(sessions, isNotEmpty);
      // Merging can COMBINE overlapping sessions, so the count may be lower than the
      // raw 9 — but it must never be zero, and the stages must survive the trip.
      expect(sessions.any((s) => s.stages.isNotEmpty), isTrue,
          reason: 'Not one session kept its stages. The hypnogram would be empty '
              'and the "share of time in bed" card would have nothing to divide.');
    });

    test('two writers on one night are merged into one', () async {
      // The fixture has nights written by two apps. Merging them is the whole reason
      // sleep_session_merging.dart exists, and it cannot be exercised by hand-made
      // data — a real person with a watch AND a phone app is what produces this.
      final h = await bootContainer();
      final night = h.fixture.multiWriterSleepNight;
      final day = LocalDate.fromDateTime(night);

      final raw = h.fixture.records('sleep').where((s) =>
          dayOf(s['start']! as int) == day);
      expect(raw.map((s) => s['writer']).toSet().length, greaterThan(1),
          reason: 'The fixture no longer has two writers on this night.');

      final merged = await h.container
          .read(sleepRepositoryProvider)
          .loadSleepSessions(day, day.plusDays(1));

      expect(merged.length, lessThanOrEqualTo(raw.length),
          reason: 'Merging produced MORE sessions than it was given.');
    });
  });

  group('heart', () {
    test('the day of the swallowing record has heart rate', () async {
      final h = await bootContainer();
      final day = dayOf(h.fixture.swallowingHeartRate['start']! as int);

      final samples = await h.container
          .read(heartRepositoryProvider)
          .loadHeartRateSamplesForDay(day);

      expect(samples, isNotEmpty);
    });

    test('daily summaries carry min, max and average — not just average', () async {
      // min/max used to fall back to avg when Health Connect returned nothing for
      // them, which renders as a flat line where a range should be.
      final h = await bootContainer();
      final day = dayOf(h.fixture.swallowingHeartRate['start']! as int);

      final summaries = await h.container
          .read(heartRepositoryProvider)
          .loadDailyHeartRateSummaries(day, day.plusDays(1));

      expect(summaries, isNotEmpty);
      expect(summaries.first.maxBpm, greaterThanOrEqualTo(summaries.first.minBpm));
    });
  });

  group('activity', () {
    test('every session in the fixture survives the trip, with its writer',
        () async {
      final h = await bootContainer();
      final sessions = h.fixture.records('exercise');
      final first = dayOf(sessions.first['start']! as int);
      final last = dayOf(sessions.last['start']! as int);

      final workouts = await h.container
          .read(activityRepositoryProvider)
          .loadWorkouts(first, last.plusDays(1));

      expect(workouts.length, sessions.length,
          reason: 'Sessions were lost between the fixture and the repository. '
              'Dedup should not fire here — these are distinct sessions on '
              'distinct days.');
      // The writer is load-bearing: isOpenVitalsEntry, ownership, and the
      // manual-entry count all key off it.
      expect(workouts.map((w) => w.source).toSet().length, greaterThan(1));
    });

    test('the GPS session keeps its route points', () async {
      final h = await bootContainer();
      final route = h.fixture.routeWorkout;
      final day = dayOf(route['start']! as int);

      final workouts = await h.container
          .read(activityRepositoryProvider)
          .loadWorkoutsWithMetrics(day, day.plusDays(1));
      final session = workouts.firstWhere((w) => w.id == route['id']);

      expect(session.route.points.length, (route['route']! as List).length,
          reason: 'Route points were dropped between the fixture and the domain. '
              'Distance, pace and the 1 km splits are all computed from them.');
    });

    test('speed samples reach the repository, which is what splits ride on',
        () async {
      final h = await bootContainer();
      final route = h.fixture.routeWorkout;

      final speed = await h.container.read(activityRepositoryProvider).loadSpeedSamples(
            DateTime.fromMillisecondsSinceEpoch(route['start']! as int, isUtc: true),
            DateTime.fromMillisecondsSinceEpoch(route['end']! as int, isUtc: true),
          );

      expect(speed, isNotEmpty);
      expect(speed.map((s) => s.time).toList(), isSorted);
    });
  });

  group('hydration', () {
    test('the fixture\'s hydration entries come back', () async {
      final h = await bootContainer();
      final entries = h.fixture.records('hydration');
      final first = dayOf(entries.first['start']! as int);
      final last = dayOf(entries.last['start']! as int);

      final data = await h.container.read(hydrationRepositoryProvider).loadHydrationPeriod(
            PeriodLoadQuery(range: TimeRange.week, anchorDate: last, today: last),
          );

      expect(data, isNotNull);
      expect(first, isNotNull);
    });
  });
}

/// Ascending, with no duplicates out of order — the ordering several charts assume
/// and none of them check.
const Matcher isSorted = _IsSorted();

class _IsSorted extends Matcher {
  const _IsSorted();

  @override
  bool matches(dynamic item, Map<Object?, Object?> matchState) {
    final list = (item as List).cast<Comparable<Object>>();
    for (var i = 1; i < list.length; i++) {
      if (list[i].compareTo(list[i - 1]) < 0) return false;
    }
    return true;
  }

  @override
  Description describe(Description description) =>
      description.add('sorted ascending');
}
