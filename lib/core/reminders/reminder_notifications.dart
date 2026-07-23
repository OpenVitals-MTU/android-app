import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:permission_handler/permission_handler.dart' show openAppSettings;

/// The notification small icon: the shared OpenVitals monochrome mark, used by
/// every notification the app posts (each [ReminderNotificationSpec.androidIcon]
/// names this same drawable) and the init-time default. Android tints small icons
/// from the alpha channel, so it must be an alpha silhouette, not the full-color
/// launcher icon.
const String _androidNotificationIcon = 'ic_launcher_monochrome';

/// The go_router location a tapped reminder points at (its payload — e.g. the
/// hydration entry route). A widget mounted above the router listens and
/// navigates; see `ReminderTapBootstrap`. Broadcast, per-isolate: only the UI
/// isolate's plugin receives taps, so only its stream ever has a listener.
final StreamController<String> _reminderTapRoutes =
    StreamController<String>.broadcast();

Stream<String> get reminderNotificationTapRoutes => _reminderTapRoutes.stream;

/// Foreground/background tap handler wired into [initializeReminderNotifications].
/// Must be top-level so the plugin can hold it as a callback. Public so a test
/// can drive the real tap pipeline — handler → stream → `ReminderTapBootstrap` —
/// exactly as the plugin does.
@pragma('vm:entry-point')
void handleReminderNotificationTap(NotificationResponse response) {
  final payload = response.payload;
  if (payload != null && payload.isNotEmpty) {
    _reminderTapRoutes.add(payload);
  }
}

/// The route of the reminder notification the app was cold-started from, or null.
/// The tap callback above only fires while the app is already running.
Future<String?> reminderNotificationLaunchRoute(
  FlutterLocalNotificationsPlugin plugin,
) async {
  try {
    final details = await plugin.getNotificationAppLaunchDetails();
    if (details?.didNotificationLaunchApp ?? false) {
      return details?.notificationResponse?.payload;
    }
    return null;
  } catch (_) {
    return null;
  }
}

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
      onDidReceiveNotificationResponse: handleReminderNotificationTap,
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

/// Whether the app may schedule an EXACT alarm right now.
///
/// Android 12 (S) gates exact alarms behind `SCHEDULE_EXACT_ALARM`; Android 14
/// denies that permission by default, so the user grants it from system settings.
/// Below Android 12 exact alarms need no permission and this is always true.
///
/// This is load-bearing, not cosmetic: `android_alarm_manager_plus` SILENTLY
/// DROPS an exact alarm when the permission is missing — it logs and schedules
/// nothing, with no inexact fallback — so [AlarmManagerReminderScheduler] must
/// consult this and downgrade to an inexact alarm itself, or the reminder chain
/// dies the moment the permission is absent.
///
/// Defaults to FALSE (inexact) on any doubt — an unresolved host, a channel
/// error, a null answer. A reminder a few minutes late beats one never armed.
Future<bool> canScheduleExactReminders(
  FlutterLocalNotificationsPlugin plugin,
) async {
  if (defaultTargetPlatform != TargetPlatform.android) return true;
  try {
    final android = plugin.resolvePlatformSpecificImplementation<
        AndroidFlutterLocalNotificationsPlugin>();
    if (android == null) return false;
    return await android.canScheduleExactNotifications() ?? false;
  } catch (_) {
    return false;
  }
}

/// Sends the user to the system screen that grants `SCHEDULE_EXACT_ALARM` — the
/// only way to grant it, since Android offers no in-app prompt. Returns whether
/// exact alarms are permitted afterwards.
Future<bool> requestExactReminderAlarms(
  FlutterLocalNotificationsPlugin plugin,
) async {
  if (defaultTargetPlatform != TargetPlatform.android) return true;
  try {
    final android = plugin.resolvePlatformSpecificImplementation<
        AndroidFlutterLocalNotificationsPlugin>();
    if (android == null) return false;
    await android.requestExactAlarmsPermission();
    return await android.canScheduleExactNotifications() ?? false;
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

  /// Whether reminders may fire at their EXACT time, or only inside Android's
  /// inexact-alarm window (which can be tens of minutes wide).
  Future<bool> canScheduleExact() => canScheduleExactReminders(_plugin);

  /// Sends the user to the system SCHEDULE_EXACT_ALARM screen. Returns whether
  /// exact alarms are permitted afterwards.
  Future<bool> requestExactAlarms() => requestExactReminderAlarms(_plugin);
}
