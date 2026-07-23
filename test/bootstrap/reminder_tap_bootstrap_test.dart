import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:openvitals/bootstrap/reminder_tap_bootstrap.dart';
import 'package:openvitals/core/reminders/reminder_notifications.dart';
import 'package:openvitals/navigation/app_router.dart';

/// The notification-tap → navigation pipeline, driven end to end: the plugin's
/// tap callback ([handleReminderNotificationTap]) feeds the same stream the
/// bootstrap listens to, so these run the exact wiring a real tap runs — only
/// the plugin itself is absent.
GoRouter _router() => GoRouter(routes: [
      GoRoute(path: '/', builder: (_, _) => const Text('home')),
      GoRoute(path: '/target', builder: (_, _) => const Text('target')),
    ]);

Widget _app(
  ProviderContainer container,
  GoRouter router, {
  Future<String?> Function(FlutterLocalNotificationsPlugin)? launchRoute,
}) =>
    UncontrolledProviderScope(
      container: container,
      child: ReminderTapBootstrap(
        // Cold-start resolver stubbed to "not launched from a notification"
        // unless the test says otherwise.
        launchRoute: launchRoute ?? (_) async => null,
        child: MaterialApp.router(routerConfig: router),
      ),
    );

ProviderContainer _container(GoRouter router) {
  final container = ProviderContainer(
    overrides: [goRouterProvider.overrideWithValue(router)],
  );
  addTearDown(container.dispose);
  return container;
}

NotificationResponse _tap(String? payload) => NotificationResponse(
      notificationResponseType:
          NotificationResponseType.selectedNotification,
      payload: payload,
    );

void main() {
  testWidgets('a tapped reminder opens the route its payload names',
      (tester) async {
    final router = _router();
    await tester.pumpWidget(_app(_container(router), router));
    await tester.pumpAndSettle();
    expect(find.text('home'), findsOneWidget);

    handleReminderNotificationTap(_tap('/target'));
    await tester.pumpAndSettle();

    expect(find.text('target'), findsOneWidget);
  });

  testWidgets('a cold start from a notification opens its route after the '
      'first frame', (tester) async {
    final router = _router();
    await tester.pumpWidget(_app(
      _container(router),
      router,
      launchRoute: (_) async => '/target',
    ));
    await tester.pumpAndSettle();

    expect(find.text('target'), findsOneWidget);
  });

  testWidgets('a payload that is not an in-app location is ignored',
      (tester) async {
    // The payload is trusted as a go_router location only because the app put
    // it on its own notifications — anything else must not reach the router.
    final router = _router();
    await tester.pumpWidget(_app(_container(router), router));
    await tester.pumpAndSettle();

    handleReminderNotificationTap(_tap('https://example.com/target'));
    handleReminderNotificationTap(_tap(''));
    handleReminderNotificationTap(_tap(null));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('home'), findsOneWidget);
    expect(find.text('target'), findsNothing);
  });
}
