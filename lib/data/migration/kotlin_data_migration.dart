import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:path/path.dart' as p;
import 'package:shared_preferences/shared_preferences.dart';

import '../../core/period/period_range_preference_key.dart';
import '../../core/period/time_range.dart';
import '../../domain/model/dashboard_query.dart';
import '../../domain/preferences/activity_week_mode.dart';
import '../../domain/preferences/app_language.dart';
import '../../domain/preferences/app_theme_mode.dart';
import '../../domain/preferences/caffeine_preferences.dart';
import '../../domain/preferences/sleep_range_mode.dart';
import '../../domain/preferences/unit_system.dart';
import '../../features/activity/maps/offline_map_metadata_store.dart';
import '../../features/homewidgets/home_widget_service.dart';
import 'legacy_data_source.dart';

/// One-shot flag: set once the migration has run, whatever the outcome.
///
/// It is written even when a step failed. A migration that keeps failing must
/// not retry on every launch — the legacy data is not going to become readable,
/// and each retry would cost startup time forever.
const String kotlinDataMigratedKey = 'kotlin_data_migrated';

/// The Kotlin `PreferencesRepository.PREFS_FILE`. Its ~95 keys are named
/// identically in the Dart port, which is a 1:1 port of the same repository.
const String _legacyPrefsName = 'openvitals_prefs';

/// The Kotlin `BleDeviceRepository.PREFS_FILE`, whose single `devices` key holds
/// the paired-sensor JSON array.
const String _legacyBlePrefsName = 'ble_sensor_devices';

/// Kotlin's key inside that file. Dart flattens the same JSON into *one* key
/// named after the file — see `BleDeviceRepositoryImpl._key`. This is the only
/// key in the whole migration whose name changes.
const String _legacyBleDevicesKey = 'devices';
const String _bleDevicesKey = 'ble_sensor_devices';

/// The Kotlin `ActivityMarkerRepository.PreferencesName`. Keys are
/// `activity_markers_<activityId>`; the Dart port keeps the names verbatim.
const String _legacyMarkerPrefsName = 'activity_marker_metadata';
const String _legacyMarkerKeyPrefix = 'activity_markers_';

/// Kotlin `OfflineMapRepository.MapsDirectoryName` / `MetadataFileName`.
const String _offlineMapsDirName = 'offline_maps';
const String _offlineMapsMetadataFileName = 'metadata.json';

/// Kotlin `HomeMetricWidgetSelection` / `HomeQuickBeverageWidgetSelection`: one
/// preferences file each, keyed `metric_id_<appWidgetId>` / `drink_id_<appWidgetId>`.
const String _legacyMetricWidgetPrefsName = 'home_metric_widgets';
const String _legacyMetricWidgetKeyPrefix = 'metric_id_';
const String _legacyBeverageWidgetPrefsName = 'home_quick_beverage_widgets';
const String _legacyBeverageWidgetKeyPrefix = 'drink_id_';

/// The database file name, identical on both sides. Room wrote it to
/// `databases/`; drift reads it from the documents directory, so only the
/// directory differs — the schema is byte-compatible (Room `version = 3`,
/// drift `schemaVersion = 3`, one `beverages` table with the same columns).
const String _databaseName = 'openvitals.db';

