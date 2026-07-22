import 'dart:convert';

import 'package:flutter/foundation.dart'; // DIAGNOSTIC: debugPrint to logcat (also re-exports Uint8List)

import '../../../../domain/model/activity_models.dart';
import '../../../../domain/model/ble_sensor_models.dart';
import 'route_file_parser.dart';

/// A Garmin sleep stage, from the FIT `sleep_level` enum (message 275, field 0):
/// see docs/reference/garmin-fit-files.md.
enum FitSleepLevel { unmeasurable, awake, light, deep, rem }

/// One stage span within a sleep session: `[start, end)` spent at [level].
class FitSleepStage {
  const FitSleepStage({
    required this.start,
    required this.end,
    required this.level,
  });

  final DateTime start;
  final DateTime end;
  final FitSleepLevel level;
}

/// A decoded Garmin sleep FIT file (file type 49): the night's bounds and its
/// stage timeline. The bounds come from the `event`/74 (sleep) start/stop pair;
/// each `sleep_level` message opens a stage that runs to the next one.
class FitSleepSession {
  const FitSleepSession({
    required this.start,
    required this.end,
    required this.stages,
    this.overallScore,
    this.awakeningsCount,
  });

  final DateTime start;
  final DateTime end;
  final List<FitSleepStage> stages;

  /// The watch's own sleep score, 0..100, as shown on the wrist.
  ///
  /// Deliberately kept alongside [stages] rather than folded into them: this is
  /// Garmin's verdict on the night, while the stages are transitions we
  /// interpret ourselves. Where the two disagree, having both is what makes the
  /// disagreement visible instead of silently picking one.
  final int? overallScore;

  /// How many times the watch counted the sleeper waking.
  final int? awakeningsCount;
}

/// A decoded Garmin HRV nightly reading (file type 68):
/// `hrv_status_summary.last_night_average` as an RMSSD in milliseconds.
class FitHrvReading {
  const FitHrvReading({required this.time, required this.rmssdMillis});

  final DateTime time;
  final double rmssdMillis;
}

/// A cumulative monitoring counter reading: a `[value]` for `[activityType]`
/// at `[time]`. Cumulative within a wear-session and per activity type, so a
/// per-file total is a sum of per-type within-file deltas (see the mapper).
class FitMonitoringPoint {
  const FitMonitoringPoint({
    required this.time,
    required this.activityType,
    required this.value,
  });

  final DateTime time;

  /// FIT `activity_type` enum (walking 6, running 1, generic 0, …), or -1 when
  /// the message did not carry one.
  final int activityType;
  final int value;
}

/// Everything a monitoring file (type 32) carried. The one-per-file summaries
/// (resting HR, BMR) plus the high-frequency series (per-minute HR, breathing,
/// and the cumulative step/distance/calorie counters). Aggregation into Health
/// Connect records happens in the mapper.
class FitMonitoringSummary {
  const FitMonitoringSummary({
    this.restingHeartRateTime,
    this.restingHeartRateBpm,
    this.bmrTime,
    this.bmrKcalPerDay,
    this.heartRateSamples = const [],
    this.respiration = const [],
    this.stepPoints = const [],
    this.distancePoints = const [],
    this.caloriePoints = const [],
    this.moderateMinutes = const [],
    this.vigorousMinutes = const [],
    this.stress = const [],
    this.bodyEnergy = const [],
  });

  final DateTime? restingHeartRateTime;
  final int? restingHeartRateBpm;
  final DateTime? bmrTime;
  final double? bmrKcalPerDay;

  /// Per-minute heart-rate samples `(time, bpm)`.
  final List<(DateTime, int)> heartRateSamples;

  /// Breathing-rate readings `(time, breathsPerMinute)`.
  final List<(DateTime, double)> respiration;

  /// Cumulative step (walk/run), distance (m) and active-calorie counters.
  final List<FitMonitoringPoint> stepPoints;
  final List<FitMonitoringPoint> distancePoints;
  final List<FitMonitoringPoint> caloriePoints;

  /// Running daily totals of Garmin's intensity minutes `(time, minutes)`.
  /// Cumulative like the step counter, not per-message increments.
  final List<(DateTime, int)> moderateMinutes;
  final List<(DateTime, int)> vigorousMinutes;

  /// Garmin stress score `(time, 0..100)`. Health Connect has no type for this,
  /// so it is kept in the app's own database rather than exported.
  final List<(DateTime, int)> stress;

  /// Garmin Body Battery `(time, 0..100)`. Same story — no Health Connect type.
  ///
  /// Note this is the WATCH's measure, distinct from the app's own computed
  /// Body Energy timeline; they are two independent estimates of a similar idea
  /// and must not be conflated.
  final List<(DateTime, int)> bodyEnergy;

  bool get isEmpty =>
      restingHeartRateBpm == null &&
      bmrKcalPerDay == null &&
      heartRateSamples.isEmpty &&
      respiration.isEmpty &&
      stepPoints.isEmpty &&
      distancePoints.isEmpty &&
      caloriePoints.isEmpty &&
      moderateMinutes.isEmpty &&
      vigorousMinutes.isEmpty &&
      stress.isEmpty &&
      bodyEnergy.isEmpty;
}

/// The fitness metrics a metrics file (Garmin type 44) carried.
///
/// Each is a snapshot the watch recomputes rather than a series, so at most one
/// of each survives a file — the last seen. Only VO2 max has a Health Connect
/// type; the rest are Garmin's own estimates and stay in the app's database,
/// the same split stress and Body Battery already follow.
class FitMetricsSummary {
  const FitMetricsSummary({
    this.time,
    this.vo2Max,
    this.recoveryTimeMinutes,
    this.trainingReadiness,
    this.trainingLoadAcute,
    this.trainingLoadChronic,
  });

  /// When the watch computed these. Null when no message carried a timestamp,
  /// which makes the whole snapshot unplaceable and therefore unusable.
  final DateTime? time;

  /// mL/kg/min.
  final double? vo2Max;

  /// How long the watch thinks recovery still needs, in minutes.
  final int? recoveryTimeMinutes;

  /// 0..100.
  final int? trainingReadiness;

  final int? trainingLoadAcute;
  final int? trainingLoadChronic;

  bool get isEmpty =>
      vo2Max == null &&
      recoveryTimeMinutes == null &&
      trainingReadiness == null &&
      trainingLoadAcute == null &&
      trainingLoadChronic == null;
}

/// The watch's own summary of a night (`daily_sleep`), computed on the wrist.
///
/// Entirely independent of [FitSleepSession]: that is built from stage
/// transitions this app interprets, while these are the numbers the watch shows
/// its wearer. Keeping both is what makes a disagreement between them visible.
class FitDailySleep {
  const FitDailySleep({
    this.endTime,
    this.score,
    this.awakeDuration,
    this.pressure,
  });

  /// When the night ended, per the watch.
  final DateTime? endTime;

  /// 0..100.
  final int? score;

  /// How long the watch counted the sleeper as awake during the night.
  final Duration? awakeDuration;

  /// Garmin's "sleep pressure" figure. Kept raw — its scale is undocumented and
  /// guessing at units would be worse than passing the number through.
  final int? pressure;

  bool get isEmpty =>
      score == null && awakeDuration == null && pressure == null;
}

/// Sleep Coach (`sleep_demand`): how much sleep the watch thinks is normally
/// needed, and how much last night's strain called for.
class FitSleepDemand {
  const FitSleepDemand({this.time, this.normal, this.demand});

  final DateTime? time;

  /// The usual nightly need.
  final Duration? normal;

  /// What this particular night demanded — higher after a hard day.
  final Duration? demand;

  bool get isEmpty => normal == null && demand == null;
}

/// One Health Snapshot recording: the two-minute on-demand measurement the
/// watch takes when the wearer asks for it.
///
/// Separate from the monitoring series even though three of the four metrics
/// overlap: these are a deliberate spot measurement at rest, sampled far more
/// densely, and averaging them into the all-day series would blur both.
class FitHealthSnapshot {
  const FitHealthSnapshot({
    this.spo2 = const [],
    this.respiration = const [],
    this.stress = const [],
    this.bodyEnergy = const [],
  });

  /// Blood oxygen `(time, percent)` — the only Pulse Ox this watch has been
  /// seen to write anywhere.
  final List<(DateTime, int)> spo2;
  final List<(DateTime, double)> respiration;
  final List<(DateTime, int)> stress;
  final List<(DateTime, int)> bodyEnergy;

  bool get isEmpty =>
      spo2.isEmpty &&
      respiration.isEmpty &&
      stress.isEmpty &&
      bodyEnergy.isEmpty;
}

/// A daytime nap the watch recorded, bounded by its own start/end fields rather
/// than the `event`/74 pair that bounds a night.
class FitNap {
  const FitNap({required this.start, required this.end});

  final DateTime start;
  final DateTime end;
}

/// The wellness data a FIT file carried, from one decode pass. Each Garmin file
/// is a single type, so at most one of these is populated (activities have none).
class FitWellness {
  const FitWellness({
    this.fileType,
    this.sleep,
    this.hrv,
    this.monitoring,
    this.metrics,
    this.naps = const [],
    this.dailySleep,
    this.sleepDemand,
    this.healthSnapshot,
  });

  /// `file_id.type` — lets the caller tell a non-activity file with no mappable
  /// data (skip it) from an activity file (parse it as an exercise).
  final int? fileType;
  final FitSleepSession? sleep;
  final FitHrvReading? hrv;
  final FitMonitoringSummary? monitoring;
  final FitMetricsSummary? metrics;

  /// Daytime naps. A list, not a single value: one sleep file can hold several.
  final List<FitNap> naps;

  /// The watch's own nightly summary and Sleep Coach figures. These arrive in
  /// the METRICS file on a vívoactive 5, not the sleep file.
  final FitDailySleep? dailySleep;
  final FitSleepDemand? sleepDemand;

  /// From a Health Snapshot file (type 70).
  final FitHealthSnapshot? healthSnapshot;

  bool get isEmpty =>
      sleep == null &&
      hrv == null &&
      monitoring == null &&
      metrics == null &&
      naps.isEmpty &&
      dailySleep == null &&
      sleepDemand == null &&
      healthSnapshot == null;

  /// True for `activity` (4), `workout` (5) and `course` (6) — the types the
  /// exercise/route importer handles. Everything else is wellness data.
  bool get isActivityType =>
      fileType == 4 || fileType == 5 || fileType == 6;
}

/// Hand-port of the Kotlin `FitRouteParser` (Garmin FIT decoder). Ported byte
/// for byte in pure Dart rather than delegating to a package, because the unit
/// tests exercise hand-crafted FIT byte streams whose exact framing must be
/// honoured. Pure Dart, no plugins.
class FitRouteParser {
  const FitRouteParser._();

  static RouteFileImport parse(Uint8List fitBytes, {String? fileName}) {
    // DIAGNOSTIC: log every file that reaches the decoder before it can throw, so
    // a header/structure failure is still attributable to a filename in logcat.
    if (kDebugMode) {
      debugPrint(
        '[FIT] decode start file=${fileName ?? "?"} bytes=${fitBytes.length}',
      );
    }
    final result = _FitDecoder(fitBytes).decode();
    final samples = result.samples.resolve(
      isCycling: _fitSportIsCycling(result.summary.sport),
    );
    final sorted = [...result.points]..sort((a, b) => a.time.compareTo(b.time));
    final seen = <int>{};
    final routePoints = <ExerciseRoutePoint>[];
    for (final point in sorted) {
      if (seen.add(point.time.microsecondsSinceEpoch)) routePoints.add(point);
    }
    // DIAGNOSTIC: the classification that decides pass/fail. fileType (activity vs
    // course/workout vs monitoring/sleep/etc.), whether a session start_time was
    // found, and how many timestamped route points survived — the three inputs the
    // reject-at-line-46 decision reads.
    if (kDebugMode) {
      debugPrint(
        '[FIT] decoded file=${fileName ?? "?"} '
        'fileType=${result.summary.fileType} sport=${result.summary.sport} '
        'subSport=${result.summary.subSport} start=${result.summary.startTime} '
        'end=${result.summary.endTime} routePoints=${routePoints.length}',
      );
    }
    switch (result.summary.fileType) {
      case _fitFileTypeCourse:
        // A course is a planned route: it has no recorded series to carry.
        return _parseCourse(fileName, routePoints, result.summary);
      case _fitFileTypeWorkout:
        return _parseWorkout(fileName, result.summary);
      default:
        return _parseActivity(fileName, routePoints, result.summary)
            .copyWith(bleSamples: samples);
    }
  }

