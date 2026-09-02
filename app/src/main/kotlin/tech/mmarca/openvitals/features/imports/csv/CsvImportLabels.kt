package tech.mmarca.openvitals.features.imports.csv

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R

/**
 * Localised labels for the CSV importer's enums.
 *
 * Kept out of the widgets so the screen renders labels and decides nothing, and
 * so every `when` here is exhaustive — adding a metric or a unit breaks the
 * build until it has a string.
 */

@Composable
internal fun csvMetricLabel(metric: CsvImportMetric): String = stringResource(
    when (metric) {
        CsvImportMetric.WEIGHT -> R.string.settings_csv_import_metric_weight
        CsvImportMetric.BODY_FAT -> R.string.settings_csv_import_metric_body_fat
        CsvImportMetric.LEAN_BODY_MASS -> R.string.settings_csv_import_metric_lean_body_mass
        CsvImportMetric.BONE_MASS -> R.string.settings_csv_import_metric_bone_mass
        CsvImportMetric.BODY_WATER_MASS -> R.string.settings_csv_import_metric_body_water_mass
        CsvImportMetric.HEIGHT -> R.string.settings_csv_import_metric_height
        CsvImportMetric.BASAL_METABOLIC_RATE -> R.string.settings_csv_import_metric_basal_metabolic_rate
        CsvImportMetric.HEART_RATE -> R.string.settings_csv_import_metric_heart_rate
        CsvImportMetric.RESTING_HEART_RATE -> R.string.settings_csv_import_metric_resting_heart_rate
        CsvImportMetric.HEART_RATE_VARIABILITY -> R.string.settings_csv_import_metric_heart_rate_variability
        CsvImportMetric.OXYGEN_SATURATION -> R.string.settings_csv_import_metric_oxygen_saturation
        CsvImportMetric.RESPIRATORY_RATE -> R.string.settings_csv_import_metric_respiratory_rate
        CsvImportMetric.BODY_TEMPERATURE -> R.string.settings_csv_import_metric_body_temperature
        CsvImportMetric.BASAL_BODY_TEMPERATURE -> R.string.settings_csv_import_metric_basal_body_temperature
        CsvImportMetric.BLOOD_GLUCOSE -> R.string.settings_csv_import_metric_blood_glucose
        CsvImportMetric.VO2_MAX -> R.string.settings_csv_import_metric_vo2_max
        CsvImportMetric.STEPS -> R.string.settings_csv_import_metric_steps
    },
)

@Composable
internal fun csvUnitLabel(unit: CsvUnit): String = stringResource(
    when (unit) {
        CsvUnit.KILOGRAMS -> R.string.settings_csv_import_unit_kilograms
        CsvUnit.POUNDS -> R.string.settings_csv_import_unit_pounds
        CsvUnit.STONES -> R.string.settings_csv_import_unit_stones
        CsvUnit.GRAMS -> R.string.settings_csv_import_unit_grams
        CsvUnit.PERCENT -> R.string.settings_csv_import_unit_percent
        CsvUnit.FRACTION -> R.string.settings_csv_import_unit_fraction
        CsvUnit.CENTIMETERS -> R.string.settings_csv_import_unit_centimeters
        CsvUnit.METERS -> R.string.settings_csv_import_unit_meters
        CsvUnit.INCHES -> R.string.settings_csv_import_unit_inches
        CsvUnit.FEET -> R.string.settings_csv_import_unit_feet
        CsvUnit.KILOCALORIES_PER_DAY -> R.string.settings_csv_import_unit_kilocalories_per_day
        CsvUnit.KILOJOULES_PER_DAY -> R.string.settings_csv_import_unit_kilojoules_per_day
        CsvUnit.CELSIUS -> R.string.settings_csv_import_unit_celsius
        CsvUnit.FAHRENHEIT -> R.string.settings_csv_import_unit_fahrenheit
        CsvUnit.BEATS_PER_MINUTE -> R.string.settings_csv_import_unit_beats_per_minute
        CsvUnit.MILLISECONDS -> R.string.settings_csv_import_unit_milliseconds
        CsvUnit.SECONDS -> R.string.settings_csv_import_unit_seconds
        CsvUnit.BREATHS_PER_MINUTE -> R.string.settings_csv_import_unit_breaths_per_minute
        CsvUnit.MILLIMOLES_PER_LITER -> R.string.settings_csv_import_unit_millimoles_per_liter
        CsvUnit.MILLIGRAMS_PER_DECILITER -> R.string.settings_csv_import_unit_milligrams_per_deciliter
        CsvUnit.MILLILITERS_PER_KG_PER_MINUTE -> R.string.settings_csv_import_unit_milliliters_per_kg_per_minute
        CsvUnit.COUNT -> R.string.settings_csv_import_unit_count
    },
)

/**
 * A direct value reads as its unit; a derived one says what it is derived from,
 * because "kg" alone on a body-fat column would be a lie.
 */
