import 'package:flutter/foundation.dart'; // DIAGNOSTIC: debugPrint to logcat (also re-exports Uint8List)

import '../../../core/fit/fit_message.dart';
import '../../../core/fit/fit_reader.dart';

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

/// Decodes the **wellness** data a Garmin FIT file carries (sleep, HRV,
/// monitoring, metrics, …) in one pass. Wellness files have no activity session
/// or route, so the activity parser rejects them — this is their path. Returns
/// an empty [FitWellness] for activity, course and workout files. Field layout:
/// docs/reference/garmin-fit-files.md.
///
/// Built on the generic [FitReader]: this file owns only the Garmin-proprietary
/// interpretation of the decoded messages.
FitWellness parseGarminWellness(Uint8List fitBytes, {String? fileName}) {
  final result = _GarminWellnessDecoder(fitBytes).decode();
  return FitWellness(
    fileType: result.fileType,
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

/// The Garmin sleep session in [fitBytes], or null if it carries none.
FitSleepSession? parseGarminSleepSession(
  Uint8List fitBytes, {
  String? fileName,
}) =>
    parseGarminWellness(fitBytes, fileName: fileName).sleep;

/// One file's decoded wellness carriers, merged across a chained stream by
/// [_GarminWellnessDecoder]. The switch cases fill disjoint carriers, so the
/// four raw structs never overlap.
class _FitWellnessResult {
  const _FitWellnessResult({
    this.fileType,
    required this.sleep,
    required this.hrv,
    required this.monitoring,
    required this.metrics,
  });

  final int? fileType;
  final _FitSleepRaw sleep;
  final _FitHrvRaw hrv;
  final _FitMonitoringRaw monitoring;
  final _FitMetricsRaw metrics;

  _FitWellnessResult merge(_FitWellnessResult other) => _FitWellnessResult(
        // First file with a file type wins, matching the activity summary's
        // per-file merge (a chained stream is one logical export).
        fileType: fileType ?? other.fileType,
        sleep: sleep.merge(other.sleep),
        hrv: hrv.merge(other.hrv),
        monitoring: monitoring.merge(other.monitoring),
        metrics: metrics.merge(other.metrics),
      );
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
    // Each `sleep_level` timestamp is the UPPER BOUND (end) of the stage it
    // names, not its start: the stage runs from the previous transition (the
    // session start for the first) up to this timestamp. Reading it as a start —
    // the stage running forward to the NEXT transition — shifts every span onto
    // the wrong stage, which tripled REM and inflated Awake against the watch's
    // own screen. Confirmed against Gadgetbridge, which fills these with an
    // UPPER_BOUND RangeMap (GarminActivitySampleProvider.overlaySleep).
    var boundary = sessionStart;
    for (var i = 0; i < sorted.length; i++) {
      final (transition, rawLevel) = sorted[i];
      // Clamp into the session so a stray pre-start / post-stop transition can
      // neither widen a stage nor walk the boundary outside the night.
      var stageEnd = transition;
      if (stageEnd.isBefore(sessionStart)) stageEnd = sessionStart;
      if (stageEnd.isAfter(sessionEnd)) stageEnd = sessionEnd;
      final stageStart = boundary;
      boundary = stageEnd;
      // Advance the boundary for every sample, then skip only an unknown raw
      // value (null). An `unmeasurable` span is a real level here and is emitted
      // like any other — it is dropped downstream at the Health Connect mapping,
      // which has no stage for it, exactly as before.
      final level = _fitSleepLevelFromRaw(rawLevel);
      if (level == null) continue;
      if (!stageStart.isBefore(stageEnd)) continue;
      stages.add(FitSleepStage(start: stageStart, end: stageEnd, level: level));
    }
    if (stages.isEmpty) return null;
    // DIAGNOSTIC: the raw transitions and the per-stage totals they produce, to
    // diff against the watch's own screen. Kept after fixing the upper-bound
    // interpretation above, because Garmin still smooths the displayed hypnogram
    // further than the raw `sleep_level` series does, so the two never match to
    // the minute — this is how we see by how much.
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

/// Walks a (possibly chained) FIT byte stream through the generic [FitReader]
/// and interprets each file's messages into the Garmin wellness raw structs.
/// One [_GarminWellnessInterpreter] per file; the results merge across the
/// stream so a later file falls back to — rather than concatenates with — an
/// earlier file's one-per-file scalar fields.
class _GarminWellnessDecoder {
  _GarminWellnessDecoder(this.fileBytes);

  final Uint8List fileBytes;

  _FitWellnessResult decode() {
    var result = const _FitWellnessResult(
      sleep: _FitSleepRaw(),
      hrv: _FitHrvRaw(),
      monitoring: _FitMonitoringRaw(),
      metrics: _FitMetricsRaw(),
    );
    var offset = 0;
    var decodedAnyFile = false;

    while (offset < fileBytes.length) {
      if (!FitReader.isFitFileAt(fileBytes, offset)) {
        if (!decodedAnyFile) {
          throw const FitFormatException('FIT file header is invalid.');
        }
        break;
      }
      final (messages, next) = FitReader.readFile(fileBytes, offset);
      final fileResult = _GarminWellnessInterpreter().interpret(messages);
      result = result.merge(fileResult);
      decodedAnyFile = true;
      offset = next;
    }
    return result;
  }
}

/// Interprets one file's decoded [FitMessage]s into the Garmin wellness raw
/// structs. Its switch cases are disjoint from the activity interpreter's, so
/// an activity file simply yields empty carriers here.
class _GarminWellnessInterpreter {
  int? _fileType;

  // Sleep (file type 49). See docs/reference/garmin-fit-files.md.
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

  _FitWellnessResult interpret(List<FitMessage> messages) {
    // Dispatch in file order, so cases that depend on an earlier message
    // (monitoring_info before its series) still see it.
    for (final message in messages) {
      _dispatch(message);
    }
    return _FitWellnessResult(
      fileType: _fileType,
      sleep: _FitSleepRaw(
        start: _sleepStart,
        stop: _sleepStop,
        levels: _sleepLevels,
        overallScore: _sleepOverallScore,
        awakeningsCount: _sleepAwakenings,
        naps: _naps,
      ),
      hrv: _FitHrvRaw(time: _hrvTime, rmssdMillis: _hrvRmssdMillis),
      monitoring: _FitMonitoringRaw(
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
      metrics: _FitMetricsRaw(
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
    );
  }

  /// Interprets one decoded message into the accumulators. Locals are bound to
  /// the message's fields so the switch below is exactly the code that used to
  /// run inline in the walk.
  void _dispatch(FitMessage message) {
    final values = message.values;
    final arrays = message.arrays;
    final messageTimestamp = message.timestamp;
    switch (message.globalMessageNumber) {
      case _fitFileIdMessageNumber:
        // Only the file type is needed here — it tells the caller whether the
        // file was wellness at all. Everything else on file_id is the activity
        // parser's concern.
        _fileType = values[_fitFileIdTypeFieldNumber] ?? _fileType;
        break;
      case _fitEventMessageNumber:
        // Only the sleep event (Garmin-proprietary value 74) bounds a night;
        // every other event (timer, lap, …) that an activity file carries is
        // ignored here.
        if (values[_fitEventFieldNumber] == _fitSleepEventValue &&
            messageTimestamp != null) {
          final at = fitDateTimeInstant(messageTimestamp);
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
          _sleepLevels.add((fitDateTimeInstant(messageTimestamp), level));
        }
        break;
      case _fitHrvStatusSummaryMessageNumber:
        final raw = values[_fitHrvLastNightAverageFieldNumber];
        if (raw != null && raw != _fitUint16Invalid && messageTimestamp != null) {
          _hrvTime = fitDateTimeInstant(messageTimestamp);
          _hrvRmssdMillis = raw / _fitHrvRmssdScale;
        }
        break;
      case _fitMonitoringHrDataMessageNumber:
        final bpm = values[_fitRestingHeartRateFieldNumber];
        if (bpm != null && bpm != _fitUint8Invalid && bpm > 0) {
          _restingHrBpm = bpm;
          if (messageTimestamp != null) {
            _restingHrTime = fitDateTimeInstant(messageTimestamp);
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
            _bmrTime = fitDateTimeInstant(messageTimestamp);
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
          final at = fitDateTimeInstant(stressTimeRaw);
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
            start: fitDateTimeInstant(napStart),
            end: fitDateTimeInstant(napEnd),
          ));
        }
        break;
      case _fitHsaSpo2MessageNumber:
      case _fitHsaStressMessageNumber:
      case _fitHsaRespirationMessageNumber:
      case _fitHsaBodyBatteryMessageNumber:
        _readHsaSamples(
          message.globalMessageNumber,
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
        if (endRaw != null) _dailySleepEndTime = fitDateTimeInstant(endRaw);
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
          _sleepDemandTime = fitDateTimeInstant(messageTimestamp);
        }
        break;
      case _fitMaxMetDataMessageNumber:
        final vo2 = values[_fitVo2MaxFieldNumber];
        if (vo2 != null && vo2 != _fitUint16Invalid && vo2 > 0) {
          _vo2Max = vo2 / _fitVo2MaxScale;
          if (messageTimestamp != null) {
            _metricsTime = fitDateTimeInstant(messageTimestamp);
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
            _metricsTime ??= fitDateTimeInstant(messageTimestamp);
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
          _metricsTime ??= fitDateTimeInstant(messageTimestamp);
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
            _metricsTime ??= fitDateTimeInstant(messageTimestamp);
          }
        }
        break;
      case _fitRespirationRateMessageNumber:
        final rateRaw = values[_fitRespirationRateFieldNumber];
        if (rateRaw != null && messageTimestamp != null) {
          final rate = rateRaw / _fitRespirationScale;
          // Negative / zero is the "not measuring" sentinel.
          if (rate > 0 && rate < 100) {
            _respiration.add((fitDateTimeInstant(messageTimestamp), rate));
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
    final start = fitDateTimeInstant(messageTimestamp);
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
    final time = fitDateTimeInstant(tsRaw);

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
}

const int _fitFileIdMessageNumber = 0;
const int _fitFileIdTypeFieldNumber = 0;

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