  /// Decodes the **wellness** data a FIT file carries (sleep, HRV, …) in one
  /// pass. Wellness files have no activity session or route, so [parse] rejects
  /// them — this is their path. Returns an empty [FitWellness] for activity,
  /// course and workout files. Field layout: docs/reference/garmin-fit-files.md.
  static FitWellness parseWellness(Uint8List fitBytes, {String? fileName}) {
    final result = _FitDecoder(fitBytes).decode();
    return FitWellness(
      fileType: result.summary.fileType,
      sleep: result.sleep.toSession(),
      hrv: result.hrv.toReading(),
      monitoring: result.monitoring.toSummary(),
      metrics: result.metrics.toSummary(),
      naps: result.sleep.naps,
      dailySleep: result.metrics.toDailySleep(),
      sleepDemand: result.metrics.toSleepDemand(),
      healthSnapshot: result.metrics.toHealthSnapshot(),
    );
  }

  /// The sleep session in [fitBytes], or null if it carries none.
  static FitSleepSession? parseSleepSession(
    Uint8List fitBytes, {
    String? fileName,
  }) =>
      parseWellness(fitBytes, fileName: fileName).sleep;

  static RouteFileImport _parseActivity(
    String? fileName,
    List<ExerciseRoutePoint> routePoints,
    _FitActivitySummary summary,
  ) {
    final startTime = summary.startTime ??
        (routePoints.isNotEmpty ? routePoints.first.time : null);
    if (startTime == null) {
      throw const RouteImportException(
        'FIT file does not contain an activity session or timestamped activity '
        'records.',
      );
    }
    final candidateEnd =
        summary.endTime ?? (routePoints.isNotEmpty ? routePoints.last.time : null);
    final endTime = (candidateEnd != null && startTime.isBefore(candidateEnd))
        ? candidateEnd
        : startTime.add(const Duration(seconds: 1));
    final metadata = RouteFileMetadata(
      name: summary.name,
      description: null,
      type: _fitSportName(summary.sport, summary.subSport),
    );

    if (routePoints.length >= minRoutePoints) {
      return buildRouteImport(
        fileName: fileName,
        points: routePoints,
        metadata: metadata,
      ).copyWith(
        distanceMeters: summary.distanceMeters ?? routeDistanceMeters(routePoints),
        elevationGainedMeters:
            summary.elevationGainedMeters ?? routeElevationGainMeters(routePoints),
        activeCaloriesKcal: summary.activeCaloriesKcal,
        totalCaloriesKcal: summary.totalCaloriesKcal,
        startTime: startTime,
        endTime: endTime,
        durationSeconds: summary.durationSeconds,
        originalPointCount: routePoints.length,
      );
    }

    return RouteFileImport(
      fileName: fileName,
      points: const [],
      distanceMeters: summary.distanceMeters ?? 0.0,
      elevationGainedMeters: summary.elevationGainedMeters ?? 0.0,
      activeCaloriesKcal: summary.activeCaloriesKcal,
      totalCaloriesKcal: summary.totalCaloriesKcal,
      startTime: startTime,
      endTime: endTime,
      durationSeconds: summary.durationSeconds,
      name: summary.name,
      description: null,
      type: _fitSportName(summary.sport, summary.subSport),
      originalPointCount: routePoints.length,
    );
  }

  static RouteFileImport _parseCourse(
    String? fileName,
    List<ExerciseRoutePoint> routePoints,
    _FitActivitySummary summary,
  ) {
    final metadata = RouteFileMetadata(
      name: summary.name,
      description: null,
      type: _fitSportName(summary.sport, summary.subSport),
    );
    if (routePoints.length >= minRoutePoints) {
      return buildRouteImport(
        fileName: fileName,
        points: routePoints,
        metadata: metadata,
        hasRecordedTimestamps: false,
        hasImportedTimeRange: false,
      ).copyWith(
        distanceMeters: summary.distanceMeters ?? routeDistanceMeters(routePoints),
        elevationGainedMeters:
            summary.elevationGainedMeters ?? routeElevationGainMeters(routePoints),
        durationSeconds: summary.durationSeconds,
      );
    }

    final startTime = summary.startTime ??
        (routePoints.isNotEmpty ? routePoints.first.time : _syntheticFitStartTime);
    final DateTime endTime;
    if (summary.endTime != null && startTime.isBefore(summary.endTime!)) {
      endTime = summary.endTime!;
    } else if (routePoints.isNotEmpty &&
        startTime.isBefore(routePoints.last.time)) {
      endTime = routePoints.last.time;
    } else {
      final seconds = summary.durationSeconds == null
          ? 1
          : (summary.durationSeconds! < 1 ? 1 : summary.durationSeconds!);
      endTime = startTime.add(Duration(seconds: seconds));
    }

    return RouteFileImport(
      fileName: fileName,
      points: const [],
      distanceMeters: summary.distanceMeters ?? 0.0,
      elevationGainedMeters: summary.elevationGainedMeters ?? 0.0,
      activeCaloriesKcal: summary.activeCaloriesKcal,
      totalCaloriesKcal: summary.totalCaloriesKcal,
      startTime: startTime,
      endTime: endTime,
      durationSeconds: summary.durationSeconds,
      name: metadata.name,
      description: metadata.description,
      type: metadata.type,
      hasRecordedTimestamps: false,
      hasImportedTimeRange: false,
      originalPointCount: routePoints.length,
    );
  }

  static RouteFileImport _parseWorkout(
    String? fileName,
    _FitActivitySummary summary,
  ) {
    final durationSeconds = summary.durationSeconds == null
        ? null
        : (summary.durationSeconds! < 1 ? 1 : summary.durationSeconds!);
    return RouteFileImport(
      fileName: fileName,
      points: const [],
      distanceMeters: summary.distanceMeters ?? 0.0,
      elevationGainedMeters: summary.elevationGainedMeters ?? 0.0,
      activeCaloriesKcal: summary.activeCaloriesKcal,
      totalCaloriesKcal: summary.totalCaloriesKcal,
      startTime: _syntheticFitStartTime,
      endTime: _syntheticFitStartTime.add(
        Duration(seconds: durationSeconds ?? _defaultFitWorkoutDurationSeconds),
      ),
      durationSeconds: durationSeconds,
      name: summary.name,
      description: null,
      type: _fitSportName(summary.sport, summary.subSport),
      hasRecordedTimestamps: false,
      hasImportedTimeRange: false,
      originalPointCount: 0,
    );
  }
}

class _FitDecodeResult {
  const _FitDecodeResult(
    this.points,
    this.summary,
    this.samples,
    this.sleep,
    this.hrv,
    this.monitoring,
    this.metrics,
  );

  final List<ExerciseRoutePoint> points;
  final _FitActivitySummary summary;
  final _FitSamples samples;
  final _FitSleepRaw sleep;
  final _FitHrvRaw hrv;
  final _FitMonitoringRaw monitoring;
  final _FitMetricsRaw metrics;
}

/// The per-record series, before the sport is known.
///
/// FIT field 4 is just "cadence" -- it does not say whether those are pedal strokes
/// or footfalls, and Health Connect keeps the two in different record types. Only
/// the session's sport can decide, and the session is parsed after the records, so
/// the kind is resolved last.
class _FitSamples {
  const _FitSamples(this.heartRate, this.speed, this.cadence);

  const _FitSamples.empty()
      : heartRate = const [],
        speed = const [],
        cadence = const [];

  final List<BleHeartRateSample> heartRate;
  final List<BleSpeedSample> speed;
  final List<(DateTime, int)> cadence;

  _FitSamples merge(_FitSamples other) => _FitSamples(
        [...heartRate, ...other.heartRate],
        [...speed, ...other.speed],
        [...cadence, ...other.cadence],
      );

  BleRecordingSampleBuffer resolve({required bool isCycling}) =>
      BleRecordingSampleBuffer(
        heartRateSamples: heartRate,
        speedSamples: [
          for (final s in speed) s.copyWith(isRunning: !isCycling),
        ],
        cyclingCadenceSamples: [
          if (isCycling)
            for (final (time, rpm) in cadence)
              BleCyclingCadenceSample(time: time, rpm: rpm),
        ],
        stepsCadenceSamples: [
          if (!isCycling)
            for (final (time, rate) in cadence)
              // FIT reports running cadence as STRIDES per minute -- one leg. Health
              // Connect wants steps. A runner at 90 spm is taking 180 steps.
              BleStepsCadenceSample(time: time, stepsPerMinute: rate * 2),
        ],
      );
}

class _FitFileDecodeResult {
  const _FitFileDecodeResult(
    this.points,
    this.summary,
    this.samples,
    this.sleep,
    this.hrv,
    this.monitoring,
    this.metrics,
    this.nextOffset,
  );

  final List<ExerciseRoutePoint> points;
  final _FitActivitySummary summary;
  final _FitSamples samples;
  final _FitSleepRaw sleep;
  final _FitHrvRaw hrv;
  final _FitMonitoringRaw monitoring;
  final _FitMetricsRaw metrics;
  final int nextOffset;
}

/// The raw HRV reading a file carried (`hrv_status_summary.last_night_average`).
/// At most one is kept — the last seen — since a status file holds one summary.
class _FitHrvRaw {
  const _FitHrvRaw({this.time, this.rmssdMillis});

  final DateTime? time;
  final double? rmssdMillis;

  _FitHrvRaw merge(_FitHrvRaw other) => _FitHrvRaw(
        time: other.time ?? time,
        rmssdMillis: other.rmssdMillis ?? rmssdMillis,
      );

  FitHrvReading? toReading() => (time != null && rmssdMillis != null)
      ? FitHrvReading(time: time!, rmssdMillis: rmssdMillis!)
      : null;
}

/// The one-per-file monitoring summaries (resting HR, BMR) collected from a
/// type-32 file. The last seen of each wins.
class _FitMonitoringRaw {
  const _FitMonitoringRaw({
    this.restingHrTime,
    this.restingHrBpm,
    this.bmrTime,
    this.bmrKcalPerDay,
    this.heartRate = const [],
    this.respiration = const [],
    this.stress = const [],
    this.bodyEnergy = const [],
    this.steps = const [],
    this.distance = const [],
    this.calories = const [],
    this.moderateMinutes = const [],
    this.vigorousMinutes = const [],
  });

  final DateTime? restingHrTime;
  final int? restingHrBpm;
  final DateTime? bmrTime;
  final double? bmrKcalPerDay;
  final List<(DateTime, int)> heartRate;
  final List<(DateTime, double)> respiration;
  final List<(DateTime, int)> stress;
  final List<(DateTime, int)> bodyEnergy;
  final List<FitMonitoringPoint> steps;
  final List<FitMonitoringPoint> distance;
  final List<FitMonitoringPoint> calories;
  final List<(DateTime, int)> moderateMinutes;
  final List<(DateTime, int)> vigorousMinutes;

