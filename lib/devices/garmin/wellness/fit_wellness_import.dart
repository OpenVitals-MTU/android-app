/// Maps decoded Garmin wellness FIT data onto the [ImportRecord] model that the
/// write pipeline (`HealthDataSource.insertImportedRecords`) understands — the
/// same pipeline the Apple Health importer uses. Activities keep their own
/// route/`writeImportedActivities` path; this is for the wellness file types
/// (sleep first). See docs/reference/garmin-fit-files.md.
library;

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

  // Cumulative counters → one running DAILY TOTAL each, per local day.
  //
  // These are day-cumulative per activity type, and a file may restate the day
  // from zero rather than continuing where the last one stopped. Turning each
  // file into a delta over its own window therefore double-counted badly: a
  // real day measured 540 steps on the wrist and 1403 in Health Connect across
  // thirteen syncs, because two files restated the whole day and a third mixed
  // a walking counter at 540 with a generic one at 0 into a 540-step "delta".
  //
  // Keying on the DAY instead makes a re-sync overwrite rather than add, so the
  // count converges on the day's total however often the watch is synced.
  for (final day in _monitoringDays(m)) {
    final steps = _dailyTotal(m.stepPoints, day);
    final start = day.start;
    final end = day.end;
    if (steps > 0) {
      records.add(StepsImportRecord(
        clientRecordId: 'garmin_fit_steps_${day.key}',
        startTime: start,
        startZoneOffset: null,
        endTime: end,
        endZoneOffset: null,
        count: steps,
      ));
    }
    final distanceRaw = _dailyTotal(m.distancePoints, day);
    if (distanceRaw > 0) {
      records.add(DistanceImportRecord(
        clientRecordId: 'garmin_fit_distance_${day.key}',
        startTime: start,
        startZoneOffset: null,
        endTime: end,
        endZoneOffset: null,
        meters: distanceRaw / _fitMonitoringDistanceScale,
      ));
    }
    final calories = _dailyTotal(m.caloriePoints, day);
    if (calories > 0) {
      records.add(ActiveCaloriesBurnedImportRecord(
        clientRecordId: 'garmin_fit_active_cal_${day.key}',
        startTime: start,
        startZoneOffset: null,
        endTime: end,
        endZoneOffset: null,
        kilocalories: calories.toDouble(),
      ));
    }
  }

  return records;
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

/// The local days a monitoring summary carried samples for.
List<_MonitoringDay> _monitoringDays(FitMonitoringSummary m) {
  final lastByDay = <DateTime, DateTime>{};
  void see(DateTime t) {
    final local = t.toLocal();
    final day = DateTime(local.year, local.month, local.day);
    final seen = lastByDay[day];
    if (seen == null || local.isAfter(seen)) lastByDay[day] = local;
  }

  for (final p in m.stepPoints) {
    see(p.time);
  }
  for (final p in m.distancePoints) {
    see(p.time);
  }
  for (final p in m.caloriePoints) {
    see(p.time);
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

/// A day's total for a cumulative counter: the highest value reached by each
/// activity type, summed.
///
/// Per type and not overall, because the counters run independently — walking
/// at 540 next to a generic counter still at 0 is not a 540-step change, which
/// is exactly what a naive `max - min` across all points made of it.
int _dailyTotal(List<FitMonitoringPoint> points, _MonitoringDay day) {
  final maxByType = <int, int>{};
  for (final p in points) {
    final local = p.time.toLocal();
    if (local.year != day.start.year ||
        local.month != day.start.month ||
        local.day != day.start.day) {
      continue;
    }
    final seen = maxByType[p.activityType];
    if (seen == null || p.value > seen) maxByType[p.activityType] = p.value;
  }
  var total = 0;
  for (final value in maxByType.values) {
    total += value;
  }
  return total;
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
