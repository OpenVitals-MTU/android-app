package tech.mmarca.openvitals.features.imports.csv

import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord
import io.mockk.coEvery
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository

private val Dialect = CsvDialect(fieldDelimiter = ",", eol = "\n")

private fun weightMapping(): CsvImportMapping = CsvImportMapping(
    columns = listOf(
        CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP),
        CsvColumnMapping(
            columnIndex = 1,
            role = CsvColumnRole.METRIC,
            metric = CsvImportMetric.WEIGHT,
            interpretation = CsvDirectValue(CsvUnit.KILOGRAMS),
        ),
    ),
    dateTime = CsvDateTimeSettings(
        format = CsvDateTimeFormat.YEAR_FIRST,
        zone = CsvTimeZoneMode.UTC,
    ),
)

/** A double over the repository: records inserts, refuses batches or one record, rate-limits, and reports existing ids. */
private class RepositoryHarness(
    existingIds: Set<String> = emptySet(),
    failEveryBatch: Boolean = false,
    rejectRecordWhere: ((Record) -> Boolean)? = null,
    rateLimitAfterBatches: Int? = null,
) {
    val inserted = mutableListOf<Record>()
    var batchCalls = 0
        private set

    val repository: AppleHealthImportRepository = mockk()

    init {
        coEvery { repository.findMatchingImportedClientRecordIds(any(), any(), any(), any()) } answers {
            val wantedIds = arg<Set<String>>(3)
            wantedIds.intersect(existingIds)
        }
        coEvery { repository.insertImportedRecords(any()) } answers {
            val records = arg<List<Record>>(0)
            batchCalls++
            if (rateLimitAfterBatches != null && batchCalls > rateLimitAfterBatches) {
                throw IllegalStateException("Quota has been exceeded")
            }
            if (records.size > 1 && failEveryBatch) {
                throw IllegalStateException("batch refused")
            }
            if (records.size == 1 && rejectRecordWhere?.invoke(records.single()) == true) {
                throw IllegalStateException("record refused")
            }
            inserted.addAll(records)
        }
    }
}

class CsvImportServiceTest {

    private fun sourceOf(content: String): CsvInputSource =
        CsvInputSource { ByteArrayInputStream(content.toByteArray(Charsets.UTF_8)) }

    /** [days] daily weigh-ins starting 2026-07-01. */
    private fun csvOf(days: Int): String = buildString {
        append("Date,Weight\n")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
        repeat(days) { day ->
            val date = Instant.parse("2026-07-01T00:00:00Z").plusSeconds(day * 86_400L)
            append("${formatter.format(date)} 08:12:00,${78 + day % 5}.0\n")
        }
    }

    private fun service(harness: RepositoryHarness): CsvImportService =
        CsvImportService(harness.repository)

    @Test
    fun `every row of a clean file is written and the run completes`() = runTest {
        val harness = RepositoryHarness()
        val content = csvOf(120)

        val result = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            mapping = weightMapping(),
        )