@Composable
internal fun csvInterpretationLabel(interpretation: CsvValueInterpretation): String =
    when (interpretation) {
        is CsvDirectValue -> csvUnitLabel(interpretation.unit)
        is CsvMassShareOfWeight -> stringResource(
            R.string.settings_csv_import_interpretation_mass_share,
            csvUnitLabel(interpretation.unit),
        )
    }

@Composable
internal fun csvDateFormatLabel(format: CsvDateTimeFormat): String = stringResource(
    when (format) {
        CsvDateTimeFormat.AUTO -> R.string.settings_csv_import_date_format_auto
        CsvDateTimeFormat.ISO_8601 -> R.string.settings_csv_import_date_format_iso
        CsvDateTimeFormat.YEAR_FIRST -> R.string.settings_csv_import_date_format_year_first
        CsvDateTimeFormat.DAY_FIRST -> R.string.settings_csv_import_date_format_day_first
        CsvDateTimeFormat.MONTH_FIRST -> R.string.settings_csv_import_date_format_month_first
        CsvDateTimeFormat.EPOCH_SECONDS -> R.string.settings_csv_import_date_format_epoch_seconds
        CsvDateTimeFormat.EPOCH_MILLIS -> R.string.settings_csv_import_date_format_epoch_millis
        CsvDateTimeFormat.CUSTOM -> R.string.settings_csv_import_date_format_custom
    },
)

@Composable
internal fun csvTimeZoneLabel(mode: CsvTimeZoneMode): String = stringResource(
    when (mode) {
        CsvTimeZoneMode.DEVICE -> R.string.settings_csv_import_time_zone_device
        CsvTimeZoneMode.UTC -> R.string.settings_csv_import_time_zone_utc
        CsvTimeZoneMode.FIXED_OFFSET -> R.string.settings_csv_import_time_zone_offset
    },
)

@Composable
internal fun csvSeparatorLabel(delimiter: String): String = stringResource(
    when (delimiter) {
        ";" -> R.string.settings_csv_import_separator_semicolon
        "\t" -> R.string.settings_csv_import_separator_tab
        "|" -> R.string.settings_csv_import_separator_pipe
        else -> R.string.settings_csv_import_separator_comma
    },
)

@Composable
internal fun csvIssueLabel(issue: CsvMappingIssue): String = stringResource(
    when (issue) {
        CsvMappingIssue.NO_TIMESTAMP_COLUMN -> R.string.settings_csv_import_issue_no_timestamp
        CsvMappingIssue.MULTIPLE_TIMESTAMP_COLUMNS -> R.string.settings_csv_import_issue_multiple_timestamps
        CsvMappingIssue.NO_METRIC_COLUMNS -> R.string.settings_csv_import_issue_no_metric
        CsvMappingIssue.DUPLICATE_METRIC -> R.string.settings_csv_import_issue_duplicate_metric
        CsvMappingIssue.MASS_SHARE_NEEDS_WEIGHT_COLUMN -> R.string.settings_csv_import_issue_needs_weight
        CsvMappingIssue.TIMESTAMP_FORMAT_MATCHES_NO_SAMPLE_ROW -> R.string.settings_csv_import_issue_timestamp_unparsed
        CsvMappingIssue.AMBIGUOUS_DAY_MONTH_ORDER -> R.string.settings_csv_import_date_format_ambiguous
        CsvMappingIssue.MULTIPLE_END_TIMESTAMP_COLUMNS -> R.string.settings_csv_import_issue_multiple_end_timestamps
    },
)

@Composable
internal fun csvDiagnosticReasonLabel(reason: CsvImportDiagnosticReason): String = stringResource(
    when (reason) {
        CsvImportDiagnosticReason.MISSING_TIMESTAMP -> R.string.settings_csv_import_reason_missing_timestamp
        CsvImportDiagnosticReason.UNPARSABLE_TIMESTAMP -> R.string.settings_csv_import_reason_unparsable_timestamp
        CsvImportDiagnosticReason.WRONG_FIELD_COUNT -> R.string.settings_csv_import_reason_wrong_field_count
        CsvImportDiagnosticReason.UNPARSABLE_NUMBER -> R.string.settings_csv_import_reason_unparsable_number
        CsvImportDiagnosticReason.OUT_OF_RANGE -> R.string.settings_csv_import_reason_out_of_range
        CsvImportDiagnosticReason.DERIVATION_MISSING_WEIGHT -> R.string.settings_csv_import_reason_derivation_missing_weight
        CsvImportDiagnosticReason.UNPARSABLE_END_TIMESTAMP -> R.string.settings_csv_import_reason_unparsable_end_timestamp
        CsvImportDiagnosticReason.END_NOT_AFTER_START -> R.string.settings_csv_import_reason_end_not_after_start
        CsvImportDiagnosticReason.WRITE_FAILED -> R.string.settings_csv_import_reason_write_failed
    },
)
