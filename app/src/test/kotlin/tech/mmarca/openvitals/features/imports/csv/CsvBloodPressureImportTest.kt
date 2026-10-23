package tech.mmarca.openvitals.features.imports.csv

import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.WeightRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BpRecordValues
import tech.mmarca.openvitals.domain.model.NutritionNutrient

private fun partAt(index: Int, part: CsvBloodPressurePart) = CsvColumnMapping(
    columnIndex = index,
    role = CsvColumnRole.BLOOD_PRESSURE,
    bloodPressurePart = part,
)

/** The app in Finnish: its own labels first, then the English fallback. */
private val FinnishLabels = CsvBloodPressureLabels(
    bodyPositions = listOf(
        mapOf(BpRecordValues.BODY_POSITION_SITTING_DOWN to "Istuen", BpRecordValues.BODY_POSITION_LYING_DOWN to "Makuulla"),
        mapOf(BpRecordValues.BODY_POSITION_SITTING_DOWN to "Sitting", BpRecordValues.BODY_POSITION_LYING_DOWN to "Lying down"),
    ),
    cuffLocations = listOf(
        mapOf(BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM to "Vasen käsivarsi"),
        mapOf(BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM to "Left arm"),
    ),
)

/** `Date,Systolic,Diastolic,Position,Cuff,Weight (kg)`: a monitor's log beside a scale's. */
private fun monitorMapping(
    extra: List<CsvColumnMapping> = listOf(
        partAt(3, CsvBloodPressurePart.BODY_POSITION),
        partAt(4, CsvBloodPressurePart.CUFF_LOCATION),
    ),
    settings: CsvBloodPressureSettings = CsvBloodPressureSettings(labels = FinnishLabels),
) = CsvImportMapping(
    columns = listOf(
        CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP),
        partAt(1, CsvBloodPressurePart.SYSTOLIC),
        partAt(2, CsvBloodPressurePart.DIASTOLIC),
        CsvColumnMapping(
            columnIndex = 5,
            role = CsvColumnRole.METRIC,
            metric = CsvImportMetric.WEIGHT,
            interpretation = CsvDirectValue(CsvUnit.KILOGRAMS),
        ),
    ) + extra,
    dateTime = CsvDateTimeSettings(format = CsvDateTimeFormat.YEAR_FIRST, zone = CsvTimeZoneMode.UTC),
    bloodPressure = settings,
)

private fun monitorRow(vararg fields: String) = CsvRow(rowNumber = 2, fields = fields.toList())

private fun CsvRowConversion.reading(): BloodPressureRecord =
    records.single { it.recordType == BloodPressureRecord::class }.record as BloodPressureRecord

private fun CsvRowConversion.hasWeight(): Boolean = records.any { it.recordType == WeightRecord::class }

class CsvBloodPressureImportTest {

    @Test
    fun `four columns of a row make one reading, its words read in the app language`() {
        val conversion = convertCsvRow(
            monitorRow("2026-10-03 08:00", "123", "88", "Istuen", "Vasen käsivarsi", "78.4"),
            monitorMapping(),
        )

        assertTrue(conversion.diagnostics.isEmpty())
        val reading = conversion.reading()
        assertEquals(123.0, reading.systolic.inMillimetersOfMercury, 1e-9)
        assertEquals(88.0, reading.diastolic.inMillimetersOfMercury, 1e-9)
        assertEquals(BpRecordValues.BODY_POSITION_SITTING_DOWN, reading.bodyPosition)
        assertEquals(BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM, reading.measurementLocation)
        assertEquals(Instant.parse("2026-10-03T08:00:00Z"), reading.time)
        assertEquals(
            buildCsvClientRecordId(CSV_BLOOD_PRESSURE_TARGET_TYPE, reading.time),
            reading.metadata.clientRecordId,
        )
        assertTrue(conversion.hasWeight())
    }

    @Test
    fun `an English word matches too, whatever its case and spacing`() {
        val reading = convertCsvRow(
            monitorRow("2026-10-03 08:00", "123", "88", " LYING_DOWN ", "left  arm", ""),
            monitorMapping(),
        ).reading()

        assertEquals(BpRecordValues.BODY_POSITION_LYING_DOWN, reading.bodyPosition)
        assertEquals(BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM, reading.measurementLocation)
    }

