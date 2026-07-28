/// What a CSV column can be mapped onto, and how its raw text becomes the
/// canonical (metric) value Health Connect stores.
///
/// v1 covers body composition only — the metrics a smart scale exports. Every
/// one is a single instant-in-time measurement, so a mapping needs exactly one
/// timestamp column and one column per metric. Interval records (steps, sleep,
/// workouts) need a second timestamp and are deliberately absent; adding them
/// later is an entry in [kCsvMetricCatalog] plus an end-timestamp column role,
/// not a rewrite.
library;

import '../../../core/presentation/measurement_input.dart';
import '../../../domain/health/health_permissions.dart';

/// A metric a CSV column can be mapped onto.
///
/// Every one is a single measurement at a single instant. Blood pressure is
/// absent on purpose: systolic and diastolic have to become ONE record, which
/// needs a two-columns-to-one-record rule the mapping model does not have.
enum CsvImportMetric {
  weight,
  bodyFat,
  leanBodyMass,
  boneMass,
  bodyWaterMass,
  height,
  basalMetabolicRate,
  heartRate,
  restingHeartRate,
  heartRateVariability,
  oxygenSaturation,
  respiratoryRate,
  bodyTemperature,
  basalBodyTemperature,
  bloodGlucose,
  vo2Max,
}

/// A unit a CSV column's numbers can be written in.
///
/// This is the unit of the *file*, chosen by the user per column. It is not the
/// app's display unit system: a file can be in pounds while the app displays
/// kilograms, and neither implies the other. Nothing here may consult
/// `unitSystemProvider` — see AGENTS.md invariant #2.
enum CsvUnit {
  kilograms,
  pounds,
  stones,
  grams,
  percent,
  fraction,
  centimeters,
  meters,
  inches,
  feet,
  kilocaloriesPerDay,
  kilojoulesPerDay,
  celsius,
  fahrenheit,
  beatsPerMinute,
  milliseconds,
  seconds,
  breathsPerMinute,
  millimolesPerLiter,
  milligramsPerDeciliter,
  millilitersPerKgPerMinute,
}

const double _kKilogramsPerStone = 6.35029318;
const double _kCentimetersPerFoot = 30.48;
const double _kKilojoulesPerKilocalorie = 4.184;

/// Health Connect stores blood glucose in mmol/L; the US convention is mg/dL.
/// The factor is the one `BloodGlucoseImportRecord.milligramsPerDeciliter`
/// already uses, kept here as its inverse so both directions agree.
const double _kMilligramsPerDeciliterPerMillimolePerLiter = 18.0;

/// How a column's raw number becomes the metric's canonical value.
sealed class CsvValueInterpretation {
  const CsvValueInterpretation();

  /// Whether resolving this needs the row's body weight as well as its own cell.
  bool get needsRowWeight => this is CsvMassShareOfWeight;
}

/// The number in the cell IS the metric, expressed in [unit].
final class CsvDirectValue extends CsvValueInterpretation {
  const CsvDirectValue(this.unit);

  final CsvUnit unit;

  @override
  bool operator ==(Object other) =>
      other is CsvDirectValue && other.unit == unit;

  @override
  int get hashCode => Object.hash(CsvDirectValue, unit);
}

/// The cell holds a **mass**, and the metric is that mass as a percentage of
/// the row's body weight.
///
/// Scales export "Fat mass (kg)" while Health Connect's `BodyFatRecord` stores a
/// percentage, so the percentage has to be derived — and it can only be derived
/// from the weight measured at the same moment, i.e. the weight column of the
/// SAME row. A row missing that weight cannot produce this metric, which is why
/// [CsvValueInterpretation.needsRowWeight] exists: the mapping validator asks
/// the interpretation, not the metric, whether a weight column is required.
final class CsvMassShareOfWeight extends CsvValueInterpretation {
  const CsvMassShareOfWeight(this.unit);

  /// Always a mass unit — the catalog only offers mass units for this.
  final CsvUnit unit;

  @override
  bool operator ==(Object other) =>
      other is CsvMassShareOfWeight && other.unit == unit;

  @override
  int get hashCode => Object.hash(CsvMassShareOfWeight, unit);
}

/// Everything the importer needs to know about one metric.
class CsvMetricSpec {
  const CsvMetricSpec({
    required this.targetType,
    required this.writePermission,
    required this.interpretations,
    required this.plausibleMin,
    required this.plausibleMax,
  });

