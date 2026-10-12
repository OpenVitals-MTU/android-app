package tech.mmarca.openvitals.features.imports.csv

import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.CsvNutritionClientRecordPrefix
import tech.mmarca.openvitals.domain.model.NutritionNutrient

private fun nutrientAt(index: Int, nutrient: NutritionNutrient, unit: CsvUnit? = null) = CsvColumnMapping(
    columnIndex = index,
    role = CsvColumnRole.NUTRIENT,
    interpretation = unit?.let(::CsvDirectValue),
    nutrient = nutrient,
)

/**
 * `Date,Food,Energy (kcal),Protein (g),Fiber (g),Sodium (mg),Vitamin D (µg)`: a row per food,
 * each amount in the unit its label prints, so every column keeps its default unit.
 */
private fun foodLogMapping(extra: List<CsvColumnMapping> = emptyList()) = CsvImportMapping(
    columns = listOf(
        CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP),
        CsvColumnMapping(columnIndex = 1, role = CsvColumnRole.FOOD_NAME),
        nutrientAt(2, NutritionNutrient.ENERGY),
        nutrientAt(3, NutritionNutrient.PROTEIN),
        nutrientAt(4, NutritionNutrient.DIETARY_FIBER),
        nutrientAt(5, NutritionNutrient.SODIUM),
        nutrientAt(6, NutritionNutrient.VITAMIN_D),
    ) + extra,
    dateTime = CsvDateTimeSettings(format = CsvDateTimeFormat.YEAR_FIRST, zone = CsvTimeZoneMode.UTC),
)

private fun foodRow(vararg fields: String) = CsvRow(rowNumber = 2, fields = fields.toList())

private val Oatmeal = foodRow("2026-10-04 12:30", "Oatmeal", "350", "12", "8", "120", "2.5")

private val Noon = CsvInstant(utc = Instant.parse("2026-10-04T12:00:00Z"), offset = ZoneOffset.UTC)

private fun CsvRowConversion.nutrition(): NutritionRecord =
    records.single { it.recordType == NutritionRecord::class }.record as NutritionRecord

class CsvNutritionImportTest {

    @Test
    fun `every nutrient column of a row lands in one record, in grams and kcal`() {
        val conversion = convertCsvRow(Oatmeal, foodLogMapping())

        assertTrue(conversion.diagnostics.isEmpty())
        val record = conversion.nutrition()
        assertEquals(350.0, record.energy!!.inKilocalories, 1e-9)
        assertEquals(12.0, record.protein!!.inGrams, 1e-9)
        assertEquals(8.0, record.dietaryFiber!!.inGrams, 1e-9)
        assertEquals(0.12, record.sodium!!.inGrams, 1e-12)
        assertEquals(0.0000025, record.vitaminD!!.inGrams, 1e-15)
        assertEquals("Oatmeal", record.name)
    }

    @Test
    fun `without an end column the food spans one minute from its time`() {
        val record = convertCsvRow(Oatmeal, foodLogMapping()).nutrition()

        assertEquals(Instant.parse("2026-10-04T12:30:00Z"), record.startTime)
        assertEquals(Instant.parse("2026-10-04T12:31:00Z"), record.endTime)
        assertEquals(ZoneOffset.UTC, record.startZoneOffset)
    }

    @Test
    fun `an end column bounds the food, and a bad end costs the food but not a weight beside it`() {
        val mapping = foodLogMapping(
            extra = listOf(
                CsvColumnMapping(columnIndex = 7, role = CsvColumnRole.END_TIMESTAMP),
                CsvColumnMapping(
                    columnIndex = 8,
                    role = CsvColumnRole.METRIC,
                    metric = CsvImportMetric.WEIGHT,
                    interpretation = CsvDirectValue(CsvUnit.KILOGRAMS),
                ),
            ),
        )

        val bounded = convertCsvRow(
            foodRow("2026-10-04 12:30", "Lunch", "650", "", "", "", "", "2026-10-04 13:00", "78.4"),
            mapping,
        )
        assertEquals(Instant.parse("2026-10-04T13:00:00Z"), bounded.nutrition().endTime)

        val swapped = convertCsvRow(
            foodRow("2026-10-04 12:30", "Lunch", "650", "", "", "", "", "2026-10-04 12:10", "78.4"),
            mapping,
        )
        assertEquals(listOf(WeightRecord::class), swapped.records.map { it.recordType })
        assertEquals(CsvImportDiagnosticReason.END_NOT_AFTER_START, swapped.diagnostics.single().reason)
        assertEquals(7, swapped.diagnostics.single().columnIndex)
    }

