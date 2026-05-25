package tech.mmarca.openvitals.features.activity

import tech.mmarca.openvitals.data.model.ActivityProgressPoint
import tech.mmarca.openvitals.data.model.DailyNutrition
import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.ActivityPeriodData
import tech.mmarca.openvitals.data.repository.ActivityRepository
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

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test fun `initial range is WEEK`() = runTest {
        val vm = ActivityViewModel(emptyRepo())
        assertEquals(TimeRange.WEEK, vm.uiState.value.selectedRange)
    }

    @Test fun `initial load clears loading and sets empty lists`() = runTest {
        val vm = ActivityViewModel(emptyRepo())
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.dailySteps.isEmpty())
        assertTrue(state.nutrition.isEmpty())
    }

    // ─── Load success / failure ───────────────────────────────────────────────

    @Test fun `load success populates data`() = runTest {
        val steps = listOf(DailySteps(today, 8_000L, 6_000.0))
        val repo = emptyRepo()
        coEvery { repo.loadDailySteps(any(), any()) } returns steps

        val vm = ActivityViewModel(repo)

        assertEquals(steps, vm.uiState.value.dailySteps)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadActivityPeriod(any(), any(), any()) } throws RuntimeException("timeout")

        val vm = ActivityViewModel(repo)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("timeout", vm.uiState.value.error)
    }

    // ─── selectRange ──────────────────────────────────────────────────────────

    @Test fun `selectRange updates selectedRange`() = runTest {
        val vm = ActivityViewModel(emptyRepo())
        vm.selectRange(TimeRange.MONTH)
        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
    }

    @Test fun `selectRange triggers reload`() = runTest {
        val repo = emptyRepo()
        val vm = ActivityViewModel(repo)
        vm.selectRange(TimeRange.MONTH)
        // init load + selectRange load = 2 bundled period calls
        coVerify(atLeast = 2) { repo.loadActivityPeriod(any(), any(), any()) }
    }

    // ─── previousPeriod ───────────────────────────────────────────────────────

    @Test fun `previousPeriod DAY moves back one day`() = runTest {
        val vm = ActivityViewModel(emptyRepo())
        vm.selectRange(TimeRange.DAY)
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusDays(1), vm.uiState.value.selectedDate)
    }

    @Test fun `previousPeriod WEEK moves back one week`() = runTest {
        val vm = ActivityViewModel(emptyRepo())
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusWeeks(1), vm.uiState.value.selectedDate)
    }

    @Test fun `previousPeriod MONTH moves back one month`() = runTest {
        val vm = ActivityViewModel(emptyRepo())
        vm.selectRange(TimeRange.MONTH)
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusMonths(1), vm.uiState.value.selectedDate)
    }

    @Test fun `previousPeriod YEAR moves back one year`() = runTest {
        val vm = ActivityViewModel(emptyRepo())
        vm.selectRange(TimeRange.YEAR)
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusYears(1), vm.uiState.value.selectedDate)
    }

    // ─── nextPeriod ───────────────────────────────────────────────────────────

    @Test fun `nextPeriod DAY is blocked when selectedDate is today`() = runTest {
        val repo = emptyRepo()
        val vm = ActivityViewModel(repo)
        vm.selectRange(TimeRange.DAY)
        // selectedDate should be today after init
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before, vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod DAY advances when selectedDate is in the past`() = runTest {
        val repo = emptyRepo()
        val vm = ActivityViewModel(repo)
        vm.selectRange(TimeRange.DAY)
        vm.selectDate(today.minusDays(2))
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before.plusDays(1), vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod WEEK advances from a past week`() = runTest {
        val repo = emptyRepo()
        val vm = ActivityViewModel(repo)
        vm.selectDate(pastAnchor)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before.plusWeeks(1), vm.uiState.value.selectedDate)
    }

    // ─── selectDate ───────────────────────────────────────────────────────────

    @Test fun `selectDate clamps future date to today`() = runTest {
        val vm = ActivityViewModel(emptyRepo())
        vm.selectDate(today.plusDays(10))
        assertEquals(today, vm.uiState.value.selectedDate)
    }

    @Test fun `selectDate accepts past date unchanged`() = runTest {
        val vm = ActivityViewModel(emptyRepo())
        vm.selectDate(pastAnchor)
        assertEquals(pastAnchor, vm.uiState.value.selectedDate)
    }

    // ─── DAY range loads activityProgress ────────────────────────────────────

    @Test fun `load for DAY range calls loadActivityProgress`() = runTest {
        val progress = listOf(ActivityProgressPoint(java.time.Instant.now(), 500L, null, null))
        val repo = emptyRepo()
        coEvery { repo.loadActivityProgress(any()) } returns progress

        val vm = ActivityViewModel(repo)
        vm.selectRange(TimeRange.DAY)

        assertEquals(progress, vm.uiState.value.activityProgress)
    }

    @Test fun `load for DAY range preserves detailed activityProgress fields`() = runTest {
        val progress = listOf(
            ActivityProgressPoint(
                time = java.time.Instant.now(),
                totalSteps = 500L,
                totalDistanceMeters = 350.0,
                totalCaloriesBurnedKcal = 80.0,
                totalActiveCaloriesKcal = 45.0,
                totalFloorsClimbed = 3,
                totalElevationGainedMeters = 12.0,
            )
        )
        val repo = emptyRepo()
        coEvery { repo.loadActivityProgress(any()) } returns progress

        val vm = ActivityViewModel(repo)
        vm.selectRange(TimeRange.DAY)

        val point = vm.uiState.value.activityProgress.single()
        assertEquals(45.0, point.totalActiveCaloriesKcal!!, 0.01)
        assertEquals(3, point.totalFloorsClimbed)
        assertEquals(12.0, point.totalElevationGainedMeters!!, 0.01)
    }

    @Test fun `load for WEEK range returns empty activityProgress`() = runTest {
        val repo = emptyRepo()
        val vm = ActivityViewModel(repo)
        // WEEK is the default range
        assertTrue(vm.uiState.value.activityProgress.isEmpty())
        coVerify(exactly = 0) { repo.loadActivityProgress(any()) }
    }

    // ─── A3: floors, active calories, elevation ───────────────────────────────

    @Test fun `DailySteps with A3 fields flows through state unchanged`() = runTest {
        val steps = listOf(
            DailySteps(
                date = today,
                steps = 8_000L,
                distanceMeters = 6_000.0,
                floorsClimbed = 12,
                activeCaloriesKcal = 350.0,
                elevationGainedMeters = 42.0,
            )
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailySteps(any(), any()) } returns steps

        val vm = ActivityViewModel(repo)

        val day = vm.uiState.value.dailySteps.single()
        assertEquals(12, day.floorsClimbed)
        assertEquals(350.0, day.activeCaloriesKcal!!, 0.01)
        assertEquals(42.0, day.elevationGainedMeters!!, 0.01)
    }

    @Test fun `DailySteps with null A3 fields flows through state as null`() = runTest {
        val steps = listOf(DailySteps(today, 5_000L, 4_000.0))
        val repo = emptyRepo()
        coEvery { repo.loadDailySteps(any(), any()) } returns steps

        val vm = ActivityViewModel(repo)

        val day = vm.uiState.value.dailySteps.single()
        assertNull(day.floorsClimbed)
        assertNull(day.activeCaloriesKcal)
        assertNull(day.elevationGainedMeters)
    }

    @Test fun `A3 fields are preserved across multiple days`() = runTest {
        val steps = listOf(
            DailySteps(today.minusDays(1), 7_000L, 5_500.0, floorsClimbed = 5, activeCaloriesKcal = 200.0, elevationGainedMeters = 20.0),
            DailySteps(today, 9_000L, 7_000.0, floorsClimbed = 10, activeCaloriesKcal = 400.0, elevationGainedMeters = 50.0),
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailySteps(any(), any()) } returns steps

        val vm = ActivityViewModel(repo)

        val days = vm.uiState.value.dailySteps
        assertEquals(2, days.size)
        assertEquals(15, days.sumOf { it.floorsClimbed ?: 0 })
        assertEquals(600.0, days.sumOf { it.activeCaloriesKcal ?: 0.0 }, 0.01)
        assertEquals(70.0, days.sumOf { it.elevationGainedMeters ?: 0.0 }, 0.01)
    }

    @Test fun `mixed A3 data — some days have fields some do not`() = runTest {
        val steps = listOf(
            DailySteps(today.minusDays(1), 6_000L, 4_800.0, floorsClimbed = null),
            DailySteps(today, 8_000L, 6_400.0, floorsClimbed = 7),
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailySteps(any(), any()) } returns steps

        val vm = ActivityViewModel(repo)

        val days = vm.uiState.value.dailySteps
        assertNull(days[0].floorsClimbed)
        assertEquals(7, days[1].floorsClimbed)
    }

    // ─── Zero = permission granted, no data ───────────────────────────────────

    @Test fun `floorsClimbed zero is non-null so chart shows for any range including DAY`() = runTest {
        val steps = listOf(DailySteps(today, 5_000L, 4_000.0, floorsClimbed = 0))
        val repo = emptyRepo()
        coEvery { repo.loadDailySteps(any(), any()) } returns steps

        val vm = ActivityViewModel(repo)
        vm.selectRange(TimeRange.DAY)

        assertTrue(vm.uiState.value.dailySteps.any { it.floorsClimbed != null })
    }

    @Test fun `floorsClimbed null means permission not granted — chart hidden for any range`() = runTest {
        val steps = listOf(DailySteps(today, 5_000L, 4_000.0, floorsClimbed = null))
        val repo = emptyRepo()
        coEvery { repo.loadDailySteps(any(), any()) } returns steps

        val vm = ActivityViewModel(repo)
        vm.selectRange(TimeRange.DAY)

        assertFalse(vm.uiState.value.dailySteps.any { it.floorsClimbed != null })
    }

    @Test fun `elevationGainedMeters zero is non-null so chart shows for any range including DAY`() = runTest {
        val steps = listOf(DailySteps(today, 5_000L, 4_000.0, elevationGainedMeters = 0.0))
        val repo = emptyRepo()
        coEvery { repo.loadDailySteps(any(), any()) } returns steps

        val vm = ActivityViewModel(repo)
        vm.selectRange(TimeRange.DAY)

        assertTrue(vm.uiState.value.dailySteps.any { it.elevationGainedMeters != null })
    }

    @Test fun `activeCaloriesKcal zero is non-null so chart shows for any range including DAY`() = runTest {
        val steps = listOf(DailySteps(today, 5_000L, 4_000.0, activeCaloriesKcal = 0.0))
        val repo = emptyRepo()
        coEvery { repo.loadDailySteps(any(), any()) } returns steps

        val vm = ActivityViewModel(repo)
        vm.selectRange(TimeRange.DAY)

        assertTrue(vm.uiState.value.dailySteps.any { it.activeCaloriesKcal != null })
    }

    // ─── Calories burned chart data ──────────────────────────────────────────

    @Test fun `nutrition with calories burned flows through state for any range including DAY`() = runTest {
        val nutrition = listOf(DailyNutrition(today, hydrationLiters = 0.0, caloriesBurnedKcal = 500.0))
        val repo = emptyRepo()
        coEvery { repo.loadDailyNutrition(any(), any()) } returns nutrition

        val vm = ActivityViewModel(repo, selectedMetric = ActivityMetric.CALORIES_BURNED)
        vm.selectRange(TimeRange.DAY)

        assertEquals(500.0, vm.uiState.value.nutrition.single().caloriesBurnedKcal, 0.001)
        assertTrue(vm.uiState.value.nutrition.any { it.caloriesBurnedKcal > 0 })
    }

    @Test fun `nutrition with zero calories burned does not satisfy calories chart condition for period ranges`() = runTest {
        val nutrition = listOf(DailyNutrition(today, hydrationLiters = 0.0, caloriesBurnedKcal = 0.0))
        val repo = emptyRepo()
        coEvery { repo.loadDailyNutrition(any(), any()) } returns nutrition

        val vm = ActivityViewModel(repo, selectedMetric = ActivityMetric.CALORIES_BURNED)

        assertFalse(vm.uiState.value.nutrition.any { it.caloriesBurnedKcal > 0 })
    }

    @Test fun `calories burned totals across multiple days sum correctly`() = runTest {
        val nutrition = listOf(
            DailyNutrition(today.minusDays(1), hydrationLiters = 0.0, caloriesBurnedKcal = 500.0),
            DailyNutrition(today, hydrationLiters = 0.0, caloriesBurnedKcal = 700.0),
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailyNutrition(any(), any()) } returns nutrition

        val vm = ActivityViewModel(repo, selectedMetric = ActivityMetric.CALORIES_BURNED)

        assertEquals(1_200.0, vm.uiState.value.nutrition.sumOf { it.caloriesBurnedKcal }, 0.001)
    }
}
