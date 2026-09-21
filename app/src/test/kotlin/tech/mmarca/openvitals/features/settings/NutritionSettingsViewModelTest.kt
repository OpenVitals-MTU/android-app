package tech.mmarca.openvitals.features.settings

import android.util.Log
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.domain.preferences.NutritionAverageBasis
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test fun `setShowOpenVitalsCalculatedCalories persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)

        vm.setShowOpenVitalsCalculatedCalories(true)

        verify { prefs.showOpenVitalsCalculatedCalories = true }
        assertTrue(vm.uiState.value.showOpenVitalsCalculatedCalories)
    }

    @Test fun `setNutritionAverageLoggedDaysOnly persists the basis and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)
        assertTrue(vm.uiState.value.nutritionAverageLoggedDaysOnly)

        vm.setNutritionAverageLoggedDaysOnly(false)

        assertEquals(NutritionAverageBasis.EVERY_DAY, prefs.nutritionAverageBasis)
        assertFalse(vm.uiState.value.nutritionAverageLoggedDaysOnly)
    }

    @Test fun `hydration goal persists and reflects the repository clamp`() = runTest {
        val vm = viewModel(preferencesRepository = prefs())

        vm.setHydrationDailyGoalLiters(2.25)
        assertEquals(2.25, vm.uiState.value.hydrationDailyGoalLiters, 0.0)

        vm.setHydrationDailyGoalLiters(0.0)
        assertEquals(0.25, vm.uiState.value.hydrationDailyGoalLiters, 0.0)
    }

    @Test fun `updateCaffeinePreferences persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)
        val caffeinePreferences = CaffeinePreferences(
            profileCompleted = true,
            halfLifeMinutes = 360,
            sleepThresholdMg = 45,
        )

        vm.updateCaffeinePreferences(caffeinePreferences)

        verify { prefs.setCaffeinePreferences(caffeinePreferences) }
        assertEquals(caffeinePreferences, vm.uiState.value.caffeinePreferences)
    }

    @Test fun `the caffeine card sees the profile Health Connect resolved`() = runTest {
        val prefs = prefs()
        val bodyRepository = bodyRepo()
        val resolved = BodyProfile(weightKg = 72.0)
        coEvery { bodyRepository.resolveBodyProfile(any()) } returns resolved
        val vm = viewModel(preferencesRepository = prefs, bodyRepository = bodyRepository)
        advanceUntilIdle()

        assertEquals(resolved, vm.uiState.value.bodyProfile)
    }

    private fun viewModel(
        preferencesRepository: PreferencesRepository = prefs(),
        bodyRepository: BodyRepository = bodyRepo(),
    ): NutritionSettingsViewModel =
        NutritionSettingsViewModel(
            preferencesRepository = preferencesRepository,
            bodyRepository = bodyRepository,
        )

    private fun bodyRepo(): BodyRepository =
        mockk<BodyRepository>().also { repo ->
            coEvery { repo.resolveBodyProfile(any()) } answers { firstArg() }
        }

    private fun prefs(): PreferencesRepository {
        var caffeinePreferences = CaffeinePreferences()
        return mockk<PreferencesRepository>().also { prefs ->
            every { prefs.unitSystem } returns UnitSystem.METRIC
            every { prefs.unitOverride(any()) } returns null
            every { prefs.showOpenVitalsCalculatedCalories } returns false
            every { prefs.showOpenVitalsCalculatedCalories = any() } just runs
            var nutritionAverageBasis = NutritionAverageBasis.LOGGED_DAYS
            every { prefs.nutritionAverageBasis } answers { nutritionAverageBasis }
            every { prefs.nutritionAverageBasis = any() } answers { nutritionAverageBasis = firstArg() }
            var hydrationGoal = PreferencesRepository.DEFAULT_HYDRATION_DAILY_GOAL_LITERS
            every { prefs.hydrationDailyGoalLiters } answers { hydrationGoal }
            every { prefs.hydrationDailyGoalLiters = any() } answers {
                hydrationGoal = firstArg<Double>().coerceIn(0.25, 10.0)
            }
            every { prefs.caffeinePreferences() } answers { caffeinePreferences }
            // The real repository normalizes on write, so the fake must too.
            every { prefs.setCaffeinePreferences(any()) } answers {
                caffeinePreferences = firstArg<CaffeinePreferences>().normalized()
            }
            every { prefs.bodyProfile() } returns BodyProfile()
        }
    }
}
