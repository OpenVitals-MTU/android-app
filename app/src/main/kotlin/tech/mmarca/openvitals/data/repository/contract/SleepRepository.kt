package tech.mmarca.openvitals.data.repository.contract

import java.time.LocalDate
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.domain.query.SleepPeriodData
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode

interface SleepRepository {
    suspend fun loadSleepPeriod(
        query: PeriodLoadQuery,
        sleepRangeMode: SleepRangeMode,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): SleepPeriodData

    suspend fun loadSleepSessions(start: LocalDate, end: LocalDate): List<SleepData>

    suspend fun loadSleepSession(id: String): SleepData?
}
