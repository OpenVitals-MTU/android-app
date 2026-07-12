import 'dart:io';

import 'package:drift/drift.dart';
import 'package:flutter/foundation.dart';
import 'package:drift/native.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../core/presentation/unit_formatter.dart';
import '../core/reminders/local_notifications_reminder_device.dart';
import '../features/manualentry/mindfulness/mindfulness_sound_player.dart';
import '../features/hydration/reminders/hydration_reminder_alarm.dart';
import '../features/mindfulness/reminders/mindfulness_reminder_alarm.dart';
import '../core/reminders/reminder_notifications.dart';
import '../core/reminders/reminder_controller.dart';
import '../data/local/beverage/beverage_store.dart';
import '../data/local/open_vitals_database.dart';
import '../data/prefs/preferences_repository.dart';
import '../data/repository/body_energy_timeline_cache_store.dart';
import '../data/repository/contract/activity_repository.dart';
import '../data/repository/contract/apple_health_import_repository.dart';
import '../data/repository/contract/ble_device_repository.dart';
import '../data/repository/contract/body_energy_repository.dart';
import '../data/repository/contract/body_repository.dart';
import '../data/repository/contract/caffeine_repository.dart';
import '../data/repository/contract/cycle_repository.dart';
import '../data/repository/contract/health_repository.dart';
import '../data/repository/contract/heart_repository.dart';
import '../data/repository/contract/hydration_repository.dart';
import '../data/repository/contract/mindfulness_repository.dart';
import '../data/repository/contract/nutrition_repository.dart';
import '../data/repository/contract/sleep_repository.dart';
import '../data/repository/contract/vitals_repository.dart';
import '../data/repository/dashboard/dashboard_data_loader.dart';
import '../data/repository/impl/activity_marker_repository_impl.dart';
import '../data/repository/impl/activity_repository_impl.dart';
import '../data/repository/impl/apple_health_import_repository_impl.dart';
import '../data/repository/impl/ble_device_repository_impl.dart';
import '../data/repository/impl/body_energy_repository_impl.dart';
import '../data/repository/impl/body_repository_impl.dart';
import '../data/repository/impl/caffeine_repository_impl.dart';
import '../data/repository/impl/cycle_repository_impl.dart';
import '../data/repository/impl/health_repository_impl.dart';
import '../data/repository/impl/heart_repository_impl.dart';
import '../data/repository/impl/hydration_repository_impl.dart';
import '../data/repository/impl/mindfulness_repository_impl.dart';
import '../data/repository/impl/nutrition_repository_impl.dart';
import '../data/repository/impl/sleep_repository_impl.dart';
import '../data/repository/impl/vitals_repository_impl.dart';
import '../domain/usecase/load_dashboard_day_use_case.dart';
import '../features/imports/applehealth/apple_health_import_report_store.dart';
import '../features/imports/applehealth/apple_health_import_service.dart';
import '../domain/usecase/delete_activity_entry_use_case.dart';
import '../domain/usecase/delete_body_measurement_entry_use_case.dart';
import '../domain/usecase/discover_ble_device_capabilities_use_case.dart';
import '../domain/usecase/save_body_measurement_use_case.dart';
import '../domain/usecase/save_carbs_entry_use_case.dart';
import '../domain/usecase/save_hydration_entry_use_case.dart';
import '../domain/usecase/save_mindfulness_session_use_case.dart';
import '../domain/usecase/save_vitals_measurement_use_case.dart';
import '../data/source/sensors/ble/ble_sensor_coordinator.dart';
import '../domain/usecase/load_achievement_history_use_case.dart';
import '../domain/usecase/load_activities_use_case.dart';
import '../domain/usecase/load_activity_detail_use_case.dart';
import '../domain/usecase/load_activity_metric_period_use_case.dart';
import '../domain/usecase/load_body_energy_timeline_use_case.dart';
import '../domain/usecase/load_body_period_use_case.dart';
import '../domain/usecase/load_caffeine_use_case.dart';
import '../domain/usecase/load_calories_use_case.dart';
import '../domain/usecase/load_cardio_load_detail_use_case.dart';
import '../domain/usecase/load_cycle_period_use_case.dart';
import '../domain/usecase/load_frequent_hydration_drinks_use_case.dart';
import '../domain/usecase/load_heart_period_use_case.dart';
import '../domain/usecase/load_hydration_period_use_case.dart';
import '../domain/usecase/load_mindfulness_period_use_case.dart';
import '../domain/usecase/load_nutrition_period_use_case.dart';
import '../domain/usecase/load_recovery_days_use_case.dart';
import '../domain/usecase/load_sleep_detail_use_case.dart';
import '../domain/usecase/load_sleep_period_use_case.dart';
import '../features/activity/maps/offline_map_import_controller.dart';
import '../features/activity/maps/offline_map_metadata_store.dart';
import '../features/homewidgets/home_widget_refresher.dart';
import '../features/homewidgets/home_widget_service.dart';
import '../features/hydration/reminders/hydration_reminder_controller.dart';
import '../features/hydration/reminders/hydration_reminder_device.dart';
import '../features/mindfulness/reminders/mindfulness_reminder_controller.dart';
import '../features/mindfulness/reminders/mindfulness_reminder_device.dart';
import '../data/source/health/health_data_source.dart';
import '../data/source/health/native/health_connect_native_data_source.dart';
import '../data/source/health/unsupported_health_data_source.dart';

