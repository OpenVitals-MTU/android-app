package tech.mmarca.openvitals.data.repository.contract

import java.time.LocalDate
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.domain.cycle.CycleStatistics
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.CycleEntry
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleEntryWriteRequest
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.query.CyclePeriodData

interface CycleRepository {
    val phase4Permissions: Set<String>

    suspend fun missingPermissions(): Set<String>

    suspend fun loadCyclePeriod(
        query: PeriodLoadQuery,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): CyclePeriodData

    suspend fun loadCycleData(start: LocalDate, end: LocalDate): CycleData

    fun cycleWritePermissions(kind: CycleEntryKind): Set<String>

    suspend fun hasCycleWritePermission(kind: CycleEntryKind): Boolean

    suspend fun writeCycleEntry(request: CycleEntryWriteRequest): String

    suspend fun loadCycleEntry(kind: CycleEntryKind, id: String): CycleEntry?

    suspend fun updateCycleEntry(id: String, request: CycleEntryWriteRequest)

    suspend fun deleteCycleEntry(kind: CycleEntryKind, id: String)

    suspend fun loadCycleStatistics(today: LocalDate = LocalDate.now()): CycleStatistics?
}
