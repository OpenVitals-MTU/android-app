import 'dart:async';

import 'package:clock/clock.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/time/local_date.dart';
import '../di/providers.dart';
import '../features/dashboard/application/dashboard_view_model.dart';
import '../state/refresh_coordinator.dart';
import '../ui/components/health_connect_gate.dart';

/// The minimum gap between two app-open refreshes.
///
/// A full refresh is ~35 Health Connect reads, so a two-second switch to another
/// app and back must not re-run them. Bypassed when the local day has rolled
/// over, which is exactly when stale data is most misleading.
const Duration kAppOpenRefreshMinInterval = Duration(seconds: 30);

/// Refreshes the app's data whenever it returns to the foreground.
///
/// The first of the three refresh triggers. Mounted ABOVE the router — and
/// therefore above every [HealthConnectGate] — which is the whole point: the
/// dashboard used to own this wiring from inside its gate, so in exactly the
/// states where the gate replaces its child (resolving, unavailable, permissions
/// missing, read failed) no lifecycle listener existed at all and a resume did
/// nothing. Granting a permission in the Health Connect app and coming back
/// could not recover without a force-stop.
///
/// Re-resolving availability and the granted set is half the job:
/// [HealthDataSource.cachedAvailability] is only ever written by
/// `refreshAvailability()`, so a cold start that resolved `notSupported` (Health
/// Connect mid-update) otherwise pinned the gate — and made every repository's
/// `grantedIfAvailable()` return an empty set — for the whole process lifetime.
class DataRefreshBootstrap extends ConsumerStatefulWidget {
  const DataRefreshBootstrap({super.key, required this.child});

  final Widget child;

  @override
  ConsumerState<DataRefreshBootstrap> createState() =>
      _DataRefreshBootstrapState();
}

class _DataRefreshBootstrapState extends ConsumerState<DataRefreshBootstrap> {
  late final AppLifecycleListener _listener;
  ProviderSubscription<bool>? _dashboardLoading;
  late DateTime _lastRefreshAt;
  late LocalDate _lastRefreshDay;
  bool _drainPending = false;

  @override
  void initState() {
    super.initState();
    // Seeded, not emitted: a cold start is already loaded by each screen's own
    // first build, and emitting here would double every launch.
    _lastRefreshAt = clock.now();
    _lastRefreshDay = LocalDate.now();
    _listener = AppLifecycleListener(onResume: _onResume);
    // The dashboard is the home screen and is always the first thing to load,
    // so its isLoading falling is the app's "the foreground read finished"
    // signal — the only safe moment to start a background drain. Health Connect
    // serializes concurrent reads, and a drain running next to a foreground load
    // is the documented 30s→80s contention.
    _dashboardLoading = ref.listenManual<bool>(
      dashboardProvider.select((s) => s.isLoading),
      (previous, next) {
        if (previous == true && next == false) _drainHistoryCaches();
      },
    );
  }

  void _onResume() {
    final now = clock.now();
    final today = LocalDate.now();
    if (today == _lastRefreshDay &&
        now.difference(_lastRefreshAt) < kAppOpenRefreshMinInterval) {
      return;
    }
    _lastRefreshAt = now;
    _lastRefreshDay = today;

    // Re-resolve the platform state the whole app gates on before asking anyone
    // to read. Both are plain FutureProviders that nothing else invalidates
    // outside an explicit tap.
    ref.invalidate(healthConnectAvailabilityProvider);
    ref.invalidate(grantedHealthPermissionsProvider);

    ref.read(refreshCoordinatorProvider.notifier).appOpened();

    // Armed, not run: the signal above sends the visible screen back to Health
    // Connect, and the drain has to wait for that read to finish.
    _drainPending = true;
  }

  /// Catch the daily-aggregate caches up on whatever moved while the app was
  /// away — a watch sync, an import, another app writing to Health Connect.
  ///
  /// Each drain used to be kicked from exactly one screen, so a user who only
  /// ever opens the dashboard never drained them at all.
  void _drainHistoryCaches() {
    if (!_drainPending) return;
    _drainPending = false;
    unawaited(ref.read(historySyncSchedulerProvider).drainIncremental());
  }

  @override
  void dispose() {
    _dashboardLoading?.close();
    _dashboardLoading = null;
    _listener.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
