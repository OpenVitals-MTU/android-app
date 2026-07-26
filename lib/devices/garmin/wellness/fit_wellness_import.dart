/// Maps decoded Garmin wellness FIT data onto the [ImportRecord] model that the
/// write pipeline (`HealthDataSource.insertImportedRecords`) understands — the
/// same pipeline the Apple Health importer uses. Activities keep their own
/// route/`writeImportedActivities` path; this is for the wellness file types
/// (sleep first). See docs/reference/garmin-fit-files.md.
library;

import 'dart:math' as math;

import '../../../domain/model/apple_health_import_records.dart';
import 'garmin_fit_wellness.dart';

/// Health Connect file type for a Garmin sleep FIT file (`file_id.type`).
const int fitFileTypeSleep = 49;

/// Turns a decoded [FitSleepSession] into a `SleepSessionRecord` import, or an
/// empty list if no stage mapped to a Health Connect stage.
///
/// The [clientRecordId] is derived from the session start so a re-import of the
/// same export dedupes instead of duplicating the night (Health Connect keys
/// upserts on `clientRecordId`).
List<ImportRecord> fitSleepImportRecords(FitSleepSession session) {
  final stages = <SleepStageValue>[];
  for (final stage in session.stages) {
    final mapped = _sleepStageFor(stage.level);
    if (mapped == null) continue;
    stages.add(
      SleepStageValue(
        startTime: stage.start,
        endTime: stage.end,
        stage: mapped,
      ),
    );
  }
  if (stages.isEmpty) return const [];
  return [
    SleepSessionImportRecord(
      clientRecordId:
          'garmin_fit_sleep_${session.start.millisecondsSinceEpoch}',
      startTime: session.start,
      startZoneOffset: null,
      endTime: session.end,
      endZoneOffset: null,
      title: 'Sleep',
      stages: stages,
    ),
  ];
}

/// Turns a decoded [FitHrvReading] into a `HeartRateVariabilityRmssdRecord`
/// import. Deterministic [clientRecordId] so a re-import dedupes.
List<ImportRecord> fitHrvImportRecords(FitHrvReading reading) => [
      HeartRateVariabilityRmssdImportRecord(
        clientRecordId:
            'garmin_fit_hrv_${reading.time.millisecondsSinceEpoch}',
        time: reading.time,
        zoneOffset: null,
        rmssdMillis: reading.rmssdMillis,
      ),
    ];

/// Turns the metrics file's VO2 max into a `Vo2MaxRecord` import.
///
/// Only VO2 max: recovery time, training readiness and training load have no
/// Health Connect type and go to the app's own table instead.
List<ImportRecord> fitMetricsImportRecords(FitMetricsSummary metrics) {
  final time = metrics.time;
  final vo2Max = metrics.vo2Max;
  if (time == null || vo2Max == null) return const [];
  return [
    Vo2MaxImportRecord(
      clientRecordId: 'garmin_fit_vo2max_${time.millisecondsSinceEpoch}',
      time: time,
      zoneOffset: null,
      vo2MillilitersPerMinuteKilogram: vo2Max,
    ),
  ];
}

/// Turns a Health Snapshot's SpO2 and respiration samples into Health Connect
/// records. Its stress and Body Battery have no Health Connect type and go to
/// the app's own table instead.
///
/// The `clientRecordId`s are keyed on the sample instant, so a re-import of the
/// same recording overwrites rather than duplicating — and they are namespaced
/// apart from the all-day series, which is a genuinely different measurement of
/// the same quantity and must not overwrite it.
List<ImportRecord> fitHealthSnapshotImportRecords(FitHealthSnapshot snapshot) =>
    [
      for (final (at, percent) in snapshot.spo2)
        OxygenSaturationImportRecord(
          clientRecordId: 'garmin_fit_hsa_spo2_${at.millisecondsSinceEpoch}',
          time: at,
          zoneOffset: null,
          percent: percent.toDouble(),
        ),
      for (final (at, rate) in snapshot.respiration)
        RespiratoryRateImportRecord(
          clientRecordId: 'garmin_fit_hsa_rr_${at.millisecondsSinceEpoch}',
          time: at,
          zoneOffset: null,
          rate: rate,
        ),
    ];

