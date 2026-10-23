package tech.mmarca.openvitals.features.imports.csv

import com.google.common.truth.Truth.assertThat
import java.time.ZoneOffset
import org.junit.Test
import tech.mmarca.openvitals.core.privacy.PrivacyRedactor
import tech.mmarca.openvitals.core.privacy.PrivacySentinels
import tech.mmarca.openvitals.domain.model.BpRecordValues
import tech.mmarca.openvitals.domain.model.NutritionNutrient

private fun reportMapping(
    bodyFat: CsvValueInterpretation = CsvMassShareOfWeight(CsvUnit.KILOGRAMS),
    dateTime: CsvDateTimeSettings? = null,
): CsvImportMapping = CsvImportMapping(
    columns = listOf(
        CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP),
        CsvColumnMapping(
            columnIndex = 1,
            role = CsvColumnRole.METRIC,
            metric = CsvImportMetric.WEIGHT,
            interpretation = CsvDirectValue(CsvUnit.KILOGRAMS),
        ),
        CsvColumnMapping(
            columnIndex = 2,
            role = CsvColumnRole.METRIC,
            metric = CsvImportMetric.BODY_FAT,
            interpretation = bodyFat,
        ),
        CsvColumnMapping(columnIndex = 3),
    ),
    dateTime = dateTime
        ?: CsvDateTimeSettings(
            format = CsvDateTimeFormat.YEAR_FIRST,
            zone = CsvTimeZoneMode.UTC,
        ),
)

private fun runResult(
    outcome: CsvImportOutcome = CsvImportOutcome.COMPLETED,
    diagnostics: List<CsvImportDiagnostic> = emptyList(),
    counts: Map<CsvImportDiagnosticReason, Int> = emptyMap(),
    error: String? = null,
): CsvImportResult = CsvImportResult(
    outcome = outcome,
    progress = CsvImportProgress(
        rowsRead = 120,
        written = 118,
        alreadyPresent = 4,
        rejected = 2,
    ),
    diagnostics = diagnostics,
    diagnosticCounts = counts,
    error = error,
)

private fun report(
    mapping: CsvImportMapping = reportMapping(),
    result: CsvImportResult = runResult(),
    delimiter: String? = ",",
    hasHeader: Boolean? = true,
): String = buildCsvImportReport(
    mapping = mapping,
    result = result,
    fieldDelimiter = delimiter,
    hasHeaderRow = hasHeader,
)

class CsvImportReportTest {

    @Test
    fun `the report gives the outcome and never the file's name`() {
        val report = report()

        assertThat(report).contains("Outcome: completed")
        assertThat(report).doesNotContain("File:")
    }

    @Test
    fun `every tally from the run appears`() {
        val report = report()

        assertThat(report).contains("Rows read:           120")
        assertThat(report).contains("Records written:     118, of which 4 replaced an earlier import")
        assertThat(report).contains("Rejections:          2")
    }

    @Test
    fun `the parsing settings that produced the run are recorded`() {
        val report = report(delimiter = ";", hasHeader = false)

        assertThat(report).contains("Separator:  semicolon")
        assertThat(report).contains("Header row: no")
        assertThat(report).contains("Date format: yearFirst")
        assertThat(report).contains("Time zone:   UTC")
    }

    @Test
    fun `a fixed offset is written out in full`() {
        val report = report(
            mapping = reportMapping(
                dateTime = CsvDateTimeSettings(
                    format = CsvDateTimeFormat.YEAR_FIRST,
                    zone = CsvTimeZoneMode.FIXED_OFFSET,
                    fixedOffset = ZoneOffset.ofHoursMinutes(-5, -30),
                ),
            ),
        )

        assertThat(report).contains("fixed offset -05:30")
    }

    @Test
    fun `a custom date pattern is recorded so a bad one can be spotted`() {
        val report = report(
            mapping = reportMapping(
                dateTime = CsvDateTimeSettings(
                    format = CsvDateTimeFormat.CUSTOM,
                    customPattern = "dd MMM yyyy HH:mm",
                ),
            ),
        )

        assertThat(report).contains("Custom pattern: dd MMM yyyy HH:mm")
    }