  /// The Health Connect record class this produces, matching
  /// `ImportRecord.targetType`.
  final String targetType;

  /// The single Health Connect write permission this metric needs.
  final String writePermission;

  /// Offered in the UI in this order; the first is the default.
  final List<CsvValueInterpretation> interpretations;

  /// Bounds on the CANONICAL value, used to reject a row rather than write a
  /// number Health Connect would happily store. A 900 kg weight is a mis-mapped
  /// column, not a person.
  final double plausibleMin;
  final double plausibleMax;

  CsvValueInterpretation get defaultInterpretation => interpretations.first;
}

/// The metric catalog. Adding a metric is an entry here plus an ARB label.
final Map<CsvImportMetric, CsvMetricSpec> kCsvMetricCatalog = {
  CsvImportMetric.weight: CsvMetricSpec(
    targetType: 'WeightRecord',
    writePermission: HcPermissions.writeWeight,
    interpretations: const [
      CsvDirectValue(CsvUnit.kilograms),
      CsvDirectValue(CsvUnit.pounds),
      CsvDirectValue(CsvUnit.stones),
      CsvDirectValue(CsvUnit.grams),
    ],
    plausibleMin: 2,
    plausibleMax: 650,
  ),
  CsvImportMetric.bodyFat: CsvMetricSpec(
    targetType: 'BodyFatRecord',
    writePermission: HcPermissions.writeBodyFat,
    // The mass interpretations are what make a Withings-style export usable
    // without editing the file.
    interpretations: const [
      CsvDirectValue(CsvUnit.percent),
      CsvDirectValue(CsvUnit.fraction),
      CsvMassShareOfWeight(CsvUnit.kilograms),
      CsvMassShareOfWeight(CsvUnit.pounds),
      CsvMassShareOfWeight(CsvUnit.grams),
    ],
    // Below ~2% is not survivable and above ~75% is not anatomically possible;
    // both mean the weight column used for the derivation was the wrong one.
    plausibleMin: 2,
    plausibleMax: 75,
  ),
  CsvImportMetric.leanBodyMass: CsvMetricSpec(
    targetType: 'LeanBodyMassRecord',
    writePermission: HcPermissions.writeLeanBodyMass,
    interpretations: const [
      CsvDirectValue(CsvUnit.kilograms),
      CsvDirectValue(CsvUnit.pounds),
      CsvDirectValue(CsvUnit.grams),
    ],
    plausibleMin: 1,
    plausibleMax: 400,
  ),
  CsvImportMetric.boneMass: CsvMetricSpec(
    targetType: 'BoneMassRecord',
    writePermission: HcPermissions.writeBoneMass,
    interpretations: const [
      CsvDirectValue(CsvUnit.kilograms),
      CsvDirectValue(CsvUnit.pounds),
      CsvDirectValue(CsvUnit.grams),
    ],
    plausibleMin: 0.1,
    plausibleMax: 20,
  ),
  CsvImportMetric.bodyWaterMass: CsvMetricSpec(
    targetType: 'BodyWaterMassRecord',
    writePermission: HcPermissions.writeBodyWaterMass,
    interpretations: const [
      CsvDirectValue(CsvUnit.kilograms),
      CsvDirectValue(CsvUnit.pounds),
      CsvDirectValue(CsvUnit.grams),
    ],
    plausibleMin: 0.5,
    plausibleMax: 400,
  ),
  CsvImportMetric.height: CsvMetricSpec(
    targetType: 'HeightRecord',
    writePermission: HcPermissions.writeHeight,
    interpretations: const [
      CsvDirectValue(CsvUnit.centimeters),
      CsvDirectValue(CsvUnit.meters),
      CsvDirectValue(CsvUnit.inches),
      CsvDirectValue(CsvUnit.feet),
    ],
    // Canonical height is METRES (HeightImportRecord.meters).
    plausibleMin: 0.3,
    plausibleMax: 2.8,
  ),
  CsvImportMetric.basalMetabolicRate: CsvMetricSpec(
    targetType: 'BasalMetabolicRateRecord',
    writePermission: HcPermissions.writeBasalMetabolicRate,
    interpretations: const [
      CsvDirectValue(CsvUnit.kilocaloriesPerDay),
      CsvDirectValue(CsvUnit.kilojoulesPerDay),
    ],
    plausibleMin: 200,
    plausibleMax: 12000,
  ),

  // ── Vitals ────────────────────────────────────────────────────────────────
  // Each is one number at one instant, which is the only shape the mapping
  // model expresses. Ranges are "survivable human", not "clinically normal":
  // rejecting a real fever or a real bradycardia would be worse than storing it.
  CsvImportMetric.heartRate: CsvMetricSpec(
    targetType: 'HeartRateRecord',
    writePermission: HcPermissions.writeHeartRate,
    interpretations: const [CsvDirectValue(CsvUnit.beatsPerMinute)],
    plausibleMin: 1,
    plausibleMax: 300,
  ),
  CsvImportMetric.restingHeartRate: CsvMetricSpec(
    targetType: 'RestingHeartRateRecord',
    writePermission: HcPermissions.writeRestingHeartRate,
    interpretations: const [CsvDirectValue(CsvUnit.beatsPerMinute)],
    plausibleMin: 1,
    plausibleMax: 300,
  ),
  CsvImportMetric.heartRateVariability: CsvMetricSpec(
    targetType: 'HeartRateVariabilityRmssdRecord',
    writePermission: HcPermissions.writeHrv,
    interpretations: const [
      CsvDirectValue(CsvUnit.milliseconds),
      CsvDirectValue(CsvUnit.seconds),
    ],
    // Health Connect rejects RMSSD outside 1..200 ms.
    plausibleMin: 1,
    plausibleMax: 200,
  ),
  CsvImportMetric.oxygenSaturation: CsvMetricSpec(
    targetType: 'OxygenSaturationRecord',
    writePermission: HcPermissions.writeSpO2,
    interpretations: const [
      CsvDirectValue(CsvUnit.percent),
      CsvDirectValue(CsvUnit.fraction),
    ],
    plausibleMin: 50,
    plausibleMax: 100,
  ),
  CsvImportMetric.respiratoryRate: CsvMetricSpec(
    targetType: 'RespiratoryRateRecord',
    writePermission: HcPermissions.writeRespiratoryRate,
    interpretations: const [CsvDirectValue(CsvUnit.breathsPerMinute)],
    plausibleMin: 1,
    plausibleMax: 100,
  ),
  CsvImportMetric.bodyTemperature: CsvMetricSpec(
    targetType: 'BodyTemperatureRecord',
    writePermission: HcPermissions.writeBodyTemperature,
    interpretations: const [
      CsvDirectValue(CsvUnit.celsius),
      CsvDirectValue(CsvUnit.fahrenheit),
    ],
    plausibleMin: 25,
    plausibleMax: 45,
  ),
  CsvImportMetric.basalBodyTemperature: CsvMetricSpec(
    targetType: 'BasalBodyTemperatureRecord',
    writePermission: HcPermissions.writeBasalBodyTemperature,
    interpretations: const [
      CsvDirectValue(CsvUnit.celsius),
      CsvDirectValue(CsvUnit.fahrenheit),
    ],
    plausibleMin: 25,
    plausibleMax: 45,
  ),
  CsvImportMetric.bloodGlucose: CsvMetricSpec(
    targetType: 'BloodGlucoseRecord',
    writePermission: HcPermissions.writeBloodGlucose,
    interpretations: const [
      CsvDirectValue(CsvUnit.millimolesPerLiter),
      CsvDirectValue(CsvUnit.milligramsPerDeciliter),
    ],
    // Canonical is mmol/L: roughly 18..900 mg/dL.
    plausibleMin: 1,
    plausibleMax: 50,
  ),
  CsvImportMetric.vo2Max: CsvMetricSpec(
    targetType: 'Vo2MaxRecord',
    writePermission: HcPermissions.writeVo2Max,
    interpretations: const [
      CsvDirectValue(CsvUnit.millilitersPerKgPerMinute),
    ],
    plausibleMin: 5,
    plausibleMax: 100,
  ),
};

