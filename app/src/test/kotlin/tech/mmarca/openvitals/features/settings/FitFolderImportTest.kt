package tech.mmarca.openvitals.features.settings

import android.net.Uri
import android.util.Log
import androidx.work.WorkInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import java.io.FileNotFoundException
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.data.sync.StepDistanceBackfillService
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.domain.preferences.ChartAggregationMode
import tech.mmarca.openvitals.domain.preferences.NutritionAverageBasis
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.preferences.UnitSystemPreference
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportWorkController
import tech.mmarca.openvitals.features.activity.maps.OfflineMapLibraryState
import tech.mmarca.openvitals.features.activity.maps.OfflineMapRepository
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderController
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportService
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportWorkController
import tech.mmarca.openvitals.features.imports.garmin.FitHrvImportService
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFileImport
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFileImporter
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFolderFile
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFolderScan
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFolderScanner
import tech.mmarca.openvitals.healthconnect.HealthConnectPermissionUxState
import tech.mmarca.openvitals.util.MainDispatcherRule

/**
 * "Import a folder of FIT files". The folder walk ([RouteFolderScanner]) is mocked;
 * this pins the scan outcomes and that the files reach the bulk importer tagged as the FIT card's run.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FitFolderImportTest {
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

    @Test fun `imports every FIT file the folder held`() = runTest {
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val scanner = mockk<RouteFolderScanner>()
        val tree = mockk<Uri>()
        val files = folderFiles(3)
        coEvery { scanner.scan(tree, RouteFolderScanner.FitExtensions) } returns
            RouteFolderScan(files = files, truncated = false)
        stubImports(routeFileImporter, files)
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true
        coEvery { activityRepository.writeActivityEntries(any()) } returns listOf("id-0", "id-1", "id-2")

        val vm = viewModel(
            activityRepository = activityRepository,
            routeFileImporter = routeFileImporter,
            routeFolderScanner = scanner,
        )
        advanceUntilIdle()

        vm.importFitFolder(tree)
        advanceUntilIdle()

        val state = vm.uiState.value
        // Every file the scan listed went through the importer, in scan order.
        for (file in files) coVerify(exactly = 1) { routeFileImporter.import(file.uri) }
        assertEquals(3, state.routeImportResult?.totalFiles)
        assertEquals(3, state.routeImportResult?.importedFiles)
        assertEquals(0, state.routeImportResult?.failedFiles)
        assertNull(state.routeImportError)
        // The run belongs to the FIT card, not the route card.
        assertEquals(RouteBulkImportSource.FIT_FOLDER, state.routeImportSource)
        assertFalse(state.isScanningFitFolder)
        assertFalse(state.isImportingRouteFiles)
        // A folder that fit the scan reports no truncation and no emptiness.
        assertNull(state.fitFolderTruncatedAt)
        assertFalse(state.fitFolderHadNoFitFiles)
        assertNull(state.fitFolderScanError)
    }

    @Test fun `opens the files one at a time, not the whole folder at once`() = runTest {
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val scanner = mockk<RouteFolderScanner>()
        val tree = mockk<Uri>()
        val files = folderFiles(2)
        coEvery { scanner.scan(tree, any()) } returns RouteFolderScan(files = files, truncated = false)
        // The scan hands over URIs only; the importer opens each file as it is reached.
        val opened = mutableListOf<Uri>()
        files.forEachIndexed { index, file ->
            coEvery { routeFileImporter.import(file.uri) } answers {
                opened += file.uri
                routeImport(file.name, BaseStart.plusSeconds(index * 600L))
            }
        }
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true
        coEvery { activityRepository.writeActivityEntries(any()) } returns listOf("id-0", "id-1")

        val vm = viewModel(
            activityRepository = activityRepository,
            routeFileImporter = routeFileImporter,
            routeFolderScanner = scanner,
        )
        advanceUntilIdle()

        vm.importFitFolder(tree)
        advanceUntilIdle()

        assertEquals(files.map { it.uri }, opened)
        assertEquals(2, vm.uiState.value.routeImportResult?.importedFiles)
    }

    @Test fun `a folder with no FIT files says so, and is not an error`() = runTest {
        val routeFileImporter = routeFileImporter()
        val scanner = mockk<RouteFolderScanner>()
        val tree = mockk<Uri>()
        coEvery { scanner.scan(tree, any()) } returns RouteFolderScan(files = emptyList(), truncated = false)

        val vm = viewModel(routeFileImporter = routeFileImporter, routeFolderScanner = scanner)
        advanceUntilIdle()

        vm.importFitFolder(tree)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.fitFolderHadNoFitFiles)
        assertNull(state.fitFolderScanError)
        assertFalse(state.isScanningFitFolder)
        // Nothing to import means the bulk importer never ran.
        assertFalse(state.isImportingRouteFiles)
        assertNull(state.routeImportResult)
        coVerify(exactly = 0) { routeFileImporter.import(any()) }
    }

    @Test fun `a folder too big to list says how much of it was taken`() = runTest {
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val scanner = mockk<RouteFolderScanner>()
        val tree = mockk<Uri>()
        val files = folderFiles(4)
        coEvery { scanner.scan(tree, any()) } returns RouteFolderScan(files = files, truncated = true)
        stubImports(routeFileImporter, files)
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true
        coEvery { activityRepository.writeActivityEntries(any()) } returns List(4) { "id-$it" }

        val vm = viewModel(
            activityRepository = activityRepository,
            routeFileImporter = routeFileImporter,
            routeFolderScanner = scanner,
        )
        advanceUntilIdle()

        vm.importFitFolder(tree)
        advanceUntilIdle()

        val state = vm.uiState.value
        // Said out loud, with the number taken; a silent skip would look like a finished import.
        assertEquals(4, state.fitFolderTruncatedAt)
        assertEquals(4, state.routeImportResult?.importedFiles)
        assertFalse(state.fitFolderHadNoFitFiles)
    }

    @Test fun `one unreadable file fails that file, not the folder`() = runTest {
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val scanner = mockk<RouteFolderScanner>()
        val tree = mockk<Uri>()
        val files = folderFiles(3)
        coEvery { scanner.scan(tree, any()) } returns RouteFolderScan(files = files, truncated = false)
        stubImports(routeFileImporter, files)
        // A file the scan listed a moment ago has since been moved.
        coEvery { routeFileImporter.import(files[1].uri) } throws FileNotFoundException("gone")
        coEvery { routeFileImporter.importFitWellnessHrv(files[1].uri) } returns emptyList()
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true
        coEvery { activityRepository.writeActivityEntries(any()) } returns listOf("id-0", "id-2")

        val vm = viewModel(
            activityRepository = activityRepository,
            routeFileImporter = routeFileImporter,
            routeFolderScanner = scanner,
        )
        advanceUntilIdle()

        vm.importFitFolder(tree)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(3, state.routeImportResult?.totalFiles)
        assertEquals(2, state.routeImportResult?.importedFiles)
        assertEquals(1, state.routeImportResult?.failedFiles)
        assertEquals("gone", state.routeImportError)
        // The scan itself was fine; only the import surface carries the failure.
        assertNull(state.fitFolderScanError)
    }

    @Test fun `a failed scan surfaces, and imports nothing`() = runTest {
        val routeFileImporter = routeFileImporter()
        val scanner = mockk<RouteFolderScanner>()
        val tree = mockk<Uri>()
        coEvery { scanner.scan(tree, any()) } throws IllegalStateException("tree unreadable")

        val vm = viewModel(routeFileImporter = routeFileImporter, routeFolderScanner = scanner)
        advanceUntilIdle()

        vm.importFitFolder(tree)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("tree unreadable", state.fitFolderScanError)
        assertFalse(state.isScanningFitFolder)
        assertFalse(state.isImportingRouteFiles)
        assertNull(state.routeImportResult)
        coVerify(exactly = 0) { routeFileImporter.import(any()) }
    }

    @Test fun `a second pick while scanning is ignored`() = runTest {
        val routeFileImporter = routeFileImporter()
        val scanner = mockk<RouteFolderScanner>()
        val tree = mockk<Uri>()
        val other = mockk<Uri>()
        // The scan never completes, so the busy flag is observable.
        coEvery { scanner.scan(tree, any()) } coAnswers {
            kotlinx.coroutines.suspendCancellableCoroutine<RouteFolderScan> { }
        }

        val vm = viewModel(routeFileImporter = routeFileImporter, routeFolderScanner = scanner)
        advanceUntilIdle()

        vm.importFitFolder(tree)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isScanningFitFolder)

        vm.importFitFolder(other)
        vm.importRouteFiles(listOf(other))
        advanceUntilIdle()

        coVerify(exactly = 0) { scanner.scan(other, any()) }
        coVerify(exactly = 0) { routeFileImporter.import(any()) }
        assertFalse(vm.uiState.value.isImportingRouteFiles)
    }

    @Test fun `a fresh pick clears the outcome of the previous one`() = runTest {
        val routeFileImporter = routeFileImporter()
        val scanner = mockk<RouteFolderScanner>()
        val empty = mockk<Uri>()
        val broken = mockk<Uri>()
        coEvery { scanner.scan(empty, any()) } returns RouteFolderScan(files = emptyList(), truncated = false)
        coEvery { scanner.scan(broken, any()) } throws IllegalStateException("tree unreadable")

        val vm = viewModel(routeFileImporter = routeFileImporter, routeFolderScanner = scanner)
        advanceUntilIdle()

        vm.importFitFolder(empty)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.fitFolderHadNoFitFiles)

        vm.importFitFolder(broken)
        advanceUntilIdle()

        // The first pick's "no FIT files" line must not linger next to the second pick's error.
        val state = vm.uiState.value
        assertFalse(state.fitFolderHadNoFitFiles)
        assertEquals("tree unreadable", state.fitFolderScanError)
    }

    // Fixtures.

    private val BaseStart: Instant = Instant.parse("2026-06-01T08:00:00Z")

    private fun folderFiles(count: Int): List<RouteFolderFile> =
        List(count) { index -> RouteFolderFile(uri = mockk<Uri>(), name = "ride-$index.fit") }

    /** Every file parses into the same canned two-point route. */
    private fun stubImports(importer: RouteFileImporter, files: List<RouteFolderFile>) {
        files.forEachIndexed { index, file ->
            coEvery { importer.import(file.uri) } returns
                routeImport(file.name, BaseStart.plusSeconds(index * 600L))
        }
    }

    private fun viewModel(
        repository: HealthRepository = repo(),
        activityRepository: ActivityRepository = activityRepo(),
        preferencesRepository: PreferencesRepository = prefs(),
        routeFileImporter: RouteFileImporter = routeFileImporter(),
        fitHrvImportService: FitHrvImportService = mockk(relaxed = true),
        routeFolderScanner: RouteFolderScanner = mockk(relaxed = true),
    ): SettingsViewModel =
        SettingsViewModel(
            repository = repository,
            activityRepository = activityRepository,
            bodyRepository = bodyRepo(),
            heartRepository = heartRepo(),
            sleepRepository = sleepRepo(),
            hydrationReminderController = mockk<HydrationReminderController>(relaxed = true),
            preferencesRepository = preferencesRepository,
            stepDistanceBackfillService = mockk<StepDistanceBackfillService>(relaxed = true),
            appleHealthImportService = mockk<AppleHealthImportService>(relaxed = true),
            appleHealthImportWorkController = importController(),
            routeFileImporter = routeFileImporter,
            fitHrvImportService = fitHrvImportService,
            routeFolderScanner = routeFolderScanner,
            offlineMapRepository = offlineMapRepository(),
            offlineMapImportWorkController = offlineMapImportController(),
            permissionUxState = mockk<HealthConnectPermissionUxState>(relaxed = true),
            coMapsNavigationRepository = mockk(relaxed = true),
            derivedMetricsResetService = mockk(relaxed = true),
        )

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

    private fun activityRepo(): ActivityRepository =
        mockk<ActivityRepository>().also { repo ->
            every { repo.activityWritePermissions() } returns setOf("write", "route")
        }

    private fun repo(
        grantedPermissions: Set<String> = setOf("write", "route"),
    ): HealthRepository =
        mockk<HealthRepository>().also { repo ->
            every { repo.availability() } returns HealthConnectAvailability.AVAILABLE
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

    private fun bodyRepo(): BodyRepository =
        mockk<BodyRepository>().also { repo ->
            coEvery { repo.resolveBodyProfile(any()) } answers { firstArg() }
            coEvery { repo.hasBodyWritePermission(any()) } returns false
            coEvery { repo.writeBodyMeasurementEntry(any()) } returns "id"
        }

    private fun heartRepo(): HeartRepository =
        mockk<HeartRepository>().also { repo ->
            coEvery {
                repo.loadHeartRateSamples(any<java.time.LocalDate>(), any<java.time.LocalDate>())
            } returns emptyList()
        }

    private fun sleepRepo(): SleepRepository =
        mockk<SleepRepository>().also { repo ->
            coEvery { repo.loadSleepSessions(any(), any()) } returns emptyList()
        }

    private fun importController(): AppleHealthImportWorkController =
        mockk<AppleHealthImportWorkController>(relaxed = true).also { controller ->
            every { controller.workInfos } returns emptyFlow<List<WorkInfo>>()
            every { controller.enqueue(any()) } returns UUID.randomUUID()
            every { controller.enqueue(any(), any(), any(), any()) } returns UUID.randomUUID()
        }

    private fun offlineMapRepository(): OfflineMapRepository =
        mockk<OfflineMapRepository>(relaxed = true).also { repository ->
            every { repository.state } returns MutableStateFlow(OfflineMapLibraryState())
        }

    private fun offlineMapImportController(): OfflineMapImportWorkController =
        mockk<OfflineMapImportWorkController>(relaxed = true).also { controller ->
            every { controller.workInfos } returns emptyFlow()
        }

    private fun prefs(): PreferencesRepository =
        mockk<PreferencesRepository>().also { prefs ->
            every { prefs.unitSystemPreference } returns UnitSystemPreference.SYSTEM
            every { prefs.unitSystem } returns UnitSystem.METRIC
            every { prefs.unitOverridesFlow } returns MutableStateFlow(emptyMap())
            every { prefs.appLanguage } returns AppLanguage.SYSTEM
            every { prefs.appThemeMode } returns AppThemeMode.SYSTEM
            every { prefs.dynamicColor } returns false
            every { prefs.chartAggregationMode } returns ChartAggregationMode.OFF
            every { prefs.dashboardSortEmptyTilesLast } returns true
            every { prefs.stepDistanceBackfillEnabled } returns false
            every { prefs.strideLengthMeters } returns 0.7
            every { prefs.nightStartHour } returns SleepWindow.Default.startHour
            every { prefs.nightEndHour } returns SleepWindow.Default.endHour
            every { prefs.activityWeekMode } returns ActivityWeekMode.MONDAY_TO_SUNDAY
            every { prefs.activitySplitDistanceMeters } returns 1000.0
            every { prefs.activityRecordingPreferences() } returns ActivityRecordingPreferences()
            every { prefs.showOpenVitalsCalculatedCalories } returns false
            every { prefs.nutritionAverageBasis } returns NutritionAverageBasis.LOGGED_DAYS
            every { prefs.favoriteActivityExerciseType } returns null
            every { prefs.lastActivityExerciseType } returns null
            every { prefs.lastActivityExerciseType = any() } just runs
            every { prefs.bodyEnergyCalibration() } returns BodyEnergyCalibration.Automatic
            every { prefs.caffeinePreferences() } returns CaffeinePreferences()
            every { prefs.bodyProfile() } returns BodyProfile()
            every { prefs.healthConnectSyncEnabled } returns true
            every { prefs.healthConnectMindfulnessEnabled } returns false
            every { prefs.appLockEnabled } returns false
            every { prefs.highHeartRateThresholdBpm } returns
                PreferencesRepository.DEFAULT_HIGH_HEART_RATE_THRESHOLD_BPM
            every { prefs.lowHeartRateThresholdBpm } returns
                PreferencesRepository.DEFAULT_LOW_HEART_RATE_THRESHOLD_BPM
            every { prefs.hydrationDailyGoalLiters } returns
                PreferencesRepository.DEFAULT_HYDRATION_DAILY_GOAL_LITERS
        }
}