/// Migrates the Kotlin app's local data into the Flutter app's stores, once.
///
/// The Flutter app ships as an in-place update of the Kotlin one (same
/// `applicationId`, same certificate), so `/data/data/tech.mmarca.openvitals/`
/// survives the update — but Flutter reads different files. Without this, a
/// long-time user's goals, custom drinks, caffeine profile, paired BLE sensors,
/// reminders, activity-marker notes and offline map packs all silently reset.
///
/// Health Connect data is *external* to the app and is unaffected; it is
/// deliberately out of scope here.
///
/// ## Deliberately not migrated
///
/// * `body_energy_timeline_cache` — a derived, rebuildable cache whose entries
///   are keyed by a JVM `String.hashCode()` signature that Dart cannot
///   reproduce. Reading it would be worse than dropping it: a stale entry that
///   Dart can never invalidate.
/// * `activity_recording` + `files/activity_recording_points.csv` — an in-flight
///   recording. Transient by nature, and the Flutter recorder has no CSV
///   sidecar to restore it into.
/// * `files/crash_reports/`, `files/import_reports/` — derived artefacts.
/// * `files/apple_health_import/` — already at the path Flutter reads.
///
/// ## Guarantees
///
/// Never throws. It runs on the startup path, before the Riverpod container
/// exists, and a failure here must not brick the app: every step is guarded on
/// its own, so one bad step cannot cost the others, and the one-shot flag is set
/// regardless of outcome.
///
/// Writes go through the [SharedPreferences] API rather than the native side, so
/// the plugin owns its own value encoding (it stores a `double` and a
/// `List<String>` as *prefixed strings*, an internal detail that has already
/// changed between plugin versions). Pass the same [prefs] instance the app will
/// use, so its in-memory cache reflects these writes.
Future<void> migrateKotlinDataIfNeeded({
  required SharedPreferences prefs,
  required LegacyDataSource native,
  required Directory documentsDir,
  HomeWidgetClient? widgets,
}) async {
  try {
    if (!await _shouldMigrate(prefs, native)) return;
  } catch (error, stack) {
    debugPrint('Kotlin migration: precondition check failed: $error\n$stack');
    return;
  }

  await _step('preferences', () => _migratePreferences(prefs, native));
  await _step('BLE devices', () => _migrateBleDevices(prefs, native));
  await _step('activity markers', () => _migrateActivityMarkers(prefs, native));
  await _step('database', () => _migrateDatabase(native, documentsDir));
  await _step('offline maps', () => _migrateOfflineMaps(prefs, native, documentsDir));
  await _step('home widgets', () => _migrateHomeWidgets(native, widgets));

  await _step(
    'completion flag',
    () => prefs.setBool(kotlinDataMigratedKey, true),
  );
}

/// All three conditions must hold.
///
/// The third is the anti-clobber guard, and it is the subtle one: a tester may
/// already have *used* the Flutter build (a nightly, a sideload) on a device
/// that still holds Kotlin data. Overwriting their current settings with stale
/// Kotlin ones would be a regression, not a migration. `onboarding_done` is the
/// earliest thing the Flutter app writes for a real user, so its absence is the
/// signal that this side is still virgin.
Future<bool> _shouldMigrate(
  SharedPreferences prefs,
  LegacyDataSource native,
) async {
  if (prefs.getBool(kotlinDataMigratedKey) ?? false) return false;
  if (prefs.containsKey('onboarding_done')) return false;
  return native.hasLegacyData();
}

/// Runs one migration step, swallowing (but reporting) any failure.
Future<void> _step(String name, Future<void> Function() body) async {
  try {
    await body();
  } catch (error, stack) {
    debugPrint('Kotlin migration: $name failed: $error\n$stack');
  }
}

/// The main preferences file. Key names are identical on both sides — the Dart
/// repository is a 1:1 port — so this is a type-preserving copy, with no ~95-entry
/// key table to fall out of sync.
///
/// Types line up with what the Dart `PreferencesRepository` reads back: Kotlin
/// `putFloat` → `getDouble` (goals, body/caffeine weight, hydration goal),
/// `putStringSet` → `getStringList` (`acknowledged_permissions`,
/// `custom_hydration_drinks`, `hydration_container_volume_milliliters`),
/// `putLong` → `getInt` (`privacy_policy_accepted_at`; Dart's `int` is 64-bit),
/// and `Int`/`Boolean`/`String` unchanged. The dynamic keys ride along for free:
/// the 14 `goal_*` keys, `acknowledged_feature_permissions_*` and the period
/// ranges are copied by value, not by name.
///
/// **Values, however, are not all portable.** Where a value is an enum, the two
/// apps persist it differently — Kotlin wrote `Enum.name` (`METRIC`, `DARK`,
/// `ORAL_CONTRACEPTIVE`), while the Dart port writes Dart's `.name`
/// (`metric`, `dark`, `oralContraceptive`) and parses it back with an exact
/// `e.name ==` match. Copying those verbatim would not fail loudly; it would
/// leave the Dart side unable to parse the value and silently fall back to its
/// default, which for `unit_system` means a user's choice quietly reverting to
/// whatever their locale implies. [_transcode] converts them; anything it cannot
/// map is skipped and logged rather than written as garbage.
Future<void> _migratePreferences(
  SharedPreferences prefs,
  LegacyDataSource native,
) async {
  final legacy = await native.readLegacyPrefs(_legacyPrefsName);
  for (final entry in legacy.entries) {
    // Never let legacy data forge the flag that guards this very migration.
    if (entry.key == kotlinDataMigratedKey) continue;
    final value = _transcode(entry.key, entry.value);
    if (value == null) continue;
    await _writeTyped(prefs, entry.key, value);
  }
}

