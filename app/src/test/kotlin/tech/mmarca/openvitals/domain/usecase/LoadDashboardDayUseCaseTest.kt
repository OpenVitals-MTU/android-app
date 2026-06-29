package tech.mmarca.openvitals.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.data.repository.dashboard.DashboardDataLoader
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import java.time.LocalDate

class LoadDashboardDayUseCaseTest {

    private val dashboardDataLoader: DashboardDataLoader = mockk()
    private val useCase = LoadDashboardDayUseCase(dashboardDataLoader)

    @Test fun `delegates to dashboard data loader`() = runTest {
        val query = DashboardQuery(
            date = LocalDate.of(2026, 6, 1),
            sleepRangeMode = SleepRangeMode.EVENING_18H,
            activityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
        )
        val expected = DashboardData(date = LocalDate.of(2026, 6, 1))
        coEvery { dashboardDataLoader.loadDashboard(query) }.returns(expected)

        val result = useCase(query)

        coVerify { dashboardDataLoader.loadDashboard(query) }
        assertEquals(expected, result)
    }
}
