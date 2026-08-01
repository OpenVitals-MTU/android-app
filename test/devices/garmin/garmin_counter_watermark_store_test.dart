import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/devices/garmin/garmin_counter_watermark_store.dart';
import 'package:openvitals/devices/garmin/wellness/fit_wellness_import.dart';

/// The watermark is what lets a day's counters be written as intraday intervals
/// across many syncs: each one differences from where the last stopped. It has
/// to survive a restart, and it has to be right — a wrong watermark either loses
/// a stretch of the day or writes it twice.
void main() {
  late SharedPreferences prefs;
  late GarminCounterWatermarkStore store;

  Future<void> setUpStore() async {
    SharedPreferences.setMockInitialValues(const {});
    prefs = await SharedPreferences.getInstance();
    store = GarminCounterWatermarkStore(prefs);
  }

  FitCounterWatermark mark(DateTime time, int steps) =>
      FitCounterWatermark(time: time, steps: steps, distance: 10, calories: 5);

  test('starts empty and round-trips through storage', () async {
    await setUpStore();
    expect(store.load(), isEmpty);

    final at = DateTime(2026, 7, 25, 20, 24);
    await store.save({'2026-07-25': mark(at, 24843)});

    // A second store over the same prefs is the real round-trip.
    final reloaded = GarminCounterWatermarkStore(prefs).load();
    expect(reloaded['2026-07-25']!.time, at);
    expect(reloaded['2026-07-25']!.steps, 24843);
    expect(reloaded['2026-07-25']!.distance, 10);
    expect(reloaded['2026-07-25']!.calories, 5);
  });

  test('a later sync of the same day moves the watermark forward', () async {
    await setUpStore();
    await store.save({'2026-07-25': mark(DateTime(2026, 7, 25, 20), 24000)});
    await store.save({'2026-07-25': mark(DateTime(2026, 7, 25, 21), 24843)});

    expect(store.load()['2026-07-25']!.steps, 24843);
  });

  test('saving one day does not forget the others', () async {
    // A sync touches the days its files covered. The rest are still true.
    await setUpStore();
    await store.save({
      '2026-07-24': mark(DateTime(2026, 7, 24, 23), 9000),
      '2026-07-25': mark(DateTime(2026, 7, 25, 20), 24000),
    });
    await store.save({'2026-07-25': mark(DateTime(2026, 7, 25, 21), 24843)});

    final marks = store.load();
    expect(marks.keys, containsAll(['2026-07-24', '2026-07-25']));
    expect(marks['2026-07-24']!.steps, 9000);
  });

  test('keeps the most recent days and drops the oldest', () async {
    await setUpStore();
    await store.save({
      for (var day = 1; day <= 70; day++)
        '2026-05-${day.toString().padLeft(2, '0')}':
            mark(DateTime(2026, 5, 1), day),
    });

    final marks = store.load();
    expect(marks, hasLength(60));
    // Sorted by day key, so what goes is the oldest.
    expect(marks.containsKey('2026-05-01'), isFalse);
    expect(marks.containsKey('2026-05-70'), isTrue);
  });

  test('an unreadable entry is dropped rather than guessed at', () async {
    // Half a watermark would either lose a day's steps or write them twice.
    // Re-importing the day from its start is the safer of the two mistakes.
    SharedPreferences.setMockInitialValues(const {
      'garmin_counter_watermarks': <String>[
        '2026-07-25|not-a-number|1|2|3',
        '2026-07-24|too|few',
        '2026-07-23|1784000000000|100|200|300',
      ],
    });
    prefs = await SharedPreferences.getInstance();

    final marks = GarminCounterWatermarkStore(prefs).load();
    expect(marks.keys, ['2026-07-23']);
    expect(marks['2026-07-23']!.steps, 100);
  });

  test('a watermark written before the legacy flag reads as not retired',
      () async {
    // Five fields is the pre-legacyRetired form, and dropping those lines would
    // re-import each day from midnight. Reading them as NOT retired is both
    // lossless and correct: a day written under the old form still has its
    // whole-day record, and is exactly the day whose next sync must supersede
    // it.
    SharedPreferences.setMockInitialValues(const {
      'garmin_counter_watermarks': <String>[
        '2026-07-25|1784000000000|100|200|300',
        '2026-07-24|1784000000000|100|200|300|1',
      ],
    });
    prefs = await SharedPreferences.getInstance();

    final marks = GarminCounterWatermarkStore(prefs).load();
    expect(marks['2026-07-25']!.legacyRetired, isFalse);
    expect(marks['2026-07-25']!.steps, 100);
    expect(marks['2026-07-24']!.legacyRetired, isTrue);
  });

  test('the legacy flag survives a save and reload', () async {
    await setUpStore();
    await store.save({
      '2026-07-25': FitCounterWatermark(
        time: DateTime(2026, 7, 25, 20),
        steps: 24843,
        legacyRetired: true,
      ),
    });

    expect(store.load()['2026-07-25']!.legacyRetired, isTrue);
  });

  test('the per-type maps survive a round trip', () async {
    await setUpStore();
    await store.save({
      '2026-07-30': FitCounterWatermark(
        time: DateTime(2026, 7, 30, 15),
        steps: 3400,
        distance: 250000,
        calories: 120,
        stepsByType: const {0: 400, 6: 3000},
        distanceByType: const {6: 250000},
        caloriesByType: const {},
      ),
    });

    final mark = GarminCounterWatermarkStore(prefs).load()['2026-07-30']!;
    expect(mark.stepsByType, {0: 400, 6: 3000});
    expect(mark.distanceByType, {6: 250000});
    expect(mark.caloriesByType, isEmpty);
  });

  test('the open-bucket seed values survive a round trip', () async {
    await setUpStore();
    await store.save({
      '2026-07-31': FitCounterWatermark(
        time: DateTime(2026, 7, 31, 9, 20),
        steps: 3400,
        stepsByType: const {6: 3400},
        openBucketSteps: 120,
        openBucketDistance: 9500,
        openBucketCalories: 8,
      ),
    });

    final mark = GarminCounterWatermarkStore(prefs).load()['2026-07-31']!;
    expect(mark.openBucketSteps, 120);
    expect(mark.openBucketDistance, 9500);
    expect(mark.openBucketCalories, 8);
  });

  test('a line from before the open-bucket seeds loads them as zero', () async {
    // Correct, not merely tolerated: those versions withheld the open bucket,
    // so there is nothing already written for the seed to restate.
    SharedPreferences.setMockInitialValues(const {
      'garmin_counter_watermarks': <String>[
        '2026-07-30|1753822800000|3400|0|0|1|6:3400|-|-',
      ],
    });
    prefs = await SharedPreferences.getInstance();
    store = GarminCounterWatermarkStore(prefs);

    final mark = store.load()['2026-07-30']!;
    expect(mark.stepsByType, {6: 3400});
    expect(mark.openBucketSteps, 0);
    expect(mark.openBucketDistance, 0);
    expect(mark.openBucketCalories, 0);
  });

  test('a line from before the maps loads with them null, and stays null',
      () async {
    // Null is "types unknowable", an empty map is "no types" — the mapper
    // adopts silently on the first and counts fully on the second, so a
    // re-save must not flatten one into the other.
    SharedPreferences.setMockInitialValues(const {
      'garmin_counter_watermarks': <String>['2026-07-29|1753822800000|6123|0|0|1'],
    });
    prefs = await SharedPreferences.getInstance();
    store = GarminCounterWatermarkStore(prefs);

    expect(store.load()['2026-07-29']!.stepsByType, isNull);

    await store.save({'2026-07-30': mark(DateTime(2026, 7, 30, 9), 100)});
    expect(store.load()['2026-07-29']!.stepsByType, isNull);
    expect(store.load()['2026-07-29']!.steps, 6123);
  });

  test('an unreadable type map drops the line, not just the map', () async {
    SharedPreferences.setMockInitialValues(const {
      'garmin_counter_watermarks': <String>[
        '2026-07-30|1753822800000|100|0|0|0|not-a-map|-|-',
      ],
    });
    prefs = await SharedPreferences.getInstance();
    store = GarminCounterWatermarkStore(prefs);

    expect(store.load(), isEmpty);
  });

  test('clear forgets everything', () async {
    // For a Health Connect wipe: the records the watermarks describe are gone,
    // so trusting them would leave every day short forever.
    await setUpStore();
    await store.save({'2026-07-25': mark(DateTime(2026, 7, 25, 20), 24843)});

    await store.clear();

    expect(store.load(), isEmpty);
  });
}
