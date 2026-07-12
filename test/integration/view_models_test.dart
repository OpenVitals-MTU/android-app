import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/period/period_selection.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/presentation/screen_error.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/features/activity/application/activities_view_model.dart';
import 'package:openvitals/features/vitals/heart_vitals_overview_screen.dart';
import 'package:openvitals/features/hydration/hydration_view_model.dart';
import 'package:openvitals/features/sleep/sleep_view_model.dart';

import '../support/boot_container.dart';

/// Every period notifier, driven for real, over real data.
///
/// One table, one set of assertions. The ~12 period notifiers are copy-paste
/// siblings — same `load(PeriodSelection)`, same `isLoading`/`error`/`result`
/// shape, same monotonic `_generation` guard against stale results — so testing
/// them one file at a time would be twelve files that each say the same thing and
/// none of which say it about the others.
///
/// The stale-generation case is the one worth having. That guard is copy-pasted
/// twelve times, and a copy-pasted guard is exactly the kind of thing that is
/// subtly wrong in one place and nowhere else.
class _Case {
  const _Case(this.name, this.keepAlive, this.load, this.inspect);

  final String name;
  final void Function(ProviderContainer) keepAlive;
  final Future<void> Function(ProviderContainer, PeriodSelection) load;
  final ({bool isLoading, ScreenError? error}) Function(ProviderContainer) inspect;
}

final _cases = <_Case>[
  _Case(
    'sleep',
    (c) => c.listen(sleepProvider, (_, _) {}),
    (c, s) => c.read(sleepProvider.notifier).load(s),
    (c) {
      final s = c.read(sleepProvider);
      return (isLoading: s.isLoading, error: s.error);
    },
  ),
  _Case(
    'activities',
    (c) => c.listen(activitiesProvider, (_, _) {}),
    (c, s) => c.read(activitiesProvider.notifier).load(s),
    (c) {
      final s = c.read(activitiesProvider);
      return (isLoading: s.isLoading, error: s.error);
    },
  ),
  _Case(
    'hydration',
    (c) => c.listen(hydrationProvider, (_, _) {}),
    (c, s) => c.read(hydrationProvider.notifier).load(s),
    (c) {
      final s = c.read(hydrationProvider);
      return (isLoading: s.isLoading, error: s.error);
    },
  ),
  _Case(
    'heartVitalsOverview',
    (c) => c.listen(heartVitalsOverviewProvider, (_, _) {}),
    (c, s) => c.read(heartVitalsOverviewProvider.notifier).load(s),
    (c) {
      final s = c.read(heartVitalsOverviewProvider);
      return (isLoading: s.isLoading, error: s.error);
    },
  ),
];

void main() {
  for (final c in _cases) {
    group(c.name, () {
      test('loads a day of real data without erroring', () async {
        final h = await bootContainer();
        final day = LocalDate.fromDateTime(
          DateTime.fromMillisecondsSinceEpoch(
            h.fixture.swallowingHeartRate['start']! as int,
            isUtc: true,
          ),
        );

        c.keepAlive(h.container);
        await c.load(h.container, PeriodSelection(TimeRange.day, day));
        await pumpEventQueue();

        final state = c.inspect(h.container);
        expect(state.isLoading, isFalse, reason: '${c.name} never settled.');
        expect(state.error, isNull,
            reason: '${c.name} errored on a day of ordinary data: ${state.error}');
      });

      test('an EMPTY day is empty-but-valid, not an error', () async {
        // "No data" and "an error" are different, and the screens branch on the
        // difference: an empty day must show an empty state, not a failure.
        final h = await bootContainer();

        c.keepAlive(h.container);
        await c.load(
          h.container,
          PeriodSelection(TimeRange.day, LocalDate.fromDateTime(h.fixture.emptyDay)),
        );
        await pumpEventQueue();

        final state = c.inspect(h.container);
        expect(state.isLoading, isFalse);
        expect(state.error, isNull,
            reason: '${c.name} reported an ERROR for a day that simply has no data.');
      });

      test('a stale load does not clobber a newer one', () async {
        // The `_generation` guard, copy-pasted twelve times. Two loads are started
        // back to back; the first must not overwrite the second's result when it
        // eventually returns. Nothing else in the suite covers this, and a
        // copy-pasted guard is exactly what is subtly wrong in one place only.
        final h = await bootContainer();
        final busy = LocalDate.fromDateTime(
          DateTime.fromMillisecondsSinceEpoch(
            h.fixture.swallowingHeartRate['start']! as int,
            isUtc: true,
          ),
        );
        final empty = LocalDate.fromDateTime(h.fixture.emptyDay);

        c.keepAlive(h.container);
        final first = c.load(h.container, PeriodSelection(TimeRange.day, empty));
        final second = c.load(h.container, PeriodSelection(TimeRange.day, busy));
        await Future.wait([first, second]);
        await pumpEventQueue();

        final state = c.inspect(h.container);
        expect(state.isLoading, isFalse,
            reason: '${c.name} is still loading after both loads resolved — the '
                'generation guard let a stale result reset the flag.');
        expect(state.error, isNull);
      });
    });
  }
}
