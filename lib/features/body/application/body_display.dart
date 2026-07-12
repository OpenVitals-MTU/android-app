import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/time/local_date.dart';
import '../../../domain/model/body_models.dart';
import '../../../domain/query/body_period_data.dart';
import '../../../ui/charts/bar_chart.dart';
import '../../../ui/charts/metric_day_chart.dart';

part 'body_display.freezed.dart';

/// The nine body-composition metrics the overview renders, in the Kotlin order
/// (Kotlin `bodyMetricData`). The title, icon, accent and unit formatting are the
/// view's; which metric it is, and what it measured, is this.
enum BodyMetricKind {
  weight,
  height,
  bmi,
  ffmi,
  bodyFat,
  leanMass,
  boneMass,
  bodyWaterMass,
  bmr,
}

/// One metric's series across the loaded period: the daily chart values, the raw
/// intraday samples (oldest first) and the latest reading behind the statistics
/// tile. All of it storage-unit; the view converts and formats.
@freezed
abstract class BodyMetricSeries with _$BodyMetricSeries {
  const BodyMetricSeries._();

  const factory BodyMetricSeries({
    required BodyMetricKind kind,

    /// The metric's latest value, or null when it has no reading at all.
    double? latest,

    /// Daily latest values feeding the period chart (Kotlin `dailyLatestValues`).
    required List<PeriodChartValue> values,

    /// Raw samples feeding the DAY-range intraday line, oldest first.
    required List<DaySample> daySamples,
  }) = _BodyMetricSeries;

  /// Kotlin `hasTrackedValues`: the metric earns a trend chart only when the
  /// period actually has values.
  bool get hasTrackedValues => values.isNotEmpty;
}

/// One reading in the combined entry list. The label and the formatted value are
/// the view's; the reading is a number, a time and a provenance.
@freezed
abstract class BodyReading with _$BodyReading {
  const BodyReading._();

  const factory BodyReading({
    required BodyMetricKind kind,
    required double value,
    required String source,
    required DateTime time,

    /// Set when the entry is an editable OpenVitals manual entry (Kotlin
    /// `isOpenVitalsEntry && id.isNotBlank()`); null rows are read-only.
    BodyMeasurementType? editType,
    String? editId,
  }) = _BodyReading;

  bool get editable => editType != null && editId != null;
}

/// The derived body-composition summary, a Dart port of the Kotlin
/// `BodyPresentationMapper` (`summary(...)` plus the BMI / FFMI helpers). It
/// resolves each metric's latest value for the loaded period and computes the
/// derived BMI, FFMI, and adjusted FFMI from weight, height, and body fat.
@freezed
abstract class BodySummary with _$BodySummary {
  const factory BodySummary({
    double? heightCm,
    double? latestWeightKg,
    double? firstWeightKg,
    double? weightChangeKg,
    double? latestBodyFatPercent,
    double? latestHeightCm,
    double? latestLeanMassKg,
    double? latestBmrKcal,
    double? latestBoneMassKg,
    double? latestBodyWaterMassKg,
    double? bmi,
    double? ffmi,
    double? adjustedFfmi,
  }) = _BodySummary;
}

/// The screen-ready derivation of one loaded body period: the summary, every
/// metric's series, and the combined reading list in display order.
///
/// Built once per load by [buildBodyDisplay] and stored on the state — the
/// view-model precomputes, the screen only renders. The screen used to resolve
/// each metric's latest reading, bucket the daily values, expand the BMI history
/// and sort the reading list on every rebuild.
@freezed
abstract class BodyDisplay with _$BodyDisplay {
  const factory BodyDisplay({
    required BodySummary summary,
    required List<BodyMetricSeries> metrics,

    /// The metrics with values in the period — the ones that earn a chart.
    required List<BodyMetricSeries> trackedMetrics,
    required List<BodyReading> readingsNewestFirst,

    /// The readings of each tracked day, so the pinned-day section looks its day
    /// up rather than scanning the whole list for it.
    required Map<LocalDate, List<BodyReading>> readingsByDate,

    /// Kotlin `bodyContent`: false when the whole period has no body data, which
    /// is when the screen shows its placeholder.
    required bool hasAnyBodyData,
  }) = _BodyDisplay;
}

