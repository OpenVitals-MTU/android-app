import 'package:flutter/foundation.dart'; // DIAGNOSTIC: debugPrint to logcat (also re-exports Uint8List)

import '../../../../core/fit/fit_message.dart';
import '../../../../core/fit/fit_reader.dart';
import '../../../../domain/model/activity_models.dart';
import '../../../../domain/model/ble_sensor_models.dart';
import 'route_file_parser.dart';

/// Decodes the **activity** data a FIT file carries (route points, session
/// summary, and the per-record HR/speed/cadence series) into a [RouteFileImport].
/// This is the general FIT decoder shared with GPX/KML/TCX manual import via
/// `route_file_parser.dart`; the Garmin-proprietary wellness interpretation
/// lives in `devices/garmin/wellness/garmin_fit_wellness.dart`. Both consume the
/// generic [FitReader]. Pure Dart, no plugins — the unit tests exercise
/// hand-crafted FIT byte streams whose exact framing must be honoured.
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

/// One decode pass's activity carriers: route points, the session/lap/record
/// summary, and the per-record sample series. [_FitDecoder] merges them across
/// a chained stream.
class _FitActivityDecode {
  const _FitActivityDecode(this.points, this.summary, this.samples);

  final List<ExerciseRoutePoint> points;
  final _FitActivitySummary summary;
  final _FitSamples samples;
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

class _FitDecoder {
  _FitDecoder(this.fileBytes);

  final Uint8List fileBytes;

  _FitActivityDecode decode() {
    final points = <ExerciseRoutePoint>[];
    var summary = const _FitActivitySummary();
    var samples = const _FitSamples.empty();
    var offset = 0;
    var decodedAnyFile = false;

    while (offset < fileBytes.length) {
      if (!FitReader.isFitFileAt(fileBytes, offset)) {
        if (!decodedAnyFile) {
          throw const FitFormatException('FIT file header is invalid.');
        }
        break;
      }
      // Read then interpret each file on its own, so the per-file merge below is
      // preserved: a chained stream's later files fall back to — rather than
      // concatenate with — an earlier file's one-per-file scalar fields.
      final (messages, next) = FitReader.readFile(fileBytes, offset);
      final result = _FitInterpreter().interpret(messages);
      points.addAll(result.points);
      summary = summary.merge(result.summary);
      samples = samples.merge(result.samples);
      decodedAnyFile = true;
      offset = next;
    }
    return _FitActivityDecode(points, summary, samples);
  }
}

/// Interprets one file's decoded [FitMessage]s into the activity raw structs.
/// One per file; [_FitDecoder] merges them across a chained stream. Its switch
/// cases are disjoint from the Garmin wellness interpreter's, so a wellness file
/// simply yields no points, an empty summary and no samples here.
class _FitInterpreter {
  final List<ExerciseRoutePoint> _points = [];
  int? _fileType;
  String? _metadataName;
  int? _sport;
  int? _subSport;
  DateTime? _firstRecordTime;
  DateTime? _lastRecordTime;
  _FitActivitySummary _sessionSummary = const _FitActivitySummary();
  _FitActivitySummary _lapSummary = const _FitActivitySummary();
  int? _workoutDurationSeconds;
  int _courseRecordIndex = 0;

  _FitActivityDecode interpret(List<FitMessage> messages) {
    // Every accumulator is filled by dispatching the messages in file order, so
    // cases that depend on an earlier message (file_id before record) still see it.
    for (final message in messages) {
      _dispatch(message);
    }
    return _FitActivityDecode(_points, _fitSummary(), samples);
  }

  /// Interprets one decoded message into the accumulators. Locals are bound to
  /// the message's fields so the switch below is exactly the code that used to
  /// run inline in the walk.
  void _dispatch(FitMessage message) {
    final values = message.values;
    final strings = message.strings;
    final messageTimestamp = message.timestamp;
    switch (message.globalMessageNumber) {
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
    final time = fitDateTimeInstant(timestampRaw);
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
        ? fitDateTimeInstant(timestampRaw)
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
    _addRecordPoint(values, fitDateTimeInstant(timestampRaw));
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


  _FitActivitySummary _toFitActivitySummary(Map<int, int> values, int? timestampRaw) {
    final startRaw = values[_fitStartTimeFieldNumber];
    final startTime = startRaw == null ? null : fitDateTimeInstant(startRaw);
    final durationRaw = values[_fitTotalElapsedTimeFieldNumber] ??
        values[_fitTotalTimerTimeFieldNumber];
    final durationSeconds = durationRaw == null ? null : durationRaw / _fitTimeScale;
    DateTime? endTime;
    if (startTime != null && durationSeconds != null && durationSeconds > 0.0) {
      endTime = startTime.add(
        Duration(milliseconds: (durationSeconds * 1000.0).round()),
      );
    } else if (timestampRaw != null) {
      endTime = fitDateTimeInstant(timestampRaw);
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

bool isFitFile(Uint8List bytes) => FitReader.isFitFileAt(bytes, 0);

int? _generic(int? value) =>
    (value == null || value == _fitSportGeneric) ? null : value;

double _fitSemicirclesToDegrees(int value) =>
    value.toDouble() * 180.0 / _fitSemicircleDegreesDivisor;

double _fitAltitudeMeters(int value) =>
    value.toDouble() / _fitAltitudeScale - _fitAltitudeOffsetMeters;

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

const int _fitFileIdMessageNumber = 0;
const int _fitFileIdTypeFieldNumber = 0;
const int _fitFileTypeWorkout = 5;
const int _fitFileTypeCourse = 6;
const int _fitRecordMessageNumber = 20;
const int _fitLapMessageNumber = 19;
const int _fitSessionMessageNumber = 18;
const int _fitCourseMessageNumber = 31;
const int _fitCourseSportFieldNumber = 4;
const int _fitCourseNameFieldNumber = 5;
const int _fitWorkoutMessageNumber = 26;
const int _fitWorkoutSportFieldNumber = 4;
const int _fitWorkoutNameFieldNumber = 8;
const int _fitWorkoutStepMessageNumber = 27;
const int _fitWorkoutStepDurationTypeFieldNumber = 1;
const int _fitWorkoutStepDurationValueFieldNumber = 2;
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
