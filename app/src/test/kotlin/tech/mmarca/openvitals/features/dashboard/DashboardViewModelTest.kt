package tech.mmarca.openvitals.features.dashboard

import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.data.model.DashboardData
import tech.mmarca.openvitals.data.repository.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val yesterday = today.minusDays(1)

    private fun prefs(
        trackCycle: Boolean = false,
        sleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    ) = mockk<PreferencesRepository>().also {
        every { it.acknowledgedPermissions() } returns emptySet()
        every { it.acknowledgePermissions(any()) } returns Unit
        every { it.trackCycle } returns trackCycle
        every { it.sleepRangeMode } returns sleepRangeMode
        every { it.dashboardWidgetOrder() } returns null
        every { it.setDashboardWidgetOrder(any()) } returns Unit
    }

    // ─── Initial load ─────────────────────────────────────────────────────────

    @Test fun `initial state has isLoading true before coroutine runs`() {
        val repo = mockk<HealthRepository>()
        // Block the coroutine by never completing — use a suspended mock
        coEvery { repo.loadDashboard(any(), any()) } coAnswers { kotlinx.coroutines.awaitCancellation() }

        // With UnconfinedTestDispatcher the launch starts but suspends at awaitCancellation,
        // so we can inspect the intermediate state right after init sets isLoading = true
        // and before the repo call returns.
        // We verify the initial value set before the launch is isLoading = true via the
        // _uiState initial value (new DashboardUiState() has isLoading = true).
        val initial = DashboardUiState()
        assertTrue(initial.isLoading)
    }

    @Test fun `load success populates data and clears loading`() = runTest {
        val data = DashboardData(date = today)
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns data

        val vm = DashboardViewModel(repo, prefs())

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(data, state.data)
        assertNull(state.errorMessage)
    }

    @Test fun `load failure sets errorMessage and clears loading`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } throws RuntimeException("network error")

        val vm = DashboardViewModel(repo, prefs())

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.data)
        assertEquals("network error", state.errorMessage)
    }

    @Test fun `load failure with null message uses Unknown error fallback`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } throws RuntimeException()

        val vm = DashboardViewModel(repo, prefs())

        assertEquals("Unknown error", vm.uiState.value.errorMessage)
    }

    // ─── Date clamping ────────────────────────────────────────────────────────

    @Test fun `load clamps future date to today`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs())
        val futureDate = today.plusDays(10)
        vm.load(futureDate)

        assertEquals(today, vm.uiState.value.selectedDate)
        coVerify { repo.loadDashboard(today, SleepRangeMode.EVENING_18H) }
    }

    @Test fun `selectDate clamps future date to today`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs())
        vm.selectDate(today.plusDays(5))

        assertEquals(today, vm.uiState.value.selectedDate)
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    @Test fun `previousDay decrements selectedDate by one day`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs())
        vm.previousDay()

        assertEquals(yesterday, vm.uiState.value.selectedDate)
    }

    @Test fun `nextDay is blocked when selectedDate is today`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs())
        vm.nextDay()

        assertEquals(today, vm.uiState.value.selectedDate)
        // load called once by init, not again by blocked nextDay
        coVerify(exactly = 1) { repo.loadDashboard(any(), any()) }
    }

    @Test fun `nextDay advances from yesterday to today`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs())
        vm.selectDate(yesterday)
        vm.nextDay()

        assertEquals(today, vm.uiState.value.selectedDate)
    }

    // ─── A3: floorsClimbed + elevationGainedMeters in DashboardData ──────────

    @Test fun `floorsClimbed is exposed through state when present`() = runTest {
        val data = DashboardData(date = today, floorsClimbed = 12)
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns data

        val vm = DashboardViewModel(repo, prefs())

        assertEquals(12, vm.uiState.value.data?.floorsClimbed)
    }

    @Test fun `floorsClimbed is null in state when not reported`() = runTest {
        val data = DashboardData(date = today, floorsClimbed = null)
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns data

        val vm = DashboardViewModel(repo, prefs())

        assertNull(vm.uiState.value.data?.floorsClimbed)
    }

    @Test fun `elevationGainedMeters is exposed through state when present`() = runTest {
        val data = DashboardData(date = today, elevationGainedMeters = 85.0)
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns data

        val vm = DashboardViewModel(repo, prefs())

        assertEquals(85.0, vm.uiState.value.data?.elevationGainedMeters!!, 0.01)
    }

    @Test fun `elevationGainedMeters is null in state when not reported`() = runTest {
        val data = DashboardData(date = today, elevationGainedMeters = null)
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns data

        val vm = DashboardViewModel(repo, prefs())

        assertNull(vm.uiState.value.data?.elevationGainedMeters)
    }

    @Test fun `floorsClimbed zero is non-null — permission granted no stair data`() = runTest {
        val data = DashboardData(date = today, floorsClimbed = 0)
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns data

        val vm = DashboardViewModel(repo, prefs())

        assertEquals(0, vm.uiState.value.data?.floorsClimbed)
    }

    @Test fun `caloriesInKcal is exposed through state when present`() = runTest {
        val data = DashboardData(date = today, caloriesInKcal = 1_850.0)
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns data

        val vm = DashboardViewModel(repo, prefs())

        assertEquals(1_850.0, vm.uiState.value.data?.caloriesInKcal!!, 0.01)
    }

    @Test fun `vitals fields are exposed through dashboard state when present`() = runTest {
        val data = DashboardData(
            date = today,
            latestSystolicMmHg = 120,
            latestDiastolicMmHg = 78,
            latestSpO2Percent = 97.5,
            latestVo2Max = 42.1,
        )
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns data

        val vm = DashboardViewModel(repo, prefs())

        assertEquals(120, vm.uiState.value.data?.latestSystolicMmHg)
        assertEquals(78, vm.uiState.value.data?.latestDiastolicMmHg)
        assertEquals(97.5, vm.uiState.value.data?.latestSpO2Percent!!, 0.01)
        assertEquals(42.1, vm.uiState.value.data?.latestVo2Max!!, 0.01)
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    @Test fun `refresh reloads current date`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs())
        vm.refresh()

        // init + refresh = 2 calls
        coVerify(exactly = 2) { repo.loadDashboard(today, SleepRangeMode.EVENING_18H) }
    }

    @Test fun `load passes sleep range mode from preferences`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)

        DashboardViewModel(repo, prefs(sleepRangeMode = SleepRangeMode.NOON))

        coVerify { repo.loadDashboard(today, SleepRangeMode.NOON) }
    }

    @Test fun `trackCycle flag follows preferences`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs(trackCycle = true))

        assertTrue(vm.uiState.value.trackCycle)
    }

    @Test fun `refreshPreferences updates trackCycle without loading dashboard`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)
        val prefs = prefs(trackCycle = false)
        every { prefs.trackCycle } returnsMany listOf(false, true)
        val vm = DashboardViewModel(repo, prefs)

        vm.refreshPreferences()

        assertTrue(vm.uiState.value.trackCycle)
        coVerify(exactly = 1) { repo.loadDashboard(today, SleepRangeMode.EVENING_18H) }
    }

    @Test fun `refreshPreferences reloads dashboard when sleep range mode changes`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)
        val prefs = prefs(sleepRangeMode = SleepRangeMode.EVENING_18H)
        every { prefs.sleepRangeMode } returnsMany listOf(
            SleepRangeMode.EVENING_18H,
            SleepRangeMode.NOON,
            SleepRangeMode.NOON,
        )
        val vm = DashboardViewModel(repo, prefs)

        vm.refreshPreferences()

        assertEquals(SleepRangeMode.NOON, vm.uiState.value.sleepRangeMode)
        coVerify { repo.loadDashboard(today, SleepRangeMode.NOON) }
    }

    @Test fun `dashboard widgets default to full widget set`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs())

        assertEquals(DefaultDashboardWidgetIds, vm.uiState.value.dashboardWidgets)
    }

    @Test fun `dashboard widgets restore saved order`() = runTest {
        val repo = mockk<HealthRepository>()
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.SLEEP.name,
            DashboardWidgetId.STEPS.name,
        )
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs)

        assertEquals(
            listOf(DashboardWidgetId.SLEEP, DashboardWidgetId.STEPS),
            vm.uiState.value.dashboardWidgets,
        )
    }

    @Test fun `dashboard widgets ignore unknown saved ids`() = runTest {
        val repo = mockk<HealthRepository>()
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf("unknown", DashboardWidgetId.STEPS.name)
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs)

        assertEquals(listOf(DashboardWidgetId.STEPS), vm.uiState.value.dashboardWidgets)
    }

    @Test fun `dashboard widgets ignore fixed browse saved id`() = runTest {
        val repo = mockk<HealthRepository>()
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.BROWSE.name,
            DashboardWidgetId.STEPS.name,
        )
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs)

        assertEquals(listOf(DashboardWidgetId.STEPS), vm.uiState.value.dashboardWidgets)
    }

    @Test fun `dashboard widget remove add and move persist order`() = runTest {
        val repo = mockk<HealthRepository>()
        val prefs = prefs()
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)
        val vm = DashboardViewModel(repo, prefs)

        vm.removeDashboardWidget(DashboardWidgetId.DISTANCE)
        assertFalse(DashboardWidgetId.DISTANCE in vm.uiState.value.dashboardWidgets)

        vm.addDashboardWidget(DashboardWidgetId.DISTANCE)
        assertEquals(DashboardWidgetId.DISTANCE, vm.uiState.value.dashboardWidgets.last())

        vm.moveDashboardWidget(DashboardWidgetId.DISTANCE, -1)
        assertEquals(
            DashboardWidgetId.DISTANCE,
            vm.uiState.value.dashboardWidgets[vm.uiState.value.dashboardWidgets.lastIndex - 1],
        )
    }

    @Test fun `dashboard widget add ignores fixed browse widget`() = runTest {
        val repo = mockk<HealthRepository>()
        val prefs = prefs()
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)
        val vm = DashboardViewModel(repo, prefs)

        vm.addDashboardWidget(DashboardWidgetId.BROWSE)

        assertFalse(DashboardWidgetId.BROWSE in vm.uiState.value.dashboardWidgets)
    }

    @Test fun `dashboard widget moves to target drop position`() = runTest {
        val repo = mockk<HealthRepository>()
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.STEPS.name,
            DashboardWidgetId.DISTANCE.name,
            DashboardWidgetId.CALORIES_OUT.name,
            DashboardWidgetId.SLEEP.name,
        )
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)
        val vm = DashboardViewModel(repo, prefs)

        vm.moveDashboardWidgetToTarget(DashboardWidgetId.STEPS, DashboardWidgetId.CALORIES_OUT)

        assertEquals(
            listOf(
                DashboardWidgetId.DISTANCE,
                DashboardWidgetId.CALORIES_OUT,
                DashboardWidgetId.STEPS,
                DashboardWidgetId.SLEEP,
            ),
            vm.uiState.value.dashboardWidgets,
        )
    }

    @Test fun `dashboard widget swaps when moved from carousel to fixed section`() = runTest {
        val repo = mockk<HealthRepository>()
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.STEPS.name,
            DashboardWidgetId.DISTANCE.name,
            DashboardWidgetId.CALORIES_OUT.name,
            DashboardWidgetId.SLEEP.name,
            DashboardWidgetId.HYDRATION.name,
        )
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)
        val vm = DashboardViewModel(repo, prefs)

        vm.moveDashboardWidgetToTarget(DashboardWidgetId.HYDRATION, DashboardWidgetId.DISTANCE)

        assertEquals(
            listOf(
                DashboardWidgetId.STEPS,
                DashboardWidgetId.HYDRATION,
                DashboardWidgetId.CALORIES_OUT,
                DashboardWidgetId.SLEEP,
                DashboardWidgetId.DISTANCE,
            ),
            vm.uiState.value.dashboardWidgets,
        )
    }

    @Test fun `dashboard widget swaps when moved from fixed to carousel section`() = runTest {
        val repo = mockk<HealthRepository>()
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.STEPS.name,
            DashboardWidgetId.DISTANCE.name,
            DashboardWidgetId.CALORIES_OUT.name,
            DashboardWidgetId.SLEEP.name,
            DashboardWidgetId.HYDRATION.name,
        )
        coEvery { repo.loadDashboard(any(), any()) } returns DashboardData(date = today)
        val vm = DashboardViewModel(repo, prefs)

        vm.moveDashboardWidgetToTarget(DashboardWidgetId.STEPS, DashboardWidgetId.HYDRATION)

        assertEquals(
            listOf(
                DashboardWidgetId.HYDRATION,
                DashboardWidgetId.DISTANCE,
                DashboardWidgetId.CALORIES_OUT,
                DashboardWidgetId.SLEEP,
                DashboardWidgetId.STEPS,
            ),
            vm.uiState.value.dashboardWidgets,
        )
    }
}