/// Pure derivation from the loaded period to its display model. No clock, no
/// formatter, no l10n — unit-testable with a fixture [BodyPeriodData].
BodyDisplay buildBodyDisplay(BodyPeriodData data) {
  final summary = _summary(data);
  final metrics = _metrics(data, summary);
  final readings = _readings(data)
    ..sort((a, b) => b.time.compareTo(a.time));

  final byDate = <LocalDate, List<BodyReading>>{};
  for (final reading in readings) {
    byDate
        .putIfAbsent(instantToLocalDate(reading.time), () => <BodyReading>[])
        .add(reading);
  }

  return BodyDisplay(
    summary: summary,
    metrics: metrics,
    trackedMetrics:
        metrics.where((metric) => metric.hasTrackedValues).toList(),
    readingsNewestFirst: readings,
    readingsByDate: byDate,
    hasAnyBodyData: metrics
            .any((metric) => metric.latest != null || metric.hasTrackedValues) ||
        readings.isNotEmpty,
  );
}

BodySummary _summary(BodyPeriodData data) {
  final heightCm = _latestBy<HeightEntry>(
        data.heightEntries,
        (e) => e.time,
        (e) => e.heightCm,
      ) ??
      data.heightCm;
  final latestWeightKg = _latestBy<WeightEntry>(
        data.weightEntries,
        (e) => e.time,
        (e) => e.weightKg,
      ) ??
      data.latestWeightKg;
  final firstWeightKg = _firstBy<WeightEntry>(
    data.weightEntries,
    (e) => e.time,
    (e) => e.weightKg,
  );
  final latestBodyFatPercent = _latestBy<BodyFatEntry>(
        data.bodyFatEntries,
        (e) => e.time,
        (e) => e.percent,
      ) ??
      data.latestBodyFatPercent;
  final ffmi = _ffmi(latestWeightKg, heightCm, latestBodyFatPercent);

  return BodySummary(
    heightCm: heightCm,
    latestWeightKg: latestWeightKg,
    firstWeightKg: firstWeightKg,
    weightChangeKg: (latestWeightKg != null &&
            firstWeightKg != null &&
            latestWeightKg != firstWeightKg)
        ? latestWeightKg - firstWeightKg
        : null,
    latestBodyFatPercent: latestBodyFatPercent,
    latestHeightCm: heightCm,
    latestLeanMassKg: _latestBy<LeanBodyMassEntry>(
          data.leanMassEntries,
          (e) => e.time,
          (e) => e.massKg,
        ) ??
        data.leanMassKg,
    latestBmrKcal: _latestBy<BmrEntry>(
          data.bmrEntries,
          (e) => e.time,
          (e) => e.kcalPerDay,
        ) ??
        data.bmrKcal,
    latestBoneMassKg: _latestBy<BoneMassEntry>(
          data.boneMassEntries,
          (e) => e.time,
          (e) => e.massKg,
        ) ??
        data.boneMassKg,
    latestBodyWaterMassKg: _latestBy<BodyWaterMassEntry>(
          data.bodyWaterMassEntries,
          (e) => e.time,
          (e) => e.massKg,
        ) ??
        data.bodyWaterMassKg,
    bmi: _bmi(latestWeightKg, heightCm),
    ffmi: ffmi,
    adjustedFfmi: _adjustedFfmi(ffmi, heightCm),
  );
}