  _FitMonitoringRaw merge(_FitMonitoringRaw other) => _FitMonitoringRaw(
        restingHrTime: other.restingHrTime ?? restingHrTime,
        restingHrBpm: other.restingHrBpm ?? restingHrBpm,
        bmrTime: other.bmrTime ?? bmrTime,
        bmrKcalPerDay: other.bmrKcalPerDay ?? bmrKcalPerDay,
        heartRate: [...heartRate, ...other.heartRate],
        respiration: [...respiration, ...other.respiration],
        stress: [...stress, ...other.stress],
        bodyEnergy: [...bodyEnergy, ...other.bodyEnergy],
        steps: [...steps, ...other.steps],
        distance: [...distance, ...other.distance],
        calories: [...calories, ...other.calories],
        moderateMinutes: [...moderateMinutes, ...other.moderateMinutes],
        vigorousMinutes: [...vigorousMinutes, ...other.vigorousMinutes],
      );

  FitMonitoringSummary? toSummary() {
    final summary = FitMonitoringSummary(
      restingHeartRateTime: restingHrTime,
      restingHeartRateBpm: restingHrBpm,
      bmrTime: bmrTime,
      bmrKcalPerDay: bmrKcalPerDay,
      heartRateSamples: heartRate,
      respiration: respiration,
      stress: stress,
      bodyEnergy: bodyEnergy,
      stepPoints: steps,
      distancePoints: distance,
      caloriePoints: calories,
      moderateMinutes: moderateMinutes,
      vigorousMinutes: vigorousMinutes,
    );
    return summary.isEmpty ? null : summary;
  }
}

/// The raw sleep messages a single FIT file carried: the `event`/74 session
/// bounds and the `sleep_level` transitions. Turned into a [FitSleepSession]
/// once the whole file (or chain of files) is decoded.
class _FitSleepRaw {
  const _FitSleepRaw({
    this.start,
    this.stop,
    this.levels = const [],
    this.overallScore,
    this.awakeningsCount,
    this.naps = const [],
  });

  final DateTime? start;
  final DateTime? stop;

  /// Each entry is `(transitionTime, sleepLevelEnumValue)`, in file order.
  final List<(DateTime, int)> levels;

  final int? overallScore;
  final int? awakeningsCount;
  final List<FitNap> naps;

  _FitSleepRaw merge(_FitSleepRaw other) => _FitSleepRaw(
        start: start ?? other.start,
        stop: stop ?? other.stop,
        levels: [...levels, ...other.levels],
        overallScore: overallScore ?? other.overallScore,
        awakeningsCount: awakeningsCount ?? other.awakeningsCount,
        naps: [...naps, ...other.naps],
      );

  FitSleepSession? toSession() {
    if (levels.isEmpty) return null;
    final sorted = [...levels]..sort((a, b) => a.$1.compareTo(b.$1));
    final sessionStart = start ?? sorted.first.$1;
    // Sleep never ends before it starts; a file that says so is unusable.
    final sessionEnd = (stop != null && stop!.isAfter(sessionStart))
        ? stop!
        : sorted.last.$1;
    if (!sessionStart.isBefore(sessionEnd)) return null;
    final stages = <FitSleepStage>[];
    for (var i = 0; i < sorted.length; i++) {
      final (transition, rawLevel) = sorted[i];
      final level = _fitSleepLevelFromRaw(rawLevel);
      if (level == null) continue;
      // A stage runs from its transition to the next one — the last to session
      // end. Clamp into the session so a stray pre-start transition can't widen it.
      final stageStart =
          transition.isBefore(sessionStart) ? sessionStart : transition;
      final stageEnd = i + 1 < sorted.length ? sorted[i + 1].$1 : sessionEnd;
      if (!stageStart.isBefore(stageEnd)) continue;
      stages.add(FitSleepStage(start: stageStart, end: stageEnd, level: level));
    }
    if (stages.isEmpty) return null;
    // DIAGNOSTIC: the raw transitions and what they add up to. A real vívoactive
    // 5 reported 3 min awake where this produced 59, so the question is whether
    // the file says something different from the watch's own screen or whether
    // these stages are being derived wrongly — and only the raw series answers
    // it. Still unresolved, which is why this is still here.
    //
    // Debug builds only, and the whole block rather than each line: this prints
    // a person's night, transition by transition, and `debugPrint` is NOT
    // stripped from a release build. Guarding the block also keeps the totals
    // from being computed for a log nobody will read.
    if (kDebugMode) {
      final totals = <FitSleepLevel, int>{};
      for (final stage in stages) {
        totals[stage.level] = (totals[stage.level] ?? 0) +
            stage.end.difference(stage.start).inMinutes;
      }
      final covered = totals.values.fold(0, (a, b) => a + b);
      debugPrint('[FIT-SLEEP] session ${sessionStart.toIso8601String()} → '
          '${sessionEnd.toIso8601String()} '
          '(${sessionEnd.difference(sessionStart).inMinutes}m) '
          'transitions=${sorted.length} stages=${stages.length} '
          'covered=${covered}m');
      debugPrint('[FIT-SLEEP] totals: '
          '${totals.entries.map((e) => "${e.key.name}=${e.value}m").join(" ")}');
      for (final (transition, rawLevel) in sorted) {
        debugPrint('[FIT-SLEEP]   ${transition.toIso8601String()} raw=$rawLevel '
            '(${_fitSleepLevelFromRaw(rawLevel)?.name ?? "UNKNOWN"})');
      }
      // The watch's own verdict on the same night, for comparison against what
      // the stages above add up to.
      debugPrint('[FIT-SLEEP] watch says: score=${overallScore ?? "-"} '
          'awakenings=${awakeningsCount ?? "-"}');
    }
    return FitSleepSession(
      start: sessionStart,
      end: sessionEnd,
      stages: stages,
      overallScore: overallScore,
      awakeningsCount: awakeningsCount,
    );
  }
}

/// The metrics-file snapshots a decode pass collected. Last seen wins for each,
/// independently: one file can carry a VO2 max message and a training-load
/// message with nothing in common but the file they share.
class _FitMetricsRaw {
  const _FitMetricsRaw({
    this.time,
    this.vo2Max,
    this.recoveryTimeMinutes,
    this.trainingReadiness,
    this.trainingLoadAcute,
    this.trainingLoadChronic,
    this.dailySleepEndTime,
    this.dailySleepScore,
    this.dailySleepAwakeSeconds,
    this.dailySleepPressure,
    this.sleepDemandTime,
    this.sleepDemandNormalMinutes,
    this.sleepDemandMinutes,
    this.hsaSpo2 = const [],
    this.hsaRespiration = const [],
    this.hsaStress = const [],
    this.hsaBodyEnergy = const [],
  });

  final DateTime? time;
  final double? vo2Max;
  final int? recoveryTimeMinutes;
  final int? trainingReadiness;
  final int? trainingLoadAcute;
  final int? trainingLoadChronic;

  // Sleep summaries that share the metrics file rather than the sleep file.
  final DateTime? dailySleepEndTime;
  final int? dailySleepScore;
  final int? dailySleepAwakeSeconds;
  final int? dailySleepPressure;
  final DateTime? sleepDemandTime;
  final int? sleepDemandNormalMinutes;
  final int? sleepDemandMinutes;

  // Health Snapshot samples. They ride here rather than in their own result
  // slot because the decode result is positional and this is already the
  // "everything that is not a session, a night or a monitoring series" carrier.
  final List<(DateTime, int)> hsaSpo2;
  final List<(DateTime, double)> hsaRespiration;
  final List<(DateTime, int)> hsaStress;
  final List<(DateTime, int)> hsaBodyEnergy;

  _FitMetricsRaw merge(_FitMetricsRaw other) => _FitMetricsRaw(
        time: other.time ?? time,
        vo2Max: other.vo2Max ?? vo2Max,
        recoveryTimeMinutes: other.recoveryTimeMinutes ?? recoveryTimeMinutes,
        trainingReadiness: other.trainingReadiness ?? trainingReadiness,
        trainingLoadAcute: other.trainingLoadAcute ?? trainingLoadAcute,
        trainingLoadChronic: other.trainingLoadChronic ?? trainingLoadChronic,
        dailySleepEndTime: other.dailySleepEndTime ?? dailySleepEndTime,
        dailySleepScore: other.dailySleepScore ?? dailySleepScore,
        dailySleepAwakeSeconds:
            other.dailySleepAwakeSeconds ?? dailySleepAwakeSeconds,
        dailySleepPressure: other.dailySleepPressure ?? dailySleepPressure,
        sleepDemandTime: other.sleepDemandTime ?? sleepDemandTime,
        sleepDemandNormalMinutes:
            other.sleepDemandNormalMinutes ?? sleepDemandNormalMinutes,
        sleepDemandMinutes: other.sleepDemandMinutes ?? sleepDemandMinutes,
        hsaSpo2: [...hsaSpo2, ...other.hsaSpo2],
        hsaRespiration: [...hsaRespiration, ...other.hsaRespiration],
        hsaStress: [...hsaStress, ...other.hsaStress],
        hsaBodyEnergy: [...hsaBodyEnergy, ...other.hsaBodyEnergy],
      );

  FitDailySleep? toDailySleep() {
    final awake = dailySleepAwakeSeconds;
    final summary = FitDailySleep(
      endTime: dailySleepEndTime,
      score: dailySleepScore,
      awakeDuration: awake == null ? null : Duration(seconds: awake),
      pressure: dailySleepPressure,
    );
    return summary.isEmpty ? null : summary;
  }

  FitHealthSnapshot? toHealthSnapshot() {
    final snapshot = FitHealthSnapshot(
      spo2: hsaSpo2,
      respiration: hsaRespiration,
      stress: hsaStress,
      bodyEnergy: hsaBodyEnergy,
    );
    return snapshot.isEmpty ? null : snapshot;
  }

  FitSleepDemand? toSleepDemand() {
    final normal = sleepDemandNormalMinutes;
    final demand = sleepDemandMinutes;
    final summary = FitSleepDemand(
      time: sleepDemandTime,
      normal: normal == null ? null : Duration(minutes: normal),
      demand: demand == null ? null : Duration(minutes: demand),
    );
    return summary.isEmpty ? null : summary;
  }

  FitMetricsSummary? toSummary() {
    final summary = FitMetricsSummary(
      time: time,
      vo2Max: vo2Max,
      recoveryTimeMinutes: recoveryTimeMinutes,
      trainingReadiness: trainingReadiness,
      trainingLoadAcute: trainingLoadAcute,
      trainingLoadChronic: trainingLoadChronic,
    );
    return summary.isEmpty ? null : summary;
  }
}

FitSleepLevel? _fitSleepLevelFromRaw(int raw) => switch (raw) {
      0 => FitSleepLevel.unmeasurable,
      1 => FitSleepLevel.awake,
      2 => FitSleepLevel.light,
      3 => FitSleepLevel.deep,
      4 => FitSleepLevel.rem,
      _ => null,
    };

class _FitActivitySummary {
  const _FitActivitySummary({
    this.fileType,
    this.name,
    this.startTime,
    this.endTime,
    this.durationSeconds,
    this.distanceMeters,
    this.elevationGainedMeters,
    this.activeCaloriesKcal,
    this.totalCaloriesKcal,
    this.sport,
    this.subSport,
  });

  final int? fileType;
  final String? name;
  final DateTime? startTime;
  final DateTime? endTime;
  final int? durationSeconds;
  final double? distanceMeters;
  final double? elevationGainedMeters;
  final double? activeCaloriesKcal;
  final double? totalCaloriesKcal;
  final int? sport;
  final int? subSport;

  _FitActivitySummary merge(_FitActivitySummary other) => _FitActivitySummary(
        fileType: fileType ?? other.fileType,
        name: name ?? other.name,
        startTime: _earliest(startTime, other.startTime),
        endTime: _latest(endTime, other.endTime),
        durationSeconds: _sumInt(durationSeconds, other.durationSeconds),
        distanceMeters: _sumDouble(distanceMeters, other.distanceMeters),
        elevationGainedMeters:
            _sumDouble(elevationGainedMeters, other.elevationGainedMeters),
        activeCaloriesKcal:
            _sumDouble(activeCaloriesKcal, other.activeCaloriesKcal),
        totalCaloriesKcal: _sumDouble(totalCaloriesKcal, other.totalCaloriesKcal),
        sport: sport ?? other.sport,
        subSport: subSport ?? other.subSport,
      );

