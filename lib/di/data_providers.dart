import '../domain/model/vitals_models.dart';
import '../domain/model/body_models.dart';
import '../domain/model/ble_sensor_models.dart';
import '../devices/core/ble/ble_sensor_coordinator.dart';
import '../devices/core/ble/ble_watch_pairing.dart';
import '../devices/garmin/garmin_device_state_store.dart';
import '../devices/garmin/garmin_file_store.dart';
import '../devices/garmin/garmin_gatt_probe.dart';
import '../devices/garmin/garmin_phone_identity.dart';
import '../devices/garmin/garmin_watch_sync_service.dart';
import '../data/repository/contract/ble_sensor_repository.dart';
import '../domain/port/garmin_transport_probe.dart';
import '../domain/port/watch_pairing_port.dart';
import 'dart:io';

import 'package:drift/drift.dart';
import 'package:flutter/foundation.dart';
import 'package:drift/native.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../data/local/beverage/beverage_store.dart';
import '../data/local/open_vitals_database.dart';
import '../data/sync/calories_history_sync_service.dart';
import '../data/sync/vitals_history_sync_service.dart';
import '../data/prefs/preferences_repository.dart';
import '../data/repository/body_energy_timeline_cache_store.dart';
import '../data/repository/contract/activity_repository.dart';
import '../data/repository/contract/apple_health_import_repository.dart';
import '../data/repository/contract/ble_device_repository.dart';
import '../data/repository/contract/body_energy_feel_check_repository.dart';
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
import '../data/repository/impl/activity_marker_repository_impl.dart';
import '../data/repository/impl/activity_repository_impl.dart';
import '../data/repository/impl/apple_health_import_repository_impl.dart';
import '../data/repository/impl/ble_device_repository_impl.dart';
import '../data/repository/impl/body_energy_feel_check_repository_impl.dart';
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
import '../features/imports/applehealth/apple_health_import_report_store.dart';
import '../features/imports/applehealth/apple_health_import_service.dart';
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

final feelCheckDaoProvider = Provider<FeelCheckDao>(
  (ref) => ref.watch(openVitalsDatabaseProvider).feelCheckDao,
);

final vitalsDailyCacheDaoProvider = Provider<VitalsDailyCacheDao>(
  (ref) => ref.watch(openVitalsDatabaseProvider).vitalsDailyCacheDao,
);

/// Stress + Body Battery from a Garmin watch. These have no Health Connect type,
/// so this table is their system of record rather than a cache.
final garminWellnessDaoProvider = Provider<GarminWellnessDao>(
  (ref) => ref.watch(openVitalsDatabaseProvider).garminWellnessDao,
);

final vitalsHistorySyncServiceProvider = Provider<VitalsHistorySyncService>(
  (ref) => VitalsHistorySyncService(
    ref.watch(vitalsDailyCacheDaoProvider),
    ref.watch(healthDataSourceProvider),
  ),
);

final caloriesHistorySyncServiceProvider =
    Provider<CaloriesHistorySyncService>(
  (ref) => CaloriesHistorySyncService(
    ref.watch(vitalsDailyCacheDaoProvider),
    ref.watch(healthDataSourceProvider),
  ),
);

final bodyEnergyFeelCheckRepositoryProvider =
    Provider<BodyEnergyFeelCheckRepository>(
      (ref) => BodyEnergyFeelCheckRepositoryImpl(
        feelCheckDao: ref.watch(feelCheckDaoProvider),
        preferencesRepository: ref.watch(preferencesRepositoryProvider),
      ),
    );

final preferencesRepositoryProvider = Provider<PreferencesRepository>(
  (ref) {
    final repo = PreferencesRepository(ref.watch(sharedPreferencesProvider));
    ref.onDispose(repo.dispose);
    return repo;
  },
);

final beverageStoreProvider = Provider<BeverageStore>(
  (ref) => BeverageStore(
    ref.watch(beverageDaoProvider),
    ref.watch(preferencesRepositoryProvider),
  ),
);

final bodyEnergyTimelineCacheStoreProvider =
    Provider<BodyEnergyTimelineCacheStore>(
      (ref) =>
          BodyEnergyTimelineCacheStore(ref.watch(sharedPreferencesProvider)),
    );

final healthDataSourceProvider = Provider<HealthDataSource>((ref) {
  if (defaultTargetPlatform == TargetPlatform.android) {
    return HealthConnectNativeDataSource(
      appPackageName: openVitalsPackageName,
      mindfulnessIntegrationEnabled: () => ref
          .read(preferencesRepositoryProvider)
          .healthConnectMindfulnessEnabled,
    );
  }
  // iOS / other platforms have no native health bridge yet.
  return UnsupportedHealthDataSource(appPackageName: openVitalsPackageName);
});

/// The data layer's object graph: bootstrap singletons (preferences,
/// drift, the Health Connect data source) and the repositories, each bound
/// from its `contract/` type to its `impl/` instance — the seam every test
/// overrides.
///
/// Imported through the `providers.dart` barrel; nothing imports this file
/// directly.

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
  (ref) => VitalsRepositoryImpl(
    ref.watch(healthDataSourceProvider),
    cacheDao: ref.watch(vitalsDailyCacheDaoProvider),
  ),
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
    caloriesCacheDao: ref.watch(vitalsDailyCacheDaoProvider),
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

