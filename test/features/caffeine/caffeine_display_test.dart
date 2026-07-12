import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/features/caffeine/application/caffeine_display.dart';

/// The derivations the caffeine cards used to do in their build paths — the
/// sleep-impact verdict, the top six slices and their shared scale, the curve's
/// axis maximum — now a pure function the view-model calls once per load.
CaffeineDistributionSlice _slice(String label, double mg) =>
    CaffeineDistributionSlice(label: label, valueMg: mg);

void main() {
  final morning = DateTime(2026, 3, 2, 8);

  test('an empty load leaves the bars empty and the scale non-zero', () {
    final display = buildCaffeineDisplay(
      home: const CaffeineInsights(),
      analytics: const CaffeineInsights(),
    );

    expect(display.home.sleepImpactStatus, CaffeineSleepImpactStatus.unlikely);
    expect(display.home.curveEntryTimes, isEmpty);
    // The threshold line and the curve both have to fit in something.
    expect(display.home.curveMaxMg, 1.0);
    expect(display.analytics.sourceBars, isEmpty);
    expect(display.analytics.topSourceLabel, isNull);
  });

  test('the sleep verdict compares bedtime first, then right now', () {
    CaffeineSleepImpactStatus statusFor({
      required double currentMg,
      required double bedtimeMg,
    }) =>
        buildCaffeineDisplay(
          home: CaffeineInsights(
            currentMg: currentMg,
            bedtimeMg: bedtimeMg,
            sleepThresholdMg: 50,
            bedtime: const LocalTime(23, 0),
          ),
          analytics: const CaffeineInsights(),
        ).home.sleepImpactStatus;

    expect(statusFor(currentMg: 120, bedtimeMg: 80),
        CaffeineSleepImpactStatus.mayAffectSleep);
    // Over the line now, but back under it by bedtime.
    expect(statusFor(currentMg: 120, bedtimeMg: 30),
        CaffeineSleepImpactStatus.elevatedNow);
    expect(statusFor(currentMg: 20, bedtimeMg: 5),
        CaffeineSleepImpactStatus.unlikely);
  });

  test('the bedtime card is safe exactly at the threshold', () {
    final display = buildCaffeineDisplay(
      home: const CaffeineInsights(bedtimeMg: 50.0, sleepThresholdMg: 50),
      analytics: const CaffeineInsights(),
    );

    expect(display.home.bedtimeIsSafe, isTrue);
    // …and the verdict agrees: 50 is not ABOVE 50.
    expect(display.home.sleepImpactStatus, CaffeineSleepImpactStatus.unlikely);
  });

  test('the curve maximum fits the tallest point and the threshold', () {
    final display = buildCaffeineDisplay(
      home: CaffeineInsights(
        sleepThresholdMg: 50,
        curvePoints: [
          CaffeinePoint(time: morning, valueMg: 90.0),
          CaffeinePoint(
            time: morning.add(const Duration(hours: 2)),
            valueMg: 140.0,
          ),
        ],
      ),
      analytics: const CaffeineInsights(),
    );

    expect(display.home.curveMaxMg, 140.0);
  });

  test('a distribution card shows six bars, scaled against their own tallest',
      () {
    final display = buildCaffeineDisplay(
      home: const CaffeineInsights(),
      analytics: CaffeineInsights(
        sourceTotals: [
          _slice('Coffee', 400),
          _slice('Tea', 200),
          _slice('Cola', 100),
          _slice('Chocolate', 50),
          _slice('Energy drink', 40),
          _slice('Matcha', 30),
          // The seventh does not fit on the card.
          _slice('Yerba mate', 20),
        ],
        timeBuckets: const [
          CaffeineTimeBucket(
            bucket: CaffeineTimeOfDayBucket.morning,
            valueMg: 300,
          ),
          CaffeineTimeBucket(
            bucket: CaffeineTimeOfDayBucket.evening,
            valueMg: 150,
          ),
        ],
      ),
    );

    final bars = display.analytics.sourceBars;
    expect(bars.length, 6);
    expect(bars.first.label, 'Coffee');
    expect(bars.first.fraction, 1.0);
    expect(bars[1].fraction, 0.5);
    expect(display.analytics.topSourceLabel, 'Coffee');

    final buckets = display.analytics.timeBucketBars;
    expect(buckets.first.bucket, CaffeineTimeOfDayBucket.morning);
    expect(buckets.first.fraction, 1.0);
    expect(buckets.last.fraction, 0.5);
  });
}
