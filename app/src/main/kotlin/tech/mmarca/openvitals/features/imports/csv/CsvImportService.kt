package tech.mmarca.openvitals.features.imports.csv

import androidx.health.connect.client.records.Record
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.contract.CycleJournalRepository
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.healthconnect.HealthConnectRateLimitBackoff

/** Records handed to Health Connect per insert call. */
const val CSV_WRITE_BATCH_SIZE = 300

/** Ids per existing-record lookup. 500 daily weigh-ins is about 1.4 years. */
const val CSV_DUPLICATE_LOOKUP_CHUNK = 500

/**
 * How long each successive pause lasts once Health Connect refuses a batch for
 * quota. Its quota refills continuously, so the first pauses are short; the
 * last value repeats until [CSV_QUOTA_PAUSE_BUDGET_MILLIS] is spent.
 */
val CSV_QUOTA_PAUSES_MILLIS: List<Long> = listOf(15_000L, 30_000L, 60_000L, 120_000L, 300_000L)

/** Pausing without a single accepted write for this long ends the run as rate limited. */
const val CSV_QUOTA_PAUSE_BUDGET_MILLIS = 30 * 60_000L

/** The pause counts down in slices this long, so Cancel is answered promptly. */
internal const val CSV_QUOTA_PAUSE_SLICE_MILLIS = 1_000L

/** A cooperative cancel flag the UI flips; checked at row and batch boundaries. */
class CsvImportCancellation {
    @Volatile
    private var cancelled = false

    val isCancelled: Boolean get() = cancelled

    fun cancel() {
        cancelled = true
    }
}

/**
 * Runs a CSV import: stream rows, convert, count what is present, write in
 * batches, tally. In-process, not a foreground service: a CSV is bounded by
 * how often a human stands on a scale, and this way it runs during a recording.
 *
 * A batch Health Connect refuses for quota is not lost: the run pauses, with
 * [CSV_QUOTA_PAUSES_MILLIS] between attempts, and writes the same batch again.
 * A heart rate export of a million rows is a few thousand writes, more than a
 * quota window allows in one go, so the pauses are what let it finish.
 */
