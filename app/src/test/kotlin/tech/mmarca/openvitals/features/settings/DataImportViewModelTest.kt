package tech.mmarca.openvitals.features.settings

import android.net.Uri
import android.util.Log
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
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.sync.StepDistanceBackfillService
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.BloodPressureGuideline
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.domain.preferences.ChartAggregationMode
import tech.mmarca.openvitals.domain.preferences.HomeWidgetRefreshInterval
import tech.mmarca.openvitals.domain.preferences.NutritionAverageBasis
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.preferences.UnitSystemPreference
import tech.mmarca.openvitals.features.activity.maps.OfflineMapLibraryState
import tech.mmarca.openvitals.features.activity.maps.OfflineMapRepository
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthExportFingerprint
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportAnalysisResult
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportCategory
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportCategorySummary
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportService
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportWorkController
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportWorker
import tech.mmarca.openvitals.features.imports.garmin.FitHrvImportService
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFileImport
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFileImporter
import tech.mmarca.openvitals.features.settings.DataImportViewModel
import tech.mmarca.openvitals.healthconnect.HealthConnectPermissionUxState
import tech.mmarca.openvitals.util.MainDispatcherRule

/**
 * The Apple Health export: what a re-pick reuses, what a failure says, and which work run
 * the observer believes. Moved here with the import workflows.
 */
class DataImportViewModelTest {

    @Test fun `apple import observer ignores stale finished failures without current work`() = runTest {
        val staleFailure = workInfo(state = WorkInfo.State.FAILED)
        val importController = importController(
            workInfos = MutableStateFlow(listOf(staleFailure)),
        )

        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs(),
            stepDistanceBackfillService = mockk<StepDistanceBackfillService>(relaxed = true),
            appleHealthImportWorkController = importController,
            permissionUxState = permissionUxState(),
        )
        advanceUntilIdle()

