package tech.mmarca.openvitals.features.imports.csv

import java.util.Locale

/**
 * The text report a finished CSV import can hand back to the user.
 *
 * Pure: takes the run's inputs and outcome, returns a string. No clock, no I/O,
 * no context — so it is trivially testable and can be rendered from anywhere.
 *
 * Deliberately NOT sanitised, exactly like the Apple Health import report: it is
 * an explicit user export for troubleshooting, so it names the file, the columns
 * and the values that were rejected. It carries no health values that the user's
 * own CSV did not already contain.
 */

/** The suggested file name for a saved CSV import report. */
const val CSV_IMPORT_REPORT_FILE_NAME = "openvitals-csv-import-report.txt"

/**
 * Renders [result] and the mapping that produced it as plain text.
 *
 * The file name and dialect are passed in from the run rather than re-derived,
 * so the report describes what actually happened rather than what a fresh sniff
 * would decide now.
 */
fun buildCsvImportReport(
    fileName: String?,
    mapping: CsvImportMapping,
    result: CsvImportResult,
    headerRow: List<String>,
    fieldDelimiter: String? = null,
    hasHeaderRow: Boolean? = null,
): String = buildString {
    appendLine("OpenVitals CSV import report")
    appendLine("===========================")
    appendLine()
    appendLine("File: ${fileName ?: "(unnamed)"}")
    appendLine("Outcome: ${outcomeLabel(result.outcome)}")
    result.error?.let { appendLine("Error: $it") }

    appendLine()
    appendLine("Totals")
    appendLine("------")
    appendLine("Rows read:       ${result.progress.rowsRead}")
    appendLine("Records written: ${result.progress.written}")
    appendLine("Already present: ${result.progress.alreadyPresent}")
    appendLine("Rejected:        ${result.progress.rejected}")
    appendLine()
    appendLine("Parsing")
    appendLine("-------")
    if (fieldDelimiter != null) {
        appendLine("Separator:  ${delimiterLabel(fieldDelimiter)}")
    }
    if (hasHeaderRow != null) {
        appendLine("Header row: ${if (hasHeaderRow) "yes" else "no"}")
    }
    appendLine("Date format: ${mapping.dateTime.format.reportName}")
    appendLine("Time zone:   ${zoneLabel(mapping.dateTime)}")
    mapping.dateTime.customPattern?.takeIf { it.isNotEmpty() }?.let {
        appendLine("Custom pattern: $it")
    }

    appendLine()
    appendLine("Column mapping")
    appendLine("--------------")
    for (column in mapping.columns) {
        val header = headerRow.getOrNull(column.columnIndex) ?: "Column ${column.columnIndex + 1}"
        appendLine("[${column.columnIndex}] $header -> ${roleLabel(column)}")
    }

    if (result.diagnosticCounts.isNotEmpty()) {
        appendLine()
        appendLine("Rejections by reason")
        appendLine("--------------------")
        // Complete counts, never truncated — the per-row log below is what gets capped.
        for ((reason, count) in result.diagnosticCounts) {
            appendLine("${reasonLabel(reason)}: $count")
        }
    }

    if (result.diagnostics.isNotEmpty()) {
        val total = result.diagnosticCounts.values.sum()
        appendLine()
        appendLine("Rejected rows")
        appendLine("-------------")
        for (diagnostic in result.diagnostics) {
            val column = diagnostic.columnIndex?.let { " column $it" } ?: ""
            val detail = diagnostic.detail?.let { " ($it)" } ?: ""
            appendLine("Row ${diagnostic.rowNumber}$column: ${reasonLabel(diagnostic.reason)}$detail")
        }
        if (total > result.diagnostics.size) {
            appendLine(
                "... and ${total - result.diagnostics.size} more " +
                    "(the per-row log is capped at $CSV_MAX_RETAINED_DIAGNOSTICS; " +
                    "the counts above are complete).",
            )
        }
    }
}

/** The Flutter build's `format.name` spellings, so reports read the same. */
private val CsvDateTimeFormat.reportName: String
    get() = when (this) {
        CsvDateTimeFormat.AUTO -> "auto"
        CsvDateTimeFormat.ISO_8601 -> "iso8601"
        CsvDateTimeFormat.YEAR_FIRST -> "yearFirst"
        CsvDateTimeFormat.DAY_FIRST -> "dayFirst"
        CsvDateTimeFormat.MONTH_FIRST -> "monthFirst"
        CsvDateTimeFormat.EPOCH_SECONDS -> "epochSeconds"
        CsvDateTimeFormat.EPOCH_MILLIS -> "epochMillis"
        CsvDateTimeFormat.CUSTOM -> "custom"
    }

private val CsvImportMetric.reportName: String
    get() = when (this) {
        CsvImportMetric.WEIGHT -> "weight"
        CsvImportMetric.BODY_FAT -> "bodyFat"
        CsvImportMetric.LEAN_BODY_MASS -> "leanBodyMass"
        CsvImportMetric.BONE_MASS -> "boneMass"
        CsvImportMetric.BODY_WATER_MASS -> "bodyWaterMass"
        CsvImportMetric.HEIGHT -> "height"
        CsvImportMetric.BASAL_METABOLIC_RATE -> "basalMetabolicRate"
        CsvImportMetric.HEART_RATE -> "heartRate"
        CsvImportMetric.RESTING_HEART_RATE -> "restingHeartRate"
        CsvImportMetric.HEART_RATE_VARIABILITY -> "heartRateVariability"
        CsvImportMetric.OXYGEN_SATURATION -> "oxygenSaturation"
        CsvImportMetric.RESPIRATORY_RATE -> "respiratoryRate"
        CsvImportMetric.BODY_TEMPERATURE -> "bodyTemperature"
        CsvImportMetric.BASAL_BODY_TEMPERATURE -> "basalBodyTemperature"
        CsvImportMetric.BLOOD_GLUCOSE -> "bloodGlucose"
        CsvImportMetric.VO2_MAX -> "vo2Max"
        CsvImportMetric.STEPS -> "steps"
    }

