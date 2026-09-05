package tech.mmarca.openvitals.features.imports

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
import io.mockk.slot
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.sync.StepDistanceBackfillService
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
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
import tech.mmarca.openvitals.features.settings.RouteBulkImportProgress
import tech.mmarca.openvitals.features.settings.SettingsViewModel
import tech.mmarca.openvitals.healthconnect.HealthConnectPermissionUxState
import tech.mmarca.openvitals.util.MainDispatcherRule

/**
 * Bulk route/activity file import, the Kotlin counterpart of Flutter's
 * `test/features/imports/route_bulk_import_view_model_test.dart`.
 *
 * The behaviour lives in [SettingsViewModel.importRouteFiles] rather than in a
 * dedicated view model, so this file drives a [SettingsViewModel] but only ever
 * asserts on the bulk-import surface (`routeImportProgress`, `routeImportResult`,
 * `routeImportError`) and on what reaches the repository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RouteBulkImportTest {

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

    @Test fun `progress counts the files as they land`() = runTest {
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val first = mockk<Uri>()
        val broken = mockk<Uri>()
        val last = mockk<Uri>()
        // Snapshotted as each file is REACHED. `uiState` is a conflated
        // StateFlow, so a collector would only ever see the last value of a run
        // that never suspends; reading it from inside the importer is what pins
        // the per-file tick.
        val progress = mutableListOf<RouteBulkImportProgress?>()
        lateinit var vm: SettingsViewModel
        coEvery { routeFileImporter.import(first) } answers {
            progress += vm.uiState.value.routeImportProgress
            routeImport("a.gpx", BaseStart)
        }
        coEvery { routeFileImporter.import(broken) } answers {
            progress += vm.uiState.value.routeImportProgress
            throw IllegalArgumentException("bad file")
        }
        coEvery { routeFileImporter.importFitWellnessHrv(broken) } returns emptyList()
        coEvery { routeFileImporter.import(last) } answers {
            progress += vm.uiState.value.routeImportProgress
            routeImport("c.gpx", BaseStart.plusSeconds(3600))
        }
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true
        coEvery { activityRepository.writeActivityEntries(any()) } returns listOf("id-0", "id-1")

        vm = viewModel(
            activityRepository = activityRepository,
            routeFileImporter = routeFileImporter,
        )
        advanceUntilIdle()

        vm.importRouteFiles(listOf(first, broken, last))
        advanceUntilIdle()

        // One snapshot per file, each already knowing the total and naming the
        // file being worked on (1-based). Failures are counted as they land;
        // the imported COUNT lands with the BATCH, not with the file, because
        // the files are written together — so it is still 0 on the last tick.
        assertEquals(
            listOf(
                RouteBulkImportProgress(totalFiles = 3, currentFileIndex = 1),
                RouteBulkImportProgress(totalFiles = 3, currentFileIndex = 2),
                RouteBulkImportProgress(totalFiles = 3, failedFiles = 1, currentFileIndex = 3),
            ),
            progress,
        )
        // The finished state drops the progress and publishes the result.
        assertNull(vm.uiState.value.routeImportProgress)
        assertFalse(vm.uiState.value.isImportingRouteFiles)
        assertEquals(2, vm.uiState.value.routeImportResult?.importedFiles)
        assertEquals(1, vm.uiState.value.routeImportResult?.failedFiles)
    }

    @Test fun `a refused write permission fails one file, not the batch`() = runTest {
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val uris = uris(2)
        stubImports(routeFileImporter, uris)
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns false

        val vm = viewModel(
            activityRepository = activityRepository,
            routeFileImporter = routeFileImporter,
        )

        vm.importRouteFiles(uris)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2, state.routeImportResult?.totalFiles)
        assertEquals(0, state.routeImportResult?.importedFiles)
        assertEquals(2, state.routeImportResult?.failedFiles)
        assertEquals("Activity import write permissions are missing.", state.routeImportError)
        // Nothing was ever offered to Health Connect.
        coVerify(exactly = 0) { activityRepository.writeActivityEntries(any()) }
        coVerify(exactly = 0) { activityRepository.writeActivityEntry(any()) }
    }

    @Test fun `a failed write surfaces the failure message`() = runTest {
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val uris = uris(1)
        stubImports(routeFileImporter, uris)
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true
        // Writes fail, batched and singly alike — otherwise the file-by-file
        // retry would quietly rescue the run the test is about.
        coEvery { activityRepository.writeActivityEntries(any()) } throws
            IllegalStateException("Health Connect said no")
        coEvery { activityRepository.writeActivityEntry(any()) } throws
            IllegalStateException("Health Connect said no")

        val vm = viewModel(
            activityRepository = activityRepository,
            routeFileImporter = routeFileImporter,
        )

        vm.importRouteFiles(uris)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, state.routeImportResult?.failedFiles)
        assertEquals(0, state.routeImportResult?.importedFiles)
        assertEquals("Health Connect said no", state.routeImportError)
    }

    @Test fun `a malformed file is tolerated and its parse error reported`() = runTest {
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val good = mockk<Uri>()
        val bad = mockk<Uri>()
        coEvery { routeFileImporter.import(good) } returns routeImport("good.gpx", BaseStart)
        coEvery { routeFileImporter.import(bad) } throws IllegalArgumentException("bad file")
        // Not a Garmin wellness FIT either, so the fallback finds nothing.
        coEvery { routeFileImporter.importFitWellnessHrv(bad) } returns emptyList()
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true
        val batch = slot<List<ActivityWriteRequest>>()
        coEvery { activityRepository.writeActivityEntries(capture(batch)) } returns listOf("id-0")

        val vm = viewModel(
            activityRepository = activityRepository,
            routeFileImporter = routeFileImporter,
        )

        vm.importRouteFiles(listOf(good, bad))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, batch.captured.size)
        assertEquals(1, state.routeImportResult?.importedFiles)
        assertEquals(1, state.routeImportResult?.failedFiles)
        assertEquals("bad file", state.routeImportError)
    }

    @Test fun `opens files as it reaches them, never the whole folder up front`() = runTest {
        // The memory contract of a FOLDER import. Activities are written in
        // batches — Health Connect charges its quota per call, so writing one
        // file at a time exhausts it — which means the BATCH is what has to fit
        // in memory, not the folder. The contract survives only because the
        // batch is bounded: the importer must never read a whole folder before
        // writing anything, or a big folder OOMs before the first import.
        val events = mutableListOf<String>()
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        // Comfortably more files than one batch holds.
        val uris = uris(60)
        uris.forEachIndexed { index, uri ->
            coEvery { routeFileImporter.import(uri) } answers {
                events += "read:f$index.fit"
                routeImport("f$index.fit", BaseStart.plusSeconds(index * 600L))
            }
        }
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true
        coEvery { activityRepository.writeActivityEntries(any()) } answers {
            events += "writeBatch"
            firstArg<List<ActivityWriteRequest>>().indices.map { "id-$it" }
        }

        val vm = viewModel(
            activityRepository = activityRepository,
            routeFileImporter = routeFileImporter,
        )

        vm.importRouteFiles(uris)
        advanceUntilIdle()

        // Something was WRITTEN before the last file was ever OPENED. That is
        // the whole guarantee: reads are bounded by the batch, not by the size
        // of the folder.
        val firstWrite = events.indexOf("writeBatch")
        val lastRead = events.lastIndexOf("read:f59.fit")
        assertTrue(firstWrite >= 0)
        assertTrue("the whole folder was read before anything was written", firstWrite < lastRead)
        // And every file still got opened exactly once, in order.
        assertEquals(60, events.count { it.startsWith("read:") })
        assertEquals(
            (0 until 60).map { "read:f$it.fit" },
            events.filter { it.startsWith("read:") },
        )
    }

    @Test fun `writes activities in batches, not one Health Connect call per file`() = runTest {
        // The reason batching exists. Health Connect charges its API-call quota
        // PER CALL, not per record, so a call per file spends a unit of quota
        // per file and a folder of a couple of thousand dies partway through on
        // "API call quota exceeded". This asserts the CALL COUNT, because the
        // call count IS the quota bill.
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val uris = uris(60)
        stubImports(routeFileImporter, uris)
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true
        val batches = mutableListOf<List<ActivityWriteRequest>>()
        coEvery { activityRepository.writeActivityEntries(any()) } answers {
            val requests = firstArg<List<ActivityWriteRequest>>()
            batches += requests.toList()
            requests.indices.map { "id-$it" }
        }

        val vm = viewModel(
            activityRepository = activityRepository,
            routeFileImporter = routeFileImporter,
        )

        vm.importRouteFiles(uris)
        advanceUntilIdle()

        // 60 files, 25 per batch: three calls, not sixty.
        assertEquals(3, batches.size)
        assertEquals(listOf(25, 25, 10), batches.map { it.size })
        assertEquals(60, batches.sumOf { it.size })
        assertEquals(60, vm.uiState.value.routeImportResult?.importedFiles)
        assertEquals(0, vm.uiState.value.routeImportResult?.failedFiles)
        coVerify(exactly = 0) { activityRepository.writeActivityEntry(any()) }
    }

    @Test fun `a spent Health Connect quota stops the run instead of failing every file`() = runTest {
        // The bug this was written for: when the quota runs out mid-import,
        // every REMAINING file fails for the same reason. Treating that as "one
        // bad file and carry on" marched through the rest of the folder and
        // reported hundreds of perfectly good files as failures. The data is
        // fine and the quota refills, so the run stops and says so.
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val uris = uris(60)
        stubImports(routeFileImporter, uris)
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true
        val batches = mutableListOf<List<ActivityWriteRequest>>()
        coEvery { activityRepository.writeActivityEntries(any()) } answers {
            batches += firstArg<List<ActivityWriteRequest>>().toList()
            throw IllegalStateException("API call quota has been exceeded")
        }

        val vm = viewModel(
            activityRepository = activityRepository,
            routeFileImporter = routeFileImporter,
        )

        vm.importRouteFiles(uris)
        advanceUntilIdle()

        val state = vm.uiState.value
        // Stopped at the first refusal: one batch attempted, and NOT retried
        // file by file (a quota refusal is not a bad record — retrying singly
        // would only spend more of a quota that is already gone).
        assertEquals(1, batches.size)
        coVerify(exactly = 0) { activityRepository.writeActivityEntry(any()) }
        // ...and the remaining files were never even opened.
        coVerify(exactly = 25) { routeFileImporter.import(any()) }
        // Crucially: nothing is blamed on the files.
        assertEquals(0, state.routeImportResult?.failedFiles)
        assertEquals(0, state.routeImportResult?.importedFiles)
        // The stop still surfaces, even with zero failed files.
        assertEquals("API call quota has been exceeded", state.routeImportError)
    }

    @Test fun `a file that cannot be opened fails that file, not the batch`() = runTest {
        // A folder scanned a minute ago can name a file that has since moved.
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val gone = mockk<Uri>()
        val here = mockk<Uri>()
        coEvery { routeFileImporter.import(gone) } throws FileNotFoundException("no such file")
        coEvery { routeFileImporter.importFitWellnessHrv(gone) } returns emptyList()
        coEvery { routeFileImporter.import(here) } returns routeImport("here.fit", BaseStart)
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true
        val batch = slot<List<ActivityWriteRequest>>()
        coEvery { activityRepository.writeActivityEntries(capture(batch)) } returns listOf("id-0")

        val vm = viewModel(
            activityRepository = activityRepository,
            routeFileImporter = routeFileImporter,
        )

        vm.importRouteFiles(listOf(gone, here))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, batch.captured.size)
        assertEquals(1, state.routeImportResult?.importedFiles)
        assertEquals(1, state.routeImportResult?.failedFiles)
        assertNotNull(state.routeImportError)
    }

    @Test fun `an activity file is imported as an activity, not skipped as wellness`() = runTest {
        // Regression: a Garmin writes VO2 max and recovery time INTO the
        // activity it just recorded. Once those messages were parsed, the file
        // started yielding wellness data, the importer branched on that rather
        // than on the file type, and a real workout was silently skipped
        // instead of imported. In the Kotlin port the wellness fallback is only
        // reachable from the FAILURE branch, so the guarantee is that a file
        // that parses as an activity never consults it at all.
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val fitHrvImportService = mockk<FitHrvImportService>(relaxed = true)
        val activityFit = mockk<Uri>()
        coEvery { routeFileImporter.import(activityFit) } returns
            routeImport("activity_120.fit", BaseStart)
        coEvery { activityRepository.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns true
        val batch = slot<List<ActivityWriteRequest>>()
        coEvery { activityRepository.writeActivityEntries(capture(batch)) } returns listOf("id-0")

        val vm = viewModel(
            activityRepository = activityRepository,
            routeFileImporter = routeFileImporter,
            fitHrvImportService = fitHrvImportService,
        )

        vm.importRouteFiles(listOf(activityFit))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, batch.captured.size)
        assertEquals(1, state.routeImportResult?.importedFiles)
        assertEquals(0, state.routeImportResult?.failedFiles)
        assertNull(state.routeImportError)
        // The workout was never diverted down the wellness path.
        coVerify(exactly = 0) { routeFileImporter.importFitWellnessHrv(any()) }
        coVerify(exactly = 0) { fitHrvImportService.writeFiles(any()) }
    }

    @Test fun `an empty pick does nothing`() = runTest {
        val activityRepository = activityRepo()
        val routeFileImporter = routeFileImporter()
        val vm = viewModel(
            activityRepository = activityRepository,
            routeFileImporter = routeFileImporter,
        )
        advanceUntilIdle()

        vm.importRouteFiles(emptyList())
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isImportingRouteFiles)
        assertNull(state.routeImportProgress)
        assertNull(state.routeImportResult)
        assertNull(state.routeImportError)
        coVerify(exactly = 0) { routeFileImporter.import(any()) }
        coVerify(exactly = 0) { activityRepository.writeActivityEntries(any()) }
    }

    // --- fixtures -----------------------------------------------------------

    private val BaseStart: Instant = Instant.parse("2026-06-01T08:00:00Z")

    private fun uris(count: Int): List<Uri> = List(count) { mockk<Uri>() }

    /** Every file parses into the same canned two-point route. */
    private fun stubImports(importer: RouteFileImporter, uris: List<Uri>) {
        uris.forEachIndexed { index, uri ->
            coEvery { importer.import(uri) } returns
                routeImport("f$index.gpx", BaseStart.plusSeconds(index * 600L))
        }
    }

    private fun viewModel(
        repository: HealthRepository = repo(),
        activityRepository: ActivityRepository = activityRepo(),
        preferencesRepository: PreferencesRepository = prefs(),
        routeFileImporter: RouteFileImporter = routeFileImporter(),
        fitHrvImportService: FitHrvImportService = mockk(relaxed = true),
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
            routeFolderScanner = mockk(relaxed = true),
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
