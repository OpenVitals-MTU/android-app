import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/model/vitals_models.dart';
import 'package:openvitals/domain/usecase/load_heart_period_use_case.dart';
import 'package:openvitals/features/vitals/application/heart_vitals_overview_display.dart';

/// The overview screen used to sort, average and `reduce` these lists inside
/// three `build` methods, on every rebuild. They are derived once now, by a pure
/// function — which is a function a test can call with a fixture, without a
/// widget tree, a clock or a repository.

HeartRateSample _sample(int hour, int bpm, {String source = 'Watch'}) =>
    HeartRateSample(
      time: DateTime.utc(2026, 3, 2, hour),
      beatsPerMinute: bpm,
      source: source,
    );

HeartRateSummary _summary(LocalDate date, int avg, int min, int max) =>
    HeartRateSummary(date: date, avgBpm: avg, minBpm: min, maxBpm: max);

SpO2Entry _spO2(int hour, double percent) => SpO2Entry(
      time: DateTime.utc(2026, 3, 2, hour),
      percent: percent,
      source: 'Ring',
    );

BloodPressureEntry _bp(int hour, int systolic, int diastolic) =>
    BloodPressureEntry(
      time: DateTime.utc(2026, 3, 2, hour),
      systolicMmHg: systolic,
      diastolicMmHg: diastolic,
      source: 'Cuff',
    );

RespiratoryRateEntry _breaths(DateTime time, double rate,
        {String source = 'Ring'}) =>
    RespiratoryRateEntry(
      time: time,
      breathsPerMinute: rate,
      source: source,
    );

SkinTemperatureEntry _skin(int hour, double? delta) => SkinTemperatureEntry(
      startTime: DateTime.utc(2026, 3, 2, hour),
      endTime: DateTime.utc(2026, 3, 2, hour),
      baselineCelsius: 33.0,
      averageDeltaCelsius: delta,
      minDeltaCelsius: delta,
      maxDeltaCelsius: delta,
      measurementLocation: 0,
      source: 'Ring',
    );

HeartVitalsOverviewDisplay _display(
  HeartPeriodLoadResult result, {
  TimeRange selectedRange = TimeRange.week,
}) =>
    buildHeartVitalsOverviewDisplay(result, selectedRange: selectedRange);