        assertEquals(CsvImportOutcome.COMPLETED, result.outcome)
        assertEquals(120, result.progress.rowsRead)
        assertEquals(120, result.progress.written)
        assertEquals(0, result.progress.rejected)
        assertEquals(120, harness.inserted.size)
    }

    @Test
    fun `records already in Health Connect are counted as present and still written so a corrected value upserts`() = runTest {
        val content = csvOf(3)
        // Pre-seed with the ids the file will produce.
        val probe = RepositoryHarness()
        service(probe).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            mapping = weightMapping(),
        )
        val existing = probe.inserted.mapNotNullTo(mutableSetOf()) { it.metadata.clientRecordId }

        val harness = RepositoryHarness(existingIds = existing)
        val result = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            mapping = weightMapping(),
        )

        assertEquals(3, result.progress.alreadyPresent)
        // Still inserted: the id excludes the value, so only a write carries a correction.
        assertEquals(3, harness.inserted.size)
        assertEquals(3, result.progress.written)
    }

    @Test
    fun `a duplicated row inside one file is written once`() = runTest {
        val content = "Date,Weight\n" +
            "2026-07-01 08:12:00,78.4\n" +
            "2026-07-01 08:12:00,78.4\n" +
            "2026-07-02 08:12:00,78.6\n"
        val harness = RepositoryHarness()

        val result = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            mapping = weightMapping(),
        )

        assertEquals(3, result.progress.rowsRead)
        assertEquals(2, harness.inserted.size)
    }

    @Test
    fun `a refused batch is retried record by record and only the bad record is counted as rejected`() = runTest {
        val content = csvOf(5)
        val harness = RepositoryHarness(
            failEveryBatch = true,
            rejectRecordWhere = { record ->
                record is WeightRecord && record.time == Instant.parse("2026-07-03T08:12:00Z")
            },
        )

        val result = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            mapping = weightMapping(),
        )

        assertEquals(CsvImportOutcome.COMPLETED, result.outcome)
        assertEquals(4, result.progress.written)
        assertEquals(1, result.progress.rejected)
        assertEquals(1, result.diagnosticCounts[CsvImportDiagnosticReason.WRITE_FAILED])
    }

    @Test
    fun `a rate-limited run stops and reports how far it got`() = runTest {
        val content = csvOf(900)
        // Batch size is 300, so the second flush is refused.
        val harness = RepositoryHarness(rateLimitAfterBatches = 1)

        val result = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            mapping = weightMapping(),
        )

        assertEquals(CsvImportOutcome.RATE_LIMITED, result.outcome)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Quota", ignoreCase = true))
        assertEquals(CSV_WRITE_BATCH_SIZE, result.progress.written)
        assertTrue(result.progress.rowsRead < 900)
    }

    @Test
    fun `cancelling mid-run keeps what was written and stops reading`() = runTest {
        val content = csvOf(2000)
        val harness = RepositoryHarness()
        val cancellation = CsvImportCancellation()

        val result = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            mapping = weightMapping(),
            cancellation = cancellation,
            onProgress = { progress ->
                if (progress.rowsRead >= 50) cancellation.cancel()
            },
        )

        assertEquals(CsvImportOutcome.CANCELLED, result.outcome)
        assertTrue(result.progress.rowsRead < 2000)
        assertEquals(result.progress.written, harness.inserted.size)
    }

    @Test
    fun `a malformed row is skipped with a diagnostic and the rest imports`() = runTest {
        val content = "Date,Weight\n" +
            "2026-07-01 08:12:00,78.4\n" +
            "not a date,78.5\n" +
            "2026-07-03 08:12:00,78.6\n"
        val harness = RepositoryHarness()

        val result = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            mapping = weightMapping(),
        )

        assertEquals(CsvImportOutcome.COMPLETED, result.outcome)
        assertEquals(2, result.progress.written)
        assertEquals(1, result.diagnosticCounts[CsvImportDiagnosticReason.UNPARSABLE_TIMESTAMP])
    }

    @Test
    fun `a missing file fails the run instead of throwing`() = runTest {
        val harness = RepositoryHarness()

        val result = service(harness).run(
            source = CsvInputSource { null },
            totalBytes = 0,
            dialect = Dialect,
            mapping = weightMapping(),
        )

        assertEquals(CsvImportOutcome.FAILED, result.outcome)
        assertNotNull(result.error)
        assertTrue(result.wroteNothing)
    }

    @Test
    fun `a file with only a header writes nothing and still completes`() = runTest {
        val content = "Date,Weight\n"
        val harness = RepositoryHarness()

        val result = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            mapping = weightMapping(),
        )

        assertEquals(CsvImportOutcome.COMPLETED, result.outcome)
        assertEquals(0, result.progress.written)
        assertEquals(0, harness.batchCalls)
    }

    @Test
    fun `the retained diagnostic log is capped while the counts stay complete`() = runTest {
        val content = buildString {
            append("Date,Weight\n")
            repeat(CSV_MAX_RETAINED_DIAGNOSTICS + 50) { append("not a date,78.4\n") }
        }
        val harness = RepositoryHarness()

        val result = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            mapping = weightMapping(),
        )

        assertEquals(CSV_MAX_RETAINED_DIAGNOSTICS, result.diagnostics.size)
        assertEquals(
            CSV_MAX_RETAINED_DIAGNOSTICS + 50,
            result.diagnosticCounts[CsvImportDiagnosticReason.UNPARSABLE_TIMESTAMP],
        )
    }

    @Test
    fun `progress reports a fraction of the file once bytes are known`() = runTest {
        val content = csvOf(200)
        val harness = RepositoryHarness()
        val fractions = mutableListOf<Float>()

        service(harness).run(
            source = sourceOf(content),
            totalBytes = content.toByteArray(Charsets.UTF_8).size.toLong(),
            dialect = Dialect,
            mapping = weightMapping(),
            onProgress = { progress ->
                progress.fraction?.let { fractions.add(it) }
            },
        )

        assertTrue(fractions.isNotEmpty())
        assertTrue(fractions.all { it in 0f..1f })
    }
}
