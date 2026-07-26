import 'package:shared_preferences/shared_preferences.dart';

import '../../core/time/local_date.dart';

/// SharedPreferences cache for the expensive 28-day Body Energy baselines,
/// keyed by date and a caller-supplied signature (permission fingerprint +
/// calibration signature + algorithm version).
///
/// The day *timelines* used to live here too, encoded as one delimited string
/// per day. They now live in drift (`BodyEnergyTimelineStore`) because the
/// chain has to read a previous day's end score cheaply and a multi-day view
/// needs a range query — neither of which a prefs blob can do. The baselines
/// stayed: they are five numbers a day with working adjacent-day reuse, and
/// moving them would add a table for no benefit.
class BodyEnergyBaselineCacheStore {
  BodyEnergyBaselineCacheStore(this._prefs);

  final SharedPreferences _prefs;

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

  /// One-shot removal of the retired timeline entries.
  ///
  /// The prefs store never evicted anything, so an install that has run since
  /// the feature shipped is carrying one ~15 KB string per (day × signature)
  /// for timelines that now live in drift. Matched narrowly: `2026-07-26|-123`
  /// is the only key shape the timeline half ever wrote, and the baseline keys
  /// carry a `baseline|` prefix so they cannot match. Flag-guarded so it runs
  /// once per install.
  Future<void> purgeLegacyTimelineEntries() async {
    if (_prefs.getBool(_purgedFlagKey) ?? false) return;
    final stale = _prefs
        .getKeys()
        .where((key) => _legacyTimelineKey.hasMatch(key))
        .toList();
    for (final key in stale) {
      await _prefs.remove(key);
    }
    await _prefs.setBool(_purgedFlagKey, true);
  }

  String _baselineCacheKey(LocalDate date, String signature) =>
      'baseline|$date|${signature.hashCode}';

  static final RegExp _legacyTimelineKey =
      RegExp(r'^\d{4}-\d{2}-\d{2}\|-?\d+$');
  static const String _purgedFlagKey = 'bodyEnergyPrefsTimelinePurged.v1';
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

String? _elementAt(List<String> list, int index) =>
    (index >= 0 && index < list.length) ? list[index] : null;

String _intCacheValue(int? value) => value?.toString() ?? '';

String _doubleCacheValue(double? value) => value?.toString() ?? '';

int? _intOrNullCache(String? value) =>
    (value == null || value.trim().isEmpty) ? null : int.tryParse(value);

double? _doubleOrNullCache(String? value) =>
    (value == null || value.trim().isEmpty) ? null : double.tryParse(value);
