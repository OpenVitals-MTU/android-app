package tech.mmarca.openvitals.domain.usecase

import javax.inject.Inject
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailySleepDuration
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode

data class SleepPeriodLoadResult(
    val sessions: List<SleepData> = emptyList(),
    val previousSessions: List<SleepData> = emptyList(),
    val baselineSessions: List<SleepData> = emptyList(),
    val dailyDurations: List<DailySleepDuration> = emptyList(),
    val previousDailyDurations: List<DailySleepDuration> = emptyList(),
    val baselineDailyDurations: List<DailySleepDuration> = emptyList(),
    val crossDailyHrv: List<DailyHrv> = emptyList(),
)

class LoadSleepPeriodUseCase @Inject constructor(
    private val sleepRepository: SleepRepository,
    private val heartRepository: HeartRepository?,
) {
    suspend operator fun invoke(
        query: PeriodLoadQuery,
        sleepRangeMode: SleepRangeMode,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): SleepPeriodLoadResult {
        val periodData = if (refreshMode == RefreshMode.NORMAL) {
            sleepRepository.loadSleepPeriod(query, sleepRangeMode)
        } else {
            sleepRepository.loadSleepPeriod(query, sleepRangeMode, refreshMode)
        }
        val crossDailyHrv = heartRepository
            ?.loadDailyHRV(query.windows.current.start, query.windows.current.end)
            .orEmpty()
        return SleepPeriodLoadResult(
            sessions = periodData.sessions,
            previousSessions = periodData.previousSessions,
            baselineSessions = periodData.baselineSessions,
            dailyDurations = periodData.dailyDurations,
            previousDailyDurations = periodData.previousDailyDurations,
            baselineDailyDurations = periodData.baselineDailyDurations,
            crossDailyHrv = crossDailyHrv,
        )
    }
}
