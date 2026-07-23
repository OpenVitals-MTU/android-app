import 'package:clock/clock.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/features/dashboard/application/dashboard_view_model.dart';

import '../../support/boot_container.dart';

/// Resume/pin semantics, driven over the REAL graph — the notifier, the real
/// `LoadDashboardDayUseCase`, the repositories and the ~2,000-line data source,
/// down to the fake host API. An earlier version of this file overrode
/// `loadDashboardDayUseCaseProvider` instead, which short-circuited every layer
/// below the notifier and proved less than it looked like it did.
///
/// The clock is fixed to a day inside the fixture corpus (2025-06-19 to
/// 2025-06-26), which does two jobs at once: "today" has real records behind
/// it, and the date cannot flip between the test's idea of today and the
/// notifier's when the suite runs at midnight.
final _now = DateTime(2025, 6, 25, 14, 30);
final _today = LocalDate(2025, 6, 25);
final _yesterday = LocalDate(2025, 6, 24);

Future<void> _atFixedNow(Future<void> Function() body) =>
    withClock(Clock.fixed(_now), body);

/// Boots the graph, lets the initial dashboard load settle, and clears the
/// host-API call log so each test observes only the traffic it causes.
Future<HealthHarness> _boot() async {
  final h = await bootContainer();
  h.keepAlive(dashboardProvider);
  await pumpEventQueue();
  expect(h.container.read(dashboardProvider).isLoading, isFalse,
      reason: 'the initial dashboard load never settled');
  h.hc.calls.clear();
  return h;
}

void main() {
  // The full dashboard load strays onto a real platform channel (unlike the
  // period notifiers); with no binding that surfaces as an unhandled-error
  // stack in the log even though the load itself degrades gracefully.
  TestWidgetsFlutterBinding.ensureInitialized();

  test('resumeCurrentDay reloads today by default', () => _atFixedNow(() async {
        final h = await _boot();
        final notifier = h.container.read(dashboardProvider.notifier);

        notifier.resumeCurrentDay();
        await pumpEventQueue();

        final state = h.container.read(dashboardProvider);
        expect(h.hc.calls, isNotEmpty,
            reason: 'resume must re-read the day through the stack, '
                'not serve whatever state was already there');
        expect(state.isLoading, isFalse);
        expect(state.selectedDate, _today);
        expect(state.data?.date, _today);
      }));

  test('resumeCurrentDay honours a day the user pinned in the past',
      () => _atFixedNow(() async {
        final h = await _boot();
        final notifier = h.container.read(dashboardProvider.notifier);

        notifier.previousDay();
        await pumpEventQueue();
        h.hc.calls.clear();

        notifier.resumeCurrentDay();
        await pumpEventQueue();

        // Refreshed in place — never yanked forward to today.
        final state = h.container.read(dashboardProvider);
        expect(h.hc.calls, isNotEmpty);
        expect(state.selectedDate, _yesterday);
        expect(state.data?.date, _yesterday);
      }));

  test('selectDate on a past day pins it; selecting today clears the pin',
      () => _atFixedNow(() async {
        final h = await _boot();
        final notifier = h.container.read(dashboardProvider.notifier);

        notifier.selectDate(_today.minusDays(3));
        await pumpEventQueue();

        notifier.resumeCurrentDay();
        await pumpEventQueue();
        expect(h.container.read(dashboardProvider).data?.date,
            _today.minusDays(3));

        notifier.selectDate(_today);
        await pumpEventQueue();
        h.hc.calls.clear();

        notifier.resumeCurrentDay();
        await pumpEventQueue();
        final state = h.container.read(dashboardProvider);
        expect(h.hc.calls, isNotEmpty);
        expect(state.data?.date, _today);
      }));

  test('nextDay onto a still-past day keeps the pin', () => _atFixedNow(() async {
        final h = await _boot();
        final notifier = h.container.read(dashboardProvider.notifier);

        notifier.previousDay();
        await pumpEventQueue();
        notifier.previousDay();
        await pumpEventQueue();
        // Two days back, then forward one: still in the past, so still pinned.
        notifier.nextDay();
        await pumpEventQueue();

        notifier.resumeCurrentDay();
        await pumpEventQueue();

        final state = h.container.read(dashboardProvider);
        expect(state.selectedDate, _yesterday);
        expect(state.data?.date, _yesterday);
      }));

  test('nextDay back onto today clears the pin', () => _atFixedNow(() async {
        final h = await _boot();
        final notifier = h.container.read(dashboardProvider.notifier);

        notifier.previousDay();
        await pumpEventQueue();
        notifier.nextDay();
        await pumpEventQueue();
        expect(h.container.read(dashboardProvider).selectedDate, _today);

        notifier.resumeCurrentDay();
        await pumpEventQueue();

        final state = h.container.read(dashboardProvider);
        expect(state.selectedDate, _today);
        expect(state.data?.date, _today);
      }));
}
