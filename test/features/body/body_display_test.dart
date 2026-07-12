import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/body_models.dart';
import 'package:openvitals/domain/query/body_period_data.dart';
import 'package:openvitals/features/body/application/body_display.dart';

/// The derivations the body screen used to do in its build path — the latest
/// value per metric, the BMI/FFMI arithmetic, the daily chart series and the
/// combined reading list — now a pure function the view-model calls once per
/// load.
WeightEntry _weight(
  DateTime time,
  double kg, {
  String id = '',
  bool isOpenVitalsEntry = false,
}) =>
    WeightEntry(
      time: time,
      weightKg: kg,
      source: 'test',
      id: id,
      isOpenVitalsEntry: isOpenVitalsEntry,
    );

BodyMetricSeries _metric(BodyDisplay display, BodyMetricKind kind) =>
    display.metrics.firstWhere((series) => series.kind == kind);

void main() {
  final monday = DateTime(2026, 3, 2, 8);
  final tuesday = DateTime(2026, 3, 3, 8);

  test('an empty period has no data, no readings and no tracked metrics', () {
    final display = buildBodyDisplay(const BodyPeriodData());

    expect(display.hasAnyBodyData, isFalse);
    expect(display.readingsNewestFirst, isEmpty);
    expect(display.trackedMetrics, isEmpty);
    expect(display.summary.bmi, isNull);
    // All nine metrics exist; they are simply empty.
    expect(display.metrics.length, 9);
  });

  test('the summary takes the latest reading, and the first weight', () {
    final display = buildBodyDisplay(BodyPeriodData(
      weightEntries: [
        _weight(tuesday, 70.0),
        _weight(monday, 72.0),
      ],
      heightEntries: [
        HeightEntry(time: monday, heightCm: 180.0, source: 'test'),
      ],
      bodyFatEntries: [
        BodyFatEntry(time: monday, percent: 20.0, source: 'test'),
      ],
    ));

    final summary = display.summary;
    expect(summary.latestWeightKg, 70.0);
    expect(summary.firstWeightKg, 72.0);
    expect(summary.weightChangeKg, -2.0);
    expect(summary.heightCm, 180.0);
    // 70 / 1.8² = 21.6
    expect(summary.bmi, closeTo(21.6, 0.01));
    // Fat-free mass 56kg / 1.8² = 17.28, adjusted by 6.3 * (1.8 - 1.8) = 0.
    expect(summary.ffmi, closeTo(17.28, 0.01));
    expect(summary.adjustedFfmi, closeTo(17.28, 0.01));
  });

  test('the daily series keeps one value per day: that day\'s latest', () {
    final display = buildBodyDisplay(BodyPeriodData(
      weightEntries: [
        _weight(monday, 72.0),
        _weight(monday.add(const Duration(hours: 10)), 71.0),
        _weight(tuesday, 70.0),
      ],
    ));

    final weight = _metric(display, BodyMetricKind.weight);
    expect(weight.values.length, 2);
    // Monday's evening reading wins; the days are in order.
    expect(weight.values.first.value, 71.0);
    expect(weight.values.last.value, 70.0);
    // The intraday samples keep every reading, oldest first.
    expect(weight.daySamples.length, 3);
    expect(weight.daySamples.first.value, 72.0);
    expect(weight.daySamples.last.value, 70.0);
    expect(weight.hasTrackedValues, isTrue);
    expect(display.trackedMetrics.map((m) => m.kind), [BodyMetricKind.weight]);
  });

  test('BMI has a series only when a height is known', () {
    final withoutHeight = buildBodyDisplay(BodyPeriodData(
      weightEntries: [_weight(monday, 72.0)],
    ));
    expect(_metric(withoutHeight, BodyMetricKind.bmi).values, isEmpty);

    final withHeight = buildBodyDisplay(BodyPeriodData(
      weightEntries: [_weight(monday, 72.9)],
      heightEntries: [
        HeightEntry(time: monday, heightCm: 180.0, source: 'test'),
      ],
    ));
    final bmi = _metric(withHeight, BodyMetricKind.bmi);
    expect(bmi.values.single.value, closeTo(22.5, 0.01));
    expect(bmi.daySamples.single.value, closeTo(22.5, 0.01));
    // FFMI never gets a series, in Kotlin or here — only a latest value.
    expect(_metric(withHeight, BodyMetricKind.ffmi).values, isEmpty);
  });

  test('readings are newest first, indexed by day, and only OpenVitals ones '
      'are editable', () {
    final display = buildBodyDisplay(BodyPeriodData(
      weightEntries: [
        _weight(monday, 72.0, id: 'w1', isOpenVitalsEntry: true),
        // An OpenVitals entry with no id is not editable.
        _weight(monday, 71.5, isOpenVitalsEntry: true),
        // Another app's entry never is.
        _weight(tuesday, 70.0, id: 'w2'),
      ],
      bmrEntries: [BmrEntry(time: tuesday, kcalPerDay: 1800, source: 'test')],
    ));

    expect(display.readingsNewestFirst.first.time, tuesday);
    expect(display.readingsNewestFirst.length, 4);

    final editable =
        display.readingsNewestFirst.where((r) => r.editable).toList();
    expect(editable.length, 1);
    expect(editable.single.editId, 'w1');
    expect(editable.single.editType, BodyMeasurementType.weight);

    expect(display.readingsByDate[const LocalDate(2026, 3, 2)]!.length, 2);
    expect(display.readingsByDate[const LocalDate(2026, 3, 3)]!.length, 2);
  });

  test('a period with a latest value but no entries still has data', () {
    // The provider can report an aggregate with no readings in the window.
    final display = buildBodyDisplay(const BodyPeriodData(latestWeightKg: 70.0));

    expect(display.hasAnyBodyData, isTrue);
    expect(display.readingsNewestFirst, isEmpty);
    expect(_metric(display, BodyMetricKind.weight).latest, 70.0);
  });
}
