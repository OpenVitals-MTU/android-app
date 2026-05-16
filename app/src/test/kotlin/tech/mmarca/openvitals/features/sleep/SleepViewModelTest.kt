package tech.mmarca.openvitals.features.sleep

import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.model.SleepStage
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.data.repository.SleepRepository
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

class SleepViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val pastAnchor = today.minusWeeks(4)

    private fun emptyRepo() = mockk<SleepRepository>().also { repo ->
        coEvery { repo.loadSleepSessions(any(), any()) } returns emptyList()
    }

    private fun sleepSession(offsetDays: Long = 0) = SleepData(
        id = "s$offsetDays",
        startTime = Instant.EPOCH,
        endTime = Instant.ofEpochMilli(28_800_000L),
        durationMs = 28_800_000L,
        source = "test",
        stages = listOf(
            SleepStage(Instant.EPOCH, Instant.ofEpochMilli(3_600_000L), SleepStage.STAGE_DEEP),
        ),
    )

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test fun `initial range is WEEK`() = runTest {
        val vm = SleepViewModel(emptyRepo())
        assertEquals(TimeRange.WEEK, vm.uiState.value.selectedRange)
    }

    @Test fun `initial load clears loading`() = runTest {
        val vm = SleepViewModel(emptyRepo())
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun `initial sessions list is empty when repo returns nothing`() = runTest {
        val vm = SleepViewModel(emptyRepo())
        assertTrue(vm.uiState.value.sessions.isEmpty())
    }

    // ─── Load success / failure ───────────────────────────────────────────────

    @Test fun `load success populates sessions`() = runTest {
        val sessions = listOf(sleepSession())
        val repo = emptyRepo()
        coEvery { repo.loadSleepSessions(any(), any()) } returns sessions

        val vm = SleepViewModel(repo)

        assertEquals(sessions, vm.uiState.value.sessions)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `load failure sets error message`() = runTest {
        val repo = mockk<SleepRepository>()
        coEvery { repo.loadSleepSessions(any(), any()) } throws RuntimeException("offline")

        val vm = SleepViewModel(repo)

        assertEquals("offline", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    // ─── selectRange ──────────────────────────────────────────────────────────

    @Test fun `selectRange updates range and triggers load`() = runTest {
        val repo = emptyRepo()
        val vm = SleepViewModel(repo)
        vm.selectRange(TimeRange.MONTH)

        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
        coVerify(atLeast = 2) { repo.loadSleepSessions(any(), any()) }
    }

    @Test fun `initial non-midnight sleep range loads the previous day too`() = runTest {
        val repo = emptyRepo()

        SleepViewModel(
            repository = repo,
            initialRange = TimeRange.DAY,
            initialSleepRangeMode = SleepRangeMode.EVENING_18H,
        )

        coVerify { repo.loadSleepSessions(today.minusDays(1), today) }
    }

    // ─── previousPeriod ───────────────────────────────────────────────────────

    @Test fun `previousPeriod DAY moves back one day`() = runTest {
        val vm = SleepViewModel(emptyRepo())
        vm.selectRange(TimeRange.DAY)
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusDays(1), vm.uiState.value.selectedDate)
    }

    @Test fun `previousPeriod WEEK moves back one week`() = runTest {
        val vm = SleepViewModel(emptyRepo())
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusWeeks(1), vm.uiState.value.selectedDate)
    }

    @Test fun `previousPeriod MONTH moves back one month`() = runTest {
        val vm = SleepViewModel(emptyRepo())
        vm.selectRange(TimeRange.MONTH)
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusMonths(1), vm.uiState.value.selectedDate)
    }

    @Test fun `previousPeriod YEAR moves back one year`() = runTest {
        val vm = SleepViewModel(emptyRepo())
        vm.selectRange(TimeRange.YEAR)
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusYears(1), vm.uiState.value.selectedDate)
    }

    // ─── nextPeriod ───────────────────────────────────────────────────────────

    @Test fun `nextPeriod DAY is blocked when selectedDate is today`() = runTest {
        val vm = SleepViewModel(emptyRepo())
        vm.selectRange(TimeRange.DAY)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before, vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod DAY advances from a past day`() = runTest {
        val vm = SleepViewModel(emptyRepo())
        vm.selectRange(TimeRange.DAY)
        vm.selectDate(today.minusDays(2))
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before.plusDays(1), vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod WEEK advances from a past anchor`() = runTest {
        val vm = SleepViewModel(emptyRepo())
        vm.selectDate(pastAnchor)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before.plusWeeks(1), vm.uiState.value.selectedDate)
    }

    // ─── selectDate ───────────────────────────────────────────────────────────

    @Test fun `selectDate clamps future date to today`() = runTest {
        val vm = SleepViewModel(emptyRepo())
        vm.selectDate(today.plusDays(5))
        assertEquals(today, vm.uiState.value.selectedDate)
    }

    @Test fun `selectDate accepts past date unchanged`() = runTest {
        val vm = SleepViewModel(emptyRepo())
        vm.selectDate(pastAnchor)
        assertEquals(pastAnchor, vm.uiState.value.selectedDate)
    }
}
