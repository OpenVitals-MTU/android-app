import 'dart:math' as math;

import 'package:shared_preferences/shared_preferences.dart';

import '../../core/time/local_date.dart';
import '../../domain/insights/body_energy_timeline.dart';

/// Versioned cache envelope for [BodyEnergyTimeline], keyed by date and a
/// caller-supplied signature (permission fingerprint + calibration signature +
/// algorithm version). Mirrors the Kotlin `BodyEnergyTimelineCacheStore`.
///
/// The Kotlin store lives in a dedicated `body_energy_timeline_cache`
/// SharedPreferences file; here it shares the app's [SharedPreferences] and
/// reproduces the same key strings (`"$date|$hash"`, `"baseline|$date|$hash"`)
/// and value encoding verbatim.
class BodyEnergyTimelineCacheStore {
  BodyEnergyTimelineCacheStore(this._prefs);

  final SharedPreferences _prefs;

  BodyEnergyTimeline? load(LocalDate date, String signature) {
    final encoded = _prefs.getString(_cacheKey(date, signature));
    if (encoded == null) return null;
    return _timelineOrNull(encoded, signature);
  }

  Future<void> save(BodyEnergyTimeline timeline) async {
    if (timeline.signature.trim().isEmpty) return;
    await _prefs.setString(
      _cacheKey(timeline.date, timeline.signature),
      _timelineToPreferenceString(timeline),
    );
  }

  BodyEnergyBaselineCacheEntry? loadBaseline(LocalDate date, String signature) {
    final encoded = _prefs.getString(_baselineCacheKey(date, signature));
    if (encoded == null) return null;
    return _baselineOrNull(encoded);
  }

  Future<void> saveBaseline(
    LocalDate date,
    String signature,
    BodyEnergyBaselineCacheEntry baseline,
  ) async {
    if (signature.trim().isEmpty) return;
    await _prefs.setString(
      _baselineCacheKey(date, signature),
      _baselineToPreferenceString(baseline),
    );
  }

  String _cacheKey(LocalDate date, String signature) =>
      '$date|${signature.hashCode}';

  String _baselineCacheKey(LocalDate date, String signature) =>
      'baseline|$date|${signature.hashCode}';
}

/// Cached day-boundary baselines used to seed the next day's timeline.
class BodyEnergyBaselineCacheEntry {
  BodyEnergyBaselineCacheEntry({
    required this.baselineRestingHeartRateBpm,
    required this.observedMaxHeartRateBpm,
    required this.hrvBaselineRmssdMs,
    required this.respiratoryRateBaseline,
    DateTime? generatedAt,
  }) : generatedAt = generatedAt ?? DateTime.now();

  final int? baselineRestingHeartRateBpm;
  final int? observedMaxHeartRateBpm;
  final double? hrvBaselineRmssdMs;
  final double? respiratoryRateBaseline;
  final DateTime generatedAt;
}

String _timelineToPreferenceString(BodyEnergyTimeline timeline) {
  final summary = timeline.inputSummary;
  final generatedAt = timeline.generatedAt ?? DateTime.now();
  final header = <Object?>[
    timeline.date.toString(),
    timeline.startScore,
    timeline.currentScore,
    timeline.charged,
    timeline.drained,
    timeline.confidence.storageName,
    generatedAt.millisecondsSinceEpoch,
    _escapeCacheField(timeline.confidenceReason),
    summary.algorithmVersion,
    summary.bucketMinutes,
    summary.heartRateSampleCount,
    summary.hrvSampleCount,
    summary.sleepSessionCount,
    summary.workoutCount,
    summary.respiratorySampleCount,
    summary.hasRestingHeartRate,
    summary.hasBaselineRestingHeartRate,
    summary.hasObservedMaxHeartRate,
    summary.hasHrvBaseline,
    summary.hasRespiratoryBaseline,
    _intCacheValue(summary.previousEndScore),
    summary.calibrationMode.storageName,
  ].join('|');
  final pointsValue = timeline.points.map((point) {
    return <Object?>[
      point.time.millisecondsSinceEpoch,
      point.score,
      point.delta.toStringAsFixed(4),
      point.state.storageName,
      point.confidence.storageName,
      point.charge.toStringAsFixed(4),
      point.intensityDrain.toStringAsFixed(4),
      point.stressDrain.toStringAsFixed(4),
      point.recoveryDebtDrain.toStringAsFixed(4),
      point.primaryInfluence.storageName,
    ].join(',');
  }).join(';');
  return '$header\n$pointsValue';
}