/// Turns daytime naps into `SleepSessionRecord` imports.
///
/// Deliberately stage-less: the nap message bounds the sleep but carries no
/// stage breakdown, and inventing one would put fabricated stages next to the
/// measured ones from a night.
List<ImportRecord> fitNapImportRecords(List<FitNap> naps) => [
      for (final nap in naps)
        if (nap.end.isAfter(nap.start))
          SleepSessionImportRecord(
            clientRecordId:
                'garmin_fit_nap_${nap.start.millisecondsSinceEpoch}',
            startTime: nap.start,
            startZoneOffset: null,
            endTime: nap.end,
            endZoneOffset: null,
            title: 'Nap',
            stages: const [],
          ),
    ];

/// FIT `monitoring.distance` is in centimetres-of-a-metre (raw ÷ 100 = metres).
const double _fitMonitoringDistanceScale = 100.0;

/// Turns a monitoring file (type 32) into its Health Connect records: the
/// one-per-file summaries (resting HR, BMR), the HR and respiration series
/// aggregated to **hourly** (per the design decision), and the cumulative step,
/// distance and active-calorie counters as one running DAILY TOTAL each.
///
/// The counters are day-cumulative per activity type, so they are recorded as a
/// total per local day keyed on that day — NOT as a per-file delta. A delta
/// model over-counted: files restate the day from zero and several activity
/// counters share the series, and thirteen syncs of a 540-step day wrote 1403
/// steps. See docs/reference/garmin-fit-files.md.
List<ImportRecord> fitMonitoringImportRecords(FitMonitoringSummary m) {
  final records = <ImportRecord>[];

  final rhrTime = m.restingHeartRateTime;
  final rhrBpm = m.restingHeartRateBpm;
  if (rhrTime != null && rhrBpm != null) {
    records.add(RestingHeartRateImportRecord(
      clientRecordId: 'garmin_fit_resting_hr_${rhrTime.millisecondsSinceEpoch}',
      time: rhrTime,
      zoneOffset: null,
      beatsPerMinute: rhrBpm,
    ));
  }
  final bmrTime = m.bmrTime;
  final bmr = m.bmrKcalPerDay;
  if (bmrTime != null && bmr != null) {
    records.add(BasalMetabolicRateImportRecord(
      clientRecordId: 'garmin_fit_bmr_${bmrTime.millisecondsSinceEpoch}',
      time: bmrTime,
      zoneOffset: null,
      kilocaloriesPerDay: bmr,
    ));
  }

  // HR — one series record per hour, samples packed in.
  //
  // Keyed on the bucket's FIRST SAMPLE, not on the hour. Keying on the hour
  // assumed one file per day, so no two files could ever touch the same hour.
  // A watch sync breaks that: it delivers a fresh file every few minutes, so
  // several files land in one hour and, sharing a clientRecordId, each REPLACED
  // the last — an hour of heart rate collapsing to whichever sliver synced most
  // recently. First-sample keying stays idempotent for a re-imported file (same
  // samples, same key) while letting successive files coexist.
  for (final entry in _bucketByHour(m.heartRateSamples, (s) => s.$1).entries) {
    final samples = entry.value..sort((a, b) => a.$1.compareTo(b.$1));
    final start = samples.first.$1;
    final end = samples.last.$1.isAfter(start)
        ? samples.last.$1
        : start.add(const Duration(seconds: 1));
    records.add(HeartRateImportRecord(
      clientRecordId: 'garmin_fit_hr_${start.millisecondsSinceEpoch}',
      startTime: start,
      startZoneOffset: null,
      endTime: end,
      endZoneOffset: null,
      samples: [for (final s in samples) HeartRateSampleValue(s.$1, s.$2)],
    ));
  }

  // Respiration — one averaged reading per hour bucket, keyed and timed on its
  // first sample for the same reason as HR above. Stamping it at the top of the
  // hour additionally made every file in that hour claim the same instant.
  for (final entry in _bucketByHour(m.respiration, (r) => r.$1).entries) {
    final readings = entry.value..sort((a, b) => a.$1.compareTo(b.$1));
    final avg =
        readings.map((r) => r.$2).reduce((a, b) => a + b) / readings.length;
    final at = readings.first.$1;
    records.add(RespiratoryRateImportRecord(
      clientRecordId: 'garmin_fit_resp_${at.millisecondsSinceEpoch}',
      time: at,
      zoneOffset: null,
      rate: avg,
    ));
  }

  // The cumulative counters are NOT mapped here — see
  // [fitMonitoringCounterRecords]. They are the one part of a monitoring file
  // that cannot be read a file at a time.

  return records;
}