private val CsvUnit.reportName: String
    get() = when (this) {
        CsvUnit.KILOGRAMS -> "kilograms"
        CsvUnit.POUNDS -> "pounds"
        CsvUnit.STONES -> "stones"
        CsvUnit.GRAMS -> "grams"
        CsvUnit.PERCENT -> "percent"
        CsvUnit.FRACTION -> "fraction"
        CsvUnit.CENTIMETERS -> "centimeters"
        CsvUnit.METERS -> "meters"
        CsvUnit.INCHES -> "inches"
        CsvUnit.FEET -> "feet"
        CsvUnit.KILOCALORIES_PER_DAY -> "kilocaloriesPerDay"
        CsvUnit.KILOJOULES_PER_DAY -> "kilojoulesPerDay"
        CsvUnit.CELSIUS -> "celsius"
        CsvUnit.FAHRENHEIT -> "fahrenheit"
        CsvUnit.BEATS_PER_MINUTE -> "beatsPerMinute"
        CsvUnit.MILLISECONDS -> "milliseconds"
        CsvUnit.SECONDS -> "seconds"
        CsvUnit.BREATHS_PER_MINUTE -> "breathsPerMinute"
        CsvUnit.MILLIMOLES_PER_LITER -> "millimolesPerLiter"
        CsvUnit.MILLIGRAMS_PER_DECILITER -> "milligramsPerDeciliter"
        CsvUnit.MILLILITERS_PER_KG_PER_MINUTE -> "millilitersPerKgPerMinute"
        CsvUnit.COUNT -> "count"
    }

private fun outcomeLabel(outcome: CsvImportOutcome): String = when (outcome) {
    CsvImportOutcome.COMPLETED -> "completed"
    CsvImportOutcome.CANCELLED -> "cancelled by the user"
    CsvImportOutcome.RATE_LIMITED -> "stopped — Health Connect rate limit"
    CsvImportOutcome.FAILED -> "failed"
}

private fun delimiterLabel(delimiter: String): String = when (delimiter) {
    "," -> "comma"
    ";" -> "semicolon"
    "\t" -> "tab"
    "|" -> "pipe"
    else -> delimiter
}

private fun zoneLabel(settings: CsvDateTimeSettings): String = when (settings.zone) {
    CsvTimeZoneMode.DEVICE -> "device time zone"
    CsvTimeZoneMode.UTC -> "UTC"
    CsvTimeZoneMode.FIXED_OFFSET -> "fixed offset ${offsetLabel(settings)}"
}

private fun offsetLabel(settings: CsvDateTimeSettings): String {
    val offset = settings.fixedOffset ?: return "+00:00"
    val totalMinutes = offset.totalSeconds / 60
    val sign = if (totalMinutes < 0) "-" else "+"
    val absolute = kotlin.math.abs(totalMinutes)
    return String.format(Locale.US, "%s%02d:%02d", sign, absolute / 60, absolute % 60)
}

private fun roleLabel(column: CsvColumnMapping): String = when (column.role) {
    CsvColumnRole.IGNORE -> "not imported"
    CsvColumnRole.TIMESTAMP -> "date and time"
    CsvColumnRole.END_TIMESTAMP -> "end date and time"
    CsvColumnRole.METRIC -> {
        val metric = column.metric
        if (metric == null) {
            "not imported"
        } else {
            "${metric.reportName} (${interpretationLabel(column.effectiveInterpretation)})"
        }
    }
}

private fun interpretationLabel(interpretation: CsvValueInterpretation?): String =
    when (interpretation) {
        is CsvDirectValue -> interpretation.unit.reportName
        is CsvMassShareOfWeight -> "${interpretation.unit.reportName} as a share of the weight column"
        null -> "default"
    }

private fun reasonLabel(reason: CsvImportDiagnosticReason): String = when (reason) {
    CsvImportDiagnosticReason.MISSING_TIMESTAMP -> "no date and time"
    CsvImportDiagnosticReason.UNPARSABLE_TIMESTAMP -> "date not understood"
    CsvImportDiagnosticReason.WRONG_FIELD_COUNT -> "too few columns"
    CsvImportDiagnosticReason.UNPARSABLE_NUMBER -> "value not a number"
    CsvImportDiagnosticReason.OUT_OF_RANGE -> "value outside a plausible range"
    CsvImportDiagnosticReason.DERIVATION_MISSING_WEIGHT -> "no weight to derive the percentage from"
    CsvImportDiagnosticReason.UNPARSABLE_END_TIMESTAMP -> "end date not understood"
    CsvImportDiagnosticReason.END_NOT_AFTER_START -> "end is not after the start"
    CsvImportDiagnosticReason.WRITE_FAILED -> "Health Connect refused the record"
}