/// The Dart enum each enum-valued preference key resolves against.
///
/// Kotlin persisted these as `SCREAMING_SNAKE_CASE`; Dart parses them as its own
/// lowerCamelCase `.name`. Resolving through the real `values` list — rather than
/// blindly rewriting the string — means a name with no Dart counterpart is caught
/// and skipped instead of being written as an unparseable value.
final Map<String, List<Enum>> _enumValuedKeys = {
  'unit_system': UnitSystem.values,
  'app_theme_mode': AppThemeMode.values,
  'sleep_range_mode': SleepRangeMode.values,
  'activity_week_mode': ActivityWeekMode.values,
  'caffeine_sleep_sensitivity': CaffeineSleepSensitivity.values,
  'caffeine_alcohol_use': CaffeineAlcoholUse.values,
  'caffeine_habituation': CaffeineHabituation.values,
  'caffeine_cyp1a2_genotype': CaffeineGenotype.values,
  'caffeine_ahr_genotype': CaffeineGenotype.values,
  'caffeine_hormonal_status': CaffeineHormonalStatus.values,
  for (final key in PeriodRangePreferenceKey.values)
    key.storageKey: TimeRange.values,
};

/// Keys whose Kotlin value means something different on the Dart side, and which
/// are therefore dropped rather than migrated.
///
/// `dashboard_widget_order` is the only one. Kotlin persisted a list of
/// `DashboardWidgetId` *enum names* (`STEPS,DISTANCE,…`); the Dart dashboard
/// persists a list of tile **titles** (`Steps`, `Distance`, `Body Energy`, …) —
/// see `DashboardNotifier.setTileOrder`, which stores what
/// `applyDashboardTileLayout` matches on `tile.title`. The two vocabularies do
/// not overlap, and Dart splits Kotlin's `STEPS`/`WEEKLY_CARDIO_LOAD` out into a
/// separate ring order (`dashboard_ring_order`) besides.
///
/// Copying the Kotlin value would not fail loudly: the unknown ids would simply
/// never match a tile, the grid would fall back to its default order *anyway* —
/// and, worse, `setTileOrder` preserves unmatched ids on every later write, so
/// the junk would outlive the migration. Dropping it costs the user only a
/// re-ordered dashboard, once.
const Set<String> _unportableKeys = {'dashboard_widget_order'};

/// Converts one legacy value into the form the Dart repository reads, or returns
/// null to skip the key.
///
/// Most keys pass straight through, and that is verified, not assumed: the
/// ordering lists (`manual_entry_widget_order`, `metric_detail_section_order`),
/// the recording dashboard layouts, the encoded hydration drinks and containers,
/// the Health Connect permission strings, the heart-zone thresholds, the Health
/// Connect exercise-type ints and the mindfulness sounds all use the *same* wire
/// format on both sides — wherever the Dart port needed Kotlin compatibility it
/// declared an explicit SCREAMING `storageName`. The keys that need work are
/// exactly those whose Dart enum is parsed by its own `.name`.
Object? _transcode(String key, Object? value) {
  if (_unportableKeys.contains(key)) {
    debugPrint('Kotlin migration: dropping unportable key "$key".');
    return null;
  }
  final enumValues = _enumValuedKeys[key];
  if (enumValues != null) return _dartEnumName(key, enumValues, value);
  if (key == 'app_language') return _dartLanguageName(value);
  return value;
}

/// The Dart `.name` matching a Kotlin `Enum.name`, comparing case-insensitively
/// and ignoring `_` — `ORAL_CONTRACEPTIVE` matches `oralContraceptive`,
/// `ROLLING_24H` matches `rolling24h`, `LAST_7_DAYS` matches `last7Days`.
String? _dartEnumName(String key, List<Enum> values, Object? value) {
  if (value is! String || value.isEmpty) return null;
  final wanted = _foldEnumName(value);
  for (final candidate in values) {
    if (_foldEnumName(candidate.name) == wanted) return candidate.name;
  }
  debugPrint(
    'Kotlin migration: "$key" has no Dart enum for "$value"; skipping.',
  );
  return null;
}

String _foldEnumName(String name) =>
    name.replaceAll('_', '').toLowerCase();

/// `app_language` is the one key whose two sides disagree on *what* is stored,
/// not merely on its casing.
///
/// Kotlin persisted `storageValue = languageTag ?: name` — a BCP-47 tag ("en",
/// "es") for a real language and the literal "SYSTEM" for "follow the system".
/// Dart persists the enum's own `.name` ("english", "system"). So the tag is
/// matched against [AppLanguage.languageTag], and "SYSTEM" (tagless) falls back
/// to a name match.
String? _dartLanguageName(Object? value) {
  if (value is! String || value.isEmpty) return null;
  for (final language in AppLanguage.values) {
    if (language.languageTag == value) return language.name;
  }
  final byName = _foldEnumName(value);
  for (final language in AppLanguage.values) {
    if (_foldEnumName(language.name) == byName) return language.name;
  }
  debugPrint(
    'Kotlin migration: "app_language" has no Dart language for "$value"; '
    'skipping.',
  );
  return null;
}

