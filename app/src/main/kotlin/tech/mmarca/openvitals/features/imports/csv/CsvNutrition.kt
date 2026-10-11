package tech.mmarca.openvitals.features.imports.csv

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import java.time.Instant
import java.util.Locale
import kotlin.math.pow
import tech.mmarca.openvitals.domain.model.NutrientLabelUnit
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionNutrientUnit
import tech.mmarca.openvitals.domain.model.labelUnit
import tech.mmarca.openvitals.healthconnect.nutritionRecordOf

/**
 * Nutrition in the CSV importer. A nutrient is not a metric: every nutrient
 * column of a row folds into one nutrition record, a food or a day's totals,
 * spanning the row's start to its end, or one minute without an end column.
 * Pure, like the row converter.
 */

/** The Health Connect record class name. A segment of the clientRecordId. */
const val CSV_NUTRITION_TARGET_TYPE = "NutritionRecord"

/** The write permission any nutrient column needs. */
val CsvNutritionWritePermission: String = HealthPermission.getWritePermission(NutritionRecord::class)

/**
 * The units a nutrient column can be written in. The first, the default, is
 * the one nutrition labels print and the food form types in.
 */
fun csvNutrientUnits(nutrient: NutritionNutrient): List<CsvUnit> = when (nutrient.labelUnit) {
    NutrientLabelUnit.KCAL -> listOf(CsvUnit.KILOCALORIES, CsvUnit.KILOJOULES)
    NutrientLabelUnit.GRAM -> listOf(CsvUnit.GRAMS, CsvUnit.MILLIGRAMS, CsvUnit.MICROGRAMS)
    NutrientLabelUnit.MILLIGRAM -> listOf(CsvUnit.MILLIGRAMS, CsvUnit.MICROGRAMS, CsvUnit.GRAMS)
    NutrientLabelUnit.MICROGRAM -> listOf(CsvUnit.MICROGRAMS, CsvUnit.MILLIGRAMS, CsvUnit.GRAMS)
}

/** [csvNutrientUnits] as the interpretations the column editor offers. */
fun csvNutrientInterpretations(nutrient: NutritionNutrient): List<CsvValueInterpretation> =
    csvNutrientUnits(nutrient).map(::CsvDirectValue)

fun csvNutrientDefaultInterpretation(nutrient: NutritionNutrient): CsvValueInterpretation =
    CsvDirectValue(csvNutrientUnits(nutrient).first())

/**
 * [nutrient]'s interpretation for a unit read off its header, or null. The
 * header's kcal is read as a daily rate, for basal metabolic rate; for a
 * nutrient it is an amount of energy.
 */
fun csvNutrientInterpretationForUnit(nutrient: NutritionNutrient, unit: CsvUnit): CsvValueInterpretation? {
    val amount = when (unit) {
        CsvUnit.KILOCALORIES_PER_DAY -> CsvUnit.KILOCALORIES
        CsvUnit.KILOJOULES_PER_DAY -> CsvUnit.KILOJOULES
        else -> unit
    }
    return CsvDirectValue(amount).takeIf { amount in csvNutrientUnits(nutrient) }
}

/** [value] in [unit] as Health Connect stores it: grams, or kcal for energy. Null for a unit no nutrient takes. */
fun convertCsvNutrientToStored(value: Double, unit: CsvUnit): Double? = when (unit) {
    CsvUnit.GRAMS, CsvUnit.KILOCALORIES -> value
    CsvUnit.MILLIGRAMS -> value / 1_000
    CsvUnit.MICROGRAMS -> value / 1_000_000
    CsvUnit.KILOJOULES -> value / KilojoulesPerKilocalorie
    else -> null
}

/**
 * The most one record may carry of [nutrient], in stored units. Health
 * Connect throws past 100 g of a vitamin or mineral; energy and the macros
 * stop at the food form's 10,000.
 */
fun csvNutrientMaxStoredValue(nutrient: NutritionNutrient): Double = when (nutrient.unit) {
    NutritionNutrientUnit.ENERGY_KCAL, NutritionNutrientUnit.MASS_GRAMS -> 10_000.0
    NutritionNutrientUnit.MASS_ADAPTIVE -> 100.0
}

/**
 * Numbers one run's nutrition records by start instant, so two foods at the
 * same minute are its first and second and keep an id each. A file numbers
 * them the same way every run, so a re-import replaces rather than adds.
 */
class CsvNutritionOccurrences {
    private val counts = mutableMapOf<Instant, Int>()

