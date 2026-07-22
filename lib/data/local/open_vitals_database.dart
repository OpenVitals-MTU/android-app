import 'package:drift/drift.dart';

import 'beverage/beverage_entity.dart';

part 'open_vitals_database.g.dart';

/// Persisted beverages, mirroring the Kotlin Room `beverages` table.
///
/// The generated query row class is [BeverageEntity] (via `@UseRowClass`), so
/// its constructor parameters must line up with these columns.
@UseRowClass(BeverageEntity)
class Beverages extends Table {
  TextColumn get id => text()();
  TextColumn get name => text()();
  TextColumn get category => text().nullable()();
  RealColumn get volumeMilliliters => real().named('volume_milliliters')();
  RealColumn get hydrationMultiplier =>
      real().named('hydration_multiplier')();
  BoolColumn get isPreloaded => boolean().named('is_preloaded')();
  BoolColumn get isDeleted => boolean().named('is_deleted')();
  IntColumn get sortOrder => integer().named('sort_order')();
  RealColumn get energyKcal => real().named('energy_kcal').nullable()();
  RealColumn get proteinGrams => real().named('protein_grams').nullable()();
  RealColumn get totalCarbohydrateGrams =>
      real().named('total_carbohydrate_grams').nullable()();
  RealColumn get totalFatGrams => real().named('total_fat_grams').nullable()();
  RealColumn get dietaryFiberGrams =>
      real().named('dietary_fiber_grams').nullable()();
  RealColumn get sugarGrams => real().named('sugar_grams').nullable()();
  RealColumn get saturatedFatGrams =>
      real().named('saturated_fat_grams').nullable()();
  RealColumn get sodiumGrams => real().named('sodium_grams').nullable()();
  RealColumn get potassiumGrams =>
      real().named('potassium_grams').nullable()();
  RealColumn get calciumGrams => real().named('calcium_grams').nullable()();
  RealColumn get caffeineGrams => real().named('caffeine_grams').nullable()();

  @override
  Set<Column> get primaryKey => {id};

  @override
  String get tableName => 'beverages';
}

/// A best-effort Dart equivalent of a Room `Migration`.
///
/// The Kotlin database exposes `MIGRATION_1_3` and `MIGRATION_2_3`, both of
/// which simply create the `beverages` table. This mirrors that shape so the
/// same intent (and versions) can be asserted in tests.
class BeverageMigration {
  const BeverageMigration(this.startVersion);

  final int startVersion;

  int get endVersion => 3;

  String get sql => OpenVitalsDatabase.createBeveragesTableSql;

  Future<void> migrate(OpenVitalsDatabase database) =>
      database.customStatement(OpenVitalsDatabase.createBeveragesTableSql);
}

/// Body Energy "feel-check" log: the user's own 0–10 energy rating at a moment
/// in time. A local time-series (Health Connect has no equivalent record), read
/// back in windows to fit the personal calibration gains.
class FeelChecks extends Table {
  IntColumn get id => integer().autoIncrement()();
  IntColumn get recordedAtMillis => integer().named('recorded_at_millis')();
  IntColumn get rating => integer()(); // 0–10

  @override
  String get tableName => 'feel_checks';
}

@DriftAccessor(tables: [FeelChecks])
class FeelCheckDao extends DatabaseAccessor<OpenVitalsDatabase>
    with _$FeelCheckDaoMixin {
  FeelCheckDao(super.db);

  Future<void> insertFeelCheck({
    required int recordedAtMillis,
    required int rating,
  }) async {
    await into(feelChecks).insert(
      FeelChecksCompanion.insert(
        recordedAtMillis: recordedAtMillis,
        rating: rating,
      ),
    );
  }

  Future<List<FeelCheck>> feelChecksBetween(
    int startMillis,
    int endMillis,
  ) {
    return (select(feelChecks)
          ..where(
            (f) => f.recordedAtMillis.isBetweenValues(startMillis, endMillis),
          )
          ..orderBy([(f) => OrderingTerm(expression: f.recordedAtMillis)]))
        .get();
  }

  Future<int> countFeelChecks() async {
    final row = await customSelect(
      'SELECT COUNT(*) AS c FROM feel_checks',
      readsFrom: {feelChecks},
    ).getSingle();
    return row.read<int>('c');
  }
}

