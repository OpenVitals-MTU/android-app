package tech.mmarca.openvitals.data.repository.contract

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.model.CycleJournalEntry

/** In-memory journal. Set what a test needs, read back what the code wrote. */
class FakeCycleJournalRepository(
    initialEntries: List<CycleJournalEntry> = emptyList(),
    initialExclusions: Map<LocalDate, CycleExclusionReason?> = emptyMap(),
) : CycleJournalRepository {

    val entries = initialEntries.associateBy { it.date }.toMutableMap()
    val exclusionsByDate = initialExclusions.toMutableMap()
    private val changeFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    override suspend fun entry(date: LocalDate): CycleJournalEntry? = entries[date]

    override suspend fun entries(start: LocalDate, end: LocalDate): List<CycleJournalEntry> =
        entries.values.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }.sortedBy { it.date }

    override suspend fun allEntries(): List<CycleJournalEntry> = entries.values.sortedBy { it.date }

    override fun changes(): Flow<Unit> = changeFlow

    override suspend fun save(entry: CycleJournalEntry) {
        if (entry.hasObservations) entries[entry.date] = entry else entries.remove(entry.date)
        changeFlow.tryEmit(Unit)
    }

    override suspend fun restore(entry: CycleJournalEntry) {
        if (entry.hasObservations) entries[entry.date] = entry else entries.remove(entry.date)
        changeFlow.tryEmit(Unit)
    }

    override suspend fun delete(date: LocalDate) {
        entries.remove(date)
        changeFlow.tryEmit(Unit)
    }

    override suspend fun exclusions(): Map<LocalDate, CycleExclusionReason?> = exclusionsByDate.toMap()

    override suspend fun exclude(start: LocalDate, end: LocalDate?, reason: CycleExclusionReason?) {
        include(start, end)
        exclusionsByDate[start] = reason
        changeFlow.tryEmit(Unit)
    }

    override suspend fun include(start: LocalDate, end: LocalDate?) {
        exclusionsByDate.keys.removeAll { !it.isBefore(start) && (end == null || !it.isAfter(end)) }
        changeFlow.tryEmit(Unit)
    }

    override suspend fun deleteAll() {
        entries.clear()
        exclusionsByDate.clear()
        changeFlow.tryEmit(Unit)
    }
}