  _FitActivitySummary withFallback(_FitActivitySummary other) =>
      _FitActivitySummary(
        fileType: fileType ?? other.fileType,
        name: name ?? other.name,
        startTime: startTime ?? other.startTime,
        endTime: endTime ?? other.endTime,
        durationSeconds: durationSeconds ?? other.durationSeconds,
        distanceMeters: distanceMeters ?? other.distanceMeters,
        elevationGainedMeters:
            elevationGainedMeters ?? other.elevationGainedMeters,
        activeCaloriesKcal: activeCaloriesKcal ?? other.activeCaloriesKcal,
        totalCaloriesKcal: totalCaloriesKcal ?? other.totalCaloriesKcal,
        sport: sport ?? other.sport,
        subSport: subSport ?? other.subSport,
      );
}

class _FitMessageDefinition {
  const _FitMessageDefinition({
    required this.globalMessageNumber,
    required this.littleEndian,
    required this.fieldList,
    required this.developerFields,
  });

  final int globalMessageNumber;
  final bool littleEndian;
  final List<_FitFieldDefinition> fieldList;
  final List<int> developerFields;
}

class _FitFieldDefinition {
  const _FitFieldDefinition(this.number, this.size, this.baseType);

  final int number;
  final int size;
  final int baseType;
}

class _FitDecoder {
  _FitDecoder(this.fileBytes);

  final Uint8List fileBytes;

  _FitDecodeResult decode() {
    final points = <ExerciseRoutePoint>[];
    var summary = const _FitActivitySummary();
    var samples = const _FitSamples.empty();
    var sleep = const _FitSleepRaw();
    var hrv = const _FitHrvRaw();
    var monitoring = const _FitMonitoringRaw();
    var metrics = const _FitMetricsRaw();
    var offset = 0;
    var decodedAnyFile = false;

    while (offset < fileBytes.length) {
      if (!_isFitFileAt(fileBytes, offset)) {
        if (!decodedAnyFile) {
          throw const RouteImportException('FIT file header is invalid.');
        }
        break;
      }
      final result = _FitSingleFileDecoder(fileBytes, offset).decode();
      points.addAll(result.points);
      summary = summary.merge(result.summary);
      samples = samples.merge(result.samples);
      sleep = sleep.merge(result.sleep);
      hrv = hrv.merge(result.hrv);
      monitoring = monitoring.merge(result.monitoring);
      metrics = metrics.merge(result.metrics);
      decodedAnyFile = true;
      offset = result.nextOffset;
    }
    return _FitDecodeResult(
        points, summary, samples, sleep, hrv, monitoring, metrics);
  }
}

class _FitSingleFileDecoder {
  _FitSingleFileDecoder(this.fileBytes, this.startOffset);

  final Uint8List fileBytes;
  final int startOffset;

  final Map<int, _FitMessageDefinition> _definitions = {};
  final List<ExerciseRoutePoint> _points = [];
  int? _fileType;
  String? _metadataName;
  int? _sport;
  int? _subSport;
  int? _lastTimestampRaw;
  DateTime? _firstRecordTime;
  DateTime? _lastRecordTime;
  _FitActivitySummary _sessionSummary = const _FitActivitySummary();
  _FitActivitySummary _lapSummary = const _FitActivitySummary();
  int? _workoutDurationSeconds;
  int _courseRecordIndex = 0;

  // Sleep (file type 49). A sleep file carries no session or route, so these are
  // collected separately from the activity summary and only used by
  // `parseSleepSession`. See docs/reference/garmin-fit-files.md.
  DateTime? _sleepStart;
  DateTime? _sleepStop;
  final List<(DateTime, int)> _sleepLevels = [];
  int? _sleepOverallScore;
  int? _sleepAwakenings;
  final List<FitNap> _naps = [];

  // Health Snapshot (file type 70): dense sample arrays, one recording.
  final List<(DateTime, int)> _hsaSpo2 = [];
  final List<(DateTime, double)> _hsaRespiration = [];
  final List<(DateTime, int)> _hsaStress = [];
  final List<(DateTime, int)> _hsaBodyEnergy = [];

  // daily_sleep / sleep_demand, which share the metrics file.
  DateTime? _dailySleepEndTime;
  int? _dailySleepScore;
  int? _dailySleepAwakeSeconds;
  int? _dailySleepPressure;
  DateTime? _sleepDemandTime;
  int? _sleepDemandNormalMinutes;
  int? _sleepDemandMinutes;

  // Metrics (file type 44): four one-per-file snapshots, last seen wins.
  DateTime? _metricsTime;
  double? _vo2Max;
  int? _recoveryTimeMinutes;
  int? _trainingReadiness;
  int? _trainingLoadAcute;
  int? _trainingLoadChronic;

  // HRV (file type 68): the last `hrv_status_summary.last_night_average` seen.
  DateTime? _hrvTime;
  double? _hrvRmssdMillis;

  // Monitoring (file type 32): the last one-per-file summary values seen.
  DateTime? _restingHrTime;
  int? _restingHrBpm;
  DateTime? _bmrTime;
  double? _bmrKcalPerDay;

  // Monitoring high-frequency series, and the running full timestamp used to
  // reconstruct each message's `timestamp_16`.
  int? _monLastTimestampRaw;
  // The last-declared activity type, carried forward: the cumulative-counter
  // messages don't repeat it, they inherit the context set by an earlier message.
  int? _monCurrentActivityType;
  final List<(DateTime, int)> _monHeartRate = [];
  final List<(DateTime, double)> _respiration = [];
  final List<(DateTime, int)> _stress = [];
  final List<(DateTime, int)> _bodyEnergy = [];
  final List<FitMonitoringPoint> _monSteps = [];
  final List<FitMonitoringPoint> _monDistance = [];
  final List<FitMonitoringPoint> _monCalories = [];
  final List<(DateTime, int)> _monModerateMinutes = [];
  final List<(DateTime, int)> _monVigorousMinutes = [];

  _FitFileDecodeResult decode() {
    final headerSize = fileBytes[startOffset] & 0xFF;
    if (headerSize < _fitMinimumHeaderSize ||
        startOffset + headerSize > fileBytes.length) {
      throw const RouteImportException('FIT file header is invalid.');
    }
    final dataSize = _readUint32(
      fileBytes,
      startOffset + _fitHeaderDataSizeOffset,
      true,
    );
    final dataStart = startOffset + headerSize;
    final dataEnd = dataStart + dataSize;
    if (dataEnd > fileBytes.length) {
      throw const RouteImportException('FIT file data section is incomplete.');
    }
    final reader = _FitDataReader(fileBytes, dataStart, dataEnd);
    while (reader.hasRemaining()) {
      _readRecord(reader);
    }
    final next = dataEnd + _fitCrcSize;
    return _FitFileDecodeResult(
      _points,
      _fitSummary(),
      samples,
      _FitSleepRaw(
        start: _sleepStart,
        stop: _sleepStop,
        levels: _sleepLevels,
        overallScore: _sleepOverallScore,
        awakeningsCount: _sleepAwakenings,
        naps: _naps,
      ),
      _FitHrvRaw(time: _hrvTime, rmssdMillis: _hrvRmssdMillis),
      _FitMonitoringRaw(
        restingHrTime: _restingHrTime,
        restingHrBpm: _restingHrBpm,
        bmrTime: _bmrTime,
        bmrKcalPerDay: _bmrKcalPerDay,
        heartRate: _monHeartRate,
        respiration: _respiration,
        stress: _stress,
        bodyEnergy: _bodyEnergy,
        steps: _monSteps,
        distance: _monDistance,
        calories: _monCalories,
        moderateMinutes: _monModerateMinutes,
        vigorousMinutes: _monVigorousMinutes,
      ),
      _FitMetricsRaw(
        time: _metricsTime,
        vo2Max: _vo2Max,
        recoveryTimeMinutes: _recoveryTimeMinutes,
        trainingReadiness: _trainingReadiness,
        trainingLoadAcute: _trainingLoadAcute,
        trainingLoadChronic: _trainingLoadChronic,
        dailySleepEndTime: _dailySleepEndTime,
        dailySleepScore: _dailySleepScore,
        dailySleepAwakeSeconds: _dailySleepAwakeSeconds,
        dailySleepPressure: _dailySleepPressure,
        sleepDemandTime: _sleepDemandTime,
        sleepDemandNormalMinutes: _sleepDemandNormalMinutes,
        sleepDemandMinutes: _sleepDemandMinutes,
        hsaSpo2: _hsaSpo2,
        hsaRespiration: _hsaRespiration,
        hsaStress: _hsaStress,
        hsaBodyEnergy: _hsaBodyEnergy,
      ),
      next > fileBytes.length ? fileBytes.length : next,
    );
  }

  void _readRecord(_FitDataReader reader) {
    final header = reader.readUnsignedByte();
    if (header & _fitCompressedHeaderFlag != 0) {
      final localMessageType = (header >> _fitCompressedLocalMessageTypeShift) &
          _fitCompressedLocalMessageTypeMask;
      final timestamp = _compressedTimestamp(header & _fitCompressedTimestampMask);
      _readDataMessage(localMessageType, timestamp, reader);
      return;
    }
    final localMessageType = header & _fitNormalLocalMessageTypeMask;
    if (header & _fitDefinitionMessageFlag != 0) {
      _definitions[localMessageType] = _readDefinitionMessage(header, reader);
    } else {
      _readDataMessage(localMessageType, null, reader);
    }
  }

  _FitMessageDefinition _readDefinitionMessage(int header, _FitDataReader reader) {
    reader.skip(1);
    final architecture = reader.readUnsignedByte();
    final bool littleEndian;
    if (architecture == _fitArchitectureLittleEndian) {
      littleEndian = true;
    } else if (architecture == _fitArchitectureBigEndian) {
      littleEndian = false;
    } else {
      throw const RouteImportException('FIT message architecture is invalid.');
    }
    final globalMessageNumber = reader.readUnsignedShort(littleEndian);
    final fieldCount = reader.readUnsignedByte();
    final fields = <_FitFieldDefinition>[];
    for (var i = 0; i < fieldCount; i++) {
      fields.add(
        _FitFieldDefinition(
          reader.readUnsignedByte(),
          reader.readUnsignedByte(),
          reader.readUnsignedByte(),
        ),
      );
    }
    final developerFieldSizes = <int>[];
    if (header & _fitDeveloperDataFlag != 0) {
      final developerFieldCount = reader.readUnsignedByte();
      for (var i = 0; i < developerFieldCount; i++) {
        reader.skip(1);
        final size = reader.readUnsignedByte();
        reader.skip(1);
        developerFieldSizes.add(size);
      }
    }
    return _FitMessageDefinition(
      globalMessageNumber: globalMessageNumber,
      littleEndian: littleEndian,
      fieldList: fields,
      developerFields: developerFieldSizes,
    );
  }

