package tech.mmarca.openvitals.features.imports.csv

import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Temperature
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.roundToLong
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient

/** One CSV row to the Health Connect records it represents. Pure: no I/O, no clock. */

/** The `clientRecordId` namespace for CSV imports. Distinct from the Apple importer's. */
const val CSV_CLIENT_RECORD_ID_NAMESPACE = "csv"

/** A record built from one CSV cell, or from a row's nutrient cells, with everything the import loop needs. */
data class CsvConvertedRecord(
    /** Null for a nutrition record, which no single metric makes. */
    val metric: CsvImportMetric?,
    /** Mirrors [CsvMetricSpec.targetType]; a segment of [clientRecordId]. */
    val targetType: String,
    val recordType: kotlin.reflect.KClass<out Record>,
    val clientRecordId: String,
    /** The measurement's instant, used to bound the duplicate-lookup window. */
    val instant: Instant,
    /** The value in the metric's canonical unit, for the confirm-step preview. Null for nutrition. */
    val canonicalValue: Double?,
    val record: Record,
    /** A nutrition record's amounts, grams or kcal, for the confirm-step preview. Empty for a metric. */
    val nutrients: Map<NutritionNutrient, Double> = emptyMap(),
)

/** What one row produced. */
data class CsvRowConversion(
    val records: List<CsvConvertedRecord> = emptyList(),
    val diagnostics: List<CsvImportDiagnostic> = emptyList(),
    /** The day's cycle journal entry, from a drip export; Room, not Health Connect. */
    val journalEntry: CycleJournalEntry? = null,
)

/**
 * Converts [row] under [mapping]. A bad timestamp costs the row, a bad value
 * only that metric or nutrient, a bad end timestamp only the interval records.
 * [nutritionOccurrences] numbers nutrition records across one run.
 */
fun convertCsvRow(
    row: CsvRow,
    mapping: CsvImportMapping,
    nutritionOccurrences: CsvNutritionOccurrences = CsvNutritionOccurrences(),
): CsvRowConversion {
    val timestampColumn = mapping.timestampColumn ?: return CsvRowConversion()

    val metricColumns = mapping.metricColumns
    if (!mapping.importsAnything) return CsvRowConversion()

    // "Too short" is relative to what the mapping reads: trailing empty columns
    // are normal. The end and food-name columns are not counted; a missing end
    // falls back to the one-minute span, a missing name to none.
    val endTimestampColumn = mapping.endTimestampColumn
    val readColumns = metricColumns + mapping.nutrientColumns + timestampColumn
    val highestIndex = readColumns.maxOf { it.columnIndex }
    if (row.fields.size <= highestIndex) {
        return CsvRowConversion(
            diagnostics = listOf(
                CsvImportDiagnostic(
                    rowNumber = row.rowNumber,
                    reason = CsvImportDiagnosticReason.WRONG_FIELD_COUNT,
                    detail = "${row.fields.size} fields, needs ${highestIndex + 1}",
                ),
            ),
        )
    }

    val timestampText = row.cell(timestampColumn.columnIndex)
        ?: return CsvRowConversion(
            diagnostics = listOf(
                CsvImportDiagnostic(
                    rowNumber = row.rowNumber,
                    reason = CsvImportDiagnosticReason.MISSING_TIMESTAMP,
                    columnIndex = timestampColumn.columnIndex,
                ),
            ),
        )

    val instant = resolveCsvInstant(timestampText, mapping.dateTime)
        ?: return CsvRowConversion(
            diagnostics = listOf(
                CsvImportDiagnostic(
                    rowNumber = row.rowNumber,
                    reason = CsvImportDiagnosticReason.UNPARSABLE_TIMESTAMP,
                    columnIndex = timestampColumn.columnIndex,
                    detail = timestampText,
                ),
            ),
        )

    // Resolved once per row: several mass-share metrics may need it.
    val rowWeightKg = resolveRowWeightKg(row, mapping)

    // A bad end costs only the interval metrics.
    val endText = endTimestampColumn?.let { row.cell(it.columnIndex) }
    val endInstant = endText?.let { resolveCsvInstant(it, mapping.dateTime) }

    val records = mutableListOf<CsvConvertedRecord>()
    val diagnostics = mutableListOf<CsvImportDiagnostic>()

    for (column in metricColumns) {
        val metric = column.metric!!
        val spec = CsvMetricCatalog[metric]
        val interpretation = column.effectiveInterpretation
        if (spec == null || interpretation == null) continue

        val intervalEnd = if (spec.isInterval) {
            val endColumnIndex = endTimestampColumn?.columnIndex ?: column.columnIndex
            csvIntervalEnd(row, instant, endText, endInstant, endColumnIndex, diagnostics) ?: continue
        } else {
            null
        }

        // A blank cell is a gap, not an error.
        val text = row.cell(column.columnIndex) ?: continue

        val raw = parseCsvNumber(text)
        if (raw == null) {
            diagnostics += CsvImportDiagnostic(
                rowNumber = row.rowNumber,
                reason = CsvImportDiagnosticReason.UNPARSABLE_NUMBER,
                columnIndex = column.columnIndex,
                detail = text,
            )
            continue
        }

        val canonical: Double = when (interpretation) {
            is CsvDirectValue -> convertCsvValueToCanonical(raw, interpretation.unit)
            is CsvMassShareOfWeight -> {
                if (rowWeightKg == null || rowWeightKg <= 0) {
                    diagnostics += CsvImportDiagnostic(
                        rowNumber = row.rowNumber,
                        reason = CsvImportDiagnosticReason.DERIVATION_MISSING_WEIGHT,
                        columnIndex = column.columnIndex,
                    )
                    continue
                }
                val massKg = convertCsvValueToCanonical(raw, interpretation.unit)
                massKg / rowWeightKg * 100
            }
        }

        if (canonical < spec.plausibleMin || canonical > spec.plausibleMax) {
            diagnostics += CsvImportDiagnostic(
                rowNumber = row.rowNumber,
                reason = CsvImportDiagnosticReason.OUT_OF_RANGE,
                columnIndex = column.columnIndex,
                detail = String.format(Locale.US, "%.2f", canonical),
            )
            continue
        }

        records += buildCsvImportRecord(
            metric = metric,
            value = canonical,
            instant = instant,
            end = intervalEnd,
        )
    }

    convertCsvNutritionRow(row, mapping, instant, endText, endInstant, nutritionOccurrences, diagnostics)
        ?.let { records += it }

    return CsvRowConversion(records = records, diagnostics = diagnostics)
}