/// Paired BLE sensors. The payload is Kotlin's JSON array, which the Dart
/// `BleDeviceRepositoryImpl` decodes verbatim — only the *key* changes.
Future<void> _migrateBleDevices(
  SharedPreferences prefs,
  LegacyDataSource native,
) async {
  final legacy = await native.readLegacyPrefs(_legacyBlePrefsName);
  final devices = legacy[_legacyBleDevicesKey];
  if (devices is! String || devices.isEmpty) return;
  await prefs.setString(_bleDevicesKey, devices);
}

/// Free-text notes pinned to GPS points. Irreplaceable: Health Connect stores
/// the route but not the markers, so nothing else can bring these back.
Future<void> _migrateActivityMarkers(
  SharedPreferences prefs,
  LegacyDataSource native,
) async {
  final legacy = await native.readLegacyPrefs(_legacyMarkerPrefsName);
  for (final entry in legacy.entries) {
    if (!entry.key.startsWith(_legacyMarkerKeyPrefix)) continue;
    final value = entry.value;
    if (value is! String || value.isEmpty) continue;
    await prefs.setString(entry.key, value);
  }
}

/// Copies the Room database to where drift looks for it.
///
/// Room's `databases/openvitals.db` and drift's `app_flutter/openvitals.db` hold
/// the same schema (both version 3, one `beverages` table), so the file itself is
/// portable — only the directory differs. The `-wal`/`-shm` sidecars come along
/// when present: dropping a `-wal` would discard the most recent commits.
///
/// It never clobbers: an existing destination means the Flutter app has already
/// written its own beverages, and those win.
Future<void> _migrateDatabase(
  LegacyDataSource native,
  Directory documentsDir,
) async {
  final sourcePath = await native.legacyDatabasePath();
  if (sourcePath == null || sourcePath.isEmpty) return;
  if (!File(sourcePath).existsSync()) return;

  final destinationPath = p.join(documentsDir.path, _databaseName);
  if (File(destinationPath).existsSync()) return;

  await documentsDir.create(recursive: true);
  for (final suffix in const ['', '-wal', '-shm']) {
    final source = File('$sourcePath$suffix');
    if (!source.existsSync()) continue;
    await source.copy('$destinationPath$suffix');
  }
}

/// Moves the offline map packs and re-homes their metadata.
///
/// A *move*, not a copy: packs run to hundreds of megabytes, and both
/// directories live on the same filesystem, so the rename is instant and does
/// not need twice the free space.
///
/// The metadata needs converting, not moving. Kotlin persisted the library to
/// `offline_maps/metadata.json`; the Dart [OfflineMapMetadataStore] keeps the
/// same JSON *shape* but stores it in SharedPreferences. Moving only the
/// directory would leave Dart reading a null payload — every pack on disk, and an
/// empty map library in the UI. Pack file paths survive the move because both
/// sides reconstruct them as `<mapsDir>/<id><extension>`.
Future<void> _migrateOfflineMaps(
  SharedPreferences prefs,
  LegacyDataSource native,
  Directory documentsDir,
) async {
  final filesDirPath = await native.legacyFilesDir();
  if (filesDirPath == null || filesDirPath.isEmpty) return;

  final source = Directory(p.join(filesDirPath, _offlineMapsDirName));
  if (!source.existsSync()) return;

  final destination = Directory(p.join(documentsDir.path, _offlineMapsDirName));
  if (destination.existsSync()) return;

  // Read the metadata before the move; afterwards the source path is gone.
  final metadataFile =
      File(p.join(source.path, _offlineMapsMetadataFileName));
  final metadata =
      metadataFile.existsSync() ? await metadataFile.readAsString() : null;

  await documentsDir.create(recursive: true);
  await source.rename(destination.path);

  if (metadata != null && metadata.isNotEmpty) {
    await prefs.setString(
      OfflineMapMetadataStore.defaultPrefsKey,
      metadata,
    );
  }
}

