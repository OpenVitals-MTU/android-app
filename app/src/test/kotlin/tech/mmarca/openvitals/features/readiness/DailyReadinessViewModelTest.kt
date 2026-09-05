package tech.mmarca.openvitals.features.readiness

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineResult
import tech.mmarca.openvitals.data.repository.dashboard.DashboardDataLoader
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.domain.usecase.LoadDashboardDayUseCase
import tech.mmarca.openvitals.util.MainDispatcherRule

/** The detail screen shares [DailyReadinessViewModel]: load publication, navigation, refresh, staleness guard, failure mapping. */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyReadinessViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()

    private fun prefs(): PreferencesRepository = mockk<PreferencesRepository>().also {
        every { it.sleepWindow } returns SleepWindow.Default
        every { it.activityWeekMode } returns ActivityWeekMode.MONDAY_TO_SUNDAY
        every { it.dailyGoalFor(any()) } answers { firstArg<MetricDailyGoalKey>().defaultValue }
        every { it.hydrationDailyGoalLiters } returns 2.0
        every { it.bodyEnergyCalibration() } returns BodyEnergyCalibration.Automatic
    }

    private fun bodyEnergyRepo(): BodyEnergyRepository = mockk<BodyEnergyRepository>().also {
        coEvery { it.loadTimeline(any()) } coAnswers { BodyEnergyTimelineResult(firstArg(), emptyList()) }
    }

    /** Records the days it is asked for; any day given a gate blocks until the test releases it. */
    private class RecordingLoader {
        val queries = mutableListOf<DashboardQuery>()
        val gates = mutableMapOf<LocalDate, CompletableDeferred<Unit>>()

        fun gate(date: LocalDate): CompletableDeferred<Unit> =
            gates.getOrPut(date) { CompletableDeferred() }

        fun mock(): DashboardDataLoader = mockk<DashboardDataLoader>().also { loader ->
            coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
                val query = firstArg<DashboardQuery>()
                queries += query
                gates[query.date]?.await()
                DashboardData(
                    date = query.date,
                    avgHeartRateBpm = 72,
                    restingHeartRateBpm = 55,
                    restingHeartRateBaselineBpm = 54,
                )
            }
        }
    }

    private fun viewModel(loader: DashboardDataLoader): DailyReadinessViewModel =
        DailyReadinessViewModel(
            loadDashboardDayUseCase = LoadDashboardDayUseCase(loader),
            prefs = prefs(),
            bodyEnergyRepository = bodyEnergyRepo(),
        )

    @Test
    fun `load publishes the insight for the loaded day`() = runTest {
        // The detail spec is built inside the composable, so the insight publication is the assertable part.
        val loader = RecordingLoader()
        val vm = viewModel(loader.mock())
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.data)
        assertNotNull(state.insight)
        assertEquals(today, state.selectedDate)
        assertTrue((state.insight?.trainingReadinessScore ?: -1) >= 0)
    }

    @Test
    fun `a future day is clamped to today`() = runTest {
        val loader = RecordingLoader()
        val vm = viewModel(loader.mock())
        advanceUntilIdle()

        vm.load(today.plusDays(5))
        advanceUntilIdle()

        assertEquals(today, vm.uiState.value.selectedDate)
        // The repository was never asked for a day that does not exist yet.
        assertTrue(loader.queries.all { !it.date.isAfter(today) })
    }

    @Test
    fun `day navigation loads the day it moves to, and stops at today`() = runTest {
        val loader = RecordingLoader()
        val vm = viewModel(loader.mock())
        advanceUntilIdle()

        vm.previousDay()
        advanceUntilIdle()
        assertEquals(today.minusDays(1), vm.uiState.value.selectedDate)
        assertEquals(today.minusDays(1), loader.queries.last().date)

        vm.nextDay()
        advanceUntilIdle()
        assertEquals(today, vm.uiState.value.selectedDate)

        // Tomorrow is not a day: no load, no move.
        val loads = loader.queries.size
        vm.nextDay()
        advanceUntilIdle()
        assertEquals(loads, loader.queries.size)
        assertEquals(today, vm.uiState.value.selectedDate)
    }

    @Test
    fun `refresh reloads the selected day, forcing it`() = runTest {
        val loader = RecordingLoader()
        val vm = viewModel(loader.mock())
        advanceUntilIdle()
        vm.load(today.minusDays(2))
        advanceUntilIdle()

        vm.refresh()
        advanceUntilIdle()

        assertEquals(today.minusDays(2), loader.queries.last().date)
        assertEquals(RefreshMode.FORCE, loader.queries.last().refreshMode)
    }

    @Test
    fun `a stale day cannot overwrite the day that overtook it`() = runTest {
        val loader = RecordingLoader()
        val stale = today.minusDays(3)
        val gate = loader.gate(stale)
        val vm = viewModel(loader.mock())
        advanceUntilIdle()

        vm.load(stale)
        vm.load(today)
        advanceUntilIdle()
        assertEquals(today, vm.uiState.value.selectedDate)

        // The overtaken load answers late, and is dropped.
        gate.complete(Unit)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(today, state.selectedDate)
        assertFalse(state.isLoading)
        assertEquals(today, state.data?.date)
        assertNotNull(state.insight)
    }

    @Test
    fun `a failed load becomes a ScreenError, not an exception`() = runTest {
        val loader = mockk<DashboardDataLoader>()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } throws RuntimeException("boom")

        val vm = viewModel(loader)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ScreenError.Message("boom"), state.error)
        assertFalse(state.isLoading)
        assertNull(state.insight)
    }
}