/// Kotlin `bodyMetricData`: the nine overview metrics, in the Kotlin order
/// (weight, height, BMI, FFMI, body fat, lean mass, bone mass, body water
/// mass, BMR).
List<BodyMetricSeries> _metrics(BodyPeriodData data, BodySummary summary) => [
      BodyMetricSeries(
        kind: BodyMetricKind.weight,
        latest: summary.latestWeightKg,
        values: dailyLatestValues(
          data.weightEntries,
          (e) => e.time,
          (e) => e.weightKg,
        ),
        daySamples: _daySamples(
          data.weightEntries,
          (e) => e.time,
          (e) => e.weightKg,
        ),
      ),
      BodyMetricSeries(
        kind: BodyMetricKind.height,
        latest: summary.latestHeightCm,
        values: dailyLatestValues(
          data.heightEntries,
          (e) => e.time,
          (e) => e.heightCm,
        ),
        daySamples: _daySamples(
          data.heightEntries,
          (e) => e.time,
          (e) => e.heightCm,
        ),
      ),
      BodyMetricSeries(
        kind: BodyMetricKind.bmi,
        latest: summary.bmi,
        values: _bmiHistoryValues(data.weightEntries, summary.heightCm),
        daySamples: _bmiDaySamples(data.weightEntries, summary.heightCm),
      ),
      BodyMetricSeries(
        kind: BodyMetricKind.ffmi,
        latest: summary.adjustedFfmi,
        values: const <PeriodChartValue>[],
        daySamples: const <DaySample>[],
      ),
      BodyMetricSeries(
        kind: BodyMetricKind.bodyFat,
        latest: summary.latestBodyFatPercent,
        values: dailyLatestValues(
          data.bodyFatEntries,
          (e) => e.time,
          (e) => e.percent,
        ),
        daySamples: _daySamples(
          data.bodyFatEntries,
          (e) => e.time,
          (e) => e.percent,
        ),
      ),
      BodyMetricSeries(
        kind: BodyMetricKind.leanMass,
        latest: summary.latestLeanMassKg,
        values: dailyLatestValues(
          data.leanMassEntries,
          (e) => e.time,
          (e) => e.massKg,
        ),
        daySamples: _daySamples(
          data.leanMassEntries,
          (e) => e.time,
          (e) => e.massKg,
        ),
      ),
      BodyMetricSeries(
        kind: BodyMetricKind.boneMass,
        latest: summary.latestBoneMassKg,
        values: dailyLatestValues(
          data.boneMassEntries,
          (e) => e.time,
          (e) => e.massKg,
        ),
        daySamples: _daySamples(
          data.boneMassEntries,
          (e) => e.time,
          (e) => e.massKg,
        ),
      ),
      BodyMetricSeries(
        kind: BodyMetricKind.bodyWaterMass,
        latest: summary.latestBodyWaterMassKg,
        values: dailyLatestValues(
          data.bodyWaterMassEntries,
          (e) => e.time,
          (e) => e.massKg,
        ),
        daySamples: _daySamples(
          data.bodyWaterMassEntries,
          (e) => e.time,
          (e) => e.massKg,
        ),
      ),
      BodyMetricSeries(
        kind: BodyMetricKind.bmr,
        latest: summary.latestBmrKcal,
        values: dailyLatestValues(
          data.bmrEntries,
          (e) => e.time,
          (e) => e.kcalPerDay,
        ),
        daySamples: _daySamples(
          data.bmrEntries,
          (e) => e.time,
          (e) => e.kcalPerDay,
        ),
      ),
    ];

/// Kotlin `bodyReadingItems`: every reading across the eight measured metrics.
/// Weight / height / body-fat OpenVitals entries carry edit + delete actions.
List<BodyReading> _readings(BodyPeriodData data) {
  BodyMeasurementType? editTypeFor(
    BodyMeasurementType type,
    bool isOpenVitalsEntry,
    String id,
  ) =>
      (isOpenVitalsEntry && id.isNotEmpty) ? type : null;

  return [
    for (final e in data.weightEntries)
      BodyReading(
        kind: BodyMetricKind.weight,
        value: e.weightKg,
        source: e.source,
        time: e.time,
        editType:
            editTypeFor(BodyMeasurementType.weight, e.isOpenVitalsEntry, e.id),
        editId: (e.isOpenVitalsEntry && e.id.isNotEmpty) ? e.id : null,
      ),
    for (final e in data.heightEntries)
      BodyReading(
        kind: BodyMetricKind.height,
        value: e.heightCm,
        source: e.source,
        time: e.time,
        editType:
            editTypeFor(BodyMeasurementType.height, e.isOpenVitalsEntry, e.id),
        editId: (e.isOpenVitalsEntry && e.id.isNotEmpty) ? e.id : null,
      ),
    for (final e in data.bodyFatEntries)
      BodyReading(
        kind: BodyMetricKind.bodyFat,
        value: e.percent,
        source: e.source,
        time: e.time,
        editType:
            editTypeFor(BodyMeasurementType.bodyFat, e.isOpenVitalsEntry, e.id),
        editId: (e.isOpenVitalsEntry && e.id.isNotEmpty) ? e.id : null,
      ),
    for (final e in data.leanMassEntries)
      BodyReading(
        kind: BodyMetricKind.leanMass,
        value: e.massKg,
        source: e.source,
        time: e.time,
      ),
    for (final e in data.bmrEntries)
      BodyReading(
        kind: BodyMetricKind.bmr,
        value: e.kcalPerDay,
        source: e.source,
        time: e.time,
      ),
    for (final e in data.boneMassEntries)
      BodyReading(
        kind: BodyMetricKind.boneMass,
        value: e.massKg,
        source: e.source,
        time: e.time,
      ),
    for (final e in data.bodyWaterMassEntries)
      BodyReading(
        kind: BodyMetricKind.bodyWaterMass,
        value: e.massKg,
        source: e.source,
        time: e.time,
      ),
  ];
}

