package tech.mmarca.openvitals.data.repository

import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.data.local.cycle.PillIntakeDao
import tech.mmarca.openvitals.data.local.cycle.PillIntakeEntity
import tech.mmarca.openvitals.data.repository.contract.PillIntakeRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Pill intakes live in Room because Health Connect has no record type for them. */
@Singleton
class PillIntakeRepositoryImpl @Inject constructor(
    private val dao: PillIntakeDao,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) : PillIntakeRepository {

    override suspend fun isTaken(date: LocalDate): Boolean = withContext(dispatchers.io) {
        dao.intake(date.toString()) != null
    }

    override suspend fun takenDays(start: LocalDate, end: LocalDate): Set<LocalDate> = withContext(dispatchers.io) {
        dao.datesBetween(start.toString(), end.toString()).mapTo(mutableSetOf()) { LocalDate.parse(it) }
    }

    override suspend fun setTaken(date: LocalDate, taken: Boolean) = withContext(dispatchers.io) {
        if (taken) {
            dao.upsert(PillIntakeEntity(date = date.toString(), takenAtMillis = System.currentTimeMillis()))
        } else {
            dao.delete(date.toString())
        }
    }

    override suspend fun deleteAll() = withContext(dispatchers.io) {
        dao.deleteAll()
    }
}
