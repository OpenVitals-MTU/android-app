package tech.mmarca.openvitals.features.imports.csv

import androidx.health.connect.client.records.BloodPressureRecord
import tech.mmarca.openvitals.domain.model.BpRecordValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val Labels = CsvBloodPressureLabels(
    bodyPositions = csvLabelLookup(
        listOf(
            mapOf(
                BpRecordValues.BODY_POSITION_SITTING_DOWN to "Istuen",
                BpRecordValues.BODY_POSITION_LYING_DOWN to "Makuulla",
            ),
            mapOf(
                BpRecordValues.BODY_POSITION_SITTING_DOWN to "Sitting",
                BpRecordValues.BODY_POSITION_STANDING_UP to "Standing",
                BpRecordValues.BODY_POSITION_LYING_DOWN to "Lying down",
                BpRecordValues.BODY_POSITION_RECLINING to "Reclining",
            ),
        ),
    ),
    cuffLocations = csvLabelLookup(
        listOf(
            mapOf(BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM to "Vasen käsivarsi"),
            mapOf(
                BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM to "Left arm",
                BpRecordValues.MEASUREMENT_LOCATION_RIGHT_UPPER_ARM to "Right arm",
                BpRecordValues.MEASUREMENT_LOCATION_LEFT_WRIST to "Left wrist",
                BpRecordValues.MEASUREMENT_LOCATION_RIGHT_WRIST to "Right wrist",
            ),
        ),
    ),
)

private fun bloodPressureMapping(
    defaultBodyPosition: Int = BpRecordValues.BODY_POSITION_UNKNOWN,
    defaultCuffLocation: Int = BpRecordValues.MEASUREMENT_LOCATION_UNKNOWN,
    withPositionColumns: Boolean = true,
): CsvImportMapping = CsvImportMapping(
    columns = listOf(
        CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP),
        CsvColumnMapping(
            columnIndex = 1,
            role = CsvColumnRole.METRIC,
            bloodPressureField = CsvBloodPressureField.SYSTOLIC,
        ),
        CsvColumnMapping(
            columnIndex = 2,
            role = CsvColumnRole.METRIC,
            bloodPressureField = CsvBloodPressureField.DIASTOLIC,
        ),
    ) + if (!withPositionColumns) emptyList() else listOf(
        CsvColumnMapping(
            columnIndex = 3,
            role = CsvColumnRole.METRIC,
            bloodPressureField = CsvBloodPressureField.BODY_POSITION,
        ),
        CsvColumnMapping(
            columnIndex = 4,
            role = CsvColumnRole.METRIC,
            bloodPressureField = CsvBloodPressureField.CUFF_LOCATION,
        ),
    ),
    dateTime = CsvDateTimeSettings(
        format = CsvDateTimeFormat.YEAR_FIRST,
        zone = CsvTimeZoneMode.UTC
    ),
    bloodPressureLabels = Labels,
    defaultBodyPosition = defaultBodyPosition,
    defaultCuffLocation = defaultCuffLocation,
)

private fun row(fields: List<String>, rowNumber: Int = 2): CsvRow =
    CsvRow(rowNumber = rowNumber, fields = fields)

class CsvBloodPressureImportTest {

    // Blood pressure.

