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

@DriftDatabase(tables: [Beverages, FeelChecks], daos: [BeverageDao, FeelCheckDao])
class OpenVitalsDatabase extends _$OpenVitalsDatabase {
  /// Construct with any [QueryExecutor]. Tests pass `NativeDatabase.memory()`
  /// (from `package:drift/native.dart`); the app wires a file-backed executor.
  OpenVitalsDatabase(super.executor);

  @override
  int get schemaVersion => 4;

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
        },
      );

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
