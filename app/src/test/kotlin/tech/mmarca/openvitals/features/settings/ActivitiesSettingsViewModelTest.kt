package tech.mmarca.openvitals.features.settings

import android.net.Uri
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.geo.HgtResolution
import tech.mmarca.openvitals.core.geo.HgtTileKey
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.sync.StepDistanceBackfillService
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences
import tech.mmarca.openvitals.domain.preferences.StrideLength
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.activity.elevation.ElevationTile
import tech.mmarca.openvitals.features.activity.elevation.ElevationTileLibraryState
import tech.mmarca.openvitals.features.activity.elevation.ElevationTileRepository
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportWorkController
import tech.mmarca.openvitals.features.activity.maps.OfflineMapLibraryState
import tech.mmarca.openvitals.features.activity.maps.OfflineMapRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ActivitiesSettingsViewModelTest {

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

    @Test fun `saving the step distance backfill normalizes and kicks a sync`() = runTest {
        val prefs = prefs()
        val service = mockk<StepDistanceBackfillService>(relaxed = true)
        val vm = viewModel(preferencesRepository = prefs, stepDistanceBackfillService = service)

        vm.saveStepDistanceBackfill(enabled = true, strideMeters = 9.0)
        advanceUntilIdle()

        verify { prefs.strideLengthMeters = StrideLength.maxMeters }
        verify { prefs.stepDistanceBackfillEnabled = true }
        coVerify(exactly = 1) { service.syncNow() }
        coVerify(exactly = 0) { service.purgeDerivedRecords() }
        assertEquals(StrideLength.maxMeters, vm.uiState.value.strideLengthMeters, 0.0)
    }

    @Test fun `disabling the step distance backfill purges derived records`() = runTest {
        val prefs = prefs()
        every { prefs.stepDistanceBackfillEnabled } returns true
        val service = mockk<StepDistanceBackfillService>(relaxed = true)
        val vm = viewModel(preferencesRepository = prefs, stepDistanceBackfillService = service)

        vm.saveStepDistanceBackfill(enabled = false, strideMeters = 0.7)
        advanceUntilIdle()

        coVerify(exactly = 1) { service.purgeDerivedRecords() }
        coVerify(exactly = 0) { service.syncNow() }
    }

    @Test fun `updateActivityRecordingPreferences persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)
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

    @Test fun `selectFavoriteActivity persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)

        vm.selectFavoriteActivity(ExerciseSessionRecord.EXERCISE_TYPE_BIKING)

        verify { prefs.favoriteActivityExerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING }
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_BIKING, vm.uiState.value.favoriteActivityExerciseType)
    }

    @Test fun `refresh reads the elevation correction preference`() = runTest {
        val prefs = prefs()
        every { prefs.elevationCorrectionEnabled } returns false
        val vm = viewModel(preferencesRepository = prefs)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.elevationCorrectionEnabled)
    }

    @Test fun `toggling elevation correction writes the preference and the state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)

        vm.setElevationCorrectionEnabled(false)

        verify { prefs.elevationCorrectionEnabled = false }
        assertFalse(vm.uiState.value.elevationCorrectionEnabled)
    }

    @Test fun `importing an elevation tile reports the result and clears the busy flag`() = runTest {
        val repository = elevationTileRepository()
        val tile = elevationTile()
        coEvery { repository.importTile(any()) } returns tile
        val vm = viewModel(elevationTileRepository = repository)

        vm.importElevationTile(mockk<Uri>())
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isImportingElevationTile)
        assertEquals(tile, vm.uiState.value.elevationTileImportResult)
        assertNull(vm.uiState.value.elevationTileImportError)
    }

    @Test fun `a failed tile import surfaces the message`() = runTest {
        val repository = elevationTileRepository()
        coEvery { repository.importTile(any()) } throws IllegalArgumentException("Not an SRTM tile")
        val vm = viewModel(elevationTileRepository = repository)

        vm.importElevationTile(mockk<Uri>())
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isImportingElevationTile)
        assertEquals("Not an SRTM tile", vm.uiState.value.elevationTileImportError)
        assertNull(vm.uiState.value.elevationTileImportResult)
    }

    @Test fun `a second tile import while one runs is ignored`() = runTest {
        val repository = elevationTileRepository()
        coEvery { repository.importTile(any()) } coAnswers {
            delay(1_000)
            elevationTile()
        }
        val vm = viewModel(elevationTileRepository = repository)

        vm.importElevationTile(mockk<Uri>())
        vm.importElevationTile(mockk<Uri>())
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.importTile(any()) }
    }

    @Test fun `deleting a tile delegates to the repository and reports a failure`() = runTest {
        val repository = elevationTileRepository()
        val key = HgtTileKey(45, 7)
        val vm = viewModel(elevationTileRepository = repository)

        vm.deleteElevationTile(key)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.deleteTile(key) }

        coEvery { repository.deleteTile(key) } throws IllegalStateException("locked")
        vm.deleteElevationTile(key)
        advanceUntilIdle()
        assertEquals("locked", vm.uiState.value.elevationTileImportError)
    }

    private fun elevationTile() = ElevationTile(
        key = HgtTileKey(45, 7),
        resolution = HgtResolution.THREE_ARC_SECOND,
        sizeBytes = HgtResolution.THREE_ARC_SECOND.byteSize,
        importedAtMillis = 0L,
        path = "/tiles/N45E007.hgt",
    )

    private fun viewModel(
        preferencesRepository: PreferencesRepository = prefs(),
        stepDistanceBackfillService: StepDistanceBackfillService = mockk(relaxed = true),
        offlineMapRepository: OfflineMapRepository = offlineMapRepository(),
        offlineMapImportWorkController: OfflineMapImportWorkController = offlineMapImportController(),
        elevationTileRepository: ElevationTileRepository = elevationTileRepository(),
    ): ActivitiesSettingsViewModel =
        ActivitiesSettingsViewModel(
            preferencesRepository = preferencesRepository,
            stepDistanceBackfillService = stepDistanceBackfillService,
            offlineMapRepository = offlineMapRepository,
            offlineMapImportWorkController = offlineMapImportWorkController,
            elevationTileRepository = elevationTileRepository,
            coMapsNavigationRepository = mockk(relaxed = true),
        )

    private fun prefs(): PreferencesRepository =
        mockk<PreferencesRepository>().also { prefs ->
            every { prefs.unitSystem } returns UnitSystem.METRIC
            every { prefs.unitOverride(any()) } returns null
            every { prefs.favoriteActivityExerciseType } returns null
            every { prefs.favoriteActivityExerciseType = any() } just runs
            every { prefs.activitySplitDistanceMeters } returns 1000.0
            every { prefs.activitySplitDistanceMeters = any() } just runs
            every { prefs.stepDistanceBackfillEnabled } returns false
            every { prefs.stepDistanceBackfillEnabled = any() } just runs
            every { prefs.strideLengthMeters } returns 0.7
            every { prefs.strideLengthMeters = any() } just runs
            every { prefs.activityRecordingPreferences() } returns ActivityRecordingPreferences()
            every { prefs.setActivityRecordingPreferences(any()) } just runs
            every { prefs.elevationCorrectionEnabled } returns true
            every { prefs.elevationCorrectionEnabled = any() } just runs
        }

    private fun offlineMapRepository(): OfflineMapRepository =
        mockk<OfflineMapRepository>(relaxed = true).also { repository ->
            every { repository.state } returns MutableStateFlow(OfflineMapLibraryState())
        }

    private fun elevationTileRepository(): ElevationTileRepository =
        mockk<ElevationTileRepository>(relaxed = true).also { repository ->
            every { repository.state } returns MutableStateFlow(ElevationTileLibraryState())
        }

    private fun offlineMapImportController(): OfflineMapImportWorkController =
        mockk<OfflineMapImportWorkController>(relaxed = true).also { controller ->
            every { controller.workInfos } returns emptyFlow()
        }
}