    @Test
    fun `a row becomes one normalised blood pressure record`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "120", "80", "Sitting", "Left arm")),
            mapping = bloodPressureMapping(),
        )

        assertTrue(conversion.diagnostics.isEmpty())

        val record = conversion.records.single().record as BloodPressureRecord

        assertEquals(120.0, record.systolic.inMillimetersOfMercury, 0.0)
        assertEquals(80.0, record.diastolic.inMillimetersOfMercury, 0.0)
        assertEquals(BpRecordValues.BODY_POSITION_SITTING_DOWN, record.bodyPosition)
        assertEquals(BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM, record.measurementLocation)
    }

    @Test
    fun `blank position cells are unknown`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "118", "76", "", "")),
            mapping = bloodPressureMapping(),
        )

        val record = conversion.records.single().record as BloodPressureRecord

        assertEquals(BloodPressureRecord.BODY_POSITION_UNKNOWN, record.bodyPosition)
        assertEquals(BloodPressureRecord.MEASUREMENT_LOCATION_UNKNOWN, record.measurementLocation)
    }

    @Test
    fun `labels match in the app language and fall back to English`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "120", "80", "Istuen", "Right wrist")),
            mapping = bloodPressureMapping(),
        )

        val record = conversion.records.single().record as BloodPressureRecord

        assertEquals(BpRecordValues.BODY_POSITION_SITTING_DOWN, record.bodyPosition)
        assertEquals(BpRecordValues.MEASUREMENT_LOCATION_RIGHT_WRIST, record.measurementLocation)
    }

    @Test
    fun `labels ignore case and whitespace`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "120", "80", "  LYING   Down ", "vasen\tKÄSIVARSI")),
            mapping = bloodPressureMapping(),
        )

        val record = conversion.records.single().record as BloodPressureRecord

        assertEquals(BpRecordValues.BODY_POSITION_LYING_DOWN, record.bodyPosition)
        assertEquals(BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM, record.measurementLocation)
    }

    @Test
    fun `an unmatched label takes the defaults`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "120", "80", "seated", "3")),
            mapping = bloodPressureMapping(
                defaultBodyPosition = BpRecordValues.BODY_POSITION_STANDING_UP,
                defaultCuffLocation = BpRecordValues.MEASUREMENT_LOCATION_RIGHT_UPPER_ARM,
            ),
        )

        val record = conversion.records.single().record as BloodPressureRecord

        assertTrue(conversion.diagnostics.isEmpty())
        assertEquals(BpRecordValues.BODY_POSITION_STANDING_UP, record.bodyPosition)
        assertEquals(BpRecordValues.MEASUREMENT_LOCATION_RIGHT_UPPER_ARM, record.measurementLocation)
    }

    @Test
    fun `columns that are not imported take the defaults`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "120", "80")),
            mapping = bloodPressureMapping(
                defaultBodyPosition = BpRecordValues.BODY_POSITION_RECLINING,
                defaultCuffLocation = BpRecordValues.MEASUREMENT_LOCATION_LEFT_WRIST,
                withPositionColumns = false,
            ),
        )

        val record = conversion.records.single().record as BloodPressureRecord

        assertEquals(BpRecordValues.BODY_POSITION_RECLINING, record.bodyPosition)
        assertEquals(BpRecordValues.MEASUREMENT_LOCATION_LEFT_WRIST, record.measurementLocation)
    }

    @Test
    fun `a missing trailing optional cell does not reject the blood pressure reading`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "120", "80", "Standing")),
            mapping = bloodPressureMapping(),
        )

        assertTrue(conversion.diagnostics.isEmpty())
        assertEquals(1, conversion.records.size)
    }

    @Test
    fun `an out of range pressure rejects the reading`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "1200", "80", "Standing", "Left wrist")),
            mapping = bloodPressureMapping(),
        )

        assertTrue(conversion.records.isEmpty())
        assertEquals(CsvImportDiagnosticReason.OUT_OF_RANGE, conversion.diagnostics.single().reason)
    }

    @Test
    fun `numeric codes are not labels`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "120", "80", "2", "3")),
            mapping = bloodPressureMapping(),
        )

        val record = conversion.records.single().record as BloodPressureRecord

        assertEquals(BpRecordValues.BODY_POSITION_UNKNOWN, record.bodyPosition)
        assertEquals(BpRecordValues.MEASUREMENT_LOCATION_UNKNOWN, record.measurementLocation)
    }

    @Test
    fun `both pressures blank skips silently and one blank rejects`() {
        val skipped = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "", "", "Standing", "Left wrist")),
            mapping = bloodPressureMapping(),
        )

        assertTrue(skipped.records.isEmpty())
        assertTrue(skipped.diagnostics.isEmpty())

        for (fields in listOf(
            listOf("2026-07-01 08:12:00", "120", "", "Standing", "Left wrist"),
            listOf("2026-07-01 08:12:00", "", "80", "Standing", "Left wrist"),
        )) {
            val conversion = convertCsvRow(
                row = row(fields),
                mapping = bloodPressureMapping(),
            )

            assertTrue(conversion.records.isEmpty())
            assertEquals(
                CsvImportDiagnosticReason.MISSING_BLOOD_PRESSURE_VALUE,
                conversion.diagnostics.single().reason,
            )
        }
    }

    @Test
    fun `systolic not above diastolic rejects the reading`() {
        for ((systolic, diastolic) in listOf("80" to "80", "70" to "90")) {
            val conversion = convertCsvRow(
                row = row(listOf("2026-07-01 08:12:00", systolic, diastolic, "Standing", "Left wrist")),
                mapping = bloodPressureMapping(),
            )

            assertTrue(conversion.records.isEmpty())
            assertEquals(
                CsvImportDiagnosticReason.SYSTOLIC_NOT_ABOVE_DIASTOLIC,
                conversion.diagnostics.single().reason,
            )
        }
    }

    @Test
    fun `blood pressure needs both systolic and diastolic columns`() {
        val mapping = CsvImportMapping(
            columns = listOf(
                CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP),
                CsvColumnMapping(
                    columnIndex = 1,
                    role = CsvColumnRole.METRIC,
                    bloodPressureField = CsvBloodPressureField.SYSTOLIC,
                ),
            ),
            dateTime = CsvDateTimeSettings(
                format = CsvDateTimeFormat.YEAR_FIRST,
                zone = CsvTimeZoneMode.UTC
            ),
        )

        val issues = validateCsvMapping(mapping, listOf(listOf("2026-07-01 08:12:00", "120")))

        assertTrue(CsvMappingIssue.BLOOD_PRESSURE_NEEDS_SYSTOLIC_AND_DIASTOLIC in issues)
        assertTrue(validateCsvMapping(bloodPressureMapping(), emptyList()).isEmpty())
    }
}
