import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/period/period_range_preference_key.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/data/migration/kotlin_data_migration.dart';
import 'package:openvitals/data/migration/legacy_data_source.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/domain/insights/daily_goals.dart';
import 'package:openvitals/domain/preferences/activity_week_mode.dart';
import 'package:openvitals/domain/preferences/app_language.dart';
import 'package:openvitals/domain/preferences/app_theme_mode.dart';
import 'package:openvitals/domain/preferences/caffeine_preferences.dart';
import 'package:openvitals/domain/preferences/sleep_range_mode.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/activity/maps/offline_map_metadata_store.dart';
import 'package:openvitals/features/homewidgets/home_widget_service.dart';
import 'package:path/path.dart' as p;
import 'package:shared_preferences/shared_preferences.dart';

/// In-memory [LegacyDataSource]. Values are typed exactly as the native side
/// hands them over: Kotlin `Float` already widened to `double`, `Set<String>`
/// already flattened to `List<String>`.
class FakeLegacyDataSource implements LegacyDataSource {
  FakeLegacyDataSource({
    this.hasData = true,
    Map<String, Map<String, Object?>>? prefs,
    this.databasePath,
    this.filesDir,
  }) : prefs = prefs ?? <String, Map<String, Object?>>{};

  final bool hasData;
  final Map<String, Map<String, Object?>> prefs;
  final String? databasePath;
  final String? filesDir;

  @override
  Future<bool> hasLegacyData() async => hasData;

  @override
  Future<Map<String, Object?>> readLegacyPrefs(String name) async =>
      prefs[name] ?? const <String, Object?>{};

  @override
  Future<String?> legacyDatabasePath() async => databasePath;

  @override
  Future<String?> legacyFilesDir() async => filesDir;
}

/// The channel-less case: iOS, unit tests, an isolate without the plugin. Every
/// call blows up, and the migration still has to come back cleanly.
class ThrowingLegacyDataSource implements LegacyDataSource {
  @override
  Future<bool> hasLegacyData() async =>
      throw MissingPluginException('no legacy channel');

  @override
  Future<Map<String, Object?>> readLegacyPrefs(String name) async =>
      throw MissingPluginException('no legacy channel');

  @override
  Future<String?> legacyDatabasePath() async =>
      throw MissingPluginException('no legacy channel');

  @override
  Future<String?> legacyFilesDir() async =>
      throw MissingPluginException('no legacy channel');
}

/// A legacy source that reports data but fails on every read — a corrupt or
/// unreadable legacy store. The migration must still finish and still flag.
class FailingReadLegacyDataSource implements LegacyDataSource {
  @override
  Future<bool> hasLegacyData() async => true;

  @override
  Future<Map<String, Object?>> readLegacyPrefs(String name) async =>
      throw const FileSystemException('unreadable');

  @override
  Future<String?> legacyDatabasePath() async =>
      throw const FileSystemException('unreadable');

  @override
  Future<String?> legacyFilesDir() async =>
      throw const FileSystemException('unreadable');
}

class FakeHomeWidgetClient implements HomeWidgetClient {
  final Map<String, Object?> saved = <String, Object?>{};

  @override
  Future<void> saveWidgetData(String key, Object? value) async {
    saved[key] = value;
  }

  @override
  Future<String?> readWidgetData(String key) async => saved[key] as String?;

  @override
  Future<void> updateWidget({
    String? qualifiedAndroidName,
    String? iOSName,
  }) async {}

  @override
  Future<List<HomeWidgetInstance>> installedWidgets() async =>
      const <HomeWidgetInstance>[];
}

Future<SharedPreferences> newPrefs([
  Map<String, Object> initial = const {},
]) async {
  SharedPreferences.setMockInitialValues(initial);
  return SharedPreferences.getInstance();
}

