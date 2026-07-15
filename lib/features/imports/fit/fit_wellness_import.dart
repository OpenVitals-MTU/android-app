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

/// Turns the one-per-file monitoring summaries into their Health Connect
/// records: resting heart rate and basal metabolic rate. The high-frequency
/// monitoring series (steps, respiration, per-sample HR) are not handled here —
/// they need downsampling and the foreground-service importer.
List<ImportRecord> fitMonitoringImportRecords(FitMonitoringSummary monitoring) {
  final records = <ImportRecord>[];
  final rhrTime = monitoring.restingHeartRateTime;
  final rhrBpm = monitoring.restingHeartRateBpm;
  if (rhrTime != null && rhrBpm != null) {
    records.add(
      RestingHeartRateImportRecord(
        clientRecordId:
            'garmin_fit_resting_hr_${rhrTime.millisecondsSinceEpoch}',
        time: rhrTime,
        zoneOffset: null,
        beatsPerMinute: rhrBpm,
      ),
    );
  }
  final bmrTime = monitoring.bmrTime;
  final bmr = monitoring.bmrKcalPerDay;
  if (bmrTime != null && bmr != null) {
    records.add(
      BasalMetabolicRateImportRecord(
        clientRecordId: 'garmin_fit_bmr_${bmrTime.millisecondsSinceEpoch}',
        time: bmrTime,
        zoneOffset: null,
        kilocaloriesPerDay: bmr,
      ),
    );
  }
  return records;
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