    @Test
    fun `a blank or zero amount is a gap, and a row with no amounts writes nothing`() {
        val partial = convertCsvRow(foodRow("2026-10-04 12:30", "Egg", "0", "6", "", "0", ""), foodLogMapping())
        val record = partial.nutrition()
        assertNull(record.energy)
        assertNull(record.sodium)
        assertEquals(6.0, record.protein!!.inGrams, 1e-9)

        val empty = convertCsvRow(foodRow("2026-10-04 12:30", "Water", "0", "", "0", "", ""), foodLogMapping())
        assertTrue(empty.records.isEmpty())
        assertTrue(empty.diagnostics.isEmpty())
    }

    @Test
    fun `an unreadable amount costs that nutrient only`() {
        val conversion = convertCsvRow(
            foodRow("2026-10-04 12:30", "Soup", "180", "9", "3", "n/a", ""),
            foodLogMapping(),
        )

        val diagnostic = conversion.diagnostics.single()
        assertEquals(CsvImportDiagnosticReason.UNPARSABLE_NUMBER, diagnostic.reason)
        assertEquals(5, diagnostic.columnIndex)
        assertNull(conversion.nutrition().sodium)
        assertEquals(180.0, conversion.nutrition().energy!!.inKilocalories, 1e-9)
    }

    @Test
    fun `an amount past a plausible ceiling is rejected instead of failing the record`() {
        // 200 g of vitamin D: Health Connect throws past 100 g of a vitamin. 20,000 kcal in one food.
        val conversion = convertCsvRow(
            foodRow("2026-10-04 12:30", "Typo", "20000", "-3", "", "", "200000000"),
            foodLogMapping(),
        )

        assertEquals(
            listOf(2, 3, 6),
            conversion.diagnostics.map { it.columnIndex },
        )
        assertTrue(conversion.diagnostics.all { it.reason == CsvImportDiagnosticReason.OUT_OF_RANGE })
        assertTrue(conversion.records.isEmpty())
    }

    @Test
    fun `every nutrient at its ceiling builds a record Health Connect accepts`() {
        // The record's own constructor enforces Health Connect's ceilings, so building is the check.
        NutritionNutrient.entries.forEach { nutrient ->
            val ceiling = csvNutrientMaxStoredValue(nutrient)
            val converted = buildCsvNutritionRecord(
                values = mapOf(nutrient to ceiling),
                name = null,
                start = Noon,
                end = CsvInstant(utc = Noon.utc.plusSeconds(60), offset = ZoneOffset.UTC),
            )
            assertEquals(mapOf(nutrient to ceiling), converted.nutrients)
        }
    }

    @Test
    fun `each nutrient defaults to the unit its label prints`() {
        assertEquals(CsvDirectValue(CsvUnit.KILOCALORIES), csvNutrientDefaultInterpretation(NutritionNutrient.ENERGY))
        assertEquals(CsvDirectValue(CsvUnit.GRAMS), csvNutrientDefaultInterpretation(NutritionNutrient.PROTEIN))
        assertEquals(CsvDirectValue(CsvUnit.MILLIGRAMS), csvNutrientDefaultInterpretation(NutritionNutrient.SODIUM))
        assertEquals(CsvDirectValue(CsvUnit.MICROGRAMS), csvNutrientDefaultInterpretation(NutritionNutrient.VITAMIN_D))
    }

