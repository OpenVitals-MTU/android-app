package tech.mmarca.openvitals.features.imports.csv

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.healthconnect.HealthConnectRateLimitBackoff

/** Records handed to Health Connect per insert call. */
const val CSV_WRITE_BATCH_SIZE = 300

/**
 * Ids per existing-record lookup. Rows arrive in time order, so 500 daily
 * weigh-ins is one query covering roughly 1.4 years.
 */
const val CSV_DUPLICATE_LOOKUP_CHUNK = 500

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
 * Runs a CSV import: stream rows, convert, count what is already there, write
 * in batches, and tally the result.
 *
 * Deliberately NOT WorkManager and NOT a foreground service. The app declares
 * exactly one foreground service, which is why an Apple Health import already
 * refuses to run while a GPS recording is active — and that machinery exists
 * for multi-gigabyte exports taking tens of minutes. A body-composition CSV is
 * bounded by how often a human stands on a scale: ten years of twice-daily
 * weigh-ins is ~7,300 rows and a file well under a megabyte. Staying in-process
 * is not just cheaper, it means a CSV import can run *while* an activity is
 * being recorded.
 */
@Singleton
class CsvImportService @Inject constructor(
    private val importRepository: AppleHealthImportRepository,
) {
    private val reader = CsvTableReader()

    /**
     * Imports [source] under [mapping], reporting progress through [onProgress].
     *
     * [totalBytes] is the file's size from the SAF provider (zero when unknown),
     * used only as the progress denominator.
     *
     * Never throws: every failure becomes a [CsvImportResult] the screen renders.
     */
    suspend fun run(
        source: CsvInputSource,
        totalBytes: Long,
        dialect: CsvDialect,
        mapping: CsvImportMapping,
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

        // Guards against one file listing the same measurement twice. Cross-import
        // duplicates are a separate, chunked lookup below.
        val seenIds = mutableSetOf<String>()
        val pending = mutableListOf<CsvConvertedRecord>()

        fun finish(outcome: CsvImportOutcome, error: String? = null): CsvImportResult =
            CsvImportResult(
                outcome = outcome,
                progress = progress,
                diagnostics = diagnostics.toList(),
                diagnosticCounts = counts.toMap(),
                error = error,
            )

        var stopped: CsvImportResult? = null
        try {
            reader.rows(source, dialect = dialect, hasHeaderRow = hasHeaderRow)
                .collect { row ->
                    if (cancellation?.isCancelled == true) {
                        val flush = flush(pending, ::record)
                        progress = progress.apply(flush)
                        onProgress(progress)
                        stopped = finish(CsvImportOutcome.CANCELLED)
                        throw StopCollecting
                    }

                    val conversion = convertCsvRow(row = row, mapping = mapping)
                    conversion.diagnostics.forEach(::record)
                    progress = progress.copy(
                        rowsRead = progress.rowsRead + 1,
                        rejected = progress.rejected + conversion.diagnostics.size,
                        bytesRead = row.bytesRead,
                    )

                    for (candidate in conversion.records) {
                        if (seenIds.add(candidate.clientRecordId)) pending += candidate
                    }

                    if (pending.size >= CSV_WRITE_BATCH_SIZE) {
                        val flush = flush(pending, ::record)
                        if (flush.rateLimited) {
                            progress = progress.apply(flush)
                            onProgress(progress)
                            stopped = finish(CsvImportOutcome.RATE_LIMITED, error = flush.error)
                            throw StopCollecting
                        }
                        progress = progress.apply(flush)
                    }
                    onProgress(progress)
                }
            stopped?.let { return@withContext it }

            val flush = flush(pending, ::record)
            progress = progress.apply(flush)
            onProgress(progress)
            if (flush.rateLimited) {
                return@withContext finish(CsvImportOutcome.RATE_LIMITED, error = flush.error)
            }
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

    /**
     * Writes everything in [pending], clearing it. Counts how many ids were
     * already in Health Connect first — purely to report "you have imported this
     * before"; the write happens either way, because the id excludes the value
     * and so cannot say whether anything changed. Health Connect upserts.
     */
    private suspend fun flush(
        pending: MutableList<CsvConvertedRecord>,
        record: (CsvImportDiagnostic) -> Unit,
    ): FlushOutcome {
        if (pending.isEmpty()) return FlushOutcome()
        val batch = pending.toList()
        pending.clear()

        val alreadyPresent = countExisting(batch)

        val batchError = runCatching {
            importRepository.insertImportedRecords(batch.map { it.record })
        }.exceptionOrNull()

        if (batchError == null) {
            return FlushOutcome(written = batch.size, alreadyPresent = alreadyPresent)
        }
        if (HealthConnectRateLimitBackoff.isRateLimitFailure(batchError)) {
            return FlushOutcome(
                rateLimited = true,
                error = batchError.message,
                alreadyPresent = alreadyPresent,
            )
        }

        // The batch is ATOMIC — Health Connect wrote none of it and the failure
        // does not name the record it choked on. Retry singly so the good
        // records still land and only the guilty one is counted as rejected.
        var written = 0
        var rejected = 0
        for (single in batch) {
            val singleError = runCatching {
                importRepository.insertImportedRecords(listOf(single.record))
            }.exceptionOrNull()
            when {
                singleError == null -> written++
                HealthConnectRateLimitBackoff.isRateLimitFailure(singleError) -> return FlushOutcome(
                    written = written,
                    rejected = rejected,
                    alreadyPresent = alreadyPresent,
                    rateLimited = true,
                    error = singleError.message,
                )
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
        return FlushOutcome(written = written, rejected = rejected, alreadyPresent = alreadyPresent)
    }

    /**
     * How many of [batch]'s ids Health Connect already holds.
     *
     * Grouped by record type (the lookup takes one) and chunked, with the window
     * spanning only the chunk's own instants. A lookup failure is not fatal: it
     * costs an accurate "already present" count, not the import.
     */
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
)

private fun CsvImportProgress.apply(flush: FlushOutcome): CsvImportProgress = copy(
    written = written + flush.written,
    alreadyPresent = alreadyPresent + flush.alreadyPresent,
    rejected = rejected + flush.rejected,
)

/**
 * Aborts the row flow once a terminal result (cancel, rate limit) is assembled.
 * Flow collection has no `break`; this is the sanctioned equivalent.
 */
private class StopCollectingException : Exception() {
    override fun fillInStackTrace(): Throwable = this
}

private val StopCollecting = StopCollectingException()