/// The cumulative step / distance / active-calorie counters a monitoring file
/// carried, kept apart from the rest so a caller can accumulate them across
/// every file of a sync before mapping.
///
/// Everything else in a monitoring file reads a file at a time: a heart-rate
/// bucket is complete in the file that holds it. These are not — they are
/// day-cumulative, and what happened between the last snapshot of one file and
/// the first of the next lives in NEITHER file's own numbers, only in the
/// difference between them.
class FitMonitoringCounters {
  const FitMonitoringCounters({
    this.steps = const [],
    this.distance = const [],
    this.calories = const [],
  });

  final List<FitMonitoringPoint> steps;
  final List<FitMonitoringPoint> distance;
  final List<FitMonitoringPoint> calories;

  bool get isEmpty => steps.isEmpty && distance.isEmpty && calories.isEmpty;

  FitMonitoringCounters merge(FitMonitoringCounters other) =>
      FitMonitoringCounters(
        steps: [...steps, ...other.steps],
        distance: [...distance, ...other.distance],
        calories: [...calories, ...other.calories],
      );
}

/// The counters [m] carried, for accumulating across a sync.
FitMonitoringCounters fitMonitoringCounters(FitMonitoringSummary m) =>
    FitMonitoringCounters(
      steps: m.stepPoints,
      distance: m.distancePoints,
      calories: m.caloriePoints,
    );

/// How far a day's counters have already been imported: the last snapshot taken
/// from them, and what each counter read at that moment.
///
/// This is the piece that makes intraday records safe across syncs. The watch's
/// counters run from midnight, and each file holds only the minutes since the
/// last sync — so the steps between one sync's last snapshot and the next
/// sync's first belong to neither file's internal differences. Carrying the
/// watermark forward closes that seam exactly, and re-importing a file already
/// behind the watermark writes nothing rather than counting it twice.
class FitCounterWatermark {
  const FitCounterWatermark({
    required this.time,
    this.steps = 0,
    this.distance = 0,
    this.calories = 0,
  });

  final DateTime time;
  final int steps;
  final int distance;
  final int calories;
}

/// The counter records, and the watermarks the caller must persist.
class FitCounterImport {
  const FitCounterImport({required this.records, required this.watermarks});

  final List<ImportRecord> records;

  /// By `yyyy-mm-dd` local day, for the caller to store and hand back next time.
  final Map<String, FitCounterWatermark> watermarks;
}