/// Cached per-day aggregate of a Health Connect vitals series, keyed by metric
/// name + [epochDay]. Densely-sampled metrics with no HC aggregate metric
/// (respiratory rate) take 40s+ to read a year raw; this table holds the daily
/// means so a long-range chart reads ~365 rows instead. [valueSum]/[sampleCount]
/// (not the mean) are stored so incremental day recomputes stay exact and the
/// mean reconstructs as valueSum/sampleCount. [secondarySum] carries blood
/// pressure's diastolic sum (null for single-value metrics). Kept in sync via
/// the Health Connect Changes API — see VitalsHistorySyncService.
class VitalsDailyAggregates extends Table {
  TextColumn get metric => text()();
  IntColumn get epochDay => integer().named('epoch_day')();
  RealColumn get valueSum => real().named('value_sum')();
  RealColumn get secondarySum => real().named('secondary_sum').nullable()();
  IntColumn get sampleCount => integer().named('sample_count')();

  @override
  Set<Column> get primaryKey => {metric, epochDay};

  @override
  String get tableName => 'vitals_daily_aggregates';
}

/// Per-metric sync bookkeeping: the Health Connect changes token to resume from,
/// and when the last full rebuild ran.
class VitalsSyncCursors extends Table {
  TextColumn get metric => text()();
  TextColumn get changesToken => text().named('changes_token').nullable()();
  IntColumn get lastFullSyncMillis =>
      integer().named('last_full_sync_millis').nullable()();

  @override
  Set<Column> get primaryKey => {metric};

  @override
  String get tableName => 'vitals_sync_cursors';
}

/// Watch-only wellness samples that Health Connect has no type for.
///
/// Stress and Body Battery are Garmin-proprietary measures with no Health
/// Connect equivalent, so unlike everything else the app reads, there is nowhere
/// else to put them — this table is their system of record, not a cache. That is
/// what distinguishes it from the universal raw-metric cache that was evaluated
/// and rejected: nothing is being duplicated here.
///
/// One table with a [metric] discriminator rather than two near-identical ones:
/// both are plain `(instant, integer)` series and arrive from the same FIT
/// message.
///
/// The `(metric, time)` primary key does the deduplication. A watch re-offers
/// the same monitoring window on successive syncs, so the same sample arrives
/// repeatedly; an upsert on that key makes a re-import idempotent, the same
/// guarantee `clientRecordId` gives the Health Connect records.
class GarminWellnessSamples extends Table {
  /// `stress` or `body_energy` — see [GarminWellnessMetric].
  TextColumn get metric => text()();

  /// Sample instant, UTC milliseconds since the epoch.
  IntColumn get timeMillis => integer().named('time_millis')();

  /// Stress 0..100, Body Battery 0..100. Stored raw, uninterpreted.
  IntColumn get value => integer()();

  @override
  Set<Column> get primaryKey => {metric, timeMillis};

  @override
  String get tableName => 'garmin_wellness_samples';
}

/// The metrics [GarminWellnessSamples] can hold. The stored name is explicit so
/// renaming a Dart identifier cannot orphan rows.
enum GarminWellnessMetric {
  stress('stress'),
  bodyEnergy('body_energy');

  const GarminWellnessMetric(this.storageName);
  final String storageName;
}

@DriftAccessor(tables: [GarminWellnessSamples])
class GarminWellnessDao extends DatabaseAccessor<OpenVitalsDatabase>
    with _$GarminWellnessDaoMixin {
  GarminWellnessDao(super.db);

  /// Upserts a batch. Re-syncing an overlapping window rewrites the same rows
  /// rather than duplicating them.
  Future<void> upsertSamples(
    List<GarminWellnessSamplesCompanion> samples,
  ) async {
    if (samples.isEmpty) return;
    await batch((b) {
      b.insertAllOnConflictUpdate(garminWellnessSamples, samples);
    });
  }

  /// Samples for [metric] in `[fromMillis, toMillis)`, oldest first.
  Future<List<GarminWellnessSample>> samplesBetween(
    GarminWellnessMetric metric,
    int fromMillis,
    int toMillis,
  ) {
    return (select(garminWellnessSamples)
          ..where((t) =>
              t.metric.equals(metric.storageName) &
              t.timeMillis.isBiggerOrEqualValue(fromMillis) &
              t.timeMillis.isSmallerThanValue(toMillis))
          ..orderBy([(t) => OrderingTerm(expression: t.timeMillis)]))
        .get();
  }

  /// The most recent sample for [metric], or null when none has been synced.
  Future<GarminWellnessSample?> latest(GarminWellnessMetric metric) {
    return (select(garminWellnessSamples)
          ..where((t) => t.metric.equals(metric.storageName))
          ..orderBy([
            (t) => OrderingTerm(
                expression: t.timeMillis, mode: OrderingMode.desc),
          ])
          ..limit(1))
        .getSingleOrNull();
  }

  /// Total rows held, for diagnostics.
  Future<int> countFor(GarminWellnessMetric metric) async {
    final rows = await (selectOnly(garminWellnessSamples)
          ..addColumns([garminWellnessSamples.timeMillis.count()])
          ..where(garminWellnessSamples.metric.equals(metric.storageName)))
        .get();
    return rows.first.read(garminWellnessSamples.timeMillis.count()) ?? 0;
  }
}

