/// Maps decoded Garmin wellness FIT data onto the [ImportRecord] model that the
/// write pipeline (`HealthDataSource.insertImportedRecords`) understands — the
/// same pipeline the Apple Health importer uses. Activities keep their own
/// route/`writeImportedActivities` path; this is for the wellness file types
/// (sleep first). See docs/reference/garmin-fit-files.md.
library;

import '../../../domain/model/apple_health_import_records.dart';
import '../../manualentry/activity/routeimport/fit_route_parser.dart';

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

/// FIT `monitoring.distance` is in centimetres-of-a-metre (raw ÷ 100 = metres).
const double _fitMonitoringDistanceScale = 100.0;

/// Turns a monitoring file (type 32) into its Health Connect records: the
/// one-per-file summaries (resting HR, BMR), the HR and respiration series
/// aggregated to **hourly** (per the design decision), and the cumulative step,
/// distance and active-calorie counters as one per-file total each.
///
/// The cumulative counters use a per-file `max − min`: it yields the file
/// window's contribution, returns ~0 for a continuation file (a flat series), so
/// non-overlapping file windows tile the timeline without double-counting. It is
/// approximate — a file that interleaves a mid-file session reset can over-count
/// (see docs/reference/garmin-fit-files.md).
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

  // Cumulative counters → one per-file total each over the file's window.
  final window = _monitoringWindow(m);
  if (window != null) {
    final (start, end) = window;
    final steps = _spanDelta(m.stepPoints);
    if (steps > 0) {
      records.add(StepsImportRecord(
        clientRecordId: 'garmin_fit_steps_${start.millisecondsSinceEpoch}',
        startTime: start,
        startZoneOffset: null,
        endTime: end,
        endZoneOffset: null,
        count: steps,
      ));
    }
    final distanceRaw = _spanDelta(m.distancePoints);
    if (distanceRaw > 0) {
      records.add(DistanceImportRecord(
        clientRecordId: 'garmin_fit_distance_${start.millisecondsSinceEpoch}',
        startTime: start,
        startZoneOffset: null,
        endTime: end,
        endZoneOffset: null,
        meters: distanceRaw / _fitMonitoringDistanceScale,
      ));
    }
    final calories = _spanDelta(m.caloriePoints);
    if (calories > 0) {
      records.add(ActiveCaloriesBurnedImportRecord(
        clientRecordId: 'garmin_fit_active_cal_${start.millisecondsSinceEpoch}',
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

/// `max − min` of a cumulative counter series (the file window's contribution).
int _spanDelta(List<FitMonitoringPoint> points) {
  if (points.isEmpty) return 0;
  var lo = points.first.value;
  var hi = points.first.value;
  for (final p in points) {
    if (p.value < lo) lo = p.value;
    if (p.value > hi) hi = p.value;
  }
  return hi - lo;
}

/// The `[start, end]` a monitoring file spans, across every series it carried,
/// or null if it carried no timestamped data. `end` is nudged past `start` so an
/// interval record is always valid.
(DateTime, DateTime)? _monitoringWindow(FitMonitoringSummary m) {
  DateTime? lo;
  DateTime? hi;
  void see(DateTime t) {
    if (lo == null || t.isBefore(lo!)) lo = t;
    if (hi == null || t.isAfter(hi!)) hi = t;
  }

  for (final s in m.heartRateSamples) {
    see(s.$1);
  }
  for (final r in m.respiration) {
    see(r.$1);
  }
  for (final p in [...m.stepPoints, ...m.distancePoints, ...m.caloriePoints]) {
    see(p.time);
  }
  if (lo == null || hi == null) return null;
  final end = hi!.isAfter(lo!) ? hi! : lo!.add(const Duration(seconds: 1));
  return (lo!, end);
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
