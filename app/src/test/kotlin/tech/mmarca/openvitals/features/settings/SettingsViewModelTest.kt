package tech.mmarca.openvitals.features.settings

import android.util.Log
import androidx.health.connect.client.records.ExerciseSessionRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.preferences.ActivityWeekMode
import tech.mmarca.openvitals.core.preferences.AppLanguage
import tech.mmarca.openvitals.core.preferences.AppThemeMode
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.core.preferences.UnitSystem
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.repository.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test fun `refresh keeps cycle permissions hidden when cycle tracking is off`() = runTest {
        val vm = SettingsViewModel(
            repository = repo(),
            preferencesRepository = prefs(trackCycle = false),
        )

        assertEquals(setOf("steps", "write", "route"), vm.uiState.value.visiblePermissions)
        assertFalse(vm.uiState.value.trackCycle)
    }

    @Test fun `refresh includes cycle permissions when cycle tracking is on`() = runTest {
        val vm = SettingsViewModel(
            repository = repo(),
            preferencesRepository = prefs(trackCycle = true),
        )

        assertEquals(setOf("steps", "write", "route", "cycle"), vm.uiState.value.visiblePermissions)
        assertTrue(vm.uiState.value.trackCycle)
    }

    @Test fun `missingVisiblePermissions excludes already granted visible permissions`() = runTest {
        val vm = SettingsViewModel(
            repository = repo(grantedPermissions = setOf("steps")),
            preferencesRepository = prefs(trackCycle = true),
        )

        assertEquals(setOf("write", "route", "cycle"), vm.uiState.value.missingVisiblePermissions)
        assertEquals(setOf("write", "cycle"), vm.uiState.value.missingRequestableVisiblePermissions)
        assertEquals(setOf("route"), vm.uiState.value.missingManualVisiblePermissions)
    }

    @Test fun `missingVisiblePermissions is empty when all visible permissions are granted`() = runTest {
        val vm = SettingsViewModel(
            repository = repo(grantedPermissions = setOf("steps", "write", "route", "cycle")),
            preferencesRepository = prefs(trackCycle = true),
        )

        assertTrue(vm.uiState.value.missingVisiblePermissions.isEmpty())
        assertTrue(vm.uiState.value.missingRequestableVisiblePermissions.isEmpty())
        assertTrue(vm.uiState.value.missingManualVisiblePermissions.isEmpty())
    }

    @Test fun `setTrackCycle persists preference and updates visible permissions`() = runTest {
        val prefs = prefs(trackCycle = false)
        val vm = SettingsViewModel(
            repository = repo(),
            preferencesRepository = prefs,
        )

        vm.setTrackCycle(true)

        verify { prefs.trackCycle = true }
        assertTrue(vm.uiState.value.trackCycle)
        assertEquals(setOf("steps", "write", "route", "cycle"), vm.uiState.value.visiblePermissions)
    }

    @Test fun `selectAppLanguage persists preference and updates ui state`() = runTest {
        val prefs = prefs(trackCycle = false)
        val vm = SettingsViewModel(
            repository = repo(),
            preferencesRepository = prefs,
        )

        vm.selectAppLanguage(AppLanguage.SPANISH)

        verify { prefs.appLanguage = AppLanguage.SPANISH }
        assertEquals(AppLanguage.SPANISH, vm.uiState.value.appLanguage)
    }

    @Test fun `selectAppThemeMode persists preference and updates ui state`() = runTest {
        val prefs = prefs(trackCycle = false)
        val vm = SettingsViewModel(
            repository = repo(),
            preferencesRepository = prefs,
        )

        vm.selectAppThemeMode(AppThemeMode.AMOLED)

        verify { prefs.appThemeMode = AppThemeMode.AMOLED }
        assertEquals(AppThemeMode.AMOLED, vm.uiState.value.appThemeMode)
    }

    @Test fun `selectSleepRangeMode persists preference and updates ui state`() = runTest {
        val prefs = prefs(trackCycle = false)
        val vm = SettingsViewModel(
            repository = repo(),
            preferencesRepository = prefs,
        )

        vm.selectSleepRangeMode(SleepRangeMode.NOON)

        verify { prefs.sleepRangeMode = SleepRangeMode.NOON }
        assertEquals(SleepRangeMode.NOON, vm.uiState.value.sleepRangeMode)
    }

    @Test fun `selectActivityWeekMode persists preference and updates ui state`() = runTest {
        val prefs = prefs(trackCycle = false)
        val vm = SettingsViewModel(
            repository = repo(),
            preferencesRepository = prefs,
        )

        vm.selectActivityWeekMode(ActivityWeekMode.LAST_7_DAYS)

        verify { prefs.activityWeekMode = ActivityWeekMode.LAST_7_DAYS }
        assertEquals(ActivityWeekMode.LAST_7_DAYS, vm.uiState.value.activityWeekMode)
    }

    @Test fun `setShowOpenVitalsCalculatedCalories persists preference and updates ui state`() = runTest {
        val prefs = prefs(trackCycle = false)
        val vm = SettingsViewModel(
            repository = repo(),
            preferencesRepository = prefs,
        )

        vm.setShowOpenVitalsCalculatedCalories(true)

        verify { prefs.showOpenVitalsCalculatedCalories = true }
        assertTrue(vm.uiState.value.showOpenVitalsCalculatedCalories)
    }

    @Test fun `selectFavoriteActivity persists preference and updates ui state`() = runTest {
        val prefs = prefs(trackCycle = false)
        val vm = SettingsViewModel(
            repository = repo(),
            preferencesRepository = prefs,
        )

        vm.selectFavoriteActivity(ExerciseSessionRecord.EXERCISE_TYPE_BIKING)

        verify { prefs.favoriteActivityExerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING }
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_BIKING, vm.uiState.value.favoriteActivityExerciseType)
    }

    @Test fun `refresh skips granted permissions when Health Connect is unsupported`() = runTest {
        val repository = repo(availability = HealthConnectAvailability.NOT_SUPPORTED)

        val vm = SettingsViewModel(
            repository = repository,
            preferencesRepository = prefs(trackCycle = true),
        )

        assertEquals(HealthConnectAvailability.NOT_SUPPORTED, vm.uiState.value.availability)
        assertTrue(vm.uiState.value.grantedPermissions.isEmpty())
        coVerify(exactly = 0) { repository.grantedPermissions() }
    }

    private fun repo(
        availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
        grantedPermissions: Set<String> = emptySet(),
    ): HealthRepository =
        mockk<HealthRepository>().also { repo ->
            every { repo.availability() } returns availability
            every { repo.corePermissions } returns setOf("steps")
            every { repo.routePermissions } returns setOf("route")
            every { repo.heartPermissions } returns emptySet()
            every { repo.bodyPermissions } returns emptySet()
            every { repo.activityExtrasPermissions } returns emptySet()
            every { repo.nutritionHydrationPermissions } returns emptySet()
            every { repo.requestableWritePermissions } returns setOf("write")
            every { repo.mindfulnessPermissions } returns emptySet()
            every { repo.additionalDataAccessPermissions } returns emptySet()
            every { repo.vitalsPermissions } returns emptySet()
            every { repo.isMindfulnessAvailable() } returns false
            every { repo.allPermissions } returns setOf("steps", "write", "route")
            every { repo.cyclePermissions } returns setOf("cycle")
            every { repo.manualOnlyPermissions } returns setOf("route")
            coEvery { repo.grantedPermissions() } returns grantedPermissions
        }

    private fun prefs(trackCycle: Boolean): PreferencesRepository =
        mockk<PreferencesRepository>().also { prefs ->
            every { prefs.unitSystem } returns UnitSystem.METRIC
            every { prefs.appLanguage } returns AppLanguage.SYSTEM
            every { prefs.appThemeMode } returns AppThemeMode.SYSTEM
            every { prefs.sleepRangeMode } returns SleepRangeMode.EVENING_18H
            every { prefs.activityWeekMode } returns ActivityWeekMode.MONDAY_TO_SUNDAY
            every { prefs.showOpenVitalsCalculatedCalories } returns false
            every { prefs.favoriteActivityExerciseType } returns null
            every { prefs.trackCycle } returns trackCycle
            every { prefs.appLanguage = any() } just runs
            every { prefs.appThemeMode = any() } just runs
            every { prefs.sleepRangeMode = any() } just runs
            every { prefs.activityWeekMode = any() } just runs
            every { prefs.showOpenVitalsCalculatedCalories = any() } just runs
            every { prefs.favoriteActivityExerciseType = any() } just runs
            every { prefs.trackCycle = any() } just runs
        }
}
