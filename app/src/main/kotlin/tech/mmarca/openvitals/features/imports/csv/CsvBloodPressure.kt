package tech.mmarca.openvitals.features.imports.csv

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.units.Pressure
import java.util.Locale
import tech.mmarca.openvitals.domain.model.BpRecordValues

/**
 * Blood pressure in the CSV importer. Like a nutrient, a blood pressure
 * column is not a metric: a row's systolic and diastolic columns make one
 * reading, and the optional body position and cuff location columns describe
 * it. Pure, like the row converter.
 */

/** The Health Connect record class name. A segment of the clientRecordId. */
const val CSV_BLOOD_PRESSURE_TARGET_TYPE = "BloodPressureRecord"

/** The write permission any blood pressure column needs. */
val CsvBloodPressureWritePermission: String = HealthPermission.getWritePermission(BloodPressureRecord::class)

// Health Connect's own bounds, in mmHg: the record refuses to be built outside them.
private const val MinSystolicMmHg = 20.0
private const val MaxSystolicMmHg = 200.0
private const val MinDiastolicMmHg = 10.0
private const val MaxDiastolicMmHg = 180.0

/** What a blood pressure column holds. */
enum class CsvBloodPressurePart {
    /** In mmHg. Required, with [DIASTOLIC]. */
    SYSTOLIC,

    /** In mmHg. Required, with [SYSTOLIC]. */
    DIASTOLIC,

    /** A word such as "Sitting". Optional; a blank or unrecognised cell takes the default. */
    BODY_POSITION,

    /** A word such as "Left arm". Optional; a blank or unrecognised cell takes the default. */
    CUFF_LOCATION,
    ;

    /** Whether the reading needs this column, rather than being described by it. */
    val isPressure: Boolean get() = this == SYSTOLIC || this == DIASTOLIC
}

/**
 * The words a body position or cuff location cell is read against: each
 * option's label in the app language, then in English. Matching ignores case,
 * spacing and punctuation, so "left arm", "LEFT_ARM" and "Left arm" agree.
 */
data class CsvBloodPressureLabels(
    /** Label sets, most preferred first, each mapping a Health Connect constant to its label. */
    val bodyPositions: List<Map<Int, String>> = emptyList(),
    val cuffLocations: List<Map<Int, String>> = emptyList(),
) {
    private val bodyPositionLookup = csvLabelLookup(bodyPositions)
    private val cuffLocationLookup = csvLabelLookup(cuffLocations)

    /** The constant [text] names for [part], or null when it is none of its labels. */
    fun match(part: CsvBloodPressurePart, text: String): Int? = when (part) {
        CsvBloodPressurePart.BODY_POSITION -> bodyPositionLookup[normalizeCsvLabel(text)]
        CsvBloodPressurePart.CUFF_LOCATION -> cuffLocationLookup[normalizeCsvLabel(text)]
        CsvBloodPressurePart.SYSTOLIC, CsvBloodPressurePart.DIASTOLIC -> null
    }

    /** Every word [part] accepts, app language first, each once, for the mapping screen. */
    fun words(part: CsvBloodPressurePart): List<String> {
        val sets = when (part) {
            CsvBloodPressurePart.BODY_POSITION -> bodyPositions
            CsvBloodPressurePart.CUFF_LOCATION -> cuffLocations
            CsvBloodPressurePart.SYSTOLIC, CsvBloodPressurePart.DIASTOLIC -> emptyList()
        }
        return sets.flatMap { it.values }.distinctBy(::normalizeCsvLabel)
    }
}

/** Lowercase letters and digits only. */
internal fun normalizeCsvLabel(text: String): String =
    text.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

/** Normalised label to constant. A label two sets share keeps the earlier set's constant. */
private fun csvLabelLookup(labelSets: List<Map<Int, String>>): Map<String, Int> {
    val lookup = mutableMapOf<String, Int>()
    for (labels in labelSets) {
        for ((value, label) in labels) lookup.putIfAbsent(normalizeCsvLabel(label), value)
    }
    return lookup
}

/** How a row's blood pressure columns are read beyond the two numbers. */
data class CsvBloodPressureSettings(
    val labels: CsvBloodPressureLabels = CsvBloodPressureLabels(),
    /** For a reading whose file gives no body position, or none of its words. */
    val defaultBodyPosition: Int = BpRecordValues.BODY_POSITION_UNKNOWN,
    /** For a reading whose file gives no cuff location, or none of its words. */
    val defaultCuffLocation: Int = BpRecordValues.MEASUREMENT_LOCATION_UNKNOWN,
)

/**
 * The row's blood pressure reading, or null. Both pressure cells blank is a
 * gap, not an error. One of them blank, a value that is no number or outside
 * Health Connect's range, or a systolic not above the diastolic costs the
 * reading with one diagnostic, so one lost reading is one rejection. A body
 * position or cuff location never costs it: a blank or unrecognised cell takes
 * the default. [instant] is the row's resolved timestamp.
 */