/// Riverpod DI graph, replacing the Hilt `AppModule` / `RepositoryModule`.
///
/// [sharedPreferencesProvider] must be overridden at app startup with a resolved
/// [SharedPreferences] instance (the standard Riverpod bootstrap pattern), e.g.:
///
/// ```dart
/// final prefs = await SharedPreferences.getInstance();
/// runApp(ProviderScope(
///   overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
///   child: const App(),
/// ));
/// ```

/// OpenVitals app package/bundle id, used for OpenVitals-record ownership
/// tagging (mirrors the Kotlin `context.packageName`).
const String openVitalsPackageName = 'tech.mmarca.openvitals';

final sharedPreferencesProvider = Provider<SharedPreferences>(
  (ref) => throw UnimplementedError(
    'sharedPreferencesProvider must be overridden at app startup',
  ),
);

QueryExecutor _openDatabaseConnection() => LazyDatabase(() async {
      final dir = await getApplicationDocumentsDirectory();
      final file = File(p.join(dir.path, 'openvitals.db'));
      return NativeDatabase.createInBackground(file);
    });

final openVitalsDatabaseProvider = Provider<OpenVitalsDatabase>((ref) {
  final db = OpenVitalsDatabase(_openDatabaseConnection());
  ref.onDispose(db.close);
  return db;
});

final beverageDaoProvider = Provider<BeverageDao>(
  (ref) => ref.watch(openVitalsDatabaseProvider).beverageDao,
);

final preferencesRepositoryProvider = Provider<PreferencesRepository>(
  (ref) => PreferencesRepository(ref.watch(sharedPreferencesProvider)),
);

final beverageStoreProvider = Provider<BeverageStore>(
  (ref) => BeverageStore(
    ref.watch(beverageDaoProvider),
    ref.watch(preferencesRepositoryProvider),
  ),
);

final bodyEnergyTimelineCacheStoreProvider =
    Provider<BodyEnergyTimelineCacheStore>(
  (ref) => BodyEnergyTimelineCacheStore(ref.watch(sharedPreferencesProvider)),
);

final healthDataSourceProvider = Provider<HealthDataSource>((ref) {
  if (defaultTargetPlatform == TargetPlatform.android) {
    return HealthConnectNativeDataSource(appPackageName: openVitalsPackageName);
  }
  // iOS / other platforms have no native health bridge yet.
  return UnsupportedHealthDataSource(appPackageName: openVitalsPackageName);
});

// ── Repositories (contract → impl) ────────────────────────────────────────

final healthRepositoryProvider = Provider<HealthRepository>(
  (ref) => HealthRepositoryImpl(ref.watch(healthDataSourceProvider)),
);

final heartRepositoryProvider = Provider<HeartRepository>(
  (ref) => HeartRepositoryImpl(ref.watch(healthDataSourceProvider)),
);

final sleepRepositoryProvider = Provider<SleepRepository>(
  (ref) => SleepRepositoryImpl(ref.watch(healthDataSourceProvider)),
);

final bodyRepositoryProvider = Provider<BodyRepository>(
  (ref) => BodyRepositoryImpl(ref.watch(healthDataSourceProvider)),
);

final vitalsRepositoryProvider = Provider<VitalsRepository>(
  (ref) => VitalsRepositoryImpl(ref.watch(healthDataSourceProvider)),
);

final nutritionRepositoryProvider = Provider<NutritionRepository>(
  (ref) => NutritionRepositoryImpl(ref.watch(healthDataSourceProvider)),
);

final caffeineRepositoryProvider = Provider<CaffeineRepository>(
  (ref) => CaffeineRepositoryImpl(ref.watch(nutritionRepositoryProvider)),
);

final mindfulnessRepositoryProvider = Provider<MindfulnessRepository>(
  (ref) => MindfulnessRepositoryImpl(ref.watch(healthDataSourceProvider)),
);

final cycleRepositoryProvider = Provider<CycleRepository>(
  (ref) => CycleRepositoryImpl(ref.watch(healthDataSourceProvider)),
);

