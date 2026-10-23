package tech.mmarca.openvitals.features.imports.csv

import java.util.Locale
import tech.mmarca.openvitals.domain.model.BpRecordValues
import tech.mmarca.openvitals.domain.model.NutritionNutrient

/**
 * The text report of a finished CSV import, to copy, save or send. Pure.
 * Structure only: counts, roles, units, reasons, row and column numbers, and
 * the shape of a cell that could not be read. Never a cell's text, a value,
 * the file's name or its header names.
 */

/** The suggested file name for a saved CSV import report. */
const val CSV_IMPORT_REPORT_FILE_NAME = "openvitals-csv-import-report.txt"

/** Renders [result] as text. The dialect comes from the run, not a fresh sniff. */
fun buildCsvImportReport(
    /** The column mapping, or null for a drip export read through [drip]. */
    mapping: CsvImportMapping?,
    result: CsvImportResult,
    fieldDelimiter: String? = null,
    hasHeaderRow: Boolean? = null,
    drip: DripImportPlan? = null,
): String = buildString {
    appendLine("OpenVitals CSV import report")
    appendLine("===========================")
    appendLine()
    appendLine("Outcome: ${outcomeLabel(result.outcome)}")
    result.error?.let { appendLine("Error: $it") }

    appendLine()
    appendLine("Totals")
    appendLine("------")
    // Rows partition three ways and add up to rows read. Records do not: a row can make several.
    val progress = result.progress
    appendLine("Rows read:           ${progress.rowsRead}")
    appendLine("  Imported:          ${progress.rowsImported} (rows that produced a record or a journal entry)")
    appendLine("  Rejected:          ${progress.rowsRejected}")
    appendLine("  Nothing to import: ${progress.emptyRows}")
    appendLine("Records written:     ${progress.written}, of which ${progress.alreadyPresent} replaced an earlier import")
    if (progress.presenceUnchecked > 0) {
        appendLine("  Not checked first: ${progress.presenceUnchecked} (Health Connect could not be read for them; written all the same)")
    }
    if (progress.journalDays > 0) {
        appendLine("Journal days:        ${progress.journalDays} (${progress.journalDaysUpdated} updated)")
    }
    appendLine("Rejections:          ${progress.rejected} (whole rows and single values, by reason below)")
    if (progress.quotaPauses > 0) {
        appendLine("Quota pauses:        ${progress.quotaPauses} (${quotaWaitLabel(progress.quotaWaitMillis)})")
    }
    appendLine()
    appendLine("Parsing")
    appendLine("-------")
    if (fieldDelimiter != null) {
        appendLine("Separator:  ${delimiterLabel(fieldDelimiter)}")
    }
    if (hasHeaderRow != null) {
        appendLine("Header row: ${if (hasHeaderRow) "yes" else "no"}")
    }
    if (mapping != null) {
        appendLine("Date format: ${mapping.dateTime.format.reportName}")
        appendLine("Time zone:   ${zoneLabel(mapping.dateTime)}")
        mapping.dateTime.customPattern?.takeIf { it.isNotEmpty() }?.let {
            appendLine("Custom pattern: $it")
        }
        // What a reading took when its file gave no position or location, or an unrecognised one.
        if (mapping.bloodPressureColumns.isNotEmpty()) {
            appendLine("Default body position: ${bodyPositionReportLabel(mapping.bloodPressure.defaultBodyPosition)}")
            appendLine("Default cuff location: ${cuffLocationReportLabel(mapping.bloodPressure.defaultCuffLocation)}")
        }
    }
    if (drip != null) {
        appendLine("Format:     drip export")
        appendLine("Time zone:  ${drip.zone.id}")
    }

    if (drip != null) {
        appendLine()
        appendLine("Imported from drip")
        appendLine("------------------")
        for (option in DripImportOption.entries) {
            appendLine("${dripOptionReportLabel(option)}: ${if (option in drip.enabled) "yes" else "no"}")
        }
    } else if (mapping != null) {
        appendLine()
        appendLine("Column mapping")
        appendLine("--------------")
        for (column in mapping.columns) {
            appendLine("[${column.columnIndex}] -> ${roleLabel(column)}")
        }
    }

    if (result.diagnosticCounts.isNotEmpty()) {
        appendLine()
        appendLine("Rejections by reason")
        appendLine("--------------------")
        // Complete counts; the per-row log below is what gets capped.
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

/** The nutrient's name in camel case, like the metrics: VITAMIN_B12 reads "vitaminB12". */
private val NutritionNutrient.reportName: String
    get() = name.lowercase(Locale.US)
        .split('_')
        .mapIndexed { index, word -> if (index == 0) word else word.replaceFirstChar { it.titlecase(Locale.US) } }
        .joinToString("")

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
        CsvUnit.KILOCALORIES -> "kilocalories"
        CsvUnit.KILOJOULES -> "kilojoules"
        CsvUnit.MILLIGRAMS -> "milligrams"
        CsvUnit.MICROGRAMS -> "micrograms"
    }

private fun dripOptionReportLabel(option: DripImportOption): String = when (option) {
    DripImportOption.BLEEDING -> "Bleeding"
    DripImportOption.TEMPERATURE -> "Basal body temperature"
    DripImportOption.CERVICAL_MUCUS -> "Cervical mucus"
    DripImportOption.SEXUAL_ACTIVITY -> "Sexual activity"
    DripImportOption.JOURNAL -> "Journal (pain, mood, cervix, desire, notes)"
}

private fun outcomeLabel(outcome: CsvImportOutcome): String = when (outcome) {
    CsvImportOutcome.COMPLETED -> "completed"
    CsvImportOutcome.CANCELLED -> "cancelled by the user"
    CsvImportOutcome.RATE_LIMITED -> "stopped — Health Connect rate limit"
    CsvImportOutcome.FAILED -> "failed"
}

/** "waited 12 min 30 s", so a slow run explains its own duration. */
private fun quotaWaitLabel(waitMillis: Long): String {
    val totalSeconds = waitMillis / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return when {
        minutes == 0L -> "waited $seconds s"
        seconds == 0L -> "waited $minutes min"
        else -> "waited $minutes min $seconds s"
    }
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
    CsvColumnRole.FOOD_NAME -> "food name"
    CsvColumnRole.NUTRIENT -> {
        val nutrient = column.nutrient
        if (nutrient == null) {
            "not imported"
        } else {
            "nutrient ${nutrient.reportName} (${interpretationLabel(column.effectiveInterpretation)})"
        }
    }
    CsvColumnRole.METRIC -> {
        val metric = column.metric
        if (metric == null) {
            "not imported"
        } else {
            "${metric.reportName} (${interpretationLabel(column.effectiveInterpretation)})"
        }
    }
    CsvColumnRole.BLOOD_PRESSURE -> when (column.bloodPressurePart) {
        CsvBloodPressurePart.SYSTOLIC -> "blood pressure systolic (millimetersOfMercury)"
        CsvBloodPressurePart.DIASTOLIC -> "blood pressure diastolic (millimetersOfMercury)"
        CsvBloodPressurePart.BODY_POSITION -> "blood pressure body position"
        CsvBloodPressurePart.CUFF_LOCATION -> "blood pressure cuff location"
        null -> "not imported"
    }
}

/** A default as the report names it: Health Connect's constant names, in words. */
private fun bodyPositionReportLabel(position: Int): String = when (position) {
    BpRecordValues.BODY_POSITION_STANDING_UP -> "standing up"
    BpRecordValues.BODY_POSITION_SITTING_DOWN -> "sitting down"
    BpRecordValues.BODY_POSITION_LYING_DOWN -> "lying down"
    BpRecordValues.BODY_POSITION_RECLINING -> "reclining"
    else -> "not specified"
}

private fun cuffLocationReportLabel(location: Int): String = when (location) {
    BpRecordValues.MEASUREMENT_LOCATION_LEFT_WRIST -> "left wrist"
    BpRecordValues.MEASUREMENT_LOCATION_RIGHT_WRIST -> "right wrist"
    BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM -> "left upper arm"
    BpRecordValues.MEASUREMENT_LOCATION_RIGHT_UPPER_ARM -> "right upper arm"
    else -> "not specified"
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
    CsvImportDiagnosticReason.MISSING_BLOOD_PRESSURE_VALUE -> "systolic or diastolic missing"
    CsvImportDiagnosticReason.SYSTOLIC_NOT_ABOVE_DIASTOLIC -> "systolic not above diastolic"
    CsvImportDiagnosticReason.WRITE_FAILED -> "Health Connect refused the record"
    CsvImportDiagnosticReason.EXCLUDED_IN_SOURCE -> "Excluded in the source app"
}
