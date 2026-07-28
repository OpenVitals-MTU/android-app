/// Localised labels for the CSV importer's enums.
///
/// Kept out of the widgets so the screen renders labels and decides nothing, and
/// so every `switch` here is exhaustive — adding a metric or a unit breaks the
/// build until it has a string.
library;

import '../../../../l10n/app_localizations.dart';
import '../csv_column_mapping.dart';
import '../csv_datetime_format.dart';
import '../csv_import_metric.dart';
import '../csv_import_models.dart';

String csvMetricLabel(AppLocalizations l10n, CsvImportMetric metric) =>
    switch (metric) {
      CsvImportMetric.weight => l10n.settingsCsvImportMetricWeight,
      CsvImportMetric.bodyFat => l10n.settingsCsvImportMetricBodyFat,
      CsvImportMetric.leanBodyMass => l10n.settingsCsvImportMetricLeanBodyMass,
      CsvImportMetric.boneMass => l10n.settingsCsvImportMetricBoneMass,
      CsvImportMetric.bodyWaterMass => l10n.settingsCsvImportMetricBodyWaterMass,
      CsvImportMetric.height => l10n.settingsCsvImportMetricHeight,
      CsvImportMetric.basalMetabolicRate =>
        l10n.settingsCsvImportMetricBasalMetabolicRate,
      CsvImportMetric.heartRate => l10n.settingsCsvImportMetricHeartRate,
      CsvImportMetric.restingHeartRate =>
        l10n.settingsCsvImportMetricRestingHeartRate,
      CsvImportMetric.heartRateVariability =>
        l10n.settingsCsvImportMetricHeartRateVariability,
      CsvImportMetric.oxygenSaturation =>
        l10n.settingsCsvImportMetricOxygenSaturation,
      CsvImportMetric.respiratoryRate =>
        l10n.settingsCsvImportMetricRespiratoryRate,
      CsvImportMetric.bodyTemperature =>
        l10n.settingsCsvImportMetricBodyTemperature,
      CsvImportMetric.basalBodyTemperature =>
        l10n.settingsCsvImportMetricBasalBodyTemperature,
      CsvImportMetric.bloodGlucose => l10n.settingsCsvImportMetricBloodGlucose,
      CsvImportMetric.vo2Max => l10n.settingsCsvImportMetricVo2Max,
    };

String csvUnitLabel(AppLocalizations l10n, CsvUnit unit) => switch (unit) {
      CsvUnit.kilograms => l10n.settingsCsvImportUnitKilograms,
      CsvUnit.pounds => l10n.settingsCsvImportUnitPounds,
      CsvUnit.stones => l10n.settingsCsvImportUnitStones,
      CsvUnit.grams => l10n.settingsCsvImportUnitGrams,
      CsvUnit.percent => l10n.settingsCsvImportUnitPercent,
      CsvUnit.fraction => l10n.settingsCsvImportUnitFraction,
      CsvUnit.centimeters => l10n.settingsCsvImportUnitCentimeters,
      CsvUnit.meters => l10n.settingsCsvImportUnitMeters,
      CsvUnit.inches => l10n.settingsCsvImportUnitInches,
      CsvUnit.feet => l10n.settingsCsvImportUnitFeet,
      CsvUnit.kilocaloriesPerDay => l10n.settingsCsvImportUnitKilocaloriesPerDay,
      CsvUnit.kilojoulesPerDay => l10n.settingsCsvImportUnitKilojoulesPerDay,
      CsvUnit.celsius => l10n.settingsCsvImportUnitCelsius,
      CsvUnit.fahrenheit => l10n.settingsCsvImportUnitFahrenheit,
      CsvUnit.beatsPerMinute => l10n.settingsCsvImportUnitBeatsPerMinute,
      CsvUnit.milliseconds => l10n.settingsCsvImportUnitMilliseconds,
      CsvUnit.seconds => l10n.settingsCsvImportUnitSeconds,
      CsvUnit.breathsPerMinute => l10n.settingsCsvImportUnitBreathsPerMinute,
      CsvUnit.millimolesPerLiter =>
        l10n.settingsCsvImportUnitMillimolesPerLiter,
      CsvUnit.milligramsPerDeciliter =>
        l10n.settingsCsvImportUnitMilligramsPerDeciliter,
      CsvUnit.millilitersPerKgPerMinute =>
        l10n.settingsCsvImportUnitMillilitersPerKgPerMinute,
    };

