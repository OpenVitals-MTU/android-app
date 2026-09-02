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

/**
 * One CSV row → the Health Connect records it represents.
 *
 * Pure and synchronous: no repository, no clock, no I/O. Everything the
 * conversion needs is the row, the mapping, and the catalog.
 */

/**
 * The namespace every CSV-imported record's `clientRecordId` carries.
 *
 * Distinct from the Apple importer's so the two can never collide on the same
 * id and silently overwrite each other's records.
 */
const val CSV_CLIENT_RECORD_ID_NAMESPACE = "csv"

/** A record built from one CSV cell, with everything the import loop needs. */
data class CsvConvertedRecord(
    val metric: CsvImportMetric,
    /** Mirrors [CsvMetricSpec.targetType]; a segment of [clientRecordId]. */
    val targetType: String,
    val recordType: kotlin.reflect.KClass<out Record>,
    val clientRecordId: String,
    /** The measurement's instant, used to bound the duplicate-lookup window. */
    val instant: Instant,
    /** The value in the metric's canonical unit, for the confirm-step preview. */
    val canonicalValue: Double,
    val record: Record,
)

/** What one row produced. */
data class CsvRowConversion(
    val records: List<CsvConvertedRecord> = emptyList(),
    val diagnostics: List<CsvImportDiagnostic> = emptyList(),
)

/**
 * Converts [row] under [mapping].
 *
 * Failure granularity matches the Apple importer: a bad **timestamp** or a row
 * too short costs the whole row, because nothing in it can be placed in time; a
 * bad **value** costs only that metric, so one unparsable body-fat cell does
 * not throw away a perfectly good weight. A bad **end timestamp** sits in
 * between: it costs the interval metrics only, because the instant metrics
 * beside it are still perfectly placed in time by the start alone.
 */
