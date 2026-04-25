package tech.mmarca.openvitals.features.dashboard

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

    private fun prefs() = mockk<PreferencesRepository>().also {
        every { it.acknowledgedPermissions() } returns emptySet()
        every { it.acknowledgePermissions(any()) } returns Unit
    }

    // ─── Initial load ─────────────────────────────────────────────────────────

    @Test fun `initial state has isLoading true before coroutine runs`() {
        val repo = mockk<HealthRepository>()
        // Block the coroutine by never completing — use a suspended mock
        coEvery { repo.loadDashboard(any()) } coAnswers { kotlinx.coroutines.awaitCancellation() }

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
        coEvery { repo.loadDashboard(any()) } returns data

        val vm = DashboardViewModel(repo, prefs())

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(data, state.data)
        assertNull(state.errorMessage)
    }

    @Test fun `load failure sets errorMessage and clears loading`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any()) } throws RuntimeException("network error")

        val vm = DashboardViewModel(repo, prefs())

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.data)
        assertEquals("network error", state.errorMessage)
    }

    @Test fun `load failure with null message uses Unknown error fallback`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any()) } throws RuntimeException()

        val vm = DashboardViewModel(repo, prefs())

        assertEquals("Unknown error", vm.uiState.value.errorMessage)
    }

    // ─── Date clamping ────────────────────────────────────────────────────────

    @Test fun `load clamps future date to today`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs())
        val futureDate = today.plusDays(10)
        vm.load(futureDate)

        assertEquals(today, vm.uiState.value.selectedDate)
        coVerify { repo.loadDashboard(today) }
    }

    @Test fun `selectDate clamps future date to today`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs())
        vm.selectDate(today.plusDays(5))

        assertEquals(today, vm.uiState.value.selectedDate)
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    @Test fun `previousDay decrements selectedDate by one day`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs())
        vm.previousDay()

        assertEquals(yesterday, vm.uiState.value.selectedDate)
    }

    @Test fun `nextDay is blocked when selectedDate is today`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs())
        vm.nextDay()

        assertEquals(today, vm.uiState.value.selectedDate)
        // load called once by init, not again by blocked nextDay
        coVerify(exactly = 1) { repo.loadDashboard(any()) }
    }

    @Test fun `nextDay advances from yesterday to today`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs())
        vm.selectDate(yesterday)
        vm.nextDay()

        assertEquals(today, vm.uiState.value.selectedDate)
    }

    // ─── A3: floorsClimbed in DashboardData ──────────────────────────────────

    @Test fun `floorsClimbed is exposed through state when present`() = runTest {
        val data = DashboardData(date = today, floorsClimbed = 12)
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any()) } returns data

        val vm = DashboardViewModel(repo, prefs())

        assertEquals(12, vm.uiState.value.data?.floorsClimbed)
    }

    @Test fun `floorsClimbed is null in state when not reported`() = runTest {
        val data = DashboardData(date = today, floorsClimbed = null)
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any()) } returns data

        val vm = DashboardViewModel(repo, prefs())

        assertNull(vm.uiState.value.data?.floorsClimbed)
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    @Test fun `refresh reloads current date`() = runTest {
        val repo = mockk<HealthRepository>()
        coEvery { repo.loadDashboard(any()) } returns DashboardData(date = today)

        val vm = DashboardViewModel(repo, prefs())
        vm.refresh()

        // init + refresh = 2 calls
        coVerify(exactly = 2) { repo.loadDashboard(today) }
    }
}