/// Converts [value] from [unit] to the metric's canonical unit — kg for masses,
/// metres for height, kcal/day for BMR, percent for body fat.
///
/// Conversion factors come from [MeasurementInput]'s shared constants; a local
/// constant here would be the "bare imperial check in a feature file" AGENTS.md
/// invariant #2 bans. Stones, feet and kJ have no constant there because no
/// entry screen offers them, so they are defined once above.
double convertCsvValueToCanonical(double value, CsvUnit unit) => switch (unit) {
      CsvUnit.kilograms => value,
      CsvUnit.pounds => value / kPoundsPerKilogram,
      CsvUnit.stones => value * _kKilogramsPerStone,
      CsvUnit.grams => value / 1000,
      CsvUnit.percent => value,
      CsvUnit.fraction => value * 100,
      CsvUnit.centimeters => value / 100,
      CsvUnit.meters => value,
      CsvUnit.inches => value * kCentimetersPerInch / 100,
      CsvUnit.feet => value * _kCentimetersPerFoot / 100,
      CsvUnit.kilocaloriesPerDay => value,
      CsvUnit.kilojoulesPerDay => value / _kKilojoulesPerKilocalorie,
      CsvUnit.celsius => value,
      CsvUnit.fahrenheit =>
        (value - kFahrenheitFreezingPoint) / kFahrenheitPerCelsius,
      CsvUnit.beatsPerMinute => value,
      CsvUnit.milliseconds => value,
      CsvUnit.seconds => value * 1000,
      CsvUnit.breathsPerMinute => value,
      CsvUnit.millimolesPerLiter => value,
      CsvUnit.milligramsPerDeciliter =>
        value / _kMilligramsPerDeciliterPerMillimolePerLiter,
      CsvUnit.millilitersPerKgPerMinute => value,
    };