/**
 * Where an interval record starting at [start] ends. No end named: a one-minute
 * span. An end that is stated but garbage, or not after the start, rejects:
 * null, with the reason added to [diagnostics] against [columnIndex].
 */
internal fun csvIntervalEnd(
    row: CsvRow,
    start: CsvInstant,
    endText: String?,
    endInstant: CsvInstant?,
    columnIndex: Int,
    diagnostics: MutableList<CsvImportDiagnostic>,
): CsvInstant? {
    val reason = when {
        endText == null -> return start.plusDefaultIntervalSpan()
        endInstant == null -> CsvImportDiagnosticReason.UNPARSABLE_END_TIMESTAMP
        !endInstant.utc.isAfter(start.utc) -> CsvImportDiagnosticReason.END_NOT_AFTER_START
        else -> return endInstant
    }
    diagnostics += CsvImportDiagnostic(
        rowNumber = row.rowNumber,
        reason = reason,
        columnIndex = columnIndex,
        detail = endText,
    )
    return null
}

/** The row's body weight in kg, or null. */
private fun resolveRowWeightKg(row: CsvRow, mapping: CsvImportMapping): Double? {
    val column = mapping.weightColumn ?: return null
    val text = row.cell(column.columnIndex) ?: return null
    val raw = parseCsvNumber(text) ?: return null
    val interpretation = column.effectiveInterpretation
    if (interpretation !is CsvDirectValue) return null
    return convertCsvValueToCanonical(raw, interpretation.unit)
}

/**
 * Builds the record for [metric] at [instant] from a canonical [value].
 * An interval metric also needs [end], already validated by the caller.
 */
