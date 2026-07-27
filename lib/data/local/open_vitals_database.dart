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
  bodyEnergy('body_energy'),

  /// Garmin intensity minutes — the running daily totals, in minutes.
  moderateMinutes('moderate_minutes'),
  vigorousMinutes('vigorous_minutes'),

  /// From the metrics file. Health Connect holds VO2 max, so it is NOT here;
  /// these are the estimates it has no type for.
  ///
  /// [recoveryTime] is in minutes, [trainingReadiness] is 0..100, and the two
  /// training loads are Garmin's own unitless scale.
  recoveryTime('recovery_time'),
  trainingReadiness('training_readiness'),
  trainingLoadAcute('training_load_acute'),
  trainingLoadChronic('training_load_chronic'),

  /// The watch's own sleep score for a night, 0..100, timestamped at the
  /// session start. Distinct from anything the app derives from stages.
  sleepScore('sleep_score'),
  sleepAwakenings('sleep_awakenings'),

  /// How long the watch itself counted the sleeper awake, in SECONDS.
  ///
  /// The number to compare our stage-derived total against — they have
  /// disagreed by nearly an hour on a real night.
  sleepAwakeSeconds('sleep_awake_seconds'),

  /// Garmin's undocumented "sleep pressure", stored raw.
  sleepPressure('sleep_pressure'),

  /// Sleep Coach, in minutes: the usual nightly need, and what the night's
  /// strain actually called for.
  sleepNeedNormalMinutes('sleep_need_normal_minutes'),
  sleepNeedMinutes('sleep_need_minutes');

  const GarminWellnessMetric(this.storageName);
  final String storageName;
}

/// One row per local calendar day of the Body Energy chain: the day's headline
/// numbers plus everything its input summary carries.
///
/// Keyed by [epochDay] ALONE, deliberately. The [signature] is a *validity
/// stamp*, not a discriminator: there is exactly one true timeline per day at
/// any moment. Keying by `(day, signature)` would accumulate an orphan row for
/// every calibration edit and force each chain read to filter; keying by day
/// means a recompute overwrites in place and the table can never exceed one row
/// per day the user has lived. A signature mismatch on read is simply a miss.
///
/// [endScore] is what seeds the *next* day — the reason this table exists. It
/// is readable without touching [BodyEnergyBuckets], which is why the buckets
/// are a separate table rather than an encoded column here: the chain walk-back
/// asks this question up to a fortnight's worth of times per screen open and
/// must not decode 288 points to answer it.
class BodyEnergyDays extends Table {
  IntColumn get epochDay => integer().named('epoch_day')();

  /// The per-day signature (`v5|<calibration+profile hash>|<permission hash>`)
  /// this row was computed under, compared on read against the signature built
  /// for THIS row's own date — the body profile's signature varies by date, so
  /// one built for day D can never validate day D-1.
  TextColumn get signature => text()();

  IntColumn get startScore => integer().named('start_score')();
  IntColumn get endScore => integer().named('end_score')();
  IntColumn get charged => integer()();
  IntColumn get drained => integer()();
  TextColumn get confidence => text()();
  TextColumn get confidenceReason => text().named('confidence_reason')();
  IntColumn get generatedAtMillis => integer().named('generated_at_millis')();

  // The input summary, one column per field.
  IntColumn get algorithmVersion => integer().named('algorithm_version')();
  IntColumn get bucketMinutes => integer().named('bucket_minutes')();
  IntColumn get heartRateSampleCount =>
      integer().named('heart_rate_sample_count')();
  IntColumn get hrvSampleCount => integer().named('hrv_sample_count')();
  IntColumn get sleepSessionCount => integer().named('sleep_session_count')();
  IntColumn get workoutCount => integer().named('workout_count')();
  IntColumn get respiratorySampleCount =>
      integer().named('respiratory_sample_count')();
  BoolColumn get hasRestingHeartRate =>
      boolean().named('has_resting_heart_rate')();
  BoolColumn get hasBaselineRestingHeartRate =>
      boolean().named('has_baseline_resting_heart_rate')();
  BoolColumn get hasObservedMaxHeartRate =>
      boolean().named('has_observed_max_heart_rate')();
  BoolColumn get hasHrvBaseline => boolean().named('has_hrv_baseline')();
  BoolColumn get hasRespiratoryBaseline =>
      boolean().named('has_respiratory_baseline')();
  IntColumn get previousEndScore =>
      integer().named('previous_end_score').nullable()();
  BoolColumn get carryOverFloorApplied =>
      boolean().named('carry_over_floor_applied')();
  TextColumn get seedSource => text().named('seed_source')();
  TextColumn get calibrationMode => text().named('calibration_mode')();

