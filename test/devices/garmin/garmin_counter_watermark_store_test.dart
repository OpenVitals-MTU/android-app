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

  test('clear forgets everything', () async {
    // For a Health Connect wipe: the records the watermarks describe are gone,
    // so trusting them would leave every day short forever.
    await setUpStore();
    await store.save({'2026-07-25': mark(DateTime(2026, 7, 25, 20), 24843)});

    await store.clear();

    expect(store.load(), isEmpty);
  });
}