/// Re-points the placed home-screen widgets at what they were showing.
///
/// The widget *instances* survive the update untouched — the Flutter receivers
/// carry the same class names as the Kotlin ones, so the launcher keeps the same
/// `appWidgetId`s. What is lost is each instance's selection, which Kotlin kept
/// in its own preferences file and Flutter keeps in the `home_widget` plugin's,
/// under `${homeWidgetKeyPrefix(widget, appWidgetId: id)}selection_id`.
///
/// Metric ids are validated against [DashboardMetric] before being written: an
/// id Dart cannot resolve would leave a tile stuck rendering nothing. Kotlin's
/// `DashboardWidgetId.CARDIO_LOAD` has no [DashboardMetric] counterpart (Dart
/// splits that into `WEEKLY_CARDIO_LOAD` / `INTENSITY_MINUTES`), so a tile on
/// that metric is skipped and logged; the user re-picks it from the widget's
/// configuration screen.
///
/// Drink ids are opaque `beverages.id` strings and need no mapping: the database
/// they refer to is copied verbatim above, so the ids still resolve.
///
/// NOTE: the two beverage widgets also cache an encoded *drink payload*
/// alongside the selection, and `readQuickBeverageDrink` requires both. This
/// migration restores only the selection — rebuilding the payload would mean
/// opening drift here, which this function deliberately does not do. A migrated
/// beverage tile therefore keeps its "select a beverage" state until it is
/// reconfigured; the selection is preserved so nothing is silently mismatched.
Future<void> _migrateHomeWidgets(
  LegacyDataSource native,
  HomeWidgetClient? widgets,
) async {
  if (widgets == null) return;

  final metricPrefs =
      await native.readLegacyPrefs(_legacyMetricWidgetPrefsName);
  for (final entry in metricPrefs.entries) {
    final appWidgetId =
        _appWidgetIdOf(entry.key, _legacyMetricWidgetKeyPrefix);
    if (appWidgetId == null) continue;
    final storedId = entry.value;
    if (storedId is! String || storedId.isEmpty) continue;
    if (DashboardMetric.fromStorage(storedId) == null) {
      debugPrint(
        'Kotlin migration: metric widget $appWidgetId has no Dart metric for '
        '"$storedId"; skipping (the user can re-pick it).',
      );
      continue;
    }
    await widgets.saveWidgetData(
      '${homeWidgetKeyPrefix(HomeWidgetId.metric, appWidgetId: appWidgetId)}'
      'selection_id',
      storedId,
    );
  }

  // Both beverage widgets (the 2x1 and the 1x1 one-tap) share one Kotlin
  // preferences file *and* one Dart storage namespace, so a single pass covers
  // them: `HomeWidgetId.quickBeverage` and `quickBeverageOneTap` resolve to the
  // same `beverage.<appWidgetId>.` prefix.
  final beveragePrefs =
      await native.readLegacyPrefs(_legacyBeverageWidgetPrefsName);
  for (final entry in beveragePrefs.entries) {
    final appWidgetId =
        _appWidgetIdOf(entry.key, _legacyBeverageWidgetKeyPrefix);
    if (appWidgetId == null) continue;
    final drinkId = entry.value;
    if (drinkId is! String || drinkId.isEmpty) continue;
    await widgets.saveWidgetData(
      '${homeWidgetKeyPrefix(HomeWidgetId.quickBeverage, appWidgetId: appWidgetId)}'
      'selection_id',
      drinkId,
    );
  }
}

/// The `appWidgetId` in a `<prefix><id>` key, or null when [key] is not one of
/// them — which also filters out Kotlin's transient `pending_*` bookkeeping keys
/// in the same files.
int? _appWidgetIdOf(String key, String prefix) {
  if (!key.startsWith(prefix)) return null;
  return int.tryParse(key.substring(prefix.length));
}

/// Writes [value] under [key] with the setter matching its runtime type.
///
/// Type-driven rather than key-driven on purpose: the native side already
/// preserved the distinction Dart needs (Kotlin `Float` → `double`, `Int`/`Long`
/// → `int`, `Set<String>` → `List<String>`), so there is no ~95-entry key table
/// here to fall out of sync with either repository.
Future<void> _writeTyped(
  SharedPreferences prefs,
  String key,
  Object? value,
) async {
  switch (value) {
    case final bool value:
      await prefs.setBool(key, value);
    case final int value:
      await prefs.setInt(key, value);
    case final double value:
      await prefs.setDouble(key, value);
    case final String value:
      await prefs.setString(key, value);
    case final List<String> value:
      await prefs.setStringList(key, value);
    case final List<Object?> value:
      await prefs.setStringList(key, value.whereType<String>().toList());
    default:
      debugPrint(
        'Kotlin migration: skipping "$key" of unsupported type '
        '${value.runtimeType}',
      );
  }
}
