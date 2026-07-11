import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

/// The launcher icon, used as the notification's small icon. Kotlin ships
/// per-feature `ic_stat_*` drawables; the Flutter port has none yet, so the
/// launcher icon stands in.
const String _androidNotificationIcon = '@mipmap/ic_launcher';

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
}
