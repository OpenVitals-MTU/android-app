
import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

import '../core/presentation/unit_formatter.dart';
import '../core/reminders/local_notifications_reminder_device.dart';
import '../features/manualentry/mindfulness/mindfulness_sound_player.dart';
import '../features/hydration/reminders/hydration_reminder_alarm.dart';
import '../features/mindfulness/reminders/mindfulness_reminder_alarm.dart';
import '../core/reminders/reminder_notifications.dart';
import '../core/reminders/reminder_controller.dart';
import '../features/activity/maps/offline_map_import_controller.dart';
import '../features/activity/maps/offline_map_metadata_store.dart';
import '../features/homewidgets/home_widget_refresher.dart';
import '../features/homewidgets/home_widget_service.dart';
import '../features/hydration/reminders/hydration_reminder_controller.dart';
import '../features/hydration/reminders/hydration_reminder_device.dart';
import '../features/mindfulness/reminders/mindfulness_reminder_controller.dart';
import '../features/mindfulness/reminders/mindfulness_reminder_device.dart';
import 'data_providers.dart';
import 'usecase_providers.dart';
/// The app's long-lived services: the two reminder stacks, the home-screen
/// widget bridge, and offline-map import.
///
/// Imported through the `providers.dart` barrel; nothing imports this file
/// directly.

// ── Reminders (hydration / mindfulness) ───────────────────────────────────

/// Shared local-notifications plugin instance. `initialize(...)` and timezone
/// setup (`tz.initializeTimeZones()`) are device bootstrap left for on-device.
final flutterLocalNotificationsProvider =
    Provider<FlutterLocalNotificationsPlugin>(
  (ref) => FlutterLocalNotificationsPlugin(),
);

/// The Android 13+ POST_NOTIFICATIONS gate, shared by every reminder's settings.
final reminderNotificationPermissionsProvider =
    Provider<ReminderNotificationPermissions>(
  (ref) => ReminderNotificationPermissions(
    ref.watch(flutterLocalNotificationsProvider),
  ),
);

final hydrationReminderDeviceProvider =
    Provider<LocalNotificationsReminderDevice>(
  (ref) => hydrationReminderDevice(ref.watch(flutterLocalNotificationsProvider)),
);

/// On Android an exact alarm wakes the app so the reminder can re-check today's
/// intake before notifying (the Kotlin model). Elsewhere there is no alarm
/// manager, so fall back to a notification scheduled ahead of time.
final hydrationReminderSchedulerProvider = Provider<ReminderScheduler>((ref) {
  if (defaultTargetPlatform == TargetPlatform.android) {
    return hydrationReminderAlarmScheduler;
  }
  return ZonedNotificationReminderScheduler(
    plugin: ref.watch(flutterLocalNotificationsProvider),
    spec: hydrationReminderNotificationSpec,
  );
});

final hydrationReminderControllerProvider =
    Provider<HydrationReminderController>((ref) {
  final plugin = ref.watch(flutterLocalNotificationsProvider);
  return HydrationReminderController(
    preferences: ref.watch(preferencesRepositoryProvider),
    hydrationRepository: ref.watch(hydrationRepositoryProvider),
    notifier: ref.watch(hydrationReminderDeviceProvider),
    scheduler: ref.watch(hydrationReminderSchedulerProvider),
    hasNotificationPermission: () => areReminderNotificationsEnabled(plugin),
  );
});

final mindfulnessReminderDeviceProvider =
    Provider<LocalNotificationsReminderDevice>(
  (ref) =>
      mindfulnessReminderDevice(ref.watch(flutterLocalNotificationsProvider)),
);

/// As with hydration: an exact alarm on Android so the reminder can re-check
/// today's mindful minutes before notifying; a scheduled notification elsewhere.
final mindfulnessReminderSchedulerProvider = Provider<ReminderScheduler>((ref) {
  if (defaultTargetPlatform == TargetPlatform.android) {
    return mindfulnessReminderAlarmScheduler;
  }
  return ZonedNotificationReminderScheduler(
    plugin: ref.watch(flutterLocalNotificationsProvider),
    spec: mindfulnessReminderNotificationSpec,
  );
});

final mindfulnessReminderControllerProvider =
    Provider<MindfulnessReminderController>((ref) {
  final plugin = ref.watch(flutterLocalNotificationsProvider);
  return MindfulnessReminderController(
    preferences: ref.watch(preferencesRepositoryProvider),
    mindfulnessRepository: ref.watch(mindfulnessRepositoryProvider),
    notifier: ref.watch(mindfulnessReminderDeviceProvider),
    scheduler: ref.watch(mindfulnessReminderSchedulerProvider),
    hasNotificationPermission: () => areReminderNotificationsEnabled(plugin),
  );
});

/// The mindfulness timer's bells + ambient loop. Silent off mobile, where there
/// is no audio host to speak of.
final mindfulnessSoundPlayerProvider = Provider<MindfulnessSoundPlayer>((ref) {
  final isMobile = defaultTargetPlatform == TargetPlatform.android ||
      defaultTargetPlatform == TargetPlatform.iOS;
  if (!isMobile) return const SilentMindfulnessSoundPlayer();
  final player = AudioMindfulnessSoundPlayer();
  ref.onDispose(player.dispose);
  return player;
});

// ── Home-screen widgets ───────────────────────────────────────────────────

final homeWidgetServiceProvider = Provider<HomeWidgetService>(
  (ref) => const HomeWidgetService(),
);

/// Pushes today's data to every placed home-screen widget. Foreground path: the
/// dashboard hands its merged data straight over (see `DashboardViewModel`); the
/// periodic path runs in the alarm isolate, which builds its own graph.
final homeWidgetRefresherProvider = Provider<HomeWidgetRefresher>((ref) {
  final preferences = ref.watch(preferencesRepositoryProvider);
  return HomeWidgetRefresher(
    service: ref.watch(homeWidgetServiceProvider),
    health: ref.watch(healthRepositoryProvider),
    loadDashboardDay: ref.watch(loadDashboardDayUseCaseProvider),
    // Built here rather than watched off `unitFormatterProvider` (which lives in
    // the shell's `app_providers`, and would make this low-level DI file import
    // it back): the closure reads the preference live, so a unit-system change
    // is picked up by the next push either way.
    unitFormatter:
        UnitFormatter(unitSystemProvider: () => preferences.unitSystem),
    localizations: homeWidgetLocalizations(),
    goals: homeWidgetReadinessGoals(preferences),
    sleepRangeMode: preferences.sleepRangeMode,
    activityWeekMode: preferences.activityWeekMode,
  );
});

// ── Offline maps ──────────────────────────────────────────────────────────

/// Import controller for offline map packs. Async because it resolves the
/// app's private maps directory; the metadata is persisted in SharedPreferences.
final offlineMapImportControllerProvider =
    FutureProvider<OfflineMapImportController>((ref) async {
  final documentsDir = await getApplicationDocumentsDirectory();
  final mapsDirectoryPath = p.join(documentsDir.path, 'offline_maps');
  final metadataStore = OfflineMapMetadataStore.sharedPreferences(
    ref.watch(sharedPreferencesProvider),
    mapsDirectoryPath,
  );
  return OfflineMapImportController(
    metadataStore: metadataStore,
    mapsDirectoryPath: mapsDirectoryPath,
  );
});
