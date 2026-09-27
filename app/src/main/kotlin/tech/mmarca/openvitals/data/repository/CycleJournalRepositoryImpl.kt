package tech.mmarca.openvitals.data.repository

import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.data.local.cycle.CycleExclusionEntity
import tech.mmarca.openvitals.data.local.cycle.CycleJournalDao
import tech.mmarca.openvitals.data.local.cycle.CycleJournalEntryEntity
import tech.mmarca.openvitals.data.repository.contract.CycleJournalRepository
import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.model.CycleJournalEntry

/** The journal lives in Room because Health Connect cannot hold subjective scales, symptoms or notes. */
@Singleton
class CycleJournalRepositoryImpl @Inject constructor(
    private val dao: CycleJournalDao,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) : CycleJournalRepository {

    override suspend fun entry(date: LocalDate): CycleJournalEntry? = withContext(dispatchers.io) {
        dao.entry(date.toString())?.toDomain()
    }

    override suspend fun entries(start: LocalDate, end: LocalDate): List<CycleJournalEntry> =
        withContext(dispatchers.io) {
            dao.entriesBetween(start.toString(), end.toString()).map { it.toDomain() }
        }

    override suspend fun allEntries(): List<CycleJournalEntry> = withContext(dispatchers.io) {
        dao.allEntries().map { it.toDomain() }
    }

    override fun changes(): Flow<Unit> =
        combine(dao.observeEntries(), dao.observeExclusions()) { _, _ -> Unit }.drop(1).map { }

    override suspend fun save(entry: CycleJournalEntry) = withContext(dispatchers.io) {
        if (entry.hasObservations) {
            dao.upsert(CycleJournalEntryEntity.fromDomain(entry.copy(updatedAt = Instant.now())))
        } else {
            dao.delete(entry.date.toString())
        }
    }

    override suspend fun restore(entry: CycleJournalEntry) = withContext(dispatchers.io) {
        if (entry.hasObservations) dao.upsert(CycleJournalEntryEntity.fromDomain(entry)) else dao.delete(entry.date.toString())
    }

    override suspend fun delete(date: LocalDate) = withContext(dispatchers.io) {
        dao.delete(date.toString())
    }

    override suspend fun exclusions(): Map<LocalDate, CycleExclusionReason?> = withContext(dispatchers.io) {
        dao.exclusions().associate { it.toDomain() }
    }

    override suspend fun exclude(start: LocalDate, end: LocalDate?, reason: CycleExclusionReason?) =
        withContext(dispatchers.io) {
            dao.deleteExclusionsBetween(start.toString(), (end ?: FAR_FUTURE).toString())
            dao.upsertExclusion(CycleExclusionEntity(startDate = start.toString(), reason = reason?.name))
        }

    override suspend fun include(start: LocalDate, end: LocalDate?) = withContext(dispatchers.io) {
        dao.deleteExclusionsBetween(start.toString(), (end ?: FAR_FUTURE).toString())
    }

    override suspend fun deleteAll() = withContext(dispatchers.io) {
        dao.deleteAllEntries()
        dao.deleteAllExclusions()
    }

    private companion object {
        /** An open cycle has no end; its exclusion window runs to here. */
        val FAR_FUTURE: LocalDate = LocalDate.of(9999, 12, 31)
    }
}