/// Turns the day-cumulative counters into INTRADAY Health Connect records: one
/// per step the counter actually took, spanning the minutes between the two
/// snapshots that bracket it.
///
/// A single record per day is what a cumulative counter most obviously maps to,
/// and it is what this wrote first — but it says only how far you walked, never
/// when, so Health Connect drew a day's steps as one straight ramp from midnight
/// to now. The watch samples the counters about once a minute, so the shape is
/// there to be read; it just has to be read as differences.
///
/// The rules that keep the total honest:
///
///  * The snapshots are the per-instant sums across activity types (see
///    [_dailySnapshots]), so a total moved between buckets never shows up as a
///    step taken.
///  * Only forward differences are recorded. The counters reset at a wear-session
///    boundary, and a reset is not a walk backwards.
///  * Nothing is written for a snapshot at or before [previous]'s watermark: those
///    minutes are already in Health Connect.
///  * A zero difference writes no record. Standing still is not an event, and a
///    night of them would bury the day in empty entries.
FitCounterImport fitMonitoringCounterRecords(
  FitMonitoringCounters counters, {
  Map<String, FitCounterWatermark> previous = const {},
}) {
  final records = <ImportRecord>[];
  final watermarks = <String, FitCounterWatermark>{};

  for (final day in _counterDays(counters)) {
    final mark = previous[day.key];
    final steps = _dailySnapshots(counters.steps, day);
    final distance = _dailySnapshots(counters.distance, day);
    final calories = _dailySnapshots(counters.calories, day);

    // The instants any counter reported, so the three stay on one timeline.
    final instants = <DateTime>{
      for (final snapshot in steps) snapshot.time,
      for (final snapshot in distance) snapshot.time,
      for (final snapshot in calories) snapshot.time,
    }.toList()
      ..sort();
    if (instants.isEmpty) continue;

    var from = mark?.time ?? day.start;
    var lastSteps = mark?.steps ?? 0;
    var lastDistance = mark?.distance ?? 0;
    var lastCalories = mark?.calories ?? 0;

    // Deltas folded onto a fixed grid anchored at local midnight, so a record's
    // identity is a pure function of its wall clock.
    final buckets = <int, _CounterDeltas>{};
    // The bucket currently filling, and the counter readings as they stood when
    // it opened. Emitting a half-filled bucket and then overwriting it on the
    // next sync would LOSE its first half, so the open one is left for next
    // time and the watermark rewinds to where it began.
    var openBucket = _counterBucketStart(from, day.start);
    var openSteps = lastSteps;
    var openDistance = lastDistance;
    var openCalories = lastCalories;

    for (final at in instants) {
      // Already imported. Not an error — every sync re-reads the file it was
      // halfway through, and the watch re-offers a file whose archive flag did
      // not stick.
      if (!at.isAfter(from)) continue;

      // The movement accrued over [from, at), so it belongs to the bucket the
      // interval STARTED in — not the one it ended in, which would push a walk
      // forward by up to a bucket every time.
      final bucket = _counterBucketStart(from, day.start);
      final stepsNow = _valueAt(steps, at, lastSteps);
      final distanceNow = _valueAt(distance, at, lastDistance);
      final caloriesNow = _valueAt(calories, at, lastCalories);
      (buckets[bucket] ??= _CounterDeltas(
        DateTime.fromMillisecondsSinceEpoch(bucket),
      )).add(
        steps: math.max(0, stepsNow - lastSteps),
        distance: math.max(0, distanceNow - lastDistance),
        calories: math.max(0, caloriesNow - lastCalories),
        until: at,
      );

      from = at;
      lastSteps = math.max(lastSteps, stepsNow);
      lastDistance = math.max(lastDistance, distanceNow);
      lastCalories = math.max(lastCalories, caloriesNow);

      final reached = _counterBucketStart(from, day.start);
      if (reached != openBucket) {
        openBucket = reached;
        openSteps = lastSteps;
        openDistance = lastDistance;
        openCalories = lastCalories;
      }
    }

    // An interval that starts in one bucket can end in the next, so a bucket's
    // data-driven end can run past its successor's start. Clamp each to the
    // next OCCUPIED bucket: that keeps records non-overlapping without
    // shortening the sparse case, where the gap to the next bucket is real.
    final ordered = buckets.keys.toList()..sort();
    for (var i = 0; i < ordered.length - 1; i++) {
      buckets[ordered[i]]!.clampEndTo(
        DateTime.fromMillisecondsSinceEpoch(ordered[i + 1]),
      );
    }

    for (final entry in buckets.entries) {
      // The still-filling bucket waits for the sync that completes it.
      if (entry.key == openBucket) continue;
      // The day's FIRST bucket carries the legacy day-keyed id on purpose.
      // Before the counters became intraday, one record per day was written as
      // `garmin_fit_steps_<yyyy-mm-dd>`; those records are still in Health
      // Connect, carrying a whole day's total each, and no new id could ever
      // collide with them. Reusing the id here makes the day's first bucket
      // overwrite the stale whole-day record instead of stacking beside it.
      final key = entry.key == day.start.millisecondsSinceEpoch
          ? day.key
          : '${entry.key}';
      records.addAll(entry.value.toRecords(key));
    }

    watermarks[day.key] = FitCounterWatermark(
      time: DateTime.fromMillisecondsSinceEpoch(openBucket),
      steps: openSteps,
      distance: openDistance,
      calories: openCalories,
    );
  }

  return FitCounterImport(records: records, watermarks: watermarks);
}

/// The grid one counter record covers.
///
/// Records are written on a fixed grid from local midnight rather than on the
/// intervals a particular sync happened to see. The interval boundaries depend
/// on which files the watch offered and where the last sync stopped, so an id
/// derived from them changes between runs: a re-sync re-partitions the day and
/// every record after the first lands BESIDE the previous run's rather than
/// replacing it. A grid position is a property of the clock, so the same
/// minutes always produce the same id and Health Connect upserts.
///
/// Fifteen minutes: the watch reports about once a minute, so per-instant
/// records would be ~1440 a day per counter, while an hour is coarse enough to
/// smear a walk across a lunch break.
const Duration _counterBucket = Duration(minutes: 15);

