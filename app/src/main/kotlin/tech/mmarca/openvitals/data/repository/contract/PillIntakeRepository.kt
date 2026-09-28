package tech.mmarca.openvitals.data.repository.contract

import java.time.LocalDate

/** The days the pill was marked as taken. The scheme itself lives in [CyclePreferences]. */
interface PillIntakeRepository {
    suspend fun isTaken(date: LocalDate): Boolean

    /** The taken days between two dates, inclusive. */
    suspend fun takenDays(start: LocalDate, end: LocalDate): Set<LocalDate>

    /** Marks or unmarks one day. */
    suspend fun setTaken(date: LocalDate, taken: Boolean)

    suspend fun deleteAll()
}
