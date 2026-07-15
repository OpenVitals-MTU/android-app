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
  });

  final DateTime start;
  final DateTime end;
  final List<FitSleepStage> stages;
}

/// A decoded Garmin HRV nightly reading (file type 68):
/// `hrv_status_summary.last_night_average` as an RMSSD in milliseconds.
class FitHrvReading {
  const FitHrvReading({required this.time, required this.rmssdMillis});

  final DateTime time;
  final double rmssdMillis;
}

/// A one-per-file summary value from a monitoring file (type 32): the resting
/// heart rate (`monitoring_hr_data`) and the resting metabolic rate /
/// `BasalMetabolicRate` (`monitoring_info`). The high-frequency series in the
/// same file are decoded elsewhere; these are the volume-safe summaries.
class FitMonitoringSummary {
  const FitMonitoringSummary({
    this.restingHeartRateTime,
    this.restingHeartRateBpm,
    this.bmrTime,
    this.bmrKcalPerDay,
  });

  final DateTime? restingHeartRateTime;
  final int? restingHeartRateBpm;
  final DateTime? bmrTime;
  final double? bmrKcalPerDay;

  bool get isEmpty =>
      restingHeartRateBpm == null && bmrKcalPerDay == null;
}

/// The wellness data a FIT file carried, from one decode pass. Each Garmin file
/// is a single type, so at most one of these is populated (activities have none).
class FitWellness {
  const FitWellness({this.sleep, this.hrv, this.monitoring});

  final FitSleepSession? sleep;
  final FitHrvReading? hrv;
  final FitMonitoringSummary? monitoring;

  bool get isEmpty => sleep == null && hrv == null && monitoring == null;
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
    debugPrint(
      '[FIT] decode start file=${fileName ?? "?"} bytes=${fitBytes.length}',
    );
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
    debugPrint(
      '[FIT] decoded file=${fileName ?? "?"} '
      'fileType=${result.summary.fileType} sport=${result.summary.sport} '
      'subSport=${result.summary.subSport} start=${result.summary.startTime} '
      'end=${result.summary.endTime} routePoints=${routePoints.length}',
    );
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
    final monitoring = result.monitoring.toSummary();
    return FitWellness(
      sleep: result.sleep.toSession(),
      hrv: result.hrv.toReading(),
      monitoring: monitoring,
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
  );

  final List<ExerciseRoutePoint> points;
  final _FitActivitySummary summary;
  final _FitSamples samples;
  final _FitSleepRaw sleep;
  final _FitHrvRaw hrv;
  final _FitMonitoringRaw monitoring;
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
    this.nextOffset,
  );

  final List<ExerciseRoutePoint> points;
  final _FitActivitySummary summary;
  final _FitSamples samples;
  final _FitSleepRaw sleep;
  final _FitHrvRaw hrv;
  final _FitMonitoringRaw monitoring;
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
  });

  final DateTime? restingHrTime;
  final int? restingHrBpm;
  final DateTime? bmrTime;
  final double? bmrKcalPerDay;

  _FitMonitoringRaw merge(_FitMonitoringRaw other) => _FitMonitoringRaw(
        restingHrTime: other.restingHrTime ?? restingHrTime,
        restingHrBpm: other.restingHrBpm ?? restingHrBpm,
        bmrTime: other.bmrTime ?? bmrTime,
        bmrKcalPerDay: other.bmrKcalPerDay ?? bmrKcalPerDay,
      );

  FitMonitoringSummary? toSummary() {
    final summary = FitMonitoringSummary(
      restingHeartRateTime: restingHrTime,
      restingHeartRateBpm: restingHrBpm,
      bmrTime: bmrTime,
      bmrKcalPerDay: bmrKcalPerDay,
    );
    return summary.isEmpty ? null : summary;
  }
}

/// The raw sleep messages a single FIT file carried: the `event`/74 session
/// bounds and the `sleep_level` transitions. Turned into a [FitSleepSession]
/// once the whole file (or chain of files) is decoded.
class _FitSleepRaw {
  const _FitSleepRaw({this.start, this.stop, this.levels = const []});

  final DateTime? start;
  final DateTime? stop;

  /// Each entry is `(transitionTime, sleepLevelEnumValue)`, in file order.
  final List<(DateTime, int)> levels;

  _FitSleepRaw merge(_FitSleepRaw other) => _FitSleepRaw(
        start: start ?? other.start,
        stop: stop ?? other.stop,
        levels: [...levels, ...other.levels],
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
    return FitSleepSession(
      start: sessionStart,
      end: sessionEnd,
      stages: stages,
    );
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
      decodedAnyFile = true;
      offset = result.nextOffset;
    }
    return _FitDecodeResult(
        points, summary, samples, sleep, hrv, monitoring);
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

  // HRV (file type 68): the last `hrv_status_summary.last_night_average` seen.
  DateTime? _hrvTime;
  double? _hrvRmssdMillis;

  // Monitoring (file type 32): the last one-per-file summary values seen.
  DateTime? _restingHrTime;
  int? _restingHrBpm;
  DateTime? _bmrTime;
  double? _bmrKcalPerDay;

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
      _FitSleepRaw(start: _sleepStart, stop: _sleepStop, levels: _sleepLevels),
      _FitHrvRaw(time: _hrvTime, rmssdMillis: _hrvRmssdMillis),
      _FitMonitoringRaw(
        restingHrTime: _restingHrTime,
        restingHrBpm: _restingHrBpm,
        bmrTime: _bmrTime,
        bmrKcalPerDay: _bmrKcalPerDay,
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
    final parsed = _fitParsedMessageNumbers.contains(definition.globalMessageNumber);
    for (final field in definition.fieldList) {
      final fieldBytes = reader.readBytes(field.size);
      if (field.number == _fitTimestampFieldNumber || parsed) {
        final longValue = _fitLong(fieldBytes, field, definition.littleEndian);
        if (longValue != null) values[field.number] = longValue;
        final stringValue = _fitString(fieldBytes, field);
        if (stringValue != null) strings[field.number] = stringValue;
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
        final rmr = values[_fitRestingMetabolicRateFieldNumber];
        if (rmr != null && rmr != _fitUint16Invalid && rmr > 0) {
          _bmrKcalPerDay = rmr.toDouble();
          if (messageTimestamp != null) {
            _bmrTime = _fitDateTimeInstant(messageTimestamp);
          }
        }
        break;
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

// Monitoring (Garmin file type 32). The high-frequency series (steps,
// respiration, per-sample HR) are deferred — they need downsampling and the
// foreground-service importer. These two are one-per-file summary values.
const int _fitMonitoringHrDataMessageNumber = 211;
const int _fitRestingHeartRateFieldNumber = 0;
const int _fitMonitoringInfoMessageNumber = 103;
const int _fitRestingMetabolicRateFieldNumber = 5;
const int _fitUint8Invalid = 0xFF;
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
};
