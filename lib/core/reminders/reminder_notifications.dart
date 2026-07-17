import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:permission_handler/permission_handler.dart' show openAppSettings;

/// The default notification small icon, a fallback for any notification that
/// doesn't set its own. Reminders each supply a per-feature `ic_stat_*` drawable
/// (see [ReminderNotificationSpec.androidIcon]); this monochrome app mark just
/// keeps an unspecified icon from collapsing to a blank white square — Android
/// tints small icons from the alpha channel, so it must be an alpha silhouette,
/// not the full-color launcher icon.
const String _androidNotificationIcon = 'ic_launcher_monochrome';

/// Prepares [plugin] to post notifications. Must run before any `show` /
/// `zonedSchedule`, in the UI isolate *and* in the alarm callback's isolate —
/// each has its own plugin instance.
///
/// Never throws: on a host with no notification channel (a unit test) this is a
/// no-op returning false, so callers can start up regardless.
Future<bool> initializeReminderNotifications(
  FlutterLocalNotificationsPlugin plugin,
) async {
  try {
    final result = await plugin.initialize(
      settings: const InitializationSettings(
        android: AndroidInitializationSettings(_androidNotificationIcon),
        iOS: DarwinInitializationSettings(),
      ),
    );
    return result ?? false;
  } catch (_) {
    return false;
  }
}

/// Whether the app may post notifications right now.
///
/// Port of the Kotlin `hasNotificationPermission`: below Android 13 the
/// permission is implicit, so the platform call answering "no" for an
/// unsupported host must not disable reminders. Anything other than an explicit
/// `false` is treated as granted.
Future<bool> areReminderNotificationsEnabled(
  FlutterLocalNotificationsPlugin plugin,
) async {
  if (defaultTargetPlatform != TargetPlatform.android) return true;
  try {
    final android = plugin.resolvePlatformSpecificImplementation<
        AndroidFlutterLocalNotificationsPlugin>();
    if (android == null) return true;
    return await android.areNotificationsEnabled() ?? true;
  } catch (_) {
    return true;
  }
}

/// Asks for the Android 13+ POST_NOTIFICATIONS permission. Returns whether the
/// app may post afterwards.
Future<bool> requestReminderNotificationPermission(
  FlutterLocalNotificationsPlugin plugin,
) async {
  if (defaultTargetPlatform != TargetPlatform.android) return true;
  try {
    final android = plugin.resolvePlatformSpecificImplementation<
        AndroidFlutterLocalNotificationsPlugin>();
    if (android == null) return true;
    return await android.requestNotificationsPermission() ?? false;
  } catch (_) {
    return false;
  }
}

/// The notification-permission seam, injected so reminder settings UI can be
/// tested without a platform channel.
class ReminderNotificationPermissions {
  const ReminderNotificationPermissions(this._plugin);

  final FlutterLocalNotificationsPlugin _plugin;

  Future<bool> isEnabled() => areReminderNotificationsEnabled(_plugin);

  Future<bool> request() => requestReminderNotificationPermission(_plugin);

  /// Opens the OS app-notification settings. The only recourse once the user has
  /// permanently denied POST_NOTIFICATIONS: Android then refuses to prompt again,
  /// so [request] returns false without showing anything.
  Future<bool> openSettings() => openAppSettings();
}