  void _readDataMessage(
    int localMessageType,
    int? compressedTimestamp,
    _FitDataReader reader,
  ) {
    final definition = _definitions[localMessageType];
    if (definition == null) {
      throw const RouteImportException('FIT data message has no definition.');
    }
    final values = <int, int>{};
    final strings = <int, String>{};
    final arrays = <int, List<int>>{};
    final parsed = _fitParsedMessageNumbers.contains(definition.globalMessageNumber);
    final packsArrays =
        _fitArrayMessageNumbers.contains(definition.globalMessageNumber);
    for (final field in definition.fieldList) {
      final fieldBytes = reader.readBytes(field.size);
      if (field.number == _fitTimestampFieldNumber || parsed) {
        final longValue = _fitLong(fieldBytes, field, definition.littleEndian);
        if (longValue != null) values[field.number] = longValue;
        final stringValue = _fitString(fieldBytes, field);
        if (stringValue != null) strings[field.number] = stringValue;
        if (packsArrays) {
          arrays[field.number] =
              _fitLongArray(fieldBytes, field, definition.littleEndian);
        }
      }
    }
    for (final size in definition.developerFields) {
      reader.skip(size);
    }

    final explicitTimestamp = values[_fitTimestampFieldNumber];
    final messageTimestamp = explicitTimestamp ?? compressedTimestamp;
    if (messageTimestamp != null) _lastTimestampRaw = messageTimestamp;

    switch (definition.globalMessageNumber) {
      case _fitFileIdMessageNumber:
        _addFileId(values);
        break;
      case _fitCourseMessageNumber:
        _addCourseMetadata(values, strings);
        break;
      case _fitWorkoutMessageNumber:
        _addWorkoutMetadata(values, strings);
        break;
      case _fitWorkoutStepMessageNumber:
        _addWorkoutStep(values);
        break;
      case _fitRecordMessageNumber:
        if (_fileType == _fitFileTypeCourse) {
          _addCourseRecordPoint(values, messageTimestamp);
        } else {
          _rememberRecordTime(messageTimestamp);
          _addRecordPointRaw(values, messageTimestamp);
        }
        break;
      case _fitLapMessageNumber:
        _lapSummary = _lapSummary.merge(_toFitActivitySummary(values, messageTimestamp));
        break;
      case _fitSessionMessageNumber:
        _sessionSummary =
            _sessionSummary.merge(_toFitActivitySummary(values, messageTimestamp));
        final sessionSport = _generic(values[_fitSessionSportFieldNumber]);
        if (_sport == null && sessionSport != null) _sport = sessionSport;
        // Read HERE and not in _toFitActivitySummary, which serves the lap
        // message too — a lap's field 6 is end_position_long, and reading a
        // longitude as a sub-sport would name the activity at random.
        final sessionSubSport = _generic(values[_fitSessionSubSportFieldNumber]);
        if (_subSport == null && sessionSubSport != null) {
          _subSport = sessionSubSport;
        }
        break;
      case _fitEventMessageNumber:
        // Only the sleep event (Garmin-proprietary value 74) bounds a night;
        // every other event (timer, lap, …) that an activity file carries is
        // ignored here.
        if (values[_fitEventFieldNumber] == _fitSleepEventValue &&
            messageTimestamp != null) {
          final at = _fitDateTimeInstant(messageTimestamp);
          switch (values[_fitEventTypeFieldNumber]) {
            case _fitEventTypeStart:
              _sleepStart ??= at;
            case _fitEventTypeStop:
              _sleepStop = at;
          }
        }
        break;
      case _fitSleepLevelMessageNumber:
        final level = values[_fitSleepLevelFieldNumber];
        if (level != null && messageTimestamp != null) {
          _sleepLevels.add((_fitDateTimeInstant(messageTimestamp), level));
        }
        break;
      case _fitHrvStatusSummaryMessageNumber:
        final raw = values[_fitHrvLastNightAverageFieldNumber];
        if (raw != null && raw != _fitUint16Invalid && messageTimestamp != null) {
          _hrvTime = _fitDateTimeInstant(messageTimestamp);
          _hrvRmssdMillis = raw / _fitHrvRmssdScale;
        }
        break;
      case _fitMonitoringHrDataMessageNumber:
        final bpm = values[_fitRestingHeartRateFieldNumber];
        if (bpm != null && bpm != _fitUint8Invalid && bpm > 0) {
          _restingHrBpm = bpm;
          if (messageTimestamp != null) {
            _restingHrTime = _fitDateTimeInstant(messageTimestamp);
          }
        }
        break;
      case _fitMonitoringInfoMessageNumber:
        // monitoring_info carries a full timestamp that anchors the following
        // messages' timestamp_16 values.
        if (messageTimestamp != null) _monLastTimestampRaw = messageTimestamp;
        final rmr = values[_fitRestingMetabolicRateFieldNumber];
        if (rmr != null && rmr != _fitUint16Invalid && rmr > 0) {
          _bmrKcalPerDay = rmr.toDouble();
          if (messageTimestamp != null) {
            _bmrTime = _fitDateTimeInstant(messageTimestamp);
          }
        }
        break;
      case _fitMonitoringMessageNumber:
        _readMonitoring(values, messageTimestamp);
        break;
      case _fitStressLevelMessageNumber:
        // The stress message carries BOTH the stress score and Body Battery —
        // Body Battery has no message of its own. Its own timestamp field is
        // preferred over the record header's, as Gadgetbridge does.
        final stressTimeRaw =
            values[_fitStressLevelTimeFieldNumber] ?? messageTimestamp;
        if (stressTimeRaw != null) {
          final at = _fitDateTimeInstant(stressTimeRaw);
          final stress = values[_fitStressLevelValueFieldNumber];
          // Negative is Garmin's "not measurable" (asleep, moving, poor
          // contact), not a low score — dropped rather than clamped to 0.
          if (stress != null && stress >= 0 && stress <= 100) {
            _stress.add((at, stress));
          }
          final energy = values[_fitStressBodyEnergyFieldNumber];
          if (energy != null && energy >= 0 && energy <= 100) {
            _bodyEnergy.add((at, energy));
          }
        }
        break;
      case _fitSleepStatsMessageNumber:
        final score = values[_fitOverallSleepScoreFieldNumber];
        if (score != null && score != _fitUint8Invalid && score <= 100) {
          _sleepOverallScore = score;
        }
        final awakenings = values[_fitAwakeningsCountFieldNumber];
        if (awakenings != null && awakenings != _fitUint8Invalid) {
          _sleepAwakenings = awakenings;
        }
        break;
      case _fitNapMessageNumber:
        final napStart = values[_fitNapStartFieldNumber];
        final napEnd = values[_fitNapEndFieldNumber];
        if (napStart != null && napEnd != null && napEnd > napStart) {
          _naps.add(FitNap(
            start: _fitDateTimeInstant(napStart),
            end: _fitDateTimeInstant(napEnd),
          ));
        }
        break;
      case _fitHsaSpo2MessageNumber:
      case _fitHsaStressMessageNumber:
      case _fitHsaRespirationMessageNumber:
      case _fitHsaBodyBatteryMessageNumber:
        _readHsaSamples(
          definition.globalMessageNumber,
          values,
          arrays,
          messageTimestamp,
        );
        break;
      case _fitDailySleepMessageNumber:
        final dailyScore = values[_fitDailySleepScoreFieldNumber];
        if (dailyScore != null &&
            dailyScore != _fitUint8Invalid &&
            dailyScore <= 100) {
          _dailySleepScore = dailyScore;
        }
        final awake = values[_fitDailySleepAwakeDurationFieldNumber];
        if (awake != null && awake != _fitUint16Invalid) {
          _dailySleepAwakeSeconds = awake;
        }
        final endRaw = values[_fitDailySleepEndTimeFieldNumber];
        if (endRaw != null) _dailySleepEndTime = _fitDateTimeInstant(endRaw);
        final pressure = values[_fitDailySleepPressureFieldNumber];
        if (pressure != null && pressure != _fitSint16Invalid) {
          _dailySleepPressure = pressure;
        }
        break;
      case _fitSleepDemandMessageNumber:
        final normal = values[_fitSleepDemandNormalFieldNumber];
        if (normal != null && normal != _fitUint16Invalid) {
          _sleepDemandNormalMinutes = normal;
        }
        final demand = values[_fitSleepDemandDemandFieldNumber];
        if (demand != null && demand != _fitUint16Invalid) {
          _sleepDemandMinutes = demand;
        }
        if (messageTimestamp != null) {
          _sleepDemandTime = _fitDateTimeInstant(messageTimestamp);
        }
        break;
      case _fitMaxMetDataMessageNumber:
        final vo2 = values[_fitVo2MaxFieldNumber];
        if (vo2 != null && vo2 != _fitUint16Invalid && vo2 > 0) {
          _vo2Max = vo2 / _fitVo2MaxScale;
          if (messageTimestamp != null) {
            _metricsTime = _fitDateTimeInstant(messageTimestamp);
          }
        }
        break;
      case _fitTrainingReadinessMessageNumber:
        final readiness = values[_fitTrainingReadinessFieldNumber];
        if (readiness != null &&
            readiness != _fitUint8Invalid &&
            readiness <= 100) {
          _trainingReadiness = readiness;
          if (messageTimestamp != null) {
            _metricsTime ??= _fitDateTimeInstant(messageTimestamp);
          }
        }
        break;
      case _fitTrainingLoadMessageNumber:
        final acute = values[_fitTrainingLoadAcuteFieldNumber];
        if (acute != null && acute != _fitUint16Invalid) {
          _trainingLoadAcute = acute;
        }
        final chronic = values[_fitTrainingLoadChronicFieldNumber];
        if (chronic != null && chronic != _fitUint16Invalid) {
          _trainingLoadChronic = chronic;
        }
        if (messageTimestamp != null) {
          _metricsTime ??= _fitDateTimeInstant(messageTimestamp);
        }
        break;
      case _fitPhysiologicalMetricsMessageNumber:
        // Only recovery_time is taken. This message also carries VO2 max under
        // a different scale, but max_met_data above is the one the watch keeps
        // current, and reading both would let a stale copy win at random.
        final recovery = values[_fitRecoveryTimeFieldNumber];
        if (recovery != null && recovery != _fitUint16Invalid) {
          _recoveryTimeMinutes = recovery;
          if (messageTimestamp != null) {
            _metricsTime ??= _fitDateTimeInstant(messageTimestamp);
          }
        }
        break;
      case _fitRespirationRateMessageNumber:
        final rateRaw = values[_fitRespirationRateFieldNumber];
        if (rateRaw != null && messageTimestamp != null) {
          final rate = rateRaw / _fitRespirationScale;
          // Negative / zero is the "not measuring" sentinel.
          if (rate > 0 && rate < 100) {
            _respiration.add((_fitDateTimeInstant(messageTimestamp), rate));
          }
        }
        break;
    }
  }

  /// One Health Snapshot message: a whole recording packed into one record.
  ///
  /// The samples are laid out FORWARD from the record's timestamp, `interval`
  /// seconds apart. That is an assumption — nothing documents it and
  /// Gadgetbridge never parses these — so the shape is logged on every record:
  /// compare the printed span against the Health Snapshot on the watch, and if
  /// the window is shifted by its own length, the timestamp marks the END and
  /// this needs inverting.
  void _readHsaSamples(
    int messageNumber,
    Map<int, int> values,
    Map<int, List<int>> arrays,
    int? messageTimestamp,
  ) {
    if (messageTimestamp == null) return;
    final samples = arrays[_fitHsaValueFieldNumber] ?? const [];
    if (samples.isEmpty) return;
    // A zero or missing interval would stack every sample on one instant.
    final interval = values[_fitHsaIntervalFieldNumber] ?? 0;
    if (interval <= 0) {
      if (kDebugMode) {
        debugPrint('[FIT-HSA] message $messageNumber: ${samples.length} samples '
            'with no usable interval ($interval) — dropped');
      }
      return;
    }
    final start = _fitDateTimeInstant(messageTimestamp);
    for (var i = 0; i < samples.length; i++) {
      final at = start.add(Duration(seconds: interval * i));
      final raw = samples[i];
      switch (messageNumber) {
        case _fitHsaSpo2MessageNumber:
          if (raw > 0 && raw <= 100) _hsaSpo2.add((at, raw));
        case _fitHsaStressMessageNumber:
          // Negative is Garmin's "not measurable", as in stress_level (227).
          if (raw >= 0 && raw <= 100) _hsaStress.add((at, raw));
        case _fitHsaRespirationMessageNumber:
          final rate = raw / _fitHsaRespirationScale;
          if (rate > 0 && rate < 100) _hsaRespiration.add((at, rate));
        case _fitHsaBodyBatteryMessageNumber:
          if (raw >= 0 && raw <= 100) _hsaBodyEnergy.add((at, raw));
      }
    }
    if (kDebugMode) {
      debugPrint('[FIT-HSA] message $messageNumber: ${samples.length} samples '
          'every ${interval}s from ${start.toIso8601String()} '
          'spanning ${interval * (samples.length - 1)}s '
          '(first=${samples.first} last=${samples.last})');
    }
  }

