package tech.mmarca.openvitals.features.hydration

import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.model.HydrationEntryRecordType
import tech.mmarca.openvitals.domain.model.HydrationReminderConfig
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.query.HydrationPeriodData
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
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

    private fun emptyNutritionRepo(
        entries: List<NutritionEntry> = emptyList(),
    ) = mockk<NutritionRepository>().also { repo ->
        coEvery { repo.loadNutritionEntries(any(), any()) } returns entries
        coEvery { repo.deleteNutritionEntry(any()) } returns Unit
    }

    private fun hydrationViewModel(
        repository: HydrationRepository,
        nutritionRepository: NutritionRepository? = null,
        initialRange: TimeRange = TimeRange.WEEK,
        initialDailyGoalLiters: Double = 2.0,
        initialReminderConfig: HydrationReminderConfig = HydrationReminderConfig(),
        onRangeSelected: (TimeRange) -> Unit = {},
        onDailyGoalChanged: (Double) -> Unit = {},
        onReminderConfigChanged: (HydrationReminderConfig) -> Unit = {},
    ) = HydrationViewModel(
        repository = repository,
        dispatchers = mainDispatcherRule.dispatcherProvider,
        nutritionRepository = nutritionRepository,
        initialRange = initialRange,
        initialDailyGoalLiters = initialDailyGoalLiters,
        initialReminderConfig = initialReminderConfig,
        onRangeSelected = onRangeSelected,
        onDailyGoalChanged = onDailyGoalChanged,
        onReminderConfigChanged = onReminderConfigChanged,
    )

    @Test fun `initial range is WEEK`() = runTest {
        val vm = hydrationViewModel(emptyRepo())
        assertEquals(TimeRange.WEEK, vm.uiState.value.selectedRange)
    }

    @Test fun `initial range can be restored`() = runTest {
        val vm = hydrationViewModel(emptyRepo(), initialRange = TimeRange.DAY)
        assertEquals(TimeRange.DAY, vm.uiState.value.selectedRange)
    }

    @Test fun `daily goal can be restored`() = runTest {
        val vm = hydrationViewModel(emptyRepo(), initialDailyGoalLiters = 2.5)
        assertEquals(2.5, vm.uiState.value.dailyGoalLiters, 0.01)
    }

    @Test fun `reminder config can be restored`() = runTest {
        val config = HydrationReminderConfig(
            enabled = true,
            intervalMinutes = 90,
            activeStartTime = LocalTime.of(8, 0),
            activeEndTime = LocalTime.of(22, 0),
        )

        val vm = hydrationViewModel(emptyRepo(), initialReminderConfig = config)

        assertEquals(config, vm.uiState.value.reminderConfig)
    }

    @Test fun `initial load clears loading and sets empty list`() = runTest {
        val vm = hydrationViewModel(emptyRepo())
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.dailyHydration.isEmpty())
        assertTrue(state.hydrationEntries.isEmpty())
    }

    @Test fun `initial load does not inspect write permissions`() = runTest {
        val repo = emptyRepo()

        hydrationViewModel(repo)

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

        val vm = hydrationViewModel(repo)

        assertEquals(hydration, vm.uiState.value.dailyHydration)
        assertEquals(3.5, vm.uiState.value.display.summary.totalLiters, 0.01)
        assertEquals(1.75, vm.uiState.value.display.summary.averageLiters, 0.01)
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

        val vm = hydrationViewModel(repo)

        assertEquals(entries, vm.uiState.value.hydrationEntries)
    }

    @Test fun `load adds OpenVitals standalone nutrition drinks to hydration entries`() = runTest {
        val start = today.atStartOfDay(ZoneId.systemDefault()).plusHours(8).toInstant()
        val hydrationEntry = HydrationEntry(
            startTime = start,
            endTime = start.plusSeconds(1),
            liters = 0.5,
            source = "tech.mmarca.openvitals.debug",
            id = "hydration-id",
            isOpenVitalsEntry = true,
        )
        val caffeineEntry = NutritionEntry(
            time = start.plusSeconds(60),
            mealType = 0,
            name = "Coffee",
            energyKcal = null,
            proteinGrams = null,
            carbsGrams = null,
            fatGrams = null,
            fiberGrams = null,
            sugarGrams = null,
            source = "tech.mmarca.openvitals.debug",
            nutrientValues = mapOf(NutritionNutrient.CAFFEINE to 0.095),
            id = "nutrition-id",
            clientRecordId = "openvitals_nutrition_1000_uuid",
            isOpenVitalsEntry = true,
        )
        val carbsEntry = caffeineEntry.copy(
            name = "OpenVitals carbs",
            id = "carbs-id",
            clientRecordId = "openvitals_nutrition_1100_uuid",
        )
        val pairedEntry = caffeineEntry.copy(
            id = "paired-id",
            clientRecordId = "openvitals_hydration_nutrition_hydration-client-id",
        )
        val noClientIdStandaloneEntry = caffeineEntry.copy(
            time = start.plusSeconds(120),
            name = "Espresso",
            id = "no-client-id-standalone",
            clientRecordId = null,
        )
        val noClientIdPairedEntry = caffeineEntry.copy(
            time = start,
            id = "no-client-id-paired",
            clientRecordId = null,
        )
        val repo = emptyRepo()
        coEvery { repo.loadHydrationEntries(any(), any()) } returns listOf(hydrationEntry)
        val nutritionRepo = emptyNutritionRepo(
            listOf(caffeineEntry, carbsEntry, pairedEntry, noClientIdStandaloneEntry, noClientIdPairedEntry)
        )

        val vm = hydrationViewModel(repo, nutritionRepository = nutritionRepo, initialRange = TimeRange.DAY)

        val entries = vm.uiState.value.hydrationEntries
        assertEquals(listOf("hydration-id", "nutrition-id", "no-client-id-standalone"), entries.map { it.id })
        assertEquals(HydrationEntryRecordType.NUTRITION_ONLY, entries[1].recordType)
        assertEquals("Coffee", entries[1].displayName)
        assertEquals("Espresso", entries[2].displayName)
        assertEquals(0.0, entries[1].liters, 0.01)
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
        val vm = hydrationViewModel(repo, initialRange = TimeRange.DAY)

        vm.deleteHydrationEntry("hydration-id")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.hydrationEntries.isEmpty())
        assertEquals(0.0, vm.uiState.value.display.summary.totalLiters, 0.01)
        coVerify { repo.deleteHydrationEntry("hydration-id") }
        coVerify { repo.loadHydrationPeriod(any(), RefreshMode.FORCE) }
    }

    @Test fun `deleteHydrationEntry removes nutrition only drink through nutrition repository`() = runTest {
        val start = today.atStartOfDay(ZoneId.systemDefault()).plusHours(8).toInstant()
        val nutritionEntry = NutritionEntry(
            time = start,
            mealType = 0,
            name = "Coffee",
            energyKcal = null,
            proteinGrams = null,
            carbsGrams = null,
            fatGrams = null,
            fiberGrams = null,
            sugarGrams = null,
            source = "tech.mmarca.openvitals.debug",
            nutrientValues = mapOf(NutritionNutrient.CAFFEINE to 0.095),
            id = "nutrition-id",
            clientRecordId = "openvitals_nutrition_1000_uuid",
            isOpenVitalsEntry = true,
        )
        var nutritionEntries = listOf(nutritionEntry)
        val repo = emptyRepo()
        val nutritionRepo = emptyNutritionRepo()
        coEvery { nutritionRepo.loadNutritionEntries(any(), any()) } answers { nutritionEntries }
        coEvery { nutritionRepo.deleteNutritionEntry("nutrition-id") } coAnswers {
            nutritionEntries = emptyList()
        }
        val vm = hydrationViewModel(repo, nutritionRepository = nutritionRepo, initialRange = TimeRange.DAY)

        vm.deleteHydrationEntry("nutrition-id")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.hydrationEntries.isEmpty())
        coVerify { nutritionRepo.deleteNutritionEntry("nutrition-id") }
        coVerify(exactly = 0) { repo.deleteHydrationEntry("nutrition-id") }
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
        val vm = hydrationViewModel(repo, initialRange = TimeRange.DAY)

        vm.deleteHydrationEntry("external-hydration-id")
        advanceUntilIdle()

        assertEquals(entries, vm.uiState.value.hydrationEntries)
        assertEquals(0.5, vm.uiState.value.display.summary.totalLiters, 0.01)
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

        val vm = hydrationViewModel(repo)

        assertEquals(3, vm.uiState.value.display.summary.trackedDays)
        assertEquals(1.5, vm.uiState.value.display.summary.averageLiters, 0.01)
        assertEquals(2.0, vm.uiState.value.display.summary.bestDayLiters, 0.01)
        assertEquals(1, vm.uiState.value.display.summary.currentTrackedStreakDays)
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

        val vm = hydrationViewModel(repo)

        assertEquals(2, vm.uiState.value.display.summary.currentTrackedStreakDays)
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

        val vm = hydrationViewModel(repo, initialDailyGoalLiters = 2.0)

        assertEquals(3, vm.uiState.value.display.summary.goalMetDays)
        assertEquals(75, vm.uiState.value.display.summary.goalSuccessRatePercent)
        assertEquals(1, vm.uiState.value.display.summary.currentGoalStreakDays)
        assertEquals(2, vm.uiState.value.display.summary.longestGoalStreakDays)
    }

    @Test fun `updating daily goal saves and recalculates goal statistics`() = runTest {
        val hydration = listOf(
            DailyHydration(today.minusDays(1), 2.0),
            DailyHydration(today, 2.5),
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailyHydration(any(), any()) } returns hydration
        var savedGoal: Double? = null
        val vm = hydrationViewModel(
            repository = repo,
            initialDailyGoalLiters = 2.0,
            onDailyGoalChanged = { goal -> savedGoal = goal },
        )

        vm.increaseDailyGoal()

        assertEquals(2.25, savedGoal ?: 0.0, 0.01)
        assertEquals(2.25, vm.uiState.value.dailyGoalLiters, 0.01)
        assertEquals(1, vm.uiState.value.display.summary.goalMetDays)
    }

    @Test fun `updating reminder config saves normalized config`() = runTest {
        var savedConfig: HydrationReminderConfig? = null
        val vm = hydrationViewModel(
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

        val vm = hydrationViewModel(repo)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(ScreenError.Message("timeout"), vm.uiState.value.error)
    }

    @Test fun `selectRange updates selectedRange and reloads`() = runTest {
        val repo = emptyRepo()
        val vm = hydrationViewModel(repo)

        vm.selectRange(TimeRange.MONTH)

        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
        coVerify(atLeast = 2) { repo.loadHydrationPeriod(any()) }
    }

    @Test fun `selectRange saves selected range`() = runTest {
        var savedRange: TimeRange? = null
        val vm = hydrationViewModel(
            repository = emptyRepo(),
            onRangeSelected = { range -> savedRange = range },
        )

        vm.selectRange(TimeRange.MONTH)

        assertEquals(TimeRange.MONTH, savedRange)
    }

    @Test fun `previousPeriod WEEK moves back one week`() = runTest {
        val vm = hydrationViewModel(emptyRepo())
        val before = vm.uiState.value.selectedDate

        vm.previousPeriod()

        assertEquals(before.minusWeeks(1), vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod DAY is blocked when selectedDate is today`() = runTest {
        val repo = emptyRepo()
        val vm = hydrationViewModel(repo)
        vm.selectRange(TimeRange.DAY)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before, vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod WEEK advances from a past week`() = runTest {
        val vm = hydrationViewModel(emptyRepo())
        vm.selectDate(pastAnchor)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before.plusWeeks(1), vm.uiState.value.selectedDate)
    }

    @Test fun `selectDate clamps future date to today`() = runTest {
        val vm = hydrationViewModel(emptyRepo())

        vm.selectDate(today.plusDays(10))

        assertEquals(today, vm.uiState.value.selectedDate)
    }

}