    @Test
    fun `a word no label matches, a code, or a blank cell takes the default and keeps the reading`() {
        val settings = CsvBloodPressureSettings(
            labels = FinnishLabels,
            defaultBodyPosition = BpRecordValues.BODY_POSITION_RECLINING,
            defaultCuffLocation = BpRecordValues.MEASUREMENT_LOCATION_RIGHT_WRIST,
        )

        for ((position, cuff) in listOf("seated" to "2.0", "" to "")) {
            val conversion = convertCsvRow(
                monitorRow("2026-10-03 08:00", "120", "80", position, cuff, "78.4"),
                monitorMapping(settings = settings),
            )

            assertTrue(conversion.diagnostics.isEmpty())
            assertEquals(BpRecordValues.BODY_POSITION_RECLINING, conversion.reading().bodyPosition)
            assertEquals(BpRecordValues.MEASUREMENT_LOCATION_RIGHT_WRIST, conversion.reading().measurementLocation)
        }
    }

    @Test
    fun `without position and cuff columns a reading takes the defaults, unknown until chosen`() {
        val reading = convertCsvRow(
            monitorRow("2026-10-03 08:00", "120", "80", "", "", ""),
            monitorMapping(extra = emptyList()),
        ).reading()

        assertEquals(BpRecordValues.BODY_POSITION_UNKNOWN, reading.bodyPosition)
        assertEquals(BpRecordValues.MEASUREMENT_LOCATION_UNKNOWN, reading.measurementLocation)
    }

    @Test
    fun `a row cut off before its optional position and cuff cells still imports`() {
        val mapping = CsvImportMapping(
            columns = listOf(
                CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP),
                partAt(1, CsvBloodPressurePart.SYSTOLIC),
                partAt(2, CsvBloodPressurePart.DIASTOLIC),
                partAt(3, CsvBloodPressurePart.BODY_POSITION),
                partAt(4, CsvBloodPressurePart.CUFF_LOCATION),
            ),
            dateTime = CsvDateTimeSettings(format = CsvDateTimeFormat.YEAR_FIRST, zone = CsvTimeZoneMode.UTC),
            bloodPressure = CsvBloodPressureSettings(labels = FinnishLabels),
        )

        val conversion = convertCsvRow(monitorRow("2026-10-03 08:00", "120", "80", "Sitting"), mapping)

