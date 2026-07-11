import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../../core/reminders/alarm_manager_reminder_scheduler.dart';
import '../../../core/reminders/local_notifications_reminder_device.dart';
import '../../../core/reminders/reminder_notifications.dart';
import '../../../data/prefs/preferences_repository.dart';
import '../../../data/repository/impl/health_repository_impl.dart';
import '../../../data/repository/impl/mindfulness_repository_impl.dart';
import '../../../di/providers.dart' show openVitalsPackageName;
import '../../../health/health_data_source.dart';
import '../../../health/native/health_connect_native_data_source.dart';
import 'mindfulness_reminder_controller.dart';
import 'mindfulness_reminder_device.dart';

/// The Android alarm id for the mindfulness reminder. Distinct from hydration's.
const int mindfulnessReminderAlarmId = 5002;

const AlarmManagerReminderScheduler mindfulnessReminderAlarmScheduler =
    AlarmManagerReminderScheduler(
  alarmId: mindfulnessReminderAlarmId,
  callback: mindfulnessReminderAlarmCallback,
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

  final HealthDataSource dataSource =
      HealthConnectNativeDataSource(appPackageName: openVitalsPackageName);
  // MUST resolve access before any read. `cachedAvailability` starts at
  // `notSupported`, and every repository gates its reads on it
  // (`_grantedIfAvailable`), so without this today's mindfulness always reads as
  // zero — the goal never counts as met, and the reminder keeps nagging instead
  // of rolling to tomorrow. The app gets this for free from `HealthConnectGate`;
  // this isolate has no widget tree.
  await HealthRepositoryImpl(dataSource).refreshAvailability();
  final repository = MindfulnessRepositoryImpl(dataSource);

  return MindfulnessReminderController(
    preferences: preferences,
    mindfulnessRepository: repository,
    notifier: LocalNotificationsReminderDevice(
      plugin: plugin,
      spec: mindfulnessReminderNotificationSpec,
    ),
    scheduler: mindfulnessReminderAlarmScheduler,
    hasNotificationPermission: () => areReminderNotificationsEnabled(plugin),
  );
}