  @override
  Set<Column> get primaryKey => {epochDay};

  @override
  String get tableName => 'body_energy_days';
}

/// The 5-minute buckets behind each [BodyEnergyDays] row — ~288 for a full day.
///
/// [epochDay] is the LOCAL calendar day and is stored explicitly rather than
/// derived from [timeMillis]: for most of the world a bucket's UTC instant
/// falls on a different UTC day than its local date, so deriving it would
/// scatter one day's buckets across two partitions.
///
/// Enum-valued columns hold their `storageName`, matching
/// [GarminWellnessSamples.metric] — greppable in a `sqlite3` dump and immune to
/// a Dart enum being reordered.
class BodyEnergyBuckets extends Table {
  IntColumn get epochDay => integer().named('epoch_day')();

  /// Bucket start, UTC milliseconds since the epoch.
  IntColumn get timeMillis => integer().named('time_millis')();

  IntColumn get score => integer()();
  RealColumn get delta => real()();
  TextColumn get state => text()();
  TextColumn get confidence => text()();
  RealColumn get charge => real()();
  RealColumn get intensityDrain => real().named('intensity_drain')();
  RealColumn get activityEnergyDrain => real().named('activity_energy_drain')();
  RealColumn get basalDrain => real().named('basal_drain')();
  RealColumn get stressDrain => real().named('stress_drain')();
  RealColumn get recoveryDebtDrain => real().named('recovery_debt_drain')();
  TextColumn get primaryInfluence => text().named('primary_influence')();

  @override
  Set<Column> get primaryKey => {epochDay, timeMillis};

  @override
  String get tableName => 'body_energy_buckets';
}

/// How many days of 5-minute buckets are kept. Past this the day's summary row
/// survives — so the chain, and any long-range daily-score chart, stay intact —
/// but its buckets are dropped. Buckets are ~99% of the chain's bytes and
/// nothing reads them more than a few weeks back.
const int bodyEnergyBucketRetentionDays = 120;

/// The [VitalsSyncCursors] key the Body Energy chain keeps its bookkeeping
/// under. That table is generic per-key sync state, so the chain reuses it
/// rather than cloning a two-column table (the same reasoning as
/// [caloriesBurnedCacheMetric]).
///
/// `changes_token` holds the GLOBAL signature — algorithm version, calibration
/// and permissions, without the per-day profile component — so a calibration
/// edit can purge the whole chain in one comparison. `last_full_sync_millis` is
/// the warm service's last completed pass.
const String bodyEnergyChainCursorKey = 'bodyEnergyChain.v1';

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