  /// One `monitoring` message: resolve its timestamp (full or `timestamp_16`
  /// relative to the running anchor) and pull HR + the cumulative counters.
  void _readMonitoring(Map<int, int> values, int? fullTimestamp) {
    int? tsRaw;
    if (fullTimestamp != null) {
      tsRaw = fullTimestamp;
      _monLastTimestampRaw = fullTimestamp;
    } else {
      final ts16 = values[_fitMonitoringTimestamp16FieldNumber];
      final anchor = _monLastTimestampRaw;
      if (ts16 != null && anchor != null) {
        // Roll the low 16 bits forward from the anchor (FIT timestamp_16).
        tsRaw = anchor + ((ts16 - (anchor & 0xFFFF)) & 0xFFFF);
        _monLastTimestampRaw = tsRaw;
      }
    }
    if (tsRaw == null) return;
    final time = _fitDateTimeInstant(tsRaw);

    final hr = values[_fitMonitoringHeartRateFieldNumber];
    if (hr != null && hr != _fitUint8Invalid && hr > 0) {
      _monHeartRate.add((time, hr));
    }
    final intensityByte = values[_fitMonitoringActivityTypeIntensityFieldNumber];
    final declaredType = values[_fitMonitoringActivityTypeFieldNumber] ??
        (intensityByte != null
            ? intensityByte & _fitMonitoringActivityTypeMask
            : null);
    if (declaredType != null) _monCurrentActivityType = declaredType;
    final activityType = _monCurrentActivityType ?? -1;
    final steps = values[_fitMonitoringStepsFieldNumber];
    if (steps != null) {
      _monSteps.add(FitMonitoringPoint(
          time: time, activityType: activityType, value: steps));
    }
    final distance = values[_fitMonitoringDistanceFieldNumber];
    if (distance != null) {
      _monDistance.add(FitMonitoringPoint(
          time: time, activityType: activityType, value: distance));
    }
    final calories = values[_fitMonitoringActiveCaloriesFieldNumber];
    if (calories != null) {
      _monCalories.add(FitMonitoringPoint(
          time: time, activityType: activityType, value: calories));
    }
    final moderate = values[_fitMonitoringModerateMinutesFieldNumber] ??
        values[_fitMonitoringModerateMinutesAltFieldNumber];
    if (moderate != null && moderate != _fitUint16Invalid) {
      _monModerateMinutes.add((time, moderate));
    }
    final vigorous = values[_fitMonitoringVigorousMinutesFieldNumber] ??
        values[_fitMonitoringVigorousMinutesAltFieldNumber];
    if (vigorous != null && vigorous != _fitUint16Invalid) {
      _monVigorousMinutes.add((time, vigorous));
    }
  }

  void _addFileId(Map<int, int> values) {
    _fileType = values[_fitFileIdTypeFieldNumber] ?? _fileType;
  }

  void _addCourseMetadata(Map<int, int> values, Map<int, String> strings) {
    _metadataName ??= strings[_fitCourseNameFieldNumber];
    _sport ??= _generic(values[_fitCourseSportFieldNumber]);
  }

  void _addWorkoutMetadata(Map<int, int> values, Map<int, String> strings) {
    _metadataName ??= strings[_fitWorkoutNameFieldNumber];
    _sport ??= _generic(values[_fitWorkoutSportFieldNumber]);
  }

  void _addWorkoutStep(Map<int, int> values) {
    final durationType = values[_fitWorkoutStepDurationTypeFieldNumber];
    if (durationType == null) return;
    final durationValue = values[_fitWorkoutStepDurationValueFieldNumber];
    if (durationValue == null) return;
    int? seconds;
    if (durationType == _fitWorkoutDurationTypeTime ||
        durationType == _fitWorkoutDurationTypeRepeatUntilTime ||
        durationType == _fitWorkoutDurationTypeRepetitionTime) {
      seconds = (durationValue / _fitTimeScale).round();
    }
    if (seconds == null || seconds <= 0) return;
    _workoutDurationSeconds = _sumInt(_workoutDurationSeconds, seconds);
  }

  void _rememberRecordTime(int? timestampRaw) {
    if (timestampRaw == null) return;
    final time = _fitDateTimeInstant(timestampRaw);
    _firstRecordTime = _earliest(_firstRecordTime, time);
    _lastRecordTime = _latest(_lastRecordTime, time);
  }

  _FitActivitySummary _fitSummary() {
    int? recordDuration;
    if (_firstRecordTime != null && _lastRecordTime != null) {
      final seconds = _lastRecordTime!.difference(_firstRecordTime!).inSeconds;
      if (seconds > 0) recordDuration = seconds;
    }
    final recordSummary = _FitActivitySummary(
      startTime: _firstRecordTime,
      endTime: _lastRecordTime,
      durationSeconds: recordDuration,
    );
    return _sessionSummary.withFallback(_lapSummary).withFallback(recordSummary).withFallback(
          _FitActivitySummary(
            fileType: _fileType,
            name: _metadataName,
            durationSeconds: _workoutDurationSeconds,
            sport: _sport,
            subSport: _subSport,
          ),
        );
  }

  void _addCourseRecordPoint(Map<int, int> values, int? timestampRaw) {
    final timestamp = timestampRaw != null
        ? _fitDateTimeInstant(timestampRaw)
        : _syntheticFitStartTime.add(Duration(seconds: _courseRecordIndex));
    _courseRecordIndex += 1;
    _addRecordPoint(values, timestamp);
  }

  /// Heart rate, cadence and speed, straight off the `record` message.
  ///
  /// FIT stores speed as an integer of millimetres per second (scale 1000), and
  /// `enhanced_speed` is the same thing with more headroom, so it wins when present.
  /// Heart rate and cadence are plain bytes. A zero cadence is a real reading --
  /// you stopped pedalling -- but a zero heart rate is not, so only the latter is
  /// dropped.
  void _addSamples(Map<int, int> values, DateTime timestamp) {
    final bpm = values[_fitRecordHeartRateFieldNumber];
    if (bpm != null && bpm > 0 && bpm < 300) {
      _heartRateSamples.add(
        BleHeartRateSample(time: timestamp, beatsPerMinute: bpm),
      );
    }

    final cadence = values[_fitRecordCadenceFieldNumber];
    if (cadence != null && cadence >= 0 && cadence < 250) {
      _cadenceSamples.add((timestamp, cadence));
    }

    final speedRaw = values[_fitRecordEnhancedSpeedFieldNumber] ??
        values[_fitRecordSpeedFieldNumber];
    if (speedRaw != null && speedRaw > 0) {
      _speedSamples.add(
        BleSpeedSample(
          time: timestamp,
          metersPerSecond: speedRaw / _fitSpeedScale,
          // Set from the session's sport once it is known -- see [sampleBuffer].
          isRunning: false,
        ),
      );
    }
  }

  final List<BleHeartRateSample> _heartRateSamples = [];
  final List<BleSpeedSample> _speedSamples = [];
  final List<(DateTime, int)> _cadenceSamples = [];

  _FitSamples get samples =>
      _FitSamples(_heartRateSamples, _speedSamples, _cadenceSamples);

  void _addRecordPointRaw(Map<int, int> values, int? timestampRaw) {
    if (timestampRaw == null) return;
    _addRecordPoint(values, _fitDateTimeInstant(timestampRaw));
  }

  void _addRecordPoint(Map<int, int> values, DateTime timestamp) {
    // BEFORE the GPS guard, deliberately. A record without a position still carries
    // a heart rate and a cadence -- an indoor trainer session has nothing else --
    // and the old early-return threw all of it away.
    _addSamples(values, timestamp);

    final latRaw = values[_fitRecordPositionLatFieldNumber];
    if (latRaw == null) return;
    final latitude = _fitSemicirclesToDegrees(latRaw);
    if (latitude < minLatitude || latitude > maxLatitude) return;
    final longRaw = values[_fitRecordPositionLongFieldNumber];
    if (longRaw == null) return;
    final longitude = _fitSemicirclesToDegrees(longRaw);
    if (longitude < minLongitude || longitude > maxLongitude) return;
    final altitudeRaw = values[_fitRecordEnhancedAltitudeFieldNumber] ??
        values[_fitRecordAltitudeFieldNumber];
    final altitudeMeters =
        altitudeRaw == null ? null : _fitAltitudeMeters(altitudeRaw);
    _points.add(
      ExerciseRoutePoint(
        time: timestamp,
        latitude: latitude,
        longitude: longitude,
        altitudeMeters: altitudeMeters,
        horizontalAccuracyMeters: null,
        verticalAccuracyMeters: null,
      ),
    );
  }

  int? _compressedTimestamp(int offset) {
    final previous = _lastTimestampRaw;
    if (previous == null) return null;
    final previousOffset = previous & _fitCompressedTimestampMask;
    final delta = offset < previousOffset
        ? offset + _fitCompressedTimestampRollover - previousOffset
        : offset - previousOffset;
    return previous + delta;
  }

  _FitActivitySummary _toFitActivitySummary(Map<int, int> values, int? timestampRaw) {
    final startRaw = values[_fitStartTimeFieldNumber];
    final startTime = startRaw == null ? null : _fitDateTimeInstant(startRaw);
    final durationRaw = values[_fitTotalElapsedTimeFieldNumber] ??
        values[_fitTotalTimerTimeFieldNumber];
    final durationSeconds = durationRaw == null ? null : durationRaw / _fitTimeScale;
    DateTime? endTime;
    if (startTime != null && durationSeconds != null && durationSeconds > 0.0) {
      endTime = startTime.add(
        Duration(milliseconds: (durationSeconds * 1000.0).round()),
      );
    } else if (timestampRaw != null) {
      endTime = _fitDateTimeInstant(timestampRaw);
    }
    final distanceRaw = values[_fitTotalDistanceFieldNumber];
    final ascentRaw = values[_fitTotalAscentFieldNumber];
    final caloriesRaw = values[_fitTotalCaloriesFieldNumber];
    return _FitActivitySummary(
      startTime: startTime,
      endTime: endTime,
      durationSeconds: durationSeconds?.round(),
      distanceMeters: distanceRaw == null ? null : distanceRaw / _fitDistanceScale,
      elevationGainedMeters: ascentRaw?.toDouble(),
      // FIT session field 11 is `total_calories`. It was being written into ACTIVE
      // calories -- the constant three lines up says TOTAL and the field it fed said
      // ACTIVE, and nothing objected.
      //
      // The consequence was not just a mislabelled number. Nothing then filled
      // `totalCalories`, so the form estimated one, and the estimate came out BELOW
      // the total that was sitting in the active field -- so importing a real ride
      // produced "Total calories cannot be lower than active calories" and would not
      // save. A 511 kcal ride arrived as 511 active against an estimated 376 total.
      //
      // The FIT session message has no separate active-calorie field, so active is
      // left unknown rather than invented. Null is honest; a number is not.
      totalCaloriesKcal: caloriesRaw?.toDouble(),
      sport: _generic(values[_fitSessionSportFieldNumber]),
    );
  }
}

class _FitDataReader {
  _FitDataReader(this.bytes, this.offset, this.endOffset);

  final Uint8List bytes;
  int offset;
  final int endOffset;

  bool hasRemaining() => offset < endOffset;

  int readUnsignedByte() {
    if (offset >= endOffset) {
      throw const RouteImportException(
        'FIT file ended before data records were complete.',
      );
    }
    return bytes[offset++] & 0xFF;
  }