@Singleton
class CsvImportService internal constructor(
    private val importRepository: AppleHealthImportRepository,
    private val journalRepository: CycleJournalRepository,
    /** Suspends for the given time. Tests pass a recorder so no pause is real. */
    private val waitMillis: suspend (Long) -> Unit,
) {
    @Inject
    constructor(
        importRepository: AppleHealthImportRepository,
        journalRepository: CycleJournalRepository,
    ) : this(importRepository, journalRepository, { delay(it) })

    private val reader = CsvTableReader()

    /**
     * Imports [source] under [plan]. [totalBytes] is only the progress
     * denominator. Never throws: every failure becomes a [CsvImportResult].
     */
    suspend fun run(
        source: CsvInputSource,
        totalBytes: Long,
        dialect: CsvDialect,
        plan: CsvImportPlan,
        hasHeaderRow: Boolean = true,
        onProgress: (CsvImportProgress) -> Unit = {},
        cancellation: CsvImportCancellation? = null,
    ): CsvImportResult = withContext(Dispatchers.IO) {
        var progress = CsvImportProgress(totalBytes = totalBytes)
        val diagnostics = mutableListOf<CsvImportDiagnostic>()
        val counts = mutableMapOf<CsvImportDiagnosticReason, Int>()

        fun record(diagnostic: CsvImportDiagnostic) {
            counts[diagnostic.reason] = (counts[diagnostic.reason] ?: 0) + 1
            if (diagnostics.size < CSV_MAX_RETAINED_DIAGNOSTICS) {
                diagnostics += diagnostic
            }
        }

        // Guards against one file listing the same measurement twice. Nutrition
        // records are numbered instead: two foods at one instant are both eaten.
        val seenIds = mutableSetOf<String>()
        val nutritionOccurrences = CsvNutritionOccurrences()
        val pending = mutableListOf<CsvConvertedRecord>()

        val convert: (CsvRow) -> CsvRowConversion = when (plan) {
            is CsvImportPlan.Columns -> { row -> convertCsvRow(row, plan.mapping, nutritionOccurrences) }
            is CsvImportPlan.Drip -> { row -> convertDripRow(row, plan.plan) }
        }

        fun finish(outcome: CsvImportOutcome, error: String? = null): CsvImportResult =
            CsvImportResult(
                outcome = outcome,
                progress = progress,
                diagnostics = diagnostics.toList(),
                diagnosticCounts = counts.toMap(),
                error = error,
            )

        val pauses = QuotaPauses(
            cancellation = cancellation,
            onCountdown = { remainingSeconds ->
                onProgress(progress.copy(resumingInSeconds = remainingSeconds))
            },
        )

        /** The outcome a refused flush ends with: cancelled if the user stopped the wait. */
        fun stoppedBy(flush: FlushOutcome): CsvImportResult {
            progress = progress.apply(flush)
            onProgress(progress)
            return if (cancellation?.isCancelled == true) {
                finish(CsvImportOutcome.CANCELLED)
            } else {
                finish(CsvImportOutcome.RATE_LIMITED, error = flush.error)
            }
        }

        var stopped: CsvImportResult? = null
        try {
            reader.rows(source, dialect = dialect, hasHeaderRow = hasHeaderRow)
                .collect { row ->
                    if (cancellation?.isCancelled == true) {
                        val flush = flush(pending, ::record, pauses)
                        progress = progress.apply(flush)
                        onProgress(progress)
                        stopped = finish(CsvImportOutcome.CANCELLED)
                        throw StopCollecting
                    }

                    val conversion = convert(row)
                    conversion.diagnostics.forEach(::record)
                    // Room, not Health Connect: written at once, outside the batches and their quota.
                    val journalWritten = conversion.journalEntry?.let { writeJournalEntry(it) } == true
                    progress = progress.copy(
                        rowsRead = progress.rowsRead + 1,
                        rejected = progress.rejected + conversion.diagnostics.size,
                        journalDays = progress.journalDays + if (journalWritten) 1 else 0,
                        bytesRead = row.bytesRead,
                    )

                    for (candidate in conversion.records) {
                        if (seenIds.add(candidate.clientRecordId)) pending += candidate
                    }

                    if (pending.size >= CSV_WRITE_BATCH_SIZE) {
                        val flush = flush(pending, ::record, pauses)
                        if (flush.rateLimited) {
                            stopped = stoppedBy(flush)
                            throw StopCollecting
                        }
                        progress = progress.apply(flush)
                    }
                    onProgress(progress)
                }
            stopped?.let { return@withContext it }

            val flush = flush(pending, ::record, pauses)
            if (flush.rateLimited) {
                return@withContext stoppedBy(flush)
            }
            progress = progress.apply(flush)
            onProgress(progress)
            finish(CsvImportOutcome.COMPLETED)
        } catch (stop: StopCollectingException) {
            // The result was assembled before aborting the collection.
            stopped ?: finish(CsvImportOutcome.FAILED, error = "Import stopped unexpectedly.")
        } catch (error: CsvReadException) {
            finish(CsvImportOutcome.FAILED, error = error.message)
        } catch (error: Exception) {
            finish(CsvImportOutcome.FAILED, error = error.toString())
        }
    }

    /** Lays [imported] over the day's journal entry. True when the day changed. */
    private suspend fun writeJournalEntry(imported: CycleJournalEntry): Boolean {
        val existing = journalRepository.entry(imported.date)
        val merged = mergeDripJournalEntry(existing, imported) ?: return false
        journalRepository.save(merged)
        return true
    }

    /**
     * Writes [pending]. Counts ids already in Health Connect first, to
     * report only; the write happens either way, since Health Connect upserts.
     * A quota refusal pauses through [pauses] and tries the same write again.
     */
    private suspend fun flush(
        pending: MutableList<CsvConvertedRecord>,
        record: (CsvImportDiagnostic) -> Unit,
        pauses: QuotaPauses,
    ): FlushOutcome {
        if (pending.isEmpty()) return FlushOutcome()
        val batch = pending.toList()
        pending.clear()

        val alreadyPresent = countExisting(batch)

        val batchError = writeUntilAccepted(batch.map { it.record }, pauses)
        if (batchError == null) {
            return FlushOutcome(written = batch.size, alreadyPresent = alreadyPresent).withPauses(pauses)
        }
        if (HealthConnectRateLimitBackoff.isRateLimitFailure(batchError)) {
            return FlushOutcome(
                rateLimited = true,
                error = batchError.message,
                alreadyPresent = alreadyPresent,
            ).withPauses(pauses)
        }

        // The batch is atomic and the failure names no record. Retry singly.
        var written = 0
        var rejected = 0
        for (single in batch) {
            val singleError = writeUntilAccepted(listOf(single.record), pauses)
            when {
                singleError == null -> written++
                HealthConnectRateLimitBackoff.isRateLimitFailure(singleError) -> return FlushOutcome(
                    written = written,
                    rejected = rejected,
                    alreadyPresent = alreadyPresent,
                    rateLimited = true,
                    error = singleError.message,
                ).withPauses(pauses)
                else -> {
                    rejected++
                    record(
                        CsvImportDiagnostic(
                            rowNumber = 0,
                            reason = CsvImportDiagnosticReason.WRITE_FAILED,
                            detail = "${single.targetType}: $singleError",
                        ),
                    )
                }
            }
        }
        return FlushOutcome(written = written, rejected = rejected, alreadyPresent = alreadyPresent).withPauses(pauses)
    }

    /**
     * Inserts [records], pausing and retrying while Health Connect refuses them
     * for quota. Null on success; the last error when the write was refused for
     * another reason, or still for quota once [pauses] would not wait again.
     */
    private suspend fun writeUntilAccepted(records: List<Record>, pauses: QuotaPauses): Throwable? {
        while (true) {
            val error = runCatching { importRepository.insertImportedRecords(records) }.exceptionOrNull()
            if (error == null) {
                pauses.writeAccepted()
                return null
            }
            if (!HealthConnectRateLimitBackoff.isRateLimitFailure(error)) return error
            if (!pauses.pause()) return error
        }
    }

    /** Tallies the pauses taken so far into this flush's outcome and resets the tally. */
    private fun FlushOutcome.withPauses(pauses: QuotaPauses): FlushOutcome =
        copy(quotaPauses = pauses.takeCount(), quotaWaitMillis = pauses.takeWaitMillis())

    /**
     * The pauses one run takes for quota. A pause counts down in slices so a
     * cancel is seen within a second; a streak of pauses without an accepted
     * write is capped by [CSV_QUOTA_PAUSE_BUDGET_MILLIS].
     */
    private inner class QuotaPauses(
        private val cancellation: CsvImportCancellation?,
        private val onCountdown: (remainingSeconds: Int) -> Unit,
    ) {
        private var streak = 0
        private var streakMillis = 0L
        private var count = 0
        private var waitMillisTotal = 0L

        fun writeAccepted() {
            streak = 0
            streakMillis = 0L
        }

        /** Waits out the next pause. False when the budget is spent or the user cancelled. */
        suspend fun pause(): Boolean {
            if (streakMillis >= CSV_QUOTA_PAUSE_BUDGET_MILLIS) return false
            val pauseMillis = CSV_QUOTA_PAUSES_MILLIS[minOf(streak, CSV_QUOTA_PAUSES_MILLIS.lastIndex)]
            streak++
            streakMillis += pauseMillis
            count++
            var remaining = pauseMillis
            while (remaining > 0L) {
                if (cancellation?.isCancelled == true) return false
                onCountdown(((remaining + 999L) / 1_000L).toInt())
                val slice = minOf(remaining, CSV_QUOTA_PAUSE_SLICE_MILLIS)
                waitMillis(slice)
                waitMillisTotal += slice
                remaining -= slice
            }
            return cancellation?.isCancelled != true
        }

        fun takeCount(): Int = count.also { count = 0 }

        fun takeWaitMillis(): Long = waitMillisTotal.also { waitMillisTotal = 0L }
    }

    /** How many of [batch]'s ids Health Connect holds. A lookup failure only costs the count. */
    private suspend fun countExisting(batch: List<CsvConvertedRecord>): Int {
        var total = 0
        for ((recordType, records) in batch.groupBy { it.recordType }) {
            var start = 0
            while (start < records.size) {
                val chunk = records.subList(start, minOf(start + CSV_DUPLICATE_LOOKUP_CHUNK, records.size))
                start += CSV_DUPLICATE_LOOKUP_CHUNK
                val times = chunk.map { it.instant }.sorted()
                if (times.isEmpty()) continue
                val found = runCatching {
                    importRepository.findMatchingImportedClientRecordIds(
                        recordType = recordType,
                        start = times.first().minusSeconds(1),
                        end = times.last().plusSeconds(1),
                        wantedIds = chunk.mapTo(mutableSetOf()) { it.clientRecordId },
                    )
                }.getOrNull()
                if (found != null) total += found.size
            }
        }
        return total
    }
}

private data class FlushOutcome(
    val written: Int = 0,
    val alreadyPresent: Int = 0,
    val rejected: Int = 0,
    val rateLimited: Boolean = false,
    val error: String? = null,
    val quotaPauses: Int = 0,
    val quotaWaitMillis: Long = 0L,
)

private fun CsvImportProgress.apply(flush: FlushOutcome): CsvImportProgress = copy(
    written = written + flush.written,
    alreadyPresent = alreadyPresent + flush.alreadyPresent,
    rejected = rejected + flush.rejected,
    quotaPauses = quotaPauses + flush.quotaPauses,
    quotaWaitMillis = quotaWaitMillis + flush.quotaWaitMillis,
    resumingInSeconds = null,
)

/** Aborts the row flow once a terminal result is assembled. Flow collection has no `break`. */
private class StopCollectingException : Exception() {
    override fun fillInStackTrace(): Throwable = this
}

private val StopCollecting = StopCollectingException()
