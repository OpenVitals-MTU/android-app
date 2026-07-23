import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/reminders/reminder_notifications.dart';
import '../di/providers.dart';
import '../navigation/app_router.dart';

/// Routes a tapped reminder notification to the screen its payload names — the
/// hydration reminder opens the hydration entry form so a drink can be logged.
///
/// Mounted above the router (like the home-widget launch bootstrap) so it can
/// navigate both on a tap into an already-running app (via the plugin's tap
/// stream) and on a cold start from a notification (via its launch details). The
/// payload is an in-app go_router location the app sets on its own reminders.
class ReminderTapBootstrap extends ConsumerStatefulWidget {
  const ReminderTapBootstrap({
    super.key,
    required this.child,
    this.launchRoute = reminderNotificationLaunchRoute,
  });

  final Widget child;

  /// Resolves the route of the notification the app was cold-started from.
  /// Injected so the cold-start path is testable without the plugin's platform
  /// channel; production always uses [reminderNotificationLaunchRoute].
  final Future<String?> Function(FlutterLocalNotificationsPlugin) launchRoute;

  @override
  ConsumerState<ReminderTapBootstrap> createState() =>
      _ReminderTapBootstrapState();
}

class _ReminderTapBootstrapState extends ConsumerState<ReminderTapBootstrap> {
  StreamSubscription<String>? _taps;

  @override
  void initState() {
    super.initState();
    _taps = reminderNotificationTapRoutes.listen(
      _open,
      onError: (Object error) =>
          debugPrint('Reminder tap stream failed: $error'),
    );
    // The router is not mounted during initState, and a cold start may have
    // launched us straight from a notification; handle that after the first frame.
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final plugin = ref.read(flutterLocalNotificationsProvider);
      _open(await widget.launchRoute(plugin));
    });
  }

  void _open(String? route) {
    // Only ever an in-app location the app put on its own notification.
    if (route == null || !route.startsWith('/') || !mounted) return;
    ref.read(goRouterProvider).push(route);
  }

  @override
  void dispose() {
    _taps?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