int _counterBucketStart(DateTime at, DateTime dayStart) {
  final elapsed = at.difference(dayStart).inMilliseconds;
  final size = _counterBucket.inMilliseconds;
  final aligned = (elapsed < 0 ? 0 : elapsed ~/ size) * size;
  return dayStart.millisecondsSinceEpoch + aligned;
}

/// One grid bucket's accumulated counter movement.
///
/// The grid fixes the record's IDENTITY and its start; the span still follows
/// the data, running to the end of the last interval folded in. Pinning the end
/// to the grid too would claim a first-sync-of-the-day reading of 8,000 steps
/// happened in the first quarter hour after midnight. Intervals are contiguous
/// and each lands wholly in the bucket it started in, so consecutive buckets
/// still cannot overlap.
class _CounterDeltas {
  _CounterDeltas(this.start) : end = start;

  final DateTime start;
  DateTime end;
  int steps = 0;
  int distance = 0;
  int calories = 0;

  void add({
    required int steps,
    required int distance,
    required int calories,
    required DateTime until,
  }) {
    this.steps += steps;
    this.distance += distance;
    this.calories += calories;
    if (until.isAfter(end)) end = until;
  }

  /// Pulls the end back to [limit] when the last interval folded in ran past
  /// the next occupied bucket. Never pushes it forward, and never before the
  /// start.
  void clampEndTo(DateTime limit) {
    if (end.isAfter(limit)) end = limit.isAfter(start) ? limit : start;
  }

  List<ImportRecord> toRecords(String key) => [
        if (steps > 0)
          StepsImportRecord(
            clientRecordId: 'garmin_fit_steps_$key',
            startTime: start,
            startZoneOffset: null,
            endTime: end,
            endZoneOffset: null,
            count: steps,
          ),
        if (distance > 0)
          DistanceImportRecord(
            clientRecordId: 'garmin_fit_distance_$key',
            startTime: start,
            startZoneOffset: null,
            endTime: end,
            endZoneOffset: null,
            meters: distance / _fitMonitoringDistanceScale,
          ),
        if (calories > 0)
          ActiveCaloriesBurnedImportRecord(
            clientRecordId: 'garmin_fit_active_cal_$key',
            startTime: start,
            startZoneOffset: null,
            endTime: end,
            endZoneOffset: null,
            kilocalories: calories.toDouble(),
          ),
      ];
}

/// The counter reading at [at]: the snapshot taken then, or — when this counter
/// reported nothing at an instant another one did — the last it did report.
int _valueAt(List<_CounterSnapshot> snapshots, DateTime at, int fallback) {
  var value = fallback;
  for (final snapshot in snapshots) {
    if (snapshot.time.isAfter(at)) break;
    value = snapshot.value;
  }
  return value;
}

/// One local day a monitoring file touched, and the span to record it over.
class _MonitoringDay {
  const _MonitoringDay({
    required this.key,
    required this.start,
    required this.end,
  });

  /// Stable `yyyy-mm-dd`, so every sync of the same day writes the same
  /// `clientRecordId` and Health Connect upserts instead of accumulating.
  final String key;

  /// Local midnight. The counter is the whole day's running total, so the
  /// record has to span the whole day or Health Connect would attribute the
  /// day's steps to whatever few minutes the file happened to cover.
  final DateTime start;

  /// The last sample seen for the day — the total is only known up to here.
  final DateTime end;
}

/// The local days the accumulated counters carried readings for.
List<_MonitoringDay> _counterDays(FitMonitoringCounters counters) =>
    _daysOf([counters.steps, counters.distance, counters.calories]);