BodyEnergyTimeline? _timelineOrNull(String encoded, String signature) {
  try {
    final newlineIndex = encoded.indexOf('\n');
    final headerLine =
        newlineIndex < 0 ? encoded : encoded.substring(0, newlineIndex);
    final pointsLine =
        newlineIndex < 0 ? '' : encoded.substring(newlineIndex + 1);
    final header = headerLine.split('|');
    final points = pointsLine
        .split(';')
        .where((it) => it.trim().isNotEmpty)
        .map(_pointOrNull)
        .whereType<BodyEnergyTimelinePoint>()
        .toList();
    return BodyEnergyTimeline(
      date: _parseLocalDate(header[0]),
      startScore: int.parse(header[1]),
      currentScore: int.parse(header[2]),
      charged: int.parse(header[3]),
      drained: int.parse(header[4]),
      confidence: BodyEnergyConfidence.fromStorage(header[5]) ??
          (throw const FormatException('bad confidence')),
      generatedAt: DateTime.fromMillisecondsSinceEpoch(
        int.parse(header[6]),
        isUtc: true,
      ),
      confidenceReason: _unescapeCacheField(_elementAt(header, 7) ?? ''),
      inputSummary: _toInputSummary(header),
      points: points,
      signature: signature,
    );
  } catch (_) {
    return null;
  }
}

BodyEnergyTimelinePoint? _pointOrNull(String encoded) {
  final parts = encoded.split(',');
  if (parts.length < 5) return null;
  final state = BodyEnergyBucketState.fromStorage(parts[3]) ??
      (throw const FormatException('bad state'));
  final delta = double.parse(parts[2]);
  return BodyEnergyTimelinePoint(
    time: DateTime.fromMillisecondsSinceEpoch(int.parse(parts[0]), isUtc: true),
    score: int.parse(parts[1]),
    delta: delta,
    state: state,
    confidence: BodyEnergyConfidence.fromStorage(parts[4]) ??
        (throw const FormatException('bad confidence')),
    charge: _tryDouble(_elementAt(parts, 5)) ?? math.max(delta, 0.0),
    intensityDrain: _tryDouble(_elementAt(parts, 6)) ?? 0.0,
    stressDrain: _tryDouble(_elementAt(parts, 7)) ?? 0.0,
    recoveryDebtDrain: _tryDouble(_elementAt(parts, 8)) ?? 0.0,
    primaryInfluence: _primaryInfluenceOrNull(_elementAt(parts, 9)) ??
        _legacyPrimaryInfluence(state, delta),
  );
}

BodyEnergyInputSummary _toInputSummary(List<String> header) {
  return BodyEnergyInputSummary(
    algorithmVersion: _tryInt(_elementAt(header, 8)) ?? 1,
    bucketMinutes: _tryInt(_elementAt(header, 9)) ?? bodyEnergyTimelineBucketMinutes,
    heartRateSampleCount: _tryInt(_elementAt(header, 10)) ?? 0,
    hrvSampleCount: _tryInt(_elementAt(header, 11)) ?? 0,
    sleepSessionCount: _tryInt(_elementAt(header, 12)) ?? 0,
    workoutCount: _tryInt(_elementAt(header, 13)) ?? 0,
    respiratorySampleCount: _tryInt(_elementAt(header, 14)) ?? 0,
    hasRestingHeartRate: _parseBoolean(_elementAt(header, 15)),
    hasBaselineRestingHeartRate: _parseBoolean(_elementAt(header, 16)),
    hasObservedMaxHeartRate: _parseBoolean(_elementAt(header, 17)),
    hasHrvBaseline: _parseBoolean(_elementAt(header, 18)),
    hasRespiratoryBaseline: _parseBoolean(_elementAt(header, 19)),
    previousEndScore: _intOrNullCache(_elementAt(header, 20)),
    calibrationMode: _primaryCalibrationOrNull(_elementAt(header, 21)) ??
        BodyEnergyCalibrationMode.automatic,
  );
}