fun buildCsvImportRecord(
    metric: CsvImportMetric,
    value: Double,
    instant: CsvInstant,
    end: CsvInstant? = null,
): CsvConvertedRecord {
    val spec = CsvMetricCatalog.getValue(metric)
    val clientRecordId = buildCsvClientRecordId(targetType = spec.targetType, utc = instant.utc)
    val metadata = csvMetadata(clientRecordId)
    val time = instant.utc
    val offset = instant.offset

    val record: Record = when (metric) {
        CsvImportMetric.WEIGHT -> WeightRecord(time, offset, Mass.kilograms(value), metadata)
        CsvImportMetric.BODY_FAT -> BodyFatRecord(
            time,
            offset,
            androidx.health.connect.client.units.Percentage(value),
            metadata,
        )
        CsvImportMetric.LEAN_BODY_MASS -> LeanBodyMassRecord(time, offset, Mass.kilograms(value), metadata)
        CsvImportMetric.BONE_MASS -> BoneMassRecord(time, offset, Mass.kilograms(value), metadata)
        CsvImportMetric.BODY_WATER_MASS -> BodyWaterMassRecord(time, offset, Mass.kilograms(value), metadata)
        CsvImportMetric.HEIGHT -> HeightRecord(time, offset, Length.meters(value), metadata)
        CsvImportMetric.BASAL_METABOLIC_RATE -> BasalMetabolicRateRecord(
            time,
            offset,
            Power.kilocaloriesPerDay(value),
            metadata,
        )
        // Health Connect models heart rate as a series: one sample, window of one instant.
        CsvImportMetric.HEART_RATE -> HeartRateRecord(
            startTime = time,
            startZoneOffset = offset,
            endTime = time,
            endZoneOffset = offset,
            samples = listOf(HeartRateRecord.Sample(time = time, beatsPerMinute = value.roundToLong())),
            metadata = metadata,
        )
        // Health Connect stores this one as an integer.
        CsvImportMetric.RESTING_HEART_RATE -> RestingHeartRateRecord(
            time = time,
            zoneOffset = offset,
            beatsPerMinute = value.roundToLong(),
            metadata = metadata,
        )
        CsvImportMetric.HEART_RATE_VARIABILITY -> HeartRateVariabilityRmssdRecord(
            time = time,
            zoneOffset = offset,
            heartRateVariabilityMillis = value,
            metadata = metadata,
        )
        CsvImportMetric.OXYGEN_SATURATION -> OxygenSaturationRecord(
            time,
            offset,
            androidx.health.connect.client.units.Percentage(value),
            metadata,
        )
        CsvImportMetric.RESPIRATORY_RATE -> RespiratoryRateRecord(time, offset, value, metadata)
        CsvImportMetric.BODY_TEMPERATURE -> BodyTemperatureRecord(
            time = time,
            zoneOffset = offset,
            temperature = Temperature.celsius(value),
            metadata = metadata,
        )
        CsvImportMetric.BASAL_BODY_TEMPERATURE -> BasalBodyTemperatureRecord(
            time = time,
            zoneOffset = offset,
            temperature = Temperature.celsius(value),
            metadata = metadata,
        )
        CsvImportMetric.BLOOD_GLUCOSE -> BloodGlucoseRecord(
            time = time,
            zoneOffset = offset,
            level = BloodGlucose.millimolesPerLiter(value),
            specimenSource = BloodGlucoseRecord.SPECIMEN_SOURCE_UNKNOWN,
            mealType = MealType.MEAL_TYPE_UNKNOWN,
            relationToMeal = BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN,
            metadata = metadata,
        )
        CsvImportMetric.VO2_MAX -> Vo2MaxRecord(
            time = time,
            zoneOffset = offset,
            vo2MillilitersPerMinuteKilogram = value,
            measurementMethod = Vo2MaxRecord.MEASUREMENT_METHOD_OTHER,
            metadata = metadata,
        )
        // The one interval metric. convertCsvRow guarantees end > start.
        CsvImportMetric.STEPS -> {
            val until = checkNotNull(end) { "STEPS needs an end instant." }
            StepsRecord(
                startTime = time,
                startZoneOffset = offset,
                endTime = until.utc,
                endZoneOffset = until.offset,
                count = value.roundToLong(),
                metadata = metadata,
            )
        }
    }

    return CsvConvertedRecord(
        metric = metric,
        targetType = spec.targetType,
        recordType = spec.recordType,
        clientRecordId = clientRecordId,
        instant = time,
        canonicalValue = value,
        record = record,
    )
}

/**
 * The span of an interval row when the file names no end. One minute: not
 * zero (Health Connect needs end > start), not a day. Flutter parity.
 */
private val DefaultIntervalSpan = Duration.ofMinutes(1)

private fun CsvInstant.plusDefaultIntervalSpan(): CsvInstant =
    CsvInstant(utc = utc.plus(DefaultIntervalSpan), offset = offset)

internal fun csvMetadata(clientRecordId: String): Metadata =
    Metadata.manualEntry(
        device = Device(type = Device.TYPE_PHONE),
        clientRecordId = clientRecordId,
    )

/**
 * The identity of a CSV-imported record: type and instant, not value, so a
 * corrected re-import replaces the old record. File name, header, unit and
 * mapping are excluded too. For an interval metric the instant is the start.
 * [occurrence] tells apart nutrition records that start at the same instant,
 * the second food of a meal; the first keeps the plain id.
 *
 * Byte-compatible with the Flutter build: hash input `"<targetType>|<epochMillis>"`,
 * SHA-256 truncated to 16 bytes, shape `csv_<slug>_<hex>`. Do not change.
 * Flutter had no nutrition, so a later occurrence appends `"|<occurrence>"`.
 */
