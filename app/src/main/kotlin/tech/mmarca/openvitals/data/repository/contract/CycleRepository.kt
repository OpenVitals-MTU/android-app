package tech.mmarca.openvitals.data.repository.contract

import java.time.LocalDate
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.domain.cycle.CycleStatistics
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.CycleDayLog
import tech.mmarca.openvitals.domain.model.CycleDayLogWrite
import tech.mmarca.openvitals.domain.model.CycleEntry
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleEntryWriteRequest
import tech.mmarca.openvitals.domain.query.CyclePeriodData

interface CycleRepository {
    val phase4Permissions: Set<String>

    suspend fun missingPermissions(): Set<String>

    suspend fun loadCyclePeriod(
        query: PeriodLoadQuery,
    ): CyclePeriodData

    suspend fun loadCycleData(start: LocalDate, end: LocalDate): CycleData

    fun cycleWritePermissions(kind: CycleEntryKind): Set<String>

    suspend fun hasCycleWritePermission(kind: CycleEntryKind): Boolean

    suspend fun writeCycleEntry(request: CycleEntryWriteRequest): String

    suspend fun updateCycleEntry(id: String, request: CycleEntryWriteRequest)

    suspend fun deleteCycleEntry(kind: CycleEntryKind, id: String)

    /**
     * Statistics over the last eighteen months of bleeding, with the user's
     * exclusions, contexts and age band applied. Null without the read permission.
     */
    suspend fun loadCycleStatistics(today: LocalDate = LocalDate.now()): CycleStatistics?

    /** Everything recorded for one day, for the day log. */
    suspend fun loadDayLog(date: LocalDate): CycleDayLog

    /**
     * Saves a day log: each Health Connect kind that changed is written,
     * updated or deleted, then the journal row is replaced. A kind that did not
     * change is not touched, so a missing write permission for it does not matter.
     */
    suspend fun saveDayLog(date: LocalDate, write: CycleDayLogWrite)
}