  int readUnsignedShort(bool littleEndian) {
    if (offset + 2 > endOffset) {
      throw const RouteImportException(
        'FIT file ended before data records were complete.',
      );
    }
    final value = _readUint16(bytes, offset, littleEndian);
    offset += 2;
    return value;
  }

  Uint8List readBytes(int size) {
    if (size < 0 || offset + size > endOffset) {
      throw const RouteImportException(
        'FIT file ended before data records were complete.',
      );
    }
    final slice = Uint8List.sublistView(bytes, offset, offset + size);
    offset += size;
    return slice;
  }

  void skip(int size) {
    if (size < 0 || offset + size > endOffset) {
      throw const RouteImportException(
        'FIT file ended before data records were complete.',
      );
    }
    offset += size;
  }
}

bool isFitFile(Uint8List bytes) => _isFitFileAt(bytes, 0);

bool _isFitFileAt(Uint8List bytes, int offset) {
  if (offset < 0 || offset + _fitMinimumHeaderSize > bytes.length) return false;
  final headerSize = bytes[offset] & 0xFF;
  return headerSize >= _fitMinimumHeaderSize &&
      offset + headerSize <= bytes.length &&
      bytes[offset + _fitHeaderDataTypeOffset] == 0x2E && // '.'
      bytes[offset + _fitHeaderDataTypeOffset + 1] == 0x46 && // 'F'
      bytes[offset + _fitHeaderDataTypeOffset + 2] == 0x49 && // 'I'
      bytes[offset + _fitHeaderDataTypeOffset + 3] == 0x54; // 'T'
}

int _readUint16(Uint8List bytes, int index, bool littleEndian) {
  final first = bytes[index] & 0xFF;
  final second = bytes[index + 1] & 0xFF;
  return littleEndian ? first | (second << 8) : (first << 8) | second;
}

int _readSignedShort(Uint8List bytes, int index, bool littleEndian) {
  final value = _readUint16(bytes, index, littleEndian);
  return value & 0x8000 != 0 ? value - 0x10000 : value;
}

