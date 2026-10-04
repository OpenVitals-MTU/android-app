package tech.mmarca.openvitals.features.activity

import tech.mmarca.openvitals.navigation.METRIC_ID_ARG
import tech.mmarca.openvitals.data.repository.contract.FakePreferences
import androidx.lifecycle.SavedStateHandle
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.domain.model.ActivityProgressPoint
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.query.ActivityPeriodData
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
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

class ActivityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val pastAnchor = today.minusWeeks(4) // safely in the past for all ranges

    private fun viewModel(
        repo: ActivityRepository,
        selectedMetric: ActivityMetric = ActivityMetric.STEPS,
        preferences: FakePreferences = FakePreferences(),
    ) = ActivityViewModel(
        repository = repo,
        periodPreferences = preferences,
        dailyGoalPreferences = preferences,
        calorieDisplayPreferences = preferences,
        dispatchers = mainDispatcherRule.dispatcherProvider,
        savedStateHandle = SavedStateHandle(mapOf(METRIC_ID_ARG to selectedMetric.routeId())),
    )

    @Test fun `every activity metric round-trips through its route id`() {
        assertEquals(
            ActivityMetric.entries.toList(),
            ActivityMetric.entries.map { activityMetricFromRoute(it.routeId()) },
        )
    }

    private fun emptyRepo() = mockk<ActivityRepository>().also { repo ->
        coEvery { repo.loadDailySteps(any(), any()) } returns emptyList()
        coEvery { repo.loadDailyNutrition(any(), any()) } returns emptyList()
        coEvery { repo.loadActivityProgress(any()) } returns emptyList()
        coEvery { repo.loadActivityPeriod(any(), any(), any()) } coAnswers {
            val query = firstArg<PeriodLoadQuery>()
            val includeSteps = secondArg<Boolean>()
            val includeNutrition = thirdArg<Boolean>()
            val windows = query.windows
            ActivityPeriodData(
                dailySteps = if (includeSteps) repo.loadDailySteps(windows.current.start, windows.current.end) else emptyList(),
                previousDailySteps = if (includeSteps) repo.loadDailySteps(windows.previous.start, windows.previous.end) else emptyList(),
                baselineDailySteps = if (includeSteps) repo.loadDailySteps(windows.baseline.start, windows.baseline.end) else emptyList(),
                nutrition = if (includeNutrition) repo.loadDailyNutrition(windows.current.start, windows.current.end) else emptyList(),
                previousNutrition = if (includeNutrition) repo.loadDailyNutrition(windows.previous.start, windows.previous.end) else emptyList(),
                baselineNutrition = if (includeNutrition) repo.loadDailyNutrition(windows.baseline.start, windows.baseline.end) else emptyList(),
                activityProgress = if (query.range == TimeRange.DAY) repo.loadActivityProgress(windows.current.start) else emptyList(),
            )
        }
    }

    // Daily goal.

    @Test fun `the goal steppers move and persist the daily goal`() = runTest {
        val preferences = FakePreferences()
        val vm = viewModel(emptyRepo(), preferences = preferences)

        // The steps goal starts at 8 000 and moves in 500s.
        assertEquals(8_000.0, vm.uiState.value.dailyGoal, 0.0)

        vm.increaseDailyGoal()
        assertEquals(8_500.0, vm.uiState.value.dailyGoal, 0.0)

        vm.decreaseDailyGoal()
        vm.decreaseDailyGoal()
        assertEquals(7_500.0, vm.uiState.value.dailyGoal, 0.0)

        // Every move is written through, not just the last one.
        assertEquals(listOf(8_500.0, 8_000.0, 7_500.0), preferences.storedGoals)
    }

    @Test fun `the daily goal stops at its floor and its ceiling`() = runTest {
        val goalKey = ActivityMetric.STEPS.dailyGoalKey
        val vm = viewModel(emptyRepo(), preferences = FakePreferences(initialGoal = goalKey.minValue))

        vm.decreaseDailyGoal()
        assertEquals(goalKey.minValue, vm.uiState.value.dailyGoal, 0.0)

        vm.setDailyGoal(goalKey.maxValue + goalKey.step)
        assertEquals(goalKey.maxValue, vm.uiState.value.dailyGoal, 0.0)
    }

    @Test fun `moving the goal re-derives the goal progress without reloading`() = runTest {
        val repo = emptyRepo()
        // 8 000 steps today clears the default 8 000 goal and misses 8 500.
        coEvery { repo.loadDailySteps(any(), any()) } returns listOf(DailySteps(today, 8_000L, 6_000.0))
        val vm = viewModel(repo)
        assertEquals(1, vm.uiState.value.display.metric.goalProgress!!.goalMetDays)

        vm.increaseDailyGoal()

        assertEquals(8_500.0, vm.uiState.value.dailyGoal, 0.0)
        assertEquals(0, vm.uiState.value.display.metric.goalProgress!!.goalMetDays)
        // A goal move is a derivation, not a load.
        coVerify(exactly = 1) { repo.loadActivityPeriod(any(), any(), any()) }
    }

    // Initial state.

    @Test fun `initial range is WEEK`() = runTest {
        val vm = viewModel(emptyRepo())
        assertEquals(TimeRange.WEEK, vm.uiState.value.selectedRange)
    }

    @Test fun `initial load clears loading and sets empty lists`() = runTest {
        val vm = viewModel(emptyRepo())
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.dailySteps.isEmpty())
        assertTrue(state.nutrition.isEmpty())
    }

    // Load success and failure.

    @Test fun `load success populates data`() = runTest {
        val steps = listOf(DailySteps(today, 8_000L, 6_000.0))
        val repo = emptyRepo()
        coEvery { repo.loadDailySteps(any(), any()) } returns steps

        val vm = viewModel(repo)

        assertEquals(steps, vm.uiState.value.dailySteps)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `load success populates display metric values`() = runTest {
        val steps = listOf(
            DailySteps(today.minusDays(1), 6_000L, 4_800.0),
            DailySteps(today, 8_000L, 6_400.0),
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailySteps(any(), any()) } returns steps

        val vm = viewModel(repo)

        assertEquals(listOf(6_000.0, 8_000.0), vm.uiState.value.display.metric.values)
    }

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadActivityPeriod(any(), any(), any()) } throws RuntimeException("timeout")

        val vm = viewModel(repo)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(ScreenError.Message("timeout"), vm.uiState.value.error)
    }

    // selectRange.

    @Test fun `selectRange updates selectedRange`() = runTest {
        val vm = viewModel(emptyRepo())
        vm.selectRange(TimeRange.MONTH)
        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
    }

    // previousPeriod.

    @Test fun `previousPeriod moves back one period of the selected range`() = runTest {
        val vm = viewModel(emptyRepo())

        val steppedBack = listOf(TimeRange.DAY, TimeRange.WEEK, TimeRange.MONTH, TimeRange.YEAR).associateWith { range ->
            vm.selectRange(range)
            vm.selectDate(today)
            vm.previousPeriod()
            vm.uiState.value.selectedDate
        }

        assertEquals(
            mapOf(
                TimeRange.DAY to today.minusDays(1),
                TimeRange.WEEK to today.minusWeeks(1),
                TimeRange.MONTH to today.minusMonths(1),
                TimeRange.YEAR to today.minusYears(1),
            ),
            steppedBack,
        )
    }

    // nextPeriod.

    @Test fun `nextPeriod DAY is blocked when selectedDate is today`() = runTest {
        val repo = emptyRepo()
        val vm = viewModel(repo)
        vm.selectRange(TimeRange.DAY)
        // selectedDate is today after init.
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before, vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod WEEK advances from a past week`() = runTest {
        val repo = emptyRepo()
        val vm = viewModel(repo)
        vm.selectDate(pastAnchor)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before.plusWeeks(1), vm.uiState.value.selectedDate)
    }

    // selectDate.

    @Test fun `selectDate keeps a past date and clamps a future one to today`() = runTest {
        val vm = viewModel(emptyRepo())

        vm.selectDate(pastAnchor)
        val afterPast = vm.uiState.value.selectedDate
        vm.selectDate(today.plusDays(10))

        assertEquals(listOf(pastAnchor, today), listOf(afterPast, vm.uiState.value.selectedDate))
    }

    // DAY range loads activityProgress.

    @Test fun `load for DAY range calls loadActivityProgress`() = runTest {
        val progress = listOf(ActivityProgressPoint(java.time.Instant.now(), 500L, null, null))
        val repo = emptyRepo()
        coEvery { repo.loadActivityProgress(any()) } returns progress

        val vm = viewModel(repo)
        vm.selectRange(TimeRange.DAY)

        assertEquals(progress, vm.uiState.value.activityProgress)
    }

    @Test fun `load for WEEK range returns empty activityProgress`() = runTest {
        val repo = emptyRepo()
        val vm = viewModel(repo)
        // WEEK is the default range.
        assertTrue(vm.uiState.value.activityProgress.isEmpty())
        coVerify(exactly = 0) { repo.loadActivityProgress(any()) }
    }

    // Calories burned chart data.

    @Test fun `nutrition with calories burned flows through state for any range including DAY`() = runTest {
        val nutrition = listOf(DailyNutrition(today, hydrationLiters = 0.0, caloriesBurnedKcal = 500.0))
        val repo = emptyRepo()
        coEvery { repo.loadDailyNutrition(any(), any()) } returns nutrition

        val vm = viewModel(repo, selectedMetric = ActivityMetric.CALORIES_BURNED)
        vm.selectRange(TimeRange.DAY)

        assertEquals(nutrition, vm.uiState.value.nutrition)
    }
}