        assertTrue(conversion.diagnostics.isEmpty())
        assertEquals(BpRecordValues.BODY_POSITION_SITTING_DOWN, conversion.reading().bodyPosition)
    }

    @Test
    fun `both pressures blank is a gap, not a rejection`() {
        val conversion = convertCsvRow(
            monitorRow("2026-10-03 08:00", "", "", "Sitting", "Left arm", "78.4"),
            monitorMapping(),
        )

        assertTrue(conversion.diagnostics.isEmpty())
        assertEquals(listOf(WeightRecord::class), conversion.records.map { it.recordType })
    }

    @Test
    fun `one pressure blank costs the reading but not the weight beside it`() {
        val conversion = convertCsvRow(
            monitorRow("2026-10-03 08:00", "120", "", "", "", "78.4"),
            monitorMapping(),
        )

        assertEquals(
            listOf(CsvImportDiagnostic(2, CsvImportDiagnosticReason.MISSING_BLOOD_PRESSURE_VALUE, columnIndex = 2)),
            conversion.diagnostics,
        )
        assertEquals(listOf(WeightRecord::class), conversion.records.map { it.recordType })
    }

    @Test
    fun `a reading Health Connect would refuse is rejected for its first fault only`() {
        val cases = listOf(
            // Two faults, one lost reading: one rejection.
            // A cell that is not a number is reported by its shape; a value never appears.
            listOf("1200", "not a number") to CsvImportDiagnostic(2, CsvImportDiagnosticReason.OUT_OF_RANGE, 1, null),
            listOf("abc", "80") to CsvImportDiagnostic(2, CsvImportDiagnosticReason.UNPARSABLE_NUMBER, 1, "aaa"),
            listOf("120", "9") to CsvImportDiagnostic(2, CsvImportDiagnosticReason.OUT_OF_RANGE, 2, null),
            listOf("80", "120") to CsvImportDiagnostic(2, CsvImportDiagnosticReason.SYSTOLIC_NOT_ABOVE_DIASTOLIC, 1, null),
        )

        for ((pressures, expected) in cases) {
            val conversion = convertCsvRow(
                monitorRow("2026-10-03 08:00", pressures[0], pressures[1], "", "", ""),
                monitorMapping(),
            )

            assertEquals(listOf(expected), conversion.diagnostics)
            assertTrue(conversion.records.isEmpty())
        }
    }

    @Test
    fun `a blood pressure column needs both a systolic and a diastolic one, each once`() {
        val sample = listOf(listOf("2026-10-03 08:00", "120", "80", "Sitting"))
        val timestamp = CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP)

        val positionOnly = CsvImportMapping(listOf(timestamp, partAt(3, CsvBloodPressurePart.BODY_POSITION)))
        val twoSystolic = CsvImportMapping(
            listOf(
                timestamp,
                partAt(1, CsvBloodPressurePart.SYSTOLIC),
                partAt(2, CsvBloodPressurePart.SYSTOLIC),
                partAt(3, CsvBloodPressurePart.DIASTOLIC),
            ),
        )

        assertEquals(
            listOf(CsvMappingIssue.BLOOD_PRESSURE_NEEDS_SYSTOLIC_AND_DIASTOLIC),
            validateCsvMapping(positionOnly, sample),
        )
        assertEquals(listOf(CsvMappingIssue.DUPLICATE_METRIC), validateCsvMapping(twoSystolic, sample))
        assertTrue(validateCsvMapping(monitorMapping(), emptyList()).isEmpty())
    }

    @Test
    fun `a reading and a food count as one record a row each, and ask for their own permissions`() {
        val mapping = monitorMapping().let {
            it.copy(
                columns = it.columns + CsvColumnMapping(
                    columnIndex = 6,
                    role = CsvColumnRole.NUTRIENT,
                    nutrient = NutritionNutrient.ENERGY,
                ) + CsvColumnMapping(
                    columnIndex = 7,
                    role = CsvColumnRole.NUTRIENT,
                    nutrient = NutritionNutrient.PROTEIN,
                ),
            )
        }

        // Weight, the reading from four columns, and the food from two.
        assertEquals(3, mapping.recordsPerRow)
        assertTrue(CsvBloodPressureWritePermission in mapping.requiredWritePermissions)
    }

    @Test
    fun `the tile lists every word once and names the sampled values it does not know`() {
        val column = partAt(3, CsvBloodPressurePart.BODY_POSITION)

        val hint = csvBloodPressureLabelHint(column, listOf("Istuen", "seated", "SITTING", "seated"), FinnishLabels)

        assertEquals(listOf("Istuen", "Makuulla", "Sitting", "Lying down"), hint?.accepted)
        assertEquals(listOf("seated"), hint?.unrecognised)
        assertNull(csvBloodPressureLabelHint(partAt(1, CsvBloodPressurePart.SYSTOLIC), listOf("120"), FinnishLabels))
    }

    @Test
    fun `a word two languages share keeps the app language's meaning`() {
        val labels = CsvBloodPressureLabels(
            cuffLocations = listOf(
                mapOf(BpRecordValues.MEASUREMENT_LOCATION_LEFT_WRIST to "Left arm"),
                mapOf(BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM to "Left arm"),
            ),
        )

        assertEquals(
            BpRecordValues.MEASUREMENT_LOCATION_LEFT_WRIST,
            labels.match(CsvBloodPressurePart.CUFF_LOCATION, "left arm"),
        )
    }

    @Test
    fun `the confirm preview reads both pressures from the rows that will be written`() {
        val rows = listOf(
            listOf("2026-10-03 08:00", "123", "88", "", "", ""),
            listOf("2026-10-03 12:58", "145", "95", "", "", ""),
            listOf("2026-10-03 22:11", "80", "120", "", "", ""),
        )

        assertEquals(listOf(123.0 to 88.0, 145.0 to 95.0), previewBloodPressureValues(rows, monitorMapping()))
    }
}