@DriftAccessor(tables: [VitalsDailyAggregates, VitalsSyncCursors])
class VitalsDailyCacheDao extends DatabaseAccessor<OpenVitalsDatabase>
    with _$VitalsDailyCacheDaoMixin {
  VitalsDailyCacheDao(super.db);

  Future<List<VitalsDailyAggregate>> aggregatesBetween(
    String metric,
    int startEpochDay,
    int endEpochDay,
  ) {
    return (select(vitalsDailyAggregates)
          ..where((a) =>
              a.metric.equals(metric) &
              a.epochDay.isBetweenValues(startEpochDay, endEpochDay))
          ..orderBy([(a) => OrderingTerm(expression: a.epochDay)]))
        .get();
  }

  /// Reactive variant, so the overview can refresh when a background sync writes.
  Stream<List<VitalsDailyAggregate>> watchAggregatesBetween(
    String metric,
    int startEpochDay,
    int endEpochDay,
  ) {
    return (select(vitalsDailyAggregates)
          ..where((a) =>
              a.metric.equals(metric) &
              a.epochDay.isBetweenValues(startEpochDay, endEpochDay))
          ..orderBy([(a) => OrderingTerm(expression: a.epochDay)]))
        .watch();
  }

  Future<void> upsertDay({
    required String metric,
    required int epochDay,
    required double valueSum,
    required int sampleCount,
    double? secondarySum,
  }) {
    return into(vitalsDailyAggregates).insertOnConflictUpdate(
      VitalsDailyAggregatesCompanion.insert(
        metric: metric,
        epochDay: epochDay,
        valueSum: valueSum,
        sampleCount: sampleCount,
        secondarySum: Value(secondarySum),
      ),
    );
  }

  Future<void> deleteDay(String metric, int epochDay) {
    return (delete(vitalsDailyAggregates)
          ..where((a) => a.metric.equals(metric) & a.epochDay.equals(epochDay)))
        .go();
  }

  /// Atomically replace every cached day for [metric] — the full-rebuild write.
  Future<void> replaceMetric(
    String metric,
    List<VitalsDailyAggregatesCompanion> days,
  ) async {
    await transaction(() async {
      await (delete(vitalsDailyAggregates)..where((a) => a.metric.equals(metric)))
          .go();
      await batch((b) => b.insertAll(vitalsDailyAggregates, days));
    });
  }

  Future<VitalsSyncCursor?> cursor(String metric) {
    return (select(vitalsSyncCursors)
          ..where((c) => c.metric.equals(metric))
          ..limit(1))
        .getSingleOrNull();
  }

  /// Full-sync bookkeeping: set the resume token and the rebuild timestamp.
  Future<void> writeFullSync(
    String metric,
    String? changesToken,
    int lastFullSyncMillis,
  ) {
    return into(vitalsSyncCursors).insertOnConflictUpdate(
      VitalsSyncCursorsCompanion.insert(
        metric: metric,
        changesToken: Value(changesToken),
        lastFullSyncMillis: Value(lastFullSyncMillis),
      ),
    );
  }

  /// Advance only the resume token (an incremental sync), preserving the row's
  /// last-full-sync stamp.
  Future<void> writeToken(String metric, String? changesToken) async {
    final updated = await (update(vitalsSyncCursors)
          ..where((c) => c.metric.equals(metric)))
        .write(VitalsSyncCursorsCompanion(changesToken: Value(changesToken)));
    if (updated == 0) {
      await into(vitalsSyncCursors).insert(
        VitalsSyncCursorsCompanion.insert(
          metric: metric,
          changesToken: Value(changesToken),
        ),
      );
    }
  }
}

