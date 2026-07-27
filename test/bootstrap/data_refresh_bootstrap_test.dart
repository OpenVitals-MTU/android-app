import 'package:clock/clock.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/bootstrap/data_refresh_bootstrap.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/state/refresh_coordinator.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';

void main() {
  late List<RefreshSignal> signals;
  late int availabilityResolves;
  late int grantedResolves;
  late DateTime now;

  /// Mounts the bootstrap over the two providers it re-resolves, counting how
  /// often each is actually recomputed.
  Future<void> pump(WidgetTester tester) async {
    signals = <RefreshSignal>[];
    availabilityResolves = 0;
    grantedResolves = 0;
    final container = ProviderContainer(overrides: [
      healthConnectAvailabilityProvider.overrideWith((ref) async {
        availabilityResolves++;
        return HealthConnectAvailability.available;
      }),
      grantedHealthPermissionsProvider.overrideWith((ref) async {
        grantedResolves++;
        return const <String>{};
      }),
    ]);
    addTearDown(container.dispose);
    container.listen<RefreshSignal>(
      refreshCoordinatorProvider,
      (previous, next) => signals.add(next),
    );
    // Both providers must be alive, or invalidating them recomputes nothing.
    container.listen(healthConnectAvailabilityProvider, (_, _) {});
    container.listen(grantedHealthPermissionsProvider, (_, _) {});
    await tester.pumpWidget(UncontrolledProviderScope(
      container: container,
      child: const DataRefreshBootstrap(child: SizedBox()),
    ));
    await tester.pump();
  }

  /// Backgrounds and foregrounds the app through the legal state walk —
  /// AppLifecycleListener asserts on skipped transitions.
  Future<void> resume(WidgetTester tester) async {
    final binding = tester.binding;
    binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
    binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
    binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();
  }

  Future<void> atClock(Future<void> Function() body) =>
      withClock(Clock(() => now), body);

  setUp(() => now = DateTime(2025, 6, 25, 14, 30));

  testWidgets('mounting alone does not refresh', (tester) async {
    await atClock(() async {
      await pump(tester);
      // Cold start is each screen's own first load; emitting here would double
      // every launch.
      expect(signals, isEmpty);
    });
  });

  testWidgets('returning to the foreground re-resolves availability and '
      'permissions and emits one app-open signal', (tester) async {
    await atClock(() async {
      await pump(tester);
      final availabilityBefore = availabilityResolves;
      final grantedBefore = grantedResolves;

      now = now.add(const Duration(minutes: 5));
      await resume(tester);
      await tester.pump();

      expect(signals, hasLength(1));
      expect(signals.single.reason, RefreshReason.appOpen);
      // Without this the granted set is frozen for the process lifetime: a
      // permission granted in the Health Connect app while OpenVitals was away
      // is never seen.
      expect(availabilityResolves, greaterThan(availabilityBefore));
      expect(grantedResolves, greaterThan(grantedBefore));
    });
  });

  testWidgets('a second resume within the guard interval emits no second '
      'signal', (tester) async {
    await atClock(() async {
      await pump(tester);

      now = now.add(const Duration(minutes: 5));
      await resume(tester);
      now = now.add(const Duration(seconds: 2));
      await resume(tester);

      // A two-second switch to another app and back must not re-run ~35 Health
      // Connect reads.
      expect(signals, hasLength(1));
    });
  });

  testWidgets('a resume past the guard interval refreshes again',
      (tester) async {
    await atClock(() async {
      await pump(tester);

      now = now.add(const Duration(minutes: 5));
      await resume(tester);
      now = now.add(kAppOpenRefreshMinInterval + const Duration(seconds: 1));
      await resume(tester);

      expect(signals, hasLength(2));
    });
  });

  testWidgets('a resume after the day rolls over refreshes inside the guard '
      'interval', (tester) async {
    await atClock(() async {
      await pump(tester);

      now = now.add(const Duration(minutes: 5));
      await resume(tester);
      // Just past midnight, well inside the 30s guard: stale data is at its
      // most misleading exactly here, so the guard must not apply.
      now = DateTime(2025, 6, 26, 0, 0, 5);
      await resume(tester);

      expect(signals, hasLength(2));
    });
  });
}