final activityMarkerRepositoryProvider = Provider<ActivityMarkerRepository>(
  (ref) => ActivityMarkerRepositoryImpl(ref.watch(sharedPreferencesProvider)),
);

final activityRepositoryProvider = Provider<ActivityRepository>(
  (ref) => ActivityRepositoryImpl(
    ref.watch(healthDataSourceProvider),
    preferencesRepository: ref.watch(preferencesRepositoryProvider),
    markerRepository: ref.watch(activityMarkerRepositoryProvider),
  ),
);

final hydrationRepositoryProvider = Provider<HydrationRepository>(
  (ref) => HydrationRepositoryImpl(
    ref.watch(healthDataSourceProvider),
    preferencesRepository: ref.watch(preferencesRepositoryProvider),
    beverageStore: ref.watch(beverageStoreProvider),
  ),
);

final bleDeviceRepositoryProvider = Provider<BleDeviceRepository>(
  (ref) => BleDeviceRepositoryImpl(ref.watch(sharedPreferencesProvider)),
);

final appleHealthImportRepositoryProvider =
    Provider<AppleHealthImportRepository>(
  (ref) => AppleHealthImportRepositoryImpl(ref.watch(healthDataSourceProvider)),
);

final appleHealthImportServiceProvider = Provider<AppleHealthImportService>(
  (ref) =>
      AppleHealthImportService(ref.watch(appleHealthImportRepositoryProvider)),
);

final appleHealthImportReportStoreProvider =
    Provider<AppleHealthImportReportStore>(
  (ref) => AppleHealthImportReportStore(ref.watch(sharedPreferencesProvider)),
);

final bodyEnergyRepositoryProvider = Provider<BodyEnergyRepository>(
  (ref) => BodyEnergyRepositoryImpl(
    heartRepository: ref.watch(heartRepositoryProvider),
    sleepRepository: ref.watch(sleepRepositoryProvider),
    activityRepository: ref.watch(activityRepositoryProvider),
    vitalsRepository: ref.watch(vitalsRepositoryProvider),
    healthRepository: ref.watch(healthRepositoryProvider),
    preferencesRepository: ref.watch(preferencesRepositoryProvider),
    cacheStore: ref.watch(bodyEnergyTimelineCacheStoreProvider),
  ),
);

// ── Read orchestrator + use cases ─────────────────────────────────────────

final dashboardDataLoaderProvider = Provider<DashboardDataLoader>(
  (ref) => DashboardDataLoader(
    ref.watch(healthDataSourceProvider),
    preferencesRepository: ref.watch(preferencesRepositoryProvider),
    bodyEnergyRepository: ref.watch(bodyEnergyRepositoryProvider),
  ),
);

final loadDashboardDayUseCaseProvider = Provider<LoadDashboardDayUseCase>(
  (ref) => LoadDashboardDayUseCase(ref.watch(dashboardDataLoaderProvider)),
);

final loadHeartPeriodUseCaseProvider = Provider<LoadHeartPeriodUseCase>(
  (ref) => LoadHeartPeriodUseCase(
    ref.watch(heartRepositoryProvider),
    ref.watch(vitalsRepositoryProvider),
  ),
);

final loadActivityDetailUseCaseProvider = Provider<LoadActivityDetailUseCase>(
  (ref) => LoadActivityDetailUseCase(
    ref.watch(activityRepositoryProvider),
    ref.watch(heartRepositoryProvider),
  ),
);

final loadActivitiesUseCaseProvider = Provider<LoadActivitiesUseCase>(
  (ref) => LoadActivitiesUseCase(
    ref.watch(activityRepositoryProvider),
    ref.watch(heartRepositoryProvider),
  ),
);

final deleteActivityEntryUseCaseProvider = Provider<DeleteActivityEntryUseCase>(
  (ref) => DeleteActivityEntryUseCase(ref.watch(activityRepositoryProvider)),
);

final loadActivityMetricPeriodUseCaseProvider =
    Provider<LoadActivityMetricPeriodUseCase>(
  (ref) => LoadActivityMetricPeriodUseCase(ref.watch(activityRepositoryProvider)),
);

final loadCaloriesUseCaseProvider = Provider<LoadCaloriesUseCase>(
  (ref) => LoadCaloriesUseCase(
    ref.watch(activityRepositoryProvider),
    ref.watch(bodyRepositoryProvider),
  ),
);

final loadCardioLoadDetailUseCaseProvider =
    Provider<LoadCardioLoadDetailUseCase>(
  (ref) => LoadCardioLoadDetailUseCase(
    ref.watch(activityRepositoryProvider),
    ref.watch(heartRepositoryProvider),
  ),
);