/// A watch's Garmin-specific per-device state (declared GFDI capabilities +
/// which files a sync already pulled), kept out of [bleDeviceRepositoryProvider]
/// so that registry carries no Garmin knowledge.
final garminDeviceStateStoreProvider = Provider<GarminDeviceStateStore>(
  (ref) => GarminDeviceStateStore(ref.watch(sharedPreferencesProvider)),
);

/// Bonding + companion association for Garmin watch onboarding. A provider so a
/// widget test can substitute one and never touch a radio.
final watchPairingPortProvider = Provider<WatchPairingPort>(
  (ref) => BleWatchPairing(),
);

/// Reads a bonded watch's GATT map to decide which GFDI transport it speaks.
final garminTransportProbeProvider = Provider<GarminTransportProbe>(
  (ref) => const GarminGattProbe(),
);

/// Drives one end-to-end GFDI sync (link, session, downloaded files).
final garminWatchSyncServiceProvider = Provider<GarminWatchSyncService>(
  (ref) => GarminWatchSyncService(
    fileStore: GarminFileStore(
      resolveDirectory: () async => Directory(
        p.join((await getApplicationDocumentsDirectory()).path, 'garmin'),
      ),
    ),
  ),
);

/// How this phone names itself to a watch. See [GarminPhoneIdentity] for why
/// these are constants rather than a device-info lookup.
final phoneIdentityProvider = Provider<GarminPhoneIdentity>(
  (ref) => const GarminPhoneIdentity(),
);

final appleHealthImportRepositoryProvider =
    Provider<AppleHealthImportRepository>(
      (ref) =>
          AppleHealthImportRepositoryImpl(ref.watch(healthDataSourceProvider)),
    );

final appleHealthImportServiceProvider = Provider<AppleHealthImportService>(
  (ref) =>
      AppleHealthImportService(ref.watch(appleHealthImportRepositoryProvider)),
);

final appleHealthImportReportStoreProvider =
    Provider<AppleHealthImportReportStore>(
      (ref) => AppleHealthImportReportStore(),
    );

final bodyEnergyRepositoryProvider = Provider<BodyEnergyRepository>(
  (ref) => BodyEnergyRepositoryImpl(
    heartRepository: ref.watch(heartRepositoryProvider),
    sleepRepository: ref.watch(sleepRepositoryProvider),
    activityRepository: ref.watch(activityRepositoryProvider),
    vitalsRepository: ref.watch(vitalsRepositoryProvider),
    bodyRepository: ref.watch(bodyRepositoryProvider),
    healthRepository: ref.watch(healthRepositoryProvider),
    preferencesRepository: ref.watch(preferencesRepositoryProvider),
    cacheStore: ref.watch(bodyEnergyTimelineCacheStoreProvider),
  ),
);

// ── BLE sensors ───────────────────────────────────────────────────────────

/// The app-lifetime BLE coordinator (Kotlin `@Singleton`), bound to its
/// contract so features never name the service class.
final bleSensorRepositoryProvider = Provider<BleSensorRepository>((ref) {
  final coordinator = BleSensorCoordinator(
    ref.watch(bleDeviceRepositoryProvider),
  );
  ref.onDispose(coordinator.dispose);
  return coordinator;
});

/// Live recording metrics (Kotlin `StateFlow<BleRecordingMetrics>`).
final bleMetricsProvider = StreamProvider<BleRecordingMetrics>((ref) {
  return ref.watch(bleSensorRepositoryProvider).metricsStream;
});

/// Live scan results (Kotlin `StateFlow<List<BleDiscoveredDevice>>`).
final bleDiscoveredDevicesProvider = StreamProvider<List<BleDiscoveredDevice>>((
  ref,
) {
  return ref.watch(bleSensorRepositoryProvider).discoveredDevicesStream;
});

/// The paired BLE sensor registry as a live list, seeded with the current
/// snapshot (Kotlin `StateFlow<List<BleSensorDevice>>`).
final bleDevicesProvider = StreamProvider<List<BleSensorDevice>>((ref) async* {
  final repository = ref.watch(bleDeviceRepositoryProvider);
  yield repository.devices;
  yield* repository.devicesStream;
});

// ── Permission sets ───────────────────────────────────────────────────────
//
// The write-permission sets a screen hands to `HealthConnectGate`. They are
// synchronous, cached, device-filtered constants — but they still come OUT of
// a repository, and a widget must not hold a repository. It watches one of
// these instead.

final mindfulnessWritePermissionsProvider = Provider<Set<String>>(
  (ref) => ref.watch(mindfulnessRepositoryProvider).mindfulnessWritePermissions,
);

final nutritionWritePermissionsProvider = Provider<Set<String>>(
  (ref) => ref.watch(nutritionRepositoryProvider).nutritionWritePermissions,
);

final bodyWritePermissionsProvider =
    Provider.family<Set<String>, BodyMeasurementType>(
      (ref, type) =>
          ref.watch(bodyRepositoryProvider).bodyWritePermissions(type),
    );

final vitalsWritePermissionsProvider =
    Provider.family<Set<String>, VitalsMeasurementType>(
      (ref, type) =>
          ref.watch(vitalsRepositoryProvider).vitalsWritePermissions(type),
    );

/// Every permission OpenVitals manages, and whether the device can store a
/// mindfulness session at all — what the add-entry picker needs to decide which
/// tiles it may offer.
final managedHealthPermissionsProvider = Provider<Set<String>>(
  (ref) => ref.watch(healthRepositoryProvider).managedPermissions,
);

final mindfulnessAvailableProvider = Provider<bool>(
  (ref) => ref.watch(healthRepositoryProvider).isMindfulnessAvailable(),
);
