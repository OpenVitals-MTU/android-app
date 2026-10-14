package tech.mmarca.openvitals.features.imports.csv

import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord
import io.mockk.coEvery
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.contract.CycleJournalRepository
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient

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
    /** How many refusals the quota failure lasts; null means for ever. */
    rateLimitRefusals: Int? = null,
) {
    val inserted = mutableListOf<Record>()
    var batchCalls = 0
        private set

    val repository: AppleHealthImportRepository = mockk()

    /** The cycle journal, as a map: what a drip import writes beside Health Connect. */
    val journalEntries = mutableMapOf<LocalDate, CycleJournalEntry>()
    val journal: CycleJournalRepository = mockk()

    init {
        coEvery { journal.entry(any()) } answers { journalEntries[arg<LocalDate>(0)] }
        coEvery { journal.save(any()) } answers {
            val entry = arg<CycleJournalEntry>(0)
            journalEntries[entry.date] = entry
        }
        coEvery { repository.findMatchingImportedClientRecordIds(any(), any(), any(), any()) } answers {
            val wantedIds = arg<Set<String>>(3)
            wantedIds.intersect(existingIds)
        }
        coEvery { repository.insertImportedRecords(any()) } answers {
            val records = arg<List<Record>>(0)
            batchCalls++
            if (rateLimitAfterBatches != null && batchCalls > rateLimitAfterBatches) {
                val refusal = batchCalls - rateLimitAfterBatches
                if (rateLimitRefusals == null || refusal <= rateLimitRefusals) {
                    throw IllegalStateException("Quota has been exceeded")
                }
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

    /** The pauses the service asked for, in milliseconds; nothing actually sleeps. */
    private val pauses = mutableListOf<Long>()

    private fun service(harness: RepositoryHarness): CsvImportService =
        CsvImportService(harness.repository, harness.journal, waitMillis = { millis -> pauses += millis })

    @Test
    fun `every row of a clean file is written and the run completes`() = runTest {
        val harness = RepositoryHarness()
        val content = csvOf(120)

        val result = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            plan = CsvImportPlan.Columns(weightMapping()),
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
            plan = CsvImportPlan.Columns(weightMapping()),
        )
        val existing = probe.inserted.mapNotNullTo(mutableSetOf()) { it.metadata.clientRecordId }

        val harness = RepositoryHarness(existingIds = existing)
        val result = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            plan = CsvImportPlan.Columns(weightMapping()),
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
            plan = CsvImportPlan.Columns(weightMapping()),
        )

        assertEquals(3, result.progress.rowsRead)
        assertEquals(2, harness.inserted.size)
    }

    @Test
    fun `two foods logged at the same time are both written, and the next run replaces them`() = runTest {
        val content = "Date,Food,Energy (kcal)\n2026-10-04 12:30:00,Oatmeal,350\n2026-10-04 12:30:00,Milk,120\n"
        val mapping = CsvImportMapping(
            columns = listOf(
                CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP),
                CsvColumnMapping(columnIndex = 1, role = CsvColumnRole.FOOD_NAME),
                CsvColumnMapping(columnIndex = 2, role = CsvColumnRole.NUTRIENT, nutrient = NutritionNutrient.ENERGY),
            ),
            dateTime = CsvDateTimeSettings(format = CsvDateTimeFormat.YEAR_FIRST, zone = CsvTimeZoneMode.UTC),
        )
        suspend fun runWith(harness: RepositoryHarness) = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            plan = CsvImportPlan.Columns(mapping),
        )

        val first = RepositoryHarness()
        assertEquals(2, runWith(first).progress.written)
        val ids = first.inserted.map { it.metadata.clientRecordId }
        assertEquals(listOf("Oatmeal", "Milk"), first.inserted.map { (it as NutritionRecord).name })
        assertEquals(2, ids.toSet().size)

        val second = RepositoryHarness(existingIds = ids.filterNotNull().toSet())
        assertEquals(2, runWith(second).progress.alreadyPresent)
        assertEquals(ids, second.inserted.map { it.metadata.clientRecordId })
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
            plan = CsvImportPlan.Columns(weightMapping()),
        )

        assertEquals(CsvImportOutcome.COMPLETED, result.outcome)
        assertEquals(4, result.progress.written)
        assertEquals(1, result.progress.rejected)
        assertEquals(1, result.diagnosticCounts[CsvImportDiagnosticReason.WRITE_FAILED])
    }

    @Test
    fun `a rate-limited batch is written again after a pause and the run completes`() = runTest {
        val content = csvOf(900)
        // Batch size is 300: the second flush is refused twice, then accepted.
        val harness = RepositoryHarness(rateLimitAfterBatches = 1, rateLimitRefusals = 2)
        val countdowns = mutableListOf<Int>()

        val result = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            plan = CsvImportPlan.Columns(weightMapping()),
            onProgress = { progress -> progress.resumingInSeconds?.let { countdowns += it } },
        )

        assertEquals(CsvImportOutcome.COMPLETED, result.outcome)
        assertEquals(900, result.progress.written)
        assertEquals(900, harness.inserted.size)
        assertEquals(2, result.progress.quotaPauses)
        // The first two pauses of the ladder, 15 s then 30 s, counted down a second at a time.
        assertEquals(CSV_QUOTA_PAUSES_MILLIS[0] + CSV_QUOTA_PAUSES_MILLIS[1], pauses.sum())
        assertEquals(pauses.sum(), result.progress.quotaWaitMillis)
        assertEquals(15, countdowns.first())
        assertEquals(1, countdowns.last())
        assertEquals(null, result.progress.resumingInSeconds)
    }

    @Test
    fun `a rate-limited run stops and reports how far it got`() = runTest {
        val content = csvOf(900)
        // Batch size is 300, so the second flush is refused, and keeps being refused.
        val harness = RepositoryHarness(rateLimitAfterBatches = 1)

        val result = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            plan = CsvImportPlan.Columns(weightMapping()),
        )

        assertEquals(CsvImportOutcome.RATE_LIMITED, result.outcome)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Quota", ignoreCase = true))
        assertEquals(CSV_WRITE_BATCH_SIZE, result.progress.written)
        assertTrue(result.progress.rowsRead < 900)
        // It gave up only once the pause budget was spent, not on the first refusal.
        assertTrue(pauses.sum() >= CSV_QUOTA_PAUSE_BUDGET_MILLIS)
        assertTrue(pauses.sum() < CSV_QUOTA_PAUSE_BUDGET_MILLIS + CSV_QUOTA_PAUSES_MILLIS.last())
        assertEquals(result.progress.quotaPauses, harness.batchCalls - 2)
    }

    @Test
    fun `cancelling during a quota pause ends the run as cancelled within a second`() = runTest {
        val content = csvOf(600)
        val harness = RepositoryHarness(rateLimitAfterBatches = 1)
        val cancellation = CsvImportCancellation()

        val result = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            plan = CsvImportPlan.Columns(weightMapping()),
            cancellation = cancellation,
            onProgress = { progress ->
                if (progress.resumingInSeconds != null && progress.resumingInSeconds!! <= 12) cancellation.cancel()
            },
        )

        assertEquals(CsvImportOutcome.CANCELLED, result.outcome)
        assertEquals(CSV_WRITE_BATCH_SIZE, result.progress.written)
        // The cancel landed during the fourth one-second slice; nothing waited beyond it.
        assertEquals(4 * CSV_QUOTA_PAUSE_SLICE_MILLIS, pauses.sum())
    }

    @Test
    fun `a drip export writes its records to Health Connect and its journal days to Room`() = runTest {
        val header = "date,temperature.value,temperature.exclude,temperature.time,temperature.note,bleeding.value,bleeding.exclude," +
            "mucus.feeling,mucus.texture,mucus.value,mucus.exclude,note.value,pain.cramps,mood.sad"
        val content = header + "\n" +
            "2015-12-31,,,,,1,false,,,,,,true,true\n" +
            "2015-12-30,36.5,false,7:10,,0,true,,,,,\"slept badly.\",,\n" +
            "2015-12-29,,,,,,,,,,,,,\n"
        val harness = RepositoryHarness()
        val plan = detectDripExport(header.split(","), ZoneOffset.UTC)!!

        val result = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            plan = CsvImportPlan.Drip(plan),
        )

        assertEquals(CsvImportOutcome.COMPLETED, result.outcome)
        assertEquals(3, result.progress.rowsRead)
        // A flow day, and a spotting day with its temperature.
        assertEquals(3, result.progress.written)
        assertEquals(3, harness.inserted.size)
        assertEquals(2, result.progress.journalDays)
        assertEquals(setOf(LocalDate.of(2015, 12, 31), LocalDate.of(2015, 12, 30)), harness.journalEntries.keys)
        assertEquals("slept badly.", harness.journalEntries.getValue(LocalDate.of(2015, 12, 30)).notes)
        assertEquals(0, result.progress.rejected)

        // The same file again changes no journal day and rewrites the same ids.
        val again = service(harness).run(
            source = sourceOf(content),
            totalBytes = content.length.toLong(),
            dialect = Dialect,
            plan = CsvImportPlan.Drip(plan),
        )
        assertEquals(0, again.progress.journalDays)
        assertEquals(3, again.progress.written)
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
            plan = CsvImportPlan.Columns(weightMapping()),
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
            plan = CsvImportPlan.Columns(weightMapping()),
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
            plan = CsvImportPlan.Columns(weightMapping()),
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
            plan = CsvImportPlan.Columns(weightMapping()),
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
            plan = CsvImportPlan.Columns(weightMapping()),
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
            plan = CsvImportPlan.Columns(weightMapping()),
            onProgress = { progress ->
                progress.fraction?.let { fractions.add(it) }
            },
        )

        assertTrue(fractions.isNotEmpty())
        assertTrue(fractions.all { it in 0f..1f })
    }
}