/// A direct value reads as its unit; a derived one says what it is derived from,
/// because "kg" alone on a body-fat column would be a lie.
String csvInterpretationLabel(
  AppLocalizations l10n,
  CsvValueInterpretation interpretation,
) =>
    switch (interpretation) {
      CsvDirectValue(:final unit) => csvUnitLabel(l10n, unit),
      CsvMassShareOfWeight(:final unit) =>
        l10n.settingsCsvImportInterpretationMassShare(
          csvUnitLabel(l10n, unit),
        ),
    };

String csvDateFormatLabel(AppLocalizations l10n, CsvDateTimeFormat format) =>
    switch (format) {
      CsvDateTimeFormat.auto => l10n.settingsCsvImportDateFormatAuto,
      CsvDateTimeFormat.iso8601 => l10n.settingsCsvImportDateFormatIso,
      CsvDateTimeFormat.yearFirst => l10n.settingsCsvImportDateFormatYearFirst,
      CsvDateTimeFormat.dayFirst => l10n.settingsCsvImportDateFormatDayFirst,
      CsvDateTimeFormat.monthFirst => l10n.settingsCsvImportDateFormatMonthFirst,
      CsvDateTimeFormat.epochSeconds =>
        l10n.settingsCsvImportDateFormatEpochSeconds,
      CsvDateTimeFormat.epochMillis =>
        l10n.settingsCsvImportDateFormatEpochMillis,
      CsvDateTimeFormat.custom => l10n.settingsCsvImportDateFormatCustom,
    };

String csvTimeZoneLabel(AppLocalizations l10n, CsvTimeZoneMode mode) =>
    switch (mode) {
      CsvTimeZoneMode.device => l10n.settingsCsvImportTimeZoneDevice,
      CsvTimeZoneMode.utc => l10n.settingsCsvImportTimeZoneUtc,
      CsvTimeZoneMode.fixedOffset => l10n.settingsCsvImportTimeZoneOffset,
    };

String csvSeparatorLabel(AppLocalizations l10n, String delimiter) =>
    switch (delimiter) {
      ';' => l10n.settingsCsvImportSeparatorSemicolon,
      '\t' => l10n.settingsCsvImportSeparatorTab,
      '|' => l10n.settingsCsvImportSeparatorPipe,
      _ => l10n.settingsCsvImportSeparatorComma,
    };

String csvIssueLabel(AppLocalizations l10n, CsvMappingIssue issue) =>
    switch (issue) {
      CsvMappingIssue.noTimestampColumn =>
        l10n.settingsCsvImportIssueNoTimestamp,
      CsvMappingIssue.multipleTimestampColumns =>
        l10n.settingsCsvImportIssueMultipleTimestamps,
      CsvMappingIssue.noMetricColumns => l10n.settingsCsvImportIssueNoMetric,
      CsvMappingIssue.duplicateMetric =>
        l10n.settingsCsvImportIssueDuplicateMetric,
      CsvMappingIssue.massShareNeedsWeightColumn =>
        l10n.settingsCsvImportIssueNeedsWeight,
      CsvMappingIssue.timestampFormatMatchesNoSampleRow =>
        l10n.settingsCsvImportIssueTimestampUnparsed,
      CsvMappingIssue.ambiguousDayMonthOrder =>
        l10n.settingsCsvImportDateFormatAmbiguous,
    };

String csvDiagnosticReasonLabel(
  AppLocalizations l10n,
  CsvImportDiagnosticReason reason,
) =>
    switch (reason) {
      CsvImportDiagnosticReason.missingTimestamp =>
        l10n.settingsCsvImportReasonMissingTimestamp,
      CsvImportDiagnosticReason.unparsableTimestamp =>
        l10n.settingsCsvImportReasonUnparsableTimestamp,
      CsvImportDiagnosticReason.wrongFieldCount =>
        l10n.settingsCsvImportReasonWrongFieldCount,
      CsvImportDiagnosticReason.unparsableNumber =>
        l10n.settingsCsvImportReasonUnparsableNumber,
      CsvImportDiagnosticReason.outOfRange =>
        l10n.settingsCsvImportReasonOutOfRange,
      CsvImportDiagnosticReason.derivationMissingWeight =>
        l10n.settingsCsvImportReasonDerivationMissingWeight,
      CsvImportDiagnosticReason.writeFailed =>
        l10n.settingsCsvImportReasonWriteFailed,
    };
