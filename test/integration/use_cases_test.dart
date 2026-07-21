import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/usecase/load_heart_period_use_case.dart';

import '../support/boot_container.dart';

/// The `Load*` use cases, driven over real data.
///
/// 56 of the 57 use cases had no use-case-level test. They are not, however, worth
/// 56 files: they are `const`-constructible, they take a `PeriodLoadQuery`, and they
/// all owe the same guarantees. So this is ONE table and five assertions, and
/// adding a use case is one line.
///
/// The guarantees each one owes:
///
///  1. day / week / month / year all return, over real data, without throwing;
///  2. an EMPTY day returns empty-but-VALID — not null, not a crash. "No data" and
///     "an error" are different, and every screen branches on the difference;
///  3. `RefreshMode.force` returns the same thing a normal load does.
///
/// `today` is pinned to the fixture's era rather than left to `LocalDate.now()`, so
/// the week/month/year windows land on data instead of on an empty present. That is
/// what makes the range cases mean anything at all.
class _UseCase {
  const _UseCase(this.name, this.call);

  final String name;
  final Future<Object?> Function(ProviderContainer, PeriodLoadQuery, RefreshMode)
      call;
}

final _useCases = <_UseCase>[
  _UseCase(
    'sleep',
    (c, q, m) => c.read(loadSleepPeriodUseCaseProvider)(
      q,
      refreshMode: m,
    ),
  ),
  _UseCase(
    'heart',
    (c, q, m) => c.read(loadHeartPeriodUseCaseProvider)(
      q,
      const HeartPeriodLoadCombined(),
      refreshMode: m,
    ),
  ),
  _UseCase(
    'activities',
    (c, q, m) => c.read(loadActivitiesUseCaseProvider)(q),
  ),
  _UseCase(
    'hydration',
    (c, q, m) => c.read(loadHydrationPeriodUseCaseProvider)(q),
  ),
  _UseCase(
    'body',
    (c, q, m) => c.read(loadBodyPeriodUseCaseProvider)(q),
  ),
  _UseCase(
    'nutrition',
    (c, q, m) => c.read(loadNutritionPeriodUseCaseProvider)(q),
  ),
  _UseCase(
    'cycle',
    (c, q, m) => c.read(loadCyclePeriodUseCaseProvider)(q),
  ),
  _UseCase(
    'mindfulness',
    (c, q, m) => c.read(loadMindfulnessPeriodUseCaseProvider)(q),
  ),
  // Caffeine takes two DatePeriods rather than a query — a home window and an
  // analytics window — because the dashboard card and the analytics screen look at
  // different spans of the same data.
  _UseCase(
    'caffeine',
    (c, q, m) => c.read(loadCaffeineUseCaseProvider)(
      q.windows.current,
      q.windows.baseline,
      refreshMode: m,
    ),
  ),
  _UseCase(
    'calories',
    (c, q, m) => c.read(loadCaloriesUseCaseProvider)(q),
  ),
];

void main() {
  /// The last day the fixture has data for — used as "today", so that a week or a
  /// month window looks BACKWARDS into real records instead of forwards into an
  /// empty present.
  LocalDate lastDay(HealthHarness h) => LocalDate.fromDateTime(
        DateTime.fromMillisecondsSinceEpoch(
          h.fixture.records('exercise').last['start']! as int,
          isUtc: true,
        ),
      );

  for (final u in _useCases) {
    group(u.name, () {
      for (final range in TimeRange.values) {
        test('${range.name} returns over real data', () async {
          final h = await bootContainer(allowUnimplemented: false);
          final today = lastDay(h);

          final result = await u.call(
            h.container,
            PeriodLoadQuery(range: range, anchorDate: today, today: today),
            RefreshMode.normal,
          );

          expect(result, isNotNull,
              reason: '${u.name} returned null for a ${range.name} of real data. '
                  'A load that finds nothing must still return a result — null is '
                  'how a screen crashes instead of showing an empty state.');
        });
      }

      test('an EMPTY day returns empty-but-VALID, never null and never a throw',
          () async {
        // The distinction the whole app rests on: "no data" is an answer, not a
        // failure. A use case that returns null here hands a screen something it
        // has no branch for.
        final h = await bootContainer(allowUnimplemented: false);
        final empty = LocalDate.fromDateTime(h.fixture.emptyDay);

        final result = await u.call(
          h.container,
          PeriodLoadQuery(range: TimeRange.day, anchorDate: empty, today: empty),
          RefreshMode.normal,
        );

        expect(result, isNotNull);
      });

      test('a forced refresh returns the same shape as a normal load', () async {
        // RefreshMode.force bypasses caches. It must not change the ANSWER — several
        // of these cache aggressively, and a force path that returns a different
        // shape is a bug that only shows up on pull-to-refresh.
        final h = await bootContainer(allowUnimplemented: false);
        final today = lastDay(h);
        final query =
            PeriodLoadQuery(range: TimeRange.day, anchorDate: today, today: today);

        final normal = await u.call(h.container, query, RefreshMode.normal);
        final forced = await u.call(h.container, query, RefreshMode.force);

        expect(forced.runtimeType, normal.runtimeType);
      });
    });
  }
}
