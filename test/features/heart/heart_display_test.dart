import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/model/vitals_models.dart';
import 'package:openvitals/domain/usecase/load_heart_period_use_case.dart';
import 'package:openvitals/features/heart/application/heart_display.dart';
import 'package:openvitals/features/heart/presentation/heart_metric_cards.dart';

/// The screen used to sort and `reduce` these lists on every rebuild. Now they
/// are derived once, by a pure function — which is a function you can call from
/// a unit test with a fixture, without a widget tree, a clock or a repository.

RestingHeartRateSample _restingSample(int hour, int bpm) =>
    RestingHeartRateSample(
      time: DateTime.utc(2026, 3, 2, hour),
      beatsPerMinute: bpm,
      source: 'Test',
    );

HeartRateSample _sample(int hour, int bpm) => HeartRateSample(
      time: DateTime.utc(2026, 3, 2, hour),
      beatsPerMinute: bpm,
      source: 'Test',
    );

HeartRateSummary _summary(LocalDate date, int avg, int min, int max) =>
    HeartRateSummary(date: date, avgBpm: avg, minBpm: min, maxBpm: max);

SpO2Entry _spO2(int hour, double percent) => SpO2Entry(
      time: DateTime.utc(2026, 3, 2, hour),
      percent: percent,
      source: 'Test',
    );

BloodPressureEntry _bp(int hour, int systolic, int diastolic) =>
    BloodPressureEntry(
      time: DateTime.utc(2026, 3, 2, hour),
      systolicMmHg: systolic,
      diastolicMmHg: diastolic,
      source: 'Test',
    );

SkinTemperatureEntry _skin(int hour, double? delta) => SkinTemperatureEntry(
      startTime: DateTime.utc(2026, 3, 2, hour),
      endTime: DateTime.utc(2026, 3, 2, hour),
      baselineCelsius: 33.0,
      averageDeltaCelsius: delta,
      minDeltaCelsius: delta,
      maxDeltaCelsius: delta,
      measurementLocation: 0,
      source: 'Test',
    );

HeartDisplay _display(
  HeartPeriodLoadResult result, {
  TimeRange selectedRange = TimeRange.week,
  int high = 120,
  int low = 50,
}) =>
    buildHeartDisplay(
      result,
      selectedRange: selectedRange,
      highHeartRateThresholdBpm: high,
      lowHeartRateThresholdBpm: low,
    );