int _readUint32(Uint8List bytes, int index, bool littleEndian) {
  final b0 = bytes[index] & 0xFF;
  final b1 = bytes[index + 1] & 0xFF;
  final b2 = bytes[index + 2] & 0xFF;
  final b3 = bytes[index + 3] & 0xFF;
  return littleEndian
      ? b0 | (b1 << 8) | (b2 << 16) | (b3 << 24)
      : (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
}

int _readInt32(Uint8List bytes, int index, bool littleEndian) {
  final raw = _readUint32(bytes, index, littleEndian);
  return raw >= 0x80000000 ? raw - 0x100000000 : raw;
}

/// Every element of an array field, invalid sentinels dropped.
///
/// FIT expresses an array as a field whose declared size is a multiple of its
/// base type's — the Health Snapshot messages pack a whole two-minute recording
/// into one record this way. [_fitLong] reads only the first element, which is
/// right for every scalar field and silently loses the rest of an array.
List<int> _fitLongArray(
  Uint8List bytes,
  _FitFieldDefinition field,
  bool littleEndian,
) {
  final baseType = field.baseType & _fitBaseTypeMask;
  final size = _fitBaseTypeSize(baseType);
  if (size <= 0) return const [];
  final out = <int>[];
  for (var offset = 0; offset + size <= bytes.length; offset += size) {
    final value = _fitLong(
      Uint8List.sublistView(bytes, offset, offset + size),
      field,
      littleEndian,
    );
    if (value != null) out.add(value);
  }
  return out;
}

int? _fitLong(Uint8List bytes, _FitFieldDefinition field, bool littleEndian) {
  final baseType = field.baseType & _fitBaseTypeMask;
  final baseTypeSize = _fitBaseTypeSize(baseType);
  if (baseTypeSize <= 0 || bytes.length < baseTypeSize) return null;
  switch (baseType) {
    case _fitBaseTypeEnum:
    case _fitBaseTypeUInt8:
      final v = bytes[0] & 0xFF;
      return v == _fitInvalidUInt8 ? null : v;
    case _fitBaseTypeSInt8:
      final v = bytes[0] & 0xFF;
      final signed = v >= 0x80 ? v - 0x100 : v;
      return signed == _fitInvalidSInt8 ? null : signed;
    case _fitBaseTypeSInt16:
      final v = _readSignedShort(bytes, 0, littleEndian);
      return v == _fitInvalidSInt16 ? null : v;
    case _fitBaseTypeUInt16:
      final v = _readUint16(bytes, 0, littleEndian);
      return v == _fitInvalidUInt16 ? null : v;
    case _fitBaseTypeSInt32:
      final v = _readInt32(bytes, 0, littleEndian);
      return v == _fitInvalidSInt32 ? null : v;
    case _fitBaseTypeUInt32:
      final v = _readUint32(bytes, 0, littleEndian);
      return v == _fitInvalidUInt32 ? null : v;
    case _fitBaseTypeUInt8z:
      final v = bytes[0] & 0xFF;
      return v == 0 ? null : v;
    case _fitBaseTypeUInt16z:
      final v = _readUint16(bytes, 0, littleEndian);
      return v == 0 ? null : v;
    case _fitBaseTypeUInt32z:
      final v = _readUint32(bytes, 0, littleEndian);
      return v == 0 ? null : v;
    default:
      return null;
  }
}

String? _fitString(Uint8List bytes, _FitFieldDefinition field) {
  final baseType = field.baseType & _fitBaseTypeMask;
  if (baseType != _fitBaseTypeString) return null;
  var decoded = utf8.decode(bytes, allowMalformed: true);
  var end = decoded.length;
  while (end > 0 && decoded.codeUnitAt(end - 1) == 0) {
    end--;
  }
  decoded = decoded.substring(0, end);
  return cleanText(decoded);
}

int _fitBaseTypeSize(int baseType) {
  switch (baseType) {
    case _fitBaseTypeEnum:
    case _fitBaseTypeSInt8:
    case _fitBaseTypeUInt8:
    case _fitBaseTypeString:
    case _fitBaseTypeUInt8z:
    case _fitBaseTypeByte:
      return 1;
    case _fitBaseTypeSInt16:
    case _fitBaseTypeUInt16:
    case _fitBaseTypeUInt16z:
      return 2;
    case _fitBaseTypeSInt32:
    case _fitBaseTypeUInt32:
    case _fitBaseTypeFloat32:
    case _fitBaseTypeUInt32z:
      return 4;
    case _fitBaseTypeFloat64:
    case _fitBaseTypeSInt64:
    case _fitBaseTypeUInt64:
    case _fitBaseTypeUInt64z:
      return 8;
    default:
      return 0;
  }
}

int? _generic(int? value) =>
    (value == null || value == _fitSportGeneric) ? null : value;

double _fitSemicirclesToDegrees(int value) =>
    value.toDouble() * 180.0 / _fitSemicircleDegreesDivisor;

double _fitAltitudeMeters(int value) =>
    value.toDouble() / _fitAltitudeScale - _fitAltitudeOffsetMeters;

DateTime _fitDateTimeInstant(int value) => DateTime.fromMillisecondsSinceEpoch(
      (_fitEpochUnixSeconds + value) * 1000,
      isUtc: true,
    );

/// FIT sport 2 and 21 are cycling; everything else is on foot or in the water.
///
/// It decides which Health Connect record the cadence goes into: pedalling cadence
/// and step cadence are different record types, and FIT field 4 is just "cadence".
bool _fitSportIsCycling(int? sport) => sport == 2 || sport == 21;

/// What the file says this was, in the words the type inference reads.
///
/// The SUB-sport wins when it names the activity outright: a treadmill run is
/// not a run that happens to be indoors, it is a different Health Connect
/// exercise type, and the same goes for a trainer ride and a strength session.
/// Sub-sports that merely qualify an outdoor sport ("street", "trail", "road")
/// name nothing and leave the sport to speak.
String? _fitSportName(int? sport, [int? subSport]) =>
    _fitSubSportName(subSport) ?? _fitPlainSportName(sport);

/// The sub-sports that ARE the activity. FIT `sub_sport` enum.
String? _fitSubSportName(int? value) => switch (value) {
      1 => 'treadmill',
      // 5 spin, 6 indoor_cycling — a trainer and a spin bike, both stationary.
      5 || 6 => 'indoor cycling',
      14 => 'indoor rowing',
      20 => 'strength training',
      _ => null,
    };

String? _fitPlainSportName(int? value) {
  switch (value) {
    case 1:
      return 'running';
    case 2:
    case 21:
      return 'cycling';
    case 4:
      return 'fitness equipment';
    case 5:
      return 'swimming';
    case 10:
      return 'training';
    case 11:
      return 'walking';
    case 12:
    case 13:
      return 'skiing';
    case 14:
      return 'snowboarding';
    case 15:
      return 'rowing';
    case 17:
      return 'hiking';
    case 19:
    case 37:
    case 41:
    case 42:
      return 'paddling';
    case 25:
      return 'golf';
    case 30:
    case 33:
      return 'skating';
    case 32:
      return 'sailing';
    case 35:
      return 'snowshoeing';
    case 38:
      return 'surfing';
    case 47:
      return 'boxing';
    case 62:
      return 'interval training';
    default:
      return null;
  }
}

DateTime? _earliest(DateTime? a, DateTime? b) {
  if (a == null) return b;
  if (b == null) return a;
  return a.isBefore(b) ? a : b;
}

DateTime? _latest(DateTime? a, DateTime? b) {
  if (a == null) return b;
  if (b == null) return a;
  return a.isAfter(b) ? a : b;
}

double? _sumDouble(double? a, double? b) {
  if (a == null) return b;
  if (b == null) return a;
  return a + b;
}

int? _sumInt(int? a, int? b) {
  if (a == null) return b;
  if (b == null) return a;
  return a + b;
}

const int _fitMinimumHeaderSize = 12;
const int _fitHeaderDataSizeOffset = 4;
const int _fitHeaderDataTypeOffset = 8;
const int _fitCrcSize = 2;
const int _fitCompressedHeaderFlag = 0x80;
const int _fitCompressedLocalMessageTypeShift = 5;
const int _fitCompressedLocalMessageTypeMask = 0x03;
const int _fitCompressedTimestampMask = 0x1F;
const int _fitCompressedTimestampRollover = 0x20;
const int _fitDefinitionMessageFlag = 0x40;
const int _fitDeveloperDataFlag = 0x20;
const int _fitNormalLocalMessageTypeMask = 0x0F;
const int _fitArchitectureLittleEndian = 0;
const int _fitArchitectureBigEndian = 1;
const int _fitFileIdMessageNumber = 0;
const int _fitFileIdTypeFieldNumber = 0;
const int _fitFileTypeWorkout = 5;
const int _fitFileTypeCourse = 6;
const int _fitRecordMessageNumber = 20;
const int _fitLapMessageNumber = 19;
const int _fitSessionMessageNumber = 18;

// Sleep (Garmin file type 49). See docs/reference/garmin-fit-files.md.
const int _fitEventMessageNumber = 21;
const int _fitSleepLevelMessageNumber = 275;
const int _fitEventFieldNumber = 0;
const int _fitEventTypeFieldNumber = 1;
const int _fitSleepLevelFieldNumber = 0;
const int _fitSleepEventValue = 74; // `event` == sleep (Garmin-proprietary)
const int _fitEventTypeStart = 0;
const int _fitEventTypeStop = 1;

// HRV status (Garmin file type 68). `hrv_status_summary.last_night_average`
// (field 1, uint16, scale 128) is the night's RMSSD in ms.
const int _fitHrvStatusSummaryMessageNumber = 370;
const int _fitHrvLastNightAverageFieldNumber = 1;
const double _fitHrvRmssdScale = 128.0;
const int _fitUint16Invalid = 0xFFFF;

// Monitoring (Garmin file type 32). One-per-file summaries:
const int _fitMonitoringHrDataMessageNumber = 211;
const int _fitRestingHeartRateFieldNumber = 0;
const int _fitMonitoringInfoMessageNumber = 103;
const int _fitRestingMetabolicRateFieldNumber = 5;
const int _fitUint8Invalid = 0xFF;

// Monitoring high-frequency series. `monitoring` (55) carries per-minute HR and
// the cumulative step/distance/calorie counters; `respiration_rate` (297) the
// breathing series. Most `monitoring` messages timestamp with `timestamp_16`
// (field 26) — the low 16 bits relative to the last full timestamp — not a full
// `timestamp` (253). See docs/reference/garmin-fit-files.md.
const int _fitMonitoringMessageNumber = 55;
const int _fitRespirationRateMessageNumber = 297;

// stress_level (227) carries the stress score AND Body Battery; the latter has
// no message of its own. Field numbers from Gadgetbridge's
// AbstractFitStressLevel (AGPLv3).
const int _fitStressLevelMessageNumber = 227;
const int _fitStressLevelValueFieldNumber = 0; // sint8, 0..100 (negative = n/a)
const int _fitStressLevelTimeFieldNumber = 1; // uint32, Garmin epoch seconds
const int _fitStressBodyEnergyFieldNumber = 3; // uint8, 0..100
const int _fitMonitoringDistanceFieldNumber = 2; // uint32, ÷100 m, cumulative
const int _fitMonitoringStepsFieldNumber = 3; // uint32, raw == steps (walk/run)
const int _fitMonitoringActivityTypeFieldNumber = 5;
const int _fitMonitoringActiveCaloriesFieldNumber = 19; // uint16, cumulative
// current_activity_type_intensity (byte): activity_type in the low 5 bits. Most
// monitoring messages carry the type here, not in field 5.
const int _fitMonitoringActivityTypeIntensityFieldNumber = 24;
const int _fitMonitoringActivityTypeMask = 0x1F;
const int _fitMonitoringTimestamp16FieldNumber = 26;
const int _fitMonitoringHeartRateFieldNumber = 27; // uint8, bpm
const int _fitRespirationRateFieldNumber = 0; // sint16, ÷100 breaths/min
// Intensity minutes. Garmin writes the running daily totals into 37/38 on this
// watch; 33/34 are the same quantity under the names the FIT profile documents.
// Both are read, later-wins, because which pair a device populates varies.
const int _fitMonitoringModerateMinutesFieldNumber = 37; // uint16, minutes
const int _fitMonitoringVigorousMinutesFieldNumber = 38; // uint16, minutes
const int _fitMonitoringModerateMinutesAltFieldNumber = 33;
const int _fitMonitoringVigorousMinutesAltFieldNumber = 34;

// Metrics (Garmin file type 44). Four unrelated messages share the file; each
// is a one-per-file snapshot rather than a series, so the last seen wins.
const int _fitMaxMetDataMessageNumber = 229;
const int _fitVo2MaxFieldNumber = 2; // uint16, scale 10, mL/kg/min
const double _fitVo2MaxScale = 10.0;
const int _fitTrainingReadinessMessageNumber = 369;
const int _fitTrainingReadinessFieldNumber = 0; // uint8, 0..100
const int _fitTrainingLoadMessageNumber = 378;
const int _fitTrainingLoadAcuteFieldNumber = 3; // uint16
const int _fitTrainingLoadChronicFieldNumber = 4; // uint16
const int _fitPhysiologicalMetricsMessageNumber = 140;
const int _fitRecoveryTimeFieldNumber = 9; // uint16, minutes

// daily_sleep (384) and sleep_demand (410) — what a vívoactive 5 actually puts
// in its metrics file, in place of the training-load messages other Garmins
// use. This is the watch's own verdict on a night, computed on the wrist.
const int _fitDailySleepMessageNumber = 384;
const int _fitDailySleepScoreFieldNumber = 2; // uint8, 0..100
// awake_duration is in SECONDS, not the minutes Garmin's FIT profile claims: a
// real night read 1020 here inside an 8.7-hour window, and 1020 minutes is 17
// hours. Reading it as minutes would report more time awake than time in bed.
const int _fitDailySleepAwakeDurationFieldNumber = 3; // uint16, seconds
const int _fitDailySleepEndTimeFieldNumber = 11; // uint32, Garmin epoch
const int _fitDailySleepPressureFieldNumber = 22; // sint16
const int _fitSleepDemandMessageNumber = 410;
const int _fitSleepDemandNormalFieldNumber = 0; // uint16, minutes
const int _fitSleepDemandDemandFieldNumber = 1; // uint16, minutes
const int _fitSint16Invalid = 0x7FFF;

// Health Snapshot (Garmin file type 70). Each message packs a whole recording
// into ONE record: field 0 is the seconds between samples and field 1 (plus 2/3
// for Body Battery) is an ARRAY of readings. Gadgetbridge pulls this file and
// never parses it, so there is no port to follow and nothing documents how the
// samples line up against the record timestamp — see the diagnostic in
// [_readHsaSamples].
const int _fitHsaSpo2MessageNumber = 305;
const int _fitHsaStressMessageNumber = 306;
const int _fitHsaRespirationMessageNumber = 307;
const int _fitHsaBodyBatteryMessageNumber = 314;
const int _fitHsaIntervalFieldNumber = 0; // uint16, seconds between samples
const int _fitHsaValueFieldNumber = 1; // array of readings
const double _fitHsaRespirationScale = 100.0;

/// Messages whose fields must be decoded as arrays, not scalars. Kept to the
/// few that need it so every other message keeps the cheaper scalar path.
const Set<int> _fitArrayMessageNumbers = {
  _fitHsaSpo2MessageNumber,
  _fitHsaStressMessageNumber,
  _fitHsaRespirationMessageNumber,
  _fitHsaBodyBatteryMessageNumber,
};

// Sleep extras, in the same type-49 file the stage transitions come from.
// sleep_stats (346) is the watch's OWN assessment of the night — the scores it
// shows on the wrist — which is independent of the stages we derive ourselves.
const int _fitSleepStatsMessageNumber = 346;
const int _fitOverallSleepScoreFieldNumber = 6; // uint8, 0..100
const int _fitAwakeningsCountFieldNumber = 11; // uint8
// nap (412) bounds a daytime sleep with its own start/end, separate from the
// night's event/74 pair.
const int _fitNapMessageNumber = 412;
const int _fitNapStartFieldNumber = 0; // uint32, Garmin epoch seconds
const int _fitNapEndFieldNumber = 2; // uint32, Garmin epoch seconds
const double _fitRespirationScale = 100.0;
const int _fitCourseMessageNumber = 31;
const int _fitCourseSportFieldNumber = 4;
const int _fitCourseNameFieldNumber = 5;
const int _fitWorkoutMessageNumber = 26;
const int _fitWorkoutSportFieldNumber = 4;
const int _fitWorkoutNameFieldNumber = 8;
const int _fitWorkoutStepMessageNumber = 27;
const int _fitWorkoutStepDurationTypeFieldNumber = 1;
const int _fitWorkoutStepDurationValueFieldNumber = 2;
const int _fitTimestampFieldNumber = 253;
const int _fitStartTimeFieldNumber = 2;
const int _fitSessionSportFieldNumber = 5;

/// FIT session field 6, `sub_sport`: the field that knows the session was run on
/// a TREADMILL rather than a street, and pedalled on a trainer rather than a
/// road. The sport alone cannot say — an indoor ride and an Alpine descent are
/// both sport 2 — and without it every indoor session imported as its outdoor
/// twin.
const int _fitSessionSubSportFieldNumber = 6;
const int _fitTotalElapsedTimeFieldNumber = 7;
const int _fitTotalTimerTimeFieldNumber = 8;
const int _fitTotalDistanceFieldNumber = 9;
const int _fitTotalCaloriesFieldNumber = 11;
const int _fitTotalAscentFieldNumber = 21;
// FIT `record` message fields. The parser read only the first three, so a FIT
// import arrived with a route and nothing else: no heart rate, no cadence, no
// speed, and therefore not a single graph on the activity. An indoor ride --
// no GPS at all -- arrived with nothing whatsoever.
const int _fitRecordHeartRateFieldNumber = 3;
const int _fitRecordCadenceFieldNumber = 4;
const int _fitRecordSpeedFieldNumber = 6;
const int _fitRecordEnhancedSpeedFieldNumber = 73;
const int _fitRecordPositionLatFieldNumber = 0;
const int _fitRecordPositionLongFieldNumber = 1;
const int _fitRecordAltitudeFieldNumber = 2;
const int _fitRecordEnhancedAltitudeFieldNumber = 78;
const int _fitSportGeneric = 0;
const int _fitBaseTypeMask = 0x1F;
const int _fitBaseTypeEnum = 0;
const int _fitBaseTypeSInt8 = 1;
const int _fitBaseTypeUInt8 = 2;
const int _fitBaseTypeSInt16 = 3;
const int _fitBaseTypeUInt16 = 4;
const int _fitBaseTypeSInt32 = 5;
const int _fitBaseTypeUInt32 = 6;
const int _fitBaseTypeString = 7;
const int _fitBaseTypeFloat32 = 8;
const int _fitBaseTypeFloat64 = 9;
const int _fitBaseTypeUInt8z = 10;
const int _fitBaseTypeUInt16z = 11;
const int _fitBaseTypeUInt32z = 12;
const int _fitBaseTypeByte = 13;
const int _fitBaseTypeSInt64 = 14;
const int _fitBaseTypeUInt64 = 15;
const int _fitBaseTypeUInt64z = 16;
const int _fitInvalidUInt8 = 0xFF;
const int _fitInvalidSInt8 = 0x7F;
const int _fitInvalidUInt16 = 0xFFFF;
const int _fitInvalidSInt16 = 0x7FFF;
const int _fitInvalidUInt32 = 0xFFFFFFFF;
const int _fitInvalidSInt32 = 0x7FFFFFFF;
const int _fitEpochUnixSeconds = 631065600;
const double _fitSemicircleDegreesDivisor = 2147483648.0;
const double _fitAltitudeScale = 5.0;
const double _fitAltitudeOffsetMeters = 500.0;
const double _fitTimeScale = 1000.0;
const double _fitDistanceScale = 100.0;
/// FIT stores speed as an integer of millimetres per second.
const double _fitSpeedScale = 1000.0;
const int _fitWorkoutDurationTypeTime = 0;
const int _fitWorkoutDurationTypeRepeatUntilTime = 7;
const int _fitWorkoutDurationTypeRepetitionTime = 28;
const int _defaultFitWorkoutDurationSeconds = 30 * 60;
final DateTime _syntheticFitStartTime =
    DateTime.fromMillisecondsSinceEpoch(0, isUtc: true);
const Set<int> _fitParsedMessageNumbers = {
  _fitFileIdMessageNumber,
  _fitRecordMessageNumber,
  _fitLapMessageNumber,
  _fitSessionMessageNumber,
  _fitCourseMessageNumber,
  _fitWorkoutMessageNumber,
  _fitWorkoutStepMessageNumber,
  _fitEventMessageNumber,
  _fitSleepLevelMessageNumber,
  _fitHrvStatusSummaryMessageNumber,
  _fitMonitoringHrDataMessageNumber,
  _fitMonitoringInfoMessageNumber,
  _fitMonitoringMessageNumber,
  _fitRespirationRateMessageNumber,
  _fitStressLevelMessageNumber,
  _fitMaxMetDataMessageNumber,
  _fitTrainingReadinessMessageNumber,
  _fitTrainingLoadMessageNumber,
  _fitPhysiologicalMetricsMessageNumber,
  _fitSleepStatsMessageNumber,
  _fitNapMessageNumber,
  _fitDailySleepMessageNumber,
  _fitSleepDemandMessageNumber,
  _fitHsaSpo2MessageNumber,
  _fitHsaStressMessageNumber,
  _fitHsaRespirationMessageNumber,
  _fitHsaBodyBatteryMessageNumber,
};
