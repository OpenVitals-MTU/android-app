package tech.mmarca.openvitals.data.repository.contract

import java.time.LocalDate
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.domain.model.CycleData
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
}
