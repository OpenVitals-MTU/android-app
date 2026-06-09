package tech.mmarca.openvitals.features.activity

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.model.BmrEntry
import tech.mmarca.openvitals.data.model.DailyNutrition
import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.data.repository.ActivityPeriodData
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.data.repository.BodyRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

class CaloriesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()

    private fun activityRepo(data: ActivityPeriodData = ActivityPeriodData()) =
        mockk<ActivityRepository>().also { repo ->
            coEvery { repo.loadActivityPeriod(any(), any(), any()) } returns data
        }

    private fun bodyRepo(
        bmrEntries: List<BmrEntry> = emptyList(),
        latestBmrKcal: Double? = null,
    ) =
        mockk<BodyRepository>().also { repo ->
            coEvery { repo.loadBmrEntries(any(), any()) } returns bmrEntries
            coEvery { repo.loadLatestBMR() } returns latestBmrKcal
        }

    @Test
    fun `initial load combines total active and BMR data`() = runTest {
        val nutrition = listOf(DailyNutrition(today, hydrationLiters = 0.0, caloriesBurnedKcal = 2_100.0))
        val steps = listOf(DailySteps(today, steps = 0L, distanceMeters = 0.0, activeCaloriesKcal = 520.0))
        val bmr = listOf(BmrEntry(time = Instant.ofEpochSecond(1_000), kcalPerDay = 1_720.0, source = "test"))
        val activityRepository = activityRepo(
            ActivityPeriodData(
                dailySteps = steps,
                nutrition = nutrition,
            )
        )
        val bodyRepository = bodyRepo(bmrEntries = bmr, latestBmrKcal = 1_715.0)

        val vm = CaloriesViewModel(activityRepository, bodyRepository)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(nutrition, state.nutrition)
        assertEquals(steps, state.dailySteps)
        assertEquals(bmr, state.bmrEntries)
        assertEquals(1_720.0, state.latestBmrEntry?.kcalPerDay ?: 0.0, 0.01)
        assertEquals(1_720.0, state.displayBmrKcal ?: 0.0, 0.01)
        coVerify {
            activityRepository.loadActivityPeriod(
                query = any<PeriodLoadQuery>(),
                includeSteps = true,
                includeNutrition = true,
            )
        }
        coVerify { bodyRepository.loadBmrEntries(any(), any()) }
        coVerify { bodyRepository.loadLatestBMR() }
    }

    @Test
    fun `latest BMR is used when selected period has no BMR readings`() = runTest {
        val bodyRepository = bodyRepo(latestBmrKcal = 1_715.0)

        val vm = CaloriesViewModel(activityRepo(), bodyRepository)

        assertEquals(1_715.0, vm.uiState.value.latestBmrKcal ?: 0.0, 0.01)
        assertEquals(1_715.0, vm.uiState.value.displayBmrKcal ?: 0.0, 0.01)
    }

    @Test
    fun `selectRange saves range and reloads`() = runTest {
        val activityRepository = activityRepo()
        val bodyRepository = bodyRepo()
        var savedRange: TimeRange? = null
        val vm = CaloriesViewModel(
            activityRepository = activityRepository,
            bodyRepository = bodyRepository,
            onRangeSelected = { savedRange = it },
        )

        vm.selectRange(TimeRange.MONTH)

        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
        assertEquals(TimeRange.MONTH, savedRange)
        coVerify(atLeast = 2) { activityRepository.loadActivityPeriod(any(), true, true) }
        coVerify(atLeast = 2) { bodyRepository.loadBmrEntries(any(), any()) }
    }

    @Test
    fun `calorie calculation preference changes reload the period`() = runTest {
        val activityRepository = activityRepo()
        val bodyRepository = bodyRepo()
        val calorieDataMode = MutableStateFlow(false)
        CaloriesViewModel(
            activityRepository = activityRepository,
            bodyRepository = bodyRepository,
            calorieDataModeChanges = calorieDataMode,
        )

        calorieDataMode.value = true

        coVerify(exactly = 2) { activityRepository.loadActivityPeriod(any(), true, true) }
        coVerify(exactly = 2) { bodyRepository.loadBmrEntries(any(), any()) }
    }

    @Test
    fun `load failure sets error and clears loading`() = runTest {
        val activityRepository = mockk<ActivityRepository>()
        coEvery { activityRepository.loadActivityPeriod(any(), any(), any()) } throws RuntimeException("timeout")
        val bodyRepository = bodyRepo()

        val vm = CaloriesViewModel(activityRepository, bodyRepository)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("timeout", vm.uiState.value.error)
    }
}
