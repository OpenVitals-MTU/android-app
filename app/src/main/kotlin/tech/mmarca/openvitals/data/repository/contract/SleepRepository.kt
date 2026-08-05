package tech.mmarca.openvitals.data.repository.contract

import java.time.LocalDate
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.domain.query.SleepPeriodData
import tech.mmarca.openvitals.domain.model.DailySleepDuration
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.preferences.SleepWindow

interface SleepRepository {
    suspend fun loadSleepPeriod(
        query: PeriodLoadQuery,
        sleepWindow: SleepWindow,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): SleepPeriodData

    /**
     * One aggregate duration per day of the range, without fetching sessions —
     * the cheap read for range consumers such as the health report. Days the
     * provider recorded no sleep come back with `durationMs == 0`.
     */
    suspend fun loadDailySleepDurations(
        start: LocalDate,
        end: LocalDate,
        sleepWindow: SleepWindow,
    ): List<DailySleepDuration>

    suspend fun loadSleepSessions(start: LocalDate, end: LocalDate): List<SleepData>

    suspend fun loadSleepSession(id: String): SleepData?
}
