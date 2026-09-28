package tech.mmarca.openvitals.data.repository.contract

import java.time.LocalDate

/** In-memory pill intakes. */
class FakePillIntakeRepository : PillIntakeRepository {
    val taken = mutableSetOf<LocalDate>()

    override suspend fun isTaken(date: LocalDate): Boolean = date in taken

    override suspend fun takenDays(start: LocalDate, end: LocalDate): Set<LocalDate> =
        taken.filterTo(mutableSetOf()) { !it.isBefore(start) && !it.isAfter(end) }

    override suspend fun setTaken(date: LocalDate, taken: Boolean) {
        if (taken) this.taken += date else this.taken -= date
    }

    override suspend fun deleteAll() {
        taken.clear()
    }
}
