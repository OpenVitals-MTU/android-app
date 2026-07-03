package tech.mmarca.openvitals.data.repository.contract

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.model.CaffeinePeriodData
import tech.mmarca.openvitals.domain.model.RefreshMode

interface CaffeineRepository {
    suspend fun loadCaffeinePeriod(
        query: PeriodLoadQuery,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): CaffeinePeriodData = loadCaffeineData(query.windows.current, refreshMode)

    suspend fun loadCaffeineData(
        period: DatePeriod,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): CaffeinePeriodData
}
