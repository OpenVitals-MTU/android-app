package tech.mmarca.openvitals.features.settings

import android.net.Uri
import android.util.Log
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.work.Data
import androidx.work.WorkInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportWorkController
import tech.mmarca.openvitals.features.activity.maps.OfflineMapLibraryState
import tech.mmarca.openvitals.features.activity.maps.OfflineMapRepository
import tech.mmarca.openvitals.healthconnect.HealthConnectPermissionUxState
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthExportFingerprint
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportAnalysisResult
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportCategory
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportCategorySummary
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportService
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportWorkController
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportWorker
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFileImporter
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFileImport
import tech.mmarca.openvitals.util.MainDispatcherRule
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
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

    @Test fun `setDynamicColor persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs,
            appleHealthImportWorkController = importController(),
            permissionUxState = permissionUxState(),
        )

        vm.setDynamicColor(true)

        verify { prefs.dynamicColor = true }
        assertTrue(vm.uiState.value.dynamicColor)
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

    @Test fun `updateCaffeinePreferences persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs,
            appleHealthImportWorkController = importController(),
            permissionUxState = permissionUxState(),
        )
        val caffeinePreferences = CaffeinePreferences(
            profileCompleted = true,
            halfLifeMinutes = 360,
            sleepThresholdMg = 45,
        )

        vm.updateCaffeinePreferences(caffeinePreferences)

        verify { prefs.setCaffeinePreferences(caffeinePreferences) }
        assertEquals(caffeinePreferences, vm.uiState.value.caffeinePreferences)
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

    @Test fun `apple import observer ignores stale finished failures without current work`() = runTest {
        val staleFailure = workInfo(state = WorkInfo.State.FAILED)
        val importController = importController(
            workInfos = MutableStateFlow(listOf(staleFailure)),
        )

        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs(),
            appleHealthImportWorkController = importController,
            permissionUxState = permissionUxState(),
        )
        advanceUntilIdle()

        assertNull(vm.uiState.value.appleHealthImportError)
        verify(exactly = 0) { importController.errorFor(staleFailure) }
    }

    @Test fun `apple import observer uses current import work over older failures`() = runTest {
        val workInfos = MutableStateFlow<List<WorkInfo>>(emptyList())
        val staleFailure = workInfo(state = WorkInfo.State.FAILED)
        val currentWorkId = UUID.randomUUID()
        val currentFailure = workInfo(id = currentWorkId, state = WorkInfo.State.FAILED)
        val importController = importController(workInfos = workInfos)
        val importService = importService()
        val uri = mockk<Uri>()
        every {
            importController.enqueue(
                uri = uri,
                selectedCategories = setOf(AppleHealthImportCategory.ACTIVITY),
                expectedSelectedRecords = 1,
            )
        } returns currentWorkId
        every { importController.errorFor(currentFailure) } returns "current failure"

        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs(),
            appleHealthImportService = importService,
            appleHealthImportWorkController = importController,
            permissionUxState = permissionUxState(),
        )

        vm.analyzeAppleHealthExport(uri)
        advanceUntilIdle()
        vm.importSelectedAppleHealthExport()
        advanceUntilIdle()
        workInfos.value = listOf(staleFailure, currentFailure)
        advanceUntilIdle()

        assertEquals("current failure", vm.uiState.value.appleHealthImportError)
        verify(exactly = 0) { importController.errorFor(staleFailure) }
        verify {
            Log.e(
                AppleHealthImportWorker.LogTag,
                match { message -> message.contains("current failure") && message.contains(currentWorkId.toString()) },
            )
        }
    }

    @Test fun `re-selecting the same file reuses the previous analysis`() = runTest {
        val importService = importService()
        val importController = importController()
        val firstUri = mockk<Uri>()
        val secondUri = mockk<Uri>()

        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs(),
            appleHealthImportService = importService,
            appleHealthImportWorkController = importController,
            permissionUxState = permissionUxState(),
        )

        vm.analyzeAppleHealthExport(firstUri)
        advanceUntilIdle()
        val firstAnalysis = vm.uiState.value.appleHealthImportAnalysis
        assertEquals(setOf(AppleHealthImportCategory.ACTIVITY), vm.uiState.value.selectedAppleHealthImportCategories)

        vm.analyzeAppleHealthExport(secondUri)
        advanceUntilIdle()

        coVerify(exactly = 1) { importService.analyzeAppleHealthExport(any(), any()) }
        assertEquals(firstAnalysis, vm.uiState.value.appleHealthImportAnalysis)
        assertEquals(setOf(AppleHealthImportCategory.ACTIVITY), vm.uiState.value.selectedAppleHealthImportCategories)
        verify { importController.persistReadPermission(firstUri) }
        verify { importController.persistReadPermission(secondUri) }
    }

    @Test fun `re-selecting a different file re-analyzes it`() = runTest {
        val importService = importService()
        val firstUri = mockk<Uri>()
        val secondUri = mockk<Uri>()
        coEvery { importService.fingerprintOf(firstUri) } returns AppleHealthExportFingerprint(
            displayName = "export-1.zip",
            size = 1L,
        )
        coEvery { importService.fingerprintOf(secondUri) } returns AppleHealthExportFingerprint(
            displayName = "export-2.zip",
            size = 2L,
        )

        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs(),
            appleHealthImportService = importService,
            appleHealthImportWorkController = importController(),
            permissionUxState = permissionUxState(),
        )

        vm.analyzeAppleHealthExport(firstUri)
        advanceUntilIdle()
        vm.analyzeAppleHealthExport(secondUri)
        advanceUntilIdle()

        coVerify(exactly = 2) { importService.analyzeAppleHealthExport(any(), any()) }
    }

    @Test fun `bulk route import writes each selected file`() = runTest {
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val firstUri = mockk<Uri>()
        val secondUri = mockk<Uri>()
        val start = Instant.parse("2024-01-01T10:00:00Z")
        coEvery { routeFileImporter.import(firstUri) } returns routeImport("morning-run.gpx", start)
        coEvery { routeFileImporter.import(secondUri) } returns routeImport("evening-walk.kml", start.plusSeconds(3600))
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true
        coEvery { activityRepository.writeActivityEntry(any()) } returnsMany listOf("first", "second")

        val vm = viewModel(
            repository = repo(grantedPermissions = setOf("write", "route")),
            activityRepository = activityRepository,
            preferencesRepository = prefs(),
            routeFileImporter = routeFileImporter,
        )

        vm.importRouteFiles(listOf(firstUri, secondUri))
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.routeImportResult?.importedFiles)
        assertEquals(0, vm.uiState.value.routeImportResult?.failedFiles)
        assertNull(vm.uiState.value.routeImportError)
        coVerify(exactly = 2) { activityRepository.writeActivityEntry(any()) }
    }

    private fun viewModel(
        repository: HealthRepository = repo(),
        activityRepository: ActivityRepository = activityRepo(),
        preferencesRepository: PreferencesRepository = prefs(),
        appleHealthImportService: AppleHealthImportService = importService(),
        appleHealthImportWorkController: AppleHealthImportWorkController = importController(),
        routeFileImporter: RouteFileImporter = routeFileImporter(),
        offlineMapRepository: OfflineMapRepository = offlineMapRepository(),
        offlineMapImportWorkController: OfflineMapImportWorkController = offlineMapImportController(),
        permissionUxState: HealthConnectPermissionUxState = permissionUxState(),
    ): SettingsViewModel =
        SettingsViewModel(
            repository = repository,
            activityRepository = activityRepository,
            preferencesRepository = preferencesRepository,
            appleHealthImportService = appleHealthImportService,
            appleHealthImportWorkController = appleHealthImportWorkController,
            routeFileImporter = routeFileImporter,
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

    private fun activityRepo(): ActivityRepository =
        mockk<ActivityRepository>().also { repo ->
            every { repo.activityWritePermissions() } returns setOf("write", "route")
        }

    private fun permissionUxState(): HealthConnectPermissionUxState =
        mockk<HealthConnectPermissionUxState>(relaxed = true)

    private fun prefs(): PreferencesRepository {
        var caffeinePreferences = CaffeinePreferences()
        return mockk<PreferencesRepository>().also { prefs ->
            every { prefs.unitSystem } returns UnitSystem.METRIC
            every { prefs.appLanguage } returns AppLanguage.SYSTEM
            every { prefs.appThemeMode } returns AppThemeMode.SYSTEM
            every { prefs.dynamicColor } returns false
            every { prefs.sleepRangeMode } returns SleepRangeMode.EVENING_18H
            every { prefs.activityWeekMode } returns ActivityWeekMode.MONDAY_TO_SUNDAY
            every { prefs.activityRecordingPreferences() } returns ActivityRecordingPreferences()
            every { prefs.showOpenVitalsCalculatedCalories } returns false
            every { prefs.favoriteActivityExerciseType } returns null
            every { prefs.lastActivityExerciseType } returns null
            every { prefs.bodyEnergyCalibration() } returns BodyEnergyCalibration.Automatic
            every { prefs.caffeinePreferences() } answers { caffeinePreferences }
            every { prefs.bodyProfile() } returns BodyProfile()
            every { prefs.healthConnectSyncEnabled } returns true
            every { prefs.appLockEnabled } returns false
            every { prefs.appLanguage = any() } just runs
            every { prefs.appThemeMode = any() } just runs
            every { prefs.dynamicColor = any() } just runs
            every { prefs.sleepRangeMode = any() } just runs
            every { prefs.activityWeekMode = any() } just runs
            every { prefs.setActivityRecordingPreferences(any()) } just runs
            every { prefs.showOpenVitalsCalculatedCalories = any() } just runs
            every { prefs.favoriteActivityExerciseType = any() } just runs
            every { prefs.lastActivityExerciseType = any() } just runs
            every { prefs.setBodyEnergyCalibration(any()) } just runs
            every { prefs.setCaffeinePreferences(any()) } answers {
                caffeinePreferences = firstArg<CaffeinePreferences>()
            }
        }
    }

    private fun routeFileImporter(): RouteFileImporter =
        mockk<RouteFileImporter>(relaxed = true)

    private fun routeImport(fileName: String, start: Instant): RouteFileImport =
        RouteFileImport(
            fileName = fileName,
            points = listOf(
                routePoint(start),
                routePoint(start.plusSeconds(30), latitude = 59.001, longitude = 24.001),
            ),
            distanceMeters = 120.0,
            elevationGainedMeters = 3.0,
            startTime = start,
            endTime = start.plusSeconds(60),
            name = fileName.substringBeforeLast('.'),
        )

    private fun routePoint(
        time: Instant,
        latitude: Double = 59.0,
        longitude: Double = 24.0,
    ): ExerciseRoutePoint =
        ExerciseRoutePoint(
            time = time,
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = 10.0,
            horizontalAccuracyMeters = null,
            verticalAccuracyMeters = null,
        )

    private fun importController(
        workInfos: MutableStateFlow<List<WorkInfo>>? = null,
    ): AppleHealthImportWorkController =
        mockk<AppleHealthImportWorkController>(relaxed = true).also { controller ->
            every { controller.workInfos } returns (workInfos ?: emptyFlow())
            every { controller.enqueue(any()) } returns UUID.randomUUID()
            every { controller.enqueue(any(), any(), any()) } returns UUID.randomUUID()
        }

    private fun importService(): AppleHealthImportService =
        mockk<AppleHealthImportService>().also { service ->
            coEvery { service.analyzeAppleHealthExport(any(), any()) } returns appleHealthAnalysis()
            coEvery { service.fingerprintOf(any()) } returns AppleHealthExportFingerprint(
                displayName = "export.zip",
                size = 1L,
            )
        }

    private fun appleHealthAnalysis(): AppleHealthImportAnalysisResult =
        AppleHealthImportAnalysisResult(
            parsedRecords = 1,
            parsedWorkouts = 0,
            parsedCorrelations = 0,
            parsedActivitySummaries = 0,
            convertedRecords = 1,
            unsupportedElements = 0,
            skippedRecords = 0,
            failedRecords = 0,
            categorySummaries = listOf(
                AppleHealthImportCategorySummary(
                    category = AppleHealthImportCategory.ACTIVITY,
                    convertedRecords = 1,
                ),
            ),
            typeSummaries = emptyList(),
            diagnostics = emptyList(),
            shareableReportText = "analysis",
        )

    private fun offlineMapRepository(): OfflineMapRepository =
        mockk<OfflineMapRepository>(relaxed = true).also { repository ->
            every { repository.state } returns MutableStateFlow(OfflineMapLibraryState())
        }

    private fun offlineMapImportController(): OfflineMapImportWorkController =
        mockk<OfflineMapImportWorkController>(relaxed = true).also { controller ->
            every { controller.workInfos } returns emptyFlow()
        }

    private fun workInfo(
        id: UUID = UUID.randomUUID(),
        state: WorkInfo.State,
    ): WorkInfo =
        mockk<WorkInfo>().also { workInfo ->
            every { workInfo.id } returns id
            every { workInfo.state } returns state
            every { workInfo.outputData } returns Data.EMPTY
            every { workInfo.progress } returns Data.EMPTY
        }
}
