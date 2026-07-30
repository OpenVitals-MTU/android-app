import 'package:shared_preferences/shared_preferences.dart';

import 'wellness/fit_wellness_import.dart';

/// How far each day's Garmin monitoring counters have already been imported.
///
/// The watch's step, distance and active-calorie counters run cumulatively from
/// local midnight, and every sync brings only the minutes since the last one. To
/// write those minutes as INTRADAY records — rather than one flat total per day —
/// each interval has to be differenced against the reading before it, and for
/// the first reading in a sync that predecessor is in a file this run does not
/// have. It was archived on the watch two syncs ago.
///
/// So the last reading imported for a day is remembered here, and the next sync
/// differences from it. That is what keeps the day's total exact across any
/// number of syncs, and what makes re-importing a file already behind the
/// watermark write nothing instead of counting it twice.
///
/// SharedPreferences-backed and fire-and-forget, like [GarminDeviceStateStore].
/// Keyed by local day and NOT by device: the counters belong to the wearer's
/// day, and a second watch reporting the same day's steps would be describing
/// the same walk.
class GarminCounterWatermarkStore {
  GarminCounterWatermarkStore(this._prefs);

  final SharedPreferences _prefs;

  static const String _prefsKey = 'garmin_counter_watermarks';

  /// Days kept. Long enough to cover a watch left in a drawer over a holiday and
  /// synced on return; short enough that the list cannot grow without bound.
  static const int _retainedDays = 60;

  Map<String, FitCounterWatermark> load() {
    final raw = _prefs.getStringList(_prefsKey);
    if (raw == null) return const {};
    final marks = <String, FitCounterWatermark>{};
    for (final line in raw) {
      final parts = line.split('|');
      // Anything unreadable is DROPPED, not guessed at. A watermark is a claim
      // about what Health Connect already holds; half of one would either lose a
      // day's steps or write them twice, and re-importing from the day's start
      // is the safer of the two mistakes.
      //
      // Five/six fields are the older forms, kept readable rather than
      // dropped. Five predates [FitCounterWatermark.legacyRetired]; both
      // predate the per-type maps, which load as null so the mapper adopts
      // their types silently instead of re-counting them.
      if (parts.length != 5 && parts.length != 6 && parts.length != 9) {
        continue;
      }
      final timeMs = int.tryParse(parts[1]);
      final steps = int.tryParse(parts[2]);
      final distance = int.tryParse(parts[3]);
      final calories = int.tryParse(parts[4]);
      if (timeMs == null ||
          steps == null ||
          distance == null ||
          calories == null) {
        continue;
      }
      var readable = true;
      Map<int, int>? decodeTypes(String raw) {
        if (raw == '-') return null;
        final types = <int, int>{};
        if (raw.isEmpty) return types;
        for (final pair in raw.split(',')) {
          final colon = pair.indexOf(':');
          final type = colon < 0 ? null : int.tryParse(pair.substring(0, colon));
          final value =
              colon < 0 ? null : int.tryParse(pair.substring(colon + 1));
          if (type == null || value == null) {
            readable = false;
            return null;
          }
          types[type] = value;
        }
        return types;
      }

      final stepsByType = parts.length == 9 ? decodeTypes(parts[6]) : null;
      final distanceByType = parts.length == 9 ? decodeTypes(parts[7]) : null;
      final caloriesByType = parts.length == 9 ? decodeTypes(parts[8]) : null;
      if (!readable) continue;
      marks[parts[0]] = FitCounterWatermark(
        time: DateTime.fromMillisecondsSinceEpoch(timeMs),
        steps: steps,
        distance: distance,
        calories: calories,
        stepsByType: stepsByType,
        distanceByType: distanceByType,
        caloriesByType: caloriesByType,
        legacyRetired: parts.length >= 6 && parts[5] == '1',
      );
    }
    return marks;
  }

  /// Merges [marks] over what is stored and prunes to [_retainedDays].
  ///
  /// Merged rather than replaced: one sync touches the days its files covered,
  /// and must not forget the others.
  Future<void> save(Map<String, FitCounterWatermark> marks) async {
    if (marks.isEmpty) return;
    final merged = <String, FitCounterWatermark>{...load(), ...marks};
    final days = merged.keys.toList()..sort();
    final kept =
        days.length > _retainedDays ? days.sublist(days.length - _retainedDays) : days;
    // '-' keeps a legacy mark's null maps null across a re-save: an empty map
    // means "no types", null means "types unknowable", and flattening the two
    // would turn silent adoption into a full re-count.
    String encodeTypes(Map<int, int>? types) => types == null
        ? '-'
        : [for (final e in types.entries) '${e.key}:${e.value}'].join(',');
    await _prefs.setStringList(_prefsKey, [
      for (final day in kept)
        '$day|${merged[day]!.time.millisecondsSinceEpoch}'
            '|${merged[day]!.steps}|${merged[day]!.distance}'
            '|${merged[day]!.calories}'
            '|${merged[day]!.legacyRetired ? 1 : 0}'
            '|${encodeTypes(merged[day]!.stepsByType)}'
            '|${encodeTypes(merged[day]!.distanceByType)}'
            '|${encodeTypes(merged[day]!.caloriesByType)}',
    ]);
  }

  /// Forgets every watermark, so the next import writes each day from its start.
  ///
  /// For a Health Connect wipe: the records the watermarks describe are gone, so
  /// the watermarks are lies, and a sync that trusted them would write only the
  /// minutes since — leaving the day short forever.
  Future<void> clear() => _prefs.remove(_prefsKey);
}
