package tech.mmarca.openvitals.data.repository.contract

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.model.CaffeinePeriodData

interface CaffeineRepository {
    suspend fun loadCaffeinePeriod(query: PeriodLoadQuery): CaffeinePeriodData =
        loadCaffeineData(query.windows.current)

    suspend fun loadCaffeineData(period: DatePeriod): CaffeinePeriodData
}