    @Test
    fun `every column is listed with the role it was given, not its header`() {
        val report = report()

        assertThat(report).contains("[0] -> date and time")
        assertThat(report).contains("[1] -> weight (kilograms)")
        assertThat(report).contains("[3] -> not imported")
    }

    @Test
    fun `a nutrient column names the nutrient and the unit it was read in`() {
        val mapping = CsvImportMapping(
            columns = listOf(
                CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP),
                CsvColumnMapping(columnIndex = 1, role = CsvColumnRole.FOOD_NAME),
                CsvColumnMapping(
                    columnIndex = 2,
                    role = CsvColumnRole.NUTRIENT,
                    nutrient = NutritionNutrient.VITAMIN_B12,
                ),
            ),
        )

        val report = buildCsvImportReport(
            mapping = mapping,
            result = runResult(),
        )

        assertThat(report).contains("[1] -> food name")
        assertThat(report).contains("[2] -> nutrient vitaminB12 (micrograms)")
    }

    @Test
    fun `blood pressure columns name their part, and the defaults a reading fell back to are recorded`() {
        val mapping = CsvImportMapping(
            columns = listOf(
                CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP),
                CsvColumnMapping(1, CsvColumnRole.BLOOD_PRESSURE, bloodPressurePart = CsvBloodPressurePart.SYSTOLIC),
                CsvColumnMapping(2, CsvColumnRole.BLOOD_PRESSURE, bloodPressurePart = CsvBloodPressurePart.DIASTOLIC),
                CsvColumnMapping(3, CsvColumnRole.BLOOD_PRESSURE, bloodPressurePart = CsvBloodPressurePart.CUFF_LOCATION),
            ),
            bloodPressure = CsvBloodPressureSettings(
                defaultBodyPosition = BpRecordValues.BODY_POSITION_SITTING_DOWN,
            ),
        )

        val report = buildCsvImportReport(
            mapping = mapping,
            result = runResult(counts = mapOf(CsvImportDiagnosticReason.SYSTOLIC_NOT_ABOVE_DIASTOLIC to 1)),
        )

        assertThat(report).contains("[1] -> blood pressure systolic (millimetersOfMercury)")
        assertThat(report).contains("[3] -> blood pressure cuff location")
        assertThat(report).contains("Default body position: sitting down")
        assertThat(report).contains("Default cuff location: not specified")
        assertThat(report).contains("systolic not above diastolic: 1")
    }

    @Test
    fun `a derived body fat says what it was derived from, not just its unit`() {
        // "kilograms" alone on a body-fat column would misdescribe what was written.
        assertThat(report()).contains(
            "[2] -> bodyFat (kilograms as a share of the weight column)",
        )
    }

    @Test
    fun `rejection counts are grouped by reason`() {
        val report = report(
            result = runResult(
                counts = mapOf(
                    CsvImportDiagnosticReason.UNPARSABLE_TIMESTAMP to 2,
                    CsvImportDiagnosticReason.OUT_OF_RANGE to 1,
                ),
            ),
        )

        assertThat(report).contains("date not understood: 2")
        assertThat(report).contains("value outside a plausible range: 1")
    }

    @Test
    fun `individual rejected rows name the row, column and the cell's shape`() {
        val report = report(
            result = runResult(
                diagnostics = listOf(
                    CsvImportDiagnostic(
                        rowNumber = 7,
                        reason = CsvImportDiagnosticReason.UNPARSABLE_NUMBER,
                        columnIndex = 2,
                        detail = PrivacyRedactor.shapeOf("n/a"),
                    ),
                ),
                counts = mapOf(CsvImportDiagnosticReason.UNPARSABLE_NUMBER to 1),
            ),
        )

        assertThat(report).contains("Row 7 column 2: value not a number (a/a)")
    }

    @Test
    fun `a capped per-row log says how many were dropped and that the counts are not`() {
        val report = report(
            result = runResult(
                diagnostics = listOf(
                    CsvImportDiagnostic(
                        rowNumber = 2,
                        reason = CsvImportDiagnosticReason.UNPARSABLE_TIMESTAMP,
                    ),
                ),
                counts = mapOf(CsvImportDiagnosticReason.UNPARSABLE_TIMESTAMP to 51),
            ),
        )

        assertThat(report).contains("... and 50 more")
        assertThat(report).contains("the counts above are complete")
    }

    @Test
    fun `a clean run has no rejection sections at all`() {
        val report = report()

        assertThat(report).doesNotContain("Rejections by reason")
        assertThat(report).doesNotContain("Rejected rows")
    }

    @Test
    fun `a failed run carries its error text`() {
        val report = report(
            result = runResult(
                outcome = CsvImportOutcome.FAILED,
                error = "File not found",
            ),
        )

        assertThat(report).contains("Outcome: failed")
        assertThat(report).contains("Error: File not found")
    }

    @Test
    fun `a rate-limited run says so rather than reading as a plain stop`() {
        assertThat(report(result = runResult(outcome = CsvImportOutcome.RATE_LIMITED)))
            .contains("Health Connect rate limit")
    }

    @Test
    fun `the totals partition the rows, qualify the records, and account for every drip day`() {
        val base = runResult()
        val result = base.copy(
            progress = base.progress.copy(
                rowsRead = 817,
                rowsImported = 815,
                rowsRejected = 1,
                emptyRows = 1,
                written = 754,
                alreadyPresent = 69,
                presenceUnchecked = 240,
                journalDays = 297,
                journalDaysUpdated = 0,
            ),
        )

        val report = report(result = result)

        assertThat(report).contains("Rows read:           817")
        assertThat(report).contains("  Imported:          815")
        assertThat(report).contains("  Rejected:          1")
        assertThat(report).contains("  Nothing to import: 1")
        assertThat(report).contains("Records written:     754, of which 69 replaced an earlier import")
        assertThat(report).contains("  Not checked first: 240")
        assertThat(report).contains("Journal days:        297 (0 updated)")
        assertThat(report()).doesNotContain("Journal days")
        assertThat(report()).doesNotContain("Not checked first")
    }

    @Test
    fun `quota pauses appear in the totals only when the run took some`() {
        val paused = runResult()
        val result = paused.copy(progress = paused.progress.copy(quotaPauses = 3, quotaWaitMillis = 105_000L))

        assertThat(report(result = result)).contains("Quota pauses:        3 (waited 1 min 45 s)")
        assertThat(report()).doesNotContain("Quota pauses")
    }
    @Test
    fun `cells and errors carrying health data come out as shapes, reasons and masked text`() {
        val cell = "${PrivacySentinels.Value} kg ${PrivacySentinels.Date}"
        val rows = listOf(
            CsvRow(rowNumber = 2, fields = listOf(PrivacySentinels.Instant, cell, "", PrivacySentinels.Name)),
            CsvRow(rowNumber = 3, fields = listOf("not a date ${PrivacySentinels.Name}", "70", "", "")),
            CsvRow(rowNumber = 4, fields = listOf(PrivacySentinels.Instant, "9999", "", "")),
        )
        val diagnostics = rows.flatMap { convertCsvRow(it, reportMapping(bodyFat = CsvDirectValue(CsvUnit.PERCENT))).diagnostics }

        val report = report(
            result = runResult(
                diagnostics = diagnostics,
                counts = diagnostics.groupingBy { it.reason }.eachCount(),
                outcome = CsvImportOutcome.FAILED,
                error = PrivacySentinels.Sentence,
            ).let { it.copy(error = PrivacyRedactor.maskMessage(it.error)) },
        )

        assertThat(diagnostics).isNotEmpty()
        PrivacySentinels.assertNoneIn(report, except = setOf(PrivacySentinels.Name))
        assertThat(report.substringAfter("Rejected rows")).doesNotContain(PrivacySentinels.Name)
    }
}
