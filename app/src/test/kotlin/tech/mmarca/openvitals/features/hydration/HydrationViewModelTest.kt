package tech.mmarca.openvitals.features.hydration

import tech.mmarca.openvitals.data.model.DailyHydration
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.HydrationRepository
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HydrationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val pastAnchor = today.minusWeeks(4)

    private fun emptyRepo() = mockk<HydrationRepository>().also { repo ->
        coEvery { repo.loadDailyHydration(any(), any()) } returns emptyList()
    }

    @Test fun `initial range is WEEK`() = runTest {
        val vm = HydrationViewModel(emptyRepo())
        assertEquals(TimeRange.WEEK, vm.uiState.value.selectedRange)
    }

    @Test fun `initial range can be restored`() = runTest {
        val vm = HydrationViewModel(emptyRepo(), initialRange = TimeRange.DAY)
        assertEquals(TimeRange.DAY, vm.uiState.value.selectedRange)
    }

    @Test fun `daily goal can be restored`() = runTest {
        val vm = HydrationViewModel(emptyRepo(), initialDailyGoalLiters = 2.5)
        assertEquals(2.5, vm.uiState.value.dailyGoalLiters, 0.01)
    }

    @Test fun `initial load clears loading and sets empty list`() = runTest {
        val vm = HydrationViewModel(emptyRepo())
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.dailyHydration.isEmpty())
    }

    @Test fun `load success populates hydration and derived totals`() = runTest {
        val hydration = listOf(
            DailyHydration(today.minusDays(1), 1.5),
            DailyHydration(today, 2.0),
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailyHydration(any(), any()) } returns hydration

        val vm = HydrationViewModel(repo)

        assertEquals(hydration, vm.uiState.value.dailyHydration)
        assertEquals(3.5, vm.uiState.value.totalLiters, 0.01)
        assertEquals(1.75, vm.uiState.value.averageLiters, 0.01)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `derived hydration statistics ignore zero intake days`() = runTest {
        val hydration = listOf(
            DailyHydration(today.minusDays(4), 0.0),
            DailyHydration(today.minusDays(3), 1.0),
            DailyHydration(today.minusDays(2), 2.0),
            DailyHydration(today.minusDays(1), 0.0),
            DailyHydration(today, 1.5),
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailyHydration(any(), any()) } returns hydration

        val vm = HydrationViewModel(repo)

        assertEquals(3, vm.uiState.value.trackedDays)
        assertEquals(1.5, vm.uiState.value.averageLiters, 0.01)
        assertEquals(2.0, vm.uiState.value.bestDayLiters, 0.01)
        assertEquals(1, vm.uiState.value.currentTrackedStreakDays)
    }

    @Test fun `current tracked streak counts consecutive intake days from period end`() = runTest {
        val hydration = listOf(
            DailyHydration(today.minusDays(3), 1.0),
            DailyHydration(today.minusDays(2), 0.0),
            DailyHydration(today.minusDays(1), 2.0),
            DailyHydration(today, 1.5),
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailyHydration(any(), any()) } returns hydration

        val vm = HydrationViewModel(repo)

        assertEquals(2, vm.uiState.value.currentTrackedStreakDays)
    }

    @Test fun `goal statistics use the configured daily goal`() = runTest {
        val hydration = listOf(
            DailyHydration(today.minusDays(3), 2.0),
            DailyHydration(today.minusDays(2), 2.5),
            DailyHydration(today.minusDays(1), 1.0),
            DailyHydration(today, 2.0),
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailyHydration(any(), any()) } returns hydration

        val vm = HydrationViewModel(repo, initialDailyGoalLiters = 2.0)

        assertEquals(3, vm.uiState.value.goalMetDays)
        assertEquals(75, vm.uiState.value.goalSuccessRatePercent)
        assertEquals(1, vm.uiState.value.currentGoalStreakDays)
        assertEquals(2, vm.uiState.value.longestGoalStreakDays)
    }

    @Test fun `updating daily goal saves and recalculates goal statistics`() = runTest {
        val hydration = listOf(
            DailyHydration(today.minusDays(1), 2.0),
            DailyHydration(today, 2.5),
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailyHydration(any(), any()) } returns hydration
        var savedGoal: Double? = null
        val vm = HydrationViewModel(
            repository = repo,
            initialDailyGoalLiters = 2.0,
            onDailyGoalChanged = { goal -> savedGoal = goal },
        )

        vm.increaseDailyGoal()

        assertEquals(2.25, savedGoal ?: 0.0, 0.01)
        assertEquals(2.25, vm.uiState.value.dailyGoalLiters, 0.01)
        assertEquals(1, vm.uiState.value.goalMetDays)
    }

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<HydrationRepository>()
        coEvery { repo.loadDailyHydration(any(), any()) } throws RuntimeException("timeout")

        val vm = HydrationViewModel(repo)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("timeout", vm.uiState.value.error)
    }

    @Test fun `selectRange updates selectedRange and reloads`() = runTest {
        val repo = emptyRepo()
        val vm = HydrationViewModel(repo)

        vm.selectRange(TimeRange.MONTH)

        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
        coVerify(atLeast = 2) { repo.loadDailyHydration(any(), any()) }
    }

    @Test fun `selectRange saves selected range`() = runTest {
        var savedRange: TimeRange? = null
        val vm = HydrationViewModel(
            repository = emptyRepo(),
            onRangeSelected = { range -> savedRange = range },
        )

        vm.selectRange(TimeRange.MONTH)

        assertEquals(TimeRange.MONTH, savedRange)
    }

    @Test fun `previousPeriod WEEK moves back one week`() = runTest {
        val vm = HydrationViewModel(emptyRepo())
        val before = vm.uiState.value.selectedDate

        vm.previousPeriod()

        assertEquals(before.minusWeeks(1), vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod DAY is blocked when selectedDate is today`() = runTest {
        val repo = emptyRepo()
        val vm = HydrationViewModel(repo)
        vm.selectRange(TimeRange.DAY)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before, vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod WEEK advances from a past week`() = runTest {
        val vm = HydrationViewModel(emptyRepo())
        vm.selectDate(pastAnchor)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before.plusWeeks(1), vm.uiState.value.selectedDate)
    }

    @Test fun `selectDate clamps future date to today`() = runTest {
        val vm = HydrationViewModel(emptyRepo())

        vm.selectDate(today.plusDays(10))

        assertEquals(today, vm.uiState.value.selectedDate)
    }
}
