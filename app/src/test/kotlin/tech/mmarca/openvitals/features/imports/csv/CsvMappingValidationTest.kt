package tech.mmarca.openvitals.features.imports.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val Sample = listOf(
    listOf("2026-07-01 08:12:00", "78.4", "15.2"),
    listOf("2026-07-02 08:14:00", "78.6", "15.3"),
)

private fun mappingOf(
    columns: List<CsvColumnMapping>,
    dateTime: CsvDateTimeSettings = CsvDateTimeSettings(
        format = CsvDateTimeFormat.YEAR_FIRST,
        zone = CsvTimeZoneMode.UTC,
    ),
): CsvImportMapping = CsvImportMapping(columns = columns, dateTime = dateTime)

private fun timestampAt(index: Int) = CsvColumnMapping(columnIndex = index, role = CsvColumnRole.TIMESTAMP)

private fun metricAt(
    index: Int,
    metric: CsvImportMetric,
    interpretation: CsvValueInterpretation = CsvDirectValue(CsvUnit.KILOGRAMS),
) = CsvColumnMapping(
    columnIndex = index,
    role = CsvColumnRole.METRIC,
    metric = metric,
    interpretation = interpretation,
)

class CsvMappingValidationTest {

    @Test
    fun `a complete mapping reports no issues`() {
        val issues = validateCsvMapping(
            mappingOf(listOf(timestampAt(0), metricAt(1, CsvImportMetric.WEIGHT))),
            Sample,
        )

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `a mapping with no timestamp column reports it`() {
        val issues = validateCsvMapping(
            mappingOf(listOf(metricAt(1, CsvImportMetric.WEIGHT))),
            Sample,
        )

        assertTrue(CsvMappingIssue.NO_TIMESTAMP_COLUMN in issues)
    }

    @Test
    fun `two timestamp columns report the conflict`() {
        val issues = validateCsvMapping(
            mappingOf(listOf(timestampAt(0), timestampAt(1))),
            Sample,
        )

        assertTrue(CsvMappingIssue.MULTIPLE_TIMESTAMP_COLUMNS in issues)
    }

    @Test
    fun `a mapping with no metric column reports it`() {
        val issues = validateCsvMapping(
            mappingOf(listOf(timestampAt(0))),
            Sample,
        )

        assertTrue(CsvMappingIssue.NO_METRIC_COLUMNS in issues)
    }

    @Test
    fun `two columns mapped to the same metric report the duplicate`() {
        val issues = validateCsvMapping(
            mappingOf(
                listOf(
                    timestampAt(0),
                    metricAt(1, CsvImportMetric.WEIGHT),
                    metricAt(2, CsvImportMetric.WEIGHT),
                ),
            ),
            Sample,
        )

        assertTrue(CsvMappingIssue.DUPLICATE_METRIC in issues)
    }

    @Test
    fun `body fat as a mass with no weight column reports that it needs one`() {
        val issues = validateCsvMapping(
            mappingOf(
                listOf(
                    timestampAt(0),
                    metricAt(2, CsvImportMetric.BODY_FAT, CsvMassShareOfWeight(CsvUnit.KILOGRAMS)),
                ),
            ),
            Sample,
        )

        assertTrue(CsvMappingIssue.MASS_SHARE_NEEDS_WEIGHT_COLUMN in issues)
    }

    @Test
    fun `body fat as a percentage needs no weight column`() {
        val issues = validateCsvMapping(
            mappingOf(
                listOf(
                    timestampAt(0),
                    metricAt(2, CsvImportMetric.BODY_FAT, CsvDirectValue(CsvUnit.PERCENT)),
                ),
            ),
            Sample,
        )

        assertFalse(CsvMappingIssue.MASS_SHARE_NEEDS_WEIGHT_COLUMN in issues)
    }

    @Test
    fun `a date format matching no sampled row reports it`() {
        val issues = validateCsvMapping(
            mappingOf(
                listOf(timestampAt(0), metricAt(1, CsvImportMetric.WEIGHT)),
                dateTime = CsvDateTimeSettings(format = CsvDateTimeFormat.EPOCH_SECONDS),
            ),
            Sample,
        )

        assertTrue(CsvMappingIssue.TIMESTAMP_FORMAT_MATCHES_NO_SAMPLE_ROW in issues)
    }

    @Test
    fun `an undecidable day month order is reported while the format is still automatic`() {
        val issues = validateCsvMapping(
            mappingOf(
                listOf(timestampAt(0), metricAt(1, CsvImportMetric.WEIGHT)),
                dateTime = CsvDateTimeSettings(),
            ),
            listOf(
                listOf("01/07/2026", "78.4"),
                listOf("02/08/2026", "78.6"),
            ),
        )

        assertTrue(CsvMappingIssue.AMBIGUOUS_DAY_MONTH_ORDER in issues)
    }

    @Test
    fun `choosing day-first answers the ambiguity and clears the issue`() {
        // Once the user has said which ordering it is, repeating the question would block the mapping.
        val issues = validateCsvMapping(
            mappingOf(
                listOf(timestampAt(0), metricAt(1, CsvImportMetric.WEIGHT)),
                dateTime = CsvDateTimeSettings(
                    format = CsvDateTimeFormat.DAY_FIRST,
                    zone = CsvTimeZoneMode.UTC,
                ),
            ),
            listOf(
                listOf("01/07/2026", "78.4"),
                listOf("02/08/2026", "78.6"),
            ),
        )

        assertTrue(issues.isEmpty())
    }

    // initialCsvMapping.

    @Test
    fun `the first column that parses as a date is pre-selected`() {
        val mapping = initialCsvMapping(
            headerRow = listOf("Date", "Weight (kg)", "Fat mass (kg)"),
            sample = Sample,
        )

        assertEquals(0, mapping.timestampColumn?.columnIndex)
    }

    @Test
    fun `no metric is guessed from a header name`() {
        // Guessing metrics from labels is the vendor-preset behaviour this importer does without.
        val mapping = initialCsvMapping(
            headerRow = listOf("Date", "Weight (kg)", "Fat mass (kg)"),
            sample = Sample,
        )

        assertTrue(mapping.metricColumns.isEmpty())
    }

    @Test
    fun `a file with no date-like column selects no timestamp`() {
        val mapping = initialCsvMapping(
            headerRow = listOf("A", "B"),
            sample = listOf(listOf("x", "1"), listOf("y", "2")),
        )

        assertNull(mapping.timestampColumn)
    }

    // requiredWritePermissions.

    @Test
    fun `only the mapped metrics permissions are required`() {
        val mapping = mappingOf(listOf(timestampAt(0), metricAt(1, CsvImportMetric.WEIGHT)))

        assertEquals(
            setOf("android.permission.health.WRITE_WEIGHT"),
            mapping.requiredWritePermissions,
        )
    }

    @Test
    fun `a body-composition mapping requires one permission per metric`() {
        val mapping = mappingOf(
            listOf(
                timestampAt(0),
                metricAt(1, CsvImportMetric.WEIGHT),
                metricAt(2, CsvImportMetric.BONE_MASS),
            ),
        )

        assertEquals(
            setOf(
                "android.permission.health.WRITE_WEIGHT",
                "android.permission.health.WRITE_BONE_MASS",
            ),
            mapping.requiredWritePermissions,
        )
    }

    // detectCsvUnitInHeader.

    @Test
    fun `a parenthesised unit is read off the header`() {
        assertEquals(CsvUnit.KILOGRAMS, detectCsvUnitInHeader("Weight (kg)"))
        assertEquals(CsvUnit.POUNDS, detectCsvUnitInHeader("Fat mass (lb)"))
        assertEquals(CsvUnit.PERCENT, detectCsvUnitInHeader("Body fat (%)"))
    }

    @Test
    fun `a unit word inside the label is not read as the unit`() {
        // Only the parenthesised tail counts, so this cannot become grams.
        assertNull(detectCsvUnitInHeader("Weight in grams of food"))
    }

    @Test
    fun `a header with no unit reads as none`() {
        assertNull(detectCsvUnitInHeader("Comments"))
        assertNull(detectCsvUnitInHeader("Date"))
    }

    // Interval metrics.

    /** `TimeFrom,TimeTo,Steps` sampled rows. */
    private val stepsSample = listOf(
        listOf("2026-07-01 08:00:00", "2026-07-01 09:00:00", "1500"),
        listOf("2026-07-01 09:00:00", "2026-07-01 10:00:00", "2500"),
    )

    private fun stepsColumns(endRole: CsvColumnRole = CsvColumnRole.END_TIMESTAMP) = listOf(
        timestampAt(0),
        CsvColumnMapping(columnIndex = 1, role = endRole),
        metricAt(2, CsvImportMetric.STEPS, CsvDirectValue(CsvUnit.COUNT)),
    )

    @Test
    fun `a steps mapping with start and end columns reports no issues`() {
        assertTrue(validateCsvMapping(mappingOf(stepsColumns()), stepsSample).isEmpty())
    }

    @Test
    fun `steps without an end column is still importable, rows default to a one-minute span`() {
        val issues = validateCsvMapping(
            mappingOf(stepsColumns(endRole = CsvColumnRole.IGNORE)),
            stepsSample,
        )

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `two end columns report the conflict`() {
        val issues = validateCsvMapping(
            mappingOf(stepsColumns() + CsvColumnMapping(columnIndex = 3, role = CsvColumnRole.END_TIMESTAMP)),
            stepsSample,
        )

        assertTrue(CsvMappingIssue.MULTIPLE_END_TIMESTAMP_COLUMNS in issues)
    }

    @Test
    fun `an end column that parses in no sampled row blocks the mapping`() {
        val issues = validateCsvMapping(
            mappingOf(stepsColumns()),
            listOf(listOf("2026-07-01 08:00:00", "not a date", "1500")),
        )

        assertTrue(CsvMappingIssue.TIMESTAMP_FORMAT_MATCHES_NO_SAMPLE_ROW in issues)
    }
}