List<_MonitoringDay> _daysOf(List<List<FitMonitoringPoint>> series) {
  final lastByDay = <DateTime, DateTime>{};
  void see(DateTime t) {
    final local = t.toLocal();
    final day = DateTime(local.year, local.month, local.day);
    final seen = lastByDay[day];
    if (seen == null || local.isAfter(seen)) lastByDay[day] = local;
  }

  for (final points in series) {
    for (final p in points) {
      see(p.time);
    }
  }

  final days = <_MonitoringDay>[];
  for (final entry in lastByDay.entries) {
    final day = entry.key;
    // An interval record must not be empty: a file whose only sample sits at
    // local midnight would otherwise produce start == end.
    final end = entry.value.isAfter(day)
        ? entry.value
        : day.add(const Duration(minutes: 1));
    final month = day.month.toString().padLeft(2, '0');
    final dayOfMonth = day.day.toString().padLeft(2, '0');
    days.add(_MonitoringDay(
      key: '${day.year}-$month-$dayOfMonth',
      start: day,
      end: end,
    ));
  }
  days.sort((a, b) => a.key.compareTo(b.key));
  return days;
}

/// One instant's reading of a counter, summed across activity types.
class _CounterSnapshot {
  const _CounterSnapshot(this.time, this.value);

  final DateTime time;
  final int value;
}

/// The day's counter as it stood at each instant the file reported, in order.
///
/// Summed per instant, and NOT per activity type over the day. The counters run
/// independently — walking at 540 beside a generic counter at 0 is not a
/// 540-step change — but they are not independent of *each other*: the watch
/// moves a total from one bucket to another and zeroes the one it left. A real
/// day showed the generic bucket holding 709 m of distance and 6181 s of active
/// time with ZERO steps, its step count having been reallocated to walking.
/// Taking each bucket's own peak keeps the abandoned peak and adds it to the
/// bucket that inherited it, so 24,724 steps on the wrist reached Health Connect
/// as 49,448 — exactly twice, the same total counted under two types. Adding up
/// one instant at a time cannot double-count a transfer: what leaves one bucket
/// arrives in the other within the same snapshot.
List<_CounterSnapshot> _dailySnapshots(
  List<FitMonitoringPoint> points,
  _MonitoringDay day,
) {
  final ofDay = <FitMonitoringPoint>[];
  var sawDeclaredType = false;
  for (final p in points) {
    final local = p.time.toLocal();
    if (local.year != day.start.year ||
        local.month != day.start.month ||
        local.day != day.start.day) {
      continue;
    }
    ofDay.add(p);
    if (p.activityType != unknownFitActivityType) sawDeclaredType = true;
  }
  ofDay.sort((a, b) => a.time.compareTo(b.time));

  // A counter that never said which activity it belongs to cannot be summed
  // beside ones that do: it is the same day's total under a name of its own, and
  // adding it to a declared bucket counts those steps twice. Dropped — unless
  // the file declared no type at all, where the untyped counter IS the total.
  final byType = <int, int>{};
  final snapshots = <_CounterSnapshot>[];
  var index = 0;
  while (index < ofDay.length) {
    // A snapshot is every point sharing one instant: the file restates each
    // active type at the same timestamp, and reading the sum mid-instant would
    // see the bucket that gained before the bucket that lost.
    final at = ofDay[index].time;
    while (index < ofDay.length && ofDay[index].time == at) {
      final point = ofDay[index];
      index++;
      if (sawDeclaredType && point.activityType == unknownFitActivityType) {
        continue;
      }
      byType[point.activityType] = point.value;
    }
    var total = 0;
    for (final value in byType.values) {
      total += value;
    }
    snapshots.add(_CounterSnapshot(at, total));
  }
  return snapshots;
}

/// Groups items into UTC-hour buckets keyed by the hour's epoch-ms.
Map<int, List<T>> _bucketByHour<T>(
  List<T> items,
  DateTime Function(T) timeOf,
) {
  final buckets = <int, List<T>>{};
  for (final item in items) {
    final t = timeOf(item);
    final hourMs =
        DateTime.utc(t.year, t.month, t.day, t.hour).millisecondsSinceEpoch;
    buckets.putIfAbsent(hourMs, () => []).add(item);
  }
  return buckets;
}

/// Garmin `sleep_level` → Health Connect `SleepSessionRecord.Stage`.
/// `unmeasurable` has no Health Connect stage, so it is dropped (the gap between
/// stages simply carries no classification).
SleepStageType? _sleepStageFor(FitSleepLevel level) => switch (level) {
      FitSleepLevel.awake => SleepStageType.awake,
      FitSleepLevel.light => SleepStageType.light,
      FitSleepLevel.deep => SleepStageType.deep,
      FitSleepLevel.rem => SleepStageType.rem,
      FitSleepLevel.unmeasurable => null,
    };
