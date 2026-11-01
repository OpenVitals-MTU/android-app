package tech.mmarca.openvitals.features.imports.csv

import androidx.health.connect.client.records.ExerciseSessionRecord
import tech.mmarca.openvitals.core.presentation.isPermissionFailure
import tech.mmarca.openvitals.core.privacy.PrivacyRedactor
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.importSessionClientRecordId
import tech.mmarca.openvitals.healthconnect.HealthConnectRateLimitBackoff

/** Workouts per `writeActivityEntries` call, as the route and FIT bulk import batches files. */
const val CSV_WORKOUT_BATCH_SIZE = 25

/**
 * Writes the workouts a run converts, through the activity repository so each
 * lands as the app's own: the session, the totals beside it and the series,
 * under ids a re-import replaces. Batches like the record flush, pauses on
 * quota like it, and retries a refused batch one workout at a time so one bad
 * row costs one row. A missing write permission is refused before anything is
 * written and would be refused again per row, so it ends the run instead.
 */
internal class CsvWorkoutWriter(
    private val activityRepository: ActivityRepository,
    /** The presence lookup only; the write goes through the activity repository. */
    private val importRepository: AppleHealthImportRepository,
    private val pauses: QuotaPauses,
    private val record: (CsvImportDiagnostic) -> Unit,
) {
    private class Pending(val rowNumber: Int, val request: ActivityWriteRequest)

    private val pending = mutableListOf<Pending>()

    /** One file listing a workout twice would put one client id twice in one insert. */
    private val seenImportKeys = mutableSetOf<String>()

    val isFull: Boolean get() = pending.size >= CSV_WORKOUT_BATCH_SIZE

    /** Queues [request]. False when its key was already queued this run; the row still counts as imported. */
    fun add(request: ActivityWriteRequest, rowNumber: Int): Boolean {
        val key = request.importKey
        if (key != null && !seenImportKeys.add(key)) return false
        pending += Pending(rowNumber, request)
        return true
    }

    /** Writes what is queued. [FlushOutcome.written] counts sessions, one per row. */
    suspend fun flush(): FlushOutcome {
        if (pending.isEmpty()) return FlushOutcome()
        val batch = pending.toList()
        pending.clear()

        val presence = countExisting(batch)
        val batchError = writeUntilAccepted(pauses) {
            activityRepository.writeActivityEntries(batch.map { it.request })
        }
        if (batchError == null) {
            return FlushOutcome(written = batch.size, alreadyPresent = presence.found, presenceUnchecked = presence.unchecked)
                .withPauses(pauses)
        }
        if (HealthConnectRateLimitBackoff.isRateLimitFailure(batchError)) {
            return FlushOutcome(
                rateLimited = true,
                error = batchError.message,
                alreadyPresent = presence.found,
                presenceUnchecked = presence.unchecked,
            ).withPauses(pauses)
        }
        if (batchError.isPermissionFailure()) {
            return FlushOutcome(
                failure = batchError,
                error = batchError.message,
                alreadyPresent = presence.found,
                presenceUnchecked = presence.unchecked,
            ).withPauses(pauses)
        }

        // The batch is atomic and the failure names no workout. Retry singly.
        var written = 0
        var rejected = 0
        for (single in batch) {
            val singleError = writeUntilAccepted(pauses) { activityRepository.writeActivityEntry(single.request) }
            when {
                singleError == null -> written++
                HealthConnectRateLimitBackoff.isRateLimitFailure(singleError) -> return FlushOutcome(
                    written = written,
                    rejected = rejected,
                    alreadyPresent = presence.found,
                    presenceUnchecked = presence.unchecked,
                    rateLimited = true,
                    error = singleError.message,
                ).withPauses(pauses)
                else -> {
                    rejected++
                    record(
                        CsvImportDiagnostic(
                            rowNumber = single.rowNumber,
                            reason = CsvImportDiagnosticReason.WRITE_FAILED,
                            detail = "ExerciseSessionRecord: ${PrivacyRedactor.describe(singleError)}",
                        ),
                    )
                }
            }
        }
        return FlushOutcome(
            written = written,
            rejected = rejected,
            alreadyPresent = presence.found,
            presenceUnchecked = presence.unchecked,
        ).withPauses(pauses)
    }

    /**
     * How many of [batch]'s sessions Health Connect already holds, by the client id
     * the import key gives them. A lookup that fails, for a missing read permission
     * or a quota, only costs the count: the batch is written all the same.
     */
    private suspend fun countExisting(batch: List<Pending>): PresenceCount {
        val wanted = batch.mapNotNullTo(mutableSetOf()) { it.request.importSessionClientRecordId() }
        if (wanted.isEmpty()) return PresenceCount(found = 0, unchecked = 0)
        val starts = batch.map { it.request.startTime }
        val found = runCatching {
            importRepository.findMatchingImportedClientRecordIds(
                recordType = ExerciseSessionRecord::class,
                start = starts.min().minusSeconds(1),
                end = starts.max().plusSeconds(1),
                wantedIds = wanted,
            )
        }.getOrNull()
        return if (found != null) PresenceCount(found = found.size, unchecked = 0) else PresenceCount(found = 0, unchecked = batch.size)
    }
}
