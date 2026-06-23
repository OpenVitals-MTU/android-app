package tech.mmarca.openvitals.features.hydration

import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.model.HydrationReminderConfig
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.HydrationPeriodData
import tech.mmarca.openvitals.data.repository.HydrationRepository
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
class HydrationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val pastAnchor = today.minusWeeks(4)

    private fun emptyRepo() = mockk<HydrationRepository>().also { repo ->
        coEvery { repo.loadDailyHydration(any(), any()) } returns emptyList()
        coEvery { repo.loadHydrationEntries(any(), any()) } returns emptyList()
        coEvery { repo.deleteHydrationEntry(any()) } returns Unit
        coEvery { repo.loadHydrationPeriod(any()) } coAnswers {
            val query = firstArg<PeriodLoadQuery>()
            val windows = query.windows
            HydrationPeriodData(
                dailyHydration = repo.loadDailyHydration(windows.current.start, windows.current.end),
                previousDailyHydration = repo.loadDailyHydration(windows.previous.start, windows.previous.end),
                baselineDailyHydration = repo.loadDailyHydration(windows.baseline.start, windows.baseline.end),
                hydrationEntries = repo.loadHydrationEntries(windows.current.start, windows.current.end),
            )
        }
        coEvery { repo.loadHydrationPeriod(any(), any()) } coAnswers {
            val query = firstArg<PeriodLoadQuery>()
            val windows = query.windows
            HydrationPeriodData(
                dailyHydration = repo.loadDailyHydration(windows.current.start, windows.current.end),
                previousDailyHydration = repo.loadDailyHydration(windows.previous.start, windows.previous.end),
                baselineDailyHydration = repo.loadDailyHydration(windows.baseline.start, windows.baseline.end),
                hydrationEntries = repo.loadHydrationEntries(windows.current.start, windows.current.end),
            )
        }
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

    @Test fun `reminder config can be restored`() = runTest {
        val config = HydrationReminderConfig(
            enabled = true,
            intervalMinutes = 90,
            activeStartTime = LocalTime.of(8, 0),
            activeEndTime = LocalTime.of(22, 0),
        )

        val vm = HydrationViewModel(emptyRepo(), initialReminderConfig = config)

        assertEquals(config, vm.uiState.value.reminderConfig)
    }

    @Test fun `initial load clears loading and sets empty list`() = runTest {
        val vm = HydrationViewModel(emptyRepo())
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.dailyHydration.isEmpty())
        assertTrue(state.hydrationEntries.isEmpty())
    }

    @Test fun `initial load does not inspect write permissions`() = runTest {
        val repo = emptyRepo()

        HydrationViewModel(repo)

        coVerify(exactly = 0) { repo.hasHydrationWritePermission() }
        coVerify(exactly = 0) { repo.writeHydrationEntry(any()) }
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

    @Test fun `load success populates hydration entries`() = runTest {
        val entries = listOf(
            HydrationEntry(
                startTime = java.time.Instant.ofEpochSecond(1_000),
                endTime = java.time.Instant.ofEpochSecond(1_300),
                liters = 0.5,
                source = "test",
            )
        )
        val repo = emptyRepo()
        coEvery { repo.loadHydrationEntries(any(), any()) } returns entries

        val vm = HydrationViewModel(repo)

        assertEquals(entries, vm.uiState.value.hydrationEntries)
    }

    @Test fun `deleteHydrationEntry removes entry and reloads period data`() = runTest {
        val start = today.atStartOfDay(ZoneId.systemDefault()).plusHours(8).toInstant()
        val entry = HydrationEntry(
            startTime = start,
            endTime = start.plusSeconds(1),
            liters = 0.5,
            source = "tech.mmarca.openvitals.debug",
            id = "hydration-id",
            isOpenVitalsEntry = true,
        )
        var hydration = listOf(DailyHydration(today, 0.5))
        var entries = listOf(entry)
        val repo = emptyRepo()
        coEvery { repo.loadDailyHydration(any(), any()) } answers { hydration }
        coEvery { repo.loadHydrationEntries(any(), any()) } answers { entries }
        coEvery { repo.deleteHydrationEntry("hydration-id") } coAnswers {
            hydration = listOf(DailyHydration(today, 0.0))
            entries = emptyList()
        }
        val vm = HydrationViewModel(repo, initialRange = TimeRange.DAY)

        vm.deleteHydrationEntry("hydration-id")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.hydrationEntries.isEmpty())
        assertEquals(0.0, vm.uiState.value.totalLiters, 0.01)
        coVerify { repo.deleteHydrationEntry("hydration-id") }
        coVerify { repo.loadHydrationPeriod(any(), RefreshMode.FORCE) }
    }

    @Test fun `deleteHydrationEntry ignores entries not created by OpenVitals`() = runTest {
        val start = today.atStartOfDay(ZoneId.systemDefault()).plusHours(8).toInstant()
        val entry = HydrationEntry(
            startTime = start,
            endTime = start.plusSeconds(1),
            liters = 0.5,
            source = "com.example.hydrotracker",
            id = "external-hydration-id",
            isOpenVitalsEntry = false,
        )
        val entries = listOf(entry)
        val repo = emptyRepo()
        coEvery { repo.loadDailyHydration(any(), any()) } returns listOf(DailyHydration(today, 0.5))
        coEvery { repo.loadHydrationEntries(any(), any()) } returns entries
        val vm = HydrationViewModel(repo, initialRange = TimeRange.DAY)

        vm.deleteHydrationEntry("external-hydration-id")
        advanceUntilIdle()

        assertEquals(entries, vm.uiState.value.hydrationEntries)
        assertEquals(0.5, vm.uiState.value.totalLiters, 0.01)
        coVerify(exactly = 0) { repo.deleteHydrationEntry("external-hydration-id") }
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

    @Test fun `updating reminder config saves normalized config`() = runTest {
        var savedConfig: HydrationReminderConfig? = null
        val vm = HydrationViewModel(
            repository = emptyRepo(),
            initialReminderConfig = HydrationReminderConfig(intervalMinutes = 120),
            onReminderConfigChanged = { config -> savedConfig = config },
        )

        vm.setHydrationRemindersEnabled(true)
        vm.increaseHydrationReminderInterval()
        vm.setHydrationReminderActiveStartTime(LocalTime.of(6, 30, 12))

        assertEquals(true, savedConfig?.enabled)
        assertEquals(150, vm.uiState.value.reminderConfig.intervalMinutes)
        assertEquals(LocalTime.of(6, 30), vm.uiState.value.reminderConfig.activeStartTime)
    }

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<HydrationRepository>()
        coEvery { repo.loadHydrationPeriod(any()) } throws RuntimeException("timeout")

        val vm = HydrationViewModel(repo)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("timeout", vm.uiState.value.error)
    }

    @Test fun `selectRange updates selectedRange and reloads`() = runTest {
        val repo = emptyRepo()
        val vm = HydrationViewModel(repo)

        vm.selectRange(TimeRange.MONTH)

        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
        coVerify(atLeast = 2) { repo.loadHydrationPeriod(any()) }
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
