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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportWorkController
import tech.mmarca.openvitals.features.activity.maps.OfflineMapLibraryState
import tech.mmarca.openvitals.features.activity.maps.OfflineMapRepository
import tech.mmarca.openvitals.healthconnect.HealthConnectPermissionUxState
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportWorkController
import tech.mmarca.openvitals.util.MainDispatcherRule
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences

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

    @Test fun `refresh includes cycle permissions with visible permissions`() = runTest {
        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs(),
            appleHealthImportWorkController = importController(),
            permissionUxState = permissionUxState(),
        )

        assertEquals(setOf("steps", "write", "route", "cycle"), vm.uiState.value.visiblePermissions)
    }

    @Test fun `missingVisiblePermissions excludes already granted visible permissions`() = runTest {
        val vm = viewModel(
            repository = repo(grantedPermissions = setOf("steps")),
            preferencesRepository = prefs(),
            appleHealthImportWorkController = importController(),
            permissionUxState = permissionUxState(),
        )

        assertEquals(setOf("write", "route", "cycle"), vm.uiState.value.missingVisiblePermissions)
        assertEquals(setOf("route"), vm.uiState.value.missingManualVisiblePermissions)
    }

    @Test fun `missingVisiblePermissions is empty when all visible permissions are granted`() = runTest {
        val vm = viewModel(
            repository = repo(grantedPermissions = setOf("steps", "write", "route", "cycle")),
            preferencesRepository = prefs(),
            appleHealthImportWorkController = importController(),
            permissionUxState = permissionUxState(),
        )

        assertTrue(vm.uiState.value.missingVisiblePermissions.isEmpty())
        assertTrue(vm.uiState.value.missingManualVisiblePermissions.isEmpty())
    }

    @Test fun `selectAppLanguage persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs,
            appleHealthImportWorkController = importController(),
            permissionUxState = permissionUxState(),
        )

        vm.selectAppLanguage(AppLanguage.SPANISH)

        verify { prefs.appLanguage = AppLanguage.SPANISH }
        assertEquals(AppLanguage.SPANISH, vm.uiState.value.appLanguage)
    }

    @Test fun `selectAppThemeMode persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs,
            appleHealthImportWorkController = importController(),
            permissionUxState = permissionUxState(),
        )

        vm.selectAppThemeMode(AppThemeMode.AMOLED)

        verify { prefs.appThemeMode = AppThemeMode.AMOLED }
        assertEquals(AppThemeMode.AMOLED, vm.uiState.value.appThemeMode)
    }

    @Test fun `selectSleepRangeMode persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs,
            appleHealthImportWorkController = importController(),
            permissionUxState = permissionUxState(),
        )

        vm.selectSleepRangeMode(SleepRangeMode.NOON)

        verify { prefs.sleepRangeMode = SleepRangeMode.NOON }
        assertEquals(SleepRangeMode.NOON, vm.uiState.value.sleepRangeMode)
    }

    @Test fun `selectActivityWeekMode persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs,
            appleHealthImportWorkController = importController(),
            permissionUxState = permissionUxState(),
        )

        vm.selectActivityWeekMode(ActivityWeekMode.LAST_7_DAYS)

        verify { prefs.activityWeekMode = ActivityWeekMode.LAST_7_DAYS }
        assertEquals(ActivityWeekMode.LAST_7_DAYS, vm.uiState.value.activityWeekMode)
    }

    @Test fun `updateActivityRecordingPreferences persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs,
            appleHealthImportWorkController = importController(),
            permissionUxState = permissionUxState(),
        )
        val recordingPreferences = ActivityRecordingPreferences(
            autoIdleEnabled = false,
            autoIdleTimeoutSeconds = 30,
            keepScreenOnDuringRecording = true,
            requiredGpsAccuracyMeters = 50,
            routeGapMeters = null,
            barometerClimbEnabled = false,
        )

        vm.updateActivityRecordingPreferences(recordingPreferences)

        verify { prefs.setActivityRecordingPreferences(recordingPreferences) }
        assertEquals(recordingPreferences, vm.uiState.value.activityRecordingPreferences)
    }

    @Test fun `setShowOpenVitalsCalculatedCalories persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs,
            appleHealthImportWorkController = importController(),
            permissionUxState = permissionUxState(),
        )

        vm.setShowOpenVitalsCalculatedCalories(true)

        verify { prefs.showOpenVitalsCalculatedCalories = true }
        assertTrue(vm.uiState.value.showOpenVitalsCalculatedCalories)
    }

    @Test fun `selectFavoriteActivity persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs,
            appleHealthImportWorkController = importController(),
            permissionUxState = permissionUxState(),
        )

        vm.selectFavoriteActivity(ExerciseSessionRecord.EXERCISE_TYPE_BIKING)

        verify { prefs.favoriteActivityExerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING }
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_BIKING, vm.uiState.value.favoriteActivityExerciseType)
    }

    @Test fun `refresh skips granted permissions when Health Connect is unsupported`() = runTest {
        val repository = repo(availability = HealthConnectAvailability.NOT_SUPPORTED)

        val vm = viewModel(
            repository = repository,
            preferencesRepository = prefs(),
            appleHealthImportWorkController = importController(),
            permissionUxState = permissionUxState(),
        )

        assertEquals(HealthConnectAvailability.NOT_SUPPORTED, vm.uiState.value.availability)
        assertTrue(vm.uiState.value.grantedPermissions.isEmpty())
        coVerify(exactly = 0) { repository.grantedPermissions() }
    }

    private fun viewModel(
        repository: HealthRepository = repo(),
        preferencesRepository: PreferencesRepository = prefs(),
        appleHealthImportWorkController: AppleHealthImportWorkController = importController(),
        offlineMapRepository: OfflineMapRepository = offlineMapRepository(),
        offlineMapImportWorkController: OfflineMapImportWorkController = offlineMapImportController(),
        permissionUxState: HealthConnectPermissionUxState = permissionUxState(),
    ): SettingsViewModel =
        SettingsViewModel(
            repository = repository,
            preferencesRepository = preferencesRepository,
            appleHealthImportWorkController = appleHealthImportWorkController,
            offlineMapRepository = offlineMapRepository,
            offlineMapImportWorkController = offlineMapImportWorkController,
            permissionUxState = permissionUxState,
        )

    private fun repo(
        availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
        grantedPermissions: Set<String> = emptySet(),
    ): HealthRepository =
        mockk<HealthRepository>().also { repo ->
            every { repo.availability() } returns availability
            every { repo.minimumOnboardingPermissions } returns setOf("steps")
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
            every { repo.dataImportWritePermissions } returns emptySet()
            every { repo.isMindfulnessAvailable() } returns false
            every { repo.allPermissions } returns setOf("steps", "write", "route", "cycle")
            every { repo.cyclePermissions } returns setOf("cycle")
            every { repo.manualOnlyPermissions } returns setOf("route")
            coEvery { repo.grantedPermissions() } returns grantedPermissions
        }

    private fun permissionUxState(): HealthConnectPermissionUxState =
        mockk<HealthConnectPermissionUxState>(relaxed = true)

    private fun prefs(): PreferencesRepository =
        mockk<PreferencesRepository>().also { prefs ->
            every { prefs.unitSystem } returns UnitSystem.METRIC
            every { prefs.appLanguage } returns AppLanguage.SYSTEM
            every { prefs.appThemeMode } returns AppThemeMode.SYSTEM
            every { prefs.sleepRangeMode } returns SleepRangeMode.EVENING_18H
            every { prefs.activityWeekMode } returns ActivityWeekMode.MONDAY_TO_SUNDAY
            every { prefs.activityRecordingPreferences() } returns ActivityRecordingPreferences()
            every { prefs.showOpenVitalsCalculatedCalories } returns false
            every { prefs.favoriteActivityExerciseType } returns null
            every { prefs.bodyEnergyCalibration() } returns BodyEnergyCalibration.Automatic
            every { prefs.healthConnectSyncEnabled } returns true
            every { prefs.appLockEnabled } returns false
            every { prefs.appLanguage = any() } just runs
            every { prefs.appThemeMode = any() } just runs
            every { prefs.sleepRangeMode = any() } just runs
            every { prefs.activityWeekMode = any() } just runs
            every { prefs.setActivityRecordingPreferences(any()) } just runs
            every { prefs.showOpenVitalsCalculatedCalories = any() } just runs
            every { prefs.favoriteActivityExerciseType = any() } just runs
            every { prefs.setBodyEnergyCalibration(any()) } just runs
        }

    private fun importController(): AppleHealthImportWorkController =
        mockk<AppleHealthImportWorkController>(relaxed = true).also { controller ->
            every { controller.workInfos } returns emptyFlow()
        }

    private fun offlineMapRepository(): OfflineMapRepository =
        mockk<OfflineMapRepository>(relaxed = true).also { repository ->
            every { repository.state } returns MutableStateFlow(OfflineMapLibraryState())
        }

    private fun offlineMapImportController(): OfflineMapImportWorkController =
        mockk<OfflineMapImportWorkController>(relaxed = true).also { controller ->
            every { controller.workInfos } returns emptyFlow()
        }
}