internal fun convertCsvBloodPressureRow(
    row: CsvRow,
    mapping: CsvImportMapping,
    instant: CsvInstant,
    diagnostics: MutableList<CsvImportDiagnostic>,
): CsvConvertedRecord? {
    val systolicColumn = mapping.bloodPressureColumn(CsvBloodPressurePart.SYSTOLIC) ?: return null
    val diastolicColumn = mapping.bloodPressureColumn(CsvBloodPressurePart.DIASTOLIC) ?: return null
    val systolicText = row.cell(systolicColumn.columnIndex)
    val diastolicText = row.cell(diastolicColumn.columnIndex)
    if (systolicText == null && diastolicText == null) return null

    fun reject(reason: CsvImportDiagnosticReason, column: CsvColumnMapping, detail: String?): Nothing? {
        diagnostics += CsvImportDiagnostic(
            rowNumber = row.rowNumber,
            reason = reason,
            columnIndex = column.columnIndex,
            detail = detail,
        )
        return null
    }

    // Systolic first, so the one fault reported is the first in the row.
    fun pressure(column: CsvColumnMapping, text: String?, range: ClosedFloatingPointRange<Double>): Double? {
        if (text == null) return reject(CsvImportDiagnosticReason.MISSING_BLOOD_PRESSURE_VALUE, column, null)
        val value = parseCsvNumber(text) ?: return reject(CsvImportDiagnosticReason.UNPARSABLE_NUMBER, column, text)
        if (value !in range) {
            return reject(CsvImportDiagnosticReason.OUT_OF_RANGE, column, String.format(Locale.US, "%.2f", value))
        }
        return value
    }

    val systolic = pressure(systolicColumn, systolicText, MinSystolicMmHg..MaxSystolicMmHg) ?: return null
    val diastolic = pressure(diastolicColumn, diastolicText, MinDiastolicMmHg..MaxDiastolicMmHg) ?: return null
    if (systolic <= diastolic) {
        return reject(
            CsvImportDiagnosticReason.SYSTOLIC_NOT_ABOVE_DIASTOLIC,
            systolicColumn,
            String.format(Locale.US, "%.0f/%.0f", systolic, diastolic),
        )
    }

    val settings = mapping.bloodPressure
    return buildCsvBloodPressureRecord(
        systolic = systolic,
        diastolic = diastolic,
        bodyPosition = row.bloodPressureLabel(mapping, CsvBloodPressurePart.BODY_POSITION)
            ?: settings.defaultBodyPosition,
        cuffLocation = row.bloodPressureLabel(mapping, CsvBloodPressurePart.CUFF_LOCATION)
            ?: settings.defaultCuffLocation,
        instant = instant,
    )
}

/** The constant this row's [part] cell names, or null when the column is unmapped, the cell blank or unrecognised. */
private fun CsvRow.bloodPressureLabel(mapping: CsvImportMapping, part: CsvBloodPressurePart): Int? {
    val column = mapping.bloodPressureColumn(part) ?: return null
    val text = cell(column.columnIndex) ?: return null
    return mapping.bloodPressure.labels.match(part, text)
}

/** The reading at [instant]: pressures in mmHg, already validated, and Health Connect's position constants. */
fun buildCsvBloodPressureRecord(
    systolic: Double,
    diastolic: Double,
    bodyPosition: Int,
    cuffLocation: Int,
    instant: CsvInstant,
): CsvConvertedRecord {
    val clientRecordId = buildCsvClientRecordId(CSV_BLOOD_PRESSURE_TARGET_TYPE, instant.utc)
    return CsvConvertedRecord(
        metric = null,
        targetType = CSV_BLOOD_PRESSURE_TARGET_TYPE,
        recordType = BloodPressureRecord::class,
        clientRecordId = clientRecordId,
        instant = instant.utc,
        canonicalValue = null,
        record = BloodPressureRecord(
            time = instant.utc,
            zoneOffset = instant.offset,
            metadata = csvMetadata(clientRecordId),
            systolic = Pressure.millimetersOfMercury(systolic),
            diastolic = Pressure.millimetersOfMercury(diastolic),
            bodyPosition = bodyPosition,
            measurementLocation = cuffLocation,
        ),
    )
}

/**
 * The systolic and diastolic values, in mmHg, the sampled [rows] would be
 * written with, from the real conversion, so the preview shows what lands.
 */
fun previewBloodPressureValues(
    rows: List<List<String>>,
    mapping: CsvImportMapping,
): List<Pair<Double, Double>> =
    previewRecords(rows, mapping)
        .mapNotNull { it.record as? BloodPressureRecord }
        .map { it.systolic.inMillimetersOfMercury to it.diastolic.inMillimetersOfMercury }

/** What a body position or cuff location column's tile tells the user about its words. */
data class CsvBloodPressureLabelHint(
    /** Every word the column accepts. */
    val accepted: List<String>,
    /** The sampled cells that are none of them, so take the default. Each once, in file order. */
    val unrecognised: List<String>,
)

/** [column]'s hint over its sampled [values], or null when it holds no body position or cuff location. */
fun csvBloodPressureLabelHint(
    column: CsvColumnMapping,
    values: List<String>,
    labels: CsvBloodPressureLabels,
): CsvBloodPressureLabelHint? {
    val part = column.bloodPressurePart?.takeIf { column.isBloodPressure && !it.isPressure } ?: return null
    return CsvBloodPressureLabelHint(
        accepted = labels.words(part),
        unrecognised = values
            .map { it.trim() }
            .filter { it.isNotEmpty() && labels.match(part, it) == null }
            .distinct(),
    )
}