    @Test
    fun `a nutrient header's unit becomes the column's unit`() {
        fun fromHeader(nutrient: NutritionNutrient, header: String) =
            detectCsvUnitInHeader(header)?.let { csvNutrientInterpretationForUnit(nutrient, it) }

        assertEquals(CsvDirectValue(CsvUnit.MILLIGRAMS), fromHeader(NutritionNutrient.SODIUM, "Sodium (mg)"))
        // A header's kcal reads as a daily rate for basal metabolic rate, and as energy here.
        assertEquals(CsvDirectValue(CsvUnit.KILOCALORIES), fromHeader(NutritionNutrient.ENERGY, "Energy (kcal)"))
        assertEquals(CsvDirectValue(CsvUnit.KILOJOULES), fromHeader(NutritionNutrient.ENERGY, "Energy (kJ)"))
        // The micro sign and the Greek mu look alike; labels use both.
        listOf("Vitamin D (µg)", "Vitamin D (μg)", "Vitamin D (mcg)", "Vitamin D (ug)").forEach { header ->
            assertEquals(header, CsvDirectValue(CsvUnit.MICROGRAMS), fromHeader(NutritionNutrient.VITAMIN_D, header))
        }
        // A mass no nutrient is written in leaves the default in place.
        assertNull(fromHeader(NutritionNutrient.PROTEIN, "Protein (kg)"))
    }

    @Test
    fun `amounts convert exactly to what Health Connect stores`() {
        assertEquals(1000.0, convertCsvNutrientToStored(4184.0, CsvUnit.KILOJOULES)!!, 1e-9)
        assertEquals(0.000015, convertCsvNutrientToStored(15.0, CsvUnit.MICROGRAMS)!!, 0.0)
        assertEquals(0.12, convertCsvNutrientToStored(120.0, CsvUnit.MILLIGRAMS)!!, 0.0)
        assertNull(convertCsvNutrientToStored(2.0, CsvUnit.KILOGRAMS))
    }

    @Test
    fun `a food's id is the plain CSV id of its start, under the prefix the drink screens skip`() {
        val converted = convertCsvRow(Oatmeal, foodLogMapping()).records.single()

        assertEquals(
            buildCsvClientRecordId("NutritionRecord", Instant.parse("2026-10-04T12:30:00Z")),
            converted.clientRecordId,
        )
        assertTrue(converted.clientRecordId.startsWith(CsvNutritionClientRecordPrefix))
    }

    @Test
    fun `two foods at the same time keep an id each, numbered the same way on every run`() {
        val milk = foodRow("2026-10-04 12:30", "Milk", "120", "8", "", "100", "")

        fun idsOfOneRun(): List<String> {
            val occurrences = CsvNutritionOccurrences()
            return listOf(Oatmeal, milk).map { row ->
                convertCsvRow(row, foodLogMapping(), occurrences).records.single().clientRecordId
            }
        }

        val firstRun = idsOfOneRun()
        assertNotEquals(firstRun[0], firstRun[1])
        assertEquals(firstRun, idsOfOneRun())
    }

    @Test
    fun `a corrected amount keeps the id, so the re-import replaces the food`() {
        val corrected = foodRow("2026-10-04 12:30", "Oatmeal", "380", "13", "8", "120", "2.5")

        assertEquals(
            convertCsvRow(Oatmeal, foodLogMapping()).records.single().clientRecordId,
            convertCsvRow(corrected, foodLogMapping()).records.single().clientRecordId,
        )
    }

    @Test
    fun `the preview shows each nutrient as it will be written, in the unit its label prints`() {
        val rows = listOf(
            listOf("2026-10-04 08:00", "Oatmeal", "350", "12", "8", "120", "2.5"),
            listOf("2026-10-04 12:30", "Soup", "180", "9", "3", "800", ""),
        )

        val values = previewNutrientValues(rows, foodLogMapping())

        assertEquals(listOf(0.12, 0.8), values[NutritionNutrient.SODIUM])
        assertEquals(listOf(0.0000025), values[NutritionNutrient.VITAMIN_D])
        assertEquals("2.5 µg", csvNutrientPreviewText(NutritionNutrient.VITAMIN_D, 0.0000025))
        assertEquals("800.0 mg", csvNutrientPreviewText(NutritionNutrient.SODIUM, 0.8))
        assertEquals("350.0 kcal", csvNutrientPreviewText(NutritionNutrient.ENERGY, 350.0))
    }
}
