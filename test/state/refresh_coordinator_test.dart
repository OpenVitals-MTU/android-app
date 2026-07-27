import 'package:clock/clock.dart';
import 'package:fake_async/fake_async.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/refresh/data_domain.dart';
import 'package:openvitals/state/refresh_coordinator.dart';

/// Collects every signal the coordinator publishes, so a test can assert on how
/// MANY were emitted, not only on the final state — coalescing is the point.
List<RefreshSignal> _recordInto(ProviderContainer container) {
  final seen = <RefreshSignal>[];
  container.listen<RefreshSignal>(
    refreshCoordinatorProvider,
    (previous, next) => seen.add(next),
  );
  return seen;
}

ProviderContainer _container() {
  final container = ProviderContainer();
  addTearDown(container.dispose);
  return container;
}

void main() {
  group('expandDomains', () {
    test('a hydration write also marks nutrition, caffeine and calories stale',
        () {
      // The hydration repository stores a paired nutrition record and caffeine
      // rides on it, so the drink the user just logged has to reach three
      // screens the entry form has never heard of.
      expect(
        expandDomains({DataDomain.hydration}),
        containsAll(<DataDomain>{
          DataDomain.hydration,
          DataDomain.nutrition,
          DataDomain.caffeine,
          DataDomain.calories,
        }),
      );
    });

    test('an activity write reaches calories, steps, body energy and '
        'achievements', () {
      expect(
        expandDomains({DataDomain.activities}),
        containsAll(<DataDomain>{
          DataDomain.calories,
          DataDomain.steps,
          DataDomain.bodyEnergy,
          DataDomain.achievements,
        }),
      );
    });

    test('a domain with no derived entries expands to just itself', () {
      expect(expandDomains({DataDomain.cycle}), <DataDomain>{DataDomain.cycle});
    });

    test('an empty set expands to nothing', () {
      expect(expandDomains(const <DataDomain>{}), isEmpty);
    });
  });

  group('RefreshSignal.touches', () {
    test('the initial state is not a signal', () {
      // A screen subscribing at startup must not fire before anything happened:
      // the cold-start load is the view-model's own job.
      expect(const RefreshSignal().touches({DataDomain.sleep}), isFalse);
    });

    test('a signal for an unrelated domain does not touch a screen', () {
      const signal = RefreshSignal(revision: 1, domains: {DataDomain.cycle});
      expect(signal.touches({DataDomain.sleep}), isFalse);
    });

    test('a signal overlapping one of several interests touches the screen', () {
      const signal = RefreshSignal(revision: 1, domains: {DataDomain.calories});
      expect(
        signal.touches({DataDomain.steps, DataDomain.calories}),
        isTrue,
      );
    });
  });

  group('RefreshCoordinator', () {
    test('an app-open signal is emitted immediately and names every domain', () {
      final container = _container();
      final seen = _recordInto(container);

      container.read(refreshCoordinatorProvider.notifier).appOpened();

      expect(seen, hasLength(1));
      expect(seen.single.reason, RefreshReason.appOpen);
      expect(seen.single.domains, DataDomain.values.toSet());
      expect(seen.single.revision, 1);
    });

    test('fifty writes inside the debounce window produce exactly one signal',
        () {
      fakeAsync((async) {
        final container = ProviderContainer();
        final seen = _recordInto(container);
        final coordinator = container.read(refreshCoordinatorProvider.notifier);

        for (var i = 0; i < 50; i++) {
          coordinator.changed({DataDomain.hydration});
          async.elapse(const Duration(milliseconds: 5));
        }
        expect(seen, isEmpty, reason: 'nothing may emit mid-burst');

        async.elapse(kDataChangeDebounce);

        expect(seen, hasLength(1));
        expect(seen.single.reason, RefreshReason.dataChanged);
        expect(seen.single.domains, contains(DataDomain.nutrition));
        container.dispose();
      });
    });

    test('a write burst longer than the max wait flushes without waiting for '
        'it to end', () {
      fakeAsync((async) {
        final container = ProviderContainer();
        final seen = _recordInto(container);
        final coordinator = container.read(refreshCoordinatorProvider.notifier);

        // A long import: a write every 100ms for well past kDataChangeMaxWait,
        // so the quiet window the debounce waits for never arrives.
        for (var i = 0; i < 40; i++) {
          coordinator.changed({DataDomain.activities});
          async.elapse(const Duration(milliseconds: 100));
        }

        expect(seen, isNotEmpty,
            reason: 'a continuous burst must not hold the UI stale until it '
                'finishes');
        container.dispose();
      });
    });

    test('an empty domain set emits nothing', () {
      fakeAsync((async) {
        final container = ProviderContainer();
        final seen = _recordInto(container);

        container
            .read(refreshCoordinatorProvider.notifier)
            .changed(const <DataDomain>{});
        async.elapse(kDataChangeMaxWait * 2);

        expect(seen, isEmpty);
        container.dispose();
      });
    });

    test('consecutive signals for the same domains are distinguishable by '
        'revision', () {
      final container = _container();
      final seen = _recordInto(container);
      final coordinator = container.read(refreshCoordinatorProvider.notifier);

      coordinator.appOpened();
      coordinator.appOpened();

      expect(seen.map((s) => s.revision), <int>[1, 2]);
    });

    test('an app-open signal supersedes a pending write burst', () {
      fakeAsync((async) {
        final container = ProviderContainer();
        final seen = _recordInto(container);
        final coordinator = container.read(refreshCoordinatorProvider.notifier);

        coordinator.changed({DataDomain.body});
        coordinator.appOpened();
        async.elapse(kDataChangeMaxWait * 2);

        expect(seen, hasLength(1),
            reason: 'app open already names every domain, so the parked write '
                'signal has nothing left to say');
        expect(seen.single.reason, RefreshReason.appOpen);
        container.dispose();
      });
    });

    test('the debounce timer does not outlive the container', () {
      fakeAsync((async) {
        final container = ProviderContainer();
        container
            .read(refreshCoordinatorProvider.notifier)
            .changed({DataDomain.sleep});
        container.dispose();

        // A surviving timer would fire into a disposed notifier and throw.
        async.elapse(kDataChangeMaxWait * 2);
      });
    });

    test('the burst clock reads the injected clock, not the wall clock', () {
      // Guards the AGENTS rule that time comes from `clock`: a DateTime.now()
      // here would make the max-wait flush untestable under fakeAsync.
      fakeAsync((async) {
        withClock(Clock(() => DateTime(2025, 6, 25).add(async.elapsed)), () {
          final container = ProviderContainer();
          final seen = _recordInto(container);
          container
              .read(refreshCoordinatorProvider.notifier)
              .changed({DataDomain.vitals});
          async.elapse(kDataChangeDebounce);
          expect(seen, hasLength(1));
          container.dispose();
        });
      });
    });
  });
}
