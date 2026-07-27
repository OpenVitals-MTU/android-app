import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/refresh/data_domain.dart';
import '../../navigation/app_router.dart' show routeObserver;
import '../../state/refresh_coordinator.dart';

/// Wires a screen to the app-open and data-changed refresh triggers.
///
/// Mixed onto a screen's [ConsumerState]: it listens to [refreshCoordinatorProvider]
/// and calls [onRefreshSignal] when a signal touches [refreshDomains] **and this
/// route is on top**. Otherwise it holds the signal and fires once the route is
/// revealed again ([RouteAware.didPopNext], through the router's [routeObserver]
/// — the mechanism the dashboard used to own alone, moved here so every screen
/// gets it once).
///
/// ## Why the screen listens and not the view-model
///
/// Every feature provider in this app is a plain (non-auto-dispose)
/// `NotifierProvider`, and the heart/activity-metric/nutrition screens each
/// eagerly build a map of one provider per metric — 25-40 view-models can be
/// alive at once after a few minutes of browsing. Subscribing in `build()` would
/// turn one app-open signal into 25-40 concurrent Health Connect read waves,
/// which Health Connect serializes: the same contention that once made the
/// calories screen's first open go from ~30s to 80s+. Mounted screens are
/// bounded by the navigation stack, which is one to three deep.
///
/// No coalescing is needed on this side: `PeriodMetricLoader.runLoad` is already
/// single-flight with latest-wins, so redundant signals collapse into one fetch.
mixin RefreshOnSignal<T extends ConsumerStatefulWidget> on ConsumerState<T>
    implements RouteAware {
  ProviderSubscription<RefreshSignal>? _subscription;
  RefreshSignal? _pending;

  /// The data this screen reads. A signal touching any of these refreshes it.
  /// An empty set opts out entirely.
  Set<DataDomain> get refreshDomains;

  /// Re-read. Called at most once per signal, on the frame the screen is (or
  /// becomes) visible.
  void onRefreshSignal(RefreshSignal signal);

  @override
  void initState() {
    super.initState();
    // listenManual, not ref.listen: the latter is only legal inside build, and
    // this subscription has to outlive a rebuild that does not touch it.
    _subscription = ref.listenManual<RefreshSignal>(
      refreshCoordinatorProvider,
      (previous, next) => _handle(next),
    );
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final route = ModalRoute.of<void>(context);
    if (route != null) routeObserver.subscribe(this, route);
  }

  @override
  void dispose() {
    routeObserver.unsubscribe(this);
    _subscription?.close();
    _subscription = null;
    super.dispose();
  }

  void _handle(RefreshSignal signal) {
    if (refreshDomains.isEmpty || !signal.touches(refreshDomains)) return;
    if (_isVisible) {
      _pending = null;
      onRefreshSignal(signal);
      return;
    }
    // Off-screen: hold the newest signal rather than reading now. The user is
    // looking at whatever was pushed on top, and that screen is doing its own
    // reading.
    _pending = signal;
  }

  /// True when this route is the one the user is looking at. `isCurrent` is
  /// false for a route with something pushed over it; the null fallback covers
  /// a screen mounted outside a [ModalRoute] (widget tests, embedded use).
  bool get _isVisible => ModalRoute.of<void>(context)?.isCurrent ?? true;

  /// A pushed screen (a metric detail, an entry form, settings…) was popped and
  /// this screen is on top again. Refreshes only if something actually changed
  /// while it was away — a plain back-navigation is no longer a reload.
  @override
  void didPopNext() {
    final signal = _pending;
    if (signal == null) return;
    _pending = null;
    onRefreshSignal(signal);
  }

  @override
  void didPush() {}

  @override
  void didPop() {}

  @override
  void didPushNext() {}
}