/// Unit tokens recognised in a column header, longest first so `kcal` is not
/// matched by `cal` and `lbs` is not matched by `lb`.
const List<(String, CsvUnit)> _kHeaderUnitTokens = [
  ('kilograms', CsvUnit.kilograms),
  ('kilogram', CsvUnit.kilograms),
  ('pounds', CsvUnit.pounds),
  ('ml/kg/min', CsvUnit.millilitersPerKgPerMinute),
  ('mmol/l', CsvUnit.millimolesPerLiter),
  ('mg/dl', CsvUnit.milligramsPerDeciliter),
  ('breaths/min', CsvUnit.breathsPerMinute),
  ('brpm', CsvUnit.breathsPerMinute),
  ('kcal', CsvUnit.kilocaloriesPerDay),
  ('kj', CsvUnit.kilojoulesPerDay),
  ('bpm', CsvUnit.beatsPerMinute),
  ('lbs', CsvUnit.pounds),
  ('lb', CsvUnit.pounds),
  ('st', CsvUnit.stones),
  ('kg', CsvUnit.kilograms),
  ('cm', CsvUnit.centimeters),
  ('ms', CsvUnit.milliseconds),
  ('°c', CsvUnit.celsius),
  ('°f', CsvUnit.fahrenheit),
  ('in', CsvUnit.inches),
  ('ft', CsvUnit.feet),
  ('%', CsvUnit.percent),
  ('c', CsvUnit.celsius),
  ('f', CsvUnit.fahrenheit),
  ('g', CsvUnit.grams),
  ('m', CsvUnit.meters),
  ('s', CsvUnit.seconds),
];

/// The unit named in [header], or null when it names none.
///
/// Reads a unit off a label the user has ALREADY chosen to map — it never maps a
/// header string to a metric, so this is a default, not a vendor preset. Only
/// the parenthesised tail is considered, so a column called "Weight in grams of
/// food" cannot be read as grams.
CsvUnit? detectCsvUnitInHeader(String header) {
  final match = RegExp(r'\(([^()]*)\)\s*$').firstMatch(header.trim());
  if (match == null) return null;
  final inner = match.group(1)!.trim().toLowerCase();
  if (inner.isEmpty) return null;
  for (final (token, unit) in _kHeaderUnitTokens) {
    if (inner == token) return unit;
  }
  return null;
}

/// [spec]'s offered interpretation whose unit is [unit], preferring a direct
/// reading over a derived one. Null when the metric cannot express that unit.
CsvValueInterpretation? interpretationForUnit(CsvMetricSpec spec, CsvUnit unit) {
  for (final interpretation in spec.interpretations) {
    if (interpretation is CsvDirectValue && interpretation.unit == unit) {
      return interpretation;
    }
  }
  for (final interpretation in spec.interpretations) {
    if (interpretation is CsvMassShareOfWeight && interpretation.unit == unit) {
      return interpretation;
    }
  }
  return null;
}
