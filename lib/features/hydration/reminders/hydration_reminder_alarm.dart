import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../../bootstrap/background_health_access.dart';
import '../../../core/reminders/alarm_manager_reminder_scheduler.dart';
import '../../../core/reminders/local_notifications_reminder_device.dart';
import '../../../core/reminders/reminder_notifications.dart';
import '../../../data/prefs/preferences_repository.dart';
import '../../../data/repository/impl/hydration_repository_impl.dart';
import '../../../data/source/health/health_data_source.dart';
import 'hydration_reminder_controller.dart';
import 'hydration_reminder_device.dart';

/// The Android alarm id for the hydration reminder. Distinct from its
/// notification id space, but kept equal for traceability in `adb dumpsys`.
const int hydrationReminderAlarmId = 5001;

/// The alarm that wakes the app to fire the hydration reminder. Mirrors the
/// Kotlin `HydrationReminderAlarmManager`, and re-arms itself across reboot.
const AlarmManagerReminderScheduler hydrationReminderAlarmScheduler =
    AlarmManagerReminderScheduler(
  alarmId: hydrationReminderAlarmId,
  callback: hydrationReminderAlarmCallback,
);

/// Runs in a **background isolate** when the alarm fires, standing in for the
/// Kotlin `HydrationReminderReceiver`.
///
/// The isolate is fresh: no `main()` has run, no plugins are registered, and no
/// Riverpod container exists. So it registers the plugins itself and builds a
/// minimal object graph by hand. It must be a top-level function annotated with
/// `@pragma('vm:entry-point')`, or tree-shaking drops it and the raw callback
/// handle the plugin stored will not resolve.
///
/// Everything is best-effort: an exception escaping here crashes the alarm
/// service, and Android will not retry, so the reminder chain would die
/// silently. Swallowing keeps the previously-armed alarm intact.
@pragma('vm:entry-point')
Future<void> hydrationReminderAlarmCallback() async {
  try {
    DartPluginRegistrant.ensureInitialized();
    final controller = await buildBackgroundHydrationReminderController();
    await controller.handleReminderAlarm();
  } catch (error, stack) {
    debugPrint('Hydration reminder alarm failed: $error\n$stack');
  }
}

/// Builds a hydration reminder controller with no Riverpod container — for the
/// alarm isolate, where the app's provider graph does not exist.
///
/// Deliberately omits the drift `BeverageStore`: the reminder only reads today's
/// intake (from Health Connect) and the daily goal (from prefs), and opening a
/// second connection to the database from a background isolate would be a real
/// risk for no benefit.
@visibleForTesting
Future<HydrationReminderController>
    buildBackgroundHydrationReminderController() async {
  final preferences =
      PreferencesRepository(await SharedPreferences.getInstance());
  final plugin = FlutterLocalNotificationsPlugin();
  await initializeReminderNotifications(plugin);
  await ensureHydrationReminderChannel(plugin);

  // Resilient, not `.orThrow()`: a momentary HC failure here must not throw
  // before the controller exists, or the alarm never re-arms and the reminder
  // chain dies silently. A degraded read just means a possible extra nag.
  final HealthDataSource dataSource = await openBackgroundHealthAccessResilient();
  final repository = HydrationRepositoryImpl(
    dataSource,
    preferencesRepository: preferences,
  );

  return HydrationReminderController(
    preferences: preferences,
    hydrationRepository: repository,
    notifier: LocalNotificationsReminderDevice(
      plugin: plugin,
      spec: hydrationReminderNotificationSpec,
    ),
    scheduler: hydrationReminderAlarmScheduler,
    hasNotificationPermission: () => areReminderNotificationsEnabled(plugin),
  );
}