fun convertCsvRow(
    row: CsvRow,
    mapping: CsvImportMapping,
): CsvRowConversion {
    val timestampColumn = mapping.timestampColumn ?: return CsvRowConversion()

    val metricColumns = mapping.metricColumns
    if (metricColumns.isEmpty()) return CsvRowConversion()

    // A row is "too short" only relative to what the mapping actually reads.
    // Trailing empty columns are normal in exports and must not reject the row.
    // The end-timestamp column is deliberately NOT counted: a row truncated
    // before it reads as a blank end and falls back to the one-minute span,
    // the same treatment a blank end cell gets — not a rejected row.
    val endTimestampColumn = mapping.endTimestampColumn
    val highestIndex = (metricColumns.map { it.columnIndex } + timestampColumn.columnIndex).max()
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

    // Resolved once per row, before the loop, because any number of mass-share
    // metrics can need it.
    val rowWeightKg = resolveRowWeightKg(row, mapping)

    // The end timestamp too — but a bad end costs only the interval metrics,
    // never the row: the instant metrics beside it are still perfectly placed
    // in time by the start alone.
    val endText = endTimestampColumn?.let { row.cell(it.columnIndex) }
    val endInstant = endText?.let { resolveCsvInstant(it, mapping.dateTime) }

    val records = mutableListOf<CsvConvertedRecord>()
    val diagnostics = mutableListOf<CsvImportDiagnostic>()

    for (column in metricColumns) {
        val metric = column.metric!!
        val spec = CsvMetricCatalog[metric]
        val interpretation = column.effectiveInterpretation
        if (spec == null || interpretation == null) continue

        // A row that names no end — the column unmapped, or its cell blank —
        // defaults to a one-minute span: the total stays pinned at its start
        // without claiming a duration the file never stated. An end the file
        // DOES state but that is garbage, or not after the start, still
        // rejects: silently re-timing stated data is worse than skipping it.
        var intervalEnd: CsvInstant? = null
        if (spec.isInterval) {
            val reason = when {
                endText == null -> null
                endInstant == null -> CsvImportDiagnosticReason.UNPARSABLE_END_TIMESTAMP
                !endInstant.utc.isAfter(instant.utc) -> CsvImportDiagnosticReason.END_NOT_AFTER_START
                else -> null
            }
            if (reason != null) {
                diagnostics += CsvImportDiagnostic(
                    rowNumber = row.rowNumber,
                    reason = reason,
                    columnIndex = endTimestampColumn?.columnIndex ?: column.columnIndex,
                    detail = endText,
                )
                continue
            }
            intervalEnd = endInstant ?: instant.plusDefaultIntervalSpan()
        }

        // A blank cell is a gap in the data, not an error: scales skip metrics.
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

    return CsvRowConversion(records = records, diagnostics = diagnostics)
}

/**
 * The row's body weight in kg, or null when the mapping has no weight column,
 * the cell is blank, or it does not parse.
 */
private fun resolveRowWeightKg(row: CsvRow, mapping: CsvImportMapping): Double? {
    val column = mapping.weightColumn ?: return null
    val text = row.cell(column.columnIndex) ?: return null
    val raw = parseCsvNumber(text) ?: return null
    val interpretation = column.effectiveInterpretation
    if (interpretation !is CsvDirectValue) return null
    return convertCsvValueToCanonical(raw, interpretation.unit)
}

/**
 * Builds the record for [metric] at [instant] from an already-canonical
 * [value]. An interval metric additionally needs [end], the caller-validated
 * end of its span; instant metrics ignore it.
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
        // Health Connect models heart rate as a SERIES, not an instant. A CSV
        // row is one spot reading, so it becomes a one-sample series whose
        // window is the single instant — the same shape the Apple importer
        // builds for a lone heart-rate sample.
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
        // The one interval metric: the span is the row's own TimeFrom..TimeTo.
        // convertCsvRow guarantees the end exists and lies after the start.
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
 * What an interval row spans when the file names no end. One minute, not zero
 * (Health Connect requires end > start) and not a day (that would claim a
 * duration the file never stated). Part of the Flutter-parity contract: both
 * apps must build the same record from the same file.
 */
private val DefaultIntervalSpan = Duration.ofMinutes(1)

private fun CsvInstant.plusDefaultIntervalSpan(): CsvInstant =
    CsvInstant(utc = utc.plus(DefaultIntervalSpan), offset = offset)

private fun csvMetadata(clientRecordId: String): Metadata =
    Metadata.manualEntry(
        device = Device(type = Device.TYPE_PHONE),
        clientRecordId = clientRecordId,
    )

/**
 * The identity of a CSV-imported record: **record type and instant, not value.**
 *
 * Health Connect upserts on `clientRecordId`, so leaving the value out is what
 * makes re-importing a corrected file REPLACE the old record instead of leaving
 * two weights at the same instant. The cost is that two genuinely different
 * measurements at the identical instant collapse to one — for a scale, the
 * right trade.
 *
 * Also deliberately excluded: the file name, the column header, the unit chosen
 * and the mapping. Re-exporting the same history with the columns reordered, or
 * in pounds instead of kilograms, resolves to the same records.
 *
 * For an interval metric the instant is the interval's START, and the end is
 * excluded exactly like the value: a re-export that corrects a bucket's end
 * time replaces the record rather than duplicating it.
 *
 * BYTE-COMPATIBLE with the Flutter build's `buildCsvClientRecordId`: users who
 * imported through that build must dedup against these exact ids, so the hash
 * input (`"<targetType>|<epochMillis>"`), the SHA-256 truncation to 16 bytes,
 * and the `csv_<slug>_<hex>` shape must not change.
 */
fun buildCsvClientRecordId(targetType: String, utc: Instant): String {
    val parts = "$targetType|${utc.toEpochMilli()}"
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

/**
 * Slugifies [value] for the middle id segment: lowercased, every run of
 * non-alphanumerics collapsed to `_`, no leading or trailing `_`. Matches the
 * Apple importer's `toStableIdSegment` and the Flutter build's slug.
 */
private fun toCsvStableIdSegment(value: String): String =
    value.lowercase(Locale.US)
        .replace(StableIdSegmentRegex, "_")
        .trim('_')
        .ifBlank { "record" }

/**
 * The canonical values [metric] would take across [rows].
 *
 * Runs the REAL conversion, derivations and plausibility rejections included,
 * so the range the confirm step shows is the range that will actually be
 * written — which is the whole point of showing it. A fat-mass column divided
 * by the wrong weight column surfaces here as 3% or 150%.
 */
fun previewCanonicalValues(
    rows: List<List<String>>,
    mapping: CsvImportMapping,
    metric: CsvImportMetric,
): List<Double> {
    val targetType = CsvMetricCatalog[metric]?.targetType ?: return emptyList()

    val values = mutableListOf<Double>()
    rows.forEachIndexed { index, fields ->
        // +2: 1-based, and past the header row.
        val conversion = convertCsvRow(
            row = CsvRow(rowNumber = index + 2, fields = fields),
            mapping = mapping,
        )
        conversion.records
            .filter { it.targetType == targetType }
            .forEach { values += it.canonicalValue }
    }
    return values
}

/**
 * The earliest and latest wall-clock times [rows] resolve to under [mapping],
 * or null when none of them parse.
 *
 * Wall clock, not the UTC instant: this is shown to the user to check against
 * the dates they can see in their own file, so it has to read the way the file
 * reads. The offset is added back for exactly that reason.
 *
 * Resolved through [resolveCsvInstant], the same path the import takes, so the
 * span cannot disagree with what gets written. It is the guard the single-row
 * echo cannot be: a day/month mix-up that happens to leave row 1 plausible
 * shows up here as a span running to the wrong month, or backwards.
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
 * Parses a numeric cell, tolerating a comma decimal separator and thousands
 * separators.
 *
 * A semicolon-delimited European export writes `78,4`; reading that as 784 or
 * as null would both be wrong. Only unambiguous shapes are accepted — a value
 * containing both separators is read with the LAST one as the decimal point.
 */
fun parseCsvNumber(text: String): Double? {
    var value = text.trim()
    if (value.isEmpty()) return null

    // Strip anything that is not part of a number (stray units, currency, spaces).
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
        // Only commas. Treat a single one as the decimal point; several means
        // thousands grouping.
        value = if (value.indexOf(',') == lastComma) {
            value.replaceFirst(",", ".")
        } else {
            value.replace(",", "")
        }
    }

    return value.toDoubleOrNull()
}
