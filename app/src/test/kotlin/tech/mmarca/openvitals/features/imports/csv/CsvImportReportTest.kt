package tech.mmarca.openvitals.features.imports.csv

import com.google.common.truth.Truth.assertThat
import java.time.ZoneOffset
import org.junit.Test

private val HeaderRow = listOf("Date", "Weight (kg)", "Fat mass (kg)", "Comments")

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
    fileName: String? = "withings.csv",
    delimiter: String? = ",",
    hasHeader: Boolean? = true,
): String = buildCsvImportReport(
    fileName = fileName,
    mapping = mapping,
    result = result,
    headerRow = HeaderRow,
    fieldDelimiter = delimiter,
    hasHeaderRow = hasHeader,
)

class CsvImportReportTest {

    @Test
    fun `the report names the file and the outcome`() {
        val report = report()

        assertThat(report).contains("File: withings.csv")
        assertThat(report).contains("Outcome: completed")
    }

    @Test
    fun `an unnamed file is reported rather than left blank`() {
        assertThat(report(fileName = null)).contains("File: (unnamed)")
    }

    @Test
    fun `every tally from the run appears`() {
        val report = report()

        assertThat(report).contains("Rows read:       120")
        assertThat(report).contains("Records written: 118")
        assertThat(report).contains("Already present: 4")
        assertThat(report).contains("Rejected:        2")
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
    fun `every column is listed with the role it was given`() {
        val report = report()

        assertThat(report).contains("[0] Date -> date and time")
        assertThat(report).contains("[1] Weight (kg) -> weight (kilograms)")
        assertThat(report).contains("[3] Comments -> not imported")
    }

    @Test
    fun `a derived body fat says what it was derived from, not just its unit`() {
        // "kilograms" alone on a body-fat column would misdescribe what was written.
        assertThat(report()).contains(
            "[2] Fat mass (kg) -> bodyFat (kilograms as a share of the weight column)",
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
    fun `individual rejected rows name the row, column and value`() {
        val report = report(
            result = runResult(
                diagnostics = listOf(
                    CsvImportDiagnostic(
                        rowNumber = 7,
                        reason = CsvImportDiagnosticReason.UNPARSABLE_NUMBER,
                        columnIndex = 2,
                        detail = "n/a",
                    ),
                ),
                counts = mapOf(CsvImportDiagnosticReason.UNPARSABLE_NUMBER to 1),
            ),
        )

        assertThat(report).contains("Row 7 column 2: value not a number (n/a)")
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
}