/// The [VitalsDailyCacheDao] metric key under which daily calories-burned totals
/// are cached. That table is a generic per-day aggregate store keyed by metric
/// name, so calories reuse it rather than clone an identical table. The calorie
/// day value is a kcal SUM, stored as [VitalsDailyAggregates.valueSum] with a
/// [VitalsDailyAggregates.sampleCount] of 1 (so valueSum/sampleCount is the day
/// total). See CaloriesHistorySyncService.
const String caloriesBurnedCacheMetric = 'totalCaloriesBurned';

/// How many days back the calories-burned cache is kept fresh. A requested range
/// that starts before this window is not covered by the cache, so it falls back
/// to a live Health Connect read rather than reading as empty.
const int caloriesCacheLookbackDays = 730;

@DriftAccessor(tables: [Beverages])
class BeverageDao extends DatabaseAccessor<OpenVitalsDatabase>
    with _$BeverageDaoMixin {
  BeverageDao(super.db);

  Future<List<BeverageEntity>> activeBeverages() {
    return (select(beverages)
          ..where((b) => b.isDeleted.equals(false))
          ..orderBy([
            (b) =>
                OrderingTerm(expression: b.sortOrder, mode: OrderingMode.asc),
            (b) => OrderingTerm(
                  expression: b.name.collate(Collate.noCase),
                  mode: OrderingMode.asc,
                ),
          ]))
        .get();
  }

  Future<BeverageEntity?> beverageById(String id) {
    return (select(beverages)
          ..where((b) => b.id.equals(id))
          ..limit(1))
        .getSingleOrNull();
  }

  Future<int> nextSortOrder() async {
    final row = await customSelect(
      'SELECT COALESCE(MAX(sort_order), -1) + 1 AS next FROM beverages',
      readsFrom: {beverages},
    ).getSingle();
    return row.read<int>('next');
  }

  Future<void> insertDefaults(List<BeverageEntity> entities) async {
    await batch((b) {
      b.insertAll(
        beverages,
        entities.map(_toCompanion).toList(),
        mode: InsertMode.insertOrIgnore,
      );
    });
  }

  Future<void> upsert(BeverageEntity entity) async {
    await into(beverages).insertOnConflictUpdate(_toCompanion(entity));
  }

  Future<void> softDelete(String id) async {
    await (update(beverages)..where((b) => b.id.equals(id)))
        .write(const BeveragesCompanion(isDeleted: Value(true)));
  }

  Future<void> updateCategory(String id, String? category) async {
    await (update(beverages)..where((b) => b.id.equals(id)))
        .write(BeveragesCompanion(category: Value(category)));
  }

  Future<void> updateSortOrder(String id, int sortOrder) async {
    await (update(beverages)..where((b) => b.id.equals(id)))
        .write(BeveragesCompanion(sortOrder: Value(sortOrder)));
  }

  /// Transactional reorder: mirrors the Kotlin `updateSortOrder(ids)` overload
  /// that reindexes each id to its position in [ids].
  Future<void> updateSortOrderForIds(List<String> ids) async {
    await transaction(() async {
      for (var index = 0; index < ids.length; index++) {
        await updateSortOrder(ids[index], index);
      }
    });
  }

  BeveragesCompanion _toCompanion(BeverageEntity e) => BeveragesCompanion(
        id: Value(e.id),
        name: Value(e.name),
        category: Value(e.category),
        volumeMilliliters: Value(e.volumeMilliliters),
        hydrationMultiplier: Value(e.hydrationMultiplier),
        isPreloaded: Value(e.isPreloaded),
        isDeleted: Value(e.isDeleted),
        sortOrder: Value(e.sortOrder),
        energyKcal: Value(e.energyKcal),
        proteinGrams: Value(e.proteinGrams),
        totalCarbohydrateGrams: Value(e.totalCarbohydrateGrams),
        totalFatGrams: Value(e.totalFatGrams),
        dietaryFiberGrams: Value(e.dietaryFiberGrams),
        sugarGrams: Value(e.sugarGrams),
        saturatedFatGrams: Value(e.saturatedFatGrams),
        sodiumGrams: Value(e.sodiumGrams),
        potassiumGrams: Value(e.potassiumGrams),
        calciumGrams: Value(e.calciumGrams),
        caffeineGrams: Value(e.caffeineGrams),
      );
}