        assertNull(vm.uiState.value.appleHealthImportError)
        coVerify(exactly = 0) { importController.errorFor(staleFailure) }
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
                expectedParsedElements = 1,
            )
        } returns currentWorkId
        coEvery { importController.errorFor(currentFailure) } returns "current failure"

        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs(),
            stepDistanceBackfillService = mockk<StepDistanceBackfillService>(relaxed = true),
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
        coVerify(exactly = 0) { importController.errorFor(staleFailure) }
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
            stepDistanceBackfillService = mockk<StepDistanceBackfillService>(relaxed = true),
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

        coVerify(exactly = 1) { importService.analyzeStagedAppleHealthExport(any(), any(), any()) }
        assertEquals(firstAnalysis, vm.uiState.value.appleHealthImportAnalysis)
        assertEquals(setOf(AppleHealthImportCategory.ACTIVITY), vm.uiState.value.selectedAppleHealthImportCategories)
        verify { importController.persistReadPermission(firstUri) }
        verify { importController.persistReadPermission(secondUri) }
    }

    @Test fun `a failed analysis reports the error and forgets the staged pick`() = runTest {
        val importService = importService()
        val importController = importController()
        coEvery {
            importService.analyzeStagedAppleHealthExport(any(), any(), any())
        } throws SecurityException("no read access")

        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs(),
            stepDistanceBackfillService = mockk<StepDistanceBackfillService>(relaxed = true),
            appleHealthImportService = importService,
            appleHealthImportWorkController = importController,
            permissionUxState = permissionUxState(),
        )

        vm.analyzeAppleHealthExport(mockk<Uri>())
        advanceUntilIdle()

        assertNull(vm.uiState.value.appleHealthImportAnalysis)
        assertTrue(vm.uiState.value.appleHealthImportError!!.contains("no read access"))
        assertTrue(vm.uiState.value.appleHealthImportPermissionDenied)
        assertFalse(vm.uiState.value.isAnalyzingAppleHealth)

        // The pending source is gone, so importing cannot reuse a bad staged copy.
        vm.importSelectedAppleHealthExport()
        advanceUntilIdle()

        assertNull(vm.uiState.value.appleHealthImportResult)
        verify(exactly = 0) { importController.enqueue(any(), any(), any(), any()) }
    }

    @Test fun `importing without an analysis does nothing`() = runTest {
        val importController = importController()
        val vm = viewModel(
            repository = repo(),
            preferencesRepository = prefs(),
            stepDistanceBackfillService = mockk<StepDistanceBackfillService>(relaxed = true),
            appleHealthImportService = importService(),
            appleHealthImportWorkController = importController,
            permissionUxState = permissionUxState(),
        )
        advanceUntilIdle()
        // Without these the comparison below would pass vacuously on isBusy.
        assertFalse(vm.uiState.value.isAnalyzingAppleHealth)
        assertFalse(vm.uiState.value.isImportingAppleHealth)
        assertNull(vm.uiState.value.appleHealthImportAnalysis)
        val before = vm.uiState.value

        vm.importSelectedAppleHealthExport()
        advanceUntilIdle()

        // Asserting nothing changed.
        assertEquals(before, vm.uiState.value)
        verify(exactly = 0) { importController.enqueue(any(), any(), any(), any()) }
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
            stepDistanceBackfillService = mockk<StepDistanceBackfillService>(relaxed = true),
            appleHealthImportService = importService,
            appleHealthImportWorkController = importController(),
            permissionUxState = permissionUxState(),
        )

        vm.analyzeAppleHealthExport(firstUri)
        advanceUntilIdle()
        vm.analyzeAppleHealthExport(secondUri)
        advanceUntilIdle()

        coVerify(exactly = 2) { importService.analyzeStagedAppleHealthExport(any(), any(), any()) }
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    /** Eight dependencies, where the settings view model needed twenty-one. */
    private fun viewModel(
        repository: HealthRepository = repo(),
        activityRepository: ActivityRepository = activityRepo(),
        preferencesRepository: PreferencesRepository = prefs(),
        appleHealthImportService: AppleHealthImportService = importService(),
        appleHealthImportWorkController: AppleHealthImportWorkController = importController(),
        routeFileImporter: RouteFileImporter = routeFileImporter(),
        fitHrvImportService: FitHrvImportService = mockk(relaxed = true),
        @Suppress("UNUSED_PARAMETER") stepDistanceBackfillService: Any? = null,
        @Suppress("UNUSED_PARAMETER") permissionUxState: Any? = null,
    ) = DataImportViewModel(
        repository = repository,
        activityRepository = activityRepository,
        recordingPreferences = preferencesRepository,
        unitPreferences = preferencesRepository,
        appleHealthImportService = appleHealthImportService,
        appleHealthImportWorkController = appleHealthImportWorkController,
        routeFileImporter = routeFileImporter,
        fitHrvImportService = fitHrvImportService,
        routeFolderScanner = mockk(relaxed = true),
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

    private fun prefs(): PreferencesRepository {
        var caffeinePreferences = CaffeinePreferences()
        return mockk<PreferencesRepository>().also { prefs ->
            every { prefs.unitSystemPreference } returns UnitSystemPreference.SYSTEM
            every { prefs.unitSystemPreference = any() } just runs
            every { prefs.unitSystem } returns UnitSystem.METRIC
            every { prefs.elevationCorrectionEnabled } returns true
            every { prefs.elevationCorrectionEnabled = any() } just runs
            var unitOverrides = mapOf<UnitQuantity, UnitSystem>()
            every { prefs.unitOverridesFlow } answers { MutableStateFlow(unitOverrides) }
            every { prefs.unitOverride(any()) } answers { unitOverrides[firstArg()] }
            every { prefs.setUnitOverride(any(), any()) } answers {
                unitOverrides = unitOverrides + (firstArg<UnitQuantity>() to secondArg<UnitSystem>())
            }
            every { prefs.setUnitOverride(any(), isNull()) } answers {
                unitOverrides = unitOverrides - firstArg<UnitQuantity>()
            }
            every { prefs.appLanguage } returns AppLanguage.SYSTEM
            every { prefs.appThemeMode } returns AppThemeMode.SYSTEM
            every { prefs.dynamicColor } returns false
            every { prefs.chartAggregationMode } returns ChartAggregationMode.OFF
            every { prefs.bloodPressureGuideline } returns BloodPressureGuideline.ACC_AHA_2017
            every { prefs.homeWidgetRefreshInterval } returns HomeWidgetRefreshInterval.DEFAULT
            every { prefs.dashboardSortEmptyTilesLast } returns true
            every { prefs.stepDistanceBackfillEnabled } returns false
            every { prefs.strideLengthMeters } returns 0.7
            every { prefs.nightStartHour } returns SleepWindow.Default.startHour
            every { prefs.nightEndHour } returns SleepWindow.Default.endHour
            every { prefs.activityWeekMode } returns ActivityWeekMode.MONDAY_TO_SUNDAY
            every { prefs.activitySplitDistanceMeters } returns 1000.0
            every { prefs.activityRecordingPreferences() } returns ActivityRecordingPreferences()
            every { prefs.showOpenVitalsCalculatedCalories } returns false
            var nutritionAverageBasis = NutritionAverageBasis.LOGGED_DAYS
            every { prefs.nutritionAverageBasis } answers { nutritionAverageBasis }
            every { prefs.nutritionAverageBasis = any() } answers { nutritionAverageBasis = firstArg() }
            every { prefs.favoriteActivityExerciseType } returns null
            every { prefs.lastActivityExerciseType } returns null
            every { prefs.bodyEnergyCalibration() } returns BodyEnergyCalibration.Automatic
            every { prefs.caffeinePreferences() } answers { caffeinePreferences }
            every { prefs.bodyProfile() } returns BodyProfile()
            every { prefs.healthConnectSyncEnabled } returns true
            var mindfulnessEnabled = false
            every { prefs.healthConnectMindfulnessEnabled } answers { mindfulnessEnabled }
            every { prefs.appLockEnabled } returns false
            var highThreshold = PreferencesRepository.DEFAULT_HIGH_HEART_RATE_THRESHOLD_BPM
            var lowThreshold = PreferencesRepository.DEFAULT_LOW_HEART_RATE_THRESHOLD_BPM
            var hydrationGoal = PreferencesRepository.DEFAULT_HYDRATION_DAILY_GOAL_LITERS
            every { prefs.highHeartRateThresholdBpm } answers { highThreshold }
            every { prefs.highHeartRateThresholdBpm = any() } answers {
                highThreshold = firstArg<Int>().coerceIn(80, 220)
            }
            every { prefs.lowHeartRateThresholdBpm } answers { lowThreshold }
            every { prefs.lowHeartRateThresholdBpm = any() } answers {
                lowThreshold = firstArg<Int>().coerceIn(30, 100)
            }
            every { prefs.hydrationDailyGoalLiters } answers { hydrationGoal }
            every { prefs.hydrationDailyGoalLiters = any() } answers {
                hydrationGoal = firstArg<Double>().coerceIn(0.25, 10.0)
            }
            every { prefs.healthConnectMindfulnessEnabled = any() } answers {
                mindfulnessEnabled = firstArg()
            }
            every { prefs.setBodyProfile(any()) } just runs
            every { prefs.appLanguage = any() } just runs
            every { prefs.appThemeMode = any() } just runs
            every { prefs.dynamicColor = any() } just runs
            every { prefs.chartAggregationMode = any() } just runs
            every { prefs.bloodPressureGuideline = any() } just runs
            every { prefs.nightStartHour = any() } just runs
            every { prefs.nightEndHour = any() } just runs
            every { prefs.activityWeekMode = any() } just runs
            every { prefs.activitySplitDistanceMeters = any() } just runs
            every { prefs.setActivityRecordingPreferences(any()) } just runs
            every { prefs.showOpenVitalsCalculatedCalories = any() } just runs
            every { prefs.favoriteActivityExerciseType = any() } just runs
            every { prefs.lastActivityExerciseType = any() } just runs
            every { prefs.setBodyEnergyCalibration(any()) } just runs
            // The real repository normalizes on write, so the fake must too.
            every { prefs.setCaffeinePreferences(any()) } answers {
                caffeinePreferences = firstArg<CaffeinePreferences>().normalized()
            }
        }
    }

    private fun importController(
        workInfos: MutableStateFlow<List<WorkInfo>>? = null,
    ): AppleHealthImportWorkController =
        mockk<AppleHealthImportWorkController>(relaxed = true).also { controller ->
            every { controller.workInfos } returns (workInfos ?: emptyFlow())
            every { controller.enqueue(any()) } returns UUID.randomUUID()
            every { controller.enqueue(any(), any(), any(), any()) } returns UUID.randomUUID()
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

    private fun importService(): AppleHealthImportService =
        mockk<AppleHealthImportService>().also { service ->
            coEvery { service.analyzeStagedAppleHealthExport(any(), any(), any()) } returns appleHealthAnalysis()
            coEvery { service.fingerprintOf(any()) } returns AppleHealthExportFingerprint(
                displayName = "export.zip",
                size = 1L,
            )
        }

    private fun permissionUxState(): HealthConnectPermissionUxState =
        mockk<HealthConnectPermissionUxState>(relaxed = true)


    private fun activityRepo(): ActivityRepository =
        mockk<ActivityRepository>().also { repo ->
            every { repo.activityWritePermissions() } returns setOf("write", "route")
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

    @Test fun `bulk route import writes all selected files in one batched call`() = runTest {
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val firstUri = mockk<Uri>()
        val secondUri = mockk<Uri>()
        val start = Instant.parse("2024-01-01T10:00:00Z")
        coEvery { routeFileImporter.import(firstUri) } returns routeImport("morning-run.gpx", start)
        coEvery { routeFileImporter.import(secondUri) } returns routeImport("evening-walk.kml", start.plusSeconds(3600))
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true
        coEvery { activityRepository.writeActivityEntries(any()) } returns listOf("first", "second")

        val vm = viewModel(
            repository = repo(grantedPermissions = setOf("write", "route")),
            activityRepository = activityRepository,
            preferencesRepository = prefs(),
            stepDistanceBackfillService = mockk<StepDistanceBackfillService>(relaxed = true),
            routeFileImporter = routeFileImporter,
        )

        vm.importRouteFiles(listOf(firstUri, secondUri))
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.routeImportResult?.importedFiles)
        assertEquals(0, vm.uiState.value.routeImportResult?.failedFiles)
        assertNull(vm.uiState.value.routeImportError)
        coVerify(exactly = 1) { activityRepository.writeActivityEntries(match { it.size == 2 }) }
        coVerify(exactly = 0) { activityRepository.writeActivityEntry(any()) }
    }

    @Test fun `a failed batch retries file by file so only the guilty file fails`() = runTest {
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val firstUri = mockk<Uri>()
        val secondUri = mockk<Uri>()
        val start = Instant.parse("2024-01-01T10:00:00Z")
        coEvery { routeFileImporter.import(firstUri) } returns routeImport("good.gpx", start)
        coEvery { routeFileImporter.import(secondUri) } returns routeImport("bad.gpx", start.plusSeconds(3600))
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true
        coEvery { activityRepository.writeActivityEntries(any()) } throws IllegalStateException("batch failed")
        coEvery { activityRepository.writeActivityEntry(any()) } returns "first" andThenThrows
            IllegalStateException("record invalid")

        val vm = viewModel(
            repository = repo(grantedPermissions = setOf("write", "route")),
            activityRepository = activityRepository,
            preferencesRepository = prefs(),
            stepDistanceBackfillService = mockk<StepDistanceBackfillService>(relaxed = true),
            routeFileImporter = routeFileImporter,
        )

        vm.importRouteFiles(listOf(firstUri, secondUri))
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.routeImportResult?.importedFiles)
        assertEquals(1, vm.uiState.value.routeImportResult?.failedFiles)
        coVerify(exactly = 2) { activityRepository.writeActivityEntry(any()) }
    }

    @Test fun `a rate-limited batch stops the run without blaming the files`() = runTest {
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val firstUri = mockk<Uri>()
        val start = Instant.parse("2024-01-01T10:00:00Z")
        coEvery { routeFileImporter.import(firstUri) } returns routeImport("run.gpx", start)
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true
        coEvery { activityRepository.writeActivityEntries(any()) } throws
            IllegalStateException("Quota has been exceeded")

        val vm = viewModel(
            repository = repo(grantedPermissions = setOf("write", "route")),
            activityRepository = activityRepository,
            preferencesRepository = prefs(),
            stepDistanceBackfillService = mockk<StepDistanceBackfillService>(relaxed = true),
            routeFileImporter = routeFileImporter,
        )

        vm.importRouteFiles(listOf(firstUri))
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.routeImportResult?.importedFiles)
        assertEquals(0, vm.uiState.value.routeImportResult?.failedFiles)
        assertTrue(vm.uiState.value.routeImportError != null)
        coVerify(exactly = 0) { activityRepository.writeActivityEntry(any()) }
    }

    @Test fun `a wellness FIT that is not an activity falls back to the HRV import`() = runTest {
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val fitHrvImportService = mockk<FitHrvImportService>()
        val wellnessUri = mockk<Uri>()
        val reading = tech.mmarca.openvitals.features.manualentry.activity.routeimport.FitHrvReading(
            time = Instant.parse("2024-01-01T02:00:00Z"),
            rmssdMillis = 62.5,
        )
        coEvery { routeFileImporter.import(wellnessUri) } throws
            IllegalArgumentException("FIT file does not contain an activity session or timestamped activity records.")
        coEvery { routeFileImporter.importFitWellnessHrv(wellnessUri) } returns listOf(reading)
        coEvery { fitHrvImportService.writeFiles(listOf(listOf(reading))) } returns
            tech.mmarca.openvitals.features.imports.garmin.FitHrvImportOutcome(importedFiles = 1)

        val vm = viewModel(
            repository = repo(grantedPermissions = setOf("write", "route")),
            activityRepository = activityRepository,
            preferencesRepository = prefs(),
            stepDistanceBackfillService = mockk<StepDistanceBackfillService>(relaxed = true),
            routeFileImporter = routeFileImporter,
            fitHrvImportService = fitHrvImportService,
        )

        vm.importRouteFiles(listOf(wellnessUri))
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.routeImportResult?.importedFiles)
        assertEquals(0, vm.uiState.value.routeImportResult?.failedFiles)
        assertNull(vm.uiState.value.routeImportError)
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

}
