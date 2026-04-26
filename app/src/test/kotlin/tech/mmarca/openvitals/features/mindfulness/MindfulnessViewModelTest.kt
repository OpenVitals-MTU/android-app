package tech.mmarca.openvitals.features.mindfulness

import tech.mmarca.openvitals.data.model.MindfulnessSession
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.MindfulnessRepository
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MindfulnessViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val pastAnchor = today.minusWeeks(4)

    private fun emptyRepo() = mockk<MindfulnessRepository>().also { repo ->
        coEvery { repo.loadMindfulnessSessions(any(), any()) } returns emptyList()
    }

    @Test fun `initial range is WEEK`() = runTest {
        val vm = MindfulnessViewModel(emptyRepo())
        assertEquals(TimeRange.WEEK, vm.uiState.value.selectedRange)
    }

    @Test fun `initial load clears loading and sets empty list`() = runTest {
        val vm = MindfulnessViewModel(emptyRepo())
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.sessions.isEmpty())
    }

    @Test fun `load success populates sessions and derived total minutes`() = runTest {
        val now = Instant.now()
        val sessions = listOf(
            MindfulnessSession("1", "Breathing", now.minusSeconds(1_800), now, 1_800_000, "test"),
            MindfulnessSession("2", "Meditation", now.minusSeconds(3_600), now, 3_600_000, "test"),
        )
        val repo = emptyRepo()
        coEvery { repo.loadMindfulnessSessions(any(), any()) } returns sessions

        val vm = MindfulnessViewModel(repo)

        assertEquals(sessions, vm.uiState.value.sessions)
        assertEquals(90L, vm.uiState.value.totalMinutes)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<MindfulnessRepository>()
        coEvery { repo.loadMindfulnessSessions(any(), any()) } throws RuntimeException("timeout")

        val vm = MindfulnessViewModel(repo)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("timeout", vm.uiState.value.error)
    }

    @Test fun `selectRange updates selectedRange and reloads`() = runTest {
        val repo = emptyRepo()
        val vm = MindfulnessViewModel(repo)

        vm.selectRange(TimeRange.MONTH)

        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
        coVerify(atLeast = 2) { repo.loadMindfulnessSessions(any(), any()) }
    }

    @Test fun `previousPeriod WEEK moves back one week`() = runTest {
        val vm = MindfulnessViewModel(emptyRepo())
        val before = vm.uiState.value.selectedDate

        vm.previousPeriod()

        assertEquals(before.minusWeeks(1), vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod DAY is blocked when selectedDate is today`() = runTest {
        val vm = MindfulnessViewModel(emptyRepo())
        vm.selectRange(TimeRange.DAY)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before, vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod WEEK advances from a past week`() = runTest {
        val vm = MindfulnessViewModel(emptyRepo())
        vm.selectDate(pastAnchor)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before.plusWeeks(1), vm.uiState.value.selectedDate)
    }

    @Test fun `selectDate clamps future date to today`() = runTest {
        val vm = MindfulnessViewModel(emptyRepo())

        vm.selectDate(today.plusDays(10))

        assertEquals(today, vm.uiState.value.selectedDate)
    }
}
