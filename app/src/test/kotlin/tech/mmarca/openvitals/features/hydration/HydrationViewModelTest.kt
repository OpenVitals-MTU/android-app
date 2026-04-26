package tech.mmarca.openvitals.features.hydration

import tech.mmarca.openvitals.data.model.DailyHydration
import tech.mmarca.openvitals.data.model.TimeRange
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