fun buildCsvClientRecordId(targetType: String, utc: Instant, occurrence: Int = 0): String {
    val parts = if (occurrence == 0) {
        "$targetType|${utc.toEpochMilli()}"
    } else {
        "$targetType|${utc.toEpochMilli()}|$occurrence"
    }
    val bytes = MessageDigest.getInstance("SHA-256").digest(parts.toByteArray(Charsets.UTF_8))
    val digest = buildString(32) {
        for (index in 0 until 16) {
            val byte = bytes[index].toInt() and 0xFF
            append(HexDigits[byte ushr 4])
            append(HexDigits[byte and 0x0F])
        }
    }
    return "${CSV_CLIENT_RECORD_ID_NAMESPACE}_${toCsvStableIdSegment(targetType)}_$digest"
}

private const val HexDigits = "0123456789abcdef"

private val StableIdSegmentRegex = Regex("[^a-z0-9]+")

/** Slugifies [value]: lowercase, non-alphanumeric runs to `_`, trimmed. Matches Flutter. */
private fun toCsvStableIdSegment(value: String): String =
    value.lowercase(Locale.US)
        .replace(StableIdSegmentRegex, "_")
        .trim('_')
        .ifBlank { "record" }

/**
 * The canonical values [metric] would take across [rows], from the real
 * conversion, so the preview shows what will be written.
 */
fun previewCanonicalValues(
    rows: List<List<String>>,
    mapping: CsvImportMapping,
    metric: CsvImportMetric,
): List<Double> {
    if (metric !in CsvMetricCatalog) return emptyList()

    return previewRecords(rows, mapping)
        .filter { it.metric == metric }
        .mapNotNull { it.canonicalValue }
}

/** Every record the sampled [rows] convert to, through the import path. */
internal fun previewRecords(
    rows: List<List<String>>,
    mapping: CsvImportMapping,
): List<CsvConvertedRecord> {
    val occurrences = CsvNutritionOccurrences()
    return rows.flatMapIndexed { index, fields ->
        // +2: 1-based, and past the header row.
        convertCsvRow(
            row = CsvRow(rowNumber = index + 2, fields = fields),
            mapping = mapping,
            nutritionOccurrences = occurrences,
        ).records
    }
}

/**
 * The earliest and latest wall-clock times [rows] resolve to, or null. Wall
 * clock, so it reads the way the file reads. Resolved through the import
 * path, so a day/month mix-up shows up as a backwards span.
 */
fun previewInstantRange(
    rows: List<List<String>>,
    mapping: CsvImportMapping,
): Pair<LocalDateTime, LocalDateTime>? {
    val column = mapping.timestampColumn ?: return null

    var first: LocalDateTime? = null
    var last: LocalDateTime? = null
    for (fields in rows) {
        val text = fields.getOrNull(column.columnIndex)?.trim()?.takeIf { it.isNotEmpty() } ?: continue
        val instant = resolveCsvInstant(text, mapping.dateTime) ?: continue
        val wallClock = LocalDateTime.ofInstant(instant.utc, instant.offset)
        if (first == null || wallClock.isBefore(first)) first = wallClock
        if (last == null || wallClock.isAfter(last)) last = wallClock
    }

    val start = first ?: return null
    val end = last ?: return null
    return start to end
}

private val NonNumericCharsRegex = Regex("""[^0-9,.\-+eE]""")

/**
 * Parses a numeric cell, tolerating a comma decimal and thousands separators.
 * With both separators present, the last one is the decimal point.
 */
fun parseCsvNumber(text: String): Double? {
    var value = text.trim()
    if (value.isEmpty()) return null

    // Strip stray units, currency and spaces.
    value = value.replace(NonNumericCharsRegex, "")
    if (value.isEmpty()) return null

    val lastComma = value.lastIndexOf(',')
    val lastDot = value.lastIndexOf('.')
    if (lastComma >= 0 && lastDot >= 0) {
        value = if (lastComma > lastDot) {
            // 1.234,5 — dot groups, comma decides.
            value.replace(".", "").replaceFirst(",", ".")
        } else {
            // 1,234.5 — comma groups.
            value.replace(",", "")
        }
    } else if (lastComma >= 0) {
        // Only commas: a single one is the decimal point, several are grouping.
        value = if (value.indexOf(',') == lastComma) {
            value.replaceFirst(",", ".")
        } else {
            value.replace(",", "")
        }
    }

    return value.toDoubleOrNull()
}
