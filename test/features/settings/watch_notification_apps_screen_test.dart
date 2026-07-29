import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notification_listener_native/notification_listener_native.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/di/providers.dart';
import 'package:openvitals/features/settings/presentation/watch_notification_apps_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';

import '../../support/notifications/fake_notification_listener_api.dart';

Future<Widget> _harness(FakeNotificationListenerApi api) async {
  SharedPreferences.setMockInitialValues({});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      notificationListenerApiProvider.overrideWithValue(api),
    ],
    child: const MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: WatchNotificationAppsScreen(),
    ),
  );
}

void main() {
  testWidgets('a phone with no launchable apps says so rather than showing an '
      'empty list', (tester) async {
    await tester.pumpWidget(await _harness(FakeNotificationListenerApi()));
    await tester.pumpAndSettle();

    expect(find.text('No apps found.'), findsOneWidget);
  });

  testWidgets('every app reads as sending to the watch until it is silenced',
      (tester) async {
    // A blocklist, not an allow-list: a newly installed messaging app must not
    // be silent by default.
    await tester.pumpWidget(await _harness(FakeNotificationListenerApi(
      launchableApps: [
        InstalledAppMsg(packageName: 'com.example.chat', label: 'Chat'),
      ],
    )));
    await tester.pumpAndSettle();

    expect(find.text('Chat'), findsOneWidget);
    expect(tester.widget<SwitchListTile>(find.byType(SwitchListTile)).value,
        isTrue);
  });

  testWidgets('silencing an app reaches the native filter', (tester) async {
    // The filter runs before any Flutter engine exists, so a choice that never
    // got there would silence nothing.
    final api = FakeNotificationListenerApi(launchableApps: [
      InstalledAppMsg(packageName: 'com.example.game', label: 'Game'),
    ]);
    await tester.pumpWidget(await _harness(api));
    await tester.pumpAndSettle();

    await tester.tap(find.byType(SwitchListTile));
    await tester.pumpAndSettle();

    expect(api.lastConfig!.blocked, ['com.example.game']);
    expect(tester.widget<SwitchListTile>(find.byType(SwitchListTile)).value,
        isFalse);
  });

  testWidgets('an app that cannot be listed leaves the screen usable rather '
      'than stuck loading', (tester) async {
    final api = _FailingAppList();
    await tester.pumpWidget(await _harness(api));
    await tester.pumpAndSettle();

    expect(find.byType(CircularProgressIndicator), findsNothing);
    expect(find.text('No apps found.'), findsOneWidget);
  });
}

/// A host whose app query throws — a platform channel with no implementation,
/// which is what a non-Android host looks like.
class _FailingAppList extends FakeNotificationListenerApi {
  @override
  Future<List<InstalledAppMsg>> listLaunchableApps() async =>
      throw StateError('no host');
}
