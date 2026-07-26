import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/data/local/open_vitals_database.dart';

void main() {
  Future<List<String>> tableNames(OpenVitalsDatabase db) async {
    final rows = await db
        .customSelect(
          "SELECT name FROM sqlite_master WHERE type='table' AND name='beverages'",
        )
        .get();
    return rows.map((row) => row.read<String>('name')).toList();
  }

  test('legacy version one migrates to beverage schema version three', () async {
    final db = OpenVitalsDatabase(NativeDatabase.memory());
    addTearDown(db.close);

    final migration = OpenVitalsDatabase.migration1To3;
    expect(migration.startVersion, 1);
    expect(migration.endVersion, 3);
    expect(migration.sql, contains('CREATE TABLE IF NOT EXISTS `beverages`'));

    await migration.migrate(db);
    expect(await tableNames(db), contains('beverages'));
  });

  test('legacy version two migrates to beverage schema version three', () async {
    final db = OpenVitalsDatabase(NativeDatabase.memory());
    addTearDown(db.close);

    final migration = OpenVitalsDatabase.migration2To3;
    expect(migration.startVersion, 2);
    expect(migration.endVersion, 3);
    expect(migration.sql, contains('CREATE TABLE IF NOT EXISTS `beverages`'));

    await migration.migrate(db);
    expect(await tableNames(db), contains('beverages'));
  });

  group('v6 -> v7 (the Body Energy chain)', () {
    /// A database standing at schema version 6: every table that shipped in v6,
    /// and `user_version` set so opening it drives `onUpgrade(from: 6)`.
    OpenVitalsDatabase openAtVersionSix() => OpenVitalsDatabase(
          NativeDatabase.memory(
            setup: (raw) {
              raw.execute(OpenVitalsDatabase.createBeveragesTableSql);
              raw.execute(OpenVitalsDatabase.createFeelChecksTableSql);
              raw.execute(
                  OpenVitalsDatabase.createVitalsDailyAggregatesTableSql);
              raw.execute(OpenVitalsDatabase.createVitalsSyncCursorsTableSql);
              raw.execute(
                  OpenVitalsDatabase.createGarminWellnessSamplesTableSql);
              raw.execute('PRAGMA user_version = 6');
            },
          ),
        );

    /// The stored DDL for [table], normalised so only structure is compared:
    /// SQLite echoes back the creating statement verbatim, so backticks,
    /// indentation and line breaks differ between the hand-written migration
    /// SQL and what drift's `createAll()` emits.
    Future<String> storedSchema(OpenVitalsDatabase db, String table) async {
      final rows = await db
          .customSelect(
            "SELECT sql FROM sqlite_master WHERE type='table' AND name='$table'",
          )
          .get();
      expect(rows, hasLength(1), reason: '$table should exist exactly once');
      return rows.first
          .read<String>('sql')
          .replaceAll('`', '')
          .replaceAll('"', '')
          .replaceAll(RegExp(r'\s+'), ' ')
          .replaceAll('( ', '(')
          .replaceAll(' )', ')')
          .trim();
    }

    test('the migration creates both chain tables and reaches version 7',
        () async {
      final db = openAtVersionSix();
      addTearDown(db.close);

      // Any query forces the migration to run first.
      expect(await db.bodyEnergyTimelineDao.countDays(), 0);
      expect(await db.bodyEnergyTimelineDao.latestDay(), isNull);
      expect(db.schemaVersion, 7);

      final rows = await db
          .customSelect(
            "SELECT name FROM sqlite_master WHERE type='table' "
            "AND name IN ('body_energy_days', 'body_energy_buckets')",
          )
          .get();
      expect(
        rows.map((row) => row.read<String>('name')).toSet(),
        {'body_energy_days', 'body_energy_buckets'},
      );
    });

    test('the pre-v7 tables and their rows survive the upgrade', () async {
      final db = openAtVersionSix();
      addTearDown(db.close);

      await db.vitalsDailyCacheDao.upsertDay(
        metric: 'restingHeartRate',
        epochDay: 20000,
        valueSum: 55.0,
        sampleCount: 1,
      );
      // Reading it back proves the v7 block did not disturb the v5 tables.
      final cached =
          await db.vitalsDailyCacheDao.aggregatesBetween('restingHeartRate', 0, 30000);
      expect(cached, hasLength(1));
      expect(cached.single.valueSum, 55.0);
    });

    test('the hand-written v7 SQL produces the same schema as onCreate',
        () async {
      // The only guard this project has: there is no drift_schemas snapshot and
      // no SchemaVerifier, so a column added to the Dart table but forgotten in
      // createBodyEnergy*TableSql would otherwise ship and only fail on an
      // upgraded install — never on a fresh one, and never in any other test.
      final migrated = openAtVersionSix();
      addTearDown(migrated.close);
      final created = OpenVitalsDatabase(NativeDatabase.memory());
      addTearDown(created.close);

      // Force both to finish opening before reading sqlite_master.
      await migrated.bodyEnergyTimelineDao.countDays();
      await created.bodyEnergyTimelineDao.countDays();

      for (final table in ['body_energy_days', 'body_energy_buckets']) {
        expect(
          await storedSchema(migrated, table),
          await storedSchema(created, table),
          reason: '$table migration SQL has drifted from the Dart table',
        );
      }
    });
  });
}
