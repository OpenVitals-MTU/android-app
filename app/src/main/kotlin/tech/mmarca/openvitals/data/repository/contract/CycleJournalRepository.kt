package tech.mmarca.openvitals.data.repository.contract

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.model.CycleJournalEntry

/**
 * The cycle journal: the day observations Health Connect has no record type
 * for, and the cycles the user keeps out of the estimate. A thin seam over
 * Room; interpretation belongs to `domain/cycle`.
 */
interface CycleJournalRepository {
    suspend fun entry(date: LocalDate): CycleJournalEntry?

    suspend fun entries(start: LocalDate, end: LocalDate): List<CycleJournalEntry>

    suspend fun allEntries(): List<CycleJournalEntry>

    /** Emits on every change, so screens reload without polling. */
    fun changes(): Flow<Unit>

    /** Stores the entry, or deletes the day's row when it holds nothing. */
    suspend fun save(entry: CycleJournalEntry)

    /** Stores an entry another phone edited, keeping its edit time so the newer side keeps winning. */
    suspend fun restore(entry: CycleJournalEntry)

    suspend fun delete(date: LocalDate)

    /** Reason per excluded date. Null reason means excluded without one. */
    suspend fun exclusions(): Map<LocalDate, CycleExclusionReason?>

    /** Excludes the cycle spanning [start]..[end], replacing any earlier exclusion inside it. */
    suspend fun exclude(start: LocalDate, end: LocalDate?, reason: CycleExclusionReason?)

    /** Includes the cycle spanning [start]..[end] again. */
    suspend fun include(start: LocalDate, end: LocalDate?)

    suspend fun deleteAll()
}