    /** 0 for the first record starting at [start], 1 for the second, and so on. */
    fun next(start: Instant): Int {
        val occurrence = counts[start] ?: 0
        counts[start] = occurrence + 1
        return occurrence
    }
}

/**
 * The row's nutrition record, or null when its nutrient cells are all blank
 * or zero. A bad cell costs only that nutrient; a bad end costs the record.
 * Rejections go to [diagnostics]. [start] is the row's resolved timestamp.
 */
internal fun convertCsvNutritionRow(
    row: CsvRow,
    mapping: CsvImportMapping,
    start: CsvInstant,
    endText: String?,
    endInstant: CsvInstant?,
    occurrences: CsvNutritionOccurrences,
    diagnostics: MutableList<CsvImportDiagnostic>,
): CsvConvertedRecord? {
    val columns = mapping.nutrientColumns
    if (columns.isEmpty()) return null

    val values = mutableMapOf<NutritionNutrient, Double>()
    for (column in columns) {
        val nutrient = column.nutrient ?: continue
        val unit = (column.effectiveInterpretation as? CsvDirectValue)?.unit ?: continue
        // A blank cell is a gap. So is a zero: the food form never stores one,
        // and a row of zeros is no food.
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

        val stored = convertCsvNutrientToStored(raw, unit) ?: continue
        if (stored == 0.0) continue
        if (stored < 0.0 || stored > csvNutrientMaxStoredValue(nutrient)) {
            diagnostics += CsvImportDiagnostic(
                rowNumber = row.rowNumber,
                reason = CsvImportDiagnosticReason.OUT_OF_RANGE,
                columnIndex = column.columnIndex,
                detail = String.format(Locale.US, "%.2f", stored),
            )
            continue
        }
        values[nutrient] = stored
    }
    if (values.isEmpty()) return null

    val endColumnIndex = mapping.endTimestampColumn?.columnIndex ?: columns.first().columnIndex
    val end = csvIntervalEnd(row, start, endText, endInstant, endColumnIndex, diagnostics) ?: return null
    val name = mapping.foodNameColumn?.let { row.cell(it.columnIndex) }
    return buildCsvNutritionRecord(
        values = values,
        name = name,
        start = start,
        end = end,
        occurrence = occurrences.next(start.utc),
    )
}

/**
 * The nutrition record for one row: [values] in stored units, grams or kcal,
 * from [start] to [end], already validated. [occurrence] is its place among
 * the run's records at [start].
 */
fun buildCsvNutritionRecord(
    values: Map<NutritionNutrient, Double>,
    name: String?,
    start: CsvInstant,
    end: CsvInstant,
    occurrence: Int = 0,
): CsvConvertedRecord {
    val clientRecordId = buildCsvClientRecordId(CSV_NUTRITION_TARGET_TYPE, start.utc, occurrence)
    return CsvConvertedRecord(
        metric = null,
        targetType = CSV_NUTRITION_TARGET_TYPE,
        recordType = NutritionRecord::class,
        clientRecordId = clientRecordId,
        instant = start.utc,
        canonicalValue = null,
        nutrients = values.toMap(),
        record = nutritionRecordOf(
            startTime = start.utc,
            startZoneOffset = start.offset,
            endTime = end.utc,
            endZoneOffset = end.offset,
            metadata = csvMetadata(clientRecordId),
            nutrientValues = values,
            name = name,
        ),
    )
}

/**
 * The amounts each nutrient would be written with across the sampled [rows],
 * in stored units, from the real conversion, so the preview shows what lands.
 */
fun previewNutrientValues(
    rows: List<List<String>>,
    mapping: CsvImportMapping,
): Map<NutritionNutrient, List<Double>> {
    val values = mutableMapOf<NutritionNutrient, MutableList<Double>>()
    for (record in previewRecords(rows, mapping)) {
        record.nutrients.forEach { (nutrient, value) -> values.getOrPut(nutrient) { mutableListOf() } += value }
    }
    return values
}

/**
 * A stored amount in the unit labels print [nutrient] in, so a column read in
 * the wrong unit shows: 0.000015 g of vitamin D reads "15.0 µg".
 */
fun csvNutrientPreviewText(nutrient: NutritionNutrient, storedValue: Double): String {
    val unit = nutrient.labelUnit
    val shown = storedValue * 10.0.pow(unit.decimalShift)
    return String.format(Locale.US, "%.1f %s", shown, unit.symbol)
}
