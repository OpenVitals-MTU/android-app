import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../../bootstrap/background_health_access.dart';
import '../../../core/reminders/alarm_manager_reminder_scheduler.dart';
import '../../../core/reminders/local_notifications_reminder_device.dart';
import '../../../core/reminders/reminder_notifications.dart';
import '../../../data/prefs/preferences_repository.dart';
import '../../../data/repository/impl/mindfulness_repository_impl.dart';
import '../../../data/source/health/health_data_source.dart';
import 'mindfulness_reminder_controller.dart';
import 'mindfulness_reminder_device.dart';

/// The Android alarm id for the mindfulness reminder. Distinct from hydration's.
const int mindfulnessReminderAlarmId = 5002;

/// Builds the mindfulness alarm scheduler, wiring the exact-alarm gate to
/// [plugin] (which answers `SCHEDULE_EXACT_ALARM`). Built per call site rather
/// than as a top-level const: the gate needs a live plugin, and a const cannot
/// hold one. The UI and the alarm isolate each pass their own plugin instance.
AlarmManagerReminderScheduler mindfulnessReminderAlarmSchedulerFor(
  FlutterLocalNotificationsPlugin plugin,
) =>
    AlarmManagerReminderScheduler(
      alarmId: mindfulnessReminderAlarmId,
      callback: mindfulnessReminderAlarmCallback,
      canScheduleExact: () => canScheduleExactReminders(plugin),
    );

/// Runs in a background isolate when the alarm fires. See
/// `hydrationReminderAlarmCallback` for why this must be a top-level
/// `vm:entry-point` function that registers its own plugins.
@pragma('vm:entry-point')
Future<void> mindfulnessReminderAlarmCallback() async {
  try {
    DartPluginRegistrant.ensureInitialized();
    final controller = await buildBackgroundMindfulnessReminderController();
    await controller.handleReminderAlarm();
  } catch (error, stack) {
    debugPrint('Mindfulness reminder alarm failed: $error\n$stack');
  }
}

/// Builds a mindfulness reminder controller with no Riverpod container, for the
/// alarm isolate.
@visibleForTesting
Future<MindfulnessReminderController>
    buildBackgroundMindfulnessReminderController() async {
  final preferences =
      PreferencesRepository(await SharedPreferences.getInstance());
  final plugin = FlutterLocalNotificationsPlugin();
  await initializeReminderNotifications(plugin);
  await ensureMindfulnessReminderChannel(plugin);

  // Resilient, not `.orThrow()`: a momentary HC failure here must not throw
  // before the controller exists, or the alarm never re-arms and the reminder
  // chain dies silently. A degraded read just means a possible extra nag.
  final HealthDataSource dataSource = await openBackgroundHealthAccessResilient();
  final repository = MindfulnessRepositoryImpl(dataSource);

  return MindfulnessReminderController(
    preferences: preferences,
    mindfulnessRepository: repository,
    notifier: LocalNotificationsReminderDevice(
      plugin: plugin,
      spec: mindfulnessReminderNotificationSpec,
    ),
    scheduler: mindfulnessReminderAlarmSchedulerFor(plugin),
    hasNotificationPermission: () => areReminderNotificationsEnabled(plugin),
  );
}
