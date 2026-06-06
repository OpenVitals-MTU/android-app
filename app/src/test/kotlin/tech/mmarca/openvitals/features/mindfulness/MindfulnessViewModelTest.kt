package tech.mmarca.openvitals.features.mindfulness

import tech.mmarca.openvitals.data.model.MindfulnessSession
import tech.mmarca.openvitals.data.model.MindfulnessReminderConfig
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.MindfulnessPeriodData
import tech.mmarca.openvitals.data.repository.MindfulnessRepository
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MindfulnessViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val pastAnchor = today.minusWeeks(4)

    private fun emptyRepo() = mockk<MindfulnessRepository>().also { repo ->
        coEvery { repo.loadMindfulnessSessions(any(), any()) } returns emptyList()
        coEvery { repo.deleteMindfulnessSessionEntry(any()) } returns Unit
        coEvery { repo.loadMindfulnessPeriod(any()) } coAnswers {
            val query = firstArg<PeriodLoadQuery>()
            val windows = query.windows
            MindfulnessPeriodData(
                sessions = repo.loadMindfulnessSessions(windows.current.start, windows.current.end),
                previousSessions = repo.loadMindfulnessSessions(windows.previous.start, windows.previous.end),
                baselineSessions = repo.loadMindfulnessSessions(windows.baseline.start, windows.baseline.end),
            )
        }
    }

    @Test fun `initial range is WEEK`() = runTest {
        val vm = MindfulnessViewModel(emptyRepo())
        assertEquals(TimeRange.WEEK, vm.uiState.value.selectedRange)
    }

    @Test fun `initial reminder config uses default disabled daily reminder`() = runTest {
        val vm = MindfulnessViewModel(emptyRepo())

        assertFalse(vm.uiState.value.reminderConfig.enabled)
        assertEquals(LocalTime.of(18, 0), vm.uiState.value.reminderConfig.reminderTime)
    }

    @Test fun `mindfulness reminder config updates and persists`() = runTest {
        val changes = mutableListOf<MindfulnessReminderConfig>()
        val vm = MindfulnessViewModel(
            repository = emptyRepo(),
            initialReminderConfig = MindfulnessReminderConfig(reminderTime = LocalTime.of(17, 0)),
            onReminderConfigChanged = changes::add,
        )

        vm.setMindfulnessRemindersEnabled(true)
        vm.setMindfulnessReminderTime(LocalTime.of(18, 30))

        assertEquals(
            MindfulnessReminderConfig(enabled = true, reminderTime = LocalTime.of(18, 30)),
            vm.uiState.value.reminderConfig,
        )
        assertEquals(
            listOf(
                MindfulnessReminderConfig(enabled = true, reminderTime = LocalTime.of(17, 0)),
                MindfulnessReminderConfig(enabled = true, reminderTime = LocalTime.of(18, 30)),
            ),
            changes,
        )
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

    @Test fun `deleteMindfulnessSessionEntry removes OpenVitals session and reloads`() = runTest {
        val now = Instant.now()
        val entry = MindfulnessSession(
            id = "session-id",
            title = "Breathing",
            startTime = now.minusSeconds(1_800),
            endTime = now,
            durationMs = 1_800_000,
            source = "tech.mmarca.openvitals.debug",
            isOpenVitalsEntry = true,
        )
        var sessions = listOf(entry)
        val repo = emptyRepo()
        coEvery { repo.loadMindfulnessSessions(any(), any()) } answers { sessions }
        coEvery { repo.deleteMindfulnessSessionEntry("session-id") } coAnswers {
            sessions = emptyList()
        }
        val vm = MindfulnessViewModel(repo)

        vm.deleteMindfulnessSessionEntry("session-id")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.sessions.isEmpty())
        assertEquals(0L, vm.uiState.value.totalMinutes)
        coVerify { repo.deleteMindfulnessSessionEntry("session-id") }
        coVerify(atLeast = 2) { repo.loadMindfulnessPeriod(any()) }
    }

    @Test fun `deleteMindfulnessSessionEntry ignores sessions not created by OpenVitals`() = runTest {
        val now = Instant.now()
        val sessions = listOf(
            MindfulnessSession(
                id = "external-session-id",
                title = "Breathing",
                startTime = now.minusSeconds(1_800),
                endTime = now,
                durationMs = 1_800_000,
                source = "com.example",
                isOpenVitalsEntry = false,
            )
        )
        val repo = emptyRepo()
        coEvery { repo.loadMindfulnessSessions(any(), any()) } returns sessions
        val vm = MindfulnessViewModel(repo)

        vm.deleteMindfulnessSessionEntry("external-session-id")
        advanceUntilIdle()

        assertEquals(sessions, vm.uiState.value.sessions)
        coVerify(exactly = 0) { repo.deleteMindfulnessSessionEntry("external-session-id") }
    }

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<MindfulnessRepository>()
        coEvery { repo.loadMindfulnessPeriod(any()) } throws RuntimeException("timeout")

        val vm = MindfulnessViewModel(repo)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("timeout", vm.uiState.value.error)
    }

    @Test fun `selectRange updates selectedRange and reloads`() = runTest {
        val repo = emptyRepo()
        val vm = MindfulnessViewModel(repo)

        vm.selectRange(TimeRange.MONTH)

        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
        coVerify(atLeast = 2) { repo.loadMindfulnessPeriod(any()) }
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