final loadSleepPeriodUseCaseProvider = Provider<LoadSleepPeriodUseCase>(
  (ref) => LoadSleepPeriodUseCase(
    ref.watch(sleepRepositoryProvider),
    ref.watch(heartRepositoryProvider),
  ),
);

final loadAchievementHistoryUseCaseProvider =
    Provider<LoadAchievementHistoryUseCase>(
  (ref) => LoadAchievementHistoryUseCase(ref.watch(activityRepositoryProvider)),
);

final loadBodyPeriodUseCaseProvider = Provider<LoadBodyPeriodUseCase>(
  (ref) => LoadBodyPeriodUseCase(ref.watch(bodyRepositoryProvider)),
);

final deleteBodyMeasurementEntryUseCaseProvider =
    Provider<DeleteBodyMeasurementEntryUseCase>(
  (ref) => DeleteBodyMeasurementEntryUseCase(ref.watch(bodyRepositoryProvider)),
);

final loadBodyEnergyTimelineUseCaseProvider =
    Provider<LoadBodyEnergyTimelineUseCase>(
  (ref) =>
      LoadBodyEnergyTimelineUseCase(ref.watch(bodyEnergyRepositoryProvider)),
);

final loadCaffeineUseCaseProvider = Provider<LoadCaffeineUseCase>(
  (ref) => LoadCaffeineUseCase(ref.watch(caffeineRepositoryProvider)),
);

final loadCyclePeriodUseCaseProvider = Provider<LoadCyclePeriodUseCase>(
  (ref) => LoadCyclePeriodUseCase(ref.watch(cycleRepositoryProvider)),
);

final loadHydrationPeriodUseCaseProvider = Provider<LoadHydrationPeriodUseCase>(
  (ref) => LoadHydrationPeriodUseCase(
    ref.watch(hydrationRepositoryProvider),
    ref.watch(nutritionRepositoryProvider),
  ),
);

final loadFrequentHydrationDrinksUseCaseProvider =
    Provider<LoadFrequentHydrationDrinksUseCase>(
  (ref) => LoadFrequentHydrationDrinksUseCase(
    ref.watch(hydrationRepositoryProvider),
    ref.watch(nutritionRepositoryProvider),
  ),
);

final loadMindfulnessPeriodUseCaseProvider =
    Provider<LoadMindfulnessPeriodUseCase>(
  (ref) => LoadMindfulnessPeriodUseCase(ref.watch(mindfulnessRepositoryProvider)),
);

final loadNutritionPeriodUseCaseProvider = Provider<LoadNutritionPeriodUseCase>(
  (ref) => LoadNutritionPeriodUseCase(ref.watch(nutritionRepositoryProvider)),
);

final loadRecoveryDaysUseCaseProvider = Provider<LoadRecoveryDaysUseCase>(
  (ref) => LoadRecoveryDaysUseCase(ref.watch(sleepRepositoryProvider)),
);

final loadSleepDetailUseCaseProvider = Provider<LoadSleepDetailUseCase>(
  (ref) => LoadSleepDetailUseCase(ref.watch(sleepRepositoryProvider)),
);

final discoverBleDeviceCapabilitiesUseCaseProvider =
    Provider<DiscoverBleDeviceCapabilitiesUseCase>(
  (ref) => DiscoverBleDeviceCapabilitiesUseCase(
    ref.watch(bleSensorCoordinatorProvider),
    ref.watch(bleDeviceRepositoryProvider),
  ),
);

// ── Write use cases ───────────────────────────────────────────────────────

final saveBodyMeasurementUseCaseProvider = Provider<SaveBodyMeasurementUseCase>(
  (ref) => SaveBodyMeasurementUseCase(ref.watch(bodyRepositoryProvider)),
);

final saveVitalsMeasurementUseCaseProvider =
    Provider<SaveVitalsMeasurementUseCase>(
  (ref) => SaveVitalsMeasurementUseCase(ref.watch(vitalsRepositoryProvider)),
);

final saveCarbsEntryUseCaseProvider = Provider<SaveCarbsEntryUseCase>(
  (ref) => SaveCarbsEntryUseCase(ref.watch(nutritionRepositoryProvider)),
);

final saveMindfulnessSessionUseCaseProvider =
    Provider<SaveMindfulnessSessionUseCase>(
  (ref) =>
      SaveMindfulnessSessionUseCase(ref.watch(mindfulnessRepositoryProvider)),
);

final saveHydrationEntryUseCaseProvider = Provider<SaveHydrationEntryUseCase>(
  (ref) => SaveHydrationEntryUseCase(
    ref.watch(hydrationRepositoryProvider),
    ref.watch(nutritionRepositoryProvider),
  ),
);

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
/// dashboard hands its merged data straight over (see `DashboardNotifier`); the
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