void main() {
  const monday = LocalDate(2026, 3, 2);
  const tuesday = LocalDate(2026, 3, 3);

  group('heart rate', () {
    test('a day of samples sorts oldest first and averages them', () {
      final display = _display(
        HeartPeriodLoadResult(
          daySamples: [_sample(14, 90), _sample(6, 52), _sample(9, 71)],
        ),
        selectedRange: TimeRange.day,
      );

      final heartRate = display.heartRate!;
      // (90 + 52 + 71) / 3 = 71.
      expect(heartRate.averageBpm, 71);
      final timeline = heartRate.dayTimeline!;
      expect(
        [for (final s in timeline.samples) s.beatsPerMinute],
        [52, 71, 90],
      );
      expect(timeline.minBpm, 52);
      expect(timeline.maxBpm, 90);
    });

    test('the axis floors at 30, never at min-5 below it', () {
      final low = _display(
        HeartPeriodLoadResult(daySamples: [_sample(6, 33), _sample(7, 40)]),
        selectedRange: TimeRange.day,
      ).heartRate!.dayTimeline!;
      // 33 - 5 = 28, below the plausible-resting floor: clamped to 30.
      expect(low.chartMinValue, 30.0);
      expect(low.chartMaxValue, 45.0);

      final normal = _display(
        HeartPeriodLoadResult(daySamples: [_sample(6, 60), _sample(7, 90)]),
        selectedRange: TimeRange.day,
      ).heartRate!.dayTimeline!;
      expect(normal.chartMinValue, 55.0);
      expect(normal.chartMaxValue, 95.0);
    });

    test('a lone day sample still fills the card but draws no timeline', () {
      final display = _display(
        HeartPeriodLoadResult(daySamples: [_sample(6, 60)]),
        selectedRange: TimeRange.day,
      );

      expect(display.heartRate!.averageBpm, 60);
      expect(display.heartRate!.dayTimeline, isNull);
    });

    test('the day card names its source only when the samples agree', () {
      final agreed = _display(
        HeartPeriodLoadResult(
          daySamples: [_sample(6, 60), _sample(7, 70)],
        ),
        selectedRange: TimeRange.day,
      );
      expect(agreed.heartRate!.source, 'Watch');

      final disagreed = _display(
        HeartPeriodLoadResult(
          daySamples: [_sample(6, 60), _sample(7, 70, source: 'Ring')],
        ),
        selectedRange: TimeRange.day,
      );
      expect(disagreed.heartRate!.source, isNull);
    });

    test('a period takes its extremes across every daily summary', () {
      final display = _display(HeartPeriodLoadResult(dailySummaries: [
        _summary(tuesday, 72, 56, 118),
        _summary(monday, 70, 55, 120),
      ]));

      final chart = display.heartRate!.periodChart!;
      expect([for (final s in chart.summaries) s.date], [monday, tuesday]);
      expect(chart.averageBpm, 71); // (70 + 72) / 2.
      expect(chart.lowestBpm, 55);
      expect(chart.highestBpm, 120);
      // The card prints the same mean; a period draws no intraday timeline.
      expect(display.heartRate!.averageBpm, 71);
      expect(display.heartRate!.dayTimeline, isNull);
      expect(display.heartRate!.source, isNull);
    });
  });

  group('resting heart rate and HRV', () {
    test('a day reads the provider aggregate, not the daily series', () {
      final display = _display(
        HeartPeriodLoadResult(
          dayRestingBpm: 58,
          dayHrvMs: 44.5,
          dailyRestingHR: [DailyRestingHR(date: monday, bpm: 99)],
        ),
        selectedRange: TimeRange.day,
      );

      expect(display.restingHeartRate!.bpm, 58);
      expect(display.restingHeartRate!.periodChart, isNull);
      expect(display.hrv!.ms, 44.5);
      expect(display.hrv!.periodChart, isNull);
    });

    test('a period averages the daily series and keeps its extremes', () {
      final display = _display(HeartPeriodLoadResult(
        dailyRestingHR: [
          DailyRestingHR(date: tuesday, bpm: 58),
          DailyRestingHR(date: monday, bpm: 61),
        ],
        dailyHrv: [
          DailyHrv(date: tuesday, rmssdMs: 45),
          DailyHrv(date: monday, rmssdMs: 41),
        ],
      ));

      final resting = display.restingHeartRate!;
      expect([for (final e in resting.periodChart!.entries) e.date],
          [monday, tuesday]);
      expect(resting.bpm, 60); // (61 + 58) / 2 = 59.5, rounded.
      expect(resting.periodChart!.lowBpm, 58);
      expect(resting.periodChart!.highBpm, 61);

      final hrv = display.hrv!;
      expect(hrv.ms, 43.0);
      expect(hrv.periodChart!.lowMs, 41.0);
      expect(hrv.periodChart!.highMs, 45.0);
    });

    test('a day with no resting aggregate has no resting card', () {
      final display = _display(
        HeartPeriodLoadResult(
          dailyRestingHR: [DailyRestingHR(date: monday, bpm: 61)],
        ),
        selectedRange: TimeRange.day,
      );

      expect(display.restingHeartRate, isNull);
      expect(display.hrv, isNull);
    });
  });

  group('cardiovascular', () {
    test('blood pressure sorts, counts and takes the latest reading', () {
      final display = _display(HeartPeriodLoadResult(
        bloodPressure: [_bp(18, 128, 82), _bp(8, 118, 76)],
      ));

      final bp = display.bloodPressure!;
      expect([for (final e in bp.entries) e.systolicMmHg], [118, 128]);
      expect(bp.latest.systolicMmHg, 128);
      expect(bp.readings, 2);
      expect(bp.hasChart, isTrue);
    });

    test('SpO2 and blood glucose average every reading', () {
      final display = _display(HeartPeriodLoadResult(
        spO2: [_spO2(9, 97), _spO2(8, 95)],
        bloodGlucose: [
          BloodGlucoseEntry(
            time: DateTime.utc(2026, 3, 2, 8),
            millimolesPerLiter: 5.0,
            specimenSource: 0,
            mealType: 0,
            relationToMeal: 0,
            source: 'Meter',
          ),
          BloodGlucoseEntry(
            time: DateTime.utc(2026, 3, 2, 12),
            millimolesPerLiter: 6.0,
            specimenSource: 0,
            mealType: 0,
            relationToMeal: 0,
            source: 'Meter',
          ),
        ],
      ));

      expect(display.spO2!.averagePercent, 96.0);
      expect(display.spO2!.latest.percent, 97.0);
      expect(display.bloodGlucose!.averageMmolPerLiter, 5.5);
      expect(display.bloodGlucose!.latest.millimolesPerLiter, 6.0);
    });

    test('within a day one timestamp draws no chart, two do', () {
      final single = _display(
        HeartPeriodLoadResult(spO2: [_spO2(9, 97)]),
        selectedRange: TimeRange.day,
      );
      // The card still shows the reading; the chart has no line to draw.
      expect(single.spO2!.latest.percent, 97.0);
      expect(single.spO2!.hasChart, isFalse);

      final pair = _display(
        HeartPeriodLoadResult(spO2: [_spO2(9, 97), _spO2(11, 95)]),
        selectedRange: TimeRange.day,
      );
      expect(pair.spO2!.hasChart, isTrue);
    });
  });

  group('respiratory', () {
    // The card and the chart summary print two different means over unevenly
    // sampled days, and both are what the screen printed before.
    final entries = [
      _breaths(DateTime.utc(2026, 3, 2, 8), 12),
      _breaths(DateTime.utc(2026, 3, 2, 12), 12),
      _breaths(DateTime.utc(2026, 3, 2, 20), 12),
      _breaths(DateTime.utc(2026, 3, 3, 12), 20),
    ];

    test('a period card and chart both print the mean of the daily means', () {
      final display =
          _display(HeartPeriodLoadResult(respiratoryRate: entries));

      final rate = display.respiratoryRate!;
      // (12 + 20) / 2 = 16, NOT the flat (12*3 + 20) / 4 = 14.
      expect(rate.periodAverage, 16.0);
      expect(rate.cardBreathsPerMinute, 16.0);
      expect(rate.cardSource, 'Ring');
      expect([for (final e in rate.entries) e.time.day], [2, 2, 2, 3]);
    });

    test('a day card prints the latest reading, the chart the daily mean', () {
      final display = _display(
        HeartPeriodLoadResult(respiratoryRate: [
          _breaths(DateTime.utc(2026, 3, 2, 20), 15),
          _breaths(DateTime.utc(2026, 3, 2, 8), 11),
        ]),
        selectedRange: TimeRange.day,
      );

      final rate = display.respiratoryRate!;
      expect(rate.cardBreathsPerMinute, 15.0);
      expect(rate.periodAverage, 13.0);
    });

    test('a mixed-source period names no source', () {
      final display = _display(HeartPeriodLoadResult(respiratoryRate: [
        _breaths(DateTime.utc(2026, 3, 2, 8), 12),
        _breaths(DateTime.utc(2026, 3, 3, 8), 14, source: 'Watch'),
      ]));

      expect(display.respiratoryRate!.cardSource, isNull);
    });

    test('skin temperature charts only the entries that carry a delta', () {
      final display = _display(HeartPeriodLoadResult(
        skinTemperature: [_skin(20, 0.4), _skin(8, null), _skin(12, -0.2)],
      ));

      final skin = display.skinTemperature!;
      expect(skin.chartEntries.length, 2);
      expect([for (final e in skin.chartEntries) e.averageDeltaCelsius],
          [-0.2, 0.4]);
      // (-0.2 + 0.4) / 2, over the delta-bearing entries only.
      expect(skin.averageDeltaCelsius, closeTo(0.1, 1e-9));
      expect(skin.cardDeltaCelsius, 0.4);
      expect(skin.latest.averageDeltaCelsius, 0.4);
    });

    test('a delta-less newest entry blanks the card but keeps the chart', () {
      final display = _display(HeartPeriodLoadResult(
        skinTemperature: [_skin(8, 0.4), _skin(20, null)],
      ));

      final skin = display.skinTemperature!;
      expect(skin.cardDeltaCelsius, isNull);
      expect(skin.hasChart, isTrue);
      expect(skin.chartEntries.single.averageDeltaCelsius, 0.4);
    });

    test('body temperature counts its readings and takes the latest', () {
      final display = _display(HeartPeriodLoadResult(bodyTemperature: [
        BodyTempEntry(
          time: DateTime.utc(2026, 3, 2, 20),
          temperatureCelsius: 36.9,
          source: 'Thermometer',
        ),
        BodyTempEntry(
          time: DateTime.utc(2026, 3, 2, 8),
          temperatureCelsius: 36.4,
          source: 'Thermometer',
        ),
      ]));

      final temp = display.bodyTemperature!;
      expect(temp.readings, 2);
      expect(temp.latest.temperatureCelsius, 36.9);
      expect([for (final e in temp.entries) e.temperatureCelsius],
          [36.4, 36.9]);
    });
  });

  test('an empty period derives an empty display, section by section', () {
    final display = _display(const HeartPeriodLoadResult());

    expect(display.heartRate, isNull);
    expect(display.restingHeartRate, isNull);
    expect(display.hrv, isNull);
    expect(display.bloodPressure, isNull);
    expect(display.spO2, isNull);
    expect(display.vo2Max, isNull);
    expect(display.bloodGlucose, isNull);
    expect(display.respiratoryRate, isNull);
    expect(display.bodyTemperature, isNull);
    expect(display.skinTemperature, isNull);
    // Which is exactly what the screen renders before its first load lands.
    expect(display, const HeartVitalsOverviewDisplay());
  });

  test('vo2 max sorts, counts and takes the latest reading', () {
    final display = _display(HeartPeriodLoadResult(vo2Max: [
      Vo2MaxEntry(
        time: DateTime.utc(2026, 3, 3),
        vo2MaxMlPerKgPerMin: 44.0,
        source: 'Watch',
      ),
      Vo2MaxEntry(
        time: DateTime.utc(2026, 3, 2),
        vo2MaxMlPerKgPerMin: 42.0,
        source: 'Watch',
      ),
    ]));

    final vo2 = display.vo2Max!;
    expect([for (final e in vo2.entries) e.vo2MaxMlPerKgPerMin], [42.0, 44.0]);
    expect(vo2.latest.vo2MaxMlPerKgPerMin, 44.0);
    expect(vo2.readings, 2);
    expect(vo2.hasChart, isTrue);
  });
}
