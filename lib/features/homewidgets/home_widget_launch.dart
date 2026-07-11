import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:home_widget/home_widget.dart';

import '../../navigation/app_router.dart';
import '../../navigation/app_routes.dart';

/// Opening the app from a home-screen widget tap.
///
/// Kotlin puts the destination in an `EXTRA_OPENVITALS_ROUTE` intent extra and
/// `MainActivity` navigates to it. The `home_widget` plugin has its own version
/// of that channel: the native widget builds a `HomeWidgetLaunchIntent` carrying
/// a `Uri`, and Dart receives it on cold start
/// ([HomeWidget.initiallyLaunchedFromHomeWidget]) or, for an app already running,
/// on the [HomeWidget.widgetClicked] stream.
///
/// The `Uri` carries the widget's stored `route` — the Kotlin wire form, e.g.
/// `metric/STEPS` — which [homeWidgetRouteLocation] validates and maps onto the
/// go_router location. Validation is not optional: the extra crosses a process
/// boundary and any app can broadcast it, so an unrecognised route is dropped
/// rather than pushed (Kotlin's `Screen` allow-list, kept here).

/// The go_router location for a widget-tap [uri], or null when it does not name
/// an allowed destination.
///
/// The native side builds `openvitals://widget?route=<route>` (see
/// `HomeWidgetSnapshot.kt`), percent-encoding the route so one containing `/`
/// — e.g. `daily_readiness/body_energy/2026-07-10` — survives the round trip.
/// That query form is the contract; the authority is the constant `widget`.
///
/// The authority+path form (`openvitals://metric/STEPS`) is still accepted as a
/// fallback so a future native change cannot silently break every tap.
String? homeWidgetRouteLocation(Uri? uri) {
  if (uri == null) return null;
  final queryRoute = uri.queryParameters['route'];
  if (queryRoute != null && queryRoute.isNotEmpty) {
    return homeWidgetRouteLocationOf(queryRoute);
  }
  final segments = <String>[
    if (uri.host.isNotEmpty) uri.host,
    ...uri.pathSegments.where((segment) => segment.isNotEmpty),
  ];
  return homeWidgetRouteLocationOf(segments.join('/'));
}

/// The go_router location for a widget `route` string, or null when it is not on
/// the allow-list.
String? homeWidgetRouteLocationOf(String route) {
  final segments = Uri.decodeFull(route)
      .split('/')
      .where((segment) => segment.isNotEmpty)
      .toList();
  return switch (segments) {
    ['dashboard'] => AppRoutes.dashboard,
    ['daily_readiness'] => AppRoutes.dailyReadiness,
    ['daily_readiness', 'body_energy', final date]
        when _isIsoDate(date) =>
      AppRoutes.bodyEnergyDetailsLocation(date),
    ['metric', final metricId]
        when DashboardMetricId.fromStorage(metricId) != null =>
      AppRoutes.metricLocation(metricId),
    ['manual_entry', 'hydration'] => AppRoutes.hydrationEntry,
    ['manual_entry', 'hydration', 'log', final drinkId]
        when drinkId.isNotEmpty =>
      AppRoutes.hydrationEntryLogDrinkLocation(drinkId),
    _ => null,
  };
}

/// `yyyy-MM-dd`, the form `LocalDate.toString()` writes into the widget route.
///
/// Round-trips through [DateTime] rather than trusting `tryParse`: that happily
/// rolls `2026-13-45` over into 2027, which would have the widget push a route
/// for a day it never meant.
bool _isIsoDate(String value) {
  final match = RegExp(r'^(\d{4})-(\d{2})-(\d{2})$').firstMatch(value);
  if (match == null) return false;
  final year = int.parse(match.group(1)!);
  final month = int.parse(match.group(2)!);
  final day = int.parse(match.group(3)!);
  final date = DateTime(year, month, day);
  return date.year == year && date.month == month && date.day == day;
}

/// Thin seam over the plugin's launch channel, so the bootstrap below is
/// testable without an Android host.
class HomeWidgetLaunchChannel {
  const HomeWidgetLaunchChannel();

  /// The widget the app was cold-started from, if any. Swallows the missing
  /// plugin on hosts that have none (the same contract as
  /// `RouteImportIntentChannel`).
  Future<Uri?> initialLaunch() async {
    try {
      return await HomeWidget.initiallyLaunchedFromHomeWidget();
    } on PlatformException catch (error) {
      debugPrint('initiallyLaunchedFromHomeWidget failed: $error');
      return null;
    } on MissingPluginException {
      return null;
    }
  }

  /// Taps arriving while the app is already running. `MainActivity` is
  /// `singleTop`, so these come through `onNewIntent` rather than a fresh start.
  Stream<Uri?> clicks() {
    try {
      return HomeWidget.widgetClicked;
    } on MissingPluginException {
      return const Stream<Uri?>.empty();
    }
  }
}

final homeWidgetLaunchChannelProvider = Provider<HomeWidgetLaunchChannel>(
  (ref) => const HomeWidgetLaunchChannel(),
);

/// Routes home-widget taps into the app.
///
/// Mounted above the router (like `RouteImportIntentBootstrap`) so it can push
/// on cold start *and* on a tap into an already-running app.
class HomeWidgetLaunchBootstrap extends ConsumerStatefulWidget {
  const HomeWidgetLaunchBootstrap({super.key, required this.child});

  final Widget child;

  @override
  ConsumerState<HomeWidgetLaunchBootstrap> createState() =>
      _HomeWidgetLaunchBootstrapState();
}

class _HomeWidgetLaunchBootstrapState
    extends ConsumerState<HomeWidgetLaunchBootstrap> {
  StreamSubscription<Uri?>? _clicks;

  @override
  void initState() {
    super.initState();
    final channel = ref.read(homeWidgetLaunchChannelProvider);
    _clicks = channel.clicks().listen(
          _open,
          onError: (Object error) =>
              debugPrint('Home widget click stream failed: $error'),
        );
    // The router is not mounted during initState; wait for the first frame so
    // the push has somewhere to land.
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      _open(await channel.initialLaunch());
    });
  }

  @override
  void dispose() {
    _clicks?.cancel();
    super.dispose();
  }

  void _open(Uri? uri) {
    final location = homeWidgetRouteLocation(uri);
    if (location == null || !mounted) return;
    ref.read(goRouterProvider).push(location);
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
