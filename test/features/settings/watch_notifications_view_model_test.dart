import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:notification_listener_native/notification_listener_native.dart';

import 'package:openvitals/core/diagnostics/diagnostics_build_config.dart';

import 'package:openvitals/di/providers.dart';
import 'package:openvitals/features/settings/application/watch_notifications_view_model.dart';

import '../../support/notifications/fake_notification_listener_api.dart';

Future<ProviderContainer> _container(FakeNotificationListenerApi api) async {
  SharedPreferences.setMockInitialValues({});
  final prefs = await SharedPreferences.getInstance();
  final container = ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      notificationListenerApiProvider.overrideWithValue(api),
    ],
  );
  addTearDown(container.dispose);
  return container;
}

/// A user who accepts the disclosure. Most tests are about what happens after
/// consent, so this is the default; the disclosure itself has its own tests.
Future<bool> _accept() async => true;

Future<bool> _decline() async => false;

/// Reads the state after the view-model's initial async refresh has settled.
Future<WatchNotificationsState> _settled(ProviderContainer container) async {
  container.read(watchNotificationsViewModelProvider);
  await container.read(watchNotificationsViewModelProvider.notifier).refresh();
  return container.read(watchNotificationsViewModelProvider);
}

void main() {
  test('the switch reads off until notification access is granted', () async {
    final api = FakeNotificationListenerApi(accessGranted: false);
    final container = await _container(api);

    final state = await _settled(container);

    expect(state.accessGranted, isFalse);
    expect(state.active, isFalse);
  });

  test('turning it on without access opens system settings and does NOT enable',
      () async {
    final api = FakeNotificationListenerApi(accessGranted: false);
    final container = await _container(api);
    await _settled(container);

    await container
        .read(watchNotificationsViewModelProvider.notifier)
        .setEnabled(enabled: true, confirmDisclosure: _accept);

    expect(api.openedAccessSettings, isTrue);
    expect(container.read(watchNotificationsViewModelProvider).active, isFalse);
    expect(api.lastConfig?.enabled, isNot(true));
  });

  test('access granted while the screen was open still lets the switch turn on',
      () async {
    // The regression. Android gives no callback when the user grants access on
    // its own settings screen, so the state cached at build time is stale — and
    // trusting it made the switch silently reopen settings forever instead of
    // turning on.
    final api = FakeNotificationListenerApi(accessGranted: false);
    final container = await _container(api);
    await _settled(container);

    api.accessGranted = true; // granted while the app was away

    await container
        .read(watchNotificationsViewModelProvider.notifier)
        .setEnabled(enabled: true, confirmDisclosure: _accept);

    final state = container.read(watchNotificationsViewModelProvider);
    expect(state.accessGranted, isTrue);
    expect(state.enabled, isTrue);
    expect(state.active, isTrue);
    expect(api.lastConfig?.enabled, isTrue);
  });

  test('enabling pushes the config the native filter runs on', () async {
    final api = FakeNotificationListenerApi();
    final container = await _container(api);
    await _settled(container);

    await container
        .read(watchNotificationsViewModelProvider.notifier)
        .setEnabled(enabled: true, confirmDisclosure: _accept);

    // Without this reaching the native side, the filter drops everything before
    // an engine is ever spun and the watch stays silent with no error anywhere.
    expect(api.lastConfig, isNotNull);
    expect(api.lastConfig!.enabled, isTrue);
  });

  test('turning it off pushes the config again, so capture stops at once',
      () async {
    final api = FakeNotificationListenerApi();
    final container = await _container(api);
    await _settled(container);
    final notifier =
        container.read(watchNotificationsViewModelProvider.notifier);

    await notifier.setEnabled(enabled: true, confirmDisclosure: _accept);
    await notifier.setEnabled(enabled: false, confirmDisclosure: _accept);

    expect(api.lastConfig!.enabled, isFalse);
    expect(container.read(watchNotificationsViewModelProvider).active, isFalse);
  });

  test('the choice survives a rebuild of the view-model', () async {
    final api = FakeNotificationListenerApi();
    final container = await _container(api);
    await _settled(container);
    await container
        .read(watchNotificationsViewModelProvider.notifier)
        .setEnabled(enabled: true, confirmDisclosure: _accept);

    container.invalidate(watchNotificationsViewModelProvider);
    final state = await _settled(container);

    expect(state.enabled, isTrue);
  });

  test('the disclosure is shown before notification access is requested',
      () async {
    // Google Play requires consent BEFORE the permission is asked for, and it
    // is the order a reasonable person expects: say what will be read, then ask.
    final api = FakeNotificationListenerApi(accessGranted: false);
    final container = await _container(api);
    await _settled(container);
    var askedFirst = false;

    await container.read(watchNotificationsViewModelProvider.notifier).setEnabled(
          enabled: true,
          confirmDisclosure: () async {
            askedFirst = !api.openedAccessSettings;
            return true;
          },
        );

    expect(askedFirst, isTrue);
    expect(api.openedAccessSettings, isTrue);
  });

  test('declining the disclosure enables nothing and opens nothing', () async {
    final api = FakeNotificationListenerApi();
    final container = await _container(api);
    await _settled(container);

    await container
        .read(watchNotificationsViewModelProvider.notifier)
        .setEnabled(enabled: true, confirmDisclosure: _decline);

    expect(container.read(watchNotificationsViewModelProvider).active, isFalse);
    expect(api.openedAccessSettings, isFalse);
    expect(api.lastConfig?.enabled, isNot(true));
  });

  test('the disclosure is shown once, not on every toggle', () async {
    // Consent is remembered, so switching off and on again must not re-prompt.
    final api = FakeNotificationListenerApi();
    final container = await _container(api);
    await _settled(container);
    final notifier =
        container.read(watchNotificationsViewModelProvider.notifier);
    var shown = 0;
    Future<bool> count() async {
      shown++;
      return true;
    }

    await notifier.setEnabled(enabled: true, confirmDisclosure: count);
    await notifier.setEnabled(enabled: false, confirmDisclosure: count);
    await notifier.setEnabled(enabled: true, confirmDisclosure: count);

    expect(shown, 1);
  });

  test('switching off never asks for consent', () async {
    final api = FakeNotificationListenerApi();
    final container = await _container(api);
    await _settled(container);

    await container
        .read(watchNotificationsViewModelProvider.notifier)
        .setEnabled(enabled: false, confirmDisclosure: _decline);

    expect(api.lastConfig!.enabled, isFalse);
  });

  test('the build\'s diagnostics flag travels to the native side', () async {
    // The plugin is a separate Gradle module with its own BuildConfig, so it
    // cannot see whether this is a debug/nightly build. Everything it logs is
    // notification-derived, so it says nothing without being told.
    final api = FakeNotificationListenerApi();
    final container = await _container(api);

    await _settled(container);

    expect(api.lastConfig!.diagnostics, kDiagnosticsEnabled);
    expect(kDiagnosticsEnabled, isTrue,
        reason: 'tests run in debug, where diagnostics are on');
  });

  group('the blocklist', () {
    test('every app is listed as sending until it is silenced', () async {
      final api = FakeNotificationListenerApi(launchableApps: [
        InstalledAppMsg(packageName: 'com.example.chat', label: 'Chat'),
        InstalledAppMsg(packageName: 'com.example.game', label: 'Game'),
      ]);
      final container = await _container(api);
      await _settled(container);

      await container
          .read(watchNotificationsViewModelProvider.notifier)
          .loadApps();

      final state = container.read(watchNotificationsViewModelProvider);
      expect(state.apps.map((a) => a.blocked), [false, false]);
      expect(state.blockedCount, 0);
    });

    test('silencing an app reaches the native filter', () async {
      // The filter runs before any Flutter engine exists, so a blocklist that
      // never got there would silence nothing.
      final api = FakeNotificationListenerApi(launchableApps: [
        InstalledAppMsg(packageName: 'com.example.game', label: 'Game'),
      ]);
      final container = await _container(api);
      await _settled(container);
      final notifier =
          container.read(watchNotificationsViewModelProvider.notifier);
      await notifier.loadApps();

      await notifier.setBlocked('com.example.game', blocked: true);

      expect(api.lastConfig!.blocked, ['com.example.game']);
      expect(container.read(watchNotificationsViewModelProvider).blockedCount, 1);
    });

    test('a silenced app stays silenced across a rebuild', () async {
      final api = FakeNotificationListenerApi(launchableApps: [
        InstalledAppMsg(packageName: 'com.example.game', label: 'Game'),
      ]);
      final container = await _container(api);
      await _settled(container);
      final notifier =
          container.read(watchNotificationsViewModelProvider.notifier);
      await notifier.loadApps();
      await notifier.setBlocked('com.example.game', blocked: true);

      container.invalidate(watchNotificationsViewModelProvider);
      await _settled(container);
      await container
          .read(watchNotificationsViewModelProvider.notifier)
          .loadApps();

      expect(
        container.read(watchNotificationsViewModelProvider).apps.single.blocked,
        isTrue,
      );
    });

    test('un-silencing removes it again', () async {
      final api = FakeNotificationListenerApi(launchableApps: [
        InstalledAppMsg(packageName: 'com.example.game', label: 'Game'),
      ]);
      final container = await _container(api);
      await _settled(container);
      final notifier =
          container.read(watchNotificationsViewModelProvider.notifier);
      await notifier.loadApps();
      await notifier.setBlocked('com.example.game', blocked: true);

      await notifier.setBlocked('com.example.game', blocked: false);

      expect(api.lastConfig!.blocked, isEmpty);
    });
  });

  test('revoking access in system settings turns the switch off again',
      () async {
    final api = FakeNotificationListenerApi();
    final container = await _container(api);
    await _settled(container);
    await container
        .read(watchNotificationsViewModelProvider.notifier)
        .setEnabled(enabled: true, confirmDisclosure: _accept);

    api.accessGranted = false; // revoked while the app was away
    await container.read(watchNotificationsViewModelProvider.notifier).refresh();

    final state = container.read(watchNotificationsViewModelProvider);
    expect(state.active, isFalse,
        reason: 'the switch must not claim to be forwarding');
  });
}