BodyEnergyPrimaryInfluence _legacyPrimaryInfluence(
  BodyEnergyBucketState state,
  double delta,
) {
  if (state == BodyEnergyBucketState.unmeasurable) {
    return BodyEnergyPrimaryInfluence.noData;
  }
  if (delta > 0.0 && state == BodyEnergyBucketState.sleep) {
    return BodyEnergyPrimaryInfluence.sleepRecovery;
  }
  if (delta > 0.0) return BodyEnergyPrimaryInfluence.quietRest;
  if (state == BodyEnergyBucketState.activity) {
    return BodyEnergyPrimaryInfluence.exertion;
  }
  if (state == BodyEnergyBucketState.stress) {
    return BodyEnergyPrimaryInfluence.elevatedHeartRate;
  }
  return BodyEnergyPrimaryInfluence.steady;
}

String _baselineToPreferenceString(BodyEnergyBaselineCacheEntry baseline) => <
    Object?>[
  _intCacheValue(baseline.baselineRestingHeartRateBpm),
  _intCacheValue(baseline.observedMaxHeartRateBpm),
  _doubleCacheValue(baseline.hrvBaselineRmssdMs),
  _doubleCacheValue(baseline.respiratoryRateBaseline),
  baseline.generatedAt.millisecondsSinceEpoch.toString(),
].join('|');

BodyEnergyBaselineCacheEntry? _baselineOrNull(String encoded) {
  try {
    final parts = encoded.split('|');
    final generatedRaw = _elementAt(parts, 4);
    return BodyEnergyBaselineCacheEntry(
      baselineRestingHeartRateBpm: _intOrNullCache(_elementAt(parts, 0)),
      observedMaxHeartRateBpm: _intOrNullCache(_elementAt(parts, 1)),
      hrvBaselineRmssdMs: _doubleOrNullCache(_elementAt(parts, 2)),
      respiratoryRateBaseline: _doubleOrNullCache(_elementAt(parts, 3)),
      generatedAt: DateTime.fromMillisecondsSinceEpoch(
        generatedRaw == null ? 0 : int.parse(generatedRaw),
        isUtc: true,
      ),
    );
  } catch (_) {
    return null;
  }
}

BodyEnergyPrimaryInfluence? _primaryInfluenceOrNull(String? value) =>
    value == null ? null : BodyEnergyPrimaryInfluence.fromStorage(value);

BodyEnergyCalibrationMode? _primaryCalibrationOrNull(String? value) =>
    value == null ? null : BodyEnergyCalibrationMode.fromStorage(value);

String? _elementAt(List<String> list, int index) =>
    (index >= 0 && index < list.length) ? list[index] : null;

String _intCacheValue(int? value) => value?.toString() ?? '';

String _doubleCacheValue(double? value) => value?.toString() ?? '';

int? _intOrNullCache(String? value) =>
    (value == null || value.trim().isEmpty) ? null : int.tryParse(value);

int? _tryInt(String? value) => value == null ? null : int.tryParse(value);

double? _tryDouble(String? value) => value == null ? null : double.tryParse(value);

double? _doubleOrNullCache(String? value) =>
    (value == null || value.trim().isEmpty) ? null : double.tryParse(value);

bool _parseBoolean(String? value) => value?.toLowerCase() == 'true';

String _escapeCacheField(String value) => value
    .replaceAll('\\', '\\\\')
    .replaceAll('|', '\\p')
    .replaceAll('\n', '\\n');

String _unescapeCacheField(String value) => value
    .replaceAll('\\n', '\n')
    .replaceAll('\\p', '|')
    .replaceAll('\\\\', '\\');

LocalDate _parseLocalDate(String value) {
  final parts = value.split('-');
  if (parts.length != 3) {
    throw FormatException('Invalid LocalDate: $value');
  }
  return LocalDate(
    int.parse(parts[0]),
    int.parse(parts[1]),
    int.parse(parts[2]),
  );
}