void main() {
  const monday = LocalDate(2026, 3, 2);
  const tuesday = LocalDate(2026, 3, 3);

  group('heart rate', () {
    test('a day of samples sorts oldest first and takes its extremes', () {
      final display = _display(
        HeartPeriodLoadResult(
          daySamples: [_sample(14, 90), _sample(6, 52), _sample(9, 71)],
        ),
        selectedRange: TimeRange.day,
      );

      final day = display.heartRateDay!;
      expect([for (final s in day.samples) s.beatsPerMinute], [52, 71, 90]);
      expect(day.minBpm, 52);
      expect(day.maxBpm, 90);
      expect(day.averageBpm, 71);
      expect(day.stats.readings, 3);
      // The intraday axis is floored at 30 and padded by 5 either side.
      expect(day.chartMinValue, 47.0);
      expect(day.chartMaxValue, 95.0);
    });

    test('a period keeps both orders: oldest first, and newest first', () {
      final display = _display(HeartPeriodLoadResult(
        dailySummaries: [
          _summary(tuesday, 72, 60, 130),
          _summary(monday, 68, 58, 120),
        ],
        previousDailySummaries: [_summary(monday, 60, 50, 100)],
      ));

      final period = display.heartRatePeriod!;
      expect([for (final s in period.summaries) s.date], [monday, tuesday]);
      expect(
        [for (final s in period.summariesNewestFirst) s.date],
        [tuesday, monday],
      );
      expect(period.averageBpm, 70);
      expect(period.lowestBpm, 58);
      expect(period.highestBpm, 130);
      expect(period.stats.comparison!.previousValue, 60);
      expect(period.stats.readings, 2);
    });

    test('the threshold checks count the days that crossed the line', () {
      final display = _display(
        HeartPeriodLoadResult(
          dailySummaries: [
            _summary(monday, 68, 58, 120),
            _summary(tuesday, 72, 44, 100),
          ],
        ),
        high: 120,
        low: 45,
      );

      expect(display.highHeartRateCheck.count, 1);
      expect(display.highHeartRateCheck.hasData, isTrue);
      expect(display.lowHeartRateCheck.count, 1);
      expect(display.lowHeartRateCheck.type, HeartRateThresholdCheckType.low);
    });
  });

  group('resting heart rate', () {
    test('the day falls back to the mean of its samples with no aggregate', () {
      final display = _display(
        HeartPeriodLoadResult(
          dayRestingSamples: [
            RestingHeartRateSample(
              time: DateTime.utc(2026, 3, 2, 8),
              beatsPerMinute: 60,
              source: 'Test',
            ),
            RestingHeartRateSample(
              time: DateTime.utc(2026, 3, 2, 3),
              beatsPerMinute: 50,
              source: 'Test',
            ),
          ],
        ),
        selectedRange: TimeRange.day,
      );

      final day = display.restingHeartRateDay!;
      expect(day.restingBpm, 55);
      expect(day.lowBpm, 50);
      expect(day.highBpm, 60);
      expect([for (final s in day.samples) s.beatsPerMinute], [50, 60]);
    });

    test('an aggregate with no samples still yields one reading', () {
      final display = _display(
        const HeartPeriodLoadResult(dayRestingBpm: 58),
        selectedRange: TimeRange.day,
      );

      final day = display.restingHeartRateDay!;
      expect(day.samples, isEmpty);
      // The low and the high collapse onto the single aggregate value.
      expect(day.restingBpm, 58);
      expect(day.lowBpm, 58);
      expect(day.highBpm, 58);
      expect(day.stats.readings, 1);
    });
  });

  group('vitals', () {
    test('blood pressure keeps the latest reading and the highest one', () {
      final display = _display(HeartPeriodLoadResult(
        bloodPressure: [_bp(9, 140, 90), _bp(18, 120, 80), _bp(7, 118, 76)],
      ));

      final bp = display.bloodPressure!;
      expect(bp.entries.first.systolicMmHg, 118);
      expect(bp.latest.systolicMmHg, 120);
      expect(bp.stats.latest!.systolicMmHg, 120);
      expect(bp.stats.highest!.systolicMmHg, 140);
      expect(bp.stats.averageSystolic, closeTo(126, 0.5));
      expect(bp.stats.readings, 3);
    });

    test('SpO2 averages every reading and sorts the series', () {
      final display = _display(HeartPeriodLoadResult(
        spO2: [_spO2(20, 95), _spO2(2, 99)],
        previousSpO2: [_spO2(2, 97)],
      ));

      final spO2 = display.spO2!;
      expect([for (final e in spO2.entries) e.percent], [99.0, 95.0]);
      expect(spO2.latest.percent, 95.0);
      expect(spO2.averagePercent, 97.0);
      expect(spO2.stats.low, 95.0);
      expect(spO2.stats.high, 99.0);
      expect(spO2.stats.comparison!.currentValue, 97.0);
    });

    test('skin temperature excludes deltaless entries from the arithmetic', () {
      final display = _display(HeartPeriodLoadResult(
        skinTemperature: [_skin(8, 0.4), _skin(20, null), _skin(2, -0.2)],
      ));

      final skin = display.skinTemperature!;
      expect(skin.entries.length, 3);
      expect(skin.chartEntries.length, 2);
      expect(skin.averageDeltaCelsius, closeTo(0.1, 0.0001));
      // …but they still count as readings, which is what the screen printed.
      expect(skin.stats!.readings, 3);
      expect(skin.stats!.low, -0.2);
      expect(skin.stats!.high, 0.4);
    });

    test('skin temperature with no delta anywhere has no statistics at all', () {
      final display = _display(HeartPeriodLoadResult(
        skinTemperature: [_skin(8, null)],
      ));

      final skin = display.skinTemperature!;
      expect(skin.chartEntries, isEmpty);
      expect(skin.averageDeltaCelsius, isNull);
      expect(skin.stats, isNull);
    });
  });

  test('an empty period leaves every section null', () {
    final display = _display(const HeartPeriodLoadResult());

    expect(display.heartRateDay, isNull);
    expect(display.heartRatePeriod, isNull);
    expect(display.restingHeartRateDay, isNull);
    expect(display.restingHeartRatePeriod, isNull);
    expect(display.hrvDay, isNull);
    expect(display.hrvPeriod, isNull);
    expect(display.bloodPressure, isNull);
    expect(display.spO2, isNull);
    expect(display.vo2Max, isNull);
    expect(display.respiratoryRate, isNull);
    expect(display.bodyTemperature, isNull);
    expect(display.bloodGlucose, isNull);
    expect(display.skinTemperature, isNull);
    // The checks still exist — with nothing to check.
    expect(display.highHeartRateCheck.hasData, isFalse);
    expect(display.highHeartRateCheck.count, 0);
  });

  group('a day average never sits outside its own range', () {
    // The average used to come from the provider's day aggregate while the low
    // and the high came from the samples we had read — two different
    // populations, printed side by side as if they were one. So the card could
    // say "avg 70, low 50, high 60".
    test('resting heart rate averages the samples it also ranges', () {
      final display = _display(HeartPeriodLoadResult(
        dayRestingSamples: [
          _restingSample(8, 50),
          _restingSample(9, 60),
        ],
        // The provider disagrees with its own samples. It loses.
        dayRestingBpm: 70,
      ));

      final day = display.restingHeartRateDay!;
      expect(day.stats.average, 55);
      expect(day.stats.low, 50);
      expect(day.stats.high, 60);
      expect(day.stats.average, inInclusiveRange(day.stats.low, day.stats.high));
    });

    test('with no samples the provider aggregate is all there is', () {
      final display = _display(const HeartPeriodLoadResult(dayRestingBpm: 70));

      final day = display.restingHeartRateDay!;
      expect(day.stats.average, 70);
      expect(day.stats.low, 70);
      expect(day.stats.high, 70);
    });
  });

  test('respiratory rate reports ONE average, the one under its chart', () {
    // Two readings on day one, one on day two. The mean of the daily means (12)
    // and the mean of every reading (11.33) are different numbers, and the
    // screen used to print both — the chart summary said one, the
    // interpretation card said the other, and neither said which.
    // Local times, not UTC: the display groups readings by LOCAL day, and a
    // UTC fixture straddles midnight somewhere on the planet (at UTC+14 these
    // three regrouped as [10] and [12, 13] and the average came out 11.25).
    final display = _display(HeartPeriodLoadResult(
      respiratoryRate: [
        RespiratoryRateEntry(
          time: DateTime(2026, 3, 2, 8),
          breathsPerMinute: 10,
          source: 'Test',
        ),
        RespiratoryRateEntry(
          time: DateTime(2026, 3, 2, 20),
          breathsPerMinute: 12,
          source: 'Test',
        ),
        RespiratoryRateEntry(
          time: DateTime(2026, 3, 3, 8),
          breathsPerMinute: 13,
          source: 'Test',
        ),
      ],
    ));

    // (11 + 13) / 2 — the mean of the two days, not of the three readings.
    expect(display.respiratoryRate!.periodAverage, closeTo(12.0, 1e-9));
  });
}