/// A realistic snapshot of the Kotlin `openvitals_prefs` file: the values as the
/// native bridge delivers them (floats widened, string sets flattened), with the
/// enum values in the SCREAMING_SNAKE form Kotlin actually persisted.
Map<String, Object?> kotlinPrefs() => <String, Object?>{
      'onboarding_done': true,
      'unit_system': 'IMPERIAL',
      'app_theme_mode': 'AMOLED',
      'app_language': 'es',
      'sleep_range_mode': 'EVENING_18H',
      'activity_week_mode': 'LAST_7_DAYS',
      'dynamic_color': true,
      'detail_range_steps': 'MONTH',
      // Goals: Kotlin `putFloat`.
      'goal_steps': 12500.0,
      'goal_sleep_hours': 7.25,
      'goal_protein_grams': 95.0,
      // Body profile: mixed int/float.
      'body_profile_birth_year': 1988,
      'body_profile_weight_kg': 82.6,
      'body_profile_resting_hr_bpm': 54,
      // Caffeine profile.
      'caffeine_profile_completed': true,
      'caffeine_half_life_minutes': 320,
      'caffeine_absorption_minutes': 40,
      'caffeine_sleep_threshold_mg': 45,
      'caffeine_bedtime': '23:15',
      'caffeine_sleep_sensitivity': 'HIGH',
      'caffeine_smoker': true,
      'caffeine_alcohol_use': 'OCCASIONAL',
      'caffeine_habituation': 'LOW',
      'caffeine_cyp1a2_genotype': 'SLOW',
      'caffeine_hormonal_status': 'ORAL_CONTRACEPTIVE',
      // Kotlin `putLong`.
      'privacy_policy_accepted_at': 1739577600000,
      // Kotlin `putStringSet`.
      'acknowledged_permissions': <String>['STEPS', 'HEART_RATE'],
      'hydration_reminders_enabled': true,
      'hydration_reminder_interval_minutes': 90,
      'high_heart_rate_threshold_bpm': 135,
    };

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late Directory documentsDir;

  setUp(() {
    documentsDir = Directory.systemTemp.createTempSync('ov_docs');
  });

  tearDown(() {
    if (documentsDir.existsSync()) {
      documentsDir.deleteSync(recursive: true);
    }
  });

  Future<void> migrate({
    required SharedPreferences prefs,
    required LegacyDataSource native,
    HomeWidgetClient? widgets,
    Directory? docs,
  }) =>
      migrateKotlinDataIfNeeded(
        prefs: prefs,
        native: native,
        documentsDir: docs ?? documentsDir,
        widgets: widgets,
      );

  group('acceptance: the migrated prefs read back through PreferencesRepository',
      () {
    test('goals, unit system, theme and the caffeine profile all round-trip',
        () async {
      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          prefs: {'openvitals_prefs': kotlinPrefs()},
        ),
      );

      // The real repository, over the migrated store. This is the only assertion
      // that actually proves the migration — not that "some keys got written",
      // but that the app's own reader sees what the user had configured.
      final repo = PreferencesRepository(prefs);

      expect(repo.unitSystem, UnitSystem.imperial);
      expect(repo.appThemeMode, AppThemeMode.amoled);
      expect(repo.appLanguage, AppLanguage.spanish);
      expect(repo.sleepRangeMode, SleepRangeMode.evening18h);
      expect(repo.activityWeekMode, ActivityWeekMode.last7Days);
      expect(repo.dynamicColor, isTrue);
      expect(repo.onboardingDone, isTrue);

      expect(repo.dailyGoalFor(MetricDailyGoalKey.steps), 12500.0);
      expect(repo.dailyGoalFor(MetricDailyGoalKey.sleepHours), 7.25);
      expect(repo.dailyGoalFor(MetricDailyGoalKey.proteinGrams), 95.0);

      final caffeine = repo.caffeinePreferences();
      expect(caffeine.profileCompleted, isTrue);
      expect(caffeine.halfLifeMinutes, 320);
      expect(caffeine.absorptionMinutes, 40);
      expect(caffeine.sleepThresholdMg, 45);
      expect(caffeine.bedtime.hour, 23);
      expect(caffeine.bedtime.minute, 15);
      expect(caffeine.sleepSensitivity, CaffeineSleepSensitivity.high);
      expect(caffeine.smoker, isTrue);
      expect(caffeine.alcoholUse, CaffeineAlcoholUse.occasional);
      expect(caffeine.caffeineHabituation, CaffeineHabituation.low);
      expect(caffeine.cyp1a2Genotype, CaffeineGenotype.slow);
      expect(caffeine.hormonalStatus, CaffeineHormonalStatus.oralContraceptive);

      final body = repo.bodyProfile();
      expect(body.birthYear, 1988);
      expect(body.weightKg, 82.6);
      expect(body.restingHeartRateBpm, 54);

      expect(repo.highHeartRateThresholdBpm, 135);
      expect(
        repo.timeRangeFor(PeriodRangePreferenceKey.steps),
        TimeRange.month,
      );
      expect(repo.acknowledgedPermissions(), {'STEPS', 'HEART_RATE'});
    });
  });

  group('type fidelity', () {
    test('a Kotlin Float goal survives as a Dart double', () async {
      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          prefs: {
            'openvitals_prefs': {'goal_steps': 9000.0},
          },
        ),
      );

      expect(prefs.getDouble('goal_steps'), 9000.0);
      expect(prefs.get('goal_steps'), isA<double>());
    });

    test('a Kotlin Set<String> survives as getStringList', () async {
      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          prefs: {
            'openvitals_prefs': {
              'custom_hydration_drinks': <String>['a', 'b'],
            },
          },
        ),
      );

      expect(prefs.getStringList('custom_hydration_drinks'), ['a', 'b']);
    });

    test('a Kotlin Long survives as a Dart int', () async {
      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          prefs: {
            'openvitals_prefs': {'privacy_policy_accepted_at': 1739577600000},
          },
        ),
      );

      expect(prefs.getInt('privacy_policy_accepted_at'), 1739577600000);
    });
  });

  group('enum value transcoding', () {
    test('SCREAMING_SNAKE enum names become the Dart lowerCamelCase names',
        () async {
      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          prefs: {
            'openvitals_prefs': {
              'unit_system': 'METRIC',
              'sleep_range_mode': 'ROLLING_24H',
              'activity_week_mode': 'MONDAY_TO_SUNDAY',
              'caffeine_hormonal_status': 'ORAL_CONTRACEPTIVE',
              'detail_range_body': 'YEAR',
            },
          },
        ),
      );

      expect(prefs.getString('unit_system'), 'metric');
      expect(prefs.getString('sleep_range_mode'), 'rolling24h');
      expect(prefs.getString('activity_week_mode'), 'mondayToSunday');
      expect(prefs.getString('caffeine_hormonal_status'), 'oralContraceptive');
      expect(prefs.getString('detail_range_body'), 'year');
    });

    test('app_language maps the Kotlin BCP-47 tag onto the Dart enum name',
        () async {
      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          prefs: {
            'openvitals_prefs': {'app_language': 'de'},
          },
        ),
      );

      expect(prefs.getString('app_language'), 'german');
      expect(PreferencesRepository(prefs).appLanguage, AppLanguage.german);
    });

    test('Kotlin\'s tagless SYSTEM language maps onto AppLanguage.system',
        () async {
      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          prefs: {
            'openvitals_prefs': {'app_language': 'SYSTEM'},
          },
        ),
      );

      expect(prefs.getString('app_language'), 'system');
    });

    test('an enum value with no Dart counterpart is skipped, not written',
        () async {
      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          prefs: {
            'openvitals_prefs': {
              'unit_system': 'NAUTICAL',
              'goal_steps': 9000.0,
            },
          },
        ),
      );

      // Skipped rather than written as an unparseable string...
      expect(prefs.containsKey('unit_system'), isFalse);
      // ...and the rest of the file still migrated.
      expect(prefs.getDouble('goal_steps'), 9000.0);
    });

    test('dashboard_widget_order is dropped: Dart keys that list by tile title',
        () async {
      // Kotlin stored DashboardWidgetId enum names; the Dart dashboard stores
      // tile *titles*. Copying would put ids into the pref that match no tile
      // and that `setTileOrder` would then preserve forever.
      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          prefs: {
            'openvitals_prefs': {
              'dashboard_widget_order': 'STEPS,DISTANCE,SLEEP',
              'manual_entry_widget_order': 'HYDRATION,WEIGHT',
            },
          },
        ),
      );

      expect(prefs.containsKey('dashboard_widget_order'), isFalse);
      // Its neighbours, whose vocabularies *do* match, still migrate.
      expect(prefs.getString('manual_entry_widget_order'), 'HYDRATION,WEIGHT');
    });

    test('the verbatim-compatible composite payloads pass through untouched',
        () async {
      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          prefs: {
            'openvitals_prefs': {
              'metric_detail_section_order': 'PERIOD_CHART,DAILY_GOAL',
              'activity_recording_dashboard_layout_56':
                  'LARGE_TOP|HEART_RATE=4x2,CADENCE=1x1',
              'body_energy_zone_thresholds_bpm': '95,115,135,155,175',
              'hydration_container_volume_milliliters': <String>['bottle=750.0'],
              'last_activity_exercise_type': 56,
            },
          },
        ),
      );

      expect(
        prefs.getString('metric_detail_section_order'),
        'PERIOD_CHART,DAILY_GOAL',
      );
      expect(
        prefs.getString('activity_recording_dashboard_layout_56'),
        'LARGE_TOP|HEART_RATE=4x2,CADENCE=1x1',
      );
      expect(
        prefs.getString('body_energy_zone_thresholds_bpm'),
        '95,115,135,155,175',
      );
      expect(
        prefs.getStringList('hydration_container_volume_milliliters'),
        ['bottle=750.0'],
      );

      final repo = PreferencesRepository(prefs);
      // The Health Connect exercise-type int vocabulary is shared, so 56 is
      // still "running" on the Dart side.
      expect(repo.lastActivityExerciseType, 56);
      expect(
        repo.hydrationContainerVolumeMilliliters(),
        {'bottle': 750.0},
      );
      expect(
        repo.metricDetailSectionOrder(),
        ['PERIOD_CHART', 'DAILY_GOAL'],
      );
    });

    test('the mindfulness sounds are already wire-compatible and pass through',
        () async {
      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          prefs: {
            'openvitals_prefs': {
              'mindfulness_timer_bell_sound': 'TEMPLE',
              'mindfulness_timer_background_sound': 'CHIMES',
            },
          },
        ),
      );

      expect(prefs.getString('mindfulness_timer_bell_sound'), 'TEMPLE');
      expect(prefs.getString('mindfulness_timer_background_sound'), 'CHIMES');
    });
  });

  group('BLE devices', () {
    test('the `devices` key is renamed to `ble_sensor_devices`', () async {
      const payload = '[{"id":"a","displayName":"Strap","address":"AA:BB"}]';
      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          prefs: {
            'ble_sensor_devices': {'devices': payload},
          },
        ),
      );

      expect(prefs.getString('ble_sensor_devices'), payload);
      // The Kotlin key name must not leak through.
      expect(prefs.containsKey('devices'), isFalse);
    });
  });

  group('activity markers', () {
    test('marker notes keep their key names', () async {
      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          prefs: {
            'activity_marker_metadata': {
              'activity_markers_run-1': 'id,1700,59.4,24.7,,TmFtZQ,,generic',
              'unrelated_key': 'ignored',
            },
          },
        ),
      );

      expect(
        prefs.getString('activity_markers_run-1'),
        'id,1700,59.4,24.7,,TmFtZQ,,generic',
      );
      expect(prefs.containsKey('unrelated_key'), isFalse);
    });
  });

  group('database', () {
    test('is copied, with its -wal sidecar, when the destination is absent',
        () async {
      final legacyDir = Directory.systemTemp.createTempSync('ov_legacy_db');
      addTearDown(() => legacyDir.deleteSync(recursive: true));
      final source = File(p.join(legacyDir.path, 'openvitals.db'))
        ..writeAsStringSync('room-payload');
      File('${source.path}-wal').writeAsStringSync('wal-payload');

      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(databasePath: source.path),
      );

      final copied = File(p.join(documentsDir.path, 'openvitals.db'));
      expect(copied.readAsStringSync(), 'room-payload');
      expect(
        File('${copied.path}-wal').readAsStringSync(),
        'wal-payload',
      );
    });

    test('never clobbers an existing drift database', () async {
      final legacyDir = Directory.systemTemp.createTempSync('ov_legacy_db');
      addTearDown(() => legacyDir.deleteSync(recursive: true));
      final source = File(p.join(legacyDir.path, 'openvitals.db'))
        ..writeAsStringSync('room-payload');

      final existing = File(p.join(documentsDir.path, 'openvitals.db'))
        ..writeAsStringSync('drift-payload');

      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(databasePath: source.path),
      );

      expect(existing.readAsStringSync(), 'drift-payload');
    });
  });

  group('offline maps', () {
    test('the pack directory is moved and its metadata lands in prefs',
        () async {
      final filesDir = Directory.systemTemp.createTempSync('ov_files');
      addTearDown(() {
        if (filesDir.existsSync()) filesDir.deleteSync(recursive: true);
      });
      final legacyMaps = Directory(p.join(filesDir.path, 'offline_maps'))
        ..createSync(recursive: true);
      File(p.join(legacyMaps.path, 'pack-1.pmtiles'))
          .writeAsStringSync('tiles');
      final metadata = jsonEncode({
        'activeFormat': 'PMTILES',
        'packs': [
          {
            'id': 'pack-1',
            'displayName': 'Estonia',
            'originalFileName': 'estonia.pmtiles',
            'format': 'PMTILES',
            'sizeBytes': 5,
            'importedAtMillis': 1739577600000,
          },
        ],
      });
      File(p.join(legacyMaps.path, 'metadata.json')).writeAsStringSync(metadata);

      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(filesDir: filesDir.path),
      );

      // Moved, not copied: the source is gone.
      expect(legacyMaps.existsSync(), isFalse);
      final moved = File(
        p.join(documentsDir.path, 'offline_maps', 'pack-1.pmtiles'),
      );
      expect(moved.readAsStringSync(), 'tiles');

      // Kotlin kept the library in metadata.json; Dart reads it from prefs.
      expect(
        prefs.getString(OfflineMapMetadataStore.defaultPrefsKey),
        metadata,
      );

      // And the real store resolves the pack, at its new path.
      final store = OfflineMapMetadataStore.sharedPreferences(
        prefs,
        p.join(documentsDir.path, 'offline_maps'),
      );
      final state = store.read();
      expect(state.mapPacks, hasLength(1));
      expect(state.mapPacks.single.id, 'pack-1');
      expect(state.mapPacks.single.displayName, 'Estonia');
      expect(state.mapPacks.single.path, moved.path);
    });

    test('an existing destination is left alone', () async {
      final filesDir = Directory.systemTemp.createTempSync('ov_files');
      addTearDown(() {
        if (filesDir.existsSync()) filesDir.deleteSync(recursive: true);
      });
      Directory(p.join(filesDir.path, 'offline_maps')).createSync();
      Directory(p.join(documentsDir.path, 'offline_maps')).createSync();

      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(filesDir: filesDir.path),
      );

      // Untouched: the Flutter app already has a maps directory of its own.
      expect(
        Directory(p.join(filesDir.path, 'offline_maps')).existsSync(),
        isTrue,
      );
    });
  });

  group('home-screen widgets', () {
    test('metric and beverage selections are re-pointed at the Dart key scheme',
        () async {
      final prefs = await newPrefs();
      final widgets = FakeHomeWidgetClient();
      await migrate(
        prefs: prefs,
        widgets: widgets,
        native: FakeLegacyDataSource(
          prefs: {
            'home_metric_widgets': {
              'metric_id_11': 'RESTING_HEART_RATE',
              // Kotlin's transient bookkeeping, not a placed instance.
              'pending_metric_id': 'STEPS',
            },
            'home_quick_beverage_widgets': {
              'drink_id_22': 'drink-uuid-1',
            },
          },
        ),
      );

      expect(
        widgets.saved['${homeWidgetKeyPrefix(HomeWidgetId.metric, appWidgetId: 11)}selection_id'],
        'RESTING_HEART_RATE',
      );
      expect(
        widgets.saved['${homeWidgetKeyPrefix(HomeWidgetId.quickBeverage, appWidgetId: 22)}selection_id'],
        'drink-uuid-1',
      );
      // The pending key is not an appWidgetId and must not be written.
      expect(widgets.saved, hasLength(2));
    });

    test('the 1x1 one-tap widget shares the 2x1 widget\'s key namespace',
        () async {
      // Both beverage widgets resolve to the same `beverage.<id>.` prefix, so a
      // single migrated selection serves whichever type is placed.
      expect(
        homeWidgetKeyPrefix(HomeWidgetId.quickBeverageOneTap, appWidgetId: 22),
        homeWidgetKeyPrefix(HomeWidgetId.quickBeverage, appWidgetId: 22),
      );
    });

    test('a metric id with no Dart counterpart is skipped, not written',
        () async {
      final prefs = await newPrefs();
      final widgets = FakeHomeWidgetClient();
      await migrate(
        prefs: prefs,
        widgets: widgets,
        native: FakeLegacyDataSource(
          prefs: {
            'home_metric_widgets': {
              // Kotlin's DashboardWidgetId.CARDIO_LOAD has no DashboardMetric.
              'metric_id_11': 'CARDIO_LOAD',
              'metric_id_12': 'STEPS',
            },
          },
        ),
      );

      expect(widgets.saved.keys, hasLength(1));
      expect(
        widgets.saved['${homeWidgetKeyPrefix(HomeWidgetId.metric, appWidgetId: 12)}selection_id'],
        'STEPS',
      );
    });
  });

  group('run conditions', () {
    test('is a no-op when there is no legacy data', () async {
      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          hasData: false,
          prefs: {'openvitals_prefs': kotlinPrefs()},
        ),
      );

      expect(prefs.containsKey('unit_system'), isFalse);
      // Nothing ran, so nothing is flagged: a later restore must still migrate.
      expect(prefs.containsKey(kotlinDataMigratedKey), isFalse);
    });

    test('is skipped when the Flutter side has already been used', () async {
      // The anti-clobber guard: a nightly tester already onboarded on Flutter.
      // Their newer settings must not be overwritten with stale Kotlin ones.
      final prefs = await newPrefs({
        'onboarding_done': true,
        'unit_system': 'metric',
      });
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          prefs: {'openvitals_prefs': kotlinPrefs()},
        ),
      );

      expect(prefs.getString('unit_system'), 'metric');
      expect(prefs.containsKey('goal_steps'), isFalse);
      expect(prefs.containsKey(kotlinDataMigratedKey), isFalse);
    });

    test('is idempotent: a second run changes nothing', () async {
      final native = FakeLegacyDataSource(
        prefs: {'openvitals_prefs': kotlinPrefs()},
      );
      final prefs = await newPrefs();
      await migrate(prefs: prefs, native: native);

      expect(prefs.getBool(kotlinDataMigratedKey), isTrue);

      // The user then changes a setting on the Flutter side.
      prefs.setString('unit_system', 'metric');

      await migrate(prefs: prefs, native: native);

      // The second run must not resurrect the Kotlin value.
      expect(prefs.getString('unit_system'), 'metric');
    });

    test('the one-shot flag is set even when every step fails', () async {
      // A persistent failure must not retry on every launch forever.
      final prefs = await newPrefs();
      await migrate(prefs: prefs, native: FailingReadLegacyDataSource());

      expect(prefs.getBool(kotlinDataMigratedKey), isTrue);
    });
  });

  group('robustness', () {
    test('never throws when the native channel is missing (iOS, tests)',
        () async {
      final prefs = await newPrefs();

      await expectLater(
        migrate(prefs: prefs, native: ThrowingLegacyDataSource()),
        completes,
      );

      // The channel is absent, so we cannot know there was legacy data: leave
      // the flag unset rather than burning the one shot.
      expect(prefs.containsKey(kotlinDataMigratedKey), isFalse);
    });

    test('a value of an unsupported type is skipped, not fatal', () async {
      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          prefs: {
            'openvitals_prefs': {
              'weird_key': const <int>[1, 2, 3],
              'goal_steps': 9000.0,
            },
          },
        ),
      );

      expect(prefs.getDouble('goal_steps'), 9000.0);
      expect(prefs.getBool(kotlinDataMigratedKey), isTrue);
    });

    test('a missing legacy database and files dir are simply skipped', () async {
      final prefs = await newPrefs();
      await migrate(
        prefs: prefs,
        native: FakeLegacyDataSource(
          databasePath: '/nonexistent/openvitals.db',
          filesDir: '/nonexistent/files',
          prefs: {'openvitals_prefs': kotlinPrefs()},
        ),
      );

      expect(prefs.getBool(kotlinDataMigratedKey), isTrue);
      expect(PreferencesRepository(prefs).unitSystem, UnitSystem.imperial);
    });

    test('a null HomeWidgetClient is tolerated', () async {
      final prefs = await newPrefs();
      await expectLater(
        migrate(
          prefs: prefs,
          native: FakeLegacyDataSource(
            prefs: {
              'home_metric_widgets': {'metric_id_11': 'STEPS'},
            },
          ),
        ),
        completes,
      );
    });
  });
}
