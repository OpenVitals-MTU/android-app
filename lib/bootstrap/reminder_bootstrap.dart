import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/reminders/alarm_manager_reminder_scheduler.dart';
import '../core/reminders/reminder_notifications.dart';
import '../core/reminders/reminder_time_zone.dart';
import '../di/providers.dart';
import '../features/homewidgets/home_widget_alarm.dart';
import '../features/hydration/reminders/hydration_reminder_device.dart';
import '../features/mindfulness/reminders/mindfulness_reminder_device.dart';

/// Brings the reminder subsystem up at app start, standing in for the Kotlin
/// boot receivers and `Application.onCreate` wiring.
///
/// Order matters:
/// 1. the time-zone database, before anything can call `zonedSchedule`;
/// 2. the notification plugin, before anything can post;
/// 3. the alarm manager service, before an alarm can be armed;
/// 4. re-arm each reminder's schedule.
///
/// Step 4 is what the Kotlin `HydrationReminderBootReceiver` did. It is still
/// needed even though the alarm is `rescheduleOnReboot`: an app *update*
/// (`MY_PACKAGE_REPLACED`) invalidates the stored callback handle, and a user
/// who enables a reminder then force-stops the app leaves nothing armed.
///
/// Every step is best-effort and independently guarded: a device that refuses
/// notifications, or a host with no alarm manager, must not stop the app from
/// starting. Returns the steps that succeeded, for logging and tests.
Future<ReminderBootstrapResult> bootstrapReminders(
  ProviderContainer container, {
  Future<bool> Function() initializeTimeZone = initializeReminderTimeZone,
  AndroidAlarmManagerApi alarms = const AndroidAlarmManagerApi(),
  TargetPlatform? platform,
}) async {
  final isAndroid = (platform ?? defaultTargetPlatform) == TargetPlatform.android;

  final timeZone = await _guard(initializeTimeZone, 'time zone');
  final notifications = await _guard(
    () async {
      final plugin = container.read(flutterLocalNotificationsProvider);
      final ready = await initializeReminderNotifications(plugin);
      // Create the high-importance channels up front, so the first reminder is a
      // heads-up rather than a silent shade entry, and so existing installs get
      // the upgraded channel without waiting for the first fire.
      await ensureHydrationReminderChannel(plugin);
      await ensureMindfulnessReminderChannel(plugin);
      return ready;
    },
    'notifications',
  );
  final alarmService =
      isAndroid ? await _guard(alarms.initialize, 'alarm manager') : false;

  // Restored even when a step above failed: a scheduled-notification fallback
  // still works without the alarm service, and a reminder that cannot notify
  // is cleared rather than left half-armed.
  final restored = await _guard(() async {
    await container.read(hydrationReminderControllerProvider).restoreSchedule();
    await container.read(mindfulnessReminderControllerProvider).restoreSchedule();
    return true;
  }, 'reminder schedules');

  // The home-screen widgets' periodic refresh rides the same alarm manager (it
  // is the Dart stand-in for Glance's `updatePeriodMillis`), so it is armed from
  // here, where the alarm service has just come up. Android-only: there is no
  // alarm manager, and no widgets, anywhere else. Re-arming an already-armed
  // alarm id simply replaces it, so a re-run is harmless.
  final homeWidgets = isAndroid
      ? await _guard(() async {
          await scheduleHomeWidgetRefresh();
          return true;
        }, 'home widget refresh')
      : false;

  return ReminderBootstrapResult(
    timeZoneReady: timeZone,
    notificationsReady: notifications,
    alarmServiceReady: alarmService,
    schedulesRestored: restored,
    homeWidgetRefreshArmed: homeWidgets,
  );
}

Future<bool> _guard(Future<bool> Function() step, String label) async {
  try {
    return await step();
  } catch (error) {
    debugPrint('Reminder bootstrap: $label failed: $error');
    return false;
  }
}

class ReminderBootstrapResult {
  const ReminderBootstrapResult({
    required this.timeZoneReady,
    required this.notificationsReady,
    required this.alarmServiceReady,
    required this.schedulesRestored,
    this.homeWidgetRefreshArmed = false,
  });

  final bool timeZoneReady;
  final bool notificationsReady;

  /// Always false off Android, where there is no alarm manager to start.
  final bool alarmServiceReady;
  final bool schedulesRestored;

  /// Whether the periodic home-screen-widget refresh was armed. Always false off
  /// Android.
  final bool homeWidgetRefreshArmed;
}