@DriftDatabase(
  tables: [
    Beverages,
    FeelChecks,
    VitalsDailyAggregates,
    VitalsSyncCursors,
    GarminWellnessSamples,
  ],
  daos: [
    BeverageDao,
    FeelCheckDao,
    VitalsDailyCacheDao,
    GarminWellnessDao,
  ],
)
class OpenVitalsDatabase extends _$OpenVitalsDatabase {
  /// Construct with any [QueryExecutor]. Tests pass `NativeDatabase.memory()`
  /// (from `package:drift/native.dart`); the app wires a file-backed executor.
  OpenVitalsDatabase(super.executor);

  @override
  int get schemaVersion => 6;

  @override
  MigrationStrategy get migration => MigrationStrategy(
        onCreate: (m) => m.createAll(),
        onUpgrade: (m, from, to) async {
          // Both Room migrations (1->3 and 2->3) simply create the beverages
          // table; anything before v3 needs it created best-effort.
          if (from < 3) {
            await customStatement(createBeveragesTableSql);
          }
          // v4 adds the Body Energy feel-check log.
          if (from < 4) {
            await customStatement(createFeelChecksTableSql);
          }
          // v5 adds the cached daily vitals aggregates + their sync cursors.
          if (from < 5) {
            await customStatement(createVitalsDailyAggregatesTableSql);
            await customStatement(createVitalsSyncCursorsTableSql);
          }
          // v6 adds the Garmin stress / Body Battery samples, which have no
          // Health Connect type and so live only here.
          if (from < 6) {
            await customStatement(createGarminWellnessSamplesTableSql);
          }
        },
      );

  /// The `CREATE TABLE` for the watch wellness samples, applied on upgrade
  /// from < v6.
  static const String createGarminWellnessSamplesTableSql = '''
CREATE TABLE IF NOT EXISTS garmin_wellness_samples (
  metric TEXT NOT NULL,
  time_millis INTEGER NOT NULL,
  value INTEGER NOT NULL,
  PRIMARY KEY (metric, time_millis)
)
''';

  /// The `CREATE TABLE`s for the daily vitals cache, applied on upgrade from < v5.
  static const String createVitalsDailyAggregatesTableSql = '''
CREATE TABLE IF NOT EXISTS `vitals_daily_aggregates` (
    `metric` TEXT NOT NULL,
    `epoch_day` INTEGER NOT NULL,
    `value_sum` REAL NOT NULL,
    `secondary_sum` REAL,
    `sample_count` INTEGER NOT NULL,
    PRIMARY KEY(`metric`, `epoch_day`)
)''';

  static const String createVitalsSyncCursorsTableSql = '''
CREATE TABLE IF NOT EXISTS `vitals_sync_cursors` (
    `metric` TEXT NOT NULL,
    `changes_token` TEXT,
    `last_full_sync_millis` INTEGER,
    PRIMARY KEY(`metric`)
)''';

  /// The `CREATE TABLE` for the feel-check log, applied on upgrade from < v4.
  static const String createFeelChecksTableSql = '''
CREATE TABLE IF NOT EXISTS `feel_checks` (
    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    `recorded_at_millis` INTEGER NOT NULL,
    `rating` INTEGER NOT NULL
)''';

  static const BeverageMigration migration1To3 = BeverageMigration(1);
  static const BeverageMigration migration2To3 = BeverageMigration(2);

  /// The exact Room `CREATE TABLE` statement for the `beverages` table, kept
  /// verbatim so migrations from legacy schema versions are faithful.
  static const String createBeveragesTableSql = '''
CREATE TABLE IF NOT EXISTS `beverages` (
    `id` TEXT NOT NULL,
    `name` TEXT NOT NULL,
    `category` TEXT,
    `volume_milliliters` REAL NOT NULL,
    `hydration_multiplier` REAL NOT NULL,
    `is_preloaded` INTEGER NOT NULL,
    `is_deleted` INTEGER NOT NULL,
    `sort_order` INTEGER NOT NULL,
    `energy_kcal` REAL,
    `protein_grams` REAL,
    `total_carbohydrate_grams` REAL,
    `total_fat_grams` REAL,
    `dietary_fiber_grams` REAL,
    `sugar_grams` REAL,
    `saturated_fat_grams` REAL,
    `sodium_grams` REAL,
    `potassium_grams` REAL,
    `calcium_grams` REAL,
    `caffeine_grams` REAL,
    PRIMARY KEY(`id`)
)''';
}