/// The Body Energy chain: day summaries, their 5-minute buckets, and the shared
/// sync cursor row keyed by [bodyEnergyChainCursorKey].
///
/// Deliberately no SQL "most recent stored day at or before D": whether a row
/// is usable depends on the signature computed for *that row's own date*, which
/// is a Dart-side calculation, so such a query would routinely hand back rows
/// the caller must reject and loop straight back into SQL. [daysBetween]
/// answers the whole lookback window in one query and the walk lives in the
/// repository, where the signature knowledge is.
@DriftAccessor(
  tables: [BodyEnergyDays, BodyEnergyBuckets, VitalsSyncCursors],
)
class BodyEnergyTimelineDao extends DatabaseAccessor<OpenVitalsDatabase>
    with _$BodyEnergyTimelineDaoMixin {
  BodyEnergyTimelineDao(super.db);

  Future<BodyEnergyDay?> day(int epochDay) {
    return (select(bodyEnergyDays)
          ..where((d) => d.epochDay.equals(epochDay))
          ..limit(1))
        .getSingleOrNull();
  }

  /// Summaries for `[startEpochDay, endEpochDay]`, oldest first. The chain
  /// walk-back reads its whole lookback window with this in ONE query and never
  /// touches a bucket.
  Future<List<BodyEnergyDay>> daysBetween(int startEpochDay, int endEpochDay) {
    return (select(bodyEnergyDays)
          ..where((d) => d.epochDay.isBetweenValues(startEpochDay, endEpochDay))
          ..orderBy([(d) => OrderingTerm(expression: d.epochDay)]))
        .get();
  }

  /// Reactive variant, so a multi-day view refreshes when the warm service
  /// writes.
  Stream<List<BodyEnergyDay>> watchDaysBetween(
    int startEpochDay,
    int endEpochDay,
  ) {
    return (select(bodyEnergyDays)
          ..where((d) => d.epochDay.isBetweenValues(startEpochDay, endEpochDay))
          ..orderBy([(d) => OrderingTerm(expression: d.epochDay)]))
        .watch();
  }

  Future<List<BodyEnergyBucket>> bucketsForDay(int epochDay) {
    return (select(bodyEnergyBuckets)
          ..where((b) => b.epochDay.equals(epochDay))
          ..orderBy([(b) => OrderingTerm(expression: b.timeMillis)]))
        .get();
  }

  /// Buckets across a day range in primary-key order — a straight index scan,
  /// and the single query a multi-day timeline chart needs.
  Future<List<BodyEnergyBucket>> bucketsBetweenDays(
    int startEpochDay,
    int endEpochDay,
  ) {
    return (select(bodyEnergyBuckets)
          ..where((b) => b.epochDay.isBetweenValues(startEpochDay, endEpochDay))
          ..orderBy([
            (b) => OrderingTerm(expression: b.epochDay),
            (b) => OrderingTerm(expression: b.timeMillis),
          ]))
        .get();
  }

  /// The newest stored day, for the warm service's anchor and for diagnostics.
  Future<BodyEnergyDay?> latestDay() {
    return (select(bodyEnergyDays)
          ..orderBy([
            (d) => OrderingTerm(
                expression: d.epochDay, mode: OrderingMode.desc),
          ])
          ..limit(1))
        .getSingleOrNull();
  }

  Future<int> countDays() async {
    final rows = await (selectOnly(bodyEnergyDays)
          ..addColumns([bodyEnergyDays.epochDay.count()]))
        .get();
    return rows.first.read(bodyEnergyDays.epochDay.count()) ?? 0;
  }

  /// How many buckets a day still has — a count rather than a read, because the
  /// only caller is asking whether a stored day is worth protecting from an
  /// empty recompute, not what is in it.
  Future<int> countBucketsForDay(int epochDay) async {
    final count = bodyEnergyBuckets.timeMillis.count();
    final rows = await (selectOnly(bodyEnergyBuckets)
          ..addColumns([count])
          ..where(bodyEnergyBuckets.epochDay.equals(epochDay)))
        .get();
    return rows.first.read(count) ?? 0;
  }

  /// Replace one day atomically: its old buckets go, the new ones land, and the
  /// summary is upserted — all in one transaction, so a crash mid-write can
  /// never leave a summary whose `end_score` disagrees with its last bucket.
  ///
  /// A full rewrite even when only the tail changed, which recomputing today
  /// mostly is. That is deliberate and measured: **2.7 ms** for a whole
  /// 288-bucket day on a file-backed database, against the ~8 Health Connect
  /// reads that had to happen first to produce those buckets. Writing only the
  /// changed tail would have to diff against what is stored, and would trade a
  /// transaction that cannot half-apply for one that can. Do not "optimise"
  /// this without a profile showing it matters.
  Future<void> upsertDay(
    BodyEnergyDaysCompanion summary,
    List<BodyEnergyBucketsCompanion> buckets,
  ) async {
    final epochDay = summary.epochDay.value;
    await transaction(() async {
      await (delete(bodyEnergyBuckets)..where((b) => b.epochDay.equals(epochDay)))
          .go();
      await into(bodyEnergyDays).insertOnConflictUpdate(summary);
      if (buckets.isNotEmpty) {
        await batch((b) => b.insertAll(bodyEnergyBuckets, buckets));
      }
    });
  }

  /// Forward ripple: drop `[startEpochDay, endEpochDay]` from both tables.
  /// Recomputing a day changes the seed of every day after it, so those days'
  /// stored scores are claims about a chain that no longer exists.
  Future<void> deleteDays(int startEpochDay, int endEpochDay) async {
    if (endEpochDay < startEpochDay) return;
    await transaction(() async {
      await (delete(bodyEnergyBuckets)
            ..where(
                (b) => b.epochDay.isBetweenValues(startEpochDay, endEpochDay)))
          .go();
      await (delete(bodyEnergyDays)
            ..where(
                (d) => d.epochDay.isBetweenValues(startEpochDay, endEpochDay)))
          .go();
    });
  }

  /// Retention: drop buckets strictly before [epochDay], keeping the summaries
  /// so the chain and any long-range daily chart survive.
  Future<void> purgeBucketsBefore(int epochDay) {
    return (delete(bodyEnergyBuckets)
          ..where((b) => b.epochDay.isSmallerThanValue(epochDay)))
        .go();
  }

  /// Everything, plus the cursor — the algorithm/calibration-change reset.
  Future<void> purgeAll() async {
    await transaction(() async {
      await delete(bodyEnergyBuckets).go();
      await delete(bodyEnergyDays).go();
      await (delete(vitalsSyncCursors)
            ..where((c) => c.metric.equals(bodyEnergyChainCursorKey)))
          .go();
    });
  }

  Future<VitalsSyncCursor?> chainCursor() {
    return (select(vitalsSyncCursors)
          ..where((c) => c.metric.equals(bodyEnergyChainCursorKey))
          ..limit(1))
        .getSingleOrNull();
  }

  /// Upsert the chain's bookkeeping. Each field is left untouched when omitted,
  /// so recording a completed pass cannot clear the stored signature.
  Future<void> writeChainCursor({
    String? globalSignature,
    int? lastPassMillis,
  }) async {
    final patch = VitalsSyncCursorsCompanion(
      changesToken:
          globalSignature == null ? const Value.absent() : Value(globalSignature),
      lastFullSyncMillis:
          lastPassMillis == null ? const Value.absent() : Value(lastPassMillis),
    );
    final updated = await (update(vitalsSyncCursors)
          ..where((c) => c.metric.equals(bodyEnergyChainCursorKey)))
        .write(patch);
    if (updated == 0) {
      await into(vitalsSyncCursors).insert(
        VitalsSyncCursorsCompanion.insert(
          metric: bodyEnergyChainCursorKey,
          changesToken: Value(globalSignature),
          lastFullSyncMillis: Value(lastPassMillis),
        ),
      );
    }
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

  /// Drop a metric's cached days and its sync cursor — retiring a legacy key
  /// after a cache-format version bump.
  Future<void> purgeMetric(String metric) async {
    await transaction(() async {
      await (delete(vitalsDailyAggregates)..where((a) => a.metric.equals(metric)))
          .go();
      await (delete(vitalsSyncCursors)..where((c) => c.metric.equals(metric)))
          .go();
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
///
/// The `.v2` suffix is the cache format version: bumping it makes the cursor
/// lookup miss, which forces a full rebuild on the next sync. v2 = buckets
/// dated by midpoint (DST) and Health Connect's synthesized basal baseline
/// filtered out — rows written under v1 contain both artifacts.
const String caloriesBurnedCacheMetric = 'totalCaloriesBurned.v2';

/// Keys the calories cache wrote under before [caloriesBurnedCacheMetric]'s
/// current version. A full sync purges them so a version bump does not leave
/// orphaned rows (and a stale cursor) behind.
const List<String> legacyCaloriesBurnedCacheMetrics = ['totalCaloriesBurned'];

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
    VitalsDailyAggregates,
    VitalsSyncCursors,
    GarminWellnessSamples,
    BodyEnergyDays,
    BodyEnergyBuckets,
  ],
  daos: [
    BeverageDao,
    VitalsDailyCacheDao,
    GarminWellnessDao,
    BodyEnergyTimelineDao,
  ],
)
class OpenVitalsDatabase extends _$OpenVitalsDatabase {
  /// Construct with any [QueryExecutor]. Tests pass `NativeDatabase.memory()`
  /// (from `package:drift/native.dart`); the app wires a file-backed executor.
  OpenVitalsDatabase(super.executor);

  @override
  int get schemaVersion => 8;

  @override
  MigrationStrategy get migration => MigrationStrategy(
        onCreate: (m) => m.createAll(),
        onUpgrade: (m, from, to) async {
          // Both Room migrations (1->3 and 2->3) simply create the beverages
          // table; anything before v3 needs it created best-effort.
          if (from < 3) {
            await customStatement(createBeveragesTableSql);
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
          // v7 adds the Body Energy chain: one summary row per day (whose
          // end_score seeds the next day) plus its 5-minute buckets. Replaces
          // the SharedPreferences timeline cache, which could not answer "what
          // did yesterday end on" without decoding 288 encoded points.
          if (from < 7) {
            await customStatement(createBodyEnergyDaysTableSql);
            await customStatement(createBodyEnergyBucketsTableSql);
          }
          // v8 drops the Body Energy feel-check log. The manual "How's your
          // energy" card was removed, so nothing writes it and nothing reads
          // it; the gains are fitted from watch readings alone. Dropped rather
          // than left orphaned because a table no code can reach is a table the
          // next reader has to work out the status of.
          if (from < 8) {
            await customStatement(dropFeelChecksTableSql);
          }
        },
      );

  /// Applied on upgrade to v8. Irreversible: any check-ins a user logged before
  /// the card was removed go with it. They have no consumer left, and the fit
  /// that read them no longer exists.
  static const String dropFeelChecksTableSql =
      'DROP TABLE IF EXISTS `feel_checks`';

  /// The `CREATE TABLE`s for the Body Energy chain, applied on upgrade from
  /// < v7.
  ///
  /// These must stay equivalent to what drift's `createAll()` emits from
  /// [BodyEnergyDays] / [BodyEnergyBuckets]. This project keeps no
  /// `drift_schemas` snapshot and runs no `SchemaVerifier`, so the migration
  /// test that diffs a migrated schema against a freshly created one is the
  /// only guard against these drifting apart.
  static const String createBodyEnergyDaysTableSql = '''
CREATE TABLE IF NOT EXISTS `body_energy_days` (
    `epoch_day` INTEGER NOT NULL,
    `signature` TEXT NOT NULL,
    `start_score` INTEGER NOT NULL,
    `end_score` INTEGER NOT NULL,
    `charged` INTEGER NOT NULL,
    `drained` INTEGER NOT NULL,
    `confidence` TEXT NOT NULL,
    `confidence_reason` TEXT NOT NULL,
    `generated_at_millis` INTEGER NOT NULL,
    `algorithm_version` INTEGER NOT NULL,
    `bucket_minutes` INTEGER NOT NULL,
    `heart_rate_sample_count` INTEGER NOT NULL,
    `hrv_sample_count` INTEGER NOT NULL,
    `sleep_session_count` INTEGER NOT NULL,
    `workout_count` INTEGER NOT NULL,
    `respiratory_sample_count` INTEGER NOT NULL,
    `has_resting_heart_rate` INTEGER NOT NULL
        CHECK (`has_resting_heart_rate` IN (0, 1)),
    `has_baseline_resting_heart_rate` INTEGER NOT NULL
        CHECK (`has_baseline_resting_heart_rate` IN (0, 1)),
    `has_observed_max_heart_rate` INTEGER NOT NULL
        CHECK (`has_observed_max_heart_rate` IN (0, 1)),
    `has_hrv_baseline` INTEGER NOT NULL
        CHECK (`has_hrv_baseline` IN (0, 1)),
    `has_respiratory_baseline` INTEGER NOT NULL
        CHECK (`has_respiratory_baseline` IN (0, 1)),
    `previous_end_score` INTEGER NULL,
    `carry_over_floor_applied` INTEGER NOT NULL
        CHECK (`carry_over_floor_applied` IN (0, 1)),
    `seed_source` TEXT NOT NULL,
    `calibration_mode` TEXT NOT NULL,
    PRIMARY KEY (`epoch_day`)
)''';

  static const String createBodyEnergyBucketsTableSql = '''
CREATE TABLE IF NOT EXISTS `body_energy_buckets` (
    `epoch_day` INTEGER NOT NULL,
    `time_millis` INTEGER NOT NULL,
    `score` INTEGER NOT NULL,
    `delta` REAL NOT NULL,
    `state` TEXT NOT NULL,
    `confidence` TEXT NOT NULL,
    `charge` REAL NOT NULL,
    `intensity_drain` REAL NOT NULL,
    `activity_energy_drain` REAL NOT NULL,
    `basal_drain` REAL NOT NULL,
    `stress_drain` REAL NOT NULL,
    `recovery_debt_drain` REAL NOT NULL,
    `primary_influence` TEXT NOT NULL,
    PRIMARY KEY (`epoch_day`, `time_millis`)
)''';

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
