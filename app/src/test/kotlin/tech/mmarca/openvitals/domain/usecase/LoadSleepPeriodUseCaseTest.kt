package tech.mmarca.openvitals.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.domain.query.SleepPeriodData
import java.time.LocalDate

class LoadSleepPeriodUseCaseTest {

    private val sleepRepository: SleepRepository = mockk()
    private val heartRepository: HeartRepository = mockk()
    private val useCase = LoadSleepPeriodUseCase(sleepRepository, heartRepository)

    private val query = PeriodLoadQuery(
        range = TimeRange.WEEK,
        anchorDate = LocalDate.of(2026, 6, 1),
        weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    )

    @Test
    fun `loads sleep period and daily hrv in parallel windows`() = runTest {
        coEvery {
            sleepRepository.loadSleepPeriod(query, SleepRangeMode.EVENING_18H)
        }.returns(
            SleepPeriodData(
                sessions = emptyList(),
                previousSessions = emptyList(),
                baselineSessions = emptyList(),
            ),
        )
        coEvery {
            heartRepository.loadDailyHRV(query.windows.current.start, query.windows.current.end)
        }.returns(listOf(DailyHrv(date = LocalDate.of(2026, 6, 1), rmssdMs = 42.0)))

        val result = useCase(query, SleepRangeMode.EVENING_18H)

        coVerify { sleepRepository.loadSleepPeriod(query, SleepRangeMode.EVENING_18H) }
        coVerify {
            heartRepository.loadDailyHRV(query.windows.current.start, query.windows.current.end)
        }
        assertEquals(42.0, result.crossDailyHrv.single().rmssdMs, 0.0)
    }

    @Test
    fun `force refresh passes refresh mode to sleep repository`() = runTest {
        coEvery {
            sleepRepository.loadSleepPeriod(query, SleepRangeMode.EVENING_18H, RefreshMode.FORCE)
        }.returns(SleepPeriodData())
        coEvery {
            heartRepository.loadDailyHRV(query.windows.current.start, query.windows.current.end)
        }.returns(emptyList())

        useCase(query, SleepRangeMode.EVENING_18H, RefreshMode.FORCE)

        coVerify {
            sleepRepository.loadSleepPeriod(query, SleepRangeMode.EVENING_18H, RefreshMode.FORCE)
        }
    }

    @Test
    fun `skips hrv load when heart repository is unavailable`() = runTest {
        val useCaseWithoutHeart = LoadSleepPeriodUseCase(sleepRepository, heartRepository = null)
        coEvery {
            sleepRepository.loadSleepPeriod(query, SleepRangeMode.EVENING_18H)
        }.returns(SleepPeriodData())

        val result = useCaseWithoutHeart(query, SleepRangeMode.EVENING_18H)

        assertTrue(result.crossDailyHrv.isEmpty())
    }
}