/// Kotlin `dailyLatestValues`: one chart value per tracked day — the value of
/// that day's latest reading.
List<PeriodChartValue> dailyLatestValues<T>(
  List<T> entries,
  DateTime Function(T) time,
  double Function(T) value,
) {
  final latestByDate = <LocalDate, T>{};
  for (final entry in entries) {
    final date = instantToLocalDate(time(entry));
    final current = latestByDate[date];
    if (current == null || time(entry).isAfter(time(current))) {
      latestByDate[date] = entry;
    }
  }
  final values = [
    for (final MapEntry(key: date, value: entry) in latestByDate.entries)
      PeriodChartValue(date, value(entry)),
  ]..sort((a, b) => a.date.compareTo(b.date));
  return values;
}

/// The DAY-range line's samples, oldest first (the card used to sort them on
/// every rebuild).
List<DaySample> _daySamples<T>(
  List<T> entries,
  DateTime Function(T) time,
  double Function(T) value,
) =>
    [
      for (final entry in entries) (time: time(entry), value: value(entry)),
    ]..sort((a, b) => a.time.compareTo(b.time));

List<PeriodChartValue> _bmiHistoryValues(
  List<WeightEntry> entries,
  double? heightCm,
) {
  final heightMeters = _heightMeters(heightCm);
  if (heightMeters == null) return const <PeriodChartValue>[];
  return dailyLatestValues(
    entries,
    (e) => e.time,
    (e) => e.weightKg / (heightMeters * heightMeters),
  );
}

List<DaySample> _bmiDaySamples(
  List<WeightEntry> entries,
  double? heightCm,
) {
  final heightMeters = _heightMeters(heightCm);
  if (heightMeters == null) return const <DaySample>[];
  return _daySamples(
    entries,
    (e) => e.time,
    (e) => e.weightKg / (heightMeters * heightMeters),
  );
}

double? _heightMeters(double? heightCm) =>
    (heightCm != null && heightCm > 0.0) ? heightCm / 100.0 : null;

double? _bmi(double? weightKg, double? heightCm) {
  if (weightKg == null || heightCm == null || heightCm <= 0.0) return null;
  final heightMeters = heightCm / 100.0;
  return weightKg / (heightMeters * heightMeters);
}

double? _ffmi(double? weightKg, double? heightCm, double? bodyFatPercent) {
  if (weightKg == null || heightCm == null || bodyFatPercent == null) {
    return null;
  }
  if (weightKg <= 0.0 ||
      heightCm <= 0.0 ||
      bodyFatPercent < 0.0 ||
      bodyFatPercent >= 100.0) {
    return null;
  }
  final heightMeters = heightCm / 100.0;
  final fatFreeMassKg = weightKg * (1.0 - bodyFatPercent / 100.0);
  return fatFreeMassKg / (heightMeters * heightMeters);
}

double? _adjustedFfmi(double? ffmi, double? heightCm) {
  if (ffmi == null || heightCm == null || heightCm <= 0.0) return null;
  final heightMeters = heightCm / 100.0;
  return ffmi + (6.3 * (1.8 - heightMeters));
}

double? _latestBy<T>(
  List<T> entries,
  DateTime Function(T) time,
  double Function(T) value,
) {
  if (entries.isEmpty) return null;
  var latest = entries.first;
  for (final entry in entries) {
    if (time(entry).isAfter(time(latest))) latest = entry;
  }
  return value(latest);
}

double? _firstBy<T>(
  List<T> entries,
  DateTime Function(T) time,
  double Function(T) value,
) {
  if (entries.isEmpty) return null;
  var first = entries.first;
  for (final entry in entries) {
    if (time(entry).isBefore(time(first))) first = entry;
  }
  return value(first);
}
