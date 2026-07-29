import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:notification_listener_native/notification_listener_native.dart';

import '../../../core/diagnostics/diagnostics_build_config.dart';
import '../../../data/prefs/stores/watch_notification_prefs_store.dart';
import '../../../devices/garmin/garmin_log.dart';
import '../../../di/providers.dart';

part 'watch_notifications_view_model.freezed.dart';

/// One app the user can silence.
@freezed
abstract class WatchNotificationApp with _$WatchNotificationApp {
  const factory WatchNotificationApp({
    required String packageName,
    required String label,
    required bool blocked,
  }) = _WatchNotificationApp;
}

/// Whether phone notifications are mirrored to the watch, and whether Android
/// will let them be.
///
/// Two separate gates, deliberately surfaced separately: the user can want the
/// feature ([enabled]) while Android has not been told to allow it
/// ([accessGranted]). Collapsing them into one switch would make a flip that
/// does nothing look like a bug.
@freezed
abstract class WatchNotificationsState with _$WatchNotificationsState {
  const WatchNotificationsState._();

  const factory WatchNotificationsState({
    /// Whether the user has switched forwarding on.
    @Default(false) bool enabled,

    /// Whether Android has granted notification access. There is no runtime
    /// prompt for it — the only way to grant it is the system settings screen,
    /// so this is polled rather than awaited.
    @Default(false) bool accessGranted,

    /// Whether the user has seen and accepted what the feature reads. Required
    /// before access is requested; remembered, so toggling does not re-prompt.
    @Default(false) bool disclosureAccepted,

    /// Every launchable app, blocked flag included. Empty until loaded — the
    /// picker asks for it, the card does not need it.
    @Default(<WatchNotificationApp>[]) List<WatchNotificationApp> apps,
    @Default(false) bool loadingApps,
    @Default(true) bool loading,
  }) = _WatchNotificationsState;

  /// Forwarding is only actually happening when both gates are open.
  bool get active => enabled && accessGranted;

  int get blockedCount => apps.where((app) => app.blocked).length;
}

class WatchNotificationsViewModel extends Notifier<WatchNotificationsState> {
  @override
  WatchNotificationsState build() {
    Future.microtask(refresh);
    return const WatchNotificationsState();
  }

  NotificationListenerHostApi get _api =>
      ref.read(notificationListenerApiProvider);

  WatchNotificationPrefsStore get _store =>
      WatchNotificationPrefsStore(ref.read(sharedPreferencesProvider));

  /// Re-reads the permission, which can only have changed while the app was
  /// away at the system settings screen.
  Future<void> refresh() async {
    final store = _store;
    final granted = await _readAccess();
    if (!ref.mounted) return;
    state = state.copyWith(
      enabled: store.enabled,
      accessGranted: granted,
      disclosureAccepted: store.disclosureAccepted,
      loading: false,
    );
    // The native filter keeps its own copy of the configuration so it can run
    // before any Flutter engine exists. Pushed on every refresh rather than
    // only on change, because the paired watch can change without this switch
    // moving.
    await _pushConfig();
  }

  /// Flips forwarding on or off.
  ///
  /// Turning it on requires two things the switch itself cannot supply: the
  /// user's informed consent, and a permission only Android's own settings
  /// screen can grant. The caller supplies the first by resolving
  /// [confirmDisclosure]; this method will not proceed without it.
  Future<void> setEnabled({
    required bool enabled,
    required Future<bool> Function() confirmDisclosure,
  }) async {
    final store = _store;

    if (!enabled) {
      store.enabled = false;
      if (!ref.mounted) return;
      state = state.copyWith(enabled: false);
      await _pushConfig();
      return;
    }

    // Consent BEFORE the permission is requested, which is what Google Play
    // requires and the order a reasonable person would expect: say what will be
    // read, then ask for it.
    if (!store.disclosureAccepted) {
      final accepted = await confirmDisclosure();
      if (!accepted) return;
      store.disclosureAccepted = true;
      if (!ref.mounted) return;
      state = state.copyWith(disclosureAccepted: true);
    }

    // Re-read rather than trust [state]: Android gives no callback when access
    // is granted — the user leaves for a system screen and comes back — so
    // anything cached before that is stale. Trusting the cache is what made the
    // switch refuse to move after access had already been granted.
    var granted = state.accessGranted;
    if (!granted) {
      granted = await _readAccess();
      if (!ref.mounted) return;
      state = state.copyWith(accessGranted: granted);
    }
    if (!granted) {
      await openAccessSettings();
      return;
    }

    store.enabled = true;
    if (!ref.mounted) return;
    state = state.copyWith(enabled: true);
    await _pushConfig();
  }

  /// Loads the app list for the picker.
  Future<void> loadApps() async {
    if (state.loadingApps) return;
    state = state.copyWith(loadingApps: true);
    final blocked = _store.blockedPackages;
    List<InstalledAppMsg> installed;
    try {
      installed = await _api.listLaunchableApps();
    } catch (error) {
      garminLog('[GARMIN-NOTIFY] could not list apps: $error');
      installed = const [];
    }
    if (!ref.mounted) return;
    state = state.copyWith(
      loadingApps: false,
      apps: [
        for (final app in installed)
          WatchNotificationApp(
            packageName: app.packageName,
            label: app.label,
            blocked: blocked.contains(app.packageName),
          ),
      ],
    );
  }

  /// Silences an app, or un-silences it.
  Future<void> setBlocked(String packageName, {required bool blocked}) async {
    _store.setBlocked(packageName, blocked: blocked);
    if (!ref.mounted) return;
    state = state.copyWith(
      apps: [
        for (final app in state.apps)
          if (app.packageName == packageName)
            app.copyWith(blocked: blocked)
          else
            app,
      ],
    );
    await _pushConfig();
  }

  Future<void> openAccessSettings() async {
    try {
      await _api.openNotificationAccessSettings();
    } catch (error) {
      garminLog('[GARMIN-NOTIFY] could not open notification settings: $error');
    }
  }

  /// Mirrors the configuration to the native filter.
  ///
  /// The ONE place this happens, because it is the one place the two copies can
  /// drift — and if they do, the filter drops everything before an engine is
  /// ever spun and the watch stays silent with no error anywhere.
  Future<void> _pushConfig() async {
    final store = _store;
    // No watch means nowhere to send anything, and the native filter treats
    // that as "capture nothing" rather than buffering for a watch that may
    // never be paired.
    final watch = ref
        .read(readPairedBleDevicesUseCaseProvider)()
        .where((device) => device.isGarminGfdi)
        .firstOrNull;
    try {
      await _api.setForwardingConfig(
        store.enabled,
        store.blockedPackages.toList(),
        watch?.address,
        // The native side has no view of the app's build type — the plugin is a
        // separate Gradle module with its own BuildConfig — so the single
        // decision made here travels down with the rest of the config. True in a
        // debug build and in a nightly; false in a store release, where nothing
        // notification-derived should reach logcat.
        kDiagnosticsEnabled,
      );
    } catch (error) {
      if (kDebugMode) {
        garminLog('[GARMIN-NOTIFY] could not push the config: $error');
      }
    }
  }

  Future<bool> _readAccess() async {
    try {
      return await _api.isNotificationAccessGranted();
    } catch (error) {
      garminLog('[GARMIN-NOTIFY] could not read notification access: $error');
      return false;
    }
  }
}

final watchNotificationsViewModelProvider =
    NotifierProvider<WatchNotificationsViewModel, WatchNotificationsState>(
  WatchNotificationsViewModel.new,
);
