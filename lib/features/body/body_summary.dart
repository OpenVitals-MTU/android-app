import '../../domain/model/body_models.dart';
import '../../domain/query/body_period_data.dart';

/// The derived body-composition summary, a Dart port of the Kotlin
/// `BodyPresentationMapper` (`summary(...)` plus the BMI / FFMI helpers). It
/// resolves each metric's latest value for the loaded period and computes the
/// derived BMI, FFMI, and adjusted FFMI from weight, height, and body fat.
class BodySummary {
  const BodySummary({
    this.heightCm,
    this.latestWeightKg,
    this.firstWeightKg,
    this.weightChangeKg,
    this.latestBodyFatPercent,
    this.latestHeightCm,
    this.latestLeanMassKg,
    this.latestBmrKcal,
    this.latestBoneMassKg,
    this.latestBodyWaterMassKg,
    this.bmi,
    this.ffmi,
    this.adjustedFfmi,
  });

  final double? heightCm;
  final double? latestWeightKg;
  final double? firstWeightKg;
  final double? weightChangeKg;
  final double? latestBodyFatPercent;
  final double? latestHeightCm;
  final double? latestLeanMassKg;
  final double? latestBmrKcal;
  final double? latestBoneMassKg;
  final double? latestBodyWaterMassKg;
  final double? bmi;
  final double? ffmi;
  final double? adjustedFfmi;

  static BodySummary fromPeriod(BodyPeriodData data) {
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
}

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